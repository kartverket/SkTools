package no.statkart.sktools.gradle.plugins.weblogic.deploy

import org.gradle.util.ConfigureUtil

/**
 * Convention for å konfigurere opp egenskaper felles for både deploy og undeploy
 *
 * @author Tor Egil R. Strand
 * @since 1.2
 */
class WeblogicDeployConvention {
    final WeblogicDeployConfiguration weblogicDeploy = new WeblogicDeployConfiguration()

    public weblogicDeploy(Closure c) {
        ConfigureUtil.configure(c, weblogicDeploy)
    }
}

/**
 * Konfigurasjon av weblogic-tjeneren det skal deployes på / undeployes fra.
 *
 * @author Tor Egil R. Strand
 * @since 1.2
 */
class WeblogicDeployConfiguration {
    private String protocol
    private String host
    private String port
    /**
     * Hvis ikke satt, genereres en fra protocol, host og port.
     */
    private String url
    private String targets

    private String username
    private String password

    /**
     * Det som skal deployes. Må inneholde bare én artefakt.
     */
    private Object artifact

    /**
     * Navn på deploymenten, slik at undeploy undeployer riktig deployment.
     */
    private String moduleName

    String getProtocol() {
        return protocol
    }

    void setProtocol(String protocol) {
        this.protocol = protocol
    }

    String getHost() {
        return host
    }

    void setHost(String host) {
        this.host = host
    }

    String getPort() {
        return port
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

    String getTargets() {
        return targets
    }

    void setTargets(String targets) {
        this.targets = targets
    }

    String getUsername() {
        return username
    }

    void setUsername(String username) {
        this.username = username
    }

    String getPassword() {
        return password
    }

    void setPassword(String password) {
        this.password = password
    }

    Object getArtifact() {
        return artifact
    }

    void setArtifact(Object artifact) {
        this.artifact = artifact
    }

    String getModuleName() {
        return moduleName
    }

    void setModuleName(String moduleName) {
        this.moduleName = moduleName
    }
}