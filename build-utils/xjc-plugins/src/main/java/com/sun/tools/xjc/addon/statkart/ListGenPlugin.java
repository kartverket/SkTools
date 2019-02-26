package com.sun.tools.xjc.addon.statkart;

import com.sun.codemodel.JClass;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.CPluginCustomization;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Plug-in som legger til felles liste adapter for alle lister.
 * <p/>
 * Utvalget er per konvensjon alle klasser som heter *List
 * <p/>
 * En kan parameterisere navn for {@link #LIST_METHOD getter metode}.
 * <p/>
 * En kan også parameterisere navn for {@link #BASE_CLASS superklasse for lister}. <nr />
 * Eksempel implementasjon av felles liste klasse:
 * <code><pre>

import java.util.Iterator;
import javax.xml.bind.annotation.XmlTransient;


&#64XmlTransient
public abstract class ListTestIterable<T> implements Iterable<T> {

    abstract public java.util.List<T> _getList();


    public Iterator<T> iterator() {
        return _getList().iterator();
    }

}

 *</pre></code>
 * <p/>
 * Det forutsettes at byggesystem kopierer inn kildekode for ListAdapter.java - se i config katalog for denne.
 * <p/>
 * <p/>
 *
 * @author Leif Lislegård
 * @since 1.1 - May 2011
 */
public class ListGenPlugin extends com.sun.tools.xjc.Plugin {

    public static final String NS = "http://grunnbok.statkart.no/tools/listgen";

    /**
     * Definerer superklasse for alle lister.
     * <p/>
     * Se {@link #parseArgument(com.sun.tools.xjc.Options, String[], int) parseArgument} for evt overstyring.
     */
    public static String BASE_CLASS = "no.statkart.grunnbok.skif.util.ListIterable";

    /**
     * Definerer metodenavn for henting av liste.
     * <p/>
     * Se {@link #parseArgument(com.sun.tools.xjc.Options, String[], int) parseArgument} for detaljer.
     */
    private static String LIST_METHOD = "getItem";

    /**
     * Defines a token that activates this filter.
     */
    @Override
    public String getOptionName() {
        return "listgen";
    }

    @Override
    public String getUsage() {
        return "Genererer liste klasser for klasser ihht til konvensjon. \n";
    }

    /**
     * Defines valid namespace
     */
    @Override
    public List<String> getCustomizationURIs() {
        return Collections.singletonList(NS);
    }

    /**
     * Defines valid tags
     */
    @Override
    public boolean isCustomizationTagName(String nsUri, String localName) {
        if (nsUri.equals(NS)) {
            if (localName.equals("listgen")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parser konfigurerbare parametere ifra byggesystem.
     * <p/>
     * Gyldige parametere er -listgen [[medthod=value] || [baseClass=value]]
     */
    @Override
    public int parseArgument(Options opt, String[] args, int i) throws BadCommandLineException, IOException {
        //holder styr på antall parametere parset
        int j = 0;

        if (args[i].startsWith("-listgen")) {
            j++;

            //parser evtentuelle properties
            while(args.length > i + j && !args[i + j].startsWith("-")) {
                StringTokenizer tokenizer = new StringTokenizer(args[i + j], "=");
                if (tokenizer.countTokens() == 2) {
                    String token = tokenizer.nextToken();
                    if (token.equalsIgnoreCase("method")) {
                        LIST_METHOD = tokenizer.nextToken();

                    } else if (token.equalsIgnoreCase("baseClass")) {
                        BASE_CLASS = tokenizer.nextToken();

                    } else {
                        throw new BadCommandLineException("Ukjent parameter: '" + token + "'");
                    }
                } else {
                    throw new BadCommandLineException("Feil ved parsing av parameter. Forventet syntaks: <navn>=<verdi>");
                }
                j++;
            }
        }
        System.out.println("return " + j);
        return j;
    }

    @Override
    public void onActivated(Options opts) throws BadCommandLineException {
        super.onActivated(opts);
//        generateBaseListClass(opts.targetDir);
    }


    @Override
    public boolean run(Outline outline, Options options, ErrorHandler errorHandler) throws SAXException {
        try {
            for (ClassOutline classOutline : outline.getClasses()) {

                //vi benytter ikke denne tagen i schema-definisjonen.... men påfører endringer per konvensjon.
                CPluginCustomization c = classOutline.target.getCustomizations().find(NS, "listgen");
                if (c != null) { // customization found for this class outline
                    c.markAsAcknowledged();
                }


                //alle klasser som ender på 'List' får per konvensjon påført endringer.
                if (classOutline.target.getSqueezedName().endsWith("List")) {

                    JClass listAdapter = null;
                    listAdapter = outline.getCodeModel().ref(BASE_CLASS);


                    applyListAdapter(listAdapter, classOutline, outline);
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }


        return true;
    }

    private static void applyListAdapter(JClass adapterClass, ClassOutline classOutline, Outline outline) throws ClassNotFoundException {
        String className = classOutline.target.getSqueezedName();
        className = className.substring(0, className.length() - 4);

        JClass clazz = outline.getCodeModel().ref(adapterClass.fullName());
        classOutline.implClass._extends(clazz);
        classOutline.implClass.direct(new StringBuilder()
                .append("\n\n    public java.util.List<").append(className).append("> _getList() {\n")
                .append("        return ").append(LIST_METHOD).append("();").append("\n    }")
                .toString()
        );
    }


    /**
     * Genererer standard superklasse for alle lister.
     * <p/>
     * Dersom en vil benytte en egen liste-implementasjon, override denne
     * via {@link #parseArgument(com.sun.tools.xjc.Options, String[], int) parseArgument}
     *
     * @param targetDir mappe
     */
    private void generateBaseListClass(File targetDir) {

        //samme som packgage definert i filen..

        File folder = new File(targetDir, new StringBuilder()
                .append("no").append(File.separatorChar)
                .append("statkart").append(File.separatorChar)
                .append("tools").append(File.separatorChar)
                .append("jaxb").append(File.separatorChar)
                .append("listgen")
                .toString());
        File baseClassFile = new File(folder, "ListIterable.java");

        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            folder.mkdirs();
            baseClassFile.exists();

            inputStream = this.getClass().getResourceAsStream("ListIterable.java");
            outputStream = new FileOutputStream(baseClassFile, false);

            byte[] buffer = new byte[4096]; // To hold file contents
            int bytes_read; // How many bytes in buffer

            while ((bytes_read = inputStream.read(buffer)) != -1) {
                // Read until EOF
                outputStream.write(buffer, 0, bytes_read); // write
            }

            outputStream.flush();

        } catch (FileNotFoundException e) {
            System.out.println("Feil ved aksessering av fil: " + baseClassFile.toString());
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Feil ved skriving til fil: " + baseClassFile.toString());
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    ;
                }
                try {
                    outputStream.close();
                } catch (IOException e) {
                    ;
                }
            }
        }
    }
}