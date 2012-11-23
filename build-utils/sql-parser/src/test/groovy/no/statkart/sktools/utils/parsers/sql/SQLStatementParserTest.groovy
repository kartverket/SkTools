package no.statkart.sktools.utils.parsers.sql

import org.testng.annotations.Test
import no.statkart.sktools.utils.parsers.sql.model.Expression
import org.testng.Assert
import no.statkart.sktools.utils.parsers.sql.model.DefaultStatement
import no.statkart.sktools.utils.parsers.sql.model.Statement
import no.statkart.sktools.utils.parsers.sql.model.PLSQLStatement
import no.statkart.sktools.utils.parsers.sql.model.PromptStatement
import no.statkart.sktools.utils.parsers.sql.model.Comment
import no.statkart.sktools.utils.parsers.sql.model.LineComment

/**
 * Tester parsing av sql setninger ifra flatfil.
 *
 * @see SQLStatementParser
 */
class SQLStatementParserTest {

    /**
     * Tester parsing av normale SQL-setninger.
     *
     * Følgende momenter blir testet:
     * <ul>
     *     <li>terminerende tegn tas bort
     *     <li>blanke tegn blir beholdt
     *     <li>enkeltlinje setninger
     *     <li>multilinje setninger
     *     <li>flere setninger på en linje
     * </ul>
     */
    @Test
    void testParsing_SQL() {

        StringReader stringReader = new StringReader("""\
select * from dual ;
 select ident from
 test;
stmt1;  stmt2;stmt3
""")
        LineNumberReader reader = new LineNumberReader(stringReader)

        List<Expression> list = SQLStatementParser.parseExpressions(reader)
        list.each { Assert.assertTrue(it instanceof DefaultStatement, "Expected DefaultStatement instance, but was ${it.class}")}

        List<Statement> statementList = list
        int i = 0
        Assert.assertEquals(statementList[i].sql, 'select * from dual ')
        Assert.assertEquals(statementList[i].lineNumber, 1, 'linenumber')
        i++
        Assert.assertEquals(statementList[i].sql, ' select ident from\n test')
        Assert.assertEquals(statementList[i].lineNumber, 2, 'linenumber')
        i++
        Assert.assertEquals(statementList[i].sql, 'stmt1')
        Assert.assertEquals(statementList[i].lineNumber, 4, 'linenumber')
        i++
        Assert.assertEquals(statementList[i].sql, '  stmt2')
        Assert.assertEquals(statementList[i].lineNumber, 4, 'linenumber')
        i++
        Assert.assertEquals(statementList[i].sql, 'stmt3\n')
        Assert.assertEquals(statementList[i].lineNumber, 4, 'linenumber')
        i++
        Assert.assertEquals(statementList.size(), i, 'antall statements')

    }

    /**
     * Tester parsing av kommentarer.
     *
     * Kommentarer blir strippet vekk, også innline kommentarer.
     */
    @Test
    void testParsing_comments() {
        StringReader stringReader = new StringReader("""\
--slett;
select --slett;
* from dual;
""")
        LineNumberReader reader = new LineNumberReader(stringReader)

        List<? extends Expression> list = SQLStatementParser.parseExpressions(reader)
        int i = 0
        Assert.assertEquals(list[i].text.trim(), '--slett;')
        Assert.assertEquals(list[i].lineNumber, 1, 'linjenr for kommentar')
        i++
        Assert.assertEquals(list[i].sql.trim(), 'select * from dual')
        Assert.assertEquals(list[i].lineNumber, 2, 'linjenr for første statement')
        i++
        Assert.assertEquals(list.size(), i, 'antall statements')

    }

    /**
     * Tester parsing av PROMPT kommentarer
     */
    @Test
    void testParsing_Prompt() {
        StringReader stringReader = new StringReader("""\
PROMPT comment line 1

PROMPT comment line 3
""")
        LineNumberReader reader = new LineNumberReader(stringReader)

        List<Expression> list = SQLStatementParser.parseExpressions(reader)
        list.each { Assert.assertTrue(it instanceof PromptStatement, "Expected PromptStatement instance, but was ${it.class}")}

        List<PromptStatement> statementList = list
        int i = 0
        Assert.assertEquals(statementList[i].text.trim(), 'PROMPT comment line 1')
        Assert.assertEquals(statementList[i].lineNumber, 1, 'linjenr for statement')
        i++
        Assert.assertEquals(statementList[i].text.trim(), 'PROMPT comment line 3')
        Assert.assertEquals(statementList[i].lineNumber, 3, 'linjenr for statement')
        i++
        Assert.assertEquals(statementList.size(), i, 'antall statements')



    }

