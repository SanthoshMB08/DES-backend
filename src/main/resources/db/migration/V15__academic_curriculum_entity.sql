ALTER TABLE academic_syllabus ALTER COLUMN program_id DROP NOT NULL;

CREATE TABLE IF NOT EXISTS academic_curriculum (
    id UUID PRIMARY KEY,
    program_id UUID NOT NULL REFERENCES program(id),
    syllabus_id UUID NOT NULL REFERENCES academic_syllabus(id),
    semester VARCHAR(20) NOT NULL,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (program_id, syllabus_id, semester)
);

ALTER TABLE academic_subject
    ADD COLUMN IF NOT EXISTS curriculum_id UUID REFERENCES academic_curriculum(id);
