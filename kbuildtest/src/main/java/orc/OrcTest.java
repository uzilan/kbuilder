package orc;

public class OrcTest {

    public static void main(String[] args) {
        Orc uruk = OrcBuilder.builder()
                .name("Uruk")
                .age(3)
                .build();

        Orc vincent = OrcBuilder.builder().name("Vince").age(12).build();

        System.out.println("uruk: " + uruk);
        System.out.println("vincent: " + vincent);
    }
}

