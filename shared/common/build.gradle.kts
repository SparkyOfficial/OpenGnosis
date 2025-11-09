plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    implementation(project(":shared:domain"))
    implementation(project(":shared:events"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter:3.2.0")
    implementation("org.springframework.boot:spring-boot-starter-web:3.2.0")
    implementation("org.springframework.boot:spring-boot-starter-data-redis:3.2.0")
    implementation("org.springframework.boot:spring-boot-starter-cache:3.2.0")
    
    // Redis
    implementation("io.lettuce:lettuce-core:6.3.0.RELEASE")
    
    // Elasticsearch
    implementation("co.elastic.clients:elasticsearch-java:8.11.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")
    
    // Kafka
    implementation("org.springframework.kafka:spring-kafka:3.1.0")
    
    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")
    
    // Jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.0")
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.2.0")
}
