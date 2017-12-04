package no.statkart.sktools.utils.wsdocgen.processor.xml;

import no.statkart.sktools.utils.wsdocgen.processor.util.JavaDocUtils;

import javax.annotation.processing.ProcessingEnvironment;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bygging av {@code <description>}
 *
 * @author Leif Lislegård
 * @since 1.3 - SKTOOLS-108
 */
@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class XMLDescriptionBuilder {

    private static final Pattern spanElementPattern = Pattern.compile("<span class=\"(\\w+)\">(.*?)</span>");

    private final XMLBuilderFactory factory;
    private final ProcessingEnvironment processingEnv;
    private final org.w3c.dom.Document document;


    XMLDescriptionBuilder(XMLBuilderFactory factory) {
        this.factory = factory;
        this.processingEnv = factory.getProcessingEnv();
        this.document = factory.getDocument();
    }


    public org.w3c.dom.Element buildDescription(JavaDocUtils javaDocUtils) {
        return buildDescription(javaDocUtils != null ? javaDocUtils.getText() : null);
    }

    public org.w3c.dom.Element buildDescription(String text) {
        final org.w3c.dom.Element descriptionElement = document.createElement("description");

        if (text != null) {
            final Matcher matcher = spanElementPattern.matcher(text);
            int last = 0;

            while (matcher.find()) {
                appendTextNodeTo(descriptionElement, text, last, matcher.start());
                final String cssClass = matcher.group(1);
                final String substring = matcher.group(2);
                appendSpanNodeTo(descriptionElement, substring, cssClass);
                last = matcher.end();
            }

            appendTextNodeTo(descriptionElement, text, last, text.length());
        }

        return descriptionElement;
    }

    private void appendTextNodeTo(org.w3c.dom.Element descriptionElement, String text, int from, int to) {
        if (to > from) {
            final String substring = text.substring(from, to);
            if (!substring.trim().isEmpty()) {
                descriptionElement.appendChild(document.createTextNode(substring));
            }
        }
    }

    private void appendSpanNodeTo(org.w3c.dom.Element descriptionElement, String text, String cssClass) {
        if (text != null) {
            descriptionElement.appendChild(buildSpanNode(text, cssClass));
        }
    }

    private org.w3c.dom.Element buildSpanNode(String text, String cssClass) {
        final org.w3c.dom.Element spanElement = document.createElement("span");
        spanElement.setTextContent(text);

        if (cssClass != null) {
            spanElement.setAttribute("class", cssClass);
        }

        return spanElement;
    }


}
