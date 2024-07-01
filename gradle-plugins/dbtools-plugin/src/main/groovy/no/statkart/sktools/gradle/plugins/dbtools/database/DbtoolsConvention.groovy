package no.statkart.sktools.gradle.plugins.dbtools.database

import no.statkart.sktools.gradle.plugins.dbtools.database.hsqldb.HsqldbTasksConvention
import no.statkart.sktools.gradle.plugins.dbtools.database.oracle.OracleTasksConvention
import no.statkart.sktools.gradle.plugins.dbtools.database.util.AbstractDatabaseConvention
import no.statkart.sktools.gradle.plugins.dbtools.database.util.tasks.SequenceTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency

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

    useDrivers 'com.oracle.database.jdbc:ojdbc8g:12.2.0.1@jar'

    toolset(type:'oracle', name:'Db', prefix:'') {

        ... //for details, see {@link DbtoolsConvention#toolset(Map, Closure) }


    }

}

 *   </code>
 * </pre>
 *
 */
public class DbtoolsConvention {
    protected final transient Project project;
    protected final List<Dependency> jdbcDependencies = new ArrayList<>(4);
    public final Map<String, AbstractDatabaseConvention> dbToolSets = new HashMap<>();

    public DbtoolsConvention(Project project) {
        this.project = project;
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
        closure.setDelegate(this);
        closure.call();
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
    protected Object toolset(Map<String, ?> params, Closure closure) {
        String type = (String) params.get("type");
        String name = (String) params.get("name");
        String prefix = (String) params.getOrDefault("prefix", name);

        project.getLogger().info("Adding {} toolset with name '{}' (prefix={})...", type, name, prefix);

        AbstractDatabaseConvention toolset;
        if ("oracle".equalsIgnoreCase(type)) {
            toolset = addOracleToolset(prefix, name, closure);
        } else if ("hsqldb".equalsIgnoreCase(type)) {
            toolset = addHsqldbToolset(prefix, name, closure);
        } else {
            throw new GradleException("Unknown toolset/database");
        }
        return toolset;
    }


    /**
     *  For å kunne benytte jdbc funksjonalitet, må jdbc klasser registreres i classloader til groovy.
     */
    public void useDrivers(Object dependencyNotation) {
        project.getDependencies().add("dbtools", project.getDependencies().create(dependencyNotation));
        jdbcDependencies.add(project.getDependencies().create(dependencyNotation));
    }

    private AbstractDatabaseConvention addOracleToolset(String prefix, String name, Closure closure) {
        OracleTasksConvention convention = (OracleTasksConvention) dbToolSets.get(name);
        if (convention == null) {
            project.getLogger().info("Applying Oracle convention with name '{}' ...", name);
            convention = new OracleTasksConvention(this, prefix, name);
            dbToolSets.put(name, convention);
        }
        convention.config(closure);
        return convention;
    }

    private AbstractDatabaseConvention addHsqldbToolset(String prefix, String name, Closure closure) {
        HsqldbTasksConvention convention = (HsqldbTasksConvention) dbToolSets.get(prefix);
        if (convention == null) {
            project.getLogger().info("Applying HSQLDB convention with name '{}' ...", name);
            convention = new HsqldbTasksConvention(this, prefix, name);
            dbToolSets.put(name, convention);
        }
        convention.config(closure);
        return convention;
    }

    public Task taskSequence(String verb, Closure config) {
        SequenceTask task = project.getTasks().create(verb, SequenceTask.class);
        if (config != null) {
            config.setDelegate(task);
            config.setResolveStrategy(Closure.DELEGATE_FIRST);
            config.call();
        }
        return task;
    }

    public Task taskSequence(Map<String, ?> params, String verb, Closure config) {
        SequenceTask task = project.getTasks().create(verb, SequenceTask.class);

        if (params != null) {
            params.each { key, value ->
                task.setProperty(key, value);
            }
        }

        if (config != null) {
            config.setDelegate(task);
            config.setResolveStrategy(Closure.DELEGATE_FIRST);
            config.call();
        }

        return task;
    }
}
