import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

fun main(args: Array<String>) {

    val cat = Cat.build().name("Fluff").lives(9).build()
    print(cat)

    createBuilder()


}

data class Dog(val name: String, val barksalot: Boolean)

fun createBuilder() {
    val companion = TypeSpec.companionObjectBuilder()
            .addProperty(PropertySpec.builder("buzz", String::class)
                    .initializer("%S", "buzz")
                    .build())
            .addFunction(FunSpec.builder("beep")
                    .addStatement("println(%S)", "Beep!")
                    .build())
            .build()


    val helloWorld = TypeSpec.classBuilder("Dog")
            .addType(companion)
            .build()
}

data class Cat(val name: String, val lives: Int) {

    private constructor(builder: CatBuilder) : this(builder.name, builder.lives)

    companion object {
        @JvmStatic
        fun build(): CatBuilder = CatBuilder()
    }


    class CatBuilder {
        internal var name: String = ""
        internal var lives: Int = 0

        fun name(name: String): CatBuilder {
            this.name = name
            return this
        }

        fun lives(lives: Int): CatBuilder = apply { this.lives = lives }

        fun build() = Cat(this)
    }
}

