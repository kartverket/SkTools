package no.statkart.sktools.gradle.plugins.filterproperties;

import no.statkart.sktools.gradle.plugins.filterproperties.extention.PropertyUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.Action;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.specs.Spec;
import org.gradle.api.internal.ConventionMapping;
import org.gradle.api.plugins.JavaBasePlugin;

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


        configureSourceSetDefaults(project);
        configureFilterResourcesTaskDefaults(filterPropertiesConvention);

        project.afterEvaluate(new Action<Project>() {
            public void execute(Project project) {
                setConventionalDefaults(filterPropertiesConvention);
            }
        });

        project.getExtensions().add(PROPERTY_UTILS_EXTENTION_NAME, new PropertyUtils(project));

    }

    private void configureFilterResourcesTaskDefaults(final FilterPropertiesConvention convention) {
        convention.project.getTasks().withType(FilterResourcesTask.class, new Action<FilterResourcesTask>() {
            public void execute(final FilterResourcesTask task) {
                //setter conventional verdi
                task.getConventionMapping().map("properties", new Callable<Object>() {
                    public Object call() throws Exception {
                        return convention.getProperties();
                    }
                });

                //legger til clean
                Task cleanTask = task.getProject().getTasks().getByName(BasePlugin.CLEAN_TASK_NAME)
                        .doFirst(new Action<Task>() {
                            public void execute(Task cleanTask) {
                                cleanTask.getLogger().info("Deleting directory " + task.getDestinationDir());
                                cleanTask.getProject().delete(task.getDestinationDir());
                            }
                        });
            }
        });
    }

    /**
     * Setter default verdier etter at all annen konfigurasjon er gjort.
     */
    private void setConventionalDefaults(FilterPropertiesConvention filterPropertiesConvention) {
        Project project = filterPropertiesConvention.project;

        if (filterPropertiesConvention.properties == null) {
            filterPropertiesConvention.properties(filterPropertiesConvention.projectProperties());
        }

    }


    private void configureSourceSetDefaults(final ProjectInternal project) {
        //for hvert source sett som finnes/blir lagt til
        project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().all(new Action<SourceSet>() {
            public void execute(final SourceSet sourceSet) {
                final FilterResourcesSourceSet filterResourcesSourceSet = new DefaultFilterResourcesSourceSet(sourceSet.getName(), project.getFileResolver());
                filterResourcesSourceSet.getUnfilteredResources().srcDir(String.format("src/%s/unfilteredResources", sourceSet.getName()));

                sourceSet.getResources().getFilter().exclude(new Spec<FileTreeElement>() {
                    public boolean isSatisfiedBy(FileTreeElement element) {
                        return filterResourcesSourceSet.getUnfilteredResources().contains(element.getFile());
                    }
                });


                String filterResourcesTaskName = filterResourcesSourceSet.getFilterResourcesTaskName();
                FilterResourcesTask filterResourcesTask = project.getTasks().add(filterResourcesTaskName, FilterResourcesTask.class);
                filterResourcesTask.setDescription(String.format("Filters the %s unfiltered resources.", sourceSet.getName()));
                ConventionMapping conventionMapping = filterResourcesTask.getConventionMapping();

                conventionMapping.map("defaultSource", new Callable<Object>() {
                    public Object call() throws Exception {
                        return filterResourcesSourceSet.getUnfilteredResources();
                    }
                });
                conventionMapping.map("destinationDir", new Callable<Object>() {
                    public Object call() throws Exception {
                        return project.file(String.format("gen/src/%s/resources", sourceSet.getName()));
                    }
                });


                //legger filtrert output til resources source set - verdien er lazy, dvs at den blir hentet runtime hver gang en beregner filer for resources sourcesett.
                sourceSet.getResources().srcDir(new Callable() {
                    public Object call() throws Exception {
                        FilterResourcesTask task = (FilterResourcesTask) project.getTasks().getByName(filterResourcesSourceSet.getFilterResourcesTaskName());
                        return task.getDestinationDir();
                    }
                });


                //hekter inn task for filtering
                project.getTasks().getByName(sourceSet.getProcessResourcesTaskName()).dependsOn(filterResourcesTaskName);

            }
        });
    }

}