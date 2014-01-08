package no.statkart.sktools.utils.wsdocgen.processor.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses blocks of javadoc
 *
 * @author Leif Lislegård
 * @since 1.3 - ny grunnbok sprint 30
 */
public class JavaDocUtils {

    /**
     * <pre>
     *
     * Group 0: whole -unfiltered- sentence
     * Group 1: tag-type
     * Group 2: not empty doc when {@code group(1) != null}
     * Group 3: name or comment
     * Group 4: has comment when {@code group(3) != null}
     * Group 5: comment
     *
     * </pre>
     */
    private static final Pattern tagletPattern = Pattern.compile("^\\s*@(\\S*)(\\s+(\\S*)(\\s+(.*))?)?");
    private static final Pattern inlineTagletPattern = Pattern.compile("\\{@(\\w+) ([^\\}]*)\\}");

    private final Map<String, Map<String, String>> tags;


    public static JavaDocUtils parse(String text) {
        return new JavaDocUtils(text);
    }

    private JavaDocUtils(String text) {
        this.tags = parseJavaDocTags(text);
    }

    public Map<String, Map<String, String>> getAllTags() {
        return tags;
    }

    /**
     * @return text without any tagglet-{@code @tags}
     */
    public String getText() {
        return getTagsForType("").get("");
    }

    public String getReturn() {
        return getTagsForType("return").get("");
    }

    public Map<String, String> getParams() {
        return getTagsForType("param");
    }

    public Map<String, String> getThrows() {
        return getTagsForType("throws");
    }


    /**
     * @return parameters for type or empty map.
     */
    private Map<String, String> getTagsForType(String type) {
        if (tags.containsKey(type)) {
            return tags.get(type);
        }
        return Collections.emptyMap();
    }

    public static Map<String, Map<String, String>> parseJavaDocTags(String docComment) {
        Map<String, Map<String, String>> tags = new HashMap<String, Map<String, String>>();
        if (docComment != null) {
            StringTokenizer st = new StringTokenizer(docComment, "\n", true);
            StringBuilder sb = new StringBuilder();
            while (st.hasMoreTokens()) {
                String token = st.nextToken();

                final Matcher tagMatcher = tagletPattern.matcher(token);
                if (tagMatcher.find()) {

                    String type = tagMatcher.group(1);
                    Map<String, String> typeMap = tags.get(type);
                    if (typeMap == null) {
                        typeMap = new LinkedHashMap<String, String>();
                        tags.put(type, typeMap);
                    }

                    if (tagMatcher.group(2) != null && ("param".equals(type) || "throws".equals(type))) {
                        String name = tagMatcher.group(3);
                        String value = tagMatcher.group(4) != null ? tagMatcher.group(5) : null;
                        typeMap.put(name, createDocString(value));
                    } else {
                        String name = "";
                        String value = tagMatcher.group(2);
                        String duplicate = typeMap.put(name, createDocString(value));
                        if (duplicate != null) {
                            System.out.println(String.format("Warning: Duplicate tag found; type: %s",type)); //todo: logge dette til warning?
                        }
                    }

                } else if (!token.trim().isEmpty()) {
                    sb.append(token.trim()).append('\n');
                }
            }
            tags.put("", Collections.singletonMap("", createDocString(sb.toString())));
        }
        return tags;
    }

    private static String createDocString(String doc) {
        String value = doc;
        if (value != null) {
            final StringBuffer sb = new StringBuffer();
            Matcher matcher = inlineTagletPattern.matcher(value.trim());
            while (matcher.find()) {
                final String tagletValue = matcher.group(2); //SKTOOLS-108
                if (tagletValue != null) {
                    matcher.appendReplacement(sb, "<span class=\"javadoc_tag_$1\">" + tagletValue + "</span>");   //SKTOOLS-108
                } else {
                    sb.append(matcher.group());
                }
            }
            matcher.appendTail(sb);
            value = sb.toString();

        }
        return value;
    }

}
