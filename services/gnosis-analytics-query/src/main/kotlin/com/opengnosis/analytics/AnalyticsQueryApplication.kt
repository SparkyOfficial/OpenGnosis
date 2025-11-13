package com.opengnosis.analytics

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.opengnosis.analytics", "com.opengnosis.common"])
class AnalyticsQueryApplication

fun main(args: Array<String>) {
    runApplication<AnalyticsQueryApplication>(*args)
}
