package org.rebecalang.afra.ideplugin.editors.formatter;

/**
 * Token-based formatter engine for Property (.property) files.
 * Enforces indentation via braces and spaces around operators, with
 * specific rule: after ':' a single space (no space before ':').
 */
final class PropertyFormatterEngine {

    private final String newline;
    private String indentUnit;

    PropertyFormatterEngine(String preferredIndentUnit, String newline) {
        this.indentUnit = preferredIndentUnit == null ? "\t" : preferredIndentUnit;
        this.newline = newline == null ? System.getProperty("line.separator") : newline;
    }

    String getIndentUnit() { return indentUnit; }

    String format(String content) {
        if (content == null || content.trim().isEmpty()) return content;
        String normalized = content.replace("\r\n", "\n").replace("\r", "\n");
        String detected = detectIndentString(normalized);
        if (detected != null) indentUnit = detected;

        State state = new State();
        StringBuilder out = new StringBuilder();
        Lexer lx = new Lexer(normalized);
        Token prev = null, t;
        while ((t = lx.next()) != null) {
            switch (t.type) {
                case WHITESPACE:
                case NEWLINE:
                    break;
                case LINE_COMMENT: {
                    if (state.atLineStart) indent(out, state.indentLevel); else if (!endsWithSpace(out)) out.append(' ');
                    out.append(t.text).append(newline);
                    state.atLineStart = true;
                    break;
                }
                case BLOCK_COMMENT: {
                    if (state.atLineStart) indent(out, state.indentLevel); else if (!endsWithSpace(out)) out.append(' ');
                    appendBlockComment(out, t.text, state);
                    break;
                }
                case OPEN_BRACE: {
                    if (!state.atLineStart && !endsWithSpaceOrNewline(out)) out.append(' ');
                    if (state.atLineStart) indent(out, state.indentLevel);
                    out.append('{').append(newline);
                    state.atLineStart = true;
                    state.indentLevel++;
                    break;
                }
                case CLOSE_BRACE: {
                    if (!state.atLineStart) out.append(newline);
                    state.indentLevel = Math.max(0, state.indentLevel - 1);
                    indent(out, state.indentLevel);
                    out.append('}').append(newline);
                    state.atLineStart = true;
                    break;
                }
                case SEMICOLON: {
                    trimTrailingSpace(out);
                    out.append(';').append(newline);
                    state.atLineStart = true;
                    break;
                }
                case COMMA: {
                    trimTrailingSpace(out); out.append(',').append(' ');
                    state.atLineStart = false; break;
                }
                case DOT: { out.append('.'); state.atLineStart = false; break; }
                case COLON: {
                    // Ensure no space before colon, exactly one space after
                    trimTrailingSpace(out);
                    out.append(':').append(' ');
                    state.atLineStart = false;
                    break;
                }
                case OPEN_PAREN: { out.append('('); state.atLineStart=false; break; }
                case CLOSE_PAREN: { trimTrailingSpace(out); out.append(')'); state.atLineStart=false; break; }
                case OPEN_BRACKET: { out.append('['); state.atLineStart=false; break; }
                case CLOSE_BRACKET: { out.append(']'); state.atLineStart=false; break; }
                case OPERATOR: {
                    String op = t.text;
                    boolean unary = isUnary(op, prev);
                    if (unary) out.append(op);
                    else { if (!endsWithSpaceOrNewline(out)) out.append(' '); out.append(op).append(' ');} 
                    state.atLineStart = false; break;
                }
                case IDENTIFIER:
                case KEYWORD:
                case NUMBER:
                case STRING:
                case CHAR: {
                    if (state.atLineStart) indent(out, state.indentLevel);
                    else if (needsSpace(prev, t)) out.append(' ');
                    out.append(t.text);
                    state.atLineStart = false;
                    break;
                }
                default: {
                    if (state.atLineStart) indent(out, state.indentLevel);
                    out.append(t.text);
                    state.atLineStart = false;
                }
            }
            if (t.type != TokenType.WHITESPACE && t.type != TokenType.NEWLINE) prev = t;
        }
        return out.toString().replaceAll("(\r?\n)+$", newline);
    }

