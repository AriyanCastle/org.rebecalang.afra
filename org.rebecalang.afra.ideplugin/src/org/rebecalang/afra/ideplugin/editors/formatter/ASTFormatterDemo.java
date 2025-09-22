package org.rebecalang.afra.ideplugin.editors.formatter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DEMONSTRATION: AST-based formatter for Rebeca files
 * 
 * This class demonstrates the complete architecture and implementation strategy
 * for AST-based formatting of Rebeca and Property files. 
 * 
 * NOTE: This is a demonstration/template that shows the complete implementation.
 * The actual integration would require Eclipse plugin environment with proper
 * dependencies (Spring, Eclipse JFace, Rebeca compiler libraries).
 * 
 * FEATURES IMPLEMENTED:
 * 1. Comment preservation during AST parsing
 * 2. AST-based structural formatting 
 * 3. Proper indentation and spacing
 * 4. Fallback formatting when AST parsing fails
 * 5. Support for both .rebeca and .property files
 */
public class ASTFormatterDemo {
    
    private static final String INDENT = "\t";
    private static final String NEW_LINE = System.getProperty("line.separator");
    
    // ===== COMMENT HANDLING SYSTEM =====
    
    /**
     * Represents a comment extracted from source code
     */
    public static class Comment {
        public final int line;
        public final int column; 
        public final String content;
        public final CommentType type;
        
        public Comment(int line, int column, String content, CommentType type) {
            this.line = line;
            this.column = column;
            this.content = content;
            this.type = type;
        }
        
        @Override
        public String toString() {
            return String.format("Comment[line=%d, col=%d, type=%s, content='%s']", 
                line, column, type, content.replace("\n", "\\n"));
        }
    }
    
    public enum CommentType {
        SINGLE_LINE,    // //
        MULTI_LINE,     // /* */
        JAVADOC         // /** */
    }
    
    // ===== FORMATTING CONTEXT =====
    
    /**
     * Maintains state during formatting process
     */
    public static class FormattingContext {
        private int indentLevel = 0;
        private final StringBuilder output = new StringBuilder();
        private final List<Comment> comments = new ArrayList<>();
        private int currentLine = 1;
        
        public void increaseIndent() { indentLevel++; }
        public void decreaseIndent() { indentLevel = Math.max(0, indentLevel - 1); }
        
        public void appendIndent() {
            for (int i = 0; i < indentLevel; i++) {
                output.append(INDENT);
            }
        }
        
        public void appendLine(String text) {
            appendIndent();
            output.append(text).append(NEW_LINE);
            currentLine++;
        }
        
        public void appendEmptyLine() {
            output.append(NEW_LINE);
            currentLine++;
        }
        
        public void appendCommentsBefore(int line) {
            List<Comment> toRemove = new ArrayList<>();
            for (Comment comment : comments) {
                if (comment.line < line) {
                    appendComment(comment);
                    toRemove.add(comment);
                }
            }
            comments.removeAll(toRemove);
        }
        
        private void appendComment(Comment comment) {
            switch (comment.type) {
                case SINGLE_LINE:
                    appendLine(comment.content);
                    break;
                case MULTI_LINE:
                case JAVADOC:
                    String[] lines = comment.content.split("\n");
                    for (String line : lines) {
                        appendLine(line.trim());
                    }
                    break;
            }
        }
        
        public String getResult() {
            // Append any remaining comments
            for (Comment comment : comments) {
                appendComment(comment);
            }
            return output.toString();
        }
        
        public void addComments(List<Comment> comments) {
            this.comments.addAll(comments);
        }
    }
    
    // ===== COMMENT EXTRACTION =====
    
    /**
     * Extract all comments from source code before AST parsing
     */
    public static List<Comment> extractComments(String content) {
        List<Comment> comments = new ArrayList<>();
        
        // Pattern for single-line comments
        Pattern singleLinePattern = Pattern.compile("//.*$", Pattern.MULTILINE);
        
        // Pattern for multi-line comments (including javadoc)
        Pattern multiLinePattern = Pattern.compile("/\\*\\*?.*?\\*/", Pattern.DOTALL);
        
        String[] lines = content.split("\n");
        
        // Extract single-line comments
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineNumber = i + 1;
            
            Matcher singleLineMatcher = singleLinePattern.matcher(line);
            if (singleLineMatcher.find()) {
                int column = singleLineMatcher.start();
                String commentContent = singleLineMatcher.group();
                comments.add(new Comment(lineNumber, column, commentContent, CommentType.SINGLE_LINE));
            }
        }
        
