package no.statkart.sktools.utils.databasepatcher

import org.testng.annotations.Test
import org.testng.Assert
import no.statkart.sktools.gradle.plugins.dbtools.HSQLDBTest
import no.statkart.sktools.utils.parsers.sql.SQLStatementParser
import no.statkart.sktools.utils.parsers.sql.model.Expression

import static no.statkart.sktools.gradle.plugins.dbtools.testutils.DbToolsTestCase.FILE_TYPE.SQL

/**
 * Tester funksjonaliteten til {@link no.statkart.sktools.utils.databasepatcher.DatabasePatcher}
 *
 * NB: Denne testen tilhører db-tools men er lagt her for enkelhetens skyld (alternativet er å opprette en egen test-modul)
 */
class DatabasePatcherTest extends HSQLDBTest {


    /**
     * Verifiserer at man kan angi absolutt filsti for "patch.sql"
     */
    @Test
    public void testAbsoluteFileName() {
        final DatabasePatcherTestCase testCase = buildDatabasePatcherTestCase()

        File patchFile = testCase.createSimplePatchFile();

        DatabasePatcher databasePatcher = testCase.setUpDatabasePatcher();
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
        final DatabasePatcherTestCase testCase = buildDatabasePatcherTestCase()

        File baseDir = new File(".")
        File subDir = new File(baseDir, "subdir")
        subDir.mkdir()
        File patchFile = testCase.createSimplePatchFile(subDir);

        String relativePath = "subdir/" + patchFile.getName()

        DatabasePatcher databasePatcher = testCase.setUpDatabasePatcher();
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
        final DatabasePatcherTestCase testCase = buildDatabasePatcherTestCase()

        File patchFile = testCase.createTempFile("");

        DatabasePatcher databasePatcher = testCase.setUpDatabasePatcher();
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
        final DatabasePatcherTestCase testCase = buildDatabasePatcherTestCase()

        File patchAFile = testCase.createTempFile(SQL, '''
-- patch for default modul. Testen kjører kun første definerte patch.

-- PATCH DB.MIN.VERSION="<any>"
-- PATCH DATA DB.VERSION="1.0" PATCH.NO="1" "Create Atable"

CREATE TABLE A_TABLE (
   ID INTEGER NOT NULL,
   NAVN VARCHAR(32) NOT NULL,
   PRIMARY KEY (ID)
);

-- PATCH DATA DB.VERSION="1.0" PATCH.NO="3" "Inserting valueA"
INSERT INTO A_TABLE (ID, NAVN) VALUES (1, 'valueA');

        ''')

        File patchBFile = testCase.createTempFile(SQL, '''
-- patch for 'modulB'

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

        ''')


        DatabasePatcher databasePatcher = testCase.setUpDatabasePatcher();

        //kjører inn patch for "default" komponent
        databasePatcher.patch(patchAFile.toString(), true) //singlestep

        Assert.assertEquals(sql.firstRow('select count(*) from PATCHINFO').getAt(0), 1, "Forventet antall rader i patchinfo")
        Assert.assertEquals(databasePatcher.getVersion().component, 'null', "forventet modul")

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

        Assert.assertEquals(databasePatcher.getVersion().component, 'modulB', "forventet modul")
        Assert.assertEquals(databasePatcher.getVersion().patchVersion.dbVersion, '0.2', "forventet patchversjon/dbVersion")
        Assert.assertEquals(databasePatcher.getVersion().patchVersion.patchNo, 4, "forventet patchnummer")

    }


    /**
     * Verifiserer intern parse-mekanikk
     */
    @Test
    public void testParsing() {
        final DatabasePatcherTestCase testCase = buildDatabasePatcherTestCase()

        File patchFile = testCase.createSimplePatchFile();

        List<? extends Expression> expressions = SQLStatementParser.parseExpressions(SqlExecutor.lesFilFraWorkingDir(patchFile.absolutePath));


        int lineno

        Assert.assertTrue(expressions.get(lineno++).text.trim().startsWith("--kommentar"));
        Assert.assertTrue(expressions.get(lineno++).text.trim().startsWith("-- PATCH DB.MIN.VERSION"));
        Assert.assertTrue(expressions.get(lineno++).text.trim().startsWith("-- PATCH DATA DB.VERSION"));
        Assert.assertTrue(expressions.get(lineno++).sql.trim().startsWith("CREATE TABLE TEST_TABLE"));

    }



