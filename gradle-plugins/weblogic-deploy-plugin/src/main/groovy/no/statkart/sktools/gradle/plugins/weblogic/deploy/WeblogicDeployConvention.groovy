package no.statkart.sktools.gradle.plugins.weblogic.deploy

import org.gradle.util.ConfigureUtil
import org.gradle.api.Project
import org.gradle.api.GradleException
import org.gradle.api.internal.ConventionTask
import org.gradle.api.file.FileCollection
import org.gradle.api.Task
import org.apache.commons.lang.StringUtils

/**
 * Convention for å konfigurere opp egenskaper felles for både deploy og undeploy
 *
 * @author Tor Egil R. Strand
 * @since 1.2
 */
class WeblogicDeployConvention {
    final private Project project

    protected WeblogicDeployConvention(Project project) {
        this.project = project
    }

    /**
     * Konfigurerer opp sett av tasker for deploy basert på 'weblogic.ant.taskdefs.management.WLDeploy'
     * <p>
     *     Dette er deploy tasker som er ment for bruk i utviklingsmiljøer for å understøtte iterativ utvikling.
     *
     * <p> Følgende tasker er implementert
     * <ul>
     *   <li> WeblogicDeployTask - deployer deployment
     *   <li> WeblogicUndeployTask - undeployer deployment
     * </ul>
     */
    public weblogicDeploy(Closure c) {
        ConfigureUtil.configure(c, new WeblogicDeployConfiguration(project))
    }

    protected getTaskName(def verb, def target = project.name) {
        return StringUtils.uncapitalize(String.format("%s%s", verb, StringUtils.capitalize(target)))
    }
}

/**
 * Konfigurasjon av weblogic-tjeneren det skal deployes på / undeployes fra.
 *
 * @author Tor Egil R. Strand
 * @author Leif Lislegård
 * @since 1.2
 */
class WeblogicDeployConfiguration {
    final private Project project

    protected String protocol
    protected String host
    protected String port

    /**
     * Hvis ikke satt, genereres en fra protocol, host og port.
     * @see #getUrl()
     */
    private String url
    protected String targets

    protected String username
    protected String password

    /**
     * Navn på deploymenten, slik at undeploy undeployer riktig deployment.
     */
    protected String name

    /**
     * Det som skal deployes. Må inneholde bare ett artefakt.
     * @see #getFile()
     */
    protected Object file

    /**
     * @see #getClasspath()
     */
    private Object classpath

    protected WeblogicDeployTask deployTask
    protected WeblogicUndeployTask undeployTask



    protected WeblogicDeployConfiguration(Project project) {
        this.project = project
    }


    WeblogicUndeployTask undeployTask(String name, Closure config = null) {
        return undeployTask([:], name, config)
    }
    WeblogicUndeployTask undeployTask(Map params, String name, Closure config = null) {
        if (name == null || name.trim().isEmpty()) {
            throw new GradleException('name parameter not supplied for task!')
        }
        undeployTask = (WeblogicUndeployTask) project.tasks.add(name: name, type: WeblogicUndeployTask.class)
        setCommonConventionalValues(undeployTask)

        ConfigureUtil.configureByMap(params, undeployTask)
        ConfigureUtil.configure(config, undeployTask, false)
        return undeployTask
    }


    WeblogicDeployTask deployTask(String name, Closure config = null) {
        return deployTask([:], name, config)
    }
    WeblogicDeployTask deployTask(Map params, String name, Closure config = null) {
        if (name == null || name.trim().isEmpty()) {
            throw new GradleException('name parameter not supplied for task!')
        }

        Task task = project.tasks.add(name: name, type: WeblogicDeployTask.class)
        deployTask = (WeblogicDeployTask) task
        setCommonConventionalValues(deployTask)
        deployTask.conventionMapping 'file', { this.getFile() }

        ConfigureUtil.configureByMap(params, deployTask)
        ConfigureUtil.configure(config, deployTask, false)
        return deployTask
    }

    private ConventionTask setCommonConventionalValues(ConventionTask task) {
        task.conventionMapping 'classpath', { this.getClasspath() }
        task.conventionMapping 'deploymentName', { this.name }
        task.conventionMapping 'url', { this.getUrl() }
        task.conventionMapping 'targets', { this.targets }
        task.conventionMapping 'username', { this.username }
        task.conventionMapping 'password', { this.password }
        task
    }

    WeblogicUndeployTask getUndeployTask() {
        if (undeployTask == null) throw new GradleException("Undeploy task not yet configured!");
        return undeployTask
    }

    WeblogicDeployTask getDeployTask() {
        if (deployTask == null) throw new GradleException("Deploy task not yet configured!");
        return deployTask
    }

    void setProtocol(String protocol) {
        this.protocol = protocol
    }

    void setHost(String host) {
        this.host = host
    }

    void setPort(String port) {
        this.port = port
    }

    String getUrl() {
        if (url == null) {
            return "${protocol}://${host}:${port}"
        } else {
            return url
        }
    }

    void setUrl(String url) {
        this.url = url
    }

    void setTargets(String targets) {
        this.targets = targets
    }

    void setUsername(String username) {
        this.username = username
    }

    void setPassword(String password) {
        this.password = password
    }


    void setFile(Object artifact) {
        this.file = artifact
    }

    void setName(String deploymentName) {
        this.name = deploymentName
    }

    void setClasspath(Object classpath) {
        this.classpath = classpath
    }

    File getFile() {
        return project.files(file).singleFile
    }

    FileCollection getClasspath() {
        return project.files(classpath);
    }
}