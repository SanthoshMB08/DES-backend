-- Bootstrap schema for a new, empty PostgreSQL database.

CREATE TABLE roles (
    id UUID PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    description VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE permissions (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(160),
    email VARCHAR(160),
    phone_number VARCHAR(40),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED')),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    force_password_change BOOLEAN NOT NULL DEFAULT FALSE,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES users(id),
    deleted_at TIMESTAMPTZ,
    manager_id UUID REFERENCES users(id),
    available BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    subject_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id),
    role_id UUID NOT NULL REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id),
    permission_id UUID NOT NULL REFERENCES permissions(id),
    PRIMARY KEY (role_id, permission_id)
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
    register_number VARCHAR(50),
    qp_code VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL
);

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
    admission_year VARCHAR(9) NOT NULL,
    label VARCHAR(100),
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_batch_program_admission_year UNIQUE (program_id, admission_year)
);

CREATE TABLE academic_syllabus (
    id UUID PRIMARY KEY,
    program_id UUID REFERENCES program(id),
    name VARCHAR(100) NOT NULL,
    year VARCHAR(20) NOT NULL,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_academic_syllabus_program_name_year UNIQUE (program_id, name, year)
);

CREATE TABLE academic_curriculum (
    id UUID PRIMARY KEY,
    program_id UUID NOT NULL REFERENCES program(id),
    syllabus_id UUID NOT NULL REFERENCES academic_syllabus(id),
    semester VARCHAR(20) NOT NULL,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_academic_curriculum_program_syllabus_semester
        UNIQUE (program_id, syllabus_id, semester)
);

CREATE TABLE academic_subject (
    id UUID PRIMARY KEY,
    syllabus_id UUID NOT NULL REFERENCES academic_syllabus(id),
    curriculum_id UUID REFERENCES academic_curriculum(id),
    semester VARCHAR(20) NOT NULL,
    subject_code VARCHAR(50) NOT NULL,
    subject_name VARCHAR(200) NOT NULL,
    qp_code VARCHAR(100) NOT NULL,
    max_marks NUMERIC(6, 2) NOT NULL DEFAULT 100,
    marking_scheme JSONB NOT NULL DEFAULT '[]'::jsonb,
    credits NUMERIC(4, 1),
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_academic_subject_syllabus_semester_qp
        UNIQUE (syllabus_id, semester, qp_code)
);

CREATE TABLE student (
    id UUID PRIMARY KEY,
    register_number VARCHAR(50) NOT NULL UNIQUE,
    student_name VARCHAR(160),
    batch_id UUID REFERENCES batch(id),
    date_of_birth DATE,
    category VARCHAR(30),
    gender VARCHAR(20),
    center_code VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE course (
    id UUID PRIMARY KEY,
    course_name VARCHAR(100) NOT NULL,
    semester VARCHAR(20) NOT NULL,
    syllabus_year VARCHAR(40) NOT NULL,
    syllabus_id UUID REFERENCES academic_syllabus(id),
    program_id UUID REFERENCES program(id),
    bo_id UUID REFERENCES users(id),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_course_offering UNIQUE (course_name, semester, syllabus_year)
);

CREATE TABLE subject (
    id UUID PRIMARY KEY,
    subject_code VARCHAR(50) NOT NULL,
    subject_name VARCHAR(150),
    course VARCHAR(100) NOT NULL,
    semester VARCHAR(20) NOT NULL,
    qp_code VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN', 'CLOSED', 'PUBLISHED')),
    max_marks NUMERIC(6, 2) NOT NULL DEFAULT 100,
    credits NUMERIC(4, 1),
    course_id UUID REFERENCES course(id),
    chief_id UUID REFERENCES users(id),
    opened_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

ALTER TABLE users
    ADD CONSTRAINT fk_users_subject FOREIGN KEY (subject_id) REFERENCES subject(id);

CREATE TABLE valuation_bundle (
    id UUID PRIMARY KEY,
    bundle_code VARCHAR(100) NOT NULL UNIQUE,
    course VARCHAR(100),
    semester VARCHAR(20),
    qp_code VARCHAR(50),
    subject VARCHAR(150),
    center_code VARCHAR(50),
    total_scripts INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL
        CHECK (status IN ('PENDING', 'VALUATED', 'REVIEWED', 'APPROVED')),
    valuator_id UUID REFERENCES users(id),
    reviewer_id UUID REFERENCES users(id),
    boe_id UUID REFERENCES users(id),
    created_by UUID NOT NULL REFERENCES users(id),
    subject_id UUID REFERENCES subject(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE valuation_script (
    id UUID PRIMARY KEY,
    bundle_id UUID REFERENCES valuation_bundle(id),
    subject_id UUID REFERENCES subject(id),
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
    status VARCHAR(30) NOT NULL
        CHECK (status IN ('PENDING', 'VALUATED', 'REVIEWED', 'APPROVED')),
    marks_json TEXT,
    total_marks NUMERIC(6, 2),
    review_comments VARCHAR(1000),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE booklet_record (
    id UUID PRIMARY KEY,
    booklet_number VARCHAR(80) NOT NULL UNIQUE,
    original_pdf_url TEXT,
    corrected_pdf_url TEXT,
    revaluation_pdf_url TEXT,
    evaluator_id UUID REFERENCES users(id),
    reviewer_id UUID REFERENCES users(id),
    boe_id UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE hall_ticket_data (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES student(id),
    usn VARCHAR(50) NOT NULL,
    qp_code VARCHAR(50) NOT NULL,
    exam_year_month VARCHAR(7) NOT NULL,
    booklet_id UUID UNIQUE REFERENCES booklet_record(id),
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_hall_ticket_data_qp_usn_month UNIQUE (qp_code, usn, exam_year_month)
);

CREATE TABLE exam_marks (
    id UUID PRIMARY KEY,
    usn VARCHAR(50) NOT NULL,
    booklet_id UUID NOT NULL UNIQUE REFERENCES booklet_record(id),
    question_marks TEXT,
    total_marks NUMERIC(7, 2),
    revaluation_question_marks TEXT,
    revaluation_total_marks NUMERIC(7, 2),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_deleted_at ON users(deleted_at);
CREATE INDEX idx_permissions_enabled ON permissions(enabled);
CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_actor ON audit_log(actor_user_id);
CREATE INDEX idx_audit_register_number ON audit_log(register_number);
CREATE INDEX idx_audit_qp_code ON audit_log(qp_code);
CREATE INDEX idx_student_center_code ON student(center_code);
CREATE INDEX idx_hall_ticket_data_usn ON hall_ticket_data(usn);
CREATE INDEX idx_hall_ticket_data_qp_month ON hall_ticket_data(qp_code, exam_year_month);
CREATE INDEX idx_exam_marks_usn ON exam_marks(usn);
CREATE INDEX idx_valuation_bundle_status ON valuation_bundle(status);
CREATE INDEX idx_valuation_bundle_valuator ON valuation_bundle(valuator_id);
CREATE INDEX idx_valuation_bundle_reviewer ON valuation_bundle(reviewer_id);
CREATE INDEX idx_valuation_script_bundle ON valuation_script(bundle_id);
CREATE INDEX idx_valuation_script_status ON valuation_script(status);