    private void indent(StringBuilder out, int level) { for (int j = 0; j < level; j++) out.append(indentUnit); }
    private static boolean endsWithSpace(StringBuilder out) { int l=out.length(); return l>0 && out.charAt(l-1)==' '; }
    private static boolean endsWithSpaceOrNewline(StringBuilder out) { int l=out.length(); if (l==0) return false; char c=out.charAt(l-1); return c==' '||c=='\n'||c=='\r'; }
    private static void trimTrailingSpace(StringBuilder out) { int l=out.length(); if (l>0 && out.charAt(l-1)==' ') out.deleteCharAt(l-1); }
    private static boolean needsSpace(Token prev, Token curr) {
        if (prev == null) return false;
        if (prev.type == TokenType.OPEN_PAREN || curr.type == TokenType.CLOSE_PAREN) return false;
        if (prev.type == TokenType.OPEN_BRACKET || curr.type == TokenType.CLOSE_BRACKET) return false;
        if (prev.type == TokenType.DOT || curr.type == TokenType.DOT) return false;
        if (prev.type == TokenType.COLON || curr.type == TokenType.COLON) return false;
        boolean pw = isWord(prev.type), cw = isWord(curr.type); if (pw && cw) return true;
        if (prev.type == TokenType.KEYWORD && curr.type == TokenType.IDENTIFIER) return true;
        return false;
    }
    private static boolean isWord(TokenType t) { return t==TokenType.IDENTIFIER||t==TokenType.KEYWORD||t==TokenType.NUMBER||t==TokenType.STRING||t==TokenType.CHAR; }
    private static boolean isUnary(String op, Token prev) {
        if ("!".equals(op) || "~".equals(op) || "++".equals(op) || "--".equals(op)) return true;
        if ("+".equals(op) || "-".equals(op)) { if (prev==null) return true; switch (prev.type) {
            case OPEN_PAREN: case OPEN_BRACKET: case OPEN_BRACE: case COMMA: case SEMICOLON: case OPERATOR: case COLON: case KEYWORD: return true; default: return false; } }
        return false;
    }
    private void appendBlockComment(StringBuilder out, String comment, State state) {
        String[] parts = comment.split("\n", -1);
        for (int i=0;i<parts.length;i++) { if (i>0) { out.append(newline); indent(out, state.indentLevel);} out.append(parts[i]); }
        out.append(newline); state.atLineStart = true;
    }

    private static final class State { int indentLevel=0; boolean atLineStart=true; }

    private enum TokenType { WHITESPACE, NEWLINE, IDENTIFIER, KEYWORD, NUMBER, STRING, CHAR, LINE_COMMENT, BLOCK_COMMENT, OPEN_BRACE, CLOSE_BRACE, OPEN_PAREN, CLOSE_PAREN, OPEN_BRACKET, CLOSE_BRACKET, SEMICOLON, COMMA, DOT, COLON, OPERATOR, OTHER }
    private static final class Token { final TokenType type; final String text; Token(TokenType t,String x){type=t;text=x;} }

