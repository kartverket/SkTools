package no.statkart.sktools.gradle.plugins.filterproperties;

import no.statkart.sktools.gradle.plugins.filterproperties.extention.PropertyUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.internal.HasConvention;
import org.gradle.api.internal.plugins.ProcessResources;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.Action;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.specs.Spec;
import org.gradle.api.internal.ConventionMapping;
import org.gradle.api.plugins.JavaBasePlugin;

import java.util.Collections;
import java.util.concurrent.Callable;

/**
* Plugin baserer seg på {@link JavaBasePlugin} og utvider alle {@code SourceSet} som blir lagt til med mulighet
* til filtrerbare ressursfiler.
* <p/>
* <p/>
* Det er mulig å konfigurere hvilke properties som blir filtrert. Se {@link FilterPropertiesConvention} for konfigurasjon. <br/>
* Standard er at alle {@code project.properties} av type {@code String} blir filtrert inn.
* <p/>
* Ressursfiler får filtrert inn konfigurerte properties på formen <b>@propertynavn@</b>.
*
 * <p>
 * Det blir også registrert en extension som utvider DSLen med 'propertyUtils' instans. Se {@link PropertyUtils}
 *
* @author Thor Åge Eldby
* @author Leif Lislegård
* @since 1.0
*/
public class FilterPropertiesPlugin implements Plugin<ProjectInternal> {

    public final static String CONVENTION_NAME = "filterProperties";
    public final static String FILTER_MAIN_RESOURCES_TASK_NAME = "filterResources";
    public final static String PROPERTY_UTILS_EXTENTION_NAME = "propertyUtils";

    /**
     * Ihht {@link FilterResourcesSourceSet#FILTER_RESOURCES_TASK_NAME_PATTERN}
     */
    public final static String FILTER_TEST_RESOURCES_TASK_NAME = "filterTestResources";


    @Override
    public void apply(ProjectInternal project) {
        project.getPlugins().apply(JavaBasePlugin.class);

        final FilterPropertiesConvention filterPropertiesConvention = new FilterPropertiesConvention(project);
        project.getConvention().getPlugins().put(CONVENTION_NAME, filterPropertiesConvention);

        final PropertyUtils propertyUtils = new PropertyUtils(project);
        project.getExtensions().add(PROPERTY_UTILS_EXTENTION_NAME, propertyUtils);

        project.afterEvaluate(new Action<Project>() {
            public void execute(Project project) {
                setConventionalDefaults(filterPropertiesConvention, propertyUtils);
            }
        });

        configureSourceSetDefaults(project, filterPropertiesConvention);

    }

    /**
     * Setter default verdier etter at all annen konfigurasjon er gjort.
     */
    private void setConventionalDefaults(FilterPropertiesConvention filterPropertiesConvention, PropertyUtils propertyUtils) {
        Project project = filterPropertiesConvention.project;

        if (filterPropertiesConvention.properties == null) {
            filterPropertiesConvention.properties = propertyUtils.projectProperties();
        }

    }


    private void configureSourceSetDefaults(final ProjectInternal project, final FilterPropertiesConvention convention) {
        //for hvert source sett som finnes/blir lagt til
        project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().all(new Action<SourceSet>() {
            public void execute(final SourceSet sourceSet) {
                //oppretter source set-utvidelse for filtrerte ressurser
                final DefaultFilterResourcesSourceSet filterResourcesSourceSet = new DefaultFilterResourcesSourceSet(sourceSet.getName(), project.getFileResolver());
                final DefaultFilterResourcesSourceSetOutput filterResourcesSourceSetOutput = new DefaultFilterResourcesSourceSetOutput(sourceSet.getName(), project.getFileResolver(), project.getTasks());

                //hekter inn utvidelser på source settet
                ((HasConvention) sourceSet).getConvention().getPlugins().put(CONVENTION_NAME, filterResourcesSourceSet); // SKIF-173
                ((HasConvention) sourceSet.getOutput()).getConvention().getPlugins().put(CONVENTION_NAME, filterResourcesSourceSetOutput); // SKIF-173

                sourceSet.getOutput().dir(Collections.singletonMap("builtBy", (Object) filterResourcesSourceSet.getFilterResourcesTaskName()), filterResourcesSourceSetOutput);

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
                final ProcessResources filterResourcesTask;
                if (project.getGradle().getGradleVersion().compareTo("1.5") > 0 ) {
                    filterResourcesTask = project.getTasks().replace(filterResourcesTaskName, ProcessResources.class); //todo: endre bruk av replace() til create()
                } else {
                    filterResourcesTask = project.getTasks().add(filterResourcesTaskName, ProcessResources.class); //todo: remove backward compability with Gradle 1.5
                }

                filterResourcesTask.setDescription(String.format("Filters the %s resources for filtering.", sourceSet.getName()));

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
                        if (filterResourcesSourceSetOutput.getFilterResourcesOutputDir() == null) {
                            filterResourcesSourceSetOutput.filterResourcesOutput(String.format("gen/%s/resources", sourceSet.getName()));
                        }

                        //registrerre properties til task
                        filterResourcesTask.getInputs().properties(convention.getProperties());
                        filterResourcesTask.filter(Collections.singletonMap("tokens", convention.getProperties()), org.apache.tools.ant.filters.ReplaceTokens.class);
                    }
                });
            }
        });
    }

}