package com.chalk.ktorcsp

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationResponse
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

fun Application.module() {
    install(CSP) {
        defaultConfig {
            directive("default-src")(self, "https://www.google.com")
            directive("img-src")(none)
            directive("test-src")(nonce)
        }
    }

    routing {
        get("/default") { call.respondText("Default") }
        get("/nonce") { call.respondText(call.cspNonce ?: "None") }
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

        csp(extendDefault = false, configure = {
            directive("default-src")(nonce)
        }) {
            get("/nonce") { call.respondText(call.cspNonce ?: "None") }
        }

        get("/disabled") { call.respondText("Disabled") }

        get("/disabledNonce") { call.respondText(call.cspNonce ?: "None") }

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

        get("/callNonce") {
            call.appendCsp(false) {
                directive("default-src")(nonce)
            }
            call.respondText(call.cspNonce ?: "None")
        }
    }
}

val TestApplicationResponse.cspHeader: String?
    get() = headers["Content-Security-Policy"]

val String.cspDirectives: List<String>
    get() = split("; ")

class CspTests {
    @Test
    fun serverDefault() {
        withTestApplication(Application::module) {
            handleRequest(HttpMethod.Get, "/default").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val cspHeader = response.cspHeader
                assertNotNull(cspHeader)
                val directives = cspHeader.cspDirectives
                assertContains(directives, "default-src 'self' https://www.google.com")
                assertContains(directives, "img-src 'none'")
            }
        }
    }

    @Test
    fun serverDefault404() {
        withTestApplication(Application::module) {
            handleRequest(HttpMethod.Get, "/404").apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
                val cspHeader = response.cspHeader
                assertNotNull(cspHeader)
                val directives = cspHeader.cspDirectives
                assertContains(directives, "default-src 'self' https://www.google.com")
                assertContains(directives, "img-src 'none'")
            }
        }
    }
    @Test
    fun serverDefaultNonce() {
        withTestApplication(Application::module) {
            handleRequest(HttpMethod.Get, "/nonce").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val cspHeader = response.cspHeader
                assertNotNull(cspHeader)
                assertNotEquals("None", response.content)
                assertContains(cspHeader, "nonce-${response.content}")
            }
        }
    }

    @Test
    fun serverRoutingDisabled() {
        withTestApplication(Application::routingModule) {
            handleRequest(HttpMethod.Get, "/disabled").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertNull(response.cspHeader)
            }
        }
    }

    @Test
    fun serverRoutingDisabledNonce() {
        withTestApplication(Application::routingModule) {
            handleRequest(HttpMethod.Get, "/disabledNonce").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertNull(response.cspHeader)
                assertEquals("None", response.content)
            }
        }
    }

    @Test
    fun serverRoutingDefault() {
        withTestApplication(Application::routingModule) {
            handleRequest(HttpMethod.Get, "/default").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val cspHeader = response.cspHeader
                assertNotNull(cspHeader)
                val directives = cspHeader.cspDirectives
                assertContains(directives, "default-src 'self' https://www.google.com")
                assertContains(directives, "img-src 'none'")
            }
        }
    }

    @Test
    fun serverRoutingExtend() {
        withTestApplication(Application::routingModule) {
            handleRequest(HttpMethod.Get, "/extend").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(response.cspHeader, "default-src 'self' https://www.google.com; img-src 'none'; style-src 'self'")
                val cspHeader = response.cspHeader
                assertNotNull(cspHeader)
                val directives = cspHeader.cspDirectives
                assertContains(directives, "default-src 'self' https://www.google.com")
                assertContains(directives, "img-src 'none'")
                assertContains(directives, "style-src 'self'")
            }
        }
    }

    @Test
    fun serverRoutingScratch() {
        withTestApplication(Application::routingModule) {
            handleRequest(HttpMethod.Get, "/scratch").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(response.cspHeader, "style-src 'self'")
            }
        }
    }

    @Test
    fun serverRoutingNonce() {
        withTestApplication(Application::routingModule) {
            handleRequest(HttpMethod.Get, "/nonce").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertNotEquals("None", response.content)
                assertEquals(response.cspHeader, "default-src nonce-${response.content}")
            }
        }
    }

    @Test
    fun serverCallDefault() {
        withTestApplication(Application::routingModule) {
            handleRequest(HttpMethod.Get, "/callDefault").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val cspHeader = response.cspHeader
                assertNotNull(cspHeader)
                val directives = cspHeader.cspDirectives
                assertContains(directives, "default-src 'self' https://www.google.com")
                assertContains(directives, "img-src 'none'")
            }
        }
    }

    @Test
    fun serverCallExtend() {
        withTestApplication(Application::routingModule) {
            handleRequest(HttpMethod.Get, "/callExtend").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val cspHeader = response.cspHeader
                assertNotNull(cspHeader)
                val directives = cspHeader.cspDirectives
                assertContains(directives, "default-src 'self' https://www.google.com")
                assertContains(directives, "img-src 'none'")
                assertContains(directives, "style-src 'self'")
            }
        }
    }

    @Test
    fun serverCallScratch() {
        withTestApplication(Application::routingModule) {
            handleRequest(HttpMethod.Get, "/callScratch").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(response.cspHeader, "style-src 'self'")
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
                val firstDirectives = headers[0].cspDirectives
                assertContains(firstDirectives, "default-src 'self' https://www.google.com")
                assertContains(firstDirectives, "img-src 'none'")
                assertEquals(headers[1], "style-src 'self'")
            }
        }
    }

    @Test
    fun serverCallNonce() {
        withTestApplication(Application::routingModule) {
            handleRequest(HttpMethod.Get, "/nonce").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertNotEquals("None", response.content)
                assertEquals(response.cspHeader, "default-src nonce-${response.content}")
            }
        }
    }
}
