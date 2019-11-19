package no.statkart.sktools.gradle.plugins.dbtools

import no.statkart.sktools.gradle.plugins.dbtools.database.DbtoolsConvention
import no.statkart.sktools.gradle.plugins.dbtools.database.util.AbstractDatabaseConvention
import no.statkart.sktools.gradle.plugins.dbtools.database.util.PatchConfiguration
import no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.SequenceTask
import no.statkart.sktools.gradle.testutils.TestKitBase
import org.gradle.api.Project
import org.testng.annotations.Test

import static org.testng.Assert.assertNotNull

/**
 * SKTOOLS-99: tester bruk av {@code taskSequence ( .. )} og {@link SequenceTask}
 *
 * @since 1.3 - ny grunnbok sprint 30
 * @author Leif Lislegård
 */
class TestSKTOOLS_99 extends TestKitBase {

    /**
     * Tester bruk av
     * <ul>
     *     <li>{@link DbtoolsConvention#taskSequence(String, Closure)}
     *     <li>{@link DbtoolsConvention#taskSequence(Map, String, Closure)}
     * </ul>
     *
     * @since 1.3 - SKTOOLS-XX
     */
    @Test
    void testTaskSequenceOnProject() {
        final Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-dbtools-plugin'

            taskSequence('ProjectTaskA') {
                dependsOn taskSequence('ProjectTaskAA') {
                }
            }

            taskSequence('ProjectTaskB', description: 'Task defined on project') {
                dependsOn taskSequence('ProjectTaskBB', description: 'Task defined on project') {
                }
            }

            taskSequence('ProjectTaskC') {
                dependsOn taskSequence('ProjectTaskCC')
            }
        }

        assertNotNull(project.tasks.findByName('ProjectTaskA'), 'Task definert av taskSequence(<name>, <closure>)')
        assertNotNull(project.tasks.findByName('ProjectTaskAA'), 'SubTask definert av taskSequence(<name>, <closure>)')

        assertNotNull(project.tasks.findByName('ProjectTaskB'), 'Task definert av taskSequence(<name>, <params>, <closure>)')
        assertNotNull(project.tasks.findByName('ProjectTaskBB'), 'SubTask definert av taskSequence(<name>, <params>, <closure>)')

        assertNotNull(project.tasks.findByName('ProjectTaskCC'), 'SubTask definert av taskSequence(<name>)')
    }

    /**
     * Tester bruk av
     * <ul>
     *     <li>{@link AbstractDatabaseConvention#taskSequence(String, Closure)}
     *     <li>{@link AbstractDatabaseConvention#taskSequence(Map, String, Closure)}
     * </ul>
     *
     * @since 1.3 - SKTOOLS-XX
     */
    @Test
    void testTaskSequenceOnDatabaseConvention() {
        final Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-dbtools-plugin'

            configureDatabasePlugin {
                toolset(name: 'testToolset', type: 'hsqldb', prefix: 'test') {

                    taskSequence('ToolsetTaskB', description: 'Task defined on toolset') {
                        dependsOn taskSequence('ToolsetTaskBB', description: 'Task defined on toolset') {
                        }
                    }
                    taskSequence('ToolsetTaskA') {
                        dependsOn taskSequence('ToolsetTaskAA') {
                        }
                    }

                    taskSequence('ToolsetTaskC') {
                        dependsOn taskSequence('ToolsetTaskCC')
                    }

                }
            }
        }

        assertNotNull(project.tasks.findByName('testToolsetTaskA'), 'Task definert av taskSequence(<name>, <closure>)')
        assertNotNull(project.tasks.findByName('testToolsetTaskAA'), 'SubTask definert av taskSequence(<name>, <closure>)')

        assertNotNull(project.tasks.findByName('testToolsetTaskB'), 'Task definert av taskSequence(<name>, <params>, <closure>)')
        assertNotNull(project.tasks.findByName('testToolsetTaskBB'), 'SubTask definert av taskSequence(<name>, <params>, <closure>)')

        assertNotNull(project.tasks.findByName('testToolsetTaskCC'), 'SubTask definert av taskSequence(<name>)')
    }

    /**
     * Tester bruk av
     * <ul>
     *     <li>{@link PatchConfiguration#taskSequence(String, Closure)}
     *     <li>{@link PatchConfiguration#taskSequence(Map, String, Closure)}
     * </ul>
     *
     * @since 1.3 - SKTOOLS-XX
     */
    @Test
    void testTaskSequenceOnPatchConfiguration() {
        final Project project = projectBuilder().build().tap {
            apply plugin: 'sktools-dbtools-plugin'

            configureDatabasePlugin {
                toolset(name: 'testToolset', type: 'hsqldb', prefix: 'test') {

                    patch {
                        taskSequence('ToolsetTaskB', description: 'Task defined on toolset') {
                            dependsOn taskSequence('ToolsetTaskBB', description: 'Task defined on toolset') {
                            }
                        }
                        taskSequence('ToolsetTaskA') {
                            dependsOn taskSequence('ToolsetTaskAA') {
                            }
                        }

                        taskSequence('ToolsetTaskC') {
                            dependsOn taskSequence('ToolsetTaskCC')
                        }
                    }
                }
            }
        }

        assertNotNull(project.tasks.findByName('testToolsetTaskA'), 'Task definert av taskSequence(<name>, <closure>)')
        assertNotNull(project.tasks.findByName('testToolsetTaskAA'), 'SubTask definert av taskSequence(<name>, <closure>)')

        assertNotNull(project.tasks.findByName('testToolsetTaskB'), 'Task definert av taskSequence(<name>, <params>, <closure>)')
        assertNotNull(project.tasks.findByName('testToolsetTaskBB'), 'SubTask definert av taskSequence(<name>, <params>, <closure>)')

        assertNotNull(project.tasks.findByName('testToolsetTaskCC'), 'SubTask definert av taskSequence(<name>)')

    }

}
