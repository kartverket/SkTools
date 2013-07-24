package no.statkart.sktools.gradle.plugins.weblogic

import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Action
import org.gradle.api.artifacts.Configuration
import java.util.concurrent.Callable

/**
 * Setter opp {@link org.gradle.api.artifacts.Configuration} for weblogic bibliotek og/eller avhengigheter som skal være providede.
 *
 * <p>
 * Pluginen setter {@link WeblogicTaskInterface#setWeblogicClasspath} conventional verdi på alle subtyper av denne
 * task som blir lagt til prosjektet.
 *
 * <p>
 * <!-- SKIF-213 -->
 * Dersom en ønsker å publisere weblogic spesifike artifakter som war filer mm så bør disse kobles til egen configuration.
 * Det er IKKE anbefalt å benytte {@link WeblogicBasePlugin#WEBLOGIC_PROVIDED_CONFIGURATION_NAME} til dette.
 *
 * @author Leif Lislegård
 */
class WeblogicBasePlugin implements Plugin<ProjectInternal> {

    public static final String WEBLOGIC_PROVIDED_CONFIGURATION_NAME = 'weblogicProvided';

    @Override
    void apply(ProjectInternal project) {

        createConfiguration(project);

        configureWeblogicTaskDefaults(project);

    }

    private Configuration createConfiguration(ProjectInternal project) {
        project.getConfigurations().create(WEBLOGIC_PROVIDED_CONFIGURATION_NAME).setVisible(false).setTransitive(true).setDescription("Weblogic tools and provided libraries.")
    }

    private void configureWeblogicTaskDefaults(final Project project) {
        project.getTasks().withType(WeblogicTaskInterface.class, new Action<WeblogicTaskInterface>() {
            public void execute(WeblogicTaskInterface compile) {
                //setter weblogicClasspath property som conventional value.
                // Dvs at følgende default verdier blir brukt dersom ikke property eksplisitt blir satt (null)
                compile.getConventionMapping().map("weblogicClasspath", new Callable() {
                    Object call() {
                        return project.getConfigurations().getByName(WeblogicBasePlugin.WEBLOGIC_PROVIDED_CONFIGURATION_NAME);
                    }
                });
            }
        });
    }

}
