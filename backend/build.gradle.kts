plugins {
    java
    id("org.springframework.boot") version "4.0.3" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

val springCloudVersion = "2025.1.1"
val lombokVersion = "1.18.42"
val mapstructVersion = "1.6.3"

allprojects {
    group = "com.krypto"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    dependencies {
        compileOnly("org.projectlombok:lombok:$lombokVersion")
        annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

val serviceProjects = listOf(
    "discovery-service",
    "config-service",
    "api-gateway",
    "user-service",
    "wallet-service",
    "coin-service",
    "trading-service",
    "blockchain-service"
)

configure(subprojects.filter { it.name in serviceProjects }) {
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
        imports {
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
        }
    }

    dependencies {
        "implementation"("org.springframework.boot:spring-boot-starter-actuator")
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }
}

val configClientProjects = listOf(
    "api-gateway",
    "user-service",
    "wallet-service",
    "coin-service",
    "trading-service",
    "blockchain-service"
)

configure(subprojects.filter { it.name in configClientProjects }) {
    dependencies {
        "implementation"("org.springframework.cloud:spring-cloud-starter-config")
    }
}
