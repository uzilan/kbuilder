package kbuilder

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate

class KBuilderSymbolProcessor(
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {
    private var invoked = false

    override fun process(resolver: Resolver): List<KSAnnotated> {

        val symbols = resolver.getSymbolsWithAnnotation(Builder::class.qualifiedName!!)
        val (valid, invalid) = symbols.partition { it.validate() }
        if (invoked) return invalid

        val declarations = valid.filterIsInstance<KSClassDeclaration>()
        val visitor = KBuilderVisitor(logger, codeGenerator)
        declarations.forEach { it.accept(visitor, Unit) }

        invoked = true
        return invalid
    }
}