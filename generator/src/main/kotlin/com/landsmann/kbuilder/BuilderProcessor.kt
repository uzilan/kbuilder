package com.landsmann.kbuilder

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
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
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.ElementFilter.fieldsIn
import kotlin.reflect.KClass

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
        roundEnv.getElementsAnnotatedWith(Builder::class.java)
                .forEach {
                    val pack = processingEnv.elementUtils.getPackageOf(it).toString()
                    generateBuilder(it, pack)
                }
        return true
    }

    data class FieldInfo(val name: String, val typeMirror: TypeMirror, val kClass: KClass<*>)

    private fun generateBuilder(klass: Element, pack: String) {
        val fileName = "${klass.simpleName}Builder"
        val classBuilder = TypeSpec.classBuilder(fileName)
        val fields = fieldsIn(klass.enclosedElements)
                .map { FieldInfo(it.toString(), it.asType(), getClass(it)) }

        generateFields(classBuilder, fields)
        generateCompanionObject(classBuilder, fileName)
        generateBuildFunction(klass, fields, classBuilder)

        val file = FileSpec.builder(pack, fileName).addType(classBuilder.build()).build()

        val kaptKotlinGeneratedDir = processingEnv.options[BuilderProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME]
        file.writeTo(File(kaptKotlinGeneratedDir, "$fileName.kt"))
    }


    private fun generateFields(classBuilder: TypeSpec.Builder, fields: List<FieldInfo>) {
        fields.forEach {
            classBuilder.addProperty(PropertySpec
                    .builder(it.name, it.kClass, KModifier.INTERNAL)
                    .mutable(true)
                    .initializer(getDefaultInitializer(it.typeMirror))
                    .build())

            classBuilder.addFunction(FunSpec
                    .builder(it.name)
                    .addParameter(ParameterSpec
                            .builder(it.name, it.kClass)
                            .build())
                    .addCode("return apply{this.${it.name} = ${it.name}}\n")
                    .build())
        }
    }

    private fun generateCompanionObject(classBuilder: TypeSpec.Builder, fileName: String) {
        classBuilder.companionObject(
                TypeSpec.companionObjectBuilder()
                        .addFunction(FunSpec
                                .builder("builder")
                                .addCode("return $fileName()")
                                .build())
                        .build())
    }

    private fun generateBuildFunction(klass: Element, fields: List<FieldInfo>, classBuilder: TypeSpec.Builder) {
        val buildFunBuilder = FunSpec.builder("build")

        buildFunBuilder.addCode("return ${klass.simpleName} (")
        val fieldsInit = fields
                .map { "${it.name} = this.${it.name} " }
                .joinToString(", ")
        buildFunBuilder.addCode(fieldsInit)
        buildFunBuilder.addCode(")\n")


        classBuilder.addFunction(
                buildFunBuilder.build()
        )
    }

    private fun getDefaultInitializer(type: TypeMirror): CodeBlock {
        return when (type.kind) {
            TypeKind.DECLARED -> {
                if (type.toString() == "java.lang.String") CodeBlock.of("\"\"")
                else CodeBlock.of(type.toString())
            }
            TypeKind.BOOLEAN -> CodeBlock.of("%L", false)
            TypeKind.BYTE, TypeKind.SHORT, TypeKind.INT, TypeKind.LONG, TypeKind.CHAR, TypeKind.FLOAT, TypeKind.DOUBLE -> CodeBlock.of("%L", 0)
            else -> throw Exception("Unknown type: $type, kind: ${type.kind}")
        }
    }

    private fun getClass(it: VariableElement): KClass<*> {
        val type = it.asType()

        return when (type.kind) {
            TypeKind.DECLARED -> Class.forName(type.toString()).kotlin
            TypeKind.BOOLEAN -> Boolean::class
            TypeKind.BYTE -> Byte::class
            TypeKind.SHORT -> Short::class
            TypeKind.INT -> Int::class
            TypeKind.LONG -> Long::class
            TypeKind.CHAR -> Char::class
            TypeKind.FLOAT -> Float::class
            TypeKind.DOUBLE -> Double::class
            else -> throw Exception("Unknown type: $type, kind: ${type.kind}")
        }
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }
}