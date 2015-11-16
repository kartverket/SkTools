package no.statkart.sktools.gradle.plugins.dbtools

import no.statkart.sktools.gradle.plugins.dbtools.database.DbtoolsConvention
import no.statkart.sktools.gradle.plugins.dbtools.database.util.AbstractDatabaseConvention
import no.statkart.sktools.gradle.plugins.dbtools.database.util.PatchConfiguration
import no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.SequenceTask
import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.builder.DbToolsProjectBuilder
import org.testng.annotations.Test

import static org.testng.Assert.assertNotNull

/**
 * SKTOOLS-99: tester bruk av {@code taskSequence ( .. )} og {@link SequenceTask}
 *
 * @since 1.3 - ny grunnbok sprint 30
 * @author Leif Lislegård
 */
class TestSKTOOLS_99 {

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
        final ProjectHelper testCase = DbToolsProjectBuilder.builder().applyDbUtilsPlugin().build();
        testCase.configureProject {
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

        assertNotNull(testCase.project.tasks.findByName('ProjectTaskA'), 'Task definert av taskSequence(<name>, <closure>)')
        assertNotNull(testCase.project.tasks.findByName('ProjectTaskAA'), 'SubTask definert av taskSequence(<name>, <closure>)')

        assertNotNull(testCase.project.tasks.findByName('ProjectTaskB'), 'Task definert av taskSequence(<name>, <params>, <closure>)')
        assertNotNull(testCase.project.tasks.findByName('ProjectTaskBB'), 'SubTask definert av taskSequence(<name>, <params>, <closure>)')

        assertNotNull(testCase.project.tasks.findByName('ProjectTaskCC'), 'SubTask definert av taskSequence(<name>)')
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

        final ProjectHelper testCase = DbToolsProjectBuilder.builder().applyDbUtilsPlugin().build();
        testCase.configureProject {
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

        assertNotNull(testCase.project.tasks.findByName('testToolsetTaskA'), 'Task definert av taskSequence(<name>, <closure>)')
        assertNotNull(testCase.project.tasks.findByName('testToolsetTaskAA'), 'SubTask definert av taskSequence(<name>, <closure>)')

        assertNotNull(testCase.project.tasks.findByName('testToolsetTaskB'), 'Task definert av taskSequence(<name>, <params>, <closure>)')
        assertNotNull(testCase.project.tasks.findByName('testToolsetTaskBB'), 'SubTask definert av taskSequence(<name>, <params>, <closure>)')

        assertNotNull(testCase.project.tasks.findByName('testToolsetTaskCC'), 'SubTask definert av taskSequence(<name>)')
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

        final ProjectHelper testCase = DbToolsProjectBuilder.builder().applyDbUtilsPlugin().build();
        testCase.configureProject {
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

        assertNotNull(testCase.project.tasks.findByName('testToolsetTaskA'), 'Task definert av taskSequence(<name>, <closure>)')
        assertNotNull(testCase.project.tasks.findByName('testToolsetTaskAA'), 'SubTask definert av taskSequence(<name>, <closure>)')

        assertNotNull(testCase.project.tasks.findByName('testToolsetTaskB'), 'Task definert av taskSequence(<name>, <params>, <closure>)')
        assertNotNull(testCase.project.tasks.findByName('testToolsetTaskBB'), 'SubTask definert av taskSequence(<name>, <params>, <closure>)')

        assertNotNull(testCase.project.tasks.findByName('testToolsetTaskCC'), 'SubTask definert av taskSequence(<name>)')

    }

}
