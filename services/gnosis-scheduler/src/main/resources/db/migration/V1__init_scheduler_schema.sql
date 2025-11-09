-- Scheduler Service Initial Schema
-- Classrooms table
CREATE TABLE scheduler.classrooms (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    building VARCHAR(100),
    floor INTEGER,
    capacity INTEGER NOT NULL,
    equipment TEXT[],
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_classroom_capacity CHECK (capacity > 0),
    CONSTRAINT chk_classroom_status CHECK (status IN ('AVAILABLE', 'MAINTENANCE', 'UNAVAILABLE'))
);

CREATE INDEX idx_classrooms_school_id ON scheduler.classrooms(school_id);
CREATE INDEX idx_classrooms_status ON scheduler.classrooms(status);

-- Teacher availability table
CREATE TABLE scheduler.teacher_availability (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    teacher_id UUID NOT NULL,
    day_of_week INTEGER NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    academic_year_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_day_of_week CHECK (day_of_week >= 1 AND day_of_week <= 7),
    CONSTRAINT chk_availability_times CHECK (end_time > start_time)
);

CREATE INDEX idx_teacher_availability_teacher_id ON scheduler.teacher_availability(teacher_id);
CREATE INDEX idx_teacher_availability_academic_year ON scheduler.teacher_availability(academic_year_id);

-- Schedules table
CREATE TABLE scheduler.schedules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    academic_year_id UUID NOT NULL,
    term_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    version INTEGER NOT NULL DEFAULT 1,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    CONSTRAINT chk_schedule_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT chk_schedule_version CHECK (version > 0)
);

CREATE INDEX idx_schedules_academic_year_id ON scheduler.schedules(academic_year_id);
CREATE INDEX idx_schedules_term_id ON scheduler.schedules(term_id);
CREATE INDEX idx_schedules_status ON scheduler.schedules(status);

-- Schedule entries table
CREATE TABLE scheduler.schedule_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    schedule_id UUID NOT NULL REFERENCES scheduler.schedules(id) ON DELETE CASCADE,
    class_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    teacher_id UUID NOT NULL,
    classroom_id UUID REFERENCES scheduler.classrooms(id),
    day_of_week INTEGER NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    lesson_number INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_entry_day_of_week CHECK (day_of_week >= 1 AND day_of_week <= 7),
    CONSTRAINT chk_entry_times CHECK (end_time > start_time),
    CONSTRAINT chk_lesson_number CHECK (lesson_number > 0),
    CONSTRAINT uq_schedule_entry UNIQUE (schedule_id, day_of_week, start_time, teacher_id)
);

CREATE INDEX idx_schedule_entries_schedule_id ON scheduler.schedule_entries(schedule_id);
CREATE INDEX idx_schedule_entries_class_id ON scheduler.schedule_entries(class_id);
CREATE INDEX idx_schedule_entries_teacher_id ON scheduler.schedule_entries(teacher_id);
CREATE INDEX idx_schedule_entries_classroom_id ON scheduler.schedule_entries(classroom_id);
CREATE INDEX idx_schedule_entries_day_time ON scheduler.schedule_entries(day_of_week, start_time);

-- Schedule conflicts table (for tracking detected conflicts)
CREATE TABLE scheduler.schedule_conflicts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    schedule_id UUID NOT NULL REFERENCES scheduler.schedules(id) ON DELETE CASCADE,
    entry_id_1 UUID NOT NULL REFERENCES scheduler.schedule_entries(id) ON DELETE CASCADE,
    entry_id_2 UUID NOT NULL REFERENCES scheduler.schedule_entries(id) ON DELETE CASCADE,
    conflict_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    detected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    CONSTRAINT chk_conflict_type CHECK (conflict_type IN ('TEACHER_DOUBLE_BOOKING', 'CLASSROOM_DOUBLE_BOOKING', 'CLASS_DOUBLE_BOOKING', 'TEACHER_UNAVAILABLE'))
);

CREATE INDEX idx_schedule_conflicts_schedule_id ON scheduler.schedule_conflicts(schedule_id);
CREATE INDEX idx_schedule_conflicts_resolved ON scheduler.schedule_conflicts(resolved);

-- Optimization constraints table
CREATE TABLE scheduler.optimization_constraints (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    schedule_id UUID NOT NULL REFERENCES scheduler.schedules(id) ON DELETE CASCADE,
    constraint_type VARCHAR(50) NOT NULL,
    constraint_data JSONB NOT NULL,
    weight INTEGER NOT NULL DEFAULT 1,
    is_hard_constraint BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_constraint_weight CHECK (weight > 0)
);

CREATE INDEX idx_optimization_constraints_schedule_id ON scheduler.optimization_constraints(schedule_id);
CREATE INDEX idx_optimization_constraints_type ON scheduler.optimization_constraints(constraint_type);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION scheduler.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers to automatically update updated_at
CREATE TRIGGER update_classrooms_updated_at BEFORE UPDATE ON scheduler.classrooms
    FOR EACH ROW EXECUTE FUNCTION scheduler.update_updated_at_column();

CREATE TRIGGER update_schedules_updated_at BEFORE UPDATE ON scheduler.schedules
    FOR EACH ROW EXECUTE FUNCTION scheduler.update_updated_at_column();

CREATE TRIGGER update_schedule_entries_updated_at BEFORE UPDATE ON scheduler.schedule_entries
    FOR EACH ROW EXECUTE FUNCTION scheduler.update_updated_at_column();
