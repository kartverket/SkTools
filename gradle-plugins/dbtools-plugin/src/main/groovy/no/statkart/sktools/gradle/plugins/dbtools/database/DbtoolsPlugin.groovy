package no.statkart.sktools.gradle.plugins.dbtools.database

import org.gradle.api.Project
import org.gradle.api.Plugin

import org.gradle.api.tasks.Copy

import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.logging.LogLevel

/**
 * Gradle plugin for database-moduler.
 *
 * <p>
 * <h5>Bruksanvisning</h5>
 *
 * <pre>
 *   <code>

apply plugin: 'sktools-dbtools-plugin'

//see {@link DbtoolsConvention#configureDatabasePlugin(Closure) }
configureDatabasePlugin {

    ...

}

 *   </code>
 * </pre>
 *     En modul kan betjene flere databaser samtidig. Disse blir satt opp via egne *Convention instanser.
 *
 *     @see DbtoolsConvention
 */
class DbtoolsPlugin implements Plugin<Project>  {

    DbtoolsConvention convention


    def void apply(Project project) {
        convention = new DbtoolsConvention(project)
        project.convention.plugins.db = convention

        convention.buildSQLTask = configureTaskBuildSQL(project, 'buildSQL', 'Filtrerer og bygger *.sql filer')
    }


    /**
     * todo: endre slik at sql script ligger relativt til prosjekt (og ikke src katalog)
     */
    private def configureTaskBuildSQL(Project project, String name, String description) {
        return project.task([type: Copy, description: description], name) {
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



