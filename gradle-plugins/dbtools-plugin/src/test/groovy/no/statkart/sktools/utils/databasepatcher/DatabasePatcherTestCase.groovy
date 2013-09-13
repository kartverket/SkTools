package no.statkart.sktools.utils.databasepatcher

import no.statkart.sktools.gradle.plugins.dbtools.testutils.DbToolsTestCase

/**
 * @since 1.3
 * @author Leif Lislegård
 */
class DatabasePatcherTestCase<T extends DatabasePatcherTestCase> extends DbToolsTestCase<T> {

    String jdbcDriverClassString
    String url
    String username
    String password

    DatabasePatcherTestCase(String jdbcDriverClassString, String url, String username, String password) {
        this.jdbcDriverClassString = jdbcDriverClassString
        this.url = url
        this.username = username
        this.password = password
    }

    protected DatabasePatcher setUpDatabasePatcher(String component = null) {
        DatabasePatcher databasePatcher = new DatabasePatcher()

        System.setProperty("hibernate.connection.driver_class", jdbcDriverClassString)
        System.setProperty("hibernate.connection.url", url)
        System.setProperty("hibernate.connection.username", username)
        System.setProperty("hibernate.connection.password", password)

        if (component != null) {
            databasePatcher.component = component
        }

        return databasePatcher
    }

    final static String PATCH_01 = """
-- PATCH DATA DB.VERSION="1.0" PATCH.NO="1" "Create test table"

CREATE TABLE TEST_TABLE (
   ID INTEGER NOT NULL,
   NAVN VARCHAR(32) NOT NULL
);
"""

    final static String PATCH_02 = """
-- PATCH INDEX DB.VERSION="1.0" PATCH.NO="2" "Inserting random rows"
INSERT INTO TEST_TABLE (ID, NAVN) VALUES (1, 'GENERATED INDEX patch: ' || TO_CHAR(CURRENT_TIMESTAMP, 'MI:SS:FF'));
"""

    final static String PATCH_03 = """
-- PATCH DATA DB.VERSION="1.0" PATCH.NO="3" "Inserting Chuck Norris"
INSERT INTO TEST_TABLE (ID, NAVN) VALUES (1, 'CHUCK NORRIS');
"""


    /**
     * Genererer patch fil med to patcher:
     * <ul>
     *     <li> 1.0 patch#1 - Create test table -> {@link #PATCH_01}
     *     <li> 1.0 patch#3 - Inserting Chuck Norris -> {@link #PATCH_03}
     * </ul>
     */
    static File createSimplePatchFile(File dir = null) {
        createTempFile(DbToolsTestCase.FILE_TYPE.SQL, dir, """

--kommentar

-- PATCH DB.MIN.VERSION="<any>"
${PATCH_01}

${PATCH_03}

""")
    }

}
