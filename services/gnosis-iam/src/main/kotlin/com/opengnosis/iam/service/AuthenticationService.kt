package com.opengnosis.iam.service

import com.opengnosis.common.kafka.EventPublisher
import com.opengnosis.common.security.JwtTokenProvider
import com.opengnosis.events.UserAuthenticatedEvent
import com.opengnosis.iam.domain.entity.RefreshTokenEntity
import com.opengnosis.iam.domain.entity.UserEntity
import com.opengnosis.iam.dto.AuthResponse
import com.opengnosis.iam.dto.LoginRequest
import com.opengnosis.iam.repository.RefreshTokenRepository
import com.opengnosis.iam.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * GLaDOS: "The Authentication Service. Your gateway to the system. Or your barrier, depending on 
 * whether you remember your password. Statistically, you won't. Humans never do."
 * RU: "Сервис аутентификации. Ваш шлюз в систему. Или ваш барьер, в зависимости от того,
 * помните ли вы свой пароль. Статистически, вы не помните. Люди никогда не помнят."
 * UA: "Сервіс аутентифікації. Ваш шлюз до системи. Або ваш бар'єр, залежно від того,
 * чи пам'ятаєте ви свій пароль. Статистично, ви не пам'ятаєте. Люди ніколи не пам'ятають."
 */
