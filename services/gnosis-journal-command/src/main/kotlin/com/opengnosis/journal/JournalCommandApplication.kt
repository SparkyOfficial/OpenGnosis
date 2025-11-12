package com.opengnosis.journal

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.opengnosis.journal", "com.opengnosis.common"])
class JournalCommandApplication

fun main(args: Array<String>) {
    runApplication<JournalCommandApplication>(*args)
}
