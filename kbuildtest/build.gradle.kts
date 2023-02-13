plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp") version "1.8.0-1.0.9"
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":builder"))
    implementation(project(":processor"))
    ksp(project(":processor"))
}

ksp {
    // Passing an argument to the symbol processor.
    // Change value to "true" in order to apply the argument.
    arg("ignoreGenericArgs", "false")
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}