package no.statkart.sktools.gradle.plugins.filterresources;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.SourceSet;
import org.gradle.plugins.ide.idea.IdeaPlugin;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * SKTOOLS-44: Plugin kun for filtrering av resourceSets
 *
 * @author Leif Lislegård
 * @since 1.3
 */
public class FilterResourcesPlugin implements Plugin<Project> {

    public final static String CONVENTION_NAME = "filterProperties";


    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(JavaBasePlugin.class);

        project.getLogger().warn("WARNING: FilterResourcesPlugin is deprecated and is scheduled for removal in sktools 7.0!");

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
                final FilterResourcesTask filterResourcesTask =
                    project.getTasks().create(sourceSet.getTaskName("filter", "Resources"), FilterResourcesTask.class);
                filterResourcesTask.setDescription(String.format("Filters the %s resources for filtering.", sourceSet.getName()));

                filterResourcesTask.setFileMode(0755);  //SKTOOLS-123 no read only generated files i linux
                filterResourcesTask.setDirMode(0755); //SKTOOLS-123 no read only generated files i linux

                //hekter inn utvidelser på source settet
                sourceSet.getExtensions().add("filterResources", filterResourcesTask); // SKIF-173

                filterResourcesTask.srcDir(String.format("src/%s/filterResources", sourceSet.getName()));

                //trekker ifra evt filer som evt også befinner seg i 'resources'
                sourceSet.getResources().getFilter().exclude(new Spec<FileTreeElement>() {
                    public boolean isSatisfiedBy(FileTreeElement element) {
                        return filterResourcesTask.getSource().contains(element.getFile());
                    }
                });


                //registrerer sourceDir + hekter inn task for filtering
                sourceSet.getResources().srcDir(filterResourcesTask);

                //clean
                Delete cleanTask = (Delete) project.getTasks().getByName(BasePlugin.CLEAN_TASK_NAME);
                cleanTask.delete(filterResourcesTask);

                project.afterEvaluate(new Action<Project>() {
                    public void execute(Project project) {
                        //default verdier for filterResources source set
                        if (filterResourcesTask.getDestinationDir() == null) {
                            filterResourcesTask.into(
                                Paths.get(project.getBuildDir().toString(), "filteredResources", sourceSet.getName()));
                        }
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
