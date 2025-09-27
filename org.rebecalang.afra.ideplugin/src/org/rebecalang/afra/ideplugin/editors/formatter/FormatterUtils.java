package org.rebecalang.afra.ideplugin.editors.formatter;
public class FormatterUtils {
    private static final String INDENT = "\t";
  
    private FormatterUtils() {
    }

    public static String getIndentString() {
        return INDENT;
    }

    public static String formatContent(String content) {
        if (content == null || content.isEmpty()) return "";

        String normalized = normalizeBrackets(content);
        String indented = applyIndentation(normalized);
        String collapsed = collapseBlankLines(indented);
        String withSpacing = addNewlineAfterClosingBraces(collapsed);
        String inlineBraces = attachOpeningBraces(withSpacing);
        
        return inlineBraces.trim();
    }

    /** 1: normalize spaces and move { } to separate lines (ignoring comments) */
    public static String normalizeBrackets(String input) {
        String code = input.replaceAll("\r\n", "\n");
        code = code.replaceAll("[ \t]+", " ");

        StringBuilder normalized = new StringBuilder();
        boolean inBlockComment = false;
        boolean inLineComment = false;

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);

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

            if (!inBlockComment && !inLineComment && (c == '{' || c == '}')) {
                normalized.append("\n").append(c).append("\n");
            } else {
                normalized.append(c);
            }
        }

        return normalized.toString();
    }

    /** 2: indent lines based on scope depth */
    public static String applyIndentation(String normalized) {
        String[] lines = normalized.split("\n");
        StringBuilder indented = new StringBuilder();
        int indent = 0;
        boolean inBlockComment = false;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("/*")) {
                inBlockComment = true;
            }
            if (line.endsWith("*/")) {
                inBlockComment = false;
            }

            if (!inBlockComment && line.equals("}")) {
                indent = Math.max(0, indent - 1);
            }

            indented.append(INDENT.repeat(indent)).append(line).append("\n");

            if (!inBlockComment && line.equals("{")) {
                indent++;
            }
        }

        return indented.toString();
    }

    /** 3: collapse multiple blank lines into one */
    public static String collapseBlankLines(String text) {
        return text.replaceAll("(?m)^[ \t]*\n{2,}", "\n");
    }

    /**  4: Add extra newline after } unless followed by else/else if */
    public static String addNewlineAfterClosingBraces(String text) {
        String[] lines = text.split("\n");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            result.append(lines[i]).append("\n");

            if (line.equals("}")) {
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
                    result.append("\n");
                }
            }
        }

        return result.toString();
    }

    
    /** 5: as discussed in the meeting, get { to the previous line for more standard formatting */
    public static String attachOpeningBraces(String input) {
        String[] lines = input.split("\n", -1);
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            if (line.trim().equals("{")) {
                int lastNewline = result.lastIndexOf("\n");
                if (lastNewline >= 0) {
                    result.deleteCharAt(result.length() - 1);
                    result.append(" {").append("\n");
                } else {
                    result.append("{\n");
                }
            } else {
                result.append(line).append("\n");
            }
        }

        return result.toString();
    }
}
