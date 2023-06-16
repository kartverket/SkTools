package no.statkart.sktools.utils.wsdocgen.processor.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
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
     * Group 0: whole -unfiltered- sentence
     * Group 1: tag-type
     * </pre>
     */
    private static final Pattern tagletPattern = Pattern.compile("^\\s*@(\\S+)");
    private static final Pattern inlineTagletPattern = Pattern.compile("\\{@(\\w+) ([^}]*)}");

    static final String DESCRIPTION = "";
    static final String TAG_RETURN = "return";
    static final String TAG_PARAM = "param";
    static final String TAG_THROWS = "throws";
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
        return getTagsForType(DESCRIPTION).get("");
    }

    public String getReturn() {
        return getTagsForType(TAG_RETURN).get("");
    }

    public Map<String, String> getParams() {
        return getTagsForType(TAG_PARAM);
    }

    public Map<String, String> getThrows() {
        return getTagsForType(TAG_THROWS);
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
        Map<String, Map<String, String>> tags = new HashMap<>();
        if (docComment != null) {
            String[] parts = docComment.split("\\n\\s*@");
            String description = null;
            if (parts[0].trim().startsWith("@")) {
                parseTag(tags, parts[0]);
            } else {
                description = createDocString(parts[0]);
            }
            for (int i = 1; i < parts.length; i++) {
                parseTag(tags, "@" + parts[i]);
            }
            tags.put(DESCRIPTION, Collections.singletonMap("", description));
        }
        return tags;
    }

    private static void parseTag(Map<String, Map<String, String>> tags, String tagText) {
        final Matcher tagMatcher = tagletPattern.matcher(tagText);
        if (tagMatcher.find()) {

            String tagName = tagMatcher.group(1);
            Map<String, String> variables = tags.get(tagName);
            if (variables == null) {
                variables = new LinkedHashMap<>();
                tags.put(tagName, variables);
            }

            boolean hasParameter = TAG_PARAM.equals(tagName) || TAG_THROWS.equals(tagName);
            String remainingText = tagText.substring(tagMatcher.group(0).length()).trim();

            String firstToken = hasParameter && !remainingText.isEmpty() ? remainingText.split("\\s+")[0] : "";
            if (!firstToken.isEmpty()) {
                remainingText = remainingText.substring(firstToken.length());
            }

            variables.put(firstToken, createDocString(remainingText));
        }
    }

    private static String createDocString(String doc) {
        if (doc != null && !doc.isEmpty()) {
            final StringBuffer sb = new StringBuffer();
            Matcher matcher = inlineTagletPattern.matcher(doc.trim());
            while (matcher.find()) {
                final String tagletValue = matcher.group(2);
                if (tagletValue != null) {
                    matcher.appendReplacement(sb, "<span class=\"javadoc_tag_$1\">" + tagletValue + "</span>");
                } else {
                    sb.append(matcher.group());
                }
            }
            matcher.appendTail(sb);
            return sb.toString();

        }
        return null;
    }

}
