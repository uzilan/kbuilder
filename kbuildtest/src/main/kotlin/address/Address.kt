package address

import kbuilder.Builder

@Builder
data class Address(
    val protocol: String,
    val url: String,
    val port: Int?,
    val path: String?,
    val description: String?,
){
    override fun toString(): String {
        val maybePort = if (port != null) ":$port" else ""
        val maybePath = path ?: ""
        val maybeDescription = if (description != null) " ($description)" else ""

        return "$protocol$url$maybePort$maybePath$maybeDescription"
    }
}