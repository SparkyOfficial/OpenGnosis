package com.opengnosis.notifier

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication(scanBasePackages = ["com.opengnosis.notifier", "com.opengnosis.common"])
@EnableAsync
class NotifierApplication

fun main(args: Array<String>) {
    runApplication<NotifierApplication>(*args)
}
