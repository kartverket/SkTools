package no.statkart.matrikkel.util.db;

import no.statkart.matrikkel.util.exception.OperationalException;
import no.statkart.matrikkel.util.log.LoggerFactory;
import no.statkart.matrikkel.util.log.Logger;

import java.sql.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Hjelpeklasser for bruk av JDBC api'et. Har metoder for å frigjøre JDBC ressurser på korrekt måte.
 * 
 * @author Aksel Hilde
 */
public class JDBCHelper {
   private static Logger logger = LoggerFactory.getInstance().getLogger(JDBCHelper.class);

   /**
    * Tilbyr bare statiske metoder, skal ikke instansieres.
    */
   private JDBCHelper() {
   }

   /**
    * Frigjør en connection
    * @param connection
    * @throws no.statkart.matrikkel.util.exception.OperationalException dersom frigjøring av ressurser feilet.
    */
   public static void close(Connection connection){
      try {
         if (connection!=null) connection.close();
      } catch( SQLException e ) {
         throw new OperationalException(logger, "Frigjøring av JDBC Connection feilet", e);
      }
   }

   /**
    * Frigjør en statement og en connection
    * @param statement
    * @param connection
    * @throws no.statkart.matrikkel.util.exception.OperationalException dersom frigjøring av ressurser feilet.
    */
   public static void close(Statement statement, Connection connection){
      try {
         if (statement!=null) statement.close();
         if (connection!=null) connection.close();
      } catch( SQLException e ) {
         throw new OperationalException(logger, "Frigjøring av JDBC Statement eller Connection feilet", e);
      }
   }

   /**
    * Frigjør en statement
    * @param statement
    * @throws no.statkart.matrikkel.util.exception.OperationalException dersom frigjøring av ressurser feilet.
    */
   public static void close(Statement statement){
      try {
         if (statement!=null) statement.close();
      } catch( SQLException e ) {
         throw new OperationalException(logger, "Frigjøring av JDBC Statement feilet", e);
      }
   }

   /**
    * Frigjør et resultatsett og en statement.
    * @param resultSet
    * @param statement
    * @throws no.statkart.matrikkel.util.exception.OperationalException dersom frigjøring av ressurser feilet.
    */
   public static void close(ResultSet resultSet, Statement statement){
      try {
         if (resultSet!=null) resultSet.close();
         if (statement!=null) statement.close();
      } catch( SQLException e ) {
         throw new OperationalException(logger, "Frigjøring av JDBC ResultSet eller Statement feilet", e);
      }
   }

   /**
    * Frigjør et resultatsett, en statement og en connection
    * @param resultSet
    * @param statement
    * @param connection
    * @throws no.statkart.matrikkel.util.exception.OperationalException dersom frigjøring av ressurser feilet.
    */
   public static void close(ResultSet resultSet, Statement statement, Connection connection){
      try {
         if (resultSet!=null) resultSet.close();
         if (statement!=null) statement.close();
         if (connection!=null) connection.close();
      } catch( SQLException e ) {
         throw new OperationalException(logger, "Frigjøring av JDBC ResultSet, Statement eller Connection feilet", e);
      }
   }


   /**
    * Oppretter en connection basert på inputparametere.
    * @param driver, som skal lastes med DriverManager.registerDriver
    * @param url, url som skal brukes
    * @param user, brukernavn
    * @param pwd, passord
    * @return connection
    */
   public static Connection createConnection(String driver, String url, String user, String pwd) throws SQLException {
      Connection connection = null;
      try {
         Class claz = Class.forName(driver);
         DriverManager.registerDriver((Driver) claz.newInstance());
         connection = DriverManager.getConnection(url, user, pwd);
      } catch( ClassNotFoundException e ) {
         throw new OperationalException(logger,"Feil under oppretting, finner ikke klassen: " + driver,e);
      } catch( IllegalAccessException e ) {
         throw new OperationalException(logger,"Har ikke tillgang til konstruktør av klassen: " + driver,e);
      } catch( InstantiationException e ) {
         throw new OperationalException(logger,"Feil under oppretting av class: " + driver,e);
      }
      return connection;
   }

   /**
    * Oppretter en connection basert på system properties for database connection:
    * -Dhibernate.connection.driver_class
    * -Dhibernate.connection.url
    * -Dhibernate.connection.username
    * -Dhibernate.connection.password=
    */
   public static Connection createConnection() throws SQLException {
      String driver = System.getProperty("hibernate.connection.driver_class");
      String url = System.getProperty("hibernate.connection.url");
      String usr = System.getProperty("hibernate.connection.username");
      String pwd = System.getProperty("hibernate.connection.password");
      Connection con = JDBCHelper.createConnection(driver, url, usr, pwd);
      if( con == null ) {
         throw new OperationalException(logger, "Klarte ikke opprette connection til " + url + " med bruker " + usr);
      }
      return con;
   }


}
