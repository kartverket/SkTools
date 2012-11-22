package no.statkart.sktools.gradle.plugins.dbtools

import org.testng.annotations.Test
import no.statkart.sktools.utils.databasepatcher.DatabasePatcher
import org.testng.Assert
import org.gradle.api.internal.file.BaseDirFileResolver

/**
 *
 */
class DatabasePatcherTest extends HSQLDBTest {

    private void setUpDatabasePatcher() {
        System.setProperty("hibernate.connection.driver_class", jdbcDriverClassString)
        System.setProperty("hibernate.connection.url", sql.connection.metaData.URL)
        System.setProperty("hibernate.connection.username", username)
        System.setProperty("hibernate.connection.password", password)
    }

    /**
     * Verifiserer at man kan angi absolutt filsti for "patch.sql"
     */
    @Test
    public void testAbsoluteFileName() {
        File patchFile = createSimplePatchFile();

        setUpDatabasePatcher()
        DatabasePatcher.main('patch', patchFile.toString())

        def row = sql.firstRow('select ID, NAVN from TEST_TABLE where ID = 1')

        Assert.assertNotNull(row, 'Forventer en rad')
        Assert.assertEquals(row.ID, 1, 'forventet ID')
        Assert.assertEquals(row.NAVN, 'CHUCK NORRIS', 'forventet NAVN')

    }

    /**
     * Verifiserer at man kan angi relativ filsti for "patch.sql"
     */
    @Test
    public void testRelativeFileName() {
        File baseDir = new File(".")
        File subDir = new File(baseDir, "subdir")
        subDir.mkdir()
        File patchFile = createSimplePatchFile(subDir);

        String relativePath = "subdir/" + patchFile.getName()

        setUpDatabasePatcher()
        DatabasePatcher.main('patch', relativePath)

        def row = sql.firstRow('select ID, NAVN from TEST_TABLE where ID = 1')

        Assert.assertNotNull(row, 'Forventer en rad')
        Assert.assertEquals(row.ID, 1, 'forventet ID')
        Assert.assertEquals(row.NAVN, 'CHUCK NORRIS', 'forventet NAVN')

    }


    /**
     * Verifiserer at tabell for patchdata opprettes automatisk
     */
    @Test
    public void testNoPatchdataTable() {
        File patchFile = createEmptyFile();

        setUpDatabasePatcher()
        DatabasePatcher.main('patch', patchFile.toString())

        def row = sql.firstRow('select * from PATCHINFO')

        Assert.assertNotNull(row, 'Forventer en rad')
        Assert.assertEquals(row.dbVersion, null, "Patchversjon/dbVersion")

    }



    // helper methods -->

    private static File createEmptyFile(File dir = null) {
        File patchFile = File.createTempFile("patch", ".sql", dir)
        return patchFile
    }

    private static File createSimplePatchFile(File dir = null) {
        File patchFile = File.createTempFile("patch", ".sql", dir)
        patchFile.withPrintWriter {
            it.println '''

-- PATCH DB.MIN.VERSION="<any>"
-- PATCH DATA DB.VERSION="1.0" PATCH.NO="1" "Create test table"

CREATE TABLE TEST_TABLE (
   ID INTEGER NOT NULL,
   NAVN VARCHAR(32) NOT NULL,
   PRIMARY KEY (ID)
);

-- PATCH DATA DB.VERSION="1.0" PATCH.NO="3" "Inserting Chuck Norris"
INSERT INTO TEST_TABLE (ID, NAVN) VALUES (1, 'CHUCK NORRIS');

'''
            it.flush()
        }
       return patchFile
    }
}
