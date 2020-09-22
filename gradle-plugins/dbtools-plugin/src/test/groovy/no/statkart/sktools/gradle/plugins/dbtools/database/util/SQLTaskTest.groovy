package no.statkart.sktools.gradle.plugins.dbtools.database.util

import no.statkart.sktools.gradle.testutils.TestKitBase
import org.gradle.api.Project
import org.testng.annotations.Test

import static org.assertj.core.api.Assertions.assertThat

class SQLTaskTest extends TestKitBase {

    @Test
    public void encoding() {
        final String norskeTegn = "\u00c6\u00e6\u00d8\u00f8\u00c5\u00e5"// 'ÆæØøÅå' with utf8 escaped values

        final String fileContents = """
-- file contents encoded in IBM 865 encoding
insert into TABLE1(ID, TEXT) VALUES (1, '${norskeTegn}');
"""

        writeFile('sql/insertData.sql', new ByteArrayInputStream(fileContents.getBytes("IBM-865")))

        Project project = projectBuilder().build()

        SQLTask sqlTask = project.tasks.create('sqlTask', SQLTask)
        sqlTask.setEncoding('IBM-865')
        sqlTask.setSqlFile(file('sql/insertData.sql'))
        sqlTask.parseStatements()

        assertThat(sqlTask.executor.getStatements())
            .extracting("sql")
            .contains('insert into TABLE1(ID, TEXT) VALUES (1, \'ÆæØøÅå\')')
    }

    @Test
    public void propertySubstitution() {
        Project project = projectBuilder().build()
        SQLTask sqlTask = project.tasks.create('sqlTask', SQLTask)

        assertThat(sqlTask.fillInnProperties("@CUSTOM1@ @CUSTOM2@"))
            .isEqualTo("@CUSTOM1@ @CUSTOM2@")

        sqlTask.ext.setProperty('CUSTOM1', 'foo')
        sqlTask.ext.setProperty('CUSTOM2', 'bar')
        assertThat(sqlTask.fillInnProperties("@CUSTOM1@ @CUSTOM2@"))
            .isEqualTo("foo bar")
    }

    @Test
    public void toolsetPropertiesPropagatesToSubstitution() {
        Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-dbtools-plugin'

            configureDatabasePlugin {
                toolset(name: 'coolDb', type: 'hsqldb', prefix: 'coolDb') {
                    sqlTask('Foo', description: 'Sql med filtrerte verdier')
                    properties = [
                        propertyA: 'foo',
                        propertyB: 'bar',
                    ]
                }
            }
        }

        SQLTask sqlTask = project.tasks.getByName('coolDbFoo') as SQLTask

        assertThat(sqlTask.fillInnProperties("@propertyA@ @propertyB@"))
            .as("Inherited properties from toolset")
            .isEqualTo("foo bar")

        sqlTask.ext.setProperty('propertyB', 'fighter')
        assertThat(sqlTask.fillInnProperties("@propertyA@ @propertyB@"))
            .as("Task properties takes precedence")
            .isEqualTo("foo fighter")
    }

}
