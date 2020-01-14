package no.statkart.sktools.utils.databasepatcher;

import no.statkart.sktools.utils.databasepatcher.exception.OperationalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Hjelpeklasser for bruk av JDBC api'et. Har metoder for å frigjøre JDBC ressurser på korrekt måte.
 */
public class JDBCHelper {
    private static final Logger logger = LoggerFactory.getLogger(JDBCHelper.class);

    /**
     * Tilbyr bare statiske metoder, skal ikke instansieres.
     */
    private JDBCHelper() {
    }


    /**
     * Oppretter en connection basert på inputparametere.
     *
     * @param driver, som skal lastes med DriverManager.registerDriver
     * @param url,    url som skal brukes
     * @param user,   brukernavn
     * @param pwd,    passord
     * @return connection
     */
    public static Connection createConnection(String driver, String url, String user, String pwd) throws SQLException {
        Connection connection;
        try {
            @SuppressWarnings("unchecked")
            Class<Driver> clazz = (Class<Driver>) Class.forName(driver);
            DriverManager.registerDriver(clazz.getConstructor().newInstance());
            connection = DriverManager.getConnection(url, user, pwd);
        } catch (ClassNotFoundException e) {
            throw new OperationalException(logger, "Feil under oppretting, finner ikke klassen: " + driver, e);
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new OperationalException(logger, "Har ikke tilgang til konstruktør av klassen: " + driver, e);
        } catch (InstantiationException | InvocationTargetException e) {
            throw new OperationalException(logger, "Feil under oppretting av class: " + driver, e);
        }
        return connection;
    }


    /**
     * Mulighet for programatisk konfigurering av properties
     *
     * @since 1.2
     */
    static Properties connectionProperties = System.getProperties();

    /**
     * Oppretter en connection basert på system properties for database connection:
     * -Dhibernate.connection.driver_class
     * -Dhibernate.connection.url
     * -Dhibernate.connection.username
     * -Dhibernate.connection.password
     */
    public static Connection createConnection() throws SQLException {
        String driver = connectionProperties.getProperty("hibernate.connection.driver_class");
        String url = connectionProperties.getProperty("hibernate.connection.url");
        String usr = connectionProperties.getProperty("hibernate.connection.username");
        String pwd = connectionProperties.getProperty("hibernate.connection.password");
        Connection con = JDBCHelper.createConnection(driver, url, usr, pwd);
        if (con == null) {
            throw new OperationalException(logger, "Klarte ikke opprette connection til " + url + " med bruker " + usr);
        }
        con.setAutoCommit(false); //SKTOOLS-148
        return con;
    }

    public static String getConnectionSchema() {
        String schema = connectionProperties.getProperty("hibernate.connection.schema");
        return schema;
    }


}
