import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class JavaBuilderTests {

   @Test
   public void should_create_object_using_builder() {
      Cat cat = Cat.build().lives(9).name("Tiger").build();

      assertEquals(9, cat.getLives());
      assertEquals("Tiger", cat.getName());
   }
}