    private static final class Lexer {
        private final String s; private final int n; private int i=0; Lexer(String s){this.s=s; this.n=s.length();}
        Token next(){ if (i>=n) return null; char c=s.charAt(i);
            if (c==' '||c=='\t'){int st=i; while(i<n&&(s.charAt(i)==' '||s.charAt(i)=='\t')) i++; return new Token(TokenType.WHITESPACE,s.substring(st,i));}
            if (c=='\n'||c=='\r'){int st=i; if (c=='\r'&&i+1<n&&s.charAt(i+1)=='\n') i+=2; else i++; return new Token(TokenType.NEWLINE,s.substring(st,i));}
            if (c=='/'&&i+1<n){char d=s.charAt(i+1); if(d=='/'){int st=i; i+=2; while(i<n&&s.charAt(i)!='\n'&&s.charAt(i)!='\r') i++; return new Token(TokenType.LINE_COMMENT,s.substring(st,i));} else if(d=='*'){int st=i; i+=2; while(i+1<n&&!(s.charAt(i)=='*'&&s.charAt(i+1)=='/')) i++; if(i+1<n) i+=2; return new Token(TokenType.BLOCK_COMMENT,s.substring(st,i));}}
            if (c=='"'){int st=i++; while(i<n){char ch=s.charAt(i++); if (ch=='\\'&&i<n){i++; continue;} if(ch=='"') break;} return new Token(TokenType.STRING,s.substring(st,i));}
            if (c=='\''){int st=i++; while(i<n){char ch=s.charAt(i++); if (ch=='\\'&&i<n){i++; continue;} if(ch=='\'') break;} return new Token(TokenType.CHAR,s.substring(st,i));}
            if (isIdentStart(c)){int st=i++; while(i<n&&isIdentPart(s.charAt(i))) i++; String tx=s.substring(st,i); if (isKeyword(tx)) return new Token(TokenType.KEYWORD,tx); return new Token(TokenType.IDENTIFIER,tx);} 
            if (isDigit(c)){int st=i++; while(i<n&&(isDigit(s.charAt(i))||isIdentPart(s.charAt(i)))) i++; return new Token(TokenType.NUMBER,s.substring(st,i));}
            switch(c){ case '{': i++; return new Token(TokenType.OPEN_BRACE,"{"); case '}': i++; return new Token(TokenType.CLOSE_BRACE,"}"); case '(': i++; return new Token(TokenType.OPEN_PAREN,"("); case ')': i++; return new Token(TokenType.CLOSE_PAREN,")"); case '[': i++; return new Token(TokenType.OPEN_BRACKET,"["); case ']': i++; return new Token(TokenType.CLOSE_BRACKET,"]"); case ';': i++; return new Token(TokenType.SEMICOLON,";"); case ',': i++; return new Token(TokenType.COMMA,","); case '.': i++; return new Token(TokenType.DOT,"."); case ':': i++; return new Token(TokenType.COLON,":"); }
            if (i+1<n){String two=s.substring(i,i+2); if (isOperator(two)){i+=2; return new Token(TokenType.OPERATOR,two);} }
            if (isOperator(String.valueOf(c))){i++; return new Token(TokenType.OPERATOR,String.valueOf(c));}
            i++; return new Token(TokenType.OTHER,String.valueOf(c));
        }
        private boolean isIdentStart(char c){ return Character.isLetter(c) || c=='_' || c=='$'; }
        private boolean isIdentPart(char c){ return Character.isLetterOrDigit(c) || c=='_' || c=='$'; }
        private boolean isDigit(char c){ return c>='0' && c<='9'; }
        private boolean isKeyword(String s){ return s.equals("property")||s.equals("define")||s.equals("Assertion")||s.equals("LTL")||s.equals("CTL")||s.equals("AG")||s.equals("EF")||s.equals("G")||s.equals("F")||s.equals("X")||s.equals("U"); }
        private boolean isOperator(String s){ return s.equals("==")||s.equals("!=")||s.equals("<=")||s.equals(">=")||s.equals("&&")||s.equals("||")||s.equals("->")||s.equals("<->")|| s.equals("+")||s.equals("-")||s.equals("*")||s.equals("/")||s.equals("%")|| s.equals("<")||s.equals(">")||s.equals("=")||s.equals("!")||s.equals("^"); }
    }

    private static String detectIndentString(String content) {
        String[] lines = content.split("\n");
        boolean hasTab = false; int gcd = 0;
        for (String line : lines) {
            if (line.isEmpty()) continue;
            int idx = 0; boolean lineHasTab = false;
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
        StringBuilder sb = new StringBuilder(); for (int i=0;i<gcd;i++) sb.append(' '); return sb.toString();
    }
    private static int gcd(int a,int b){ while(b!=0){ int t=a%b; a=b; b=t;} return Math.abs(a); }
}


