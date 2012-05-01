package no.statkart.sktools.gradle.plugins.weblogic

import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Action
import org.gradle.api.artifacts.Configuration
import java.util.concurrent.Callable

/**
 * Setter opp {@link org.gradle.api.artifacts.Configuration} for weblogic clsaspath.
 * Pluginen setter {@link WeblogicTaskInterface#setWeblogicClasspath} conventional verdi på alle subtyper av denne
 * task som blir lagt til prosjektet.
 *
 * @author Leif Lislegård
 */
class WeblogicBasePlugin implements Plugin<ProjectInternal> {

    public static final String WEBLOGIC_CONFIGURATION_NAME = 'weblogic';

    @Override
    void apply(ProjectInternal project) {

        createConfiguration(project);
        configureCompileDefaults(project);

    }

    private Configuration createConfiguration(ProjectInternal project) {
        project.getConfigurations().add(WEBLOGIC_CONFIGURATION_NAME).setVisible(false).setTransitive(false).setDescription("The weblogic libraries to be used for this project.")
    }

    private void configureCompileDefaults(final Project project) {
        project.getTasks().withType(WeblogicTaskInterface.class, new Action<WeblogicTaskInterface>() {
            public void execute(WeblogicTaskInterface compile) {
                //setter weblogicClasspath property som conventional value.
                // Dvs at følgende default verdier blir brukt dersom ikke property eksplisitt blir satt (null)
                compile.getConventionMapping().map("weblogicClasspath", new Callable() {
                    Object call() {
                        return project.getConfigurations().getByName(WeblogicBasePlugin.WEBLOGIC_CONFIGURATION_NAME);
//                        return project.getConfigurations().getByName(WeblogicBasePlugin.WEBLOGIC_CONFIGURATION_NAME).copy().setTransitive(true);
                    }
                });
            }
        });
    }

}
