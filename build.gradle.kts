plugins {
    kotlin("jvm") version "1.2.71"
}

dependencies {
    compile(kotlin("stdlib"))
    compile("com.squareup:kotlinpoet:1.0.0-RC1")
    testCompile("junit:junit:4.8.1")
    testCompile("io.kotlintest:kotlintest-runner-junit5:3.1.8")
}

repositories {
    jcenter()
}
