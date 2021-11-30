# ktor-csp

[![Maven Central](https://img.shields.io/maven-central/v/com.chalk/ktor-csp.svg)](https://search.maven.org/artifact/com.chalk/ktor-csp)

A Ktor web framework plugin to help configure a Content Security Policy (CSP) header.

## Installation

```kotlin
implementation("com.chalk:ktor-csp:<version>")
```

## Usage

Install the plugin in your application module:

```kotlin
install(CSP) {
    defaultConfig { call ->
        defaultSrc(none)
        imgSrc(self, "https://example.net")
        scriptSrc(self, nonce)
        
        // These are all equivalent
        fontSrc("https://fonts.google.com")
        fontSrc = listOf("https://fonts.google.com")
        fontSrc += "https://fonts.google.com"
        
        // For any CSP directives that don't have a helper
        directive("some-directive") += listOf("https://example.net")
    }
}
```

### Use nonce value in call

```kotlin
get("/some-path") {
    val nonce = call.cspNonce
    call.respondText("<script nonce=\"$nonce\">...</script>")
}
```

### Applying only to certain routes

Disable sending the header for all requests:

```kotlin
install(CSP) {
    allRequests = false
    defaultConfig { call -> 
        ... 
    }
}
```

> ⚠️ **Warning**
>
> If you do not disable the header for all requests and combine any of the below methods, you will have **multiple** Content-Security-Policy headers, they will not overwrite each other.
> 
> As per the [MDN documentation](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy#multiple_content_security_policies), having multiple headers is valid but may not be what you expect.
> Each additional CSP header may only further restrict the capabilities of the page, not expand them.

#### For multiple routes

Surround your routes with a `csp` block:

```kotlin
routes {
    // Use default config
    csp { 
        get("/has-csp") { ... }
    }

    // Extend default config with more directives
    csp({
        scriptSrc("https://example.net")
    }) {
        get("/has-csp")
    }

    // Ignore defaults and start from scratch
    csp(extendDefault = false, {
        scriptSrc("https://example.net")
    }) {
        get("/has-csp")
    }

    // Not inside a csp block, will not have the header
    get("/no-csp") { ... }
}
```

#### For a single route or call

```kotlin
get("/some-route") {
    // Use default config
    call.appendCsp()

    // Extend default config with more directives
    call.appendCsp { 
        scriptSrc("https://example.net")
    }

    // Ignore defaults and start from scratch
    call.appendCsp(extendDefault = false) {
        scriptSrc("https://example.net")
    }
}
```
