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
	// Seguridad: valida el JWT emitido por Cognito (resource server)
	implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("tools.jackson.module:jackson-module-kotlin")

	// La base de datos es PostgreSQL, una por microservicio. Nada de H2 en produccion.
	runtimeOnly("org.postgresql:postgresql")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	// Para fabricar JWTs falsos con jwt() en los tests, sin depender de AWS.
	testImplementation("org.springframework.security:spring-security-test")

	// Base en memoria SOLO para los tests de integracion con JPA (nunca en runtime).
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

// Lo que la rubrica permite dejar fuera de la cuenta de cobertura: clases de
// configuracion, DTOs sin logica, entidades sin comportamiento y la clase Application.
val coverageExclusions = listOf(
	"com/pucetec/users/UsersApplication*",
	"com/pucetec/users/config/**",
	"com/pucetec/users/dto/**",
	"com/pucetec/users/entities/**",
	"com/pucetec/users/audit/AuditLog*",
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
