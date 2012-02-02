package no.statkart.sktools.gradle.plugins.dbtools.database.util

import org.gradle.api.Project
import no.statkart.sktools.gradle.plugins.dbtools.database.SQLTask

/**
 * Felles tasks for alle databaser
 */
abstract class AbstractDatabaseTasks<I extends AbstractDatabaseConvention> implements DatabaseTasksInterface<I> {

    private final String relativePath
    protected final I conv

    public AbstractDatabaseTasks(String relativePath, I convention) {
        this.relativePath = relativePath
        this.conv = convention
    }

    def init(Project project) {

        configureTargets(project, 'Database')

        return this
    }

    protected def configureTargets(Project project, String groupString) {

        def sourceRoot = new File(project.getProjectDir(), 'src')

        def files = project.fileTree(new File(sourceRoot, relativePath))
        files.include '**/*.sql'
        files.each() {
            File file ->

            project.task([type: SQLTask, dependsOn: [project.convention.plugins.db.buildSQLTask]], conv.prefix + file.name.substring(0, file.name.length() - 4)) {
                group = groupString
                convention = conv
                sqlFile = new File(project.getBuildDir(), file.getAbsolutePath().replace(sourceRoot.getAbsolutePath(), ''))
            }
        }

    }
}
