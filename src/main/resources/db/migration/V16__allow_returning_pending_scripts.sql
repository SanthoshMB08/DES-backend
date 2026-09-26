-- A partially completed bundle must be able to return unfinished scripts to
-- the unbundled pool by clearing their bundle_id.
ALTER TABLE valuation_script
    ALTER COLUMN bundle_id DROP NOT NULL;
