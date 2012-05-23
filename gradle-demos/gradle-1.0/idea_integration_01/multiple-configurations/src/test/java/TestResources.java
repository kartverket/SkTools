import java.io.FileNotFoundException;
import java.lang.Object;
import java.util.Arrays;
import java.util.List;

public class TestResources {

    public static void main(String... args) throws Throwable {

        //verifiserer at ressurser er tilgengelige
        if (TestResources.class.getResource("main.txt") == null) {
            throw new FileNotFoundException("main.txt");
        }

        if (TestResources.class.getResource("other.txt") == null) {
            throw new FileNotFoundException("other.txt");
        }

        //verifiserer at klasser er tilgjengelige
        List<String> classes = Arrays.asList("Dummy", "OtherDummy", "org.apache.commons.lang.StringUtils");
        for (String clazzName : classes) {
                Class<?> messengerClass = TestResources.class.getClassLoader().loadClass(clazzName);
        }

        System.out.println("OK!");
    }

}