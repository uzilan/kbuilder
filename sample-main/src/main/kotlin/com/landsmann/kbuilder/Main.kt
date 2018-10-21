package com.landsmann.kbuilder

@Kson
data class User(val name: String,
                val email: String)

@Builder
data class Cat(val name: String, val lifes: Int)

@Builder
data class Dog(val name: String, val barksalot: Boolean)

fun main(args: Array<String>) {

    println("User to JSON")
    val user = User(
            name = "Test",
            email = "test@email.com"
    )
    println("User: $user")
    println("Json: ${user.toJson()}")


    val cat = Cat("Wilma", 5)
    println("Cat: $cat")

    val dog = Dog("Siri", false)
    println("Dog: $dog")

}