    /**
     * Tester parsing av PL/SQL setninger
     */
    @Test
    void testParsing_PLSQL() {
        def oracle_db_schema = 'TESTSCHEMA'
        def plsqlLoopStatement = """\
BEGIN
    FOR i IN (SELECT table_name FROM all_tables where owner = '${oracle_db_schema}')
        LOOP
            EXECUTE IMMEDIATE('DROP TABLE ${oracle_db_schema}.' || i.table_name || ' CASCADE CONSTRAINTS PURGE');
        END LOOP;
END;"""

        StringReader stringReader = new StringReader("""\
${plsqlLoopStatement}
/
create function test
...
/
CREATE OR REPLACE Function tt;
/
""")
        LineNumberReader reader = new LineNumberReader(stringReader)
        List<Expression> list = SQLStatementParser.parseExpressions(reader)
        list.each { Assert.assertTrue(it instanceof PLSQLStatement, "Expected PLSQLStatement instance, but was ${it.class}")}

        List<Statement> statementList = list
        int i = 0
        Assert.assertEquals(statementList[i].sql.trim(), plsqlLoopStatement)
        Assert.assertEquals(statementList[i].lineNumber, 1, 'linjenr for statement')
        i++
        Assert.assertEquals(statementList[i].sql.trim(), 'create function test\n...')
        Assert.assertEquals(statementList[i].lineNumber, 8, 'linjenr for statement')
        i++
        Assert.assertEquals(statementList[i].sql.trim(), 'CREATE OR REPLACE Function tt;')
        Assert.assertEquals(statementList[i].lineNumber, 11, 'linjenr for statement')
        i++
        Assert.assertEquals(statementList.size(), i, 'antall statements')

    }

    
    
    /**
     * Tester parsing av statements av ulike typer. 
     */
    @Test
    void testParsing_mixed() {

        LineNumberReader reader = new LineNumberReader(new StringReader("""\
BEGIN
    EXECUTE IMMEDIATE('select * from dual')
END
/
select column from TESTTABLE;
--kommentar som eget element som ikke er Statement
yes;
PROMPT no

select slutt
"""))
        List<Expression> list = SQLStatementParser.parseExpressions(reader)

        int i = 0
        Assert.assertTrue(list[i] instanceof PLSQLStatement, "instanceof ${PLSQLStatement.class}")
        Assert.assertTrue(((PLSQLStatement)list[i]).sql.contains("EXECUTE IMMEDIATE('select * from dual')"), 'innhold')
        i++
        Assert.assertTrue(list[i] instanceof DefaultStatement, "instanceof ${DefaultStatement.class}")
        Assert.assertEquals(((DefaultStatement)list[i]).sql.trim(), 'select column from TESTTABLE')
        i++
        Assert.assertTrue(list[i] instanceof LineComment, "instanceof ${LineComment.class}")
        Assert.assertEquals(((LineComment)list[i]).text.trim(), '--kommentar som eget element som ikke er Statement')
        i++
        Assert.assertTrue(list[i] instanceof Statement, "instanceof ${Statement.class}")
        Assert.assertEquals(((Statement)list[i]).sql.trim(), 'yes')
        i++
        Assert.assertTrue(list[i] instanceof PromptStatement, "instanceof ${PromptStatement.class}")
        Assert.assertEquals(((PromptStatement)list[i]).text.trim(), 'PROMPT no')
        i++
        Assert.assertTrue(list[i] instanceof DefaultStatement, "instanceof ${DefaultStatement.class}")
        Assert.assertEquals(((DefaultStatement)list[i]).sql.trim(), 'select slutt')
        i++
        Assert.assertEquals(list.size(), i, 'antall statements')
    }
}