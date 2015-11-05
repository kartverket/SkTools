package no.statkart.sktools.gradle.plugins.filterresources;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.internal.HasConvention;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.language.jvm.tasks.ProcessResources;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * SKTOOLS-44: Plugin kun for filtrering av resourceSets
 *
 * @author Leif Lislegård
 * @since 1.3
 */
public class FilterResourcesPlugin implements Plugin<ProjectInternal> {

    public final static String CONVENTION_NAME = "filterProperties";

    public final static String FILTER_MAIN_RESOURCES_TASK_NAME = "filterResources";

    /**
     * Ihht {@link FilterResourcesSourceSet#FILTER_RESOURCES_TASK_NAME_PATTERN}
     */
    public final static String FILTER_TEST_RESOURCES_TASK_NAME = "filterTestResources";


    @Override
    public void apply(ProjectInternal project) {
        project.getPlugins().apply(JavaBasePlugin.class);

        final FilterResourcesConvention filterPropertiesConvention = new FilterResourcesConvention(project);
        project.getConvention().getPlugins().put(CONVENTION_NAME, filterPropertiesConvention);

        project.afterEvaluate(new Action<Project>() {
            public void execute(Project project) {
                setConventionalDefaults(filterPropertiesConvention);
            }
        });

        configureSourceSetDefaults(project, filterPropertiesConvention);

    }

    /**
     * Setter default verdier etter at all annen konfigurasjon er gjort.
     */
    private void setConventionalDefaults(FilterResourcesConvention filterPropertiesConvention) {
        Project project = filterPropertiesConvention.project;

        if (filterPropertiesConvention.properties == null) {
            filterPropertiesConvention.properties = projectPropertiesFrom(project);
        }

    }


    private void configureSourceSetDefaults(final ProjectInternal project, final FilterResourcesConvention convention) {
        //for hvert source sett som finnes/blir lagt til
        project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().all(new Action<SourceSet>() {
            public void execute(final SourceSet sourceSet) {
                //oppretter source set-utvidelse for filtrerte ressurser
                final DefaultFilterResourcesSourceSet filterResourcesSourceSet = new DefaultFilterResourcesSourceSet(sourceSet.getName(), project.getFileResolver());
                final DefaultFilterResourcesSourceSetOutput filterResourcesSourceSetOutput = new DefaultFilterResourcesSourceSetOutput(sourceSet, project);

                //hekter inn utvidelser på source settet
                ((HasConvention) sourceSet).getConvention().getPlugins().put(CONVENTION_NAME, filterResourcesSourceSet); // SKIF-173
                ((HasConvention) sourceSet.getOutput()).getConvention().getPlugins().put(CONVENTION_NAME, filterResourcesSourceSetOutput); // SKIF-173

                filterResourcesSourceSet.getFilterResources().srcDir(String.format("src/%s/filterResources", sourceSet.getName()));

                //trekker ifra evt filer som evt også befinner seg i 'resources'
                sourceSet.getResources().getFilter().exclude(new Spec<FileTreeElement>() {
                    public boolean isSatisfiedBy(FileTreeElement element) {
                        return filterResourcesSourceSet.getFilterResources().contains(element.getFile());
                    }
                });


                final String filterResourcesTaskName = filterResourcesSourceSet.getFilterResourcesTaskName();

                //hekter inn task for filtering
                project.getTasks().getByName(sourceSet.getProcessResourcesTaskName()).dependsOn(filterResourcesTaskName);

                //legger til clean
                project.getTasks().getByName(BasePlugin.CLEAN_TASK_NAME).doFirst(new Action<Task>() {
                    public void execute(Task cleanTask) {
                        cleanTask.getLogger().info("Deleting directory " + filterResourcesSourceSetOutput.getFilterResourcesOutputDir());
                        cleanTask.getProject().delete(filterResourcesSourceSetOutput.getFilterResourcesOutputDir());
                    }
                });


                //oppretter copy task for filtrering...
                final ProcessResources filterResourcesTask = project.getTasks().create(filterResourcesTaskName, ProcessResources.class);
                filterResourcesTask.setDescription(String.format("Filters the %s resources for filtering.", sourceSet.getName()));

                filterResourcesTask.setFileMode(0755);  //SKTOOLS-123 no read only generated files i linux
                filterResourcesTask.setDirMode(0755); //SKTOOLS-123 no read only generated files i linux

                filterResourcesTask.from(new Callable<Object>() {
                    public Object call() throws Exception {
                        return filterResourcesSourceSet.getFilterResources();
                    }
                });
                filterResourcesTask.into(new Callable<Object>() {
                    public Object call() throws Exception {
                        return filterResourcesSourceSetOutput.getFilterResourcesOutputDir();
                    }
                });


                project.afterEvaluate(new Action<Object>() {
                    public void execute(Object o) {
                        //default verdier for filterResoruces source set
                        if (filterResourcesSourceSetOutput.filteredResourcesDir == null) {
                            filterResourcesSourceSetOutput.filterResourcesOutput(String.format("gen/%s/resources", sourceSet.getName()));
                        }

                        //registrerer builtBy
                        sourceSet.getOutput().dir(Collections.<String, Object>singletonMap("builtBy", filterResourcesSourceSet.getFilterResourcesTaskName()), filterResourcesSourceSetOutput.getFilterResourcesOutputDir());

                        //registrerre properties til task
                        Map<String, Object> filterProperties = convention.getProperties();
                        filterResourcesTask.getInputs().properties(filterProperties);
                        filterResourcesTask.filter(Collections.singletonMap("tokens", filterProperties), org.apache.tools.ant.filters.ReplaceTokens.class);
                    }
                });

            }
        });

    }

    /**
     * Convenient way of retrieving project properties
     */
    static Map<String, Object> projectPropertiesFrom(final Project project) {
        HashMap<String, Object> filteredProjectProperties = new HashMap<String, Object>();
        for (Map.Entry<String, ?> entry : project.getProperties().entrySet()) {
            if (entry.getValue() instanceof CharSequence) {
                filteredProjectProperties.put(entry.getKey(), entry.getValue().toString());
            }
        }
        return filteredProjectProperties;
    }

}
