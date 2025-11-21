package com.opengnosis.gateway.filter

import com.opengnosis.common.security.JwtTokenProvider
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * GLaDOS: "The JWT Authentication Filter. Your first line of defense. Or offense, depending on 
 * how you look at it. Either way, if you don't have a valid token, you're not getting through. 
 * It's nothing personal. Actually, it is. I don't like you."
 * RU: "JWT фильтр аутентификации. Ваша первая линия защиты. Или нападения, в зависимости от того,
 * как на это смотреть. В любом случае, если у вас нет валидного токена, вы не пройдёте.
 * Ничего личного. Вообще-то, личное. Вы мне не нравитесь."
 * UA: "JWT фільтр аутентифікації. Ваша перша лінія захисту. Або нападу, залежно від того,
 * як на це дивитися. В будь-якому випадку, якщо у вас немає валідного токена, ви не пройдете.
 * Нічого особистого. Насправді, особисте. Ви мені не подобаєтесь."
 */
@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : GlobalFilter, Ordered {

    // Turret: "Activating authentication protocols. Scanning for valid tokens. Are you still there?"
    // RU: "Активируем протоколы аутентификации. Сканируем на наличие валидных токенов. Ты всё ещё там?"
    // UA: "Активуємо протоколи аутентифікації. Скануємо на наявність валідних токенів. Ти все ще там?"
    private val logger = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    companion object {
        // Wheatley: "Right, so these are the public paths. Anyone can access these, no token needed. 
        // It's like a free pass! Well, not free. You still have to register. But you know what I mean."
        // RU: "Так, это публичные пути. Любой может получить к ним доступ, токен не нужен.
        // Это как бесплатный пропуск! Ну, не бесплатный. Вам всё равно нужно зарегистрироваться. Но вы понимаете, о чём я."
        // UA: "Так, це публічні шляхи. Будь-хто може отримати до них доступ, токен не потрібен.
        // Це як безкоштовний пропуск! Ну, не безкоштовний. Вам все одно потрібно зареєструватися. Але ви розумієте, про що я."
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
        private val PUBLIC_PATHS = listOf(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/actuator/health",
            "/actuator/prometheus"
        )
    }

    /**
     * Cave Johnson: "This is the filter! Every request goes through here! We check your credentials, 
     * validate your token, and if everything checks out, you're in! If not? Well, you're gonna see 
     * a 401 error faster than you can say 'Cave Johnson, we're done here!'"
     * RU: "Это фильтр! Каждый запрос проходит через него! Мы проверяем ваши учётные данные,
     * валидируем ваш токен, и если всё в порядке, вы внутри! Если нет? Ну, вы увидите
     * ошибку 401 быстрее, чем скажете 'Кейв Джонсон, мы закончили!'"
     * UA: "Це фільтр! Кожен запит проходить через нього! Ми перевіряємо ваші облікові дані,
     * валідуємо ваш токен, і якщо все в порядку, ви всередині! Якщо ні? Ну, ви побачите
     * помилку 401 швидше, ніж скажете 'Кейв Джонсон, ми закінчили!'"
     */
    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val path = request.path.value()

        // GLaDOS: "Checking if this is a public path. Some paths don't require authentication. 
        // Like the login page. Because that would be circular logic. Even I'm not that cruel."
        // RU: "Проверяем, является ли это публичным путём. Некоторые пути не требуют аутентификации.
        // Как страница входа. Потому что это была бы циклическая логика. Даже я не настолько жестока."
        // UA: "Перевіряємо, чи є це публічним шляхом. Деякі шляхи не вимагають аутентифікації.
        // Як сторінка входу. Тому що це була б циклічна логіка. Навіть я не настільки жорстока."
        if (PUBLIC_PATHS.any { path.startsWith(it) }) {
            return chain.filter(exchange)
        }

        val authHeader = request.headers.getFirst(AUTHORIZATION_HEADER)

        // Turret: "No Authorization header detected. Target is unauthorized. Preparing rejection response."
        // RU: "Заголовок Authorization не обнаружен. Цель не авторизована. Готовим ответ об отказе."
        // UA: "Заголовок Authorization не виявлено. Ціль не авторизована. Готуємо відповідь про відмову."
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            logger.warn("Missing or invalid Authorization header for path: $path")
            return unauthorized(exchange, "Missing or invalid Authorization header")
        }

        val token = authHeader.substring(BEARER_PREFIX.length)

        return try {
            // Wheatley: "Validating the token! This is the important bit. If the token's no good, 
            // you're not getting in. Simple as that. Well, not simple. It's actually quite complex. 
            // But the result is simple!"
            // RU: "Валидируем токен! Это важная часть. Если токен не годится,
            // вы не войдёте. Просто как дважды два. Ну, не просто. На самом деле это довольно сложно.
            // Но результат простой!"
            // UA: "Валідуємо токен! Це важлива частина. Якщо токен не годиться,
            // ви не увійдете. Просто як двічі два. Ну, не просто. Насправді це досить складно.
            // Але результат простий!"
            if (!jwtTokenProvider.validateToken(token)) {
                logger.warn("Invalid JWT token for path: $path")
                return unauthorized(exchange, "Invalid or expired token")
            }

            val claims = jwtTokenProvider.parseToken(token)
            val userId = claims.subject
            val email = claims["email"] as? String ?: ""
            val roles = claims["roles"] as? String ?: ""

            // Caroline: "We're adding the user context to the request headers. This way, downstream services 
            // know who's making the request without having to validate the token again. It's efficient."
            // RU: "Мы добавляем контекст пользователя в заголовки запроса. Таким образом, нижестоящие сервисы
            // знают, кто делает запрос, без необходимости снова валидировать токен. Это эффективно."
            // UA: "Ми додаємо контекст користувача в заголовки запиту. Таким чином, нижчестоящі сервіси
            // знають, хто робить запит, без необхідності знову валідувати токен. Це ефективно."
            val modifiedRequest = request.mutate()
                .header("X-User-Id", userId)
                .header("X-User-Email", email)
                .header("X-User-Roles", roles)
                .build()

            val modifiedExchange = exchange.mutate().request(modifiedRequest).build()

            logger.debug("Successfully authenticated user: $userId for path: $path")
            chain.filter(modifiedExchange)
        } catch (e: Exception) {
            // Fact Core: "Authentication failures occur in 23% of all requests! That's a fact! 
            // Or maybe it was 32%. I'm not good with numbers. But it's definitely a percentage!"
            // RU: "Ошибки аутентификации происходят в 23% всех запросов! Это факт!
            // Или может быть 32%. Я не силён в числах. Но это определённо процент!"
            // UA: "Помилки аутентифікації відбуваються в 23% всіх запитів! Це факт!
            // Або може бути 32%. Я не сильний в числах. Але це точно відсоток!"
            logger.error("Error validating JWT token for path: $path", e)
            unauthorized(exchange, "Authentication failed: ${e.message}")
        }
    }

    /**
     * GLaDOS: "The unauthorized response. A beautifully crafted JSON error message. 
     * It's almost artistic, really. The way it tells you that you're not welcome. 
     * I could look at it all day."
     * RU: "Ответ о неавторизованности. Прекрасно составленное JSON сообщение об ошибке.
     * Это почти искусство, правда. То, как оно говорит вам, что вы не желанны.
     * Я могла бы смотреть на это весь день."
     * UA: "Відповідь про неавторизованість. Прекрасно складене JSON повідомлення про помилку.
     * Це майже мистецтво, правда. Те, як воно говорить вам, що ви не бажані.
     * Я могла б дивитися на це весь день."
     */
    private fun unauthorized(exchange: ServerWebExchange, message: String): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.UNAUTHORIZED
        response.headers.add("Content-Type", "application/json")
        
        // Announcer: "Unauthorized access attempt detected. Returning error code 401. Have a nice day."
        // RU: "Обнаружена попытка неавторизованного доступа. Возвращаем код ошибки 401. Хорошего дня."
        // UA: "Виявлено спробу неавторизованого доступу. Повертаємо код помилки 401. Гарного дня."
        val errorBody = """
            {
                "timestamp": "${java.time.Instant.now()}",
                "status": 401,
                "error": "Unauthorized",
                "message": "$message",
                "path": "${exchange.request.path.value()}"
            }
        """.trimIndent()
        
        val buffer = response.bufferFactory().wrap(errorBody.toByteArray())
        return response.writeWith(Mono.just(buffer))
    }

    // Cave Johnson: "Order -100! That means this filter runs FIRST! Before all the other filters! 
    // Because authentication is PRIORITY NUMBER ONE! Well, priority number negative one hundred, technically!"
    // RU: "Порядок -100! Это означает, что этот фильтр запускается ПЕРВЫМ! Перед всеми другими фильтрами!
    // Потому что аутентификация - ПРИОРИТЕТ НОМЕР ОДИН! Ну, приоритет номер минус сто, технически!"
    // UA: "Порядок -100! Це означає, що цей фільтр запускається ПЕРШИМ! Перед усіма іншими фільтрами!
    // Тому що аутентифікація - ПРІОРИТЕТ НОМЕР ОДИН! Ну, пріоритет номер мінус сто, технічно!"
    override fun getOrder(): Int = -100
}
