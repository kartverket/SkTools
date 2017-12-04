package com.sun.tools.xjc.addon.statkart;

/* Work around: klassen må ligge i pakke som starter med com.sun.tools.xjc.addon. Ellers kjører ikke denne sammen med java6 der jaxb 2.0 er pre installert */

import com.sun.codemodel.*;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.CCustomizations;
import com.sun.tools.xjc.model.CPluginCustomization;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;
import com.sun.tools.xjc.util.DOMUtils;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Plugin for JAXB XJC task (GPLAN-115)
 * <p/>
 * Plug in som dekorerer genererte java klasser med dokumentasjon.
 * <p/>
 * #doc tag er valgfri
 * #see tag kan enten angis, eller så mappes det til et standard pakkenavn.
 * <p/>
 * Eksempel på bruk:
 * <xjc ...>
 * ...
 * <arg line="-grunnbokDoc no.statkart.grunnbok.fast.wsapi.domain no.statkart.grunnbok.fast.domain" />
 * </xjc>
 * <p/>
 * Ovenstående eksempel mapper pakke navn i @see tag fra no.statkart.grunnbok.fast.wsapi.domain.* til no.statkart.grunnbok.fast.domain.*
 *
 * @author Leif Lislegård
 * @since 0.3
 */
public class GrunnbokDocPlugin extends com.sun.tools.xjc.Plugin {

    public static final String NS = "http://grunnbok.statkart.no/tools/gdoc";

    //package name mapping
    public String MAPPING_FROM = "no.statkart.grunnbok.fast.basis.wsapi.domain";

    //package name mapping
    public String MAPPING_TO = "no.statkart.grunnbok.fast.basis.domain";


    /**
     * Defines a token that activates this filter.
     */
    @Override
    public String getOptionName() {
        return "grunnbokDoc";
    }

    @Override
    public String getUsage() {
        return "Legger til dokumentasjon hentet ifra domeneklasser. \n";
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
            if (localName.equals("doc")) {
                return true;
            } else if (localName.equals("see")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int parseArgument(Options opt, String[] args, int i) throws BadCommandLineException, IOException {

        if (args[i].startsWith("-grunnbokDoc")) {
            if (args.length > i + 1 && !args[i + 1].startsWith("-")) {
                MAPPING_FROM = args[i + 1];
                if (args.length > i + 2 && !args[i + 2].startsWith("-")) {
                    MAPPING_TO = args[i + 2];
                    return 3;
                }
                return 2;
            }
            return 1;
        }
        return 0;
    }

    @Override
    public boolean run(Outline outline, Options options, ErrorHandler errorHandler) throws SAXException {

        for (ClassOutline classOutline : outline.getClasses()) {
            StringBuilder header = new StringBuilder();
            StringBuilder footer = new StringBuilder();
            header.append("Java klasse for ").append(classOutline.target.getTypeName()).append('.');

            String javadoc = classOutline.target.javadoc;   //extracting std generated javadoc
            int start = javadoc.indexOf("<pre>");
            if (start != -1) {
                int end = javadoc.lastIndexOf("</pre>");
                String schemaDefinition = javadoc.subSequence(start, end + 6).toString();
                footer.append("\nSchema definisjon:\n\n").append(schemaDefinition);
            }

            JDocComment doc = classOutline.implClass.javadoc();
            doc.set(0, header.toString());

            handleDocCustomization(doc, classOutline.target.getCustomizations());
            doc.add("\n\n<p>"); //adds a new line
            if (footer.length() > 0) {
                doc.add(footer.toString());
                doc.add("\n"); //adds a new line
            }
            handleSeeCustomization(doc, classOutline.target.getCustomizations(), classOutline);

            for (FieldOutline fieldOutline : classOutline.getDeclaredFields()) {
                String privateName = fieldOutline.getPropertyInfo().getName(false);

                // Sett javadoc på feltet (det blir som standard satt på getter)
                JFieldVar fieldVar = classOutline.implClass.fields().get(privateName);
                if (fieldVar != null) {
                    fieldVar.javadoc().append(fieldOutline.getPropertyInfo().javadoc);
                }

                // Fjern javadoc på getter og setter
                String publicName = fieldOutline.getPropertyInfo().getName(true);
                JType type = fieldOutline.getRawType();

                JMethod getter = classOutline.implClass.getMethod("get" + publicName, new JType[0]);
                if (getter != null) {
                    JDocComment getterDoc = getter.javadoc();
                    getterDoc.clear();
                    JCommentPart returnDoc = getterDoc.addReturn();
                    returnDoc.clear();
                }

                JMethod setter = classOutline.implClass.getMethod("set" + publicName, new JType[]{type});
                if (setter != null) {
                    JDocComment setterDoc = setter.javadoc();
                    setterDoc.clear();
                    JCommentPart paramDoc = setterDoc.addParam("value");
                    paramDoc.clear();
                }
            }

        }

        return true;
    }


    /**
     * Adds optional documentation to the javadoc
     */
    private void handleDocCustomization(JDocComment doc, CCustomizations customizations) {
        CPluginCustomization c = customizations.find(NS, "doc");
        if (c != null) { // customization found for this class outline
            c.markAsAcknowledged();

            doc.add("\n<br><p>\n");
            doc.add(DOMUtils.getElementText(c.element)); //appends the documentation found to the javadoc

        }
    }

    /**
     * Adds a default @see comment to the javadoc. This can optionally be spesified as a customization
     */
    private void handleSeeCustomization(JDocComment doc, CCustomizations customizations, ClassOutline outline) {
        CPluginCustomization c = customizations.find(NS, "see");

        String see = null;

        if (c != null) { // customization found for this class outline
            c.markAsAcknowledged();
            see = DOMUtils.getElementText(c.element);
        } else {
            see = outline.target.getName();
            see = see.replaceFirst(MAPPING_FROM, MAPPING_TO);
            //todo: assure that the refering class exists in classpath?

            if (see.endsWith("List")) { //no @see for *List classes 
                see = null;
            }

        }

        if (see != null) {
            doc.add("\n@see " + see); //appends the documentation found to the javadoc
        }
    }
}