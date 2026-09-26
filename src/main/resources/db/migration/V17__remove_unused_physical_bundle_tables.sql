-- These legacy physical-bundle tables have no application consumers. Refuse to
-- drop them if they contain data so an unexpected populated database is preserved.
DO $$
DECLARE
    table_has_rows BOOLEAN;
BEGIN
    IF to_regclass('scanned_answer_booklet') IS NOT NULL THEN
        EXECUTE 'SELECT EXISTS (SELECT 1 FROM scanned_answer_booklet)' INTO table_has_rows;
        IF table_has_rows THEN
            RAISE EXCEPTION 'scanned_answer_booklet contains data; refusing to drop it';
        END IF;
    END IF;

    IF to_regclass('physical_bundle') IS NOT NULL THEN
        EXECUTE 'SELECT EXISTS (SELECT 1 FROM physical_bundle)' INTO table_has_rows;
        IF table_has_rows THEN
            RAISE EXCEPTION 'physical_bundle contains data; refusing to drop it';
        END IF;
    END IF;
END;
$$;

DROP TABLE IF EXISTS scanned_answer_booklet;
DROP TABLE IF EXISTS physical_bundle;