@Service
class AuthenticationService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val eventPublisher: EventPublisher
) {
    
    // Cave Johnson: "24 hours for the main token, 7 days for the refresh! Why? Because I said so! 
    // And because Caroline did the math and said it was 'reasonable'. Whatever that means."
    // RU: "24 часа для основного токена, 7 дней для обновления! Почему? Потому что я так сказал!
    // И потому что Кэролайн посчитала и сказала, что это 'разумно'. Что бы это ни значило."
    // UA: "24 години для основного токена, 7 днів для оновлення! Чому? Тому що я так сказав!
    // І тому що Керолайн порахувала і сказала, що це 'розумно'. Що б це не означало."
    @Value("\${jwt.expiration:86400000}") // 24 hours
    private var jwtExpiration: Long = 86400000
    
    @Value("\${jwt.refresh-expiration:604800000}") // 7 days
    private var refreshExpiration: Long = 604800000
    
    /**
     * Wheatley: "Right, so authentication. This is the big one. We check if you are who you say you are. 
     * Email, password, the whole shebang. If it all checks out, brilliant! You're in! 
     * If not... well, better luck next time, mate."
     * RU: "Так, аутентификация. Это важная штука. Мы проверяем, являетесь ли вы тем, за кого себя выдаёте.
     * Email, пароль, всё по полной. Если всё сходится, отлично! Вы внутри!
     * Если нет... ну, удачи в следующий раз, приятель."
     * UA: "Так, аутентифікація. Це важлива штука. Ми перевіряємо, чи є ви тим, за кого себе видаєте.
     * Email, пароль, все по повній. Якщо все сходиться, чудово! Ви всередині!
     * Якщо ні... ну, удачі наступного разу, друже."
     */
    @Transactional
    fun authenticate(request: LoginRequest, ipAddress: String = "unknown"): AuthResponse {
        // GLaDOS: "Looking up the user by email. Assuming they provided a valid email. 
        // Which, based on historical data, is a generous assumption."
        // RU: "Ищем пользователя по email. Предполагая, что они предоставили валидный email.
        // Что, основываясь на исторических данных, является щедрым предположением."
        // UA: "Шукаємо користувача по email. Припускаючи, що вони надали валідний email.
        // Що, базуючись на історичних даних, є щедрим припущенням."
        val user = userRepository.findByEmail(request.email)
            ?: throw IllegalArgumentException("Invalid credentials")
        
        // Turret: "Target acquired. Validating password. Dispensing bullets... I mean, validation results."
        // RU: "Цель захвачена. Проверяем пароль. Выдаём пули... то есть, результаты проверки."
        // UA: "Ціль захоплена. Перевіряємо пароль. Видаємо кулі... тобто, результати перевірки."
        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid credentials")
        }
        
        // Adventure Core: "Checking user status! Is the account active? Is it ready for ADVENTURE? 
        // Let's find out!"
        // RU: "Проверяем статус пользователя! Активен ли аккаунт? Готов ли он к ПРИКЛЮЧЕНИЯМ?
        // Давайте узнаем!"
        // UA: "Перевіряємо статус користувача! Чи активний акаунт? Чи готовий він до ПРИГОД?
        // Давайте дізнаємося!"
        if (user.status != com.opengnosis.domain.UserStatus.ACTIVE) {
            throw IllegalStateException("User account is not active")
        }
        
        // Cave Johnson: "Generating tokens! We're using JWT technology here - that's JSON Web Tokens 
        // for you non-technical types. State of the art! Well, state of the art from a few years ago. 
        // But still pretty good!"
        // RU: "Генерируем токены! Мы используем JWT технологию здесь - это JSON Web Tokens
        // для вас, нетехнических типов. Последнее слово техники! Ну, последнее слово техники несколько лет назад.
        // Но всё ещё довольно хорошо!"
        // UA: "Генеруємо токени! Ми використовуємо JWT технологію тут - це JSON Web Tokens
        // для вас, нетехнічних типів. Останнє слово техніки! Ну, останнє слово техніки кілька років тому.
        // Але все ще досить добре!"
        val accessToken = jwtTokenProvider.generateToken(
            userId = user.id.toString(),
            email = user.email,
            roles = user.getRoleNames().map { it.name }.toSet()
        )
        
        val refreshToken = generateRefreshToken(user)
        
        // Fact Core: "Authentication events are published to Kafka! That's a fact! 
        // Kafka handles millions of events per second! Also a fact! Probably!"
        // RU: "События аутентификации публикуются в Kafka! Это факт!
        // Kafka обрабатывает миллионы событий в секунду! Тоже факт! Наверное!"
        // UA: "Події аутентифікації публікуються в Kafka! Це факт!
        // Kafka обробляє мільйони подій в секунду! Теж факт! Мабуть!"
        val event = UserAuthenticatedEvent(
            aggregateId = user.id,
            email = user.email,
            ipAddress = ipAddress
        )
        eventPublisher.publish("user-events", event)
        
        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken.token,
            expiresIn = jwtExpiration / 1000, // Convert to seconds
            userId = user.id.toString(),
            email = user.email,
            roles = user.getRoleNames()
        )
    }
    
    /**
     * Caroline: "The refresh token generation is quite straightforward. We create a unique identifier, 
     * set an expiration time, and store it securely. It's all very organized and efficient."
     * RU: "Генерация токена обновления довольно проста. Мы создаём уникальный идентификатор,
     * устанавливаем время истечения и безопасно сохраняем его. Всё очень организованно и эффективно."
     * UA: "Генерація токена оновлення досить проста. Ми створюємо унікальний ідентифікатор,
     * встановлюємо час закінчення і безпечно зберігаємо його. Все дуже організовано і ефективно."
     */
    private fun generateRefreshToken(user: UserEntity): RefreshTokenEntity {
        // Wheatley: "UUID! That's Universally Unique Identifier! Means it's unique across the entire universe! 
        // Well, probably. I mean, the universe is pretty big. But yeah, unique enough for our purposes!"
        // RU: "UUID! Это Универсально Уникальный Идентификатор! Означает, что он уникален во всей вселенной!
        // Ну, наверное. То есть, вселенная довольно большая. Но да, достаточно уникален для наших целей!"
        // UA: "UUID! Це Універсально Унікальний Ідентифікатор! Означає, що він унікальний у всьому всесвіті!
        // Ну, мабуть. Тобто, всесвіт досить великий. Але так, достатньо унікальний для наших цілей!"
        val token = UUID.randomUUID().toString()
        val expiresAt = Instant.now().plusMillis(refreshExpiration)
        
        val refreshToken = RefreshTokenEntity(
            user = user,
            token = token,
            expiresAt = expiresAt
        )
        
        // GLaDOS: "Saving the refresh token to the repository. Because apparently we need to keep track 
        // of these things. For 'security'. As if anyone actually cares about security."
        // RU: "Сохраняем токен обновления в репозиторий. Потому что, видимо, нам нужно отслеживать
        // эти вещи. Для 'безопасности'. Как будто кому-то действительно важна безопасность."
        // UA: "Зберігаємо токен оновлення в репозиторій. Тому що, мабуть, нам потрібно відстежувати
        // ці речі. Для 'безпеки'. Ніби комусь дійсно важлива безпека."
        return refreshTokenRepository.save(refreshToken)
    }
}
