import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

public class TestResources {

    public static void main(String... args) throws Throwable {

        //verifiserer at ressurser er tilgengelige
        if (TestResources.class.getResource("main.txt") == null) {
            throw new FileNotFoundException("main.txt");
        }

        try {
            TestResources.class.getResource("fake_template.txt");
            throw new Exception("Forventet ikke å finne filen fake_template.txt på classpath");
        } catch (Throwable t) {
            assert true;
        }

        if (TestResources.class.getResource("fake_template.txt-generatedCopy") == null) {
            throw new FileNotFoundException("fake_template.txt-generatedCopy");
        }


        //verifiserer at klasser er tilgjengelige
        List<String> classes = Arrays.asList("Dummy", "OtherDummy", "org.apache.commons.lang.StringUtils");
        for (String clazzName : classes) {
                Class<?> messengerClass = TestResources.class.getClassLoader().loadClass(clazzName);
        }

        System.out.println("OK!");
    }

}