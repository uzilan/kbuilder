package hobbit

object HobbitTest {

    @JvmStatic
    fun main(args: Array<String>) {

        val bilbo = HobbitBuilder.builder()
            .name("Bilbo")
            .age(300)
            .pet("Gollum")
            .build()

        println(bilbo)
    }
}

