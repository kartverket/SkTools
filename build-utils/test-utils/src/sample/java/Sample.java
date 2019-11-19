import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Sample {

    public static void main(String... args) throws IOException {
        System.out.println("Contents of sample.txt: ");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Sample.class.getResourceAsStream("/sample.txt"), StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            while (line != null) {
                System.out.println(line);
                line = reader.readLine();
            }
        }
    }

}