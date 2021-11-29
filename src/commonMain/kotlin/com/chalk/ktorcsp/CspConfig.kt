package com.chalk.ktorcsp

class CspConfig {
    var nonceValue: String = "unset"
        internal set
    var directives = mutableMapOf<String, MutableList<String>>()
        internal set
    var skip = false

    val none = "'none'"
    val self = "'self'"
    val nonce: String
        get() = "nonce-$nonceValue"


    fun directive(name: String): MutableList<String> = directives[name]
        ?: mutableListOf<String>().also { directives[name] = it }

    override fun toString(): String {
        return directives.entries
            .filter { it.value.isNotEmpty() }
            .joinToString("; ") { (directive, values) ->
                "$directive ${values.joinToString(" ")}"
            }
    }

    operator fun invoke(block: CspConfig.() -> Unit): CspConfig = apply(block)

    operator fun MutableList<String>.invoke(vararg values: String) = addAll(values)

    internal fun copy(nonce: String): CspConfig {
        val newConfig = CspConfig()
        newConfig.nonceValue = nonce
        newConfig.directives = mutableMapOf<String, MutableList<String>>().also { it.putAll(directives) }
        newConfig.skip = skip
        return newConfig
    }
}
