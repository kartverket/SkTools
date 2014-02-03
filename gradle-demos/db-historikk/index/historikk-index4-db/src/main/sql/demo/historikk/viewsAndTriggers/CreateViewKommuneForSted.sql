CREATE OR REPLACE FORCE VIEW "@historikk_index_db_username@"."KOMMUNERFORSTED"
(
  "STEDID",
  "KOMMUNEID",
  "TVERSION",
  "TBEGIN",
  "TEND"
)
AS
  SELECT
    h."STEDID",
    h."KOMMUNEID",
    h."TVERSION",
    h."TBEGIN",
    h."TEND"
  FROM "@historikk_index_db_schema@"."KOMMUNERFORSTED_H" h
  WHERE
     (h."TBEGIN" <= SNAPSHOT_TIME.Get_T() AND h."TEND" > SNAPSHOT_TIME.Get_T())
   ORDER BY
    h."STEDID",
    h."KOMMUNEID",
    h."TEND" DESC,
    h."TBEGIN" DESC
 ;


CREATE OR REPLACE TRIGGER "@historikk_index_db_schema@"."T_H_KOMMUNERFORSTED"
  INSTEAD OF INSERT OR UPDATE OR DELETE ON "@historikk_index_db_username@"."KOMMUNERFORSTED"
  FOR EACH ROW
  DECLARE
  t_Trans TIMESTAMP(9) := HISTORIKK_TRANSACTION.Get_T_Trans();
  LOGISK_FEIL EXCEPTION;
BEGIN
  IF snapshot_time.GeT_T() <> SNAPSHOT_TIME.Get_T_LIVE() THEN
    RAISE_APPLICATION_ERROR(-20100, 'Modifikasjon mot historikk-data fungerer kun med t satt til t_LIVE');
  END IF;
  IF INSERTING THEN
    INSERT INTO "@historikk_index_db_schema@"."KOMMUNERFORSTED_H" VALUES
      (
        1,
        t_Trans,
        SNAPSHOT_TIME.Get_T_END(),
        :new."STEDID",
        :new."KOMMUNEID"
      );
  ELSIF UPDATING THEN
    IF :old."TBEGIN" < t_Trans THEN
      INSERT INTO "@historikk_index_db_schema@"."KOMMUNERFORSTED_H" VALUES
        (
          :old."TVERSION",
          :old."TBEGIN",
          t_Trans,
          :old."STEDID",
          :old."KOMMUNEID"
        ) ;
      UPDATE "@historikk_index_db_schema@"."KOMMUNERFORSTED_H" SET
       --GBOK-1824: setter verdier for transaksjon en og kun kun en gang per transaksjon.
        "TVERSION"    = :old."TVERSION" + 1 ,
        "TBEGIN"      = t_Trans
      WHERE
          "STEDID"    = :old."STEDID"
      AND "KOMMUNEID" = :old."KOMMUNEID"
      AND "TEND"      = SNAPSHOT_TIME.Get_T_END()
      AND "TBEGIN"    = :old."TBEGIN"
      ;
    END IF;

    UPDATE "@historikk_index_db_schema@"."KOMMUNERFORSTED_H" SET
      "STEDID"      = :new."STEDID",
      "KOMMUNEID"   = :new."KOMMUNEID"
    WHERE
        "STEDID"    = :old."STEDID"
    AND "KOMMUNEID" = :old."KOMMUNEID"
    AND "TEND"      = SNAPSHOT_TIME.Get_T_END()
    AND "TBEGIN"    = t_Trans
    ;

  ELSIF DELETING THEN
    IF :old."TBEGIN" < t_Trans THEN
      INSERT INTO "@historikk_index_db_schema@"."KOMMUNERFORSTED_H" VALUES
        (
          :old."TVERSION",
          :old."TBEGIN",
          t_Trans,
          :old."STEDID",
          :old."KOMMUNEID"
        ) ;
    END IF;

    DELETE FROM "@historikk_index_db_schema@"."KOMMUNERFORSTED_H"
    WHERE
        "STEDID"    = :old."STEDID"
    AND "KOMMUNEID" = :old."KOMMUNEID"
    AND "TEND"      = SNAPSHOT_TIME.Get_T_END()
    AND "TBEGIN"    = :old."TBEGIN"
    ;
  END IF;
END "T_H_KOMMUNERFORSTED";
/

ALTER TRIGGER "@historikk_index_db_schema@"."T_H_KOMMUNERFORSTED" ENABLE;
