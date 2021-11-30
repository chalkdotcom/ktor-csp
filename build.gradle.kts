plugins {
    kotlin("multiplatform") version "1.6.0"
    id("convention.publication")
}

group = ext["GROUP"]!!
version = ext["VERSION"]!!

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                compileOnly("io.ktor:ktor-server-core:1.6.6")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("io.ktor:ktor-server-test-host:1.6.6")
            }
        }
    }
}
