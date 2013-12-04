package no.statkart.sktools.gradle.plugins.dbtools.database

import org.gradle.api.Project
import no.statkart.sktools.gradle.plugins.dbtools.database.oracle.OracleTasksConvention

import no.statkart.sktools.gradle.plugins.dbtools.database.hsqldb.HsqldbTasksConvention
import org.gradle.api.GradleException
import no.statkart.sktools.gradle.plugins.dbtools.database.util.AbstractDatabaseConvention
import org.gradle.api.artifacts.Dependency
import org.gradle.api.Task
import no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.SequenceTask

/**
 * Pluginen kan konfigureres til å håndtere flere ulike databaser og flere instanser av denne.
 * <p>
 *     For at pluginen kan utføre JDBC kall mot databasen trenger en å registrere driverene. Dette konfigureres via {@link DbtoolsConvention#useDrivers(Object) }
 * </p>
 *
 *
 * Konfigurasjon skjer via {@link DbtoolsConvention#configureDatabasePlugin(Closure)}
 *
 * <pre>
 *   <code>

configureDatabasePlugin {

    useDrivers "com.oracle:ojdbc6:11.2.0.2.0@jar"

    toolset(type:'oracle', name:'Db', prefix:'') {

        ... //for details, see {@link DbtoolsConvention#useToolset(String, String, String, Closure) }


    }

}

 *   </code>
 * </pre>
 *
 */
public class DbtoolsConvention {
    public final Project project;

    protected final List<Dependency> jdbcDependencies = new ArrayList<Dependency>(4);

    public final Map<String, ? extends AbstractDatabaseConvention> dbToolSets = new HashMap<String, AbstractDatabaseConvention>()

    DbtoolsConvention(Project project) {
        this.project = project
    }

    /**
     * Configures this plugin by running closure defined in your project.
     *
     * Configuration methods:
     * <ul>
     *     <li> {@link #toolset(Map, Closure) }
     *     <li> {@link #useDrivers(Object)}
     * </ul>
     */
    void configureDatabasePlugin(Closure closure) {
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.delegate = this
        closure()

    }


    /**
     * Følgende toolset er tilgjengelige:
     *
     * <ul>
     *     <li>'oracle' - se {@link OracleTasksConvention#config(Closure)} </li>
     *     <li>'hsqldb' - se {@link HsqldbTasksConvention#config(Closure)} </li>
     * </ul>
     *
     * <p>
     *
     * Eksempel for å legge til egne oracle targets:
     *
     * <pre><code>

configureDatabasePlugin {
     useToolset 'oracle', 'Db', 'mineScript' {

        url = "jdbc:oracle:thin:@oraclehost:1521:testbase"
        credentials.username = 'sa'
        credentials.password = ''

        //annen oracle config her. Se {@link OracleTasksConvention}
     }
}
     * </code></pre>
     *
     * Utifra dette genereres det opp tasks som kan kjøres på bakgrunn av filer som ligger  i ./scr/mineScript/&#42&#42/&#42.sql <br />
     * Taskene vil bli eksekvert med konfigurert url, username og password. <br />
     * Taskene vil bli navngitt <b>&lt;prefix&gt;&lt;filnavn&gt; </b><br />
     * Standard tasks for import og eksport vil også bli lagt til. <br />
     *
     *
     * <p>
     * <p>
     * Pluginen støtter flere samtidige toolset.
     *
     *
     * @param type type toolset (oracle eller hsqldb)
     * @param prefix prefiks for alle tasks for toolsett
     * @param path plassering for sql script
     * @param closure konfigurasjon av toolset
     * @return
     */
    @Deprecated //since 1.2
    protected def useToolset(String type, String prefix, String path, Closure closure) {
        println "useToolset(...){} is deprecated - use toolset(Map){} instead!"
        println "new config syntax: "
        println "toolset( name:'${prefix}', type:'${type}', prefix:'${prefix}') {"
        AbstractDatabaseConvention toolset = toolset(type:type, name:prefix, closure)
        toolset.config {

            project.fileTree("src/${path}").include('**/*.sql').files.each { File file ->
                String taskName = file.name.substring(0, file.name.length() - 4)
                String taskNameWithPrefix = prefix + taskName

                //ny syntax for config
                println "   sqlTask( '${taskName}', sqlFile:'${project.relativePath(file).replaceAll('\\\\', '/')}')"

                //legger til task
                sqlTask(taskName, sqlFile:file)
            }
        }
        println "   properties = project.properties"
        println "   ..."
        println "}"
    }

    protected def toolset(Map<String, ?> params, Closure closure) {

        String type = params.get('type')
        String name = params.get('name')
        String prefix = params.get('prefix', name)

        project.logger.info("Adding ${type} toolset with name '${name}' (prefix=${prefix})...")

        if ('oracle'.equalsIgnoreCase(type)) {
            return addOracleToolset(prefix, name, closure)

        } else if ('hsqldb'.equalsIgnoreCase(type)) {
            return addHsqldbToolset(prefix, name, closure)

        } else {
            throw new GradleException("Ukjent verktøyset/database")
        }

    }


    /**
     *  For å kunne benytte jdbc funksjonalitet, må jdbc klasser registreres i classloader til groovy.
     */
    protected void useDrivers(Object dependencyNotation) {
        [dependencyNotation].flatten().each {
            Dependency dependency = project.dependencies.create(it)

            project.dependencies.add(DbtoolsPlugin.DBTOOLS_CONFIGURATION, dependency)
            jdbcDependencies.add(dependency)
        }
    }

    private def addOracleToolset(String prefix, String name, Closure closure) {
        OracleTasksConvention convention = dbToolSets.get(name)

        if (convention == null) {
            project.logger.info("Applying Oracle convention with name ${name} ...")
            convention = new OracleTasksConvention(this, prefix, name)
            dbToolSets.put(name, convention)

        }

        convention.config(closure)

        return convention
    }

    private def addHsqldbToolset(String prefix, String name, Closure closure) {
        HsqldbTasksConvention convention = dbToolSets.get(prefix)

        if (convention == null) {
            project.logger.info("Applying HSQLDB convention with name ${name} ...")
            convention = new HsqldbTasksConvention(this, prefix, name)
            dbToolSets.put(name, convention)

        }

        convention.config(closure)

        return convention
    }

    protected Task taskSequence(String verb, Closure config = null) {
        return taskSequence([:], verb, config)
    }
    protected Task taskSequence(Map params, String verb, Closure config = null) {
        params['type'] = SequenceTask.class
        return project.task(params, verb, config)
    }

}
