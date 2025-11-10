-- Structure Service Initial Schema

-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS structure;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Schools table
CREATE TABLE structure.schools (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    address TEXT NOT NULL,
    principal_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_school_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED'))
);

CREATE INDEX idx_schools_status ON structure.schools(status);
CREATE INDEX idx_schools_principal_id ON structure.schools(principal_id);

-- Academic years table
CREATE TABLE structure.academic_years (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID NOT NULL REFERENCES structure.schools(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_academic_year_dates CHECK (end_date > start_date),
    CONSTRAINT chk_academic_year_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'ARCHIVED'))
);

CREATE INDEX idx_academic_years_school_id ON structure.academic_years(school_id);
CREATE INDEX idx_academic_years_status ON structure.academic_years(status);

-- Terms table
CREATE TABLE structure.terms (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    academic_year_id UUID NOT NULL REFERENCES structure.academic_years(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    term_number INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_term_dates CHECK (end_date > start_date),
    CONSTRAINT chk_term_number CHECK (term_number > 0)
);

CREATE INDEX idx_terms_academic_year_id ON structure.terms(academic_year_id);

-- Classes table
CREATE TABLE structure.classes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID NOT NULL REFERENCES structure.schools(id) ON DELETE CASCADE,
    academic_year_id UUID NOT NULL REFERENCES structure.academic_years(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    grade INTEGER NOT NULL,
    class_teacher_id UUID NOT NULL,
    capacity INTEGER NOT NULL DEFAULT 30,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_class_grade CHECK (grade >= 1 AND grade <= 12),
    CONSTRAINT chk_class_capacity CHECK (capacity > 0),
    CONSTRAINT chk_class_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    CONSTRAINT uq_class_name_year UNIQUE (school_id, academic_year_id, name)
);

CREATE INDEX idx_classes_school_id ON structure.classes(school_id);
CREATE INDEX idx_classes_academic_year_id ON structure.classes(academic_year_id);
CREATE INDEX idx_classes_teacher_id ON structure.classes(class_teacher_id);
CREATE INDEX idx_classes_status ON structure.classes(status);

-- Subjects table
CREATE TABLE structure.subjects (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(20) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_subjects_code ON structure.subjects(code);

-- Class subjects junction table
CREATE TABLE structure.class_subjects (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    class_id UUID NOT NULL REFERENCES structure.classes(id) ON DELETE CASCADE,
    subject_id UUID NOT NULL REFERENCES structure.subjects(id) ON DELETE CASCADE,
    teacher_id UUID NOT NULL,
    hours_per_week INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_hours_per_week CHECK (hours_per_week > 0),
    CONSTRAINT uq_class_subject UNIQUE (class_id, subject_id)
);

CREATE INDEX idx_class_subjects_class_id ON structure.class_subjects(class_id);
CREATE INDEX idx_class_subjects_subject_id ON structure.class_subjects(subject_id);
CREATE INDEX idx_class_subjects_teacher_id ON structure.class_subjects(teacher_id);

-- Enrollments table
CREATE TABLE structure.enrollments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id UUID NOT NULL,
    class_id UUID NOT NULL REFERENCES structure.classes(id) ON DELETE CASCADE,
    enrollment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    unenrollment_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_enrollment_dates CHECK (unenrollment_date IS NULL OR unenrollment_date > enrollment_date),
    CONSTRAINT chk_enrollment_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'WITHDRAWN', 'TRANSFERRED')),
    CONSTRAINT uq_student_class_active UNIQUE (student_id, class_id, status)
);

CREATE INDEX idx_enrollments_student_id ON structure.enrollments(student_id);
CREATE INDEX idx_enrollments_class_id ON structure.enrollments(class_id);
CREATE INDEX idx_enrollments_status ON structure.enrollments(status);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION structure.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers to automatically update updated_at
CREATE TRIGGER update_schools_updated_at BEFORE UPDATE ON structure.schools
    FOR EACH ROW EXECUTE FUNCTION structure.update_updated_at_column();

CREATE TRIGGER update_classes_updated_at BEFORE UPDATE ON structure.classes
    FOR EACH ROW EXECUTE FUNCTION structure.update_updated_at_column();

CREATE TRIGGER update_enrollments_updated_at BEFORE UPDATE ON structure.enrollments
    FOR EACH ROW EXECUTE FUNCTION structure.update_updated_at_column();
