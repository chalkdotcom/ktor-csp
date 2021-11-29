package com.chalk.ktorcsp

class CspConfig(val nonceValue: String) {
    var directives = mutableMapOf<String, MutableList<String>>()
        internal set
    var enabled = true

    val none = "'none'"
    val self = "'self'"
    val nonce: String
        get() = "nonce-$nonceValue"

    fun directive(name: String): MutableList<String> = directives[name]
        ?: mutableListOf<String>().also { directives[name] = it }

    fun clear() {
        directives.clear()
    }

    override fun toString(): String {
        return directives.entries
            .filter { it.value.isNotEmpty() }
            .joinToString("; ") { (directive, values) ->
                "$directive ${values.joinToString(" ")}"
            }
    }

    operator fun invoke(block: CspConfig.() -> Unit): CspConfig = apply(block)

    operator fun MutableList<String>.invoke(vararg values: String) = addAll(values)
}
