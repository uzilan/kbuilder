package orc;

import kbuilder.Builder;
import kbuilder.NotNull;

@Builder
public class Orc {
    @NotNull
    private String name;
    private int age;

    public Orc(final String name, final Integer age) {
        this.name = name;
        this.age = age;
    }

    @Override
    public String toString() {
        return "Orc(name=" + name + ", age=" + age + ")";
    }
}
