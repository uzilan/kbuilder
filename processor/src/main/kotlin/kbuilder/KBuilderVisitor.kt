package kbuilder

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo

class KBuilderVisitor(
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
) : KSVisitorVoid() {
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        val packageName = classDeclaration.packageName.asString()
        val className = classDeclaration.simpleName.asString()
        val builderName = className + "Builder"
        val properties = classDeclaration.getAllProperties()
            .map { Prop(it.toString(), it.type.toString()) }

        val fileSpec = FileSpec.builder(packageName, builderName).apply {

            // create the builder class
            val classBuilder = TypeSpec.classBuilder(builderName)

            addType(
                classBuilder
                    // add a primary private constructor
                    .primaryConstructor(createPrivateConstructor())

                    // add the build() method
                    .addFunction(createBuildMethod(packageName, className, properties))

                    // add all the properties
                    .addProperties(createProperties(packageName, properties))

                    // add a builder function for each property
                    .addFunctions(createBuildMethods(packageName, properties, builderName))

                    // add the companion object
                    .addType(createCompanionObject(builderName))

                    .build()
            )
        }.build()
        fileSpec.writeTo(codeGenerator, true)
    }

    private fun createPrivateConstructor() = FunSpec.constructorBuilder()
        .addModifiers(KModifier.PRIVATE)
        .build()

    private fun createBuildMethods(
        packageName: String,
        properties: Sequence<Prop>,
        builderName: String,
    ): List<FunSpec> {
        return properties.map {
            FunSpec.builder(it.name)
                .returns(ClassName(packageName, builderName))
                .addParameter(
                    ParameterSpec.builder(
                        it.name,
                        ClassName(packageName, it.type).copy(nullable = true)
                    ).build()
                ).addCode(
                    """this.${it.name} = ${it.name} 
                      |return this""".trimMargin()
                ).build()
        }.toList()
    }

    private fun createProperties(
        packageName: String,
        properties: Sequence<Prop>,
    ) = properties.map {
        PropertySpec.builder(
            it.name,
            ClassName(packageName, it.type).copy(nullable = true)
        )
            .addModifiers(KModifier.PRIVATE)
            .mutable()
            .initializer("null")
            .build()
    }.toList()

    private fun createBuildMethod(
        packageName: String,
        className: String,
        properties: Sequence<Prop>,
    ) = FunSpec.builder("build")
        .returns(ClassName(packageName, className))
        .addCode(
            """${
                properties.map { "require(${it.name} != null) {\"Missing property ${it.name}\"}" }
                    .joinToString("\n")
            }                   
                                          |return $className(${
                properties.map { "this.${it.name}!!" }
                    .joinToString(", ")
            })""".trimMargin()
        ).build()

    private fun createCompanionObject(builderName: String) = TypeSpec.companionObjectBuilder()
        // add the static builder() extension method
        .addFunction(
            FunSpec.builder("builder")
                .addCode(
                    """return $builderName()"""
                )
                .addAnnotation(JvmStatic::class)
                .build()
        ).build()
}