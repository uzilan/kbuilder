package kbuilder

import com.google.devtools.ksp.processing.CodeGenerator
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
    private val codeGenerator: CodeGenerator,
) : KSVisitorVoid() {

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        val packageName = classDeclaration.packageName.asString()
        val className = classDeclaration.simpleName.asString()
        val builderName = className + "Builder"
        val properties = classDeclaration.getAllProperties()
            .map { property ->
                Prop(
                    name = property.toString(),
                    type = property.type.toString(),
                    isNullable = isNullable(property),
                )
            }.toList()

        val propertyMap = properties.zipWithNext().toMap()
        val context = Context(packageName, className, builderName, properties, propertyMap)
        val fileSpec = FileSpec.builder(packageName, builderName).apply {
            // create the Build interface
            addType(createBuildInterface(context))
            // create an interface for each non-nullable property
            context.properties
                .filter { !it.isNullable }
                .forEach { prop ->
                    addType(createPropertyInterface(context, prop, findReturnType(context, prop)))
                }
            // create the builder class
            val classBuilder = TypeSpec.classBuilder(builderName)
                // add a list of interfaces to implement
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

    // create the Build interface
    private fun createBuildInterface(ctx: Context): TypeSpec {
        return TypeSpec.interfaceBuilder("Build")
            .addFunctions(
                // a function to set the value for each non-mandatory (nullable) property
                ctx.properties
                    .filter { it.isNullable }
                    .map { prop ->
                        FunSpec.builder(prop.name)
                            .returns(ClassName(ctx.packageName, "Build"))
                            .addModifiers(ABSTRACT)
                            .addParameter(
                                ParameterSpec.builder(
                                    prop.name,
                                    ClassName(ctx.packageName, prop.type),
                                ).build(),
                            )
                            .build()
                    },
            ).addFunction(
                // the build method that returns the instance of the class
                FunSpec.builder("build")
                    .returns(ClassName(ctx.packageName, ctx.className))
                    .addModifiers(ABSTRACT)
                    .build(),
            ).build()
    }

    // create an interface for a mandatory property
    private fun createPropertyInterface(ctx: Context, prop: Prop, returnClass: String): TypeSpec {
        return TypeSpec.interfaceBuilder(prop.name.cap())
            .addFunction(
                FunSpec.builder(prop.name)
                    .returns(ClassName(ctx.packageName, returnClass))
                    .addModifiers(ABSTRACT)
                    .addParameter(
                        ParameterSpec.builder(
                            prop.name,
                            ClassName(ctx.packageName, prop.type),
                        ).build(),
                    ).build(),
            ).build()
    }

    // create a list of interfaces to be implemented by the builder class
    private fun createInterfaceList(ctx: Context): List<ClassName> {
        return ctx.properties
            .filter { !it.isNullable }
            .map { ClassName(ctx.packageName, it.name.cap()) }.toList() +
            ClassName(ctx.packageName, "Build")
    }

    // create a private constructor
    private fun createPrivateConstructor(): FunSpec {
        return FunSpec.constructorBuilder()
            .addModifiers(PRIVATE)
            .build()
    }

    // create the build method that creates the class instance,
    // using the values set in the builder as arguments
    private fun createBuildMethod(ctx: Context): FunSpec {
        val values = ctx.properties.joinToString(", ") { prop ->
            // non-nullable properties must have been given a value using
            // the mandatory property methods
            if (prop.isNullable) prop.name else "${prop.name}!!"
        }

        return FunSpec.builder("build")
            .returns(ClassName(ctx.packageName, ctx.className))
            .addModifiers(OVERRIDE)
            .addCode("return ${ctx.className}($values)")
            .build()
    }

    // create the properties in the builder class to be used by the build method
    private fun createProperties(ctx: Context): List<PropertySpec> {
        return ctx.properties.map {
            PropertySpec.builder(it.name, ClassName(ctx.packageName, it.type).copy(nullable = true))
                .addModifiers(PRIVATE)
                .mutable()
                .initializer("null")
                .build()
        }.toList()
    }

    // implement the interface property methods
    private fun createPropertyMethods(ctx: Context): List<FunSpec> {
        return ctx.properties.map { prop ->
            val returnClass = findReturnType(ctx, prop)
            FunSpec.builder(prop.name)
                .addModifiers(OVERRIDE)
                .returns(ClassName(ctx.packageName, returnClass))
                .addParameter(
                    ParameterSpec.builder(
                        prop.name,
                        ClassName(ctx.packageName, prop.type),
                    ).build(),
                ).addCode(
                    // set the value of the property in the builder class, then return this
                    """this.${prop.name} = ${prop.name} 
                      |return this
                    """.trimMargin(),
                ).build()
        }.toList()
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
                    .build(),
            ).build()
    }

    // figure out if a property is nullable
    private fun isNullable(property: KSPropertyDeclaration): Boolean {
        // try to find a NotNull annotation
        val annotationPresent = property.annotations.toList()
            .firstOrNull { it.shortName.asString() == NotNull::class.simpleName } != null

        return when {
            annotationPresent -> false // annotated with @NotNull? return false
            property.type.origin == Origin.JAVA -> true // Java? return true
            else -> property.type.resolve().nullability == NULLABLE // Kotlin? return is type nullable
        }
    }

    // figure out a method's return type: if the current property is not pointing at anything in the property map
    // or that the next property is nullable, then it is the Build interface. Otherwise, it is the
    // property that the current one is pointing at in the property map
    private fun findReturnType(ctx: Context, prop: Prop): String {
        val nextProp = ctx.propertyMap[prop]
        return when {
            nextProp == null || nextProp.isNullable -> "Build"
            else -> nextProp.name.cap()
        }
    }
}

// String.capitalize() is deprecated, creating our own version here instead
fun String.cap() = this.replaceFirstChar(Char::uppercaseChar)
