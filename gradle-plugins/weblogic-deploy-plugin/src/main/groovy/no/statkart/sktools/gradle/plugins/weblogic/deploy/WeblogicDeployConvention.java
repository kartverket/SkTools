package no.statkart.sktools.gradle.plugins.weblogic.deploy;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.util.ConfigureUtil;
import org.gradle.util.GUtil;

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

    public static String getTaskName(String verb, String target) {
        return GUtil.toLowerCamelCase(verb + ' ' + target);
    }

    Action<AbstractWeblogicDeployTask> conventionalValuesForAbstractWeblogicDeployTask() {
        return new Action<AbstractWeblogicDeployTask>() {
            @Override
            public void execute(AbstractWeblogicDeployTask task) {
                task.conventionMapping("classpath", new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        FileCollection classpath = configuration.getClasspath();
                        if (classpath.isEmpty()) {
                            return findAntClasspath(project);
                        }
                        return classpath;
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


    static FileCollection findAntClasspath(Project project) {
        if (project.hasProperty("WEBLOGIC_HOME")) {
            Object wlsVersion = project.findProperty("WEBLOGIC_VERSION");
            return weblogicClasspathFor(String.valueOf(wlsVersion), project);
        }
        return project.files();
    }


    static FileCollection weblogicClasspathFor(String wlsVersion, Project project) {
        Object WEBLOGIC_HOME = project.property("WEBLOGIC_HOME");

        if (wlsVersion.startsWith("10.3")) {
            return project.files(WEBLOGIC_HOME + "/wlserver_10.3/server/lib/weblogic.jar");
        }

        if (wlsVersion.startsWith("12.1")) {
            return project.files(WEBLOGIC_HOME + "/wlserver/server/lib/weblogic.jar");
        }

        if (wlsVersion.startsWith("12.2")) {
            return project.files(WEBLOGIC_HOME + "/wlserver/modules/features/wlst.wls.classpath.jar");
        }

        project.getLogger().warn("WARNING: no classpath defined for weblogic version " + wlsVersion + " - defaulting to WLS 12.x");
        ConfigurableFileCollection wls12 = project.files(WEBLOGIC_HOME + "/wlserver/server/lib/weblogic.jar"); //12.1
        wls12.from(WEBLOGIC_HOME + "/wlserver/modules/features/wlst.wls.classpath.jar"); //12.2
        return wls12;
    }

}
