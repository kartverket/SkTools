package no.statkart.sktools.gradle.plugins.dbtools

import no.statkart.sktools.gradle.plugins.dbtools.database.util.PatchConfiguration
import no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.SequenceTask
import no.statkart.sktools.gradle.plugins.dbtools.database.util.AbstractDatabaseConvention
import no.statkart.sktools.gradle.plugins.dbtools.database.DbtoolsConvention
import no.statkart.sktools.gradle.plugins.dbtools.testutils.DbToolsPluginTestContext
import org.testng.annotations.Test

/**
 * SKTOOLS-99: tester bruk av {@code taskSequence(..)} og {@link SequenceTask}
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

        final def testCase = new DbToolsPluginTestContext()

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

        testCase.assertProjectContainsTask('ProjectTaskA', 'Task definert av taskSequence(<name>, <closure>)')
        testCase.assertProjectContainsTask('ProjectTaskAA', 'SubTask definert av taskSequence(<name>, <closure>)')

        testCase.assertProjectContainsTask('ProjectTaskB', 'Task definert av taskSequence(<name>, <params>, <closure>)')
        testCase.assertProjectContainsTask('ProjectTaskBB', 'SubTask definert av taskSequence(<name>, <params>, <closure>)')

        testCase.assertProjectContainsTask('ProjectTaskCC', 'SubTask definert av taskSequence(<name>)')
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

        final def testCase = new DbToolsPluginTestContext()

        testCase.configureDatabasePlugin {
            toolset(name:'testToolset', type:'hsqldb', prefix:'test') {

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

        testCase.assertProjectContainsTask('testToolsetTaskA', 'Task definert av taskSequence(<name>, <closure>)')
        testCase.assertProjectContainsTask('testToolsetTaskAA', 'SubTask definert av taskSequence(<name>, <closure>)')

        testCase.assertProjectContainsTask('testToolsetTaskB', 'Task definert av taskSequence(<name>, <params>, <closure>)')
        testCase.assertProjectContainsTask('testToolsetTaskBB', 'SubTask definert av taskSequence(<name>, <params>, <closure>)')

        testCase.assertProjectContainsTask('testToolsetTaskCC', 'SubTask definert av taskSequence(<name>')
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

        final def testCase = new DbToolsPluginTestContext()

        testCase.configureDatabasePlugin {
            toolset(name:'testToolset', type:'hsqldb', prefix:'test') {

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

        testCase.assertProjectContainsTask('testToolsetTaskA', 'Task definert av taskSequence(<name>, <closure>)')
        testCase.assertProjectContainsTask('testToolsetTaskAA', 'SubTask definert av taskSequence(<name>, <closure>)')

        testCase.assertProjectContainsTask('testToolsetTaskB', 'Task definert av taskSequence(<name>, <params>, <closure>)')
        testCase.assertProjectContainsTask('testToolsetTaskBB', 'SubTask definert av taskSequence(<name>, <params>, <closure>)')

        testCase.assertProjectContainsTask('testToolsetTaskCC', 'SubTask definert av taskSequence(<name>)')
    }

}
