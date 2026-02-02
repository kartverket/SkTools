package no.statkart.sktools.gradle.plugins.dbtools.database.util

import org.gradle.api.UncheckedIOException
import org.gradle.internal.IoActions

import java.util.regex.Matcher
import java.util.regex.Pattern

class Utils {

    private static final Pattern WORD_SEPARATOR

    static {
        WORD_SEPARATOR = Pattern.compile("\\W+")
    }

    static String toCamelCase(CharSequence string) {
        return toCamelCaseInternal(string, false)
    }

    static String toLowerCamelCase(CharSequence string) {
        return toCamelCaseInternal(string, true)
    }

    private static String toCamelCaseInternal(CharSequence string, boolean lower) {
        if (string == null) {
            return null
        } else {
            StringBuilder builder = new StringBuilder()
            Matcher matcher = WORD_SEPARATOR.matcher(string)
            int pos = 0
            boolean first = true

            while(matcher.find()) {
                String chunk = string.subSequence(pos, matcher.start()).toString()
                pos = matcher.end()
                if (!chunk.isEmpty()) {
                    if (lower && first) {
                        chunk = chunk.uncapitalize()
                        first = false
                    } else {
                        chunk = chunk.capitalize()
                    }

                    builder.append(chunk)
                }
            }

            String rest = string.subSequence(pos, string.length()).toString()
            if (lower && first) {
                rest = rest.uncapitalize()
            } else {
                rest = rest.capitalize()
            }

            builder.append(rest)
            return builder.toString()
        }
    }

    static Properties loadProperties(InputStream inputStream) {
        Properties properties = new Properties()

        try {
            properties.load(inputStream)
        } catch (IOException e) {
            throw new UncheckedIOException(e)
        } finally {
            IoActions.closeQuietly(inputStream)
        }

        return properties
    }

}
