package de.innologic.templateservice.rendering;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleTemplateRenderer {

    private static final Pattern TOKEN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*}}");

    public String render(String template, Map<String, Object> model, MissingKeyPolicy policy, boolean escapeHtml) {
        Matcher matcher = TOKEN.matcher(template);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = model.get(key);
            String replacement;
            if (value == null) {
                replacement = switch (policy) {
                    case KEEP_TOKEN -> matcher.group(0);
                    case EMPTY -> "";
                    case FAIL -> throw new IllegalArgumentException("MISSING_KEYS missingKeys=[" + key + "]");
                };
            } else {
                String text = String.valueOf(value);
                replacement = escapeHtml ? htmlEscape(text) : text;
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String htmlEscape(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
