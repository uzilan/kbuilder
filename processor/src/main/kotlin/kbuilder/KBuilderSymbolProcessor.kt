package kbuilder

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate

data class Prop(
    val name: String,
    val type: String,
//    val isNullable: Boolean,
)

class KBuilderSymbolProcessor(
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {
    private var invoked = false

    override fun process(resolver: Resolver): List<KSAnnotated> {

        val symbols = resolver.getSymbolsWithAnnotation(Builder::class.qualifiedName!!)
        val (valid, nonValid) = symbols.partition { it.validate() }
        if (invoked) return nonValid

        val visitor = KBuilderVisitor(logger, codeGenerator)
        valid.filterIsInstance<KSClassDeclaration>()
            .forEach { it.accept(visitor, Unit) }

        invoked = true
        return nonValid
    }
}