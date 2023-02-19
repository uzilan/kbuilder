package orc;

public class OrcTest {

    public static void main(String[] args) {
        Orc uruk = OrcBuilder.builder()
                .name("Uruk")
                .age(3)
                .build();

        System.out.println("uruk: " + uruk);
    }
}

