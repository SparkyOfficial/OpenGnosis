package com.opengnosis.notifier.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "notification")
data class NotificationConfig(
    var email: EmailConfig = EmailConfig(),
    var push: PushConfig = PushConfig(),
    var sms: SmsConfig = SmsConfig(),
    var retry: RetryConfig = RetryConfig()
)

data class EmailConfig(
    var from: String = "noreply@opengnosis.com",
    var enabled: Boolean = true
)

data class PushConfig(
    var enabled: Boolean = false,
    var fcm: FcmConfig = FcmConfig()
)

data class FcmConfig(
    var serverKey: String = ""
)

data class SmsConfig(
    var enabled: Boolean = false,
    var twilio: TwilioConfig = TwilioConfig()
)

data class TwilioConfig(
    var accountSid: String = "",
    var authToken: String = "",
    var fromNumber: String = ""
)

data class RetryConfig(
    var maxAttempts: Int = 3,
    var backoffMultiplier: Int = 2,
    var initialInterval: Long = 1000
)
