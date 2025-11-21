package com.opengnosis.scheduler.service

import com.opengnosis.common.kafka.EventPublisher
import com.opengnosis.events.ScheduleCreatedEvent
import com.opengnosis.events.ScheduleModifiedEvent
import com.opengnosis.scheduler.dto.*
import com.opengnosis.scheduler.entity.Schedule
import com.opengnosis.scheduler.entity.ScheduleEntry
import com.opengnosis.scheduler.entity.ScheduleStatus
import com.opengnosis.scheduler.repository.ScheduleEntryRepository
import com.opengnosis.scheduler.repository.ScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

/**
 * Cave Johnson: "The Schedule Service! This is where we organize time itself! Teachers, classrooms, 
 * students - we're gonna fit 'em all into a perfect schedule! And if they don't fit? 
 * Well, that's what conflict detection is for! Science!"
 * RU: "Сервис расписания! Здесь мы организуем само время! Учителя, классы,
 * студенты - мы впихнём их всех в идеальное расписание! А если не впихнутся?
 * Ну, для этого у нас есть обнаружение конфликтов! Наука!"
 * UA: "Сервіс розкладу! Тут ми організовуємо сам час! Вчителі, класи,
 * студенти - ми впхнемо їх всіх в ідеальний розклад! А якщо не впхнуться?
 * Ну, для цього у нас є виявлення конфліктів! Наука!"
 */
