package com.landsmann.kbuilder

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter.fieldsIn

@AutoService(Processor::class)
class BuilderProcessor : AbstractProcessor() {

    override fun init(p0: ProcessingEnvironment) {
        super.init(p0)
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(Builder::class.java.name)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latest()
    }


    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        println("process")
        roundEnv.getElementsAnnotatedWith(Builder::class.java)
                .forEach {
                    println("Processing: ${it.simpleName}")
                    val pack = processingEnv.elementUtils.getPackageOf(it).toString()
                    generateBuilder(it, pack)
                }
        return true
    }

    private fun generateBuilder(klass: Element, pack: String) {
        val fileName = "${klass.simpleName}Builder"
        val classBuilder = TypeSpec.classBuilder(fileName)

        fieldsIn(klass.enclosedElements)
                .forEach {
                    classBuilder
                            .addProperty(PropertySpec
                                    .builder(it.toString(), String::class, KModifier.INTERNAL)
                                    .mutable(true)
                                    .initializer("\"\"")
                                    .build())
                }

        val file = FileSpec.builder(pack, fileName).addType(classBuilder.build()).build()

        val kaptKotlinGeneratedDir = processingEnv.options[KsonProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME]
        file.writeTo(File(kaptKotlinGeneratedDir, "$fileName.kt"))
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }
}