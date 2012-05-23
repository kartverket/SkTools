import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

public class TestResources {

    public static void main(String... args) throws Throwable {

        //verifiserer at ressurser er tilgengelige
        if (TestResources.class.getResource("main.txt") == null) {
            throw new FileNotFoundException("main.txt");
        }

        if (TestResources.class.getResource("main2.txt") == null) {
            throw new FileNotFoundException("main2.txt");
        }

        if (TestResources.class.getResource("noneGeneratedOther.txt") == null) {
            throw new FileNotFoundException("noneGeneratedOther.txt");
        }

        //verifiserer at klasser er tilgjengelige
        List<String> classes = Arrays.asList("Dummy", "Dummy2", "NoneGeneratedDummy", "GeneratedSpecial");
        for (String clazzName : classes) {
                Class<?> messengerClass = TestResources.class.getClassLoader().loadClass(clazzName);
        }

        System.out.println("OK!");

    }

}