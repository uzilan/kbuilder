package orc;

import kbuilder.Builder;

@Builder
public class Orc {
    private String name;
    private int age;

    public Orc(final String name, final int age) {
        this.name = name;
        this.age = age;
    }

    @Override
    public String toString() {
        return "Orc(name=" + name + ", age=" + age + ")";
    }
}
