package kbuilder

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.symbol.Nullability.NULLABLE
import com.google.devtools.ksp.symbol.Origin
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.ABSTRACT
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
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
            .map {
                Prop(
                    name = it.toString(),
                    type = it.type.toString(),
                    isNullable = isNullable(it)
                )
            }.toList()

        val propertyMap = properties.zipWithNext().toMap()
        val context = Context(packageName, className, builderName, properties, propertyMap)

        val fileSpec = FileSpec.builder(packageName, builderName).apply {

            // add interfaces for each property
            addInterfaces(context)

            // create the builder class
            val classBuilder = TypeSpec.classBuilder(builderName)

                // add a list of interfaces for the builder class to implement
                .addSuperinterfaces(createInterfaceList(context))

                // add a primary private constructor
                .primaryConstructor(createPrivateConstructor())

                // add the build() method
                .addFunction(createBuildMethod(context))

                // add all the properties
                .addProperties(createProperties(context))

                // add a property method for each property
                .addFunctions(createPropertyMethods(context))

                // add the companion object with the builder() method
                .addType(createCompanionObject(context))

                .build()
            addType(classBuilder)
        }.build()
        fileSpec.writeTo(codeGenerator, true)
    }

    @OptIn(KspExperimental::class)
    private fun isNullable(property: KSPropertyDeclaration): Boolean {

        val annotationPresent = property.annotations.toList()
            .firstOrNull { it.shortName.asString() == NotNull::class.simpleName } != null

        return when {
            annotationPresent -> false
            property.type.origin == Origin.JAVA -> true
            else -> property.type.resolve().nullability == NULLABLE
        }
    }

    private fun createInterfaceList(ctx: Context): List<ClassName> {
        return ctx.properties
            .filter { !it.isNullable }
            .map { ClassName(ctx.packageName, it.name.cap()) }.toList() +
            ClassName(ctx.packageName, "Build")
    }

    private fun FileSpec.Builder.addInterfaces(ctx: Context) {
        addType(
            TypeSpec.interfaceBuilder("Build")
                .addFunctions(ctx.properties
                    .filter { it.isNullable }
                    .map { prop ->
                        FunSpec.builder(prop.name)
                            .returns(ClassName(ctx.packageName, "Build"))
                            .addModifiers(ABSTRACT)
                            .addParameter(
                                ParameterSpec.builder(
                                    prop.name,
                                    ClassName(ctx.packageName, prop.type)
                                ).build()
                            )
                            .build()
                    })
                .addFunction(
                    FunSpec.builder("build")
                        .returns(ClassName(ctx.packageName, ctx.className))
                        .addModifiers(ABSTRACT)
                        .build()
                ).build()
        )
        ctx.properties
            .filter { !it.isNullable }
            .forEach { prop ->
                val nextProp = ctx.propertyMap[prop]
                val returnClass = when {
                    nextProp == null || nextProp.isNullable -> "Build"
                    else -> nextProp.name.cap()
                }
                addType(
                    TypeSpec.interfaceBuilder(prop.name.cap())
                        .addFunction(
                            FunSpec.builder(prop.name)
                                .returns(ClassName(ctx.packageName, returnClass))
                                .addModifiers(ABSTRACT)
                                .addParameter(
                                    ParameterSpec.builder(
                                        prop.name,
                                        ClassName(ctx.packageName, prop.type)
                                    ).build()
                                ).build()
                        ).build()
                )
            }
    }

    private fun createPrivateConstructor() = FunSpec.constructorBuilder()
        .addModifiers(PRIVATE)
        .build()

    private fun createPropertyMethods(ctx: Context): List<FunSpec> {
        return ctx.properties.map { prop ->
            val nextProp = ctx.propertyMap[prop]
            val returnClass = when {
                nextProp == null || nextProp.isNullable -> "Build"
                else -> nextProp.name.cap()
            }
            FunSpec.builder(prop.name)
                .addModifiers(OVERRIDE)
                .returns(ClassName(ctx.packageName, returnClass))
                .addParameter(
                    ParameterSpec.builder(
                        prop.name,
                        ClassName(ctx.packageName, prop.type)
                    ).build()
                ).addCode(
                    """this.${prop.name} = ${prop.name} 
                      |return this""".trimMargin()
                ).build()
        }.toList()
    }

    private fun createProperties(ctx: Context): List<PropertySpec> {
        return ctx.properties.map {
            PropertySpec.builder(it.name, ClassName(ctx.packageName, it.type).copy(nullable = true))
                .addModifiers(PRIVATE)
                .mutable()
                .initializer("null")
                .build()
        }.toList()
    }

    private fun createBuildMethod(ctx: Context): FunSpec {

        val values = ctx.properties
            .map { prop -> if (prop.isNullable) prop.name else "${prop.name}!!" }
            .joinToString(", ")

        return FunSpec.builder("build")
            .returns(ClassName(ctx.packageName, ctx.className))
            .addModifiers(OVERRIDE)
            .addCode("return ${ctx.className}($values)")
            .build()
    }

    private fun createCompanionObject(ctx: Context): TypeSpec {
        val returnClass = ctx.properties.firstOrNull() { !it.isNullable }?.name?.cap() ?: "Build"
        return TypeSpec.companionObjectBuilder()
            // add the static builder() extension method
            .addFunction(
                FunSpec.builder("builder")
                    .returns(ClassName(ctx.packageName, returnClass))
                    .addCode("return ${ctx.builderName}()")
                    .addAnnotation(JvmStatic::class)
                    .build()
            ).build()
    }
}

// String.capitalize() is deprecated, creating our own version here instead
fun String.cap() = this.replaceFirstChar(Char::uppercaseChar)