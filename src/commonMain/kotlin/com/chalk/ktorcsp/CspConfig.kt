package com.chalk.ktorcsp

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class CspConfig(val nonceValue: String) {
    var directives = mutableMapOf<String, MutableList<String>>()
        internal set
    var enabled = true

    val none = "'none'"
    val self = "'self'"
    val strictDynamic = "'strict-dynamic'"
    val reportSample = "'report-sample'"
    val unsafeInline = "'unsafe-inline'"
    val unsafeEval = "'unsafe-eval'"
    val unsafeHashes = "'unsafe-hashes'"
    val unsafeAllowRedirects = "'unsafe-allow-redirects'"
    val nonce: String
        get() = "'nonce-$nonceValue'"
    fun hash(algorithm: String, hash: String) = "'$algorithm-$hash'"

    fun directive(name: String): MutableList<String> = directives[name]
        ?: mutableListOf<String>().also { directives[name] = it }

    fun clear() {
        directives.clear()
    }

    fun remove(name: String) {
        directives.remove(name)
    }

    override fun toString(): String {
        return directives.entries
            .joinToString("; ") { (directive, values) ->
                if (values.isEmpty()) directive else "$directive ${values.joinToString(" ")}"
            }
    }

    operator fun invoke(block: CspConfig.() -> Unit): CspConfig = apply(block)

    operator fun MutableList<String>.invoke(vararg values: String) = addAll(values)

    private fun directiveDelegate(directive: String) = object : ReadWriteProperty<CspConfig, List<String>> {
        override fun getValue(thisRef: CspConfig, property: KProperty<*>) = thisRef.directive(directive)

        override fun setValue(thisRef: CspConfig, property: KProperty<*>, value: List<String>) {
            thisRef.directives[directive] = value.toMutableList()
        }
    }

    var childSrc by directiveDelegate(Directives.ChildSrc)
    var connectSrc by directiveDelegate(Directives.ConnectSrc)
    var defaultSrc by directiveDelegate(Directives.DefaultSrc)
    var fontSrc by directiveDelegate(Directives.FontSrc)
    var frameSrc by directiveDelegate(Directives.FrameSrc)
    var imgSrc by directiveDelegate(Directives.ImgSrc)
    var manifestSrc by directiveDelegate(Directives.ManifestSrc)
    var mediaSrc by directiveDelegate(Directives.MediaSrc)
    var objectSrc by directiveDelegate(Directives.ObjectSrc)
    var prefetchSrc by directiveDelegate(Directives.PrefetchSrc)
    var scriptSrc by directiveDelegate(Directives.ScriptSrc)
    var styleSrc by directiveDelegate(Directives.StyleSrc)
    var workerSrc by directiveDelegate(Directives.WorkerSrc)

    var baseUri by directiveDelegate(Directives.BaseUri)
    var sandbox by directiveDelegate(Directives.Sandbox)

    var formAction by directiveDelegate(Directives.FormAction)
    var frameAncestors by directiveDelegate(Directives.FrameAncestors)
    var navigateTo by directiveDelegate(Directives.NavigateTo)

    var reportUri by directiveDelegate(Directives.ReportUri)
    var reportTo by directiveDelegate(Directives.ReportTo)

    fun upgradeInsecureRequests() {
        directive(Directives.UpgradeInsecureRequests)
    }

    object Directives {
        const val ChildSrc = "child-src"
        const val ConnectSrc = "connect-src"
        const val DefaultSrc = "default-src"
        const val FontSrc = "font-src"
        const val FrameSrc = "frame-src"
        const val ImgSrc = "img-src"
        const val ManifestSrc = "manifest-src"
        const val MediaSrc = "media-src"
        const val ObjectSrc = "object-src"
        const val PrefetchSrc = "prefetch-src"
        const val ScriptSrc = "script-src"
        const val ScriptSrcElem = "script-src-elem"
        const val ScriptSrcAttr = "script-src-attr"
        const val StyleSrc = "style-src"
        const val StyleSrcElem = "style-src-elem"
        const val StyleSrcAttr = "style-src-attr"
        const val WorkerSrc = "worker-src"

        const val BaseUri = "base-uri"
        const val Sandbox = "sandbox"

        const val FormAction = "form-action"
        const val FrameAncestors = "frame-ancestors"
        const val NavigateTo = "navigate-to"

        const val ReportUri = "report-uri"
        const val ReportTo = "report-to"

        const val RequireSriFor = "require-sri-for"
        const val RequireTrustedTypesFor = "require-trusted-types-for"
        const val TrustedTypes = "trusted-types"
        const val UpgradeInsecureRequests = "upgrade-insecure-requests"

        @Deprecated("Deprecated CSP directive")
        const val BlockAllMixedContent = "block-all-mixed-content"
        @Deprecated("Deprecated CSP directive")
        const val PluginTypes = "plugin-types"
        @Deprecated("Deprecated CSP directive")
        const val Referrer = "referrer"
    }
}
