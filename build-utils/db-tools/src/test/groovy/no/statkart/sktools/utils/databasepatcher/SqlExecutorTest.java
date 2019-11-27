package no.statkart.sktools.utils.databasepatcher;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.testng.annotations.Test;

import java.sql.SQLException;

public class SqlExecutorTest {
    private final static Condition<String> WARNING = new Condition<String>() {
        @Override
        public boolean matches(String exceptionMessage) {
            return SqlExecutor.isWarning(new SQLException(exceptionMessage));
        }
    };

    @Test
    public void dropNoneExistingConstraintIsWarning() {
        Assertions.assertThat("ORA-02443 - Cannot drop constraint - nonexistent constraint").is(WARNING);
    }

    @Test
    public void referentialConstraintAlreadyExistsIsWarning() {
        Assertions.assertThat("ORA-02275: such a referential constraint already exists in the table").is(WARNING);
    }

   @Test
    public void nameConflictIsWarning() {
        Assertions.assertThat("ORA-00955: name is already being used by existing object").is(WARNING);
    }

   @Test
    public void dropNoneExistentIndexIsWarning() {
        Assertions.assertThat("ORA-01418: specified index does not exist").is(WARNING);
    }

   @Test
    public void referenceToNoneexistentTableOrViewIsWarning() {
        Assertions.assertThat("ORA-00942: table or view does not exist").is(WARNING);
    }
}
