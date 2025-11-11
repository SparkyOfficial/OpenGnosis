-- Classrooms table
CREATE TABLE classrooms (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    capacity INTEGER NOT NULL,
    CONSTRAINT chk_capacity_positive CHECK (capacity > 0)
);

CREATE INDEX idx_classroom_school ON classrooms(school_id);

-- Classroom equipment table
CREATE TABLE classroom_equipment (
    classroom_id UUID NOT NULL,
    equipment VARCHAR(50) NOT NULL,
    CONSTRAINT fk_classroom_equipment FOREIGN KEY (classroom_id) REFERENCES classrooms(id) ON DELETE CASCADE,
    PRIMARY KEY (classroom_id, equipment)
);

-- Teacher availability table
CREATE TABLE teacher_availability (
    id UUID PRIMARY KEY,
    teacher_id UUID NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_time_range CHECK (end_time > start_time)
);

CREATE INDEX idx_teacher_availability_teacher ON teacher_availability(teacher_id);

-- Schedules table
CREATE TABLE schedules (
    id UUID PRIMARY KEY,
    academic_year_id UUID NOT NULL,
    term_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
);

CREATE INDEX idx_schedule_academic_year ON schedules(academic_year_id);
CREATE INDEX idx_schedule_term ON schedules(term_id);

-- Schedule entries table
CREATE TABLE schedule_entries (
    id UUID PRIMARY KEY,
    schedule_id UUID NOT NULL,
    class_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    teacher_id UUID NOT NULL,
    classroom_id UUID NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    CONSTRAINT fk_schedule_entry_schedule FOREIGN KEY (schedule_id) REFERENCES schedules(id) ON DELETE CASCADE,
    CONSTRAINT fk_schedule_entry_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms(id),
    CONSTRAINT chk_entry_time_range CHECK (end_time > start_time)
);

CREATE INDEX idx_schedule_entry_schedule ON schedule_entries(schedule_id);
CREATE INDEX idx_schedule_entry_class ON schedule_entries(class_id);
CREATE INDEX idx_schedule_entry_teacher ON schedule_entries(teacher_id);
CREATE INDEX idx_schedule_entry_classroom ON schedule_entries(classroom_id);
