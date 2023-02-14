package kbuilder

data class Context(
    val packageName: String,
    val className: String,
    val builderName: String,
    val properties: List<Prop>,
    val propertyMap: Map<Prop, Prop>,
)