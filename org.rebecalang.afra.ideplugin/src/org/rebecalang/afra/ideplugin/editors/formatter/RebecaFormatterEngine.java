package org.rebecalang.afra.ideplugin.editors.formatter;

/**
 * Token-based formatter engine for Rebeca (.rebeca) files.
 * Focuses on correct indentation and robust spacing around tokens,
 * while preserving comments and string/char literals.
 */
final class RebecaFormatterEngine {

    private final String newline;
    private String indentUnit;

    RebecaFormatterEngine(String preferredIndentUnit, String newline) {
        this.indentUnit = preferredIndentUnit == null ? "\t" : preferredIndentUnit;
        this.newline = newline == null ? System.getProperty("line.separator") : newline;
    }

    String getIndentUnit() {
        return indentUnit;
    }

    String format(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }

        String normalized = content.replace("\r\n", "\n").replace("\r", "\n");
        // Detect indentation unit from the content
        String detected = detectIndentString(normalized);
        if (detected != null) {
            this.indentUnit = detected;
        }

        State state = new State();
        StringBuilder out = new StringBuilder();
        Lexer lexer = new Lexer(normalized);
        Token prev = null;
        Token t;
        while ((t = lexer.next()) != null) {
            if (state.pendingNewlineAfterCloseBrace) {
                if (isJoinerAfterCloseBrace(t)) {
                    if (!endsWithSpaceOrNewline(out)) out.append(' ');
                    state.pendingNewlineAfterCloseBrace = false;
                } else {
                    if (!state.atLineStart) out.append(newline);
                    state.atLineStart = true;
                    state.pendingNewlineAfterCloseBrace = false;
                }
            }

            switch (t.type) {
                case WHITESPACE:
                case NEWLINE:
                    // ignore original layout
                    break;
                case LINE_COMMENT: {
                    if (state.atLineStart) indent(out, state.indentLevel);
                    else if (!endsWithSpace(out)) out.append(' ');
                    out.append(t.text);
                    out.append(newline);
                    state.atLineStart = true;
                    state.pendingNewlineAfterCloseBrace = false;
                    break;
                }
                case BLOCK_COMMENT: {
                    if (state.atLineStart) indent(out, state.indentLevel);
                    else if (!endsWithSpace(out)) out.append(' ');
                    appendBlockComment(out, t.text, state);
                    state.pendingNewlineAfterCloseBrace = false;
                    break;
                }
                case OPEN_BRACE: {
                    if (!state.atLineStart && !endsWithSpaceOrNewline(out)) out.append(' ');
                    if (state.atLineStart) indent(out, state.indentLevel);
                    out.append('{');
                    out.append(newline);
                    state.atLineStart = true;
                    state.indentLevel++;
                    state.pendingNewlineAfterCloseBrace = false;
                    break;
                }
                case CLOSE_BRACE: {
                    if (!state.atLineStart) out.append(newline);
                    state.indentLevel = Math.max(0, state.indentLevel - 1);
                    indent(out, state.indentLevel);
                    out.append('}');
                    state.atLineStart = false;
                    state.pendingNewlineAfterCloseBrace = true;
                    break;
                }
                case SEMICOLON: {
                    trimTrailingSpace(out);
                    out.append(';');
                    out.append(newline);
                    state.atLineStart = true;
                    state.pendingNewlineAfterCloseBrace = false;
                    break;
                }
                case COMMA: {
                    trimTrailingSpace(out);
                    out.append(',');
                    out.append(' ');
                    state.atLineStart = false;
                    state.pendingNewlineAfterCloseBrace = false;
                    break;
                }
                case DOT: {
                    out.append('.');
                    state.atLineStart = false;
                    state.pendingNewlineAfterCloseBrace = false;
                    break;
                }
                case COLON: {
                    // No spaces around ':' in .rebeca samples
                    trimTrailingSpace(out);
                    out.append(':');
                    state.atLineStart = false;
                    state.pendingNewlineAfterCloseBrace = false;
                    break;
                }
                case OPEN_PAREN: {
                    if (prev != null && prev.type == TokenType.KEYWORD && needsSpaceBeforeParen(prev.text)) {
                        if (!endsWithSpaceOrNewline(out)) out.append(' ');
                    }
                    out.append('(');
                    state.atLineStart = false;
                    state.pendingNewlineAfterCloseBrace = false;
                    break;
                }
                case CLOSE_PAREN: {
                    trimTrailingSpace(out);
                    out.append(')');
                    state.atLineStart = false;
                    state.pendingNewlineAfterCloseBrace = false;
                    break;
                }
                case OPEN_BRACKET: out.append('['); state.atLineStart=false; break;
                case CLOSE_BRACKET: out.append(']'); state.atLineStart=false; break;

                case OPERATOR: {
                    String op = t.text;
                    boolean unary = isUnary(op, prev);
                    if (unary) {
                        out.append(op);
                    } else {
                        if (!endsWithSpaceOrNewline(out)) out.append(' ');
                        out.append(op);
                        out.append(' ');
                    }
                    state.atLineStart = false;
                    state.pendingNewlineAfterCloseBrace = false;
                    break;
                }
                case IDENTIFIER:
                case KEYWORD:
                case NUMBER:
                case STRING:
                case CHAR: {
                    if (state.atLineStart) indent(out, state.indentLevel);
                    else if (needsSpaceBetween(prev, t)) out.append(' ');
                    out.append(t.text);
                    state.atLineStart = false;
                    break;
                }
                default: {
                    if (state.atLineStart) indent(out, state.indentLevel);
                    out.append(t.text);
                    state.atLineStart = false;
                    state.pendingNewlineAfterCloseBrace = false;
                }
            }

            if (t.type != TokenType.WHITESPACE && t.type != TokenType.NEWLINE) prev = t;
        }

