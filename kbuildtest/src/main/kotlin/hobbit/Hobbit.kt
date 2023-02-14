package hobbit

import kbuilder.Builder

@Builder
data class Hobbit(val name: String, val age: Int, val pet: String?)