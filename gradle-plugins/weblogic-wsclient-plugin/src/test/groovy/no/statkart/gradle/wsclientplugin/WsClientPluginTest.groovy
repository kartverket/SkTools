package no.statkart.gradle.wsclientplugin

import org.testng.annotations.Test
import org.apache.commons.io.FileUtils
import static org.testng.Assert.*

class WsClientPluginTest {

    @Test
    void fixWSUrl_correctly_fixes_url_lookup() {
        File dir = makeTempDir()
        File sourceFile = getFromResource('BorettInformasjonServiceWS.orig')
        File targetFile = new File(dir, 'BorettInformasjonServiceWS.java')
        copyFile(sourceFile, targetFile)
        WsClientTask.fixWSUrl(new AntBuilder(), dir);
        assertEquals(FileUtils.readFileToString(getFromResource('BorettInformasjonServiceWS.result')), FileUtils.readFileToString(targetFile))
    }

    private File getFromResource(java.lang.String name) {
        return new File(getClass().getResource(name).getPath())
    }

    private def copyFile(File from, File to) {
        String content = FileUtils.readFileToString(from);
        FileUtils.writeStringToFile(to, content)
    }

    private File makeTempDir() {
        File dir = File.createTempFile("test-", "dir")
        dir.delete()
        dir.mkdir()
        dir
    }

}
