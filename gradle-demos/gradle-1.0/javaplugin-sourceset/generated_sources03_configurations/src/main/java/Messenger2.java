import javax.swing.text.html.HTMLDocument;

/**
 * @author Leif Lislegård
 */
public class Messenger2 {

    //kaller via reflection her da klassen ikke ligger på compile classpath
    public static void main(String... args) throws Throwable {
        String message = Special.getMessage();
        System.out.println(message);
    }

}
