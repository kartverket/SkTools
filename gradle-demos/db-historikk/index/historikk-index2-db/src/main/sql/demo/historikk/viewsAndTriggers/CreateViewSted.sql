CREATE OR REPLACE FORCE VIEW "@historikk_index1_db_username@"."STED"
(
  "ID",
  "STEDSNAVN",
  "GRUPPE",
  "TVERSION",
  "TBEGIN",
  "TEND"
)
AS
  SELECT
    h."ID",
    h."STEDSNAVN",
    h."GRUPPE",
    h."TVERSION",
    h."TBEGIN",
    h."TEND"
FROM "@historikk_index1_db_schema@"."STED_H" h
WHERE
     (h."TEND" > SNAPSHOT_TIME.Get_T() AND h."TBEGIN" <= SNAPSHOT_TIME.Get_T())
  OR (h."TEND" = SNAPSHOT_TIME.Get_T_CURRENT() AND h."TBEGIN" <= SNAPSHOT_TIME.Get_T())
;

CREATE OR REPLACE TRIGGER "@historikk_index1_db_schema@"."T_H_STED"
INSTEAD OF INSERT OR UPDATE OR DELETE ON "@historikk_index1_db_username@"."STED"
FOR EACH ROW
DECLARE
  t_Trans TIMESTAMP(9) := HISTORIKK_TRANSACTION.Get_T_Trans();
  t_End TIMESTAMP(9) := SNAPSHOT_TIME.Get_T_CURRENT();
  LOGISK_FEIL EXCEPTION;
BEGIN
  IF snapshot_time.GeT_T() <> t_End THEN
    RAISE_APPLICATION_ERROR(-20100, 'Modifikasjon mot historikk-data fungerer kun med t satt til t_CURRENT');
  END IF;
  IF INSERTING THEN
    INSERT INTO "@historikk_index1_db_schema@"."STED_H" VALUES
      (
        1,
        t_Trans,
        t_End,
        :new."ID",
        :new."STEDSNAVN",
        :new."GRUPPE"
      );
  ELSIF UPDATING THEN
    IF :old."TBEGIN" < t_Trans THEN
      INSERT INTO "@historikk_index1_db_schema@"."STED_H" VALUES
        (
          :old."TVERSION",
          :old."TBEGIN",
          t_Trans,
          :old."ID",
          :old."STEDSNAVN",
          :old."GRUPPE"
        );
      UPDATE "@historikk_index1_db_schema@"."STED_H" SET
        --GBOK-1824: setter verdier for transaksjon en og kun kun en gang per transaksjon.
        "TVERSION"    = :old."TVERSION" + 1 ,
        "TBEGIN"      = t_Trans
      WHERE
          "ID"        = :old."ID"
      AND "TEND"      = t_End
      AND "TBEGIN"    = :old."TBEGIN"
      ;
    END IF;

    UPDATE "@historikk_index1_db_schema@"."STED_H" SET
      "ID"          = :new."ID",
      "STEDSNAVN"   = :new."STEDSNAVN",
      "GRUPPE"      = :new."GRUPPE"
    WHERE
        "ID"        = :old."ID"
    AND "TEND"      = t_End
    AND "TBEGIN"    = t_Trans
    ;

  ELSIF DELETING THEN
    IF :old."TBEGIN" < t_Trans THEN
      INSERT INTO "@historikk_index1_db_schema@"."STED_H" VALUES
        (
          :old."TVERSION",
          :old."TBEGIN",
          t_Trans,
          :old."ID",
          :old."STEDSNAVN",
          :old."GRUPPE"
        ) ;
    END IF;

    DELETE FROM "@historikk_index1_db_schema@"."STED_H"
    WHERE
        "ID"     = :old."ID"
    AND "TEND"   = t_End
    AND "TBEGIN" = :old."TBEGIN"
    ;

  END IF;
END "T_H_STED";
/

ALTER TRIGGER "@historikk_index1_db_schema@"."T_H_STED" ENABLE;