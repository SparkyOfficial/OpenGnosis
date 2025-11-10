package com.opengnosis.iam

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.opengnosis"])
class GnosisIamApplication

fun main(args: Array<String>) {
    runApplication<GnosisIamApplication>(*args)
}
