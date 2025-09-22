package org.rebecalang.afra.ideplugin.editors.formatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * Robust token-based formatter for Property (.property) files
 * Handles proper indentation, spacing, and formatting rules
 */
public class FixedPropertyFormatter implements IAfraFormatter {
    
    private static final String INDENT = "\t";
    private static final String NEW_LINE = System.getProperty("line.separator");
    
    // Property keywords
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
        "property", "define", "Assertion", "LTL", "Safety", "G", "F", "U", "X", "R"
    ));
    
    // Binary operators that need spaces around them
    private static final Set<String> BINARY_OPERATORS = new HashSet<>(Arrays.asList(
        "=", "==", "!=", "<", ">", "<=", ">=", "&&", "||", "+", "-", "*", "/", "%"
    ));
    
    @Override
    public String format(IDocument document) {
        try {
            String content = document.get();
            return formatContent(content);
        } catch (Exception e) {
            e.printStackTrace();
            return document.get();
        }
    }
    
    @Override
    public String format(IDocument document, IRegion region) {
        try {
            String content = document.get(region.getOffset(), region.getLength());
            return formatContent(content);
        } catch (BadLocationException e) {
            e.printStackTrace();
            return document.get();
        }
    }
    
    @Override
    public String getIndentString() {
        return INDENT;
    }
    
    private String formatContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }
        
        // Normalize line endings
        content = content.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
        
        // Tokenize and format
        List<Token> tokens = tokenize(content);
        return formatTokens(tokens);
    }
    
    private List<Token> tokenize(String content) {
        List<Token> tokens = new ArrayList<>();
        int pos = 0;
        int line = 1;
        
        while (pos < content.length()) {
            char ch = content.charAt(pos);
            
            // Skip whitespace (will be reformatted)
            if (Character.isWhitespace(ch)) {
                if (ch == '\n') {
                    tokens.add(new Token(TokenType.NEWLINE, "\n", line++, pos));
                }
                pos++;
                continue;
            }
            
            // Multi-line comment
            if (pos < content.length() - 1 && content.charAt(pos) == '/' && content.charAt(pos + 1) == '*') {
                int end = content.indexOf("*/", pos + 2);
                if (end == -1) end = content.length();
                else end += 2;
                
                String comment = content.substring(pos, end);
                tokens.add(new Token(TokenType.MULTILINE_COMMENT, comment, line, pos));
                
                // Count newlines in comment
                for (int i = pos; i < end; i++) {
                    if (content.charAt(i) == '\n') line++;
                }
                pos = end;
                continue;
            }
            
            // Single-line comment
            if (pos < content.length() - 1 && content.charAt(pos) == '/' && content.charAt(pos + 1) == '/') {
                int end = content.indexOf('\n', pos);
                if (end == -1) end = content.length();
                
                String comment = content.substring(pos, end);
                tokens.add(new Token(TokenType.SINGLE_LINE_COMMENT, comment, line, pos));
                pos = end;
                continue;
            }
            
            // String literals
            if (ch == '"') {
                int end = pos + 1;
                while (end < content.length() && content.charAt(end) != '"') {
                    if (content.charAt(end) == '\\' && end + 1 < content.length()) {
                        end += 2; // Skip escaped character
                    } else {
                        end++;
                    }
                }
                if (end < content.length()) end++; // Include closing quote
                
                String literal = content.substring(pos, end);
                tokens.add(new Token(TokenType.STRING_LITERAL, literal, line, pos));
                pos = end;
                continue;
            }
            
            // Numbers
            if (Character.isDigit(ch)) {
                int end = pos;
                while (end < content.length() && 
                       (Character.isDigit(content.charAt(end)) || content.charAt(end) == '.')) {
                    end++;
                }
                
                String number = content.substring(pos, end);
                tokens.add(new Token(TokenType.NUMBER, number, line, pos));
                pos = end;
                continue;
            }
            
            // Identifiers and keywords
            if (Character.isJavaIdentifierStart(ch)) {
                int end = pos;
                while (end < content.length() && Character.isJavaIdentifierPart(content.charAt(end))) {
                    end++;
                }
                
                String identifier = content.substring(pos, end);
                TokenType type = KEYWORDS.contains(identifier) ? TokenType.KEYWORD : TokenType.IDENTIFIER;
                tokens.add(new Token(type, identifier, line, pos));
                pos = end;
                continue;
            }
            
            // Two-character operators
            if (pos < content.length() - 1) {
                String twoChar = content.substring(pos, pos + 2);
                if (BINARY_OPERATORS.contains(twoChar)) {
                    tokens.add(new Token(TokenType.OPERATOR, twoChar, line, pos));
                    pos += 2;
                    continue;
                }
            }
            
            // Single-character tokens
            String singleChar = String.valueOf(ch);
            TokenType type = getTokenType(ch);
            tokens.add(new Token(type, singleChar, line, pos));
            pos++;
        }
        
        return tokens;
    }
    
    private TokenType getTokenType(char ch) {
        switch (ch) {
            case '{': case '}': return TokenType.BRACE;
            case '(': case ')': return TokenType.PARENTHESIS;
            case '[': case ']': return TokenType.BRACKET;
            case ';': return TokenType.SEMICOLON;
            case ',': return TokenType.COMMA;
            case '.': return TokenType.DOT;
            case ':': return TokenType.COLON;
            case '=': case '!': case '<': case '>': case '+': case '-': 
            case '*': case '/': case '%': case '&': case '|': 
                return TokenType.OPERATOR;
            default: return TokenType.OTHER;
        }
    }
    
    private String formatTokens(List<Token> tokens) {
        StringBuilder result = new StringBuilder();
        int indentLevel = 0;
        boolean needsIndent = true;
        boolean lastWasNewline = true;
        boolean inMultiLineExpression = false;
        
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            Token prev = i > 0 ? tokens.get(i - 1) : null;
            Token next = i < tokens.size() - 1 ? tokens.get(i + 1) : null;
            
            switch (token.type) {
                case NEWLINE:
                    result.append(NEW_LINE);
                    needsIndent = true;
                    lastWasNewline = true;
                    break;
                    
                case BRACE:
                    if (token.value.equals("{")) {
                        if (!lastWasNewline && prev != null && prev.type != TokenType.NEWLINE) {
                            result.append(" ");
                        }
                        result.append(token.value);
                        indentLevel++;
                        needsIndent = true;
                    } else { // "}"
                        if (needsIndent) {
                            indentLevel = Math.max(0, indentLevel - 1);
                            appendIndent(result, indentLevel);
                            needsIndent = false;
                        }
                        result.append(token.value);
                    }
                    lastWasNewline = false;
                    break;
                    
                case OPERATOR:
                    appendOperator(result, token, prev, next);
                    lastWasNewline = false;
                    needsIndent = false;
                    break;
                    
                case SEMICOLON:
                    result.append(token.value);
                    lastWasNewline = false;
                    needsIndent = false;
                    break;
                    
                case COMMA:
                    result.append(token.value);
                    if (next != null && next.type != TokenType.NEWLINE) {
                        result.append(" ");
                    }
                    lastWasNewline = false;
                    needsIndent = false;
                    break;
                    
                case PARENTHESIS:
                    if (token.value.equals("(")) {
                        // No space before opening parenthesis in function calls
                        result.append(token.value);
                    } else { // ")"
                        result.append(token.value);
                    }
                    lastWasNewline = false;
                    needsIndent = false;
                    break;
                    
                case SINGLE_LINE_COMMENT:
                case MULTILINE_COMMENT:
                    if (needsIndent) {
                        appendIndent(result, indentLevel);
                        needsIndent = false;
                    } else if (prev != null && prev.type != TokenType.NEWLINE && 
                              token.type == TokenType.SINGLE_LINE_COMMENT) {
                        result.append(" ");
                    }
                    result.append(token.value);
                    lastWasNewline = false;
                    break;
                    
                default:
                    if (needsIndent) {
                        // Special handling for continued expressions
                        if (inMultiLineExpression && isOperatorOrContinuation(token)) {
                            appendIndent(result, indentLevel + 1);
                        } else {
                            appendIndent(result, indentLevel);
                        }
                        needsIndent = false;
                    } else if (needsSpaceBefore(token, prev)) {
                        result.append(" ");
                    }
                    result.append(token.value);
                    lastWasNewline = false;
                    break;
            }
            
            // Track multi-line expressions
            if (token.type == TokenType.OPERATOR && token.value.equals("=")) {
                inMultiLineExpression = true;
            } else if (token.type == TokenType.SEMICOLON) {
                inMultiLineExpression = false;
            }
        }
        
        return result.toString().replaceAll("\n+$", "\n");
    }
    
    private void appendOperator(StringBuilder result, Token token, Token prev, Token next) {
        if (BINARY_OPERATORS.contains(token.value)) {
            // Add space before binary operator
            if (prev != null && prev.type != TokenType.NEWLINE && 
                !result.toString().endsWith(" ")) {
                result.append(" ");
            }
            result.append(token.value);
            // Add space after binary operator
            if (next != null && next.type != TokenType.NEWLINE) {
                result.append(" ");
            }
        } else {
            // Unary operators or other operators
            result.append(token.value);
        }
    }
    
    private void appendIndent(StringBuilder result, int level) {
        for (int i = 0; i < level; i++) {
            result.append(INDENT);
        }
    }
    
    private boolean needsSpaceBefore(Token token, Token prev) {
        if (prev == null || prev.type == TokenType.NEWLINE) {
            return false;
        }
        
        // No space after dots
        if (prev.type == TokenType.DOT) {
            return false;
        }
        
        // No space before dots, commas, semicolons
        if (token.type == TokenType.DOT || token.type == TokenType.COMMA || 
            token.type == TokenType.SEMICOLON) {
            return false;
        }
        
        // Space after keywords (except in specific cases)
        if (prev.type == TokenType.KEYWORD) {
            return !token.value.equals("(") && !token.value.equals(";");
        }
        
        // Space between identifiers
        return prev.type == TokenType.IDENTIFIER && token.type == TokenType.IDENTIFIER;
    }
    
    private boolean isOperatorOrContinuation(Token token) {
        return token.type == TokenType.OPERATOR || 
               (token.type == TokenType.KEYWORD && 
                ("&&".equals(token.value) || "||".equals(token.value)));
    }
    
    // Token class to represent parsed elements
    private static class Token {
        final TokenType type;
        final String value;
        @SuppressWarnings("unused")
        final int line;
        @SuppressWarnings("unused")
        final int position;
        
        Token(TokenType type, String value, int line, int position) {
            this.type = type;
            this.value = value;
            this.line = line;
            this.position = position;
        }
        
        @Override
        public String toString() {
            return type + ": " + value;
        }
    }
    
    // Token types
    private enum TokenType {
        KEYWORD, IDENTIFIER, NUMBER, STRING_LITERAL,
        OPERATOR, BRACE, PARENTHESIS, BRACKET,
        SEMICOLON, COMMA, DOT, COLON,
        SINGLE_LINE_COMMENT, MULTILINE_COMMENT,
        NEWLINE, OTHER
    }
}
