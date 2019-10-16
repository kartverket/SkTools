package no.statkart.sktools.gradle.plugins.filterresources;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.internal.HasConvention;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.SourceSet;
import org.gradle.plugins.ide.idea.IdeaPlugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static no.statkart.sktools.gradle.plugins.filterresources.FilterResourcesSourceSetConvention.FILTER_RESOURCES_TASK_NAME_PATTERN;

/**
 * SKTOOLS-44: Plugin kun for filtrering av resourceSets
 *
 * @author Leif Lislegård
 * @since 1.3
 */
public class FilterResourcesPlugin implements Plugin<Project> {

    public final static String CONVENTION_NAME = "filterProperties";

    /**
     * Ihht {@link FilterResourcesSourceSetConvention}
     */
    public final static String FILTER_MAIN_RESOURCES_TASK_NAME = "filterResources";

    /**
     * Ihht {@link FilterResourcesSourceSetConvention}
     */
    public final static String FILTER_TEST_RESOURCES_TASK_NAME = String.format(FILTER_RESOURCES_TASK_NAME_PATTERN, "Test");


    @Override
    public void apply(Project project) {
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
    private static void setConventionalDefaults(FilterResourcesConvention filterPropertiesConvention) {
        Project project = filterPropertiesConvention.project;

        if (filterPropertiesConvention.properties == null) {
            filterPropertiesConvention.properties = projectPropertiesFrom(project);
        }

    }


    private static void configureSourceSetDefaults(final Project project, final FilterResourcesConvention convention) {
        //for hvert source sett som finnes/blir lagt til
        project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().all(new Action<SourceSet>() {
            public void execute(final SourceSet sourceSet) {


                //oppretter copy task for filtrering...
                final FilterResourcesTask filterResourcesTask;
                filterResourcesTask = project.getTasks().create(FilterResourcesSourceSetConvention.getFilterResourcesTaskName(sourceSet), FilterResourcesTask.class);
                filterResourcesTask.setDescription(String.format("Filters the %s resources for filtering.", sourceSet.getName()));

                filterResourcesTask.setFileMode(0755);  //SKTOOLS-123 no read only generated files i linux
                filterResourcesTask.setDirMode(0755); //SKTOOLS-123 no read only generated files i linux

                //oppretter source set-utvidelse for filtrerte ressurser
                final FilterResourcesSourceSetConvention sourceSetConvention = new FilterResourcesSourceSetConvention(sourceSet, filterResourcesTask);
                final FilterResourcesSourceSetOutputConvention sourceSetOutputConvention = new FilterResourcesSourceSetOutputConvention(filterResourcesTask, sourceSet, project);

                //hekter inn utvidelser på source settet
                ((HasConvention) sourceSet).getConvention().getPlugins().put(CONVENTION_NAME, sourceSetConvention); // SKIF-173
                ((HasConvention) sourceSet.getOutput()).getConvention().getPlugins().put(CONVENTION_NAME, sourceSetOutputConvention); // SKIF-173

                filterResourcesTask.srcDir(String.format("src/%s/filterResources", sourceSet.getName()));

                //trekker ifra evt filer som evt også befinner seg i 'resources'
                sourceSet.getResources().getFilter().exclude(new Spec<FileTreeElement>() {
                    public boolean isSatisfiedBy(FileTreeElement element) {
                        return filterResourcesTask.getSource().contains(element.getFile());
                    }
                });


                //hekter inn task for filtering
                project.getTasks().getByName(sourceSet.getProcessResourcesTaskName()).dependsOn(sourceSetConvention.getFilterResourcesTaskName());

                //clean
                Delete cleanTask = (Delete) project.getTasks().getByName(BasePlugin.CLEAN_TASK_NAME);
                cleanTask.delete(filterResourcesTask);

                project.afterEvaluate(new Action<Project>() {
                    public void execute(Project project) {
                        //default verdier for filterResources source set
                        if (sourceSetOutputConvention.getFilterResourcesOutputDir() == null) {
                            sourceSetOutputConvention.filterResourcesOutput(String.format("build/filteredResources/%s", sourceSet.getName()));
                        }

                        //registrerer sourceDir
                        sourceSet.getResources().srcDir(filterResourcesTask.getDestinationDir());

                        //registrerer properties til task
                        Map<String, Object> filterProperties = convention.getProperties();
                        filterResourcesTask.getInputs().properties(filterProperties);
                        filterResourcesTask.filter(Collections.singletonMap("tokens", filterProperties), org.apache.tools.ant.filters.ReplaceTokens.class);

                        // Fortell IntelliJ at filene er genererte
                        project.getPlugins().withType(IdeaPlugin.class, new Action<IdeaPlugin>() {
                            @Override
                            public void execute(IdeaPlugin ideaPlugin) {
                                filterResourcesTask.getDestinationDir().mkdirs();
                                ideaPlugin.getModel().getModule().getGeneratedSourceDirs().add(filterResourcesTask.getDestinationDir());
                            }
                        });
                    }
                });

            }
        });

    }

    /**
     * Convenient way of retrieving project properties
     */
    static Map<String, Object> projectPropertiesFrom(final Project project) {
        HashMap<String, Object> filteredProjectProperties = new HashMap<>();
        for (Map.Entry<String, ?> entry : project.getProperties().entrySet()) {
            if (entry.getValue() instanceof CharSequence) {
                filteredProjectProperties.put(entry.getKey(), entry.getValue().toString());
            }
        }
        return filteredProjectProperties;
    }

}