    /**
     * Verifiserer at indexer blir kjørt inn igjen dersom ikke {@link DatabasePatcher.PatchInfo#indexesInSyncWithPatch}
     */
    @Test
    public void testIndexesInSynchWithPatch() {
        final DatabasePatcherTestCase testCase = buildDatabasePatcherTestCase()

        File oldPatchFile = testCase.createTempFile("""-- patchefil uten patch#2
-- PATCH DB.MIN.VERSION="<any>"
${testCase.PATCH_01}
${testCase.PATCH_03}
""");

        DatabasePatcher databasePatcher = testCase.setUpDatabasePatcher();

        //STEG: Patcher opp basen med oldPatchFile
        databasePatcher.patch(oldPatchFile.toString(), false);
        sql.firstRow('''select count(*) from TEST_TABLE''').with { def row ->
            Assert.assertEquals(row[0], 1, 'Forventer en rad')
        }

        sql.firstRow('select * from PATCHINFO').with { def row ->
            Assert.assertNotNull(row, 'Forventer rad')
            Assert.assertEquals(row.dbVersion, '1.0', "Patchversjon/dbVersion")
            Assert.assertEquals(row.patchNo, 3, "patchNo")
            Assert.assertEquals(row.indexesInSyncWithPatch, 1, "indexesInSyncWithPatch")
        }

        //STEG: Patcher opp basen igjen med oldPatchFile - forventer uforandret resultat
        databasePatcher.patch(oldPatchFile.toString(), false);
        sql.firstRow('''select count(*) from TEST_TABLE''').with { def row ->
            Assert.assertEquals(row[0], 1, 'Forventer en rad')
        }


        File updatedPatchFileWithIndex = testCase.createTempFile("""-- patchefil med patch#2 - patchtype: INDEX
-- PATCH DB.MIN.VERSION="<any>"
${testCase.PATCH_01}
${testCase.PATCH_02}
${testCase.PATCH_03}
""");

        //STEG: Patcher opp databasen med oppdatert patchfil - forventer ikke endringer da databasen allerede har siste patch# og indexesInSyncWithPatch
        databasePatcher.patch(updatedPatchFileWithIndex.toString(), false);
        sql.firstRow('''select count(*) from TEST_TABLE''').with { def row ->
            Assert.assertEquals(row[0], 1, 'Forventer fortsatt en rad da indexesInSyncWithPatch')
        }

        sql.firstRow('select * from PATCHINFO').with { def row ->
            Assert.assertNotNull(row, 'Forventer rad')
            Assert.assertEquals(row.dbVersion, '1.0', "Patchversjon/dbVersion")
            Assert.assertEquals(row.patchNo, 3, "patchNo")
            Assert.assertEquals(row.indexesInSyncWithPatch, 1, "indexesInSyncWithPatch")
        }



        //STEG: Setter indexesInSyncWithPatch = false og patcher opp databasen
        databasePatcher.setIndexesInSyncWithPatch(false);

        sql.firstRow('select * from PATCHINFO').with { def row ->
            Assert.assertEquals(row.indexesInSyncWithPatch, 0, "indexesInSyncWithPatch")
        }

        databasePatcher.patch(updatedPatchFileWithIndex.toString(), false);

        sql.firstRow('''select count(*) from TEST_TABLE''').with { def row ->
            Assert.assertEquals(row[0], 2, 'Forventer fortsatt en rad da indexesInSyncWithPatch')
        }
        sql.firstRow('select * from PATCHINFO').with { def row ->
            Assert.assertEquals(row.indexesInSyncWithPatch, 1, "indexesInSyncWithPatch")
        }

        
        
        //STEG: patcher opp databasen igjen - forventer ingen endringer
        databasePatcher.patch(updatedPatchFileWithIndex.toString(), false);

        sql.firstRow('''select count(*) from TEST_TABLE''').with { def row ->
            Assert.assertEquals(row[0], 2, 'Forventer fortsatt en rad da indexesInSyncWithPatch')
        }
        sql.firstRow('select * from PATCHINFO').with { def row ->
            Assert.assertEquals(row.indexesInSyncWithPatch, 1, "indexesInSyncWithPatch")
        }
    }

}
