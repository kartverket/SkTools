package no.statkart.sktools.gradle.plugins.weblogic.deploy

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.util.ConfigureUtil

/**
 * Konfigurasjon av weblogic-tjeneren det skal deployes på / undeployes fra.
 *
 * @author Tor Egil R. Strand
 * @author Leif Lislegård
 * @since 1.2
 */
class WeblogicDeployConfiguration {
    protected final transient Project project
    protected final WeblogicDeployConvention convention

    protected String protocol
    protected String host
    protected String port

    /**
     * Hvis ikke satt, genereres en fra protocol, host og port.
     * @see #getUrl()
     */
    private String url
    /**
     * Komma sepparert liste av targets for deployment. <br>
     * Dersom ikke satt defaulter weblogic til Admin server instansen
     */
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
    protected Task askIfProdserverTask

    protected def dependsOn = []


    protected WeblogicDeployConfiguration(Project project, WeblogicDeployConvention convention) {
        this.project = project
        this.convention = convention
    }

    /**
     * Legger til task for interaksjon med bruker.
     * <p>
     *  Typisk legges denne til:
     *  <code> onlyIf { productionServerList.find {it.equalsIgnoreCase(host)} == null } </code>
     *
     * @param config
     * @return
     */
    Task askIfProdserverTask(Closure config) {
        askIfProdserverTask = project.task(convention.getTaskName('askIfProdserver', name)) {
            doFirst {
                print("\nDeploy til PRODSERVER (${url}), vil du fortsette? (j/n) ")
                System.out.flush();
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in))
                def svar = br.readLine()
                if( !svar.equalsIgnoreCase("j") && !svar.equalsIgnoreCase("ja") ) {
                    println "Build aborted by user"
                    assert false
                }
            }
        }
        this.dependsOn askIfProdserverTask
        ConfigureUtil.configure(config, askIfProdserverTask)
    }


    WeblogicUndeployTask undeployTask(String name, Closure config = null) {
        return undeployTask([:], name, config)
    }
    WeblogicUndeployTask undeployTask(Map params, String name, Closure config = null) {
        if (name == null || name.trim().isEmpty()) {
            throw new GradleException('name parameter not supplied for task!')
        }
        undeployTask = (WeblogicUndeployTask) project.task(type: WeblogicUndeployTask.class, name)
        setCommonConventionalValues(undeployTask)

        ConfigureUtil.configureByMap(params, undeployTask)
        ConfigureUtil.configure(config, undeployTask)
        return undeployTask
    }


    WeblogicDeployTask deployTask(String name, Closure config = null) {
        return deployTask([:], name, config)
    }
    WeblogicDeployTask deployTask(Map params, String name, Closure config = null) {
        if (name == null || name.trim().isEmpty()) {
            throw new GradleException('name parameter not supplied for task!')
        }

        Task task = project.task(type: WeblogicDeployTask.class, name)
        deployTask = (WeblogicDeployTask) task
        setCommonConventionalValues(deployTask)
        deployTask.conventionMapping 'file', { this.getFile() }

        ConfigureUtil.configureByMap(params, deployTask)
        ConfigureUtil.configure(config, deployTask)
        return deployTask
    }

    private ConventionTask setCommonConventionalValues(ConventionTask task) {
        task.dependsOn { this.dependsOn }
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

    Task getAskIfProdserverTask() {
        if (askIfProdserverTask == null) throw new GradleException("Ask if prodserver task not yet configured!");
        return askIfProdserverTask
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

    void url(Map params) {
        ConfigureUtil.configureByMap(params, this)
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

    /**
     * File, artifakt, task
     * @param buildable
     */
    void setFile(Object deployable) {
        this.file = deployable
//        dependsOn((deployable instanceof org.gradle.api.Buildable || deployable instanceof Task) ? deployable : project.files(deployable))
    }

    void setName(String deploymentName) {
        this.name = deploymentName
    }

    public String getName() {
        return name;
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

    void dependsOn(Object... objects) {
        dependsOn << objects
    }
}