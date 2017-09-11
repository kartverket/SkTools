package no.statkart.sktools.gradle.plugins.weblogic;

import org.gradle.api.Task;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Action;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.specs.Spec;

/**
 * Setter opp {@link org.gradle.api.artifacts.Configuration} for weblogic bibliotek og/eller avhengigheter som skal være providede.
 *
 * <p>
 * Pluginen setter {@link WeblogicTaskInterface#setWeblogicClasspath} conventional verdi på alle subtyper av denne
 * task som blir lagt til prosjektet.
 *
 * <p>
 * <!-- SKTOOLS-19: Provided configuration -->
 * Dersom en ønsker å publisere weblogic spesifike artifakter som war filer mm så bør disse kobles til egen configuration.
 * Det er IKKE anbefalt å benytte {@link WeblogicBasePlugin#WEBLOGIC_PROVIDED_CONFIGURATION_NAME} til dette.
 *
 * @author Leif Lislegård
 */
public class WeblogicBasePlugin implements Plugin<Project> {

    public static final String WEBLOGIC_PROVIDED_CONFIGURATION_NAME = "weblogicProvided";

    @Override
    public void apply(Project project) {
        createConfiguration(project);
        configureWeblogicTaskDefaults(project);
    }

    private static Configuration createConfiguration(Project project) {
        return project.getConfigurations().create(WEBLOGIC_PROVIDED_CONFIGURATION_NAME)
                .setVisible(false)
                .setTransitive(true)
                .setDescription("Weblogic tools and provided libraries.")
        ;
    }

    private static void configureWeblogicTaskDefaults(final Project project) {
        project.getTasks().matching(new Spec<Task>(){
            @Override
            public boolean isSatisfiedBy(Task task) {
                return task instanceof WeblogicTaskInterface;
            }
        }).whenTaskAdded( new Action<Task>() {
            @Override
            public void execute(Task compile) {
                final WeblogicTaskInterface weblogicTask = (WeblogicTaskInterface) compile;
                final Configuration weblogicProviedConfiguration = project.getConfigurations().getByName(WeblogicBasePlugin.WEBLOGIC_PROVIDED_CONFIGURATION_NAME);
                //sets conventional weblogicClasspath.
                weblogicTask.setWeblogicClasspath(weblogicProviedConfiguration);
            }
        });
    }

    public static void addToolsJarToWeblogicProvidedClasspath(Project project) {
        project.getDependencies().add(WEBLOGIC_PROVIDED_CONFIGURATION_NAME, project.files(
                System.getProperty("java.home") + "/../lib/tools.jar" /* for windows */,
                System.getProperty("java.home") + "/../classes/classes.jar" /* for mac os*/
        ));
    }

}
