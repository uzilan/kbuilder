plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":builder"))
    implementation("com.google.devtools.ksp:symbol-processing-api:1.8.0-1.0.9")
    implementation("com.squareup:kotlinpoet-ksp:1.12.0")
}