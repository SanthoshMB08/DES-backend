-- Fresh-install schema snapshot. This includes the current schema through V15
-- so an empty database can be initialized even when historical migrations are
-- no longer retained individually.
-- Existing populated databases without Flyway history are baselined at V14,
-- then reconciled by V15__academic_curriculum_entity.sql.

-- -----------------------------------------------------------------------------
-- V1__initial_schema.sql
-- -----------------------------------------------------------------------------
CREATE TABLE roles (
    id UUID PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(160),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id),
    role_id UUID NOT NULL REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE student (
    id UUID PRIMARY KEY,
    register_number VARCHAR(50) NOT NULL UNIQUE,
    student_name VARCHAR(160),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE hall_ticket (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES student(id),
    register_number VARCHAR(50) NOT NULL,
    student_type VARCHAR(20) NOT NULL CHECK (student_type IN ('FRESHER', 'REPEATER')),
    qr_hash VARCHAR(128) UNIQUE,
    scanned_at TIMESTAMPTZ NOT NULL,
    scanned_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE hall_ticket_exam (
    id UUID PRIMARY KEY,
    hall_ticket_id UUID NOT NULL REFERENCES hall_ticket(id) ON DELETE CASCADE,
    exam_date DATE NOT NULL,
    qp_code VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_hall_ticket_exam UNIQUE (hall_ticket_id, exam_date, qp_code)
);

CREATE TABLE attendance (
    id UUID PRIMARY KEY,
    student_id UUID REFERENCES student(id),
    register_number VARCHAR(50) NOT NULL,
    exam_date DATE NOT NULL,
    qp_code VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PRESENT', 'ABSENT')),
    answer_booklet_number VARCHAR(80) UNIQUE,
    marked_by UUID REFERENCES users(id),
    marked_at TIMESTAMPTZ,
    source VARCHAR(30) NOT NULL CHECK (source IN ('HALL_TICKET_SCAN', 'BOOKLET_SCAN', 'MANUAL_ABSENT')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_attendance_exam UNIQUE (register_number, exam_date, qp_code)
);

CREATE TABLE answer_booklet (
    id UUID PRIMARY KEY,
    booklet_number VARCHAR(80) NOT NULL UNIQUE,
    attendance_id UUID UNIQUE REFERENCES attendance(id),
    assigned_at TIMESTAMPTZ,
    assigned_by UUID REFERENCES users(id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('AVAILABLE', 'ASSIGNED', 'CANCELLED', 'DAMAGED'))
);

CREATE TABLE audit_log (
    id UUID PRIMARY KEY,
    actor_user_id UUID REFERENCES users(id),
    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID,
    action VARCHAR(80) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(80),
    user_agent VARCHAR(300),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_attendance_register_number ON attendance(register_number);
CREATE INDEX idx_attendance_exam_date_qp_code ON attendance(exam_date, qp_code);
CREATE INDEX idx_attendance_status ON attendance(status);
CREATE INDEX idx_attendance_student_id ON attendance(student_id);
CREATE INDEX idx_hall_ticket_student_id ON hall_ticket(student_id);
CREATE INDEX idx_hall_ticket_register_number ON hall_ticket(register_number);
CREATE INDEX idx_hall_ticket_exam_date_qp ON hall_ticket_exam(exam_date, qp_code);
CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_actor ON audit_log(actor_user_id);

-- -----------------------------------------------------------------------------
-- V2__admin_rbac.sql
-- -----------------------------------------------------------------------------
ALTER TABLE roles
    ADD COLUMN description VARCHAR(255),
    ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE users
    ADD COLUMN email VARCHAR(160),
    ADD COLUMN phone_number VARCHAR(40),
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN force_password_change BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN locked_at TIMESTAMPTZ,
    ADD COLUMN deleted_by UUID REFERENCES users(id),
    ADD COLUMN deleted_at TIMESTAMPTZ,
    ADD CONSTRAINT chk_user_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED'));

ALTER TABLE audit_log
    ADD COLUMN register_number VARCHAR(50),
    ADD COLUMN qp_code VARCHAR(50);

CREATE TABLE permissions (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id),
    permission_id UUID NOT NULL REFERENCES permissions(id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_deleted_at ON users(deleted_at);
CREATE INDEX idx_permissions_enabled ON permissions(enabled);
CREATE INDEX idx_audit_register_number ON audit_log(register_number);
CREATE INDEX idx_audit_qp_code ON audit_log(qp_code);

-- -----------------------------------------------------------------------------
-- V3__support_staff_schema.sql
-- -----------------------------------------------------------------------------
ALTER TABLE student
    ADD COLUMN center_code VARCHAR(50);

CREATE INDEX idx_student_center_code
ON student(center_code);

CREATE TABLE physical_bundle (
    id UUID PRIMARY KEY,
    bundle_code VARCHAR(100) NOT NULL UNIQUE,
    center_code VARCHAR(50) NOT NULL,
    exam_date DATE,
    qp_code VARCHAR(50),
    expected_booklet_count INTEGER,
    uploaded_booklet_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL CHECK (status IN ('OPEN', 'COMPLETED', 'CANCELLED')),
    started_by UUID NOT NULL REFERENCES users(id),
    started_at TIMESTAMPTZ NOT NULL,
    completed_by UUID REFERENCES users(id),
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_physical_bundle_center_code
ON physical_bundle(center_code);

CREATE INDEX idx_physical_bundle_exam
ON physical_bundle(exam_date, qp_code);

CREATE TABLE scanned_answer_booklet (
    id UUID PRIMARY KEY,
    answer_booklet_id UUID NOT NULL UNIQUE REFERENCES answer_booklet(id),
    physical_bundle_id UUID NOT NULL REFERENCES physical_bundle(id),
    center_code VARCHAR(50) NOT NULL,
    answer_booklet_number VARCHAR(80) NOT NULL,
    file_url TEXT NOT NULL,
    storage_provider VARCHAR(30) NOT NULL CHECK (
        storage_provider IN ('LOCAL', 'S3', 'MINIO', 'AZURE_BLOB', 'GOOGLE_CLOUD')
    ),
    original_file_name VARCHAR(255),
    file_size_bytes BIGINT,
    file_checksum VARCHAR(128),
    uploaded_by UUID NOT NULL REFERENCES users(id),
    uploaded_at TIMESTAMPTZ NOT NULL,
    replaced_by UUID REFERENCES users(id),
    replaced_at TIMESTAMPTZ,
    replacement_reason VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_scanned_booklet_center_code
ON scanned_answer_booklet(center_code);

CREATE INDEX idx_scanned_booklet_bundle
ON scanned_answer_booklet(physical_bundle_id);

CREATE INDEX idx_scanned_booklet_uploaded_by
ON scanned_answer_booklet(uploaded_by);


-- -----------------------------------------------------------------------------
-- V4__valuation_schema.sql
-- -----------------------------------------------------------------------------
CREATE TABLE valuation_bundle (
    id UUID PRIMARY KEY,
    bundle_code VARCHAR(100) NOT NULL UNIQUE,
    course VARCHAR(100),
    semester VARCHAR(20),
    qp_code VARCHAR(50),
    subject VARCHAR(150),
    center_code VARCHAR(50),
    total_scripts INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL CHECK (status IN ('PENDING', 'VALUATED', 'REVIEWED', 'APPROVED')),
    valuator_id UUID REFERENCES users(id),
    reviewer_id UUID REFERENCES users(id),
    boe_id UUID REFERENCES users(id),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_valuation_bundle_status ON valuation_bundle(status);
CREATE INDEX idx_valuation_bundle_valuator ON valuation_bundle(valuator_id);
CREATE INDEX idx_valuation_bundle_reviewer ON valuation_bundle(reviewer_id);

CREATE TABLE valuation_script (
    id UUID PRIMARY KEY,
    bundle_id UUID NOT NULL REFERENCES valuation_bundle(id),
    answer_booklet_id VARCHAR(100) NOT NULL UNIQUE,
    candidate_id VARCHAR(100),
    candidate_name VARCHAR(200),
    qp_code VARCHAR(50),
    subject VARCHAR(150),
    exam_date DATE,
    course VARCHAR(100),
    semester VARCHAR(20),
    center_code VARCHAR(50),
    scanned_file_name VARCHAR(255),
    file_url TEXT,
    status VARCHAR(30) NOT NULL CHECK (status IN ('PENDING', 'VALUATED', 'REVIEWED', 'APPROVED')),
    marks_json TEXT,
    total_marks NUMERIC(6, 2),
    review_comments VARCHAR(1000),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_valuation_script_bundle ON valuation_script(bundle_id);
CREATE INDEX idx_valuation_script_status ON valuation_script(status);

-- -----------------------------------------------------------------------------
-- V5__evaluation_hierarchy.sql
-- -----------------------------------------------------------------------------
-- Adds support for the RE -> BO -> Reviewer -> Evaluator approval chain,
-- per-subject evaluation windows, evaluator availability, and the public
-- marks-lookup feature.

CREATE TABLE subject (
    id UUID PRIMARY KEY,
    subject_code VARCHAR(50) NOT NULL,
    subject_name VARCHAR(150),
    course VARCHAR(100) NOT NULL,
    semester VARCHAR(20) NOT NULL,
    qp_code VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'CLOSED')) DEFAULT 'OPEN',
    bo_id UUID REFERENCES users(id),
    opened_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE curriculum_subject (
    id UUID PRIMARY KEY,
    course VARCHAR(100) NOT NULL,
    semester VARCHAR(20) NOT NULL,
    subject_order INTEGER NOT NULL,
    qp_code VARCHAR(50) NOT NULL,
    subject_name VARCHAR(150) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (course, semester, subject_order)
);

-- Self-referencing chain of command: RE created BO, BO created Reviewer, Reviewer created Evaluator.
ALTER TABLE users ADD COLUMN manager_id UUID REFERENCES users(id);
-- Only meaningful for evaluators: toggled manually, auto-locked while a bundle is assigned.
ALTER TABLE users ADD COLUMN available BOOLEAN NOT NULL DEFAULT true;
-- The subject a BO/Reviewer/Evaluator is scoped to (inherited down the chain from the BO's subject).
ALTER TABLE users ADD COLUMN subject_id UUID REFERENCES subject(id);

-- Scripts are now scanned into an unbundled pool per-subject; the BO groups them
-- into bundles of 20 explicitly, so bundle_id is no longer set at scan time.
ALTER TABLE valuation_script ALTER COLUMN bundle_id DROP NOT NULL;
ALTER TABLE valuation_script ADD COLUMN subject_id UUID REFERENCES subject(id);
ALTER TABLE valuation_bundle ADD COLUMN subject_id UUID REFERENCES subject(id);

-- -----------------------------------------------------------------------------
-- V6__hallticket_semester.sql
-- -----------------------------------------------------------------------------
-- ============================================================
-- Mobile invigilator flow: a hall ticket is now keyed by
-- (register_number, semester), not by a hash of the exam list.
-- This lets the same semester's hall ticket be reused/extended
-- across multiple exam days, while a different semester (e.g. a
-- backlog paper) always starts a fresh hall ticket.
-- ============================================================

ALTER TABLE hall_ticket ADD COLUMN semester VARCHAR(20);

-- Backfill any pre-existing rows (there should be none in a fresh install)
-- so the NOT NULL below doesn't fail.
UPDATE hall_ticket SET semester = 'UNKNOWN' WHERE semester IS NULL;

ALTER TABLE hall_ticket ALTER COLUMN semester SET NOT NULL;

ALTER TABLE hall_ticket ADD CONSTRAINT uk_hall_ticket_register_semester UNIQUE (register_number, semester);

-- qr_hash is no longer used to identify a hall ticket (kept, nullable, for
-- backward compatibility with any existing rows / external references).
ALTER TABLE hall_ticket ALTER COLUMN qr_hash DROP NOT NULL;

-- -----------------------------------------------------------------------------
-- V7__course_hierarchy.sql
-- -----------------------------------------------------------------------------
-- ============================================================
-- Restructures the evaluation hierarchy:
--   RE creates a Course (course + semester), and one or more
--   Subjects (QP codes) within it. RE appoints one BO per Course
--   (previously: per Subject). The BO appoints a Subject Chief for
--   each Subject in their course; the Chief appoints the Reviewer;
--   the Reviewer appoints up to 4 Evaluators (unchanged). The
--   Chief - not the BO - forms bundles of 20 and assigns them to
--   Evaluators. The BO gets a read-only rollup of every paper's
--   status across every subject in their course.
-- ============================================================

CREATE TABLE course (
    id UUID PRIMARY KEY,
    course_name VARCHAR(100) NOT NULL,
    semester VARCHAR(20) NOT NULL,
    bo_id UUID REFERENCES users(id),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (course_name, semester)
);

-- A subject now belongs to a course, and (once the BO assigns one) has its
-- own Subject Chief. The single "bo_id" a subject used to carry directly is
-- replaced by the course's bo_id - a BO now oversees every subject in their
-- course, not just one.
ALTER TABLE subject ADD COLUMN course_id UUID REFERENCES course(id);
ALTER TABLE subject ADD COLUMN chief_id UUID REFERENCES users(id);
ALTER TABLE subject DROP COLUMN bo_id;

-- Bug fix: the RE's "publish results" action sets a subject's status to
-- PUBLISHED, but this CHECK constraint only ever allowed OPEN/CLOSED -
-- meaning every publish attempt failed with a constraint violation.
ALTER TABLE subject DROP CONSTRAINT IF EXISTS subject_status_check;
ALTER TABLE subject ADD CONSTRAINT subject_status_check CHECK (status IN ('OPEN', 'CLOSED', 'PUBLISHED'));

-- -----------------------------------------------------------------------------
-- V8__subject_max_marks.sql
-- -----------------------------------------------------------------------------
-- ============================================================
-- Phase 1 hardening: a Subject needs a maximum-marks ceiling so
-- evaluator/reviewer marks entry can be validated against it (a
-- floor of 0 already existed at the DTO level - this adds the
-- corresponding ceiling). Defaults to 100, the common case, but
-- the RE can set a different value per subject at creation time.
-- ============================================================

ALTER TABLE subject ADD COLUMN max_marks NUMERIC(6, 2) NOT NULL DEFAULT 100;

-- -----------------------------------------------------------------------------
-- V9__user_optimistic_locking.sql
-- -----------------------------------------------------------------------------
-- ============================================================
-- Phase 1 hardening: optimistic locking on users, to close the
-- read-then-write race where two concurrent bundle-assignment
-- requests could both see an evaluator as "available" before
-- either commits, double-booking them.
-- ============================================================

ALTER TABLE users ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- -----------------------------------------------------------------------------
-- V10__academic_structure.sql
-- -----------------------------------------------------------------------------
-- ============================================================
-- Phase 2: real academic data model.
--
-- Department -> Program (e.g. "BCA", 6 semesters) -> Batch (a
-- specific admission cohort of a Program, e.g. "BCA 2023-26").
--
-- Course (the per-semester evaluation cycle the RE opens today)
-- gets an optional link to a Program - existing free-text
-- course_name/semester columns are untouched, so nothing already
-- built breaks. Student gets an optional link to a Batch plus
-- the demographic fields a real student master needs. Subject
-- gets the CIE/SEE metadata needed before any grading is possible.
--
-- Every new link is nullable - this phase is purely additive.
-- ============================================================

CREATE TABLE department (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    code VARCHAR(20) NOT NULL UNIQUE,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE program (
    id UUID PRIMARY KEY,
    department_id UUID REFERENCES department(id),
    name VARCHAR(150) NOT NULL,
    code VARCHAR(20) NOT NULL UNIQUE,
    duration_semesters INTEGER NOT NULL DEFAULT 6,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE batch (
    id UUID PRIMARY KEY,
    program_id UUID NOT NULL REFERENCES program(id),
    admission_year VARCHAR(9) NOT NULL, -- e.g. "2023" or "2023-24"
    label VARCHAR(100), -- e.g. "BCA 2023-26" - auto-derivable but stored for display convenience
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (program_id, admission_year)
);

ALTER TABLE course ADD COLUMN program_id UUID REFERENCES program(id);

ALTER TABLE student ADD COLUMN batch_id UUID REFERENCES batch(id);
ALTER TABLE student ADD COLUMN date_of_birth DATE;
ALTER TABLE student ADD COLUMN category VARCHAR(30);
ALTER TABLE student ADD COLUMN gender VARCHAR(20);

ALTER TABLE subject ADD COLUMN credits NUMERIC(4, 1);
ALTER TABLE subject ADD COLUMN cie_weightage NUMERIC(5, 2);
ALTER TABLE subject ADD COLUMN see_weightage NUMERIC(5, 2);
ALTER TABLE subject ADD COLUMN max_cie_marks NUMERIC(6, 2);
ALTER TABLE subject ADD COLUMN min_pass_marks NUMERIC(6, 2);

-- -----------------------------------------------------------------------------
-- V11__cie_marks.sql
-- -----------------------------------------------------------------------------
-- ============================================================
-- Phase 3: internal marks (CIE - Continuous Internal Evaluation).
--
-- This runs entirely alongside the existing SEE (semester-end
-- exam) evaluation pipeline built in earlier phases - nothing
-- about scripts/bundles/evaluators changes. A subject gets an
-- assigned Faculty member (parallel to how a BO assigns a
-- Chief for SEE), who defines their own CIE components (Test 1,
-- Test 2, Assignment, etc.) and enters marks per student per
-- component. No separate "roster" table - a student enters the
-- CIE sheet the moment Faculty enters their first mark, the same
-- find-or-create pattern already used by the invigilator and
-- scanner flows.
-- ============================================================

ALTER TABLE subject ADD COLUMN faculty_id UUID REFERENCES users(id);
ALTER TABLE subject ADD COLUMN cie_locked BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE cie_component (
    id UUID PRIMARY KEY,
    subject_id UUID NOT NULL REFERENCES subject(id),
    name VARCHAR(100) NOT NULL,
    max_marks NUMERIC(6, 2) NOT NULL,
    sequence INTEGER NOT NULL DEFAULT 0,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (subject_id, name)
);

CREATE TABLE cie_mark (
    id UUID PRIMARY KEY,
    component_id UUID NOT NULL REFERENCES cie_component(id),
    student_id UUID NOT NULL REFERENCES student(id),
    marks_obtained NUMERIC(6, 2) NOT NULL,
    entered_by UUID REFERENCES users(id),
    entered_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (component_id, student_id)
);

CREATE INDEX idx_cie_mark_student ON cie_mark(student_id);

-- -----------------------------------------------------------------------------
-- V12__hallticket_bulk_import.sql
-- -----------------------------------------------------------------------------
-- ============================================================
-- OCR module integration: hall ticket data can now arrive as
-- bulk pre-data (extracted from the university's own hall
-- ticket PDFs) instead of only being built up one invigilator
-- scan at a time. This adds the exam metadata the OCR module
-- extracts but HallTicketExam didn't previously store, and an
-- index to support "find this student's exam scheduled for
-- today" lookups across ALL of their hall tickets/semesters at
-- once (needed for the barcode-first invigilator flow, where
-- the invigilator no longer picks a semester/QP code upfront).
-- ============================================================

ALTER TABLE hall_ticket_exam ADD COLUMN IF NOT EXISTS course_name VARCHAR(255);
ALTER TABLE hall_ticket_exam ADD COLUMN IF NOT EXISTS exam_time VARCHAR(20);
ALTER TABLE hall_ticket_exam ADD COLUMN IF NOT EXISTS centre_code VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_hall_ticket_exam_date ON hall_ticket_exam(exam_date);
CREATE INDEX IF NOT EXISTS idx_hall_ticket_register_number ON hall_ticket(register_number);

-- -----------------------------------------------------------------------------
-- V13__course_syllabus_year.sql
-- -----------------------------------------------------------------------------
-- A course offering is identified by program/course, semester, and syllabus
-- version. This permits BCA semester 6 to exist under both NEP-2020 and
-- SEP-2023 without mixing their subjects or valuation workflow.

ALTER TABLE course ADD COLUMN syllabus_year VARCHAR(40);

-- Preserve courses created before syllabus versions were introduced.
UPDATE course
SET syllabus_year = 'LEGACY'
WHERE syllabus_year IS NULL;

ALTER TABLE course ALTER COLUMN syllabus_year SET NOT NULL;
ALTER TABLE course DROP CONSTRAINT IF EXISTS course_course_name_semester_key;
ALTER TABLE course
    ADD CONSTRAINT uk_course_offering
    UNIQUE (course_name, semester, syllabus_year);

-- -----------------------------------------------------------------------------
-- V14__academic_syllabus_subject_setup.sql
-- -----------------------------------------------------------------------------
CREATE TABLE academic_syllabus (
    id UUID PRIMARY KEY,
    program_id UUID NOT NULL REFERENCES program(id),
    name VARCHAR(100) NOT NULL,
    year VARCHAR(20) NOT NULL,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (program_id, name, year)
);

ALTER TABLE course ADD COLUMN syllabus_id UUID REFERENCES academic_syllabus(id);

CREATE TABLE academic_subject (
    id UUID PRIMARY KEY,
    syllabus_id UUID NOT NULL REFERENCES academic_syllabus(id),
    semester VARCHAR(20) NOT NULL,
    subject_code VARCHAR(50) NOT NULL,
    subject_name VARCHAR(200) NOT NULL,
    qp_code VARCHAR(100) NOT NULL,
    max_marks NUMERIC(6,2) NOT NULL DEFAULT 100,
    marking_scheme JSONB NOT NULL DEFAULT '[]'::jsonb,
    credits NUMERIC(4,1),
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (syllabus_id, semester, qp_code)
);


-- V15 academic curriculum (manual/bootstrap schema)
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
ALTER TABLE academic_subject ADD COLUMN IF NOT EXISTS curriculum_id UUID REFERENCES academic_curriculum(id);
