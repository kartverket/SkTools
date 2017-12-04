import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

/**
 * Klient for visning av miljø variabler.
 *
 * @author Leif Lislegård
 */
public class WebstartTester {

    String[] applicationArgs = null;

    JFrame mainFrame;
    JTextArea textArea;


    public static void main(String[] args) {

        WebstartTester client = new WebstartTester();
        client.applicationArgs  = args;
    }


    public WebstartTester() {

        mainFrame = new JFrame("Web Start Test");
        java.awt.Container contentPane = mainFrame.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.setBackground(Color.white);
        textArea = new JTextArea();
        contentPane.add(new JScrollPane(textArea), BorderLayout.CENTER);

        textArea.setText("Velkommen til Java Web Start test\n\n");

        printJavaVersion();
        printApplicationArgs();
        printVmArgs();
        printHeapSizes();

        printClasspath();
        printSystemProperties();


        //JOptionPane.showMessageDialog(null, "Continue");

        mainFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                mainFrame.setVisible(false);
                System.exit(0);
            }
        });
        mainFrame.pack();
        mainFrame.validate();
        mainFrame.setSize(800, 800);
        mainFrame.setVisible(true);
    }


    private void printApplicationArgs() {
        printTextLine("Application args: ");
        if (applicationArgs == null || applicationArgs.length == 0) {
            printTextLine("<empty>");
        } else {
            for (String arg : applicationArgs) {
                printTextLine(arg);
            }
        }
        printTextLine("\n\n");

    }

    private void printSystemProperties() {
        printTextLine("System properties: ");

        ArrayList<String> lines = new ArrayList<String>();
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            lines.add(entry.getKey() + " = '" + entry.getValue() + "'");
        }
        Collections.sort(lines);

        for (String string : lines) {
            printTextLine(string);
        }
        printTextLine("\n\n");
    }


    private void printClasspath() {
        ClassLoader applicationClassLoader = this.getClass().getClassLoader();
        if (applicationClassLoader == null) {
            applicationClassLoader = ClassLoader.getSystemClassLoader();
        }
        URL[] urls = ((URLClassLoader) applicationClassLoader).getURLs();
        printTextLine("Found classpath: ");
        for (URL url : urls) {
            printTextLine(" += " + url.getFile());
        }
        printTextLine("\n\n");
    }

    private void printJavaVersion() {
        String javaVersion = System.getProperty("java.version");
        String javaVendor = System.getProperty("java.vm.vendor");

        printTextLine("Java version:" + javaVendor + " " + javaVersion);
        printTextLine("\n\n");
    }

    private void printHeapSizes() {
        printTextLine("Heap-info:");
        printTextLine(" - max heap(MB): " + (Runtime.getRuntime().maxMemory() / 1024 / 1024));
        printTextLine(" - current heap(MB): " + (Runtime.getRuntime().totalMemory() / 1024 / 1024));
        printTextLine(" - free heap(MB): " + (Runtime.getRuntime().freeMemory() / 1024 / 1024));

        printTextLine("\n\n");
    }

    private void printVmArgs() {
        printTextLine("VmArgs:");
        RuntimeMXBean RuntimemxBean = ManagementFactory.getRuntimeMXBean();
        for (String arg : RuntimemxBean.getInputArguments()) {
            printTextLine("\t" + arg);
        }

        printTextLine("\n\n");
    }

    private void printTextLine(String message) {
        System.out.print(message);
        textArea.append(message + "\n");
    }


}