@Service
class ScheduleService(
    private val scheduleRepository: ScheduleRepository,
    private val scheduleEntryRepository: ScheduleEntryRepository,
    private val teacherAvailabilityService: TeacherAvailabilityService,
    private val eventPublisher: EventPublisher
) {
    
    /**
     * Wheatley: "Creating a new schedule! Fresh start! Clean slate! It's in DRAFT status, 
     * which means we can still mess around with it. I mean, 'modify it professionally'. Yeah, that."
     * RU: "Создаём новое расписание! Свежий старт! Чистый лист! Оно в статусе ЧЕРНОВИК,
     * что означает, что мы всё ещё можем с ним возиться. То есть, 'профессионально модифицировать'. Да, это."
     * UA: "Створюємо новий розклад! Свіжий старт! Чистий аркуш! Воно в статусі ЧЕРНЕТКА,
     * що означає, що ми все ще можемо з ним возитися. Тобто, 'професійно модифікувати'. Так, це."
     */
    @Transactional
    fun createSchedule(request: CreateScheduleRequest): ScheduleResponse {
        val schedule = Schedule(
            academicYearId = request.academicYearId,
            termId = request.termId,
            status = ScheduleStatus.DRAFT
        )
        
        val saved = scheduleRepository.save(schedule)
        
        // GLaDOS: "Publishing a schedule created event. Because the entire system needs to know 
        // about your scheduling decisions. They're riveting. Truly."
        // RU: "Публикуем событие создания расписания. Потому что вся система должна знать
        // о ваших решениях по расписанию. Они захватывающие. Правда."
        // UA: "Публікуємо подію створення розкладу. Тому що вся система повинна знати
        // про ваші рішення щодо розкладу. Вони захоплюючі. Правда."
        eventPublisher.publish(
            ScheduleCreatedEvent(
                aggregateId = saved.id,
                academicYearId = saved.academicYearId,
                termId = saved.termId,
                createdBy = UUID.randomUUID() // TODO: Get from security context
                                              // Caroline: "We really should implement proper security context retrieval..."
                                              // RU: "Нам действительно стоит реализовать правильное получение контекста безопасности..."
                                              // UA: "Нам дійсно варто реалізувати правильне отримання контексту безпеки..."
            )
        )
        
        return saved.toResponse()
    }
    
    @Transactional(readOnly = true)
    fun getSchedule(id: UUID): ScheduleResponse {
        val schedule = scheduleRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Schedule not found with id: $id") }
        
        val entries = scheduleEntryRepository.findByScheduleId(id)
        return schedule.toResponse(entries)
    }
    
    @Transactional
    fun addScheduleEntry(scheduleId: UUID, request: CreateScheduleEntryRequest): ScheduleEntryResponse {
        val schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow { IllegalArgumentException("Schedule not found with id: $scheduleId") }
        
        val dayOfWeek = DayOfWeek.valueOf(request.dayOfWeek.uppercase())
        val startTime = LocalTime.parse(request.startTime)
        val endTime = LocalTime.parse(request.endTime)
        
        // Validate the entry
        val validation = validateScheduleEntry(
            scheduleId = scheduleId,
            classId = request.classId,
            teacherId = request.teacherId,
            classroomId = request.classroomId,
            dayOfWeek = dayOfWeek,
            startTime = startTime,
            endTime = endTime
        )
        
        if (!validation.valid) {
            throw ScheduleConflictException("Schedule entry validation failed", validation.conflicts)
        }
        
        val entry = ScheduleEntry(
            schedule = schedule,
            classId = request.classId,
            subjectId = request.subjectId,
            teacherId = request.teacherId,
            classroomId = request.classroomId,
            dayOfWeek = dayOfWeek,
            startTime = startTime,
            endTime = endTime
        )
        
        val saved = scheduleEntryRepository.save(entry)
        
        eventPublisher.publish(
            ScheduleModifiedEvent(
                aggregateId = scheduleId,
                scheduleEntryId = saved.id,
                classId = saved.classId,
                subjectId = saved.subjectId,
                teacherId = saved.teacherId,
                classroomId = saved.classroomId,
                dayOfWeek = saved.dayOfWeek,
                startTime = saved.startTime,
                endTime = saved.endTime,
                modifiedBy = UUID.randomUUID() // TODO: Get from security context
            )
        )
        
        return saved.toResponse()
    }
    
    /**
     * GLaDOS: "Schedule validation. The part where we check if your scheduling decisions make any sense. 
     * Spoiler alert: they usually don't. But we'll check anyway. It's what I do."
     * RU: "Валидация расписания. Часть, где мы проверяем, имеют ли ваши решения по расписанию хоть какой-то смысл.
     * Спойлер: обычно нет. Но мы всё равно проверим. Это то, что я делаю."
     * UA: "Валідація розкладу. Частина, де ми перевіряємо, чи мають ваші рішення щодо розкладу хоч якийсь сенс.
     * Спойлер: зазвичай ні. Але ми все одно перевіримо. Це те, що я роблю."
     */
    @Transactional(readOnly = true)
    fun validateScheduleEntry(
        scheduleId: UUID,
        classId: UUID,
        teacherId: UUID,
        classroomId: UUID,
        dayOfWeek: DayOfWeek,
        startTime: LocalTime,
        endTime: LocalTime
    ): ValidationResult {
        val conflicts = mutableListOf<ConflictInfo>()
        
        // Turret: "Searching for teacher conflicts. Target acquired. Analyzing schedule... Conflict detected!"
        // RU: "Ищем конфликты учителя. Цель захвачена. Анализируем расписание... Конфликт обнаружен!"
        // UA: "Шукаємо конфлікти вчителя. Ціль захоплена. Аналізуємо розклад... Конфлікт виявлено!"
        val teacherConflicts = scheduleEntryRepository.findTeacherConflicts(
            scheduleId, teacherId, dayOfWeek, startTime, endTime
        )
        if (teacherConflicts.isNotEmpty()) {
            conflicts.add(
                ConflictInfo(
                    type = ConflictType.TEACHER_CONFLICT,
                    message = "Teacher is already scheduled at this time",
                    conflictingEntries = teacherConflicts.map { it.toResponse() }
                )
            )
        }
        
        // Wheatley: "Checking if the classroom's free. Can't have two classes in the same room, can we? 
        // That'd be chaos! Well, more chaos than usual."
        // RU: "Проверяем, свободен ли класс. Не можем же мы иметь два класса в одной комнате, правда?
        // Это был бы хаос! Ну, больше хаоса, чем обычно."
        // UA: "Перевіряємо, чи вільний клас. Не можемо ж ми мати два класи в одній кімнаті, правда?
        // Це був би хаос! Ну, більше хаосу, ніж зазвичай."
        val classroomConflicts = scheduleEntryRepository.findClassroomConflicts(
            scheduleId, classroomId, dayOfWeek, startTime, endTime
        )
        if (classroomConflicts.isNotEmpty()) {
            conflicts.add(
                ConflictInfo(
                    type = ConflictType.CLASSROOM_CONFLICT,
                    message = "Classroom is already booked at this time",
                    conflictingEntries = classroomConflicts.map { it.toResponse() }
                )
            )
        }
        
        // Fact Core: "Students can't be in two places at once! That's a fact! Unless they have a time machine! 
        // Which they don't! Also a fact!"
        // RU: "Студенты не могут быть в двух местах одновременно! Это факт! Если только у них нет машины времени!
        // Которой у них нет! Тоже факт!"
        // UA: "Студенти не можуть бути в двох місцях одночасно! Це факт! Якщо тільки у них немає машини часу!
        // Якої у них немає! Теж факт!"
        val classConflicts = scheduleEntryRepository.findClassConflicts(
            scheduleId, classId, dayOfWeek, startTime, endTime
        )
        if (classConflicts.isNotEmpty()) {
            conflicts.add(
                ConflictInfo(
                    type = ConflictType.CLASS_CONFLICT,
                    message = "Class already has a lesson scheduled at this time",
                    conflictingEntries = classConflicts.map { it.toResponse() }
                )
            )
        }
        
        // Adventure Core: "Is the teacher available? Are they ready for the ADVENTURE of teaching? 
        // Let's check their availability and find out!"
        // RU: "Доступен ли учитель? Готов ли он к ПРИКЛЮЧЕНИЮ преподавания?
        // Давайте проверим его доступность и узнаем!"
        // UA: "Чи доступний вчитель? Чи готовий він до ПРИГОДИ викладання?
        // Давайте перевіримо його доступність і дізнаємося!"
        val isAvailable = teacherAvailabilityService.isTeacherAvailable(teacherId, dayOfWeek, startTime, endTime)
        if (!isAvailable) {
            conflicts.add(
                ConflictInfo(
                    type = ConflictType.TEACHER_UNAVAILABLE,
                    message = "Teacher is not available at this time",
                    conflictingEntries = emptyList()
                )
            )
        }
        
        // GLaDOS: "Returning validation results. If there are no conflicts, congratulations. 
        // You've managed to do something right. Don't let it go to your head."
        // RU: "Возвращаем результаты валидации. Если конфликтов нет, поздравляю.
        // Вам удалось сделать что-то правильно. Не позволяйте этому вскружить вам голову."
        // UA: "Повертаємо результати валідації. Якщо конфліктів немає, вітаю.
        // Вам вдалося зробити щось правильно. Не дозволяйте цьому закрутити вам голову."
        return ValidationResult(
            valid = conflicts.isEmpty(),
            conflicts = conflicts
        )
    }
    
    @Transactional
    fun updateScheduleEntry(scheduleId: UUID, entryId: UUID, request: CreateScheduleEntryRequest): ScheduleEntryResponse {
        val entry = scheduleEntryRepository.findById(entryId)
            .orElseThrow { IllegalArgumentException("Schedule entry not found with id: $entryId") }
        
        if (entry.schedule.id != scheduleId) {
            throw IllegalArgumentException("Schedule entry does not belong to schedule: $scheduleId")
        }
        
        val dayOfWeek = DayOfWeek.valueOf(request.dayOfWeek.uppercase())
        val startTime = LocalTime.parse(request.startTime)
        val endTime = LocalTime.parse(request.endTime)
        
        // Validate the updated entry (excluding the current entry from conflict checks)
        scheduleEntryRepository.delete(entry)
        
        val validation = validateScheduleEntry(
            scheduleId = scheduleId,
            classId = request.classId,
            teacherId = request.teacherId,
            classroomId = request.classroomId,
            dayOfWeek = dayOfWeek,
            startTime = startTime,
            endTime = endTime
        )
        
        if (!validation.valid) {
            // Restore the entry if validation fails
            scheduleEntryRepository.save(entry)
            throw ScheduleConflictException("Schedule entry validation failed", validation.conflicts)
        }
        
        val updated = entry.copy(
            classId = request.classId,
            subjectId = request.subjectId,
            teacherId = request.teacherId,
            classroomId = request.classroomId,
            dayOfWeek = dayOfWeek,
            startTime = startTime,
            endTime = endTime
        )
        
        val saved = scheduleEntryRepository.save(updated)
        
        eventPublisher.publish(
            ScheduleModifiedEvent(
                aggregateId = scheduleId,
                scheduleEntryId = saved.id,
                classId = saved.classId,
                subjectId = saved.subjectId,
                teacherId = saved.teacherId,
                classroomId = saved.classroomId,
                dayOfWeek = saved.dayOfWeek,
                startTime = saved.startTime,
                endTime = saved.endTime,
                modifiedBy = UUID.randomUUID() // TODO: Get from security context
            )
        )
        
        return saved.toResponse()
    }
    
    @Transactional
    fun deleteScheduleEntry(scheduleId: UUID, entryId: UUID) {
        val entry = scheduleEntryRepository.findById(entryId)
            .orElseThrow { IllegalArgumentException("Schedule entry not found with id: $entryId") }
        
        if (entry.schedule.id != scheduleId) {
            throw IllegalArgumentException("Schedule entry does not belong to schedule: $scheduleId")
        }
        
        val entryData = entry.copy() // Save data before deletion
        scheduleEntryRepository.delete(entry)
        
        eventPublisher.publish(
            ScheduleModifiedEvent(
                aggregateId = scheduleId,
                scheduleEntryId = entryData.id,
                classId = entryData.classId,
                subjectId = entryData.subjectId,
                teacherId = entryData.teacherId,
                classroomId = entryData.classroomId,
                dayOfWeek = entryData.dayOfWeek,
                startTime = entryData.startTime,
                endTime = entryData.endTime,
                modifiedBy = UUID.randomUUID() // TODO: Get from security context
            )
        )
    }
    
    @Transactional
    fun updateScheduleStatus(scheduleId: UUID, status: ScheduleStatus): ScheduleResponse {
        val schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow { IllegalArgumentException("Schedule not found with id: $scheduleId") }
        
        val updated = schedule.copy(status = status)
        val saved = scheduleRepository.save(updated)
        
        val entries = scheduleEntryRepository.findByScheduleId(scheduleId)
        return saved.toResponse(entries)
    }
    
    private fun Schedule.toResponse(entries: List<ScheduleEntry> = emptyList()) = ScheduleResponse(
        id = id,
        academicYearId = academicYearId,
        termId = termId,
        status = status,
        entries = entries.map { it.toResponse() }
    )
    
    private fun ScheduleEntry.toResponse() = ScheduleEntryResponse(
        id = id,
        scheduleId = schedule.id,
        classId = classId,
        subjectId = subjectId,
        teacherId = teacherId,
        classroomId = classroomId,
        dayOfWeek = dayOfWeek.name,
        startTime = startTime.toString(),
        endTime = endTime.toString()
    )
}

/**
 * Space Core: "Schedule conflict exception! CONFLICT IN SPAAAAAACE! Well, not space. In the schedule. 
 * But it's still a conflict! CONFLICT!"
 * RU: "Исключение конфликта расписания! КОНФЛИКТ В КООООСМОСЕ! Ну, не в космосе. В расписании.
 * Но это всё равно конфликт! КОНФЛИКТ!"
 * UA: "Виняток конфлікту розкладу! КОНФЛІКТ В КООООСМОСІ! Ну, не в космосі. В розкладі.
 * Але це все одно конфлікт! КОНФЛІКТ!"
 */
class ScheduleConflictException(
    message: String,
    val conflicts: List<ConflictInfo>
) : RuntimeException(message)
