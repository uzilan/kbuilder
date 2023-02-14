package address

object AddressTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val google = AddressBuilder.builder()
            .protocol("https://")
            .url("google.com")
            .description("Google, what else?")
            .build()

        println("google: $google")
    }
}