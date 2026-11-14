rootProject.name = "OguretsIntellij"

plugins {
    // Auto-provision the Java toolchain if a matching JDK isn't installed locally
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
