import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.20" apply false
    kotlin("plugin.spring") version "1.9.20" apply false
    kotlin("plugin.jpa") version "1.9.20" apply false
    id("org.springframework.boot") version "3.2.0" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    id("jacoco") apply false
    id("org.owasp.dependencycheck") version "9.0.9" apply false
}

allprojects {
    group = "com.opengnosis"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "jacoco")
    apply(plugin = "org.owasp.dependencycheck")

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "21"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        finalizedBy(tasks.named("jacocoTestReport"))
    }

    // Configure JaCoCo
    configure<JacocoPluginExtension> {
        toolVersion = "0.8.11"
    }

    tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.withType<Test>())
        
        reports {
            xml.required.set(true)
            html.required.set(true)
            csv.required.set(false)
        }
        
        classDirectories.setFrom(
            files(classDirectories.files.map {
                fileTree(it) {
                    exclude(
                        "**/config/**",
                        "**/entity/**",
                        "**/dto/**",
                        "**/model/**",
                        "**/*Application.class",
                        "**/*Application\$*.class"
                    )
                }
            })
        )
    }

    tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
        dependsOn(tasks.named("jacocoTestReport"))
        
        violationRules {
            rule {
                limit {
                    minimum = "0.80".toBigDecimal()
                }
            }
            
            rule {
                element = "CLASS"
                limit {
                    counter = "LINE"
                    value = "COVEREDRATIO"
                    minimum = "0.70".toBigDecimal()
                }
                
                excludes = listOf(
                    "*.config.*",
                    "*.entity.*",
                    "*.dto.*",
                    "*.model.*",
                    "*.*Application"
                )
            }
        }
        
        classDirectories.setFrom(
            files(classDirectories.files.map {
                fileTree(it) {
                    exclude(
                        "**/config/**",
                        "**/entity/**",
                        "**/dto/**",
                        "**/model/**",
                        "**/*Application.class",
                        "**/*Application\$*.class"
                    )
                }
            })
        )
    }

    // Create integration test source set
    sourceSets {
        create("integrationTest") {
            kotlin {
                compileClasspath += sourceSets["main"].output + sourceSets["test"].output
                runtimeClasspath += sourceSets["main"].output + sourceSets["test"].output
                srcDir("src/test/kotlin")
            }
            resources.srcDir("src/test/resources")
        }
    }

    // Integration test task
    tasks.register<Test>("integrationTest") {
        description = "Runs integration tests"
        group = "verification"
        
        testClassesDirs = sourceSets["integrationTest"].output.classesDirs
        classpath = sourceSets["integrationTest"].runtimeClasspath
        
        useJUnitPlatform {
            includeTags("integration")
        }
        
        shouldRunAfter(tasks.named("test"))
        
        // Ensure integration tests have more time
        systemProperty("junit.jupiter.execution.timeout.default", "5m")
    }

    tasks.named("check") {
        dependsOn(tasks.named("jacocoTestCoverageVerification"))
    }

    // Configure OWASP Dependency Check
    configure<org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension> {
        formats = listOf("HTML", "XML", "JSON")
        failBuildOnCVSS = 7.0f
        suppressionFile = "${rootProject.projectDir}/config/dependency-check-suppressions.xml"
        analyzers.apply {
            assemblyEnabled = false
            nuspecEnabled = false
            nodeEnabled = false
        }
    }
}
