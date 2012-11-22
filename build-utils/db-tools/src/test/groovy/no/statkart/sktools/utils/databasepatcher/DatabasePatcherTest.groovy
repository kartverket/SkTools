package no.statkart.sktools.utils.databasepatcher

import org.testng.annotations.Test
import junit.framework.Assert

/**
 * Tester funksjonaliteten til {@link DatabasePatcher}
 */
class DatabasePatcherTest {

    @Test
    public void testParsing() {

        List<SqlExecutor.ScriptLine> lines = SqlExecutor.parseSQL('''--kommentar
-- Angir minste db versjon som denne endringsfil klarer å oppgradere. <any> angir alle databaser versjoner
-- PATCH DB.MIN.VERSION="<any>"

-- PATCH DATA DB.VERSION="1.2" PATCH.NO="6" "Legger språkform på kommune og setter den til bokmål"
alter table kommune add (sprakform number(5,0));
update kommune set sprakform=1;


--  PATCH INDEX DB.VERSION="1.2" PATCH.NO="14" "Legger til entydig indeks på løpenr på forretning"
create unique index Forretning_lopenr on Forretning(lopenr asc) tablespace @matrikkel.db.index.tablespace@;

''');

        Assert.assertTrue(lines.get(0).line.trim().startsWith("--kommentar"));
        Assert.assertTrue(lines.get(1).line.trim().startsWith("-- Angir"));
        Assert.assertTrue(lines.get(2).line.trim().startsWith("-- PATCH"));

    }

}
