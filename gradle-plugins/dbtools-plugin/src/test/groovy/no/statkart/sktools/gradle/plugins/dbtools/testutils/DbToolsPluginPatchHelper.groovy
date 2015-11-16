package no.statkart.sktools.gradle.plugins.dbtools.testutils

import no.statkart.sktools.gradle.testutils.ProjectHelper

/**
 * @since 1.3
 * @author Leif Lislegård
 */
class DbToolsPluginPatchHelper {

    static File createSimplePatchFile(File dir = null) {
        ProjectHelper.createTempFile(".patch.sql", dir, '''

--kommentar

-- PATCH DB.MIN.VERSION="<any>"
-- PATCH DATA DB.VERSION="1.0" PATCH.NO="1" "Create test table"

CREATE TABLE TEST_TABLE (
   ID INTEGER NOT NULL,
   NAVN VARCHAR(32) NOT NULL,
   PRIMARY KEY (ID)
);

-- PATCH INDEX DB.VERSION="1.0" PATCH.NO="2" "Indexing names"
CREATE INDEX TEST_TABLE_IDX_NAME ON TEST_TABLE(NAVN);

-- PATCH DATA DB.VERSION="1.0" PATCH.NO="3" "Inserting Chuck Norris"
INSERT INTO TEST_TABLE (ID, NAVN) VALUES (1, 'CHUCK NORRIS');

-- PATCH INDEX DB.VERSION="1.0" PATCH.NO="4" "Indexing names and id"
CREATE INDEX TEST_TABLE_IDX_01 ON TEST_TABLE(NAVN, ID);

''')
    }

}
