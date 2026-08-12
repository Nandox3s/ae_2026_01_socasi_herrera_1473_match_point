plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "4.0.6"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "2.2.21"
	jacoco
}

group = "com.pucetec"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("tools.jackson.module:jackson-module-kotlin")

	runtimeOnly("org.postgresql:postgresql")

	implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	testImplementation("org.springframework.security:spring-security-test")

	testRuntimeOnly("com.h2database:h2")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
	finalizedBy(tasks.jacocoTestReport)
}

val coverageExclusions = listOf(
	"com/pucetec/matchpoint/MatchpointApplication*",
	"com/pucetec/matchpoint/config/**",
	"com/pucetec/matchpoint/dto/**",
	"com/pucetec/matchpoint/entities/**",
	"com/pucetec/matchpoint/enums/**",
	"com/pucetec/matchpoint/audit/AuditLog*",
	"com/pucetec/matchpoint/clients/UserProfile*",
)

tasks.jacocoTestReport {
	dependsOn(tasks.test)
	reports {
		html.required = true
		xml.required = true
	}
	classDirectories.setFrom(
		files(classDirectories.files.map { fileTree(it) { exclude(coverageExclusions) } })
	)
}

tasks.jacocoTestCoverageVerification {
	violationRules {
		rule {
			limit { counter = "LINE"; minimum = "1.00".toBigDecimal() }
		}
	}
	classDirectories.setFrom(
		files(classDirectories.files.map { fileTree(it) { exclude(coverageExclusions) } })
	)
}

tasks.check { dependsOn(tasks.jacocoTestCoverageVerification) }
