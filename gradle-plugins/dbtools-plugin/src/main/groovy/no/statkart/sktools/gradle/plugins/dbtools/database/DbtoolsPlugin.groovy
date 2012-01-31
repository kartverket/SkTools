package no.statkart.sktools.gradle.plugins.dbtools.database

import org.gradle.api.Project
import org.gradle.api.Plugin

import org.apache.commons.lang.NotImplementedException
import org.gradle.api.tasks.Copy
import no.statkart.sktools.gradle.plugins.dbtools.database.oracle.OracleTasks
import no.statkart.sktools.gradle.plugins.dbtools.database.oracle.OracleDatabaseConvention
import java.sql.Driver
import java.sql.DriverManager

import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.logging.LogLevel

/**
 * Gradle plugin for database-moduler.
 *
 * <p>
 *     <code>apply plugin: no.statkart.matrikkel.build.utils.gradle.plugins.database.DatabasePlugin</code> kobler inn denne pluginen.
 * <p>
 *     En modul kan betjene flere databaser samtidig. Disse blir satt opp via egne *Convention instanser.
 *
 *     @see DbtoolsConvention
 */
class DbtoolsPlugin implements Plugin<Project>  {

    def void apply(Project project) {
        project.convention.plugins.db = new DbtoolsConvention(project)
        configureTaskBuildSQL(project, 'buildSQL', 'Filtrerer og bygger *.sql filer')
    }


    private def configureTaskBuildSQL(Project project, String name, String description) {
        project.task([type: Copy, description: description], name) {
//            group = groupString

            from('src') {
                include = '**/sql/**/*.sql'
            }

            destinationDir = project.buildDir

            outputs.upToDateWhen { false }  //skal alltid kjøre denne task uansett!


            // late binding enables use of any conventional properties potentially defined by plugins
            doFirst() {
                Properties props = new Properties()
                project.properties.each {
                    if (it.value instanceof CharSequence || it.value instanceof Number || it.value instanceof Boolean) {
                        props.setProperty(it.key, String.valueOf(it.value))
                    }
                }

                filter([tokens: props, beginToken: '@', endToken: '@'], ReplaceTokens)

                if (logger.isEnabled(LogLevel.DEBUG)) {
                    logger.debug('substitution properties:')
                    props.sort().each {
                        logger.debug(it.key + ' -> ' + it.value)
                    }
                }

//                expand() virker ikke på større filer... kommenterer derfor denne ut her...
//
//                // Substitute property references in files
//                expand(project.properties)
            }
        }
    }

}



