# AST-Based Formatter Implementation Guide

## Overview

This guide provides a complete implementation strategy for AST-based formatting of Rebeca (.rebeca) and Property (.property) files in the Eclipse plugin. The formatter uses the existing Rebeca compiler infrastructure to parse files into AST, preserves comments, and produces properly formatted output.

## Architecture

### 1. Core Components

#### **ASTBasedRebecaFormatter**

- Primary formatter for `.rebeca` files
- Uses `RebecaModelCompiler` for AST parsing
- Implements comment preservation system
- Provides fallback formatting when AST parsing fails

#### **ASTBasedPropertyFormatter**

- Primary formatter for `.property` files
- Uses `PropertyCompiler` for AST parsing
- Handles property-specific constructs (define, Assertion, LTL)
- Maintains proper section formatting

#### **ASTFormatterFactory**

- Factory pattern for creating appropriate formatters
- Handles file type detection and formatter instantiation
- Provides fallback formatter selection

#### **UnifiedFormattingStrategy**

- Eclipse integration layer
- Implements `IFormattingStrategy` interface
- Coordinates between Eclipse and AST formatters

### 2. Comment Preservation System

The most challenging aspect of AST-based formatting is preserving comments, which are typically stripped during parsing.

```java
public static class Comment {
    int line;           // Source line number
    int column;         // Column position
    String content;     // Comment text
    CommentType type;   // SINGLE_LINE, MULTI_LINE, JAVADOC
}
```

**Comment Extraction Process:**

1. Extract all comments with their positions before AST parsing
2. Remove comments from source for clean parsing
3. Parse AST structure
4. Reinsert comments at appropriate positions during formatting

### 3. Formatting Context

```java
public static class FormattingContext {
    private int indentLevel = 0;
    private StringBuilder output = new StringBuilder();
    private List<Comment> comments = new ArrayList<>();
    private int currentLine = 1;

    // Methods for managing indentation and output
    void increaseIndent();
    void decreaseIndent();
    void appendLine(String text);
    void appendCommentsBefore(int line);
}
```

## Implementation Steps

### Step 1: Comment Extraction

```java
public List<Comment> extractComments(String content) {
    List<Comment> comments = new ArrayList<>();

    // Single-line comments: //
    Pattern singleLinePattern = Pattern.compile("//.*$", Pattern.MULTILINE);

    // Multi-line comments: /* */ and /** */
    Pattern multiLinePattern = Pattern.compile("/\\*\\*?.*?\\*/", Pattern.DOTALL);

    // Extract and store with position information
    // ... implementation details

    return comments;
}
```

### Step 2: AST Parsing

```java
private RebecaModel parseAST(String content) {
    try {
        // Create temporary file
        File tempFile = File.createTempFile("rebeca_format_", ".rebeca");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            writer.write(content);
        }

        // Use existing compiler infrastructure
        Set<CompilerExtension> extensions = getCompilerExtensions();
        CoreVersion version = getProjectVersion();

        Pair<RebecaModel, SymbolTable> result =
            modelCompiler.compileRebecaFile(tempFile, extensions, version);

        return result.getFirst();
    } catch (Exception e) {
        // Return null to trigger fallback formatting
        return null;
    }
}
```

### Step 3: AST-Based Formatting

#### Reactive Class Formatting

```java
private void formatReactiveClass(ReactiveClassDeclaration classDecl, FormattingContext ctx) {
    // Insert comments before class declaration
    ctx.appendCommentsBefore(classDecl.getLineNumber());

    // Format class header
    StringBuilder classLine = new StringBuilder();
    classLine.append("reactiveclass ").append(classDecl.getName());
    if (classDecl.getQueueLength() != null) {
        classLine.append("(").append(classDecl.getQueueLength()).append(")");
    }
    classLine.append(" {");
    ctx.appendLine(classLine.toString());
    ctx.increaseIndent();

    // Format sections in order: knownrebecs, statevars, constructor, msgsrvs
    formatKnownRebecs(classDecl.getKnownRebecs(), ctx);
    formatStatevars(classDecl.getStatevars(), ctx);
    formatConstructors(classDecl.getConstructors(), ctx);
    formatMsgsrvs(classDecl.getMsgsrvs(), ctx);

    ctx.decreaseIndent();
    ctx.appendLine("}");
}
```

