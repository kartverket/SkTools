package no.statkart.sktools.utils.databasepatcher

import java.sql.Connection
import groovy.sql.Sql

/**
 * Wrapper for å kjøre {@link DatabasePatcher} ifra gradle/groovy
 * 
 * Wrapperen er ansvarlig for å sette {@link Connection}
 *
 * @author Leif Lislegård 
 * @since 1.2
 */
public class DatabasePatcherWrapper extends DatabasePatcher {


    String username
    String password

    String url
    String driver



    @Override
    protected Connection createConnection() {
        final Sql sql = Sql.newInstance(url, username, password, driver);
        final Connection connection = sql.getConnection();
        connection.setAutoCommit(false);
        return connection;
    }


}
