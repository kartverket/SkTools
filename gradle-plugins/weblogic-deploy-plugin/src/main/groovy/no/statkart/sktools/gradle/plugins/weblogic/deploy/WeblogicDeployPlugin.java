package no.statkart.sktools.gradle.plugins.weblogic.deploy;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Plugin for deployment-tasker til Weblogic Server.
 *
 * @author Tor Egil R. Strand
 * @author Leif Lislegård
 * @since 1.2
 */
public class WeblogicDeployPlugin implements Plugin<Project> {
    public final static String WEBLOGIC_DEPLOY_CONVENTION_NAME = "weblogicDeployConvention";

    @Override
    public void apply(Project project) {
        project.getLogger().warn("WARNING: WeblogicDeployPlugin is deprecated and is scheduled for removal in sktools 7.0!");

        WeblogicDeployConvention convention = new WeblogicDeployConvention(project);
        project.getConvention().getPlugins().put(WEBLOGIC_DEPLOY_CONVENTION_NAME, convention);

        project.getTasks().withType(AbstractWeblogicDeployTask.class, convention.conventionalValuesForAbstractWeblogicDeployTask());
    }
}
