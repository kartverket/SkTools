import java.lang.Class;
import java.lang.RuntimeException;
import java.lang.System;
import java.lang.Throwable;
import java.lang.reflect.Method;

/**
 * @author Leif Lislegård
 */
public class Messenger {


    //kaller via reflection her da klassen ikke ligger på compile classpath
    public static void main(String... args) throws Throwable {

        try {
            System.out.println("\n");
            {
                Class<?> messengerClass = Messenger.class.getClassLoader().loadClass("Special");

                Method method = messengerClass.getDeclaredMethod("getMessage", new Class[0]);
                method.invoke(null);
            }
            System.out.println("\n");

        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }

        System.exit(0);
    }
}