#### Method Formatting

```java
private void formatMethod(MethodDeclaration method, FormattingContext ctx, boolean isConstructor) {
    StringBuilder methodLine = new StringBuilder();

    if (isConstructor) {
        methodLine.append(method.getName());
    } else {
        methodLine.append("msgsrv ").append(method.getName());
    }

    // Format parameters
    methodLine.append("(");
    if (method.getFormalParameters() != null) {
        for (int i = 0; i < method.getFormalParameters().size(); i++) {
            if (i > 0) methodLine.append(", ");
            FieldDeclaration param = method.getFormalParameters().get(i);
            methodLine.append(param.getType().getTypeName()).append(" ");
            methodLine.append(param.getVariableDeclarators().get(0).getVariableName());
        }
    }
    methodLine.append(") {");

    ctx.appendLine(methodLine.toString());
    ctx.increaseIndent();

    // Format method body (requires statement-level AST traversal)
    formatMethodBody(method.getBlock(), ctx);

    ctx.decreaseIndent();
    ctx.appendLine("}");
}
```

### Step 4: Statement-Level Formatting

For complete formatting, implement AST visitors for different statement types:

```java
private void formatStatement(Statement stmt, FormattingContext ctx) {
    if (stmt instanceof IfStatement) {
        formatIfStatement((IfStatement) stmt, ctx);
    } else if (stmt instanceof ForStatement) {
        formatForStatement((ForStatement) stmt, ctx);
    } else if (stmt instanceof AssignmentStatement) {
        formatAssignmentStatement((AssignmentStatement) stmt, ctx);
    } else if (stmt instanceof MethodCallStatement) {
        formatMethodCallStatement((MethodCallStatement) stmt, ctx);
    }
    // ... handle other statement types
}

private void formatIfStatement(IfStatement ifStmt, FormattingContext ctx) {
    StringBuilder line = new StringBuilder();
    line.append("if (");
    line.append(formatExpression(ifStmt.getCondition()));
    line.append(") {");
    ctx.appendLine(line.toString());

    ctx.increaseIndent();
    formatStatement(ifStmt.getThenStatement(), ctx);
    ctx.decreaseIndent();

    if (ifStmt.getElseStatement() != null) {
        ctx.appendLine("} else {");
        ctx.increaseIndent();
        formatStatement(ifStmt.getElseStatement(), ctx);
        ctx.decreaseIndent();
    }
    ctx.appendLine("}");
}
```

### Step 5: Expression Formatting

```java
private String formatExpression(Expression expr) {
    if (expr instanceof BinaryExpression) {
        BinaryExpression binExpr = (BinaryExpression) expr;
        return formatExpression(binExpr.getLeft()) + " " +
               binExpr.getOperator() + " " +
               formatExpression(binExpr.getRight());
    } else if (expr instanceof MethodCallExpression) {
        MethodCallExpression callExpr = (MethodCallExpression) expr;
        StringBuilder sb = new StringBuilder();
        if (callExpr.getReceiver() != null) {
            sb.append(formatExpression(callExpr.getReceiver())).append(".");
        }
        sb.append(callExpr.getMethodName()).append("(");
        // Format arguments...
        sb.append(")");
        return sb.toString();
    }
    // Handle other expression types...
    return expr.toString();
}
```

## Property File Formatting

### Property AST Structure