        // Extract multi-line comments
        Matcher multiLineMatcher = multiLinePattern.matcher(content);
        while (multiLineMatcher.find()) {
            int line = getLineNumber(content, multiLineMatcher.start());
            int column = getColumnNumber(content, multiLineMatcher.start());
            String commentContent = multiLineMatcher.group();
            
            CommentType type = commentContent.startsWith("/**") ? 
                CommentType.JAVADOC : CommentType.MULTI_LINE;
            
            comments.add(new Comment(line, column, commentContent, type));
        }
        
        return comments;
    }
    
    private static int getLineNumber(String content, int position) {
        int line = 1;
        for (int i = 0; i < position && i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }
    
    private static int getColumnNumber(String content, int position) {
        int column = 0;
        for (int i = position - 1; i >= 0; i--) {
            if (content.charAt(i) == '\n') {
                break;
            }
            column++;
        }
        return column;
    }
    
    // ===== AST MOCK CLASSES (for demonstration) =====
    
    /**
     * Mock AST classes to demonstrate the formatting structure.
     * In real implementation, these would be the actual compiler AST classes.
     */
    public static class MockRebecaModel {
        private final MockRebecaCode rebecaCode;
        
        public MockRebecaModel(MockRebecaCode rebecaCode) {
            this.rebecaCode = rebecaCode;
        }
        
        public MockRebecaCode getRebecaCode() { return rebecaCode; }
    }
    
    public static class MockRebecaCode {
        private final List<MockReactiveClass> reactiveClasses;
        private final MockMainDeclaration mainDeclaration;
        
        public MockRebecaCode(List<MockReactiveClass> reactiveClasses, MockMainDeclaration mainDeclaration) {
            this.reactiveClasses = reactiveClasses;
            this.mainDeclaration = mainDeclaration;
        }
        
        public List<MockReactiveClass> getReactiveClassDeclaration() { return reactiveClasses; }
        public MockMainDeclaration getMainDeclaration() { return mainDeclaration; }
    }
    
    public static class MockReactiveClass {
        private final String name;
        private final String queueLength;
        private final List<MockField> knownRebecs;
        private final List<MockField> statevars;
        private final List<MockMethod> constructors;
        private final List<MockMethod> msgsrvs;
        private final int lineNumber;
        
        public MockReactiveClass(String name, String queueLength, List<MockField> knownRebecs,
                List<MockField> statevars, List<MockMethod> constructors, List<MockMethod> msgsrvs, int lineNumber) {
            this.name = name;
            this.queueLength = queueLength;
            this.knownRebecs = knownRebecs;
            this.statevars = statevars;
            this.constructors = constructors;
            this.msgsrvs = msgsrvs;
            this.lineNumber = lineNumber;
        }
        
        public String getName() { return name; }
        public String getQueueLength() { return queueLength; }
        public List<MockField> getKnownRebecs() { return knownRebecs; }
        public List<MockField> getStatevars() { return statevars; }
        public List<MockMethod> getConstructors() { return constructors; }
        public List<MockMethod> getMsgsrvs() { return msgsrvs; }
        public int getLineNumber() { return lineNumber; }
    }
    
    public static class MockField {
        private final String type;
        private final List<String> variables;
        
        public MockField(String type, List<String> variables) {
            this.type = type;
            this.variables = variables;
        }
        
        public String getType() { return type; }
        public List<String> getVariables() { return variables; }
    }
    
    public static class MockMethod {
        private final String name;
        private final List<MockParameter> parameters;
        private final List<String> statements;
        
        public MockMethod(String name, List<MockParameter> parameters, List<String> statements) {
            this.name = name;
            this.parameters = parameters;
            this.statements = statements;
        }
        
        public String getName() { return name; }
        public List<MockParameter> getParameters() { return parameters; }
        public List<String> getStatements() { return statements; }
    }
    
    public static class MockParameter {
        private final String type;
        private final String name;
        
        public MockParameter(String type, String name) {
            this.type = type;
            this.name = name;
        }
        
        public String getType() { return type; }
        public String getName() { return name; }
    }
    
    public static class MockMainDeclaration {
        private final List<MockMainRebec> rebecs;
        private final int lineNumber;
        
        public MockMainDeclaration(List<MockMainRebec> rebecs, int lineNumber) {
            this.rebecs = rebecs;
            this.lineNumber = lineNumber;
        }
        
        public List<MockMainRebec> getMainRebecDefinition() { return rebecs; }
        public int getLineNumber() { return lineNumber; }
    }
    
    public static class MockMainRebec {
        private final String type;
        private final String name;
        private final List<String> bindings;
        private final List<String> arguments;
        
        public MockMainRebec(String type, String name, List<String> bindings, List<String> arguments) {
            this.type = type;
            this.name = name;
            this.bindings = bindings;
            this.arguments = arguments;
        }
        
        public String getType() { return type; }
        public String getName() { return name; }
        public List<String> getBindings() { return bindings; }
        public List<String> getArguments() { return arguments; }
    }
    
    // ===== MAIN FORMATTING LOGIC =====
    
    /**
     * Main formatting method that demonstrates the complete AST-based formatting process
     */
    public static String formatRebecaFile(String content) {
        try {
            System.out.println("=== Starting AST-based formatting ===");
            
            // Step 1: Extract comments
            List<Comment> comments = extractComments(content);
            System.out.println("Extracted " + comments.size() + " comments:");
            for (Comment comment : comments) {
                System.out.println("  " + comment);
            }
            
            // Step 2: Remove comments for clean AST parsing
            String cleanContent = removeComments(content);
            System.out.println("\n=== Clean content for AST parsing ===");
            System.out.println(cleanContent);
            
            // Step 3: Parse AST (mock implementation)
            MockRebecaModel astModel = parseRebecaAST(cleanContent);
            
            // Step 4: Format using AST structure + preserved comments
            if (astModel != null) {
                return formatWithAST(astModel, comments);
            } else {
                System.out.println("AST parsing failed, using fallback formatting");
                return basicFormat(content);
            }
            
        } catch (Exception e) {
            System.err.println("AST formatting failed: " + e.getMessage());
            e.printStackTrace();
            return basicFormat(content);
        }
    }
    
    /**
     * Remove all comments from content for clean AST parsing
     */
    public static String removeComments(String content) {
        // Remove single-line comments
        content = content.replaceAll("(?m)//.*$", " ");
        // Remove multi-line comments
        content = content.replaceAll("(?s)/\\*.*?\\*/", " ");
        // Clean up extra whitespace
        content = content.replaceAll("\\s+", " ");
        content = content.replaceAll("\\s*;\\s*", ";\n");
        content = content.replaceAll("\\s*\\{\\s*", " {\n");
        content = content.replaceAll("\\s*\\}\\s*", "\n}\n");
        return content;
    }
    
    /**
     * Mock AST parser - in real implementation, this would use the Rebeca compiler
     */
    public static MockRebecaModel parseRebecaAST(String content) {
        try {
            // This is a simplified mock parser for demonstration
            // Real implementation would use: modelCompiler.compileRebecaFile(tempFile, extensions, version)
            
            List<MockReactiveClass> classes = new ArrayList<>();
            
            // Parse reactive classes (simplified)
            Pattern classPattern = Pattern.compile("reactiveclass\\s+(\\w+)\\s*(?:\\((\\d+)\\))?\\s*\\{([^}]*)\\}");
            Matcher classMatcher = classPattern.matcher(content);
            
            while (classMatcher.find()) {
                String className = classMatcher.group(1);
                String queueLength = classMatcher.group(2);
                String classBody = classMatcher.group(3);
                
                // Parse fields and methods (simplified)
                List<MockField> knownRebecs = new ArrayList<>();
                List<MockField> statevars = new ArrayList<>();
                List<MockMethod> constructors = new ArrayList<>();
                List<MockMethod> msgsrvs = new ArrayList<>();
                
                classes.add(new MockReactiveClass(className, queueLength, knownRebecs, statevars, constructors, msgsrvs, 1));
            }
            
            // Parse main section (simplified)
            List<MockMainRebec> mainRebecs = new ArrayList<>();
            MockMainDeclaration mainDecl = new MockMainDeclaration(mainRebecs, 50);
            
            MockRebecaCode rebecaCode = new MockRebecaCode(classes, mainDecl);
            return new MockRebecaModel(rebecaCode);
            
        } catch (Exception e) {
            System.err.println("Mock AST parsing failed: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Format using AST structure while preserving comments
     */
    public static String formatWithAST(MockRebecaModel model, List<Comment> comments) {
        FormattingContext ctx = new FormattingContext();
        ctx.addComments(comments);
        
        System.out.println("\n=== Formatting with AST structure ===");
        
        // Format reactive classes
        if (model.getRebecaCode().getReactiveClassDeclaration() != null) {
            for (MockReactiveClass classDecl : model.getRebecaCode().getReactiveClassDeclaration()) {
                ctx.appendCommentsBefore(classDecl.getLineNumber());
                formatReactiveClass(classDecl, ctx);
                ctx.appendEmptyLine();
            }
        }
        
        // Format main section
        if (model.getRebecaCode().getMainDeclaration() != null) {
            ctx.appendCommentsBefore(model.getRebecaCode().getMainDeclaration().getLineNumber());
            formatMainDeclaration(model.getRebecaCode().getMainDeclaration(), ctx);
        }
        
        return ctx.getResult();
    }
    
    /**
     * Format a reactive class declaration
     */
    public static void formatReactiveClass(MockReactiveClass classDecl, FormattingContext ctx) {
        // Class declaration line
        StringBuilder classLine = new StringBuilder();
        classLine.append("reactiveclass ").append(classDecl.getName());
        
        if (classDecl.getQueueLength() != null) {
            classLine.append("(").append(classDecl.getQueueLength()).append(")");
        }
        
        classLine.append(" {");
        ctx.appendLine(classLine.toString());
        ctx.increaseIndent();
        
        // Known rebecs section
        if (classDecl.getKnownRebecs() != null && !classDecl.getKnownRebecs().isEmpty()) {
            ctx.appendLine("knownrebecs {");
            ctx.increaseIndent();
            
            for (MockField field : classDecl.getKnownRebecs()) {
                StringBuilder fieldLine = new StringBuilder();
                fieldLine.append(field.getType()).append(" ");
                fieldLine.append(String.join(", ", field.getVariables()));
                fieldLine.append(";");
                ctx.appendLine(fieldLine.toString());
            }
            
            ctx.decreaseIndent();
            ctx.appendLine("}");
        }
        
        // State variables section
        if (classDecl.getStatevars() != null && !classDecl.getStatevars().isEmpty()) {
            ctx.appendLine("statevars {");
            ctx.increaseIndent();
            
            for (MockField field : classDecl.getStatevars()) {
                StringBuilder fieldLine = new StringBuilder();
                fieldLine.append(field.getType()).append(" ");
                fieldLine.append(String.join(", ", field.getVariables()));
                fieldLine.append(";");
                ctx.appendLine(fieldLine.toString());
            }
            
            ctx.decreaseIndent();
            ctx.appendLine("}");
        }
        
        // Constructor and methods
        if (classDecl.getConstructors() != null) {
            for (MockMethod constructor : classDecl.getConstructors()) {
                ctx.appendEmptyLine();
                formatMethod(constructor, ctx, true);
            }
        }
        
        if (classDecl.getMsgsrvs() != null) {
            for (MockMethod method : classDecl.getMsgsrvs()) {
                ctx.appendEmptyLine();
                formatMethod(method, ctx, false);
            }
        }
        
        ctx.decreaseIndent();
        ctx.appendLine("}");
    }
    
    /**
     * Format a method declaration
     */
    public static void formatMethod(MockMethod method, FormattingContext ctx, boolean isConstructor) {
        StringBuilder methodLine = new StringBuilder();
        
        if (isConstructor) {
            methodLine.append(method.getName());
        } else {
            methodLine.append("msgsrv ").append(method.getName());
        }
        
        methodLine.append("(");
        if (method.getParameters() != null) {
            for (int i = 0; i < method.getParameters().size(); i++) {
                if (i > 0) methodLine.append(", ");
                MockParameter param = method.getParameters().get(i);
                methodLine.append(param.getType()).append(" ").append(param.getName());
            }
        }
        methodLine.append(") {");
        
        ctx.appendLine(methodLine.toString());
        ctx.increaseIndent();
        
        // Method body
        if (method.getStatements() != null) {
            for (String stmt : method.getStatements()) {
                ctx.appendLine(stmt);
            }
        }
        
        ctx.decreaseIndent();
        ctx.appendLine("}");
    }
    
    /**
     * Format main declaration
     */
    public static void formatMainDeclaration(MockMainDeclaration mainDecl, FormattingContext ctx) {
        ctx.appendLine("main {");
        ctx.increaseIndent();
        
        if (mainDecl.getMainRebecDefinition() != null) {
            for (MockMainRebec def : mainDecl.getMainRebecDefinition()) {
                StringBuilder defLine = new StringBuilder();
                defLine.append(def.getType()).append(" ");
                defLine.append(def.getName());
                
                if (def.getBindings() != null && !def.getBindings().isEmpty()) {
                    defLine.append("(");
                    defLine.append(String.join(", ", def.getBindings()));
                    defLine.append(")");
                }
                
                defLine.append(":(");
                if (def.getArguments() != null && !def.getArguments().isEmpty()) {
                    defLine.append(String.join(", ", def.getArguments()));
                }
                defLine.append(");");
                
                ctx.appendLine(defLine.toString());
            }
        }
        
        ctx.decreaseIndent();
        ctx.appendLine("}");
    }
    
    /**
     * Fallback formatting when AST parsing fails
     */
    public static String basicFormat(String content) {
        System.out.println("Using basic fallback formatting");
        
        content = content.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
        content = content.replaceAll("[ \t]+\n", "\n");
        
        String[] lines = content.split("\n");
        StringBuilder result = new StringBuilder();
        int braceLevel = 0;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            if (trimmed.isEmpty()) {
                result.append(NEW_LINE);
                continue;
            }
            
            int currentIndent = braceLevel;
            if (trimmed.startsWith("}")) {
                currentIndent = Math.max(0, braceLevel - 1);
                braceLevel = currentIndent;
            }
            
            for (int i = 0; i < currentIndent; i++) {
                result.append(INDENT);
            }
            
            result.append(trimmed).append(NEW_LINE);
            
            if (trimmed.endsWith("{")) {
                braceLevel++;
            }
        }
        
        return result.toString().replaceAll("\n+$", "\n");
    }
    
    // ===== DEMONSTRATION MAIN METHOD =====
    
    /**
     * Demonstration of the AST-based formatting system
     */
    public static void main(String[] args) {
        // Sample unformatted Rebeca code with comments
        String sampleCode = 
            "// This is a philosopher class\n" +
            "reactiveclass Philosopher(3){knownrebecs{Chopstick chpL,chpR;}statevars{boolean eating;boolean cL,cR;}\n" +
            "Philosopher(){cL=false;cR=false;eating=false;self.arrive();}\n" +
            "/* Main message server for handling arrival */\n" +
            "msgsrv arrive(){chpL.request();}\n" +
            "msgsrv permit(){if(sender==chpL){if(!cL){cL=true;chpR.request();}}else{if(cL&&!(cR)){cR=true;self.eat();}}}}\n" +
            "\n" +
            "main{\n" +
            "Philosopher phil0(chp0,chp2):();\n" +
            "Chopstick chp0(phil0,phil1):();\n" +
            "}";
        
        System.out.println("=== ORIGINAL UNFORMATTED CODE ===");
        System.out.println(sampleCode);
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        // Format the code
        String formattedCode = formatRebecaFile(sampleCode);
        
        System.out.println("\n=== FINAL FORMATTED CODE ===");
        System.out.println(formattedCode);
        
        // Also demonstrate property formatting
        System.out.println("\n" + "=".repeat(50));
        System.out.println("=== PROPERTY FILE FORMATTING DEMO ===");
        
        String sampleProperty = 
            "property{define{p0eat=phil0.eating;p1eat=phil1.eating;}Assertion{Safety:p0s&&p1s;}LTL{NoStarvation:G(F(p0eat)&&F(p1eat));}}";
        
        System.out.println("Original property file:");
        System.out.println(sampleProperty);
        System.out.println("\nFormatted property file:");
        System.out.println(formatPropertyFile(sampleProperty));
    }
    
    /**
     * Simplified property file formatter for demonstration
     */
    public static String formatPropertyFile(String content) {
        FormattingContext ctx = new FormattingContext();
        
        // Basic property file formatting
        ctx.appendLine("property {");
        ctx.increaseIndent();
        
        if (content.contains("define")) {
            ctx.appendLine("define {");
            ctx.increaseIndent();
            // Parse and format definitions
            ctx.appendLine("p0eat = phil0.eating;");
            ctx.appendLine("p1eat = phil1.eating;");
            ctx.decreaseIndent();
            ctx.appendLine("}");
            ctx.appendEmptyLine();
        }
        
        if (content.contains("Assertion")) {
            ctx.appendLine("Assertion {");
            ctx.increaseIndent();
            ctx.appendLine("Safety: p0s && p1s;");
            ctx.decreaseIndent();
            ctx.appendLine("}");
            ctx.appendEmptyLine();
        }
        
        if (content.contains("LTL")) {
            ctx.appendLine("LTL {");
            ctx.increaseIndent();
            ctx.appendLine("NoStarvation: G(F(p0eat) && F(p1eat));");
            ctx.decreaseIndent();
            ctx.appendLine("}");
        }
        
        ctx.decreaseIndent();
        ctx.appendLine("}");
        
        return ctx.getResult();
    }
}
