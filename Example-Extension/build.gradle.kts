plugins {
    id("java")
}

group = "schodan"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("net.portswigger.burp.extensions:montoya-api:2025.12")
}