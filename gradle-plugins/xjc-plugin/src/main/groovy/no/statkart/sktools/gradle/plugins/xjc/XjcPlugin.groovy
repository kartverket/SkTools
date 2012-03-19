package no.statkart.sktools.gradle.plugins.xjc

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.ConventionValue
import org.gradle.api.plugins.Convention
import org.gradle.api.internal.IConventionAware

/**
 * Genererer JAXB java klasser basert på <code>*.xsd<code> filer. <br />
 * Pluginen baserer seg på {@code JavaPlugin} og integrerer seg dynamiskt med denne.
 *
 * Se dokumentasjon for <i>xjc-plugins<i> modul for bruk av evt utvidelser.
 * <ul>
 *  <li>{@link GrunnbokDocPlugin}
 *  <li>{@link ListGenPlugin}
 * </ul>
 *
 * @since 1.0
 * @author Thor Åge Eldby
 * @author Leif Lislegård

 */
class XjcPlugin implements Plugin<Project> {

    final static String CONVENTION_NAME = 'xjc'
    final static String JAXB_CONFIGURATION_NAME = 'jaxb'
    final static String XJC_TASK_NAME = 'xjcGenerate'


    @Override
    void apply(Project project) {
        project.apply plugin: JavaPlugin.class

        final XjcConvention xjcConvention = new XjcConvention(project)
        project.convention.plugins.put(XjcPlugin.CONVENTION_NAME, xjcConvention);

        final Configuration xjcConfiguration = createConfiguration(project);

        //creates the task
        Task xjcTask = project.task(XJC_TASK_NAME, type: XjcTask.class).dependsOn(
                xjcConfiguration,
        );

        project.tasks[JavaPlugin.PROCESS_RESOURCES_TASK_NAME].dependsOn(
                XjcPlugin.XJC_TASK_NAME,    //hooks it into prior to 'resources' task
        );



        //setting defaults if not already configured
        project.afterEvaluate {
            setConventionalDefaults(xjcConvention)
            configureSourceSet(xjcConvention)
            configureConventionalValuesForXjcTask(project, xjcConvention)
        }

    }

    /**
     * Legger til mappe for generert kildekode til valgt source sett.
     * <p>
     * Se {@link XjcConvention#sourceSetName} for valg av sourceSet
     */
    private void configureSourceSet(XjcConvention xjcConvention) {
        Project project = xjcConvention.project
        JavaPluginConvention javaConvention = project.getConvention().getPlugins().get("java");
        SourceSet sourceSet = javaConvention.getSourceSets().getByName(xjcConvention.sourceSetName)

        //legger genererte java filer til source set
        sourceSet.getJava().srcDir(xjcConvention.targetDir)

        //legger til source slik at de kan bli plukket opp av dokumentajonsverktøy, kildekode distribusjon mm
        SourceTask xjcTask = project.getTasks().getByName(XjcPlugin.XJC_TASK_NAME)
        sourceSet.getAllSource().srcDir(xjcTask)

    }

    /**
     * Setter default verdier etter at all annen konfigurasjon er gjort.
     */
    private void setConventionalDefaults(XjcConvention xjcConvention) {
        Project project = xjcConvention.project

        if (xjcConvention.targetDir == null) {
            xjcConvention.targetDir = project.file("gen/${xjcConvention.sourceSetName}/java")
        }

        xjcConvention.schema.each {
            if (it.includes == null) it.includes = ['*.xsd']
        }
    }


    private configureConventionalValuesForXjcTask(final Project project, final XjcConvention xjcConvention) {
        project.tasks.getByName(XJC_TASK_NAME).getConventionMapping().with {
            map("defaultSource", new ConventionValue() { //tildeler en ikke dynamisk default verdi
                public Object getValue(Convention conventionManager, IConventionAware conventionAwareObject) {
                    return project.files(xjcConvention.schema.collect {it.dir}).getAsFileTree();  //default source
                }
            });
            map("schemas", new ConventionValue() {
                public Object getValue(Convention conventionManager, IConventionAware conventionAwareObject) {
                    return xjcConvention.schema;
                }
            });
            map("outputDirectory", new ConventionValue() {
                public Object getValue(Convention conventionManager, IConventionAware conventionAwareObject) {
                    return xjcConvention.targetDir;
                }
            });
            map("classpath", new ConventionValue() {
                public Object getValue(Convention conventionManager, IConventionAware conventionAwareObject) {
                    return project.getConfigurations().getByName(XjcPlugin.JAXB_CONFIGURATION_NAME);
                }
            });
        }
    }

    //classpath classpath for jaxb libs (needed at runtime)

    private Configuration createConfiguration(Project project) {
        return project.configurations.add(JAXB_CONFIGURATION_NAME).setVisible(false).setDescription("Classpath for jaxb library and extensions.");
    }

}
