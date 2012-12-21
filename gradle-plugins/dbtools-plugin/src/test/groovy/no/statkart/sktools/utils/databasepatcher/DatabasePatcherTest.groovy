package no.statkart.sktools.utils.databasepatcher

import org.testng.annotations.Test
import org.testng.Assert
import no.statkart.sktools.gradle.plugins.dbtools.HSQLDBTest
import no.statkart.sktools.utils.parsers.sql.SQLStatementParser
import no.statkart.sktools.utils.parsers.sql.model.Expression

/**
 * Tester funksjonaliteten til {@link no.statkart.sktools.utils.databasepatcher.DatabasePatcher}
 *
 * NB: Denne testen tilhører db-tools men er lagt her for enkelhetens skyld (alternativet er å opprette en egen test-modul)
 */
class DatabasePatcherTest extends HSQLDBTest {

    private DatabasePatcher setUpDatabasePatcher(String component = null) {
        DatabasePatcher databasePatcher = new DatabasePatcher()

        System.setProperty("hibernate.connection.driver_class", jdbcDriverClassString)
        System.setProperty("hibernate.connection.url", sql.connection.metaData.URL)
        System.setProperty("hibernate.connection.username", username)
        System.setProperty("hibernate.connection.password", password)

        if (component != null) {
            databasePatcher.component = component
        }

        return databasePatcher
    }

    /**
     * Verifiserer at man kan angi absolutt filsti for "patch.sql"
     */
    @Test
    public void testAbsoluteFileName() {
        File patchFile = createSimplePatchFile();

        DatabasePatcher databasePatcher = setUpDatabasePatcher();
        databasePatcher.patch(patchFile.toString(), false);

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

        DatabasePatcher databasePatcher = setUpDatabasePatcher();
        databasePatcher.patch(relativePath, false);

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

        DatabasePatcher databasePatcher = setUpDatabasePatcher();
        databasePatcher.patch(patchFile.toString(), false);

        def row = sql.firstRow('select * from PATCHINFO')

        Assert.assertNotNull(row, 'Forventer rad')
        Assert.assertEquals(row.dbVersion, null, "Patchversjon/dbVersion")

    }


    /**
     * Verifiserer patching av ulike komponenter
     */
    @Test
    public void testPatchdataForComponents() {
        File patchAFile = createAPatchFile()
        File patchBFile = createBPatchFile()

        DatabasePatcher databasePatcher = setUpDatabasePatcher();

        //kjører inn patch for "default" komponent
        databasePatcher.patch(patchAFile.toString(), true) //singlestep

        Assert.assertEquals(sql.firstRow('select count(*) from PATCHINFO').getAt(0), 1, "Forventet antall rader i patchinfo")

        def row = sql.firstRow('select * from PATCHINFO')
        Assert.assertNotNull(row, 'Forventer en rad')
        Assert.assertEquals(row.dbVersion, "1.0", "Patchversjon/dbVersion")


        //kjører inn patcher for komponent "modulB"
        databasePatcher.component = 'modulB'


        while (databasePatcher.patch(patchBFile.toString(), true) != 0) { //singlestepper igjennom alle patcher for "modulB"
            Assert.assertEquals(sql.firstRow('select count(*) from PATCHINFO').getAt(0), 2, "Forventet antall rader i patchinfo")

            def result = sql.firstRow('select * from PATCHINFO where component=?', 'modulB')
            Assert.assertNotNull(result, 'Forventer en rad')
            Assert.assertEquals(result.dbVersion, "0.2", "Patchversjon/dbVersion")

        }

        Assert.assertEquals(databasePatcher.getVersion().patchVersion.dbVersion, '0.2', "forventet patchversjon/dbVersion")
        Assert.assertEquals(databasePatcher.getVersion().patchVersion.patchNo, 4, "forventet patchnummer")

    }


    /**
     * Verifiserer intern parse-mekanikk
     */
    @Test
    public void testParsing() {
        File patchFile = createSimplePatchFile();

        List<? extends Expression> expressions = SQLStatementParser.parseExpressions(SqlExecutor.lesFilFraWorkingDir(patchFile.absolutePath));


        int lineno

        Assert.assertTrue(expressions.get(lineno++).text.trim().startsWith("--kommentar"));
        Assert.assertTrue(expressions.get(lineno++).text.trim().startsWith("-- PATCH DB.MIN.VERSION"));
        Assert.assertTrue(expressions.get(lineno++).text.trim().startsWith("-- PATCH DATA DB.VERSION"));
        Assert.assertTrue(expressions.get(lineno++).sql.trim().startsWith("CREATE TABLE TEST_TABLE"));

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

--kommentar

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

    /**
     * Patch som inneholder patcher for tabell "A_TABLE".
     * <p>
     * Patchnummer:
     * <ul>
     *     <li> 1.0 patch# 1
     *     <li> 1.0 patch# 3
     * </ul>
     *
     */
    private static File createAPatchFile(File dir = null) {
        File patchFile = File.createTempFile("patchA", ".sql", dir)
        patchFile.withPrintWriter {
            it.println '''

--kommentar

-- PATCH DB.MIN.VERSION="<any>"
-- PATCH DATA DB.VERSION="1.0" PATCH.NO="1" "Create Atable"

CREATE TABLE A_TABLE (
   ID INTEGER NOT NULL,
   NAVN VARCHAR(32) NOT NULL,
   PRIMARY KEY (ID)
);

-- PATCH DATA DB.VERSION="1.0" PATCH.NO="3" "Inserting valueA"
INSERT INTO A_TABLE (ID, NAVN) VALUES (1, 'valueA');

'''
            it.flush()
        }
        return patchFile
    }


    /**
     * Patch som inneholder patcher for tabell "B_TABLE".
     * <p>
     * Patchnummer:
     * <ul>
     *     <li> 0.2 patch# 1
     *     <li> 0.2 patch# 3
     *     <li> 0.2 patch# 4
     * </ul>
     *
     */
    private static File createBPatchFile(File dir = null) {
        File patchFile = File.createTempFile("patchB", ".sql", dir)
        patchFile.withPrintWriter {
            it.println '''

--kommentar

-- PATCH DB.MIN.VERSION="<any>"
-- PATCH DATA DB.VERSION="0.2" PATCH.NO="1" "Create Btable"

CREATE TABLE B_TABLE (
   ID INTEGER NOT NULL,
   NAVN VARCHAR(32) NOT NULL,
   PRIMARY KEY (ID)
);

-- PATCH DATA DB.VERSION="0.2" PATCH.NO="3" "Inserting valueA"
INSERT INTO B_TABLE (ID, NAVN) VALUES (1, 'valueA');

-- PATCH DATA DB.VERSION="0.2" PATCH.NO="4" "Inserting valueB"
INSERT INTO B_TABLE (ID, NAVN) VALUES (2, 'valueB');

'''
            it.flush()
        }
        return patchFile
    }

}
