package hobbit

object HobbitTest {

    @JvmStatic
    fun main(args: Array<String>) {
        val bilbo = HobbitBuilder.builder()
            .name("Bilbo")
            .age(203)
            .height(75)
            .build()

        println(bilbo)
    }
}