        return out.toString().replaceAll("(\r?\n)+$", newline);
    }

    private static boolean isJoinerAfterCloseBrace(Token t) {
        // Tokens that should follow '}' on the same line: else, catch, finally
        if (t == null) return false;
        if (t.type == TokenType.KEYWORD || t.type == TokenType.IDENTIFIER) {
            String x = t.text;
            return "else".equals(x) || "catch".equals(x) || "finally".equals(x);
        }
        return false;
    }

    private void indent(StringBuilder out, int level) {
        for (int j = 0; j < level; j++) out.append(indentUnit);
    }

    private static boolean endsWithSpace(StringBuilder out) {
        int len = out.length();
        return len > 0 && out.charAt(len - 1) == ' ';
    }
    private static boolean endsWithSpaceOrNewline(StringBuilder out) {
        int len = out.length();
        if (len == 0) return false;
        char c = out.charAt(len - 1);
        return c == ' ' || c == '\n' || c == '\r';
    }
    private static void trimTrailingSpace(StringBuilder out) {
        int len = out.length();
        if (len > 0 && out.charAt(len - 1) == ' ') out.deleteCharAt(len - 1);
    }

    private static boolean needsSpaceBetween(Token prev, Token curr) {
        if (prev == null) return false;
        if (prev.type == TokenType.OPEN_PAREN || curr.type == TokenType.CLOSE_PAREN) return false;
        if (prev.type == TokenType.OPEN_BRACKET || curr.type == TokenType.CLOSE_BRACKET) return false;
        if (prev.type == TokenType.DOT || curr.type == TokenType.DOT) return false;
        if (prev.type == TokenType.COLON || curr.type == TokenType.COLON) return false;
        if (curr.type == TokenType.OPEN_PAREN && (prev.type == TokenType.IDENTIFIER || prev.type == TokenType.KEYWORD)) return false;
        boolean prevWord = isWord(prev.type);
        boolean currWord = isWord(curr.type);
        if (prevWord && currWord) return true;
        if (prev.type == TokenType.KEYWORD && curr.type == TokenType.IDENTIFIER) return true;
        if (prev.type == TokenType.CLOSE_PAREN && (curr.type == TokenType.IDENTIFIER || curr.type == TokenType.NUMBER)) return false;
        return false;
    }

    private static boolean isWord(TokenType t) {
        return t == TokenType.IDENTIFIER || t == TokenType.KEYWORD || t == TokenType.NUMBER || t == TokenType.STRING || t == TokenType.CHAR;
    }
    private static boolean needsSpaceBeforeParen(String kw) {
        return "if".equals(kw) || "for".equals(kw) || "while".equals(kw) || "switch".equals(kw) || "catch".equals(kw);
    }
    private static boolean isUnary(String op, Token prev) {
        if ("!".equals(op) || "~".equals(op) || "++".equals(op) || "--".equals(op)) return true;
        if ("+".equals(op) || "-".equals(op)) {
            if (prev == null) return true;
            switch (prev.type) {
                case OPEN_PAREN:
                case OPEN_BRACKET:
                case OPEN_BRACE:
                case COMMA:
                case SEMICOLON:
                case OPERATOR:
                case COLON:
                case KEYWORD:
                    return true;
                default:
                    return false;
            }
        }
        return false;
    }
    private void appendBlockComment(StringBuilder out, String comment, State state) {
        String[] parts = comment.split("\n", -1);
        for (int i = 0; i < parts.length; i++) {
            String line = parts[i];
            if (i > 0) {
                out.append(newline);
                indent(out, state.indentLevel);
            }
            out.append(line);
        }
        out.append(newline);
        state.atLineStart = true;
    }

    private static final class State {
        int indentLevel = 0;
        boolean atLineStart = true;
        boolean pendingNewlineAfterCloseBrace = false;
    }

    private enum TokenType {
        WHITESPACE,
        NEWLINE,
        IDENTIFIER,
        KEYWORD,
        NUMBER,
        STRING,
        CHAR,
        LINE_COMMENT,
        BLOCK_COMMENT,
        OPEN_BRACE,
        CLOSE_BRACE,
        OPEN_PAREN,
        CLOSE_PAREN,
        OPEN_BRACKET,
        CLOSE_BRACKET,
        SEMICOLON,
        COMMA,
        DOT,
        COLON,
        OPERATOR,
        OTHER
    }

    private static final class Token {
        final TokenType type;
        final String text;
        Token(TokenType type, String text) { this.type = type; this.text = text; }
    }

    private static final class Lexer {
        private final String s;
        private final int n;
        private int i = 0;
        Lexer(String s) { this.s = s; this.n = s.length(); }

        Token next() {
            if (i >= n) return null;
            char c = s.charAt(i);

            if (c == ' ' || c == '\t') {
                int start = i; while (i < n && (s.charAt(i) == ' ' || s.charAt(i) == '\t')) i++;
                return new Token(TokenType.WHITESPACE, s.substring(start, i));
            }
            if (c == '\n' || c == '\r') {
                int start = i; if (c == '\r' && i + 1 < n && s.charAt(i + 1) == '\n') i += 2; else i++;
                return new Token(TokenType.NEWLINE, s.substring(start, i));
            }
            if (c == '/' && i + 1 < n) {
                char d = s.charAt(i + 1);
                if (d == '/') {
                    int start = i; i += 2; while (i < n && s.charAt(i) != '\n' && s.charAt(i) != '\r') i++;
                    return new Token(TokenType.LINE_COMMENT, s.substring(start, i));
                } else if (d == '*') {
                    int start = i; i += 2; while (i + 1 < n && !(s.charAt(i) == '*' && s.charAt(i + 1) == '/')) i++;
                    if (i + 1 < n) i += 2; return new Token(TokenType.BLOCK_COMMENT, s.substring(start, i));
                }
            }
            if (c == '"') {
                int start = i++;
                while (i < n) { char ch = s.charAt(i++); if (ch == '\\' && i < n) { i++; continue; } if (ch == '"') break; }
                return new Token(TokenType.STRING, s.substring(start, i));
            }
            if (c == '\'') {
                int start = i++;
                while (i < n) { char ch = s.charAt(i++); if (ch == '\\' && i < n) { i++; continue; } if (ch == '\'') break; }
                return new Token(TokenType.CHAR, s.substring(start, i));
            }
            if (isIdentStart(c) || c == '?') {
                int start = i++;
                while (i < n && isIdentPart(s.charAt(i))) i++;
                String text = s.substring(start, i);
                if (isKeyword(text)) return new Token(TokenType.KEYWORD, text);
                return new Token(TokenType.IDENTIFIER, text);
            }
            if (isDigit(c)) {
                int start = i++;
                while (i < n && (isDigit(s.charAt(i)) || isIdentPart(s.charAt(i)))) i++;
                return new Token(TokenType.NUMBER, s.substring(start, i));
            }
            switch (c) {
                case '{': i++; return new Token(TokenType.OPEN_BRACE, "{");
                case '}': i++; return new Token(TokenType.CLOSE_BRACE, "}");
                case '(': i++; return new Token(TokenType.OPEN_PAREN, "(");
                case ')': i++; return new Token(TokenType.CLOSE_PAREN, ")");
                case '[': i++; return new Token(TokenType.OPEN_BRACKET, "[");
                case ']': i++; return new Token(TokenType.CLOSE_BRACKET, "]");
                case ';': i++; return new Token(TokenType.SEMICOLON, ";");
                case ',': i++; return new Token(TokenType.COMMA, ",");
                case '.': i++; return new Token(TokenType.DOT, ".");
                case ':': i++; return new Token(TokenType.COLON, ":");
            }
            if (i + 1 < n) {
                String two = s.substring(i, i + 2);
                if (isOperator(two)) { i += 2; return new Token(TokenType.OPERATOR, two); }
            }
            if (isOperator(String.valueOf(c))) { i++; return new Token(TokenType.OPERATOR, String.valueOf(c)); }
            i++; return new Token(TokenType.OTHER, String.valueOf(c));
        }

        private boolean isIdentStart(char c) { return Character.isLetter(c) || c == '_' || c == '$'; }
        private boolean isIdentPart(char c) { return Character.isLetterOrDigit(c) || c == '_' || c == '$'; }
        private boolean isDigit(char c) { return c >= '0' && c <= '9'; }
        private boolean isKeyword(String s) {
            return s.equals("if") || s.equals("else") || s.equals("for") || s.equals("while") || s.equals("switch") || s.equals("catch") ||
                   s.equals("msgsrv") || s.equals("reactiveclass") || s.equals("knownrebecs") || s.equals("statevars") || s.equals("main") ||
                   s.equals("env") || s.equals("assertion") || s.equals("delay") || s.equals("return") || s.equals("do") || s.equals("finally");
        }
        private boolean isOperator(String s) {
            return s.equals("==") || s.equals("!=") || s.equals("<=") || s.equals(">=") || s.equals("&&") || s.equals("||") ||
                   s.equals("++") || s.equals("--") || s.equals("+=") || s.equals("-=") || s.equals("*=") || s.equals("/=") || s.equals("%=") ||
                   s.equals("<<") || s.equals(">>") || s.equals("<") || s.equals(">") || s.equals("=") || s.equals("+") || s.equals("-") ||
                   s.equals("*") || s.equals("/") || s.equals("%") || s.equals("!") || s.equals("~") || s.equals("&") || s.equals("|") || s.equals("^");
        }
    }

    static String detectIndentString(String content) {
        String[] lines = content.split("\n");
        boolean hasTab = false;
        int gcd = 0;
        for (String line : lines) {
            if (line.isEmpty()) continue;
            int idx = 0;
            boolean lineHasTab = false;
            while (idx < line.length()) {
                char c = line.charAt(idx);
                if (c == '\t') { lineHasTab = true; break; }
                else if (c == ' ') idx++;
                else break;
            }
            if (lineHasTab) { hasTab = true; break; }
            if (idx > 0) gcd = (gcd == 0) ? idx : gcd(gcd, idx);
        }
        if (hasTab) return "\t";
        if (gcd <= 0) return "\t";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < gcd; i++) sb.append(' ');
        return sb.toString();
    }

    private static int gcd(int a, int b) {
        while (b != 0) { int t = a % b; a = b; b = t; }
        return Math.abs(a);
    }
}


