package no.statkart.sktools.gradle.plugins.weblogic.deploy;

import groovy.lang.Closure;
import org.apache.commons.lang.StringUtils;
import org.gradle.api.Project;
import org.gradle.util.ConfigureUtil;

/**
 * Convention for å konfigurere opp egenskaper felles for både deploy og undeploy
 *
 * @author Tor Egil R. Strand
 * @since 1.2
 */
public class WeblogicDeployConvention {
    protected final transient Project project;

    WeblogicDeployConvention(Project project) {
        this.project = project;
    }

    /**
     * Konfigurerer opp sett av tasker for deploy basert på 'weblogic.ant.taskdefs.management.WLDeploy'
     * <p/>
     * Dette er deploy tasker som er ment for bruk i utviklingsmiljøer for å understøtte iterativ utvikling.
     * <p/>
     * <p> Følgende tasker er implementert
     * <ul>
     * <li> WeblogicDeployTask - deployer deployment
     * <li> WeblogicUndeployTask - undeployer deployment
     * </ul>
     */
    public WeblogicDeployConfiguration weblogicDeploy(Closure c) {
        return ConfigureUtil.configure(c, new WeblogicDeployConfiguration(project, this));
    }

    public String getTaskName(String verb) {
        return getTaskName(verb, project.getName());
    }

    public String getTaskName(String verb, String target) {
        return StringUtils.uncapitalize(String.format("%s%s", verb, StringUtils.capitalize(target)));
    }

}
