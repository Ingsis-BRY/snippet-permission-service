plugins {
    id("com.ingsisbry.kotlin-quality") version "2.0.0"
    id("com.ingsisbry.spring-boot-application") version "2.0.0"
    id("com.ingsisbry.jpa") version "2.0.0"
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("com.h2database:h2")
}

repositories {
    mavenCentral()
}
