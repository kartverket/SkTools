package no.statkart.sktools.utils.databasepatcher;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.testng.annotations.Test;

import java.sql.SQLException;

public class SqlExecutorTest {
    private final static Condition<String> WARNING = new Condition<String>("classified as warning") {
        @Override
        public boolean matches(String exceptionMessage) {
            return SqlExecutor.isWarning(new SQLException(exceptionMessage));
        }
    };

    @Test
    public void warningIsContainedByFirstLineOnly() {
        Assertions.assertThat("ORA-02443").is(WARNING);
        Assertions.assertThat("ORA-02443 \n ORA-02443").is(WARNING);
        Assertions.assertThat(" ORA-00000 \n ORA-02443").isNot(WARNING);
        Assertions.assertThat("           \n ORA-02443").isNot(WARNING);
        Assertions.assertThat("\n ORA-02443").isNot(WARNING);

        Assertions.assertThat("\r\n ORA-02443").isNot(WARNING);
        Assertions.assertThat("\f ORA-02443").isNot(WARNING);
        Assertions.assertThat("\r ORA-02443").isNot(WARNING);

        //some more chars defined as line breaks (in unicode)
        Assertions.assertThat("\u000B ORA-02443").isNot(WARNING); //vertical tab (LINE TABULATION)
        Assertions.assertThat("\u0085 ORA-02443").isNot(WARNING); //next line
        Assertions.assertThat("\u2028 ORA-02443").isNot(WARNING); //line-separator
        Assertions.assertThat("\u2029 ORA-02443").isNot(WARNING); //paragraph-separator
    }

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
