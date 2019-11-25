package no.statkart.sktools.gradle.plugins.dbtools.database.util

import no.statkart.sktools.gradle.testutils.TestKitBase
import org.gradle.api.Project
import org.testng.annotations.Test

import static org.assertj.core.api.Assertions.assertThat

class SQLTaskTest extends TestKitBase {

    @Test
    public void encoding() {
        final String norskeTegn = "\u00c6\u00e6\u00d8\u00f8\u00c5\u00e5"// 'ÆæØøÅå' written with utf8 escaped values

        final String fileContents = """
-- file contents encoded in IBM 865 encoding
insert into TABLE1(ID, TEXT) VALUES (1, '${norskeTegn}');
insert into TABLE1(ID, TEXT) VALUES (2, '@CUSTOM@');
"""

        writeFile('sql/insertData.sql', new ByteArrayInputStream(fileContents.getBytes("IBM-865")))

        final Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-dbtools-plugin'
        }

        SQLTask sqlTask = project.tasks.create('sqlTask', SQLTask)
        sqlTask.setEncoding('IBM-865')
        sqlTask.setSqlFile(file('sql/insertData.sql'))
        sqlTask.parseStatements()

        assertThat(sqlTask.executor.getStatements())
            .extracting("sql")
            .containsSequence(
                'insert into TABLE1(ID, TEXT) VALUES (1, \'ÆæØøÅå\')',
                'insert into TABLE1(ID, TEXT) VALUES (2, \'@CUSTOM@\')')
    }

}
