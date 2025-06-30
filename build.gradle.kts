///////////////////////////////////////////////////////////////////////////////
//  GRADLE CONFIGURATION
///////////////////////////////////////////////////////////////////////////////

plugins {
    id("org.jetbrains.dokka") version "1.7.10"
}
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.20")
        classpath("com.android.tools.build:gradle:7.4.2")
        classpath ("javax.xml.bind:jaxb-api:2.3.1")
        classpath ("com.sun.xml.bind:jaxb-impl:2.3.9")
    }
}

