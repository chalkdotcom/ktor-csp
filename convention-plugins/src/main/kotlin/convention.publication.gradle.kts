import gradle.kotlin.dsl.accessors._98ddc69e404e030b2baf3d8bc3aab94c.ext
import gradle.kotlin.dsl.accessors._98ddc69e404e030b2baf3d8bc3aab94c.publishing
import gradle.kotlin.dsl.accessors._98ddc69e404e030b2baf3d8bc3aab94c.signing
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.`maven-publish`
import org.gradle.kotlin.dsl.signing
import java.util.*

plugins {
    `maven-publish`
    signing
}

ext["signing.keyId"] = ""
ext["signing.password"] = ""
ext["signing.key"] = ""
ext["ossrhUsername"] = ""
ext["ossrhPassword"] = ""

val secretPropsFile = project.rootProject.file("local.properties")
if (secretPropsFile.exists()) {
    secretPropsFile.reader().use {
        Properties().apply {
            load(it)
        }
    }.onEach { (name, value) ->
        ext[name.toString()] = value
    }
} else {
    ext["signing.keyId"] = System.getenv("SIGNING_KEY_ID")
    ext["signing.password"] = System.getenv("SIGNING_PASSWORD")
    ext["signing.key"] = System.getenv("SIGNING_KEY")
    ext["ossrhUsername"] = System.getenv("OSSRH_USERNAME")
    ext["ossrhPassword"] = System.getenv("OSSRH_PASSWORD")
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

fun getExtraString(name: String) = ext[name].toString()

fun isReleaseBuild() = !getExtraString("VERSION").contains("SNAPSHOT")

publishing {
    repositories {
        maven {
            name = "sonatype"
            setUrl(if (isReleaseBuild()) "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/" else "https://s01.oss.sonatype.org/content/repositories/snapshots/")
            credentials {
                username = getExtraString("ossrhUsername")
                password = getExtraString("ossrhPassword")
            }
        }
    }

    publications.withType<MavenPublication> {
        artifact(javadocJar.get())

        pom {
            name.set(getExtraString("POM_NAME"))
            description.set(getExtraString("POM_DESCRIPTION"))
            url.set(getExtraString("POM_URL"))

            licenses {
                license {
                    name.set(getExtraString("POM_LICENCE_NAME"))
                    url.set(getExtraString("POM_LICENCE_URL"))
                    distribution.set(getExtraString("POM_LICENCE_DIST"))
                }
            }
            developers {
                developer {
                    id.set(getExtraString("POM_DEVELOPER_ID"))
                    name.set(getExtraString("POM_DEVELOPER_NAME"))
                }
            }
            scm {
                url.set(getExtraString("POM_SCM_URL"))
                connection.set(getExtraString("POM_SCM_CONNECTION"))
                developerConnection.set(getExtraString("POM_SCM_DEV_CONNECTION"))
            }

        }
    }
}

signing {
    isRequired = isReleaseBuild()
    val signingKeyId = getExtraString("signing.keyId")
    val signingKey = getExtraString("signing.key")
    val signingPassword = getExtraString("signing.password")
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    sign(publishing.publications)
}
