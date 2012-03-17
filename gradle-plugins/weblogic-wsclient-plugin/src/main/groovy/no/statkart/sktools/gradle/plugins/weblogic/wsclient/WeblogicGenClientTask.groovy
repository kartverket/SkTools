package no.statkart.sktools.gradle.plugins.weblogic.wsclient

import org.gradle.api.tasks.compile.Compile
import no.statkart.sktools.gradle.plugins.weblogic.WeblogicTaskInterface
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction

/**
 * Task for generering av weblogic webservice klient
 *
 *
 * @since 1.1
 * @author Leif Lislegård
 */
class WeblogicGenClientTask extends Compile implements WeblogicTaskInterface {

    private FileCollection weblogicClasspath


    WeblogicGenClientTask() {
        javaCompiler = new WeblogicJaxWsClientCompiler()
        logging.captureStandardOutput LogLevel.INFO
        logging.captureStandardError LogLevel.DEBUG

        include('**/*.wsdl') //inkluderer denne som input fra sourceset (benyttes bla for skipIfEmpty beregning)
    }

    public void setWeblogicClasspath(FileCollection weblogicClasspath) {
        this.weblogicClasspath = weblogicClasspath;
    }

    public FileCollection getWeblogicClasspath() {
        return weblogicClasspath;
    }

    @TaskAction
    protected void compile() {
        WeblogicJaxWsClientCompiler compiler = getJavaCompiler()
        compiler.weblogicClasspath = getWeblogicClasspath().files
        compiler.ant = getProject().createAntBuilder()
        compiler.webServices = project.getConvention().getPlugins().get(WeblogicWsClientPlugin.CONVENTION_NAME).webService
        super.compile()
    }

    /**
     * Action som retter loading av wsdl filer ifra webstart klienter osv.
     * Rettinger blir påført i klikdekoden.
     */
    @TaskAction
    protected void fixResourceLoaders() {
        ant.replaceregexp {
            regexp(pattern: /URL baseUrl;[^=]+\s(.*getResource).*;[^=]*.*baseUrl, "(.*)".*;([^{]*)MalformedURL/)
            substitution(expression: ('url \\1("/\\2");\\3'))
            fileset(dir: getDestinationDir(), erroronmissingdir: true) {
                include(name: '**/*.java')
            }
        }
    }

    /**
     * Action som samler alle exceptions for services til felles pakke.
     * Dette da vi ønsker at den genererte klientkoden skal gjenspeile strukturen til serveren, samt at man ønsker å gjenbruke exception klassene.
     */



}
