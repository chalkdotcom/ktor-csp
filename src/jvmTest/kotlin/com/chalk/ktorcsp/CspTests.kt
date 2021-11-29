package com.chalk.ktorcsp

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import kotlin.test.Test
import kotlin.test.assertEquals

fun Application.module() {
    install(CSP) {
        defaultConfig {
            directive("default-src")(self, "https://www.google.com")
            directive("img-src")(none)
        }
    }

    routing {
        get("/default") { call.respondText("Default") }
    }
}

class CspTests {
    @Test
    fun serverDefault() {
        withTestApplication(Application::module) {
            handleRequest(HttpMethod.Get, "/default").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(response.headers["Content-Security-Policy"], "default-src 'self' https://www.google.com; img-src 'none'")
            }
        }
    }

    @Test
    fun server404() {
        withTestApplication(Application::module) {
            handleRequest(HttpMethod.Get, "/404").apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
                assertEquals(response.headers["Content-Security-Policy"], "default-src 'self' https://www.google.com; img-src 'none'")
            }
        }
    }
}
