package no.statkart.sktools.utils.databasepatcher

import groovy.sql.GroovyRowResult
import no.statkart.sktools.utils.databasepatcher.testutils.DatabasePatcherTestContext
import org.testng.annotations.Test
import org.testng.Assert
import no.statkart.sktools.gradle.plugins.dbtools.HSQLDBTest
import no.statkart.sktools.utils.parsers.sql.SQLStatementParser
import no.statkart.sktools.utils.parsers.sql.model.Expression

import static no.statkart.sktools.gradle.plugins.dbtools.testutils.DbToolsTestContext.FILE_TYPE.SQL
import no.statkart.sktools.utils.parsers.sql.model.Statement

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
        final DatabasePatcherTestContext testContext = buildDatabasePatcherTestFixture()

        File patchFile = testContext.createSimplePatchFile();

        DatabasePatcher databasePatcher = testContext.setUpDatabasePatcher();
        databasePatcher.patch(patchFile.toString());

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
        final DatabasePatcherTestContext testContext = buildDatabasePatcherTestFixture()

        File baseDir = new File(".")
        File subDir = new File(baseDir, "subdir")
        subDir.mkdir()
        File patchFile = testContext.createSimplePatchFile(subDir);

        String relativePath = "subdir/" + patchFile.getName()

        DatabasePatcher databasePatcher = testContext.setUpDatabasePatcher();
        databasePatcher.patch(relativePath);

        def row = sql.firstRow('select ID, NAVN from TEST_TABLE where ID = 1')

        Assert.assertNotNull(row, 'Forventer en rad')
        Assert.assertEquals(row.ID, 1, 'forventet ID')
        Assert.assertEquals(row.NAVN, 'CHUCK NORRIS', 'forventet NAVN')

    }


    /**
     * Verifiserer at tabell for patchdata opprettes automatisk
     */
    @Test
    public void testNoPatchinfoTable() {
        final DatabasePatcherTestContext testContext = buildDatabasePatcherTestFixture()

        File patchFile = testContext.createTempFile("");

        DatabasePatcher databasePatcher = testContext.setUpDatabasePatcher();
        databasePatcher.patch(patchFile.toString());

        try {
            def row = sql.firstRow('select * from PATCHINFO')

            Assert.assertNotNull(row, 'Forventer rad')
            Assert.assertEquals(row.dbVersion, null, "Patchversjon/dbVersion")

        } catch (Exception e) {
            Assert.fail("Forventer å finne PATCHINFO tabell", e)
        }

    }

    /**
     * Verifiserer at tabell for patchdata opprettes automatisk
     * @since 1.3
     */
    @Test
    public void testNoPatchinfoTableSystemUser() {

        def user1 = systemCredentials
        def user2 = defaultCredentials

        final DatabasePatcherTestContext testContext = buildDatabasePatcherTestFixture(systemCredentials, defaultCredentials)

        File patchFile = testContext.createTempFile("");

        DatabasePatcher databasePatcher = testContext.setUpDatabasePatcher();
        databasePatcher.patch(patchFile.toString());

        try {
            def row = getSql(defaultCredentials).firstRow('select * from PATCHINFO')

            Assert.assertNotNull(row, 'Forventer rad')
            Assert.assertEquals(row.dbVersion, null, "Patchversjon/dbVersion")
        } catch (Exception e) {
            Assert.fail("Forventer å finne PATCHINFO tabell", e)
        }
    }


    /**
     * Verifiserer patching av ulike komponenter
     */
    @Test
    public void testPatchdataForComponents() {
        final DatabasePatcherTestContext testContext = buildDatabasePatcherTestFixture()

        File patchAFile = testContext.createTempFile(SQL, '''
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

        File patchBFile = testContext.createTempFile(SQL, '''
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


        DatabasePatcher databasePatcher = testContext.setUpDatabasePatcher();

        //kjører inn patch for "default" komponent
        databasePatcher.singleStepPatches = true  //singlestep
        databasePatcher.patch(patchAFile.toString())

        Assert.assertEquals(sql.firstRow('select count(*) from PATCHINFO').getAt(0), 1, "Forventet antall rader i patchinfo")
        Assert.assertEquals(databasePatcher.getVersion().component, 'null', "forventet modul")

        def row = sql.firstRow('select * from PATCHINFO')
        Assert.assertNotNull(row, 'Forventer en rad')
        Assert.assertEquals(row.dbVersion, "1.0", "Patchversjon/dbVersion")


        //kjører inn patcher for komponent "modulB"
        databasePatcher.component = 'modulB'


        databasePatcher.singleStepPatches = true  //singlestep
        while (databasePatcher.patch(patchBFile.toString()) != 0) { //singlestepper igjennom alle patcher for "modulB"
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
        final DatabasePatcherTestContext testContext = buildDatabasePatcherTestFixture()

        File patchFile = testContext.createSimplePatchFile();

        List<? extends Expression> expressions = SQLStatementParser.parseExpressions(SqlExecutor.lesFilFraWorkingDir(patchFile.absolutePath));

        int lineNo = 0

        Assert.assertTrue(expressions.get(lineNo++).text.trim().startsWith("--kommentar"));
        Assert.assertTrue(expressions.get(lineNo++).text.trim().startsWith("-- PATCH DB.MIN.VERSION"));
        Assert.assertTrue(expressions.get(lineNo++).text.trim().startsWith("-- PATCH DATA DB.VERSION"));
        Assert.assertTrue(expressions.get(lineNo++).sql.trim().startsWith("CREATE TABLE TEST_TABLE"));


        final LinkedHashMap<DatabasePatcher.PatchVersion, List<? extends Expression>> patches = DatabasePatcher.parsePatches(expressions);

        patches.entrySet().asList().with { def entries ->
            int expressionNo = 0

            Assert.assertNotNull(entries[expressionNo].key, "Forventer at første element er minversion")
            Assert.assertEquals(entries[expressionNo].key.dbVersion, "<any>", "dbVersion")

            expressionNo++
            Assert.assertEquals(entries[expressionNo].key.patchtype.name, 'DATA', "Forventer at element er data patch")
            Assert.assertEquals(entries[expressionNo].key.dbVersion, '1.0', "Forventet dbVersion")
            Assert.assertEquals(entries[expressionNo].key.patchNo, 1, "Forventet patchNo")
            Assert.assertEquals(entries[expressionNo].key.kommentar, '"Create test table"', "Kommentar")
            Assert.assertEquals(entries[expressionNo].value.findAll {it instanceof Statement}.size(), 1, "Forventet antall statements")

            expressionNo++
            Assert.assertEquals(entries[expressionNo].key.patchtype.name, 'DATA', "Forventer at element er data patch")
            Assert.assertEquals(entries[expressionNo].key.dbVersion, '1.0', "Forventet dbVersion")
            Assert.assertEquals(entries[expressionNo].key.patchNo, 3, "Forventet patchNo")
            Assert.assertEquals(entries[expressionNo].value.findAll {it instanceof Statement}.size(), 1, "Forventet antall statements")

            Assert.assertEquals(entries.size(), 3, "Forventet antall patcher + minversion")
        }

    }


    /**
     * Verifiserer intern parse-mekanikk
     */
    @Test
    public void testParsingOfAlwaysPatchBlocks() {
        final DatabasePatcherTestContext testContext = buildDatabasePatcherTestFixture()

        File patchFile = testContext.createTempFile(SQL, """
-- PATCH DB.MIN.VERSION="1.0"

-- PATCH ALWAYS DB.VERSION="0" PATCH.NO="-1" "Definerer skjema for påfølgende patcher"
ALTER SESSION SET CURRENT_SCHEMA = "USER";


-- PATCH SCHEMA DB.VERSION="1.1" PATCH.NO="1" "Create test table"
CREATE TABLE TEST_TABLE;

-- PATCH DATA DB.VERSION="1.1" PATCH.NO="2" "Create test tables"
CREATE TABLE TEST_TABLE1;
CREATE TABLE TEST_TABLE2;

""");

        List<? extends Expression> expressions = SQLStatementParser.parseExpressions(SqlExecutor.lesFilFraWorkingDir(patchFile.absolutePath));

        final LinkedHashMap<DatabasePatcher.PatchVersion, List<? extends Expression>> patches = DatabasePatcher.parsePatches(expressions);

        patches.entrySet().asList().with { def entries ->
            int expressionNo = 0

            Assert.assertNotNull(entries[expressionNo].key, "Forventer at første element er minversion")
            Assert.assertEquals(entries[expressionNo].key.dbVersion, "1.0", "dbVersion")

            expressionNo++
            Assert.assertEquals(entries[expressionNo].key.patchtype.name, 'ALWAYS', "Forventer at element er data patch")
            Assert.assertEquals(entries[expressionNo].key.dbVersion, '0', "Forventet dbVersion")
            Assert.assertEquals(entries[expressionNo].key.patchNo, -1, "Forventet patchNo")
            Assert.assertEquals(entries[expressionNo].key.kommentar, '"Definerer skjema for påfølgende patcher"', "Kommentar")
            Assert.assertEquals(entries[expressionNo].value.findAll {it instanceof Statement}.size(), 1, "Forventet antall statements")

            expressionNo++
            Assert.assertEquals(entries[expressionNo].key.patchtype.name, 'SCHEMA', "Forventer at element er data patch")
            Assert.assertEquals(entries[expressionNo].key.dbVersion, '1.1', "Forventet dbVersion")
            Assert.assertEquals(entries[expressionNo].key.patchNo, 1, "Forventet patchNo")
            Assert.assertEquals(entries[expressionNo].value.findAll {it instanceof Statement}.size(), 1, "Forventet antall statements")

            expressionNo++
            Assert.assertEquals(entries[expressionNo].key.patchtype.name, 'DATA', "Forventer at element er data patch")
            Assert.assertEquals(entries[expressionNo].key.dbVersion, '1.1', "Forventet dbVersion")
            Assert.assertEquals(entries[expressionNo].key.patchNo, 2, "Forventet patchNo")
            Assert.assertEquals(entries[expressionNo].value.findAll {it instanceof Statement}.size(), 2, "Forventet antall statements")

            Assert.assertEquals(entries.size(), 4, "Forventet antall patcher + minversion")
        }

    }

    /**
     * Verifiserer at indexer blir kjørt inn igjen uavhengig av {@link DatabasePatcher.PatchInfo#indexesInSyncWithPatch}
     *
     * SKTOOLS-112: Endrer betydning av dette flagget til å overse dette. Dette endrer da den opprinnelige virkemåten
     * i implementasjon ifra matrikkelen.
     */
    @Test
    public void testIndexesInSyncWithPatch() {
        final DatabasePatcherTestContext testContext = buildDatabasePatcherTestFixture()

        File oldPatchFile = testContext.createTempFile("""-- patchefil uten patch#2
-- PATCH DB.MIN.VERSION="<any>"
${testContext.PATCH_01}
${testContext.PATCH_03}
""");

        DatabasePatcher databasePatcher = testContext.setUpDatabasePatcher();

        //STEG: Patcher opp basen med oldPatchFile
        databasePatcher.patch(oldPatchFile.toString());

        Assert.assertEquals(sql.firstRow('''select count(*) from TEST_TABLE''')[0], 1, 'Forventer en rad')
        assertPatchInfoRow(sql.firstRow('select * from PATCHINFO'), '1.0', 3, true, "PatchInfo")

        //STEG: Patcher opp basen igjen med oldPatchFile - forventer uforandret resultat
        databasePatcher.syncPatch(oldPatchFile.toString(), Collections.singleton(PatchtypeKode.INDEX));

        Assert.assertEquals(sql.firstRow('''select count(*) from TEST_TABLE''')[0], 1, 'Forventet #rader')
        assertPatchInfoRow(sql.firstRow('select * from PATCHINFO'), '1.0', 3, true, "uforandret patchinfo")


        File updatedPatchFileWithIndex = testContext.createTempFile("""-- patchefil med patch#2 - patchtype: INDEX
-- PATCH DB.MIN.VERSION="<any>"
${testContext.PATCH_01}
${testContext.PATCH_02}
${testContext.PATCH_03}
""");

        //STEG: Patcher opp databasen med oppdatert patchfil - forventer ikke endringer da databasen allerede har siste patch# og indexesInSyncWithPatch
        databasePatcher.patch(updatedPatchFileWithIndex.toString());

        Assert.assertEquals(sql.firstRow('''select count(*) from TEST_TABLE''')[0], 1, 'Forventet #rader')
        assertPatchInfoRow(sql.firstRow('select * from PATCHINFO'), '1.0', 3, true, "uforandret patchinfo da basen inneholder siste patch...")



        //STEG: repatch
        // - Forventer ny rad ved syncPatch
        databasePatcher.syncPatch(updatedPatchFileWithIndex.toString(), Collections.singleton(PatchtypeKode.INDEX));

        Assert.assertEquals(sql.firstRow('''select count(*) from TEST_TABLE''')[0], 2, 'Forventet #rader')
        assertPatchInfoRow(sql.firstRow('select * from PATCHINFO'), '1.0', 3, true, "uforandret patchinfo")


        //STEG: repatch med indexesInSyncWithPatch = false
        // - Forventer ny rad ved syncPatch
        databasePatcher.setIndexesInSyncWithPatch(false);
        Assert.assertEquals(sql.firstRow('select * from PATCHINFO').indexesInSyncWithPatch, 0, "indexesInSyncWithPatch")

        databasePatcher.syncPatch(updatedPatchFileWithIndex.toString(), Collections.singleton(PatchtypeKode.INDEX));

        Assert.assertEquals(sql.firstRow('select * from PATCHINFO').indexesInSyncWithPatch, 1, "indexesInSyncWithPatch")
        Assert.assertEquals(sql.firstRow('''select count(*) from TEST_TABLE''')[0], 3, 'Forventet #rader')

    }

    /**
     * Verifiserer at patchblokker av type {@link PatchtypeKode#ALWAYS} alltid blir kjørt.
     */
    @Test
    public void testAlwaysPatchblokkPatching() {
        final DatabasePatcherTestContext testContext = buildDatabasePatcherTestFixture()

        final def PATCH2 = """
-- PATCH ALWAYS DB.VERSION="1.0" PATCH.NO="2" "Inserting random rows"
INSERT INTO TEST_TABLE (ID, NAVN) VALUES (1, 'GENERATED INDEX patch: ' || TO_CHAR(CURRENT_TIMESTAMP, 'MI:SS:FF'));
"""

        File patchFile = testContext.createTempFile("""-- patchefil
-- PATCH DB.MIN.VERSION="<any>"
${testContext.PATCH_01}
${PATCH2}
${testContext.PATCH_03}
""");

        final DatabasePatcher databasePatcher = testContext.setUpDatabasePatcher();

        //STEG: Patcher opp basen med patchFile
        databasePatcher.patch(patchFile.toString());
        sql.firstRow('''select count(*) from TEST_TABLE''').with { def row ->
            Assert.assertEquals(row[0], 2, 'Forventet antall rader')
        }

        assertPatchInfoRow(sql.firstRow('select * from PATCHINFO'), '1.0', 3, true, "initiell patchinfo")


        //STEG: Patcher opp basen igjen - forventer da at kun patch#2 blir kjørt  da denne er tagget som ALWAYS
        databasePatcher.patch(patchFile.toString());
        sql.firstRow('''select count(*) from TEST_TABLE''').with { def row ->
            Assert.assertEquals(row[0], 3, 'Forventet antall rader')
        }

        assertPatchInfoRow(sql.firstRow('select * from PATCHINFO'), '1.0', 3, true, "uforandret patchinfo")
    }


    /**
     * SKTOOLS-114: PatchInfo skal oppdateres for ALWAYS når disse er nye
     */
    @Test
    public void testAlwaysPatchblokkPatchingUpdatedPatchInfo() {
        final DatabasePatcherTestContext testContext = buildDatabasePatcherTestFixture()

        final def PATCH2 = """
-- PATCH DATA DB.VERSION="1.0" PATCH.NO="2" "Inserting random rows"
INSERT INTO TEST_TABLE (ID, NAVN) VALUES (2, 'GENERATED ALWAYS patch: ' || TO_CHAR(CURRENT_TIMESTAMP, 'MI:SS:FF'));
"""
        final def PATCH3 = """
-- PATCH ALWAYS DB.VERSION="1.0" PATCH.NO="3" "Inserting random rows"
INSERT INTO TEST_TABLE (ID, NAVN) VALUES (3, 'GENERATED ALWAYS patch: ' || TO_CHAR(CURRENT_TIMESTAMP, 'MI:SS:FF'));
"""

        File patchFile1 = testContext.createTempFile("""-- patchefil frem til patch#2
-- PATCH DB.MIN.VERSION="<any>"
${testContext.PATCH_01}
${PATCH2}
""");

        File patchFile2 = testContext.createTempFile("""-- patchefil frem til patch#3
-- PATCH DB.MIN.VERSION="<any>"
${testContext.PATCH_01}
${PATCH2}
${PATCH3}
""");

        final DatabasePatcher databasePatcher = testContext.setUpDatabasePatcher();

        //STEG: Patcher opp basen med patchFile1
        databasePatcher.patch(patchFile1.toString());
        sql.firstRow('''select count(*) from TEST_TABLE''').with { def row ->
            Assert.assertEquals(row[0], 1, 'Forventet antall rader')
        }

        assertPatchInfoRow(sql.firstRow('select * from PATCHINFO'), '1.0', 2, true, "initiell patchinfo")


        //STEG: Patcher opp basen med patchFile2
        databasePatcher.patch(patchFile2.toString());
        sql.firstRow('''select count(*) from TEST_TABLE''').with { def row ->
            Assert.assertEquals(row[0], 2, 'Forventet antall rader')
        }

        assertPatchInfoRow(sql.firstRow('select * from PATCHINFO'), '1.0', 3, true, "oppdatert patchinfo")


        //STEG: RE-Patcher basen med patchFile2
        databasePatcher.patch(patchFile2.toString());
        sql.firstRow('''select count(*) from TEST_TABLE''').with { def row ->
            Assert.assertEquals(row[0], 3, 'Forventet antall rader')
        }

        assertPatchInfoRow(sql.firstRow('select * from PATCHINFO'), '1.0', 3, true, "samme oppdaterte patchinfo")

    }


    static void assertPatchInfoRow(GroovyRowResult row, String dbVersion, int patchNo, Boolean indexesInSyncWithPatch = null, String message) {
        Assert.assertNotNull(row, "${message}: Forventet en rad")
        Assert.assertEquals(row.dbVersion, dbVersion, "${message}: Patchversjon/dbVersion")
        Assert.assertEquals(row.patchNo, patchNo, "${message}: patchNo")
        if (indexesInSyncWithPatch != null) {
            Assert.assertEquals(row.indexesInSyncWithPatch, indexesInSyncWithPatch ? 1 : 0, "${message}: indexesInSyncWithPatch")
        }
    }

}