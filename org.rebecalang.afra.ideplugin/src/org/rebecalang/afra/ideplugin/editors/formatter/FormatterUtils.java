package org.rebecalang.afra.ideplugin.editors.formatter;

/**
 * Utility class containing shared formatting logic for Afra language formatters
 */
public class FormatterUtils {
    
    private static final String INDENT = "\t";
    
    /**
     * Private constructor to prevent instantiation
     */
    private FormatterUtils() {
        // Utility class
    }
    
    /**
     * Get the indentation string used by formatters
     * @return The indentation string (tab character)
     */
    public static String getIndentString() {
        return INDENT;
    }
    
    /**
     * Format content by applying all formatting steps in sequence
     * @param content The content to format
     * @return The formatted content
     */
    public static String formatContent(String content) {
        if (content == null || content.isEmpty()) return "";

        String normalized = normalizeBrackets(content);
        String indented = applyIndentation(normalized);
        String collapsed = collapseBlankLines(indented);
        String withSpacing = addNewlineAfterClosingBraces(collapsed);

        return withSpacing.trim();
    }

    /** Step 1 & 2: Normalize spaces and move { } to separate lines (ignoring comments) */
    public static String normalizeBrackets(String input) {
        String code = input.replaceAll("\r\n", "\n"); // normalize line endings
        code = code.replaceAll("[ \t]+", " ");        // collapse multiple spaces

        StringBuilder normalized = new StringBuilder();
        boolean inBlockComment = false;
        boolean inLineComment = false;

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);

            // detect start/end of comments
            if (!inBlockComment && !inLineComment && i + 1 < code.length()) {
                if (c == '/' && code.charAt(i + 1) == '*') {
                    inBlockComment = true;
                } else if (c == '/' && code.charAt(i + 1) == '/') {
                    inLineComment = true;
                }
            }
            if (inBlockComment && i > 0 && code.charAt(i - 1) == '*' && c == '/') {
                inBlockComment = false;
            }
            if (inLineComment && c == '\n') {
                inLineComment = false;
            }

            // only break brackets if not inside comments
            if (!inBlockComment && !inLineComment && (c == '{' || c == '}')) {
                normalized.append("\n").append(c).append("\n");
            } else {
                normalized.append(c);
            }
        }

        return normalized.toString();
    }

    /** Step 3: Indent lines based on scope depth */
    public static String applyIndentation(String normalized) {
        String[] lines = normalized.split("\n");
        StringBuilder indented = new StringBuilder();
        int indent = 0;
        boolean inBlockComment = false;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            // Track block comment state
            if (line.startsWith("/*")) {
                inBlockComment = true;
            }
            if (line.endsWith("*/")) {
                inBlockComment = false;
            }

            // Adjust indent BEFORE printing if line is closing bracket
            if (!inBlockComment && line.equals("}")) {
                indent = Math.max(0, indent - 1);
            }

            // Append line with indent
            indented.append(INDENT.repeat(indent)).append(line).append("\n");

            // Adjust indent AFTER printing if line is opening bracket
            if (!inBlockComment && line.equals("{")) {
                indent++;
            }
        }

        return indented.toString();
    }

    /** Step 4: Collapse multiple blank lines into one */
    public static String collapseBlankLines(String text) {
        return text.replaceAll("(?m)^[ \t]*\n{2,}", "\n");
    }

    /** Step 5: Add extra newline after } unless followed by else/else if */
    public static String addNewlineAfterClosingBraces(String text) {
        String[] lines = text.split("\n");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            result.append(lines[i]).append("\n");

            if (line.equals("}")) {
                // look ahead
                int j = i + 1;
                while (j < lines.length && lines[j].trim().isEmpty()) {
                    j++;
                }
                if (j < lines.length) {
                    String next = lines[j].trim();
                    if (!next.startsWith("else")) {
                        result.append("\n");
                    }
                } else {
                    result.append("\n"); // last line
                }
            }
        }

        return result.toString();
    }
}
