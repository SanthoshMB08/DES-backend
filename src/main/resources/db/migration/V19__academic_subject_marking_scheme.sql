ALTER TABLE academic_subject
    ADD COLUMN IF NOT EXISTS marking_scheme JSONB NOT NULL DEFAULT '[]'::jsonb;
