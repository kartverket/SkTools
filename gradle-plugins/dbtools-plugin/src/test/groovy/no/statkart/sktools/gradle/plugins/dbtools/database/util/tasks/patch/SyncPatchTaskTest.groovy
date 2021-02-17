package no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.patch

import no.statkart.sktools.gradle.testutils.TestKitBase
import org.gradle.api.Project
import org.testng.annotations.Test

import static org.assertj.core.api.Assertions.assertThat

class SyncPatchTaskTest extends TestKitBase {

    @Test
    void propertySubstitution() {
        Project project = projectBuilder().build()
        SyncPatchTask task = project.tasks.create('task', SyncPatchTask)

        task.ext.setProperty('CUSTOM1', 'foo')
        assertThat(task.fillInnProperties(["'@CUSTOM1@'", "'@CUSTOM1@ bar'"]))
            .containsExactly("'foo'", "'foo bar'")
    }

}
