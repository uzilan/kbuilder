package hobbit

import kbuilder.Builder

object HobbitTest {

    @JvmStatic
    fun main(args: Array<String>) {
        val bilbo = HobbitBuilder.builder()
            .name("Bilbo")
            .age(201)
            .build()

        println(bilbo)
    }
}

@Builder
data class Hobbit(val name: String, val age: Int)