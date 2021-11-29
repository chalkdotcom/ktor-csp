package com.chalk.ktorcsp

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.application.feature
import io.ktor.response.header
import io.ktor.routing.Route
import io.ktor.routing.RouteSelector
import io.ktor.routing.RouteSelectorEvaluation
import io.ktor.routing.RoutingResolveContext
import io.ktor.util.AttributeKey
import java.util.Base64
import kotlin.random.Random

class CSP(val configuration: Configuration) {
    class Configuration {
        internal var configure: CspConfig.(ApplicationCall) -> Unit = {}
        var allRequests = true

        fun defaultConfig(configure: CspConfig.(ApplicationCall) -> Unit) {
            this.configure = configure
        }
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, CSP> {
        override val key = AttributeKey<CSP>("CSP")

        val ConfigKey = AttributeKey<CspConfig>("CSPConfig")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): CSP {
            val configuration = Configuration().apply(configure)
            val feature = CSP(configuration)

            if (configuration.allRequests) {
                pipeline.intercept(ApplicationCallPipeline.Features) {
                    call.appendCsp(true)
                }
            }

            return feature
        }
    }
}

fun Route.csp(
    extendDefault: Boolean,
    configure: (CspConfig.(call: ApplicationCall) -> Unit)? = null,
    build: Route.() -> Unit,
): Route {
    val cspRoute = createChild(object : RouteSelector() {
        override fun evaluate(context: RoutingResolveContext, segmentIndex: Int) = RouteSelectorEvaluation.Constant
    })

    cspRoute.intercept(ApplicationCallPipeline.Features) {
        call.appendCsp(extendDefault, configure)
    }

    cspRoute.build()

    return cspRoute
}

fun Route.csp(
    configure: (CspConfig.(call: ApplicationCall) -> Unit)? = null,
    build: Route.() -> Unit,
) = csp(extendDefault = true, configure, build)

fun ApplicationCall.appendCsp(
    extendDefault: Boolean = true,
    configure: (CspConfig.(ApplicationCall) -> Unit)? = null,
): CspConfig {
    val defaultConfigure = application.feature(CSP).configuration.configure
    val config = CspConfig(Base64.getEncoder().encodeToString(Random.nextBytes(32)))
        .apply {
            if (extendDefault) {
                defaultConfigure(this, this@appendCsp)
            }

            if (configure != null) {
                configure(this@appendCsp)
            }
        }
    attributes.put(CSP.ConfigKey, config)
    if (config.enabled) {
        response.header("Content-Security-Policy", config.toString())
    }
    return config
}
