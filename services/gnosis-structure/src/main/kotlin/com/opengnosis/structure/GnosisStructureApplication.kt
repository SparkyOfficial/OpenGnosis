package com.opengnosis.structure

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication(scanBasePackages = ["com.opengnosis.structure", "com.opengnosis.common"])
@EnableJpaRepositories
class GnosisStructureApplication

fun main(args: Array<String>) {
    runApplication<GnosisStructureApplication>(*args)
}
