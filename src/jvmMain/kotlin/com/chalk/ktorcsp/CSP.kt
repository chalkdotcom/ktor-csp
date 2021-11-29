package com.chalk.ktorcsp

import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.response.header
import io.ktor.util.AttributeKey
import java.util.Base64
import kotlin.random.Random

class CSP(val configuration: Configuration) {
    class Configuration {
        val defaultConfig = CspConfig()
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, CSP> {
        override val key = AttributeKey<CSP>("CSP")

        val ConfigKey = AttributeKey<CspConfig>("CSPConfig")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): CSP {
            val configuration = Configuration().apply(configure)
            val feature = CSP(configuration)

            pipeline.intercept(ApplicationCallPipeline.Setup) {
                call.attributes.put(ConfigKey, configuration.defaultConfig.copy(Base64.getEncoder().encodeToString(Random.nextBytes(32))))
            }

            pipeline.intercept(ApplicationCallPipeline.Call) {
                println("call phase")
                val config = call.attributes[ConfigKey]
                if (!config.skip) {
                    call.response.header("Content-Security-Policy", config.toString())
                }
            }

            return feature
        }
    }
}
