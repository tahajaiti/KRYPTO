plugins {
    `java-library`
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:2.18.3")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.3")
    api("jakarta.persistence:jakarta.persistence-api:3.2.0")
    api("jakarta.servlet:jakarta.servlet-api:6.1.0")
    api("org.springframework:spring-web:7.0.0")
    api("org.springframework.security:spring-security-core:7.0.0")
    api("org.springframework.security:spring-security-web:7.0.0")
    api("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")
}