```java
private void formatPropertyModel(PropertyModel model, FormattingContext ctx) {
    ctx.appendLine("property {");
    ctx.increaseIndent();

    // Define section
    if (hasDefinitions(model)) {
        formatDefineSection(model.getPropertyDefinition(), ctx);
        ctx.appendEmptyLine();
    }

    // Assertion section
    if (hasAssertions(model)) {
        formatAssertionSection(model, ctx);
        ctx.appendEmptyLine();
    }

    // LTL section
    if (hasLTLProperties(model)) {
        formatLTLSection(model, ctx);
    }

    ctx.decreaseIndent();
    ctx.appendLine("}");
}
```

## Integration with Eclipse

### Plugin.xml Configuration

```xml
<extension point="org.eclipse.ui.editors.documentProviders">
    <provider
        class="org.rebecalang.afra.ideplugin.editors.RebecaDocumentProvider"
        extensions="rebeca"
        id="org.rebecalang.afra.ideplugin.editors.rebeca.RebecaDocumentProvider">
    </provider>
</extension>

<extension point="org.eclipse.ui.editors.formattingStrategies">
    <formattingStrategy
        class="org.rebecalang.afra.ideplugin.editors.formatter.UnifiedFormattingStrategy"
        contentType="org.rebecalang.afra.ideplugin.rebeca">
    </formattingStrategy>
</extension>
```

### Editor Integration

```java
public class RebecaSourceViewerConfiguration extends SourceViewerConfiguration {

    @Override
    public IFormattingStrategy getFormattingStrategy(ISourceViewer sourceViewer, String contentType) {
        return new UnifiedFormattingStrategy();
    }

    @Override
    public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
        return new String[] {
            IDocument.DEFAULT_CONTENT_TYPE,
            RebecaPartitionScanner.SINGLE_LINE_COMMENT,
            RebecaPartitionScanner.MULTI_LINE_COMMENT,
            RebecaPartitionScanner.STRING
        };
    }
}
```

## Testing Strategy

### Unit Tests

```java
@Test
public void testCommentPreservation() {
    String input = "// Important comment\nreactiveclass Test { }";
    String output = formatter.format(input);
    assertTrue(output.contains("// Important comment"));
}

@Test
public void testIndentation() {
    String input = "reactiveclass Test{statevars{int x;}}";
    String output = formatter.format(input);
    String[] lines = output.split("\n");
    assertEquals("\tstatevars {", lines[1]); // Proper indentation
}

@Test
public void testFallbackFormatting() {
    String malformedInput = "reactiveclass Test { invalid syntax }";
    String output = formatter.format(malformedInput);
    assertNotNull(output);
    // Should not crash, should produce reasonable output
}
```

### Integration Tests

```java
@Test
public void testCompleteFileFormatting() {
    String sampleFile = readFile("samples/DiningPhilosophers.rebeca");
    String formatted = formatter.format(sampleFile);

    // Verify structure preservation
    assertTrue(formatted.contains("reactiveclass Philosopher"));
    assertTrue(formatted.contains("knownrebecs {"));
    assertTrue(formatted.contains("statevars {"));

    // Verify proper indentation
    assertProperIndentation(formatted);
}
```

## Performance Considerations

1. **Caching**: Cache parsed AST for active documents
2. **Incremental Parsing**: Only reparse when necessary
3. **Background Processing**: Run expensive operations in background threads
4. **Memory Management**: Dispose of temporary files and AST objects

## Error Handling

1. **Graceful Degradation**: Always provide fallback formatting
2. **Error Reporting**: Log parsing errors for debugging
3. **User Feedback**: Show progress for long operations
4. **Recovery**: Preserve original content if formatting fails

## Configuration Options

Allow users to configure:

- Indentation style (tabs vs spaces)
- Line length limits
- Comment formatting preferences
- Brace placement style
- Spacing around operators

## Conclusion

This AST-based formatter provides a robust, extensible foundation for Rebeca and Property file formatting. The comment preservation system ensures no information is lost, while the AST-based approach guarantees syntactically correct output. The fallback system provides reliability even when AST parsing fails.

The implementation leverages existing Rebeca compiler infrastructure, making it consistent with the language's formal definition and ensuring compatibility with future language evolution.
