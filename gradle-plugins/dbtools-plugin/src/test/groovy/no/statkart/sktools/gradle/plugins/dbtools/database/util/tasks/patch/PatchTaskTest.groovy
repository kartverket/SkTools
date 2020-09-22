package no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.patch

import no.statkart.sktools.gradle.testutils.TestKitBase
import org.gradle.api.Project
import org.testng.annotations.Test

import static org.assertj.core.api.Assertions.assertThat

class PatchTaskTest extends TestKitBase {

    @Test
    public void encoding() {
        final String norskeTegn = "\u00c6\u00e6\u00d8\u00f8\u00c5\u00e5"// 'ÆæØøÅå' with utf8 escaped values

        final String fileContents = """
-- file contents encoded in IBM 865 encoding
insert into TABLE1(ID, TEXT) VALUES (1, '${norskeTegn}');
"""

        writeFile('sql/insertData.sql', new ByteArrayInputStream(fileContents.getBytes("IBM-865")))

        Project project = projectBuilder().build()
        PatchTask patchTask = project.tasks.create('patchTask', PatchTask)
        patchTask.setEncoding('IBM-865')

        assertThat(patchTask.mappedSqlFile(file('sql/insertData.sql')))
            .usingCharset('IBM-865')
            .hasContent("""
-- file contents encoded in IBM 865 encoding
insert into TABLE1(ID, TEXT) VALUES (1, 'ÆæØøÅå');
""")
    }


    @Test
    public void propertySubstitution() {
        Project project = projectBuilder().build()
        PatchTask patchTask = project.tasks.create('patchTask', PatchTask)

        patchTask.ext.setProperty('CUSTOM1', 'foo')
        assertThat(patchTask.fillInnProperties(["'@CUSTOM1@'", "'@CUSTOM1@ bar'"]))
            .containsExactly("'foo'", "'foo bar'")
    }

    @Test
    public void toolsetPropertiesPropagatesToSubstitution() {
        Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-dbtools-plugin'

            configureDatabasePlugin {
                toolset(name: 'coolDb', type: 'hsqldb', prefix: '') {
                    patch {
                        patchTask('Foo', description: 'Task med verdier ifra konfigurasjon og convention')
                    }
                    properties = [propertyA: 'foo']
                }
            }
        }

        PatchTask patchTask = project.tasks.getByName('patchFoo') as PatchTask
        assertThat(patchTask.fillInnProperties(["@propertyA@ @propertyA@"]))
            .containsExactly("foo foo")

        patchTask.ext.propertyA = "bar"
        assertThat(patchTask.fillInnProperties(["@propertyA@ @propertyA@"]))
            .containsExactly("bar bar")
    }
}
