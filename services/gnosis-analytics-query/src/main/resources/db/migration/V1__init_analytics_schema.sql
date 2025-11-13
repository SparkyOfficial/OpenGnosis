-- Student Grades Read Model
CREATE TABLE student_grades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    grade_value INTEGER NOT NULL,
    grade_type VARCHAR(50) NOT NULL,
    comment TEXT,
    placed_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_student_subject ON student_grades(student_id, subject_id);
CREATE INDEX idx_student_id ON student_grades(student_id);
CREATE INDEX idx_subject_id ON student_grades(subject_id);
CREATE INDEX idx_created_at ON student_grades(created_at);

-- Student Attendance Read Model
CREATE TABLE student_attendance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL,
    class_id UUID NOT NULL,
    date DATE NOT NULL,
    lesson_number INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    marked_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(student_id, class_id, date, lesson_number)
);

CREATE INDEX idx_student_class ON student_attendance(student_id, class_id);
CREATE INDEX idx_student_date ON student_attendance(student_id, date);
CREATE INDEX idx_class_date ON student_attendance(class_id, date);

-- Student Enrollment Read Model
CREATE TABLE student_enrollment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL,
    class_id UUID NOT NULL,
    enrollment_date DATE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE(student_id, class_id)
);

CREATE INDEX idx_student_enrollment ON student_enrollment(student_id, class_id);
CREATE INDEX idx_student_id_enroll ON student_enrollment(student_id);
CREATE INDEX idx_class_id_enroll ON student_enrollment(class_id);
CREATE INDEX idx_is_active ON student_enrollment(is_active);
