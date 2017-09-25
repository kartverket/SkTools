package no.statkart.sktools.gradle.plugins.weblogic.deploy;

import groovy.lang.Closure;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.util.ConfigureUtil;

import java.util.concurrent.Callable;

/**
 * Convention for å konfigurere opp egenskaper felles for både deploy og undeploy
 *
 * @author Tor Egil R. Strand
 * @since 1.2
 */
public class WeblogicDeployConvention {
    protected final transient Project project;
    final WeblogicDeployConfiguration configuration;

    WeblogicDeployConvention(Project project) {
        this.project = project;
        configuration = new WeblogicDeployConfiguration(project, this);
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
        return ConfigureUtil.configure(c, configuration);
    }

    public String getTaskName(String verb) {
        return getTaskName(verb, project.getName());
    }

    public String getTaskName(String verb, String target) {
        return StringUtils.uncapitalize(String.format("%s%s", verb, StringUtils.capitalize(target)));
    }

    Action<AbstractWeblogicDeployTask> conventionalValuesForAbstractWeblogicDeployTask() {
        return new Action<AbstractWeblogicDeployTask>() {
            @Override
            public void execute(AbstractWeblogicDeployTask task) {
                task.conventionMapping("classpath", new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        return configuration.getClasspath();
                    }
                });
                task.conventionMapping("url", new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        return configuration.getUrl();
                    }
                });
                task.conventionMapping("targets", new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        return configuration.getTargets();
                    }
                });
                task.conventionMapping("username", new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        return configuration.getUsername();
                    }
                });
                task.conventionMapping("password", new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        return configuration.getPassword();
                    }
                });
                task.conventionMapping("deploymentName", new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        return configuration.getName();
                    }
                });
            }
        };
    }
}
