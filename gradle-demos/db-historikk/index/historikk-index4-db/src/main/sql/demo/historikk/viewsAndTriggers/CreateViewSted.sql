CREATE OR REPLACE FORCE VIEW "@historikk_index_db_username@"."STED"
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
  FROM "@historikk_index_db_schema@"."STED_H" h
  WHERE
     (h."TBEGIN" <= SNAPSHOT_TIME.Get_T())
    AND
     (nvl(h."TEND", TIMESTAMP'9999-12-12 23:59:59.59') > SNAPSHOT_TIME.Get_T())
  ORDER BY
    h."ID",
    h."TEND" DESC,
    h."TBEGIN" DESC
;

CREATE OR REPLACE TRIGGER "@historikk_index_db_schema@"."T_H_STED"
INSTEAD OF INSERT OR UPDATE OR DELETE ON "@historikk_index_db_username@"."STED"
FOR EACH ROW
DECLARE
  t_Trans TIMESTAMP(9) := HISTORIKK_TRANSACTION.Get_T_Trans();
  LOGISK_FEIL EXCEPTION;
BEGIN
  IF snapshot_time.GeT_T() < SNAPSHOT_TIME.To_T('9999-01-01 00:00:00.00') THEN
    RAISE_APPLICATION_ERROR(-20100, 'Modifikasjon mot historikk-data fungerer kun med t > ''9999-01-01 00:00:00.00''');
  END IF;
  IF INSERTING THEN
    INSERT INTO "@historikk_index_db_schema@"."STED_H" VALUES
      (
        1,
        t_Trans,
        null,
        :new."ID",
        :new."STEDSNAVN",
        :new."GRUPPE"
      );
  ELSIF UPDATING THEN
    IF :old."TBEGIN" < t_Trans THEN
      INSERT INTO "@historikk_index_db_schema@"."STED_H" VALUES
        (
          :old."TVERSION",
          :old."TBEGIN",
          t_Trans,
          :old."ID",
          :old."STEDSNAVN",
          :old."GRUPPE"
        );
      UPDATE "@historikk_index_db_schema@"."STED_H" SET
        --GBOK-1824: setter verdier for transaksjon en og kun kun en gang per transaksjon.
        "TVERSION"    = :old."TVERSION" + 1 ,
        "TBEGIN"      = t_Trans
      WHERE
          "ID"        = :old."ID"
      AND "TEND"      = null
      AND "TBEGIN"    = :old."TBEGIN"
      ;
    END IF;

    UPDATE "@historikk_index_db_schema@"."STED_H" SET
      "ID"          = :new."ID",
      "STEDSNAVN"   = :new."STEDSNAVN",
      "GRUPPE"      = :new."GRUPPE"
    WHERE
        "ID"        = :old."ID"
    AND "TEND"      = null
    AND "TBEGIN"    = t_Trans
    ;

  ELSIF DELETING THEN
    IF :old."TBEGIN" < t_Trans THEN
      INSERT INTO "@historikk_index_db_schema@"."STED_H" VALUES
        (
          :old."TVERSION",
          :old."TBEGIN",
          t_Trans,
          :old."ID",
          :old."STEDSNAVN",
          :old."GRUPPE"
        ) ;
    END IF;

    DELETE FROM "@historikk_index_db_schema@"."STED_H"
    WHERE
        "ID"     = :old."ID"
    AND "TEND"   = null
    AND "TBEGIN" = :old."TBEGIN"
    ;

  END IF;
END "T_H_STED";
/

ALTER TRIGGER "@historikk_index_db_schema@"."T_H_STED" ENABLE;
