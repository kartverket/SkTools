package no.statkart.sktools.gradle.plugins.wsdocgen


/**
 *
 * @since 1.0
 * @author Thor Åge Eldby
 * @author Leif Lislegård
 */
class WsDocGenConvention implements Serializable {
    private static final long serialVersionUID = 1L; //SKTOOLS-130: remove Serializable in sktools version 2.1

    public final String GEN_TASK_NAME_PATTERN = "gen%sWsdoc%s"


    URL defaultXsltTransform = WsDocGenConvention.class.getResource('tasks/DefaultTransform.xsl')
    public File generateTestFile(File testFile) {
        if (testFile.exists()) return testFile;

        testFile.getParentFile().mkdirs()
        testFile.createNewFile()

        testFile.withWriter { def writer ->
            defaultXsltTransform.with { xsltResources -> //groovy way of handling streams
                if (!xsltResources) {
                    throw new RuntimeException("Resource not found: " + defaultXsltTransform)
                }
                xsltResources.withReader() {
                    it.readLines().each { writer.write(it); writer.write("\n") }
                }
            }
            writer.flush()
        }
        return testFile
    }

}

