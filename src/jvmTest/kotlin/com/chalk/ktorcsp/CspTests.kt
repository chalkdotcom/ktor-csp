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
import kotlin.test.assertNull

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

fun Application.routingModule() {
    install(CSP) {
        allRequests = false
        defaultConfig {
            directive("default-src")(self, "https://www.google.com")
            directive("img-src")(none)
        }
    }

    routing {
        csp {
            get("/default") { call.respondText("Default") }

            get("/callAppend") {
                call.appendCsp(extendDefault = false) {
                    directive("style-src")(self)
                }
                call.respondText("Call Append")
            }
        }

        csp(extendDefault = true, configure = {
            directive("style-src")(self)
        }) {
            get("/extend") { call.respondText("Extend") }
        }

        csp(extendDefault = false, configure = {
            directive("style-src")(self)
        }) {
            get("/scratch") { call.respondText("Scratch") }
        }

        get("/disabled") { call.respondText("Disabled") }

        get("/callDefault") {
            call.appendCsp()
            call.respondText("Call Default")
        }

        get("/callExtend") {
            call.appendCsp {
                directive("style-src")(self)
            }
            call.respondText("Call Default")
        }

        get("/callScratch") {
            call.appendCsp(extendDefault = false) {
                directive("style-src")(self)
            }
            call.respondText("Call Scratch")
        }
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

    @Test
    fun serverRoutingDisabled() {
        withTestApplication(Application::routingModule) {
            handleRequest(HttpMethod.Get, "/disabled").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertNull(response.headers["Content-Security-Policy"])
            }
        }
    }

    @Test
    fun serverRoutingDefault() {
        withTestApplication(Application::routingModule) {
            handleRequest(HttpMethod.Get, "/default").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(response.headers["Content-Security-Policy"], "default-src 'self' https://www.google.com; img-src 'none'")
            }
        }
    }

    @Test
    fun serverRoutingExtend() {
        withTestApplication(Application::routingModule) {
            handleRequest(HttpMethod.Get, "/extend").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(response.headers["Content-Security-Policy"], "default-src 'self' https://www.google.com; img-src 'none'; style-src 'self'")
            }
        }
    }

    @Test
    fun serverRoutingScratch() {
        withTestApplication(Application::routingModule) {
            handleRequest(HttpMethod.Get, "/scratch").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(response.headers["Content-Security-Policy"], "style-src 'self'")
            }
        }
    }

    @Test
    fun serverCallDefault() {
        withTestApplication(Application::routingModule) {
            handleRequest(HttpMethod.Get, "/callDefault").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(response.headers["Content-Security-Policy"], "default-src 'self' https://www.google.com; img-src 'none'")
            }
        }
    }

    @Test
    fun serverCallExtend() {
        withTestApplication(Application::routingModule) {
            handleRequest(HttpMethod.Get, "/callExtend").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(response.headers["Content-Security-Policy"], "default-src 'self' https://www.google.com; img-src 'none'; style-src 'self'")
            }
        }
    }

    @Test
    fun serverCallScratch() {
        withTestApplication(Application::routingModule) {
            handleRequest(HttpMethod.Get, "/callScratch").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(response.headers["Content-Security-Policy"], "style-src 'self'")
            }
        }
    }

    @Test
    fun serverCallAppend() {
        withTestApplication(Application::routingModule) {
            handleRequest(HttpMethod.Get, "/callAppend").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val headers = response.headers.values("Content-Security-Policy")
                assertEquals(2, headers.size)
                assertEquals(headers[0], "default-src 'self' https://www.google.com; img-src 'none'")
                assertEquals(headers[1], "style-src 'self'")
            }
        }
    }
}
