# AST-Based Formatter for Rebeca Language

This document describes the new AST-based formatter implementation for both `.rebeca` and `.property` files in the Rebeca IDE plugin.

## Overview

The AST-based formatter provides comprehensive code formatting using the Abstract Syntax Tree (AST) from the Rebeca compiler. This ensures accurate, structure-aware formatting that preserves semantic meaning while improving code readability.

## Key Features

### ✅ **Complete AST Integration**

- Uses `RebecaModelCompiler` from the official Rebeca compiler
- Leverages the full object model for accurate parsing
- Handles all Rebeca language constructs properly

### ✅ **Comment Preservation**

- Preserves all comment types: `//`, `/* */`, `/** */`
- Maintains comment positioning relative to code structures
- Proper indentation of multi-line comments

### ✅ **Comprehensive Formatting**

- **Indentation**: Consistent tab-based indentation
- **Spacing**: Normalized spacing around operators, parentheses, braces
- **Line breaks**: Strategic blank lines between sections
- **Alignment**: Proper alignment of declarations and statements

### ✅ **Dual File Support**

- **Rebeca files** (`.rebeca`): Full AST-based formatting with visitor pattern
- **Property files** (`.property`): Structure-aware formatting with proper sectioning

### ✅ **Fallback Mechanism**

- Graceful degradation to simple formatting if AST parsing fails
- Robust error handling ensures formatting never breaks the IDE

## Architecture

### Core Components

1. **`ASTBasedFormatter`** - Main formatter implementing `IAfraFormatter`
2. **`RebecaASTVisitor`** - Visitor pattern implementation for AST traversal
3. **`PropertyASTFormatter`** - Specialized formatter for property files
4. **`FormatterRegistry`** - Central registry for accessing formatters
5. **Enhanced Strategies** - Drop-in replacements for existing formatting strategies

### Class Hierarchy

```
IAfraFormatter
├── ASTBasedFormatter (main implementation)
├── FixedRebecaFormatter (legacy fallback)
└── FixedPropertyFormatter (legacy fallback)

IFormattingStrategy
├── EnhancedRebecaFormattingStrategy
└── EnhancedPropertyFormattingStrategy
```

## Usage

### Basic Usage

```java
// Get formatter from registry
FormatterRegistry registry = FormatterRegistry.getInstance();
IAfraFormatter formatter = registry.getRebecaFormatter();

// Format a document
IDocument document = ...; // Your Eclipse document
String formatted = formatter.format(document);

// Format a region
IRegion region = ...; // Specific region to format
String formattedRegion = formatter.format(document, region);
```

### Integration with Existing IDE

```java
// Replace existing formatting strategies
EnhancedRebecaFormattingStrategy rebecaStrategy = new EnhancedRebecaFormattingStrategy();
EnhancedPropertyFormattingStrategy propertyStrategy = new EnhancedPropertyFormattingStrategy();

// Use in source viewer configuration
// (integrate with existing RebecaSourceViewerConfiguration)
```

### Direct API Usage

```java
// Create formatter directly
ASTBasedFormatter formatter = new ASTBasedFormatter();

// Format content string
String code = "reactiveclass Test{...}";
Document doc = new Document(code);
String formatted = formatter.format(doc);
```

## Formatting Rules

### Rebeca Files

#### Class Structure

```rebeca
reactiveclass ClassName(queueSize) {
	knownrebecs {
		Type instance1, instance2;
	}

	statevars {
		int variable1;
		boolean flag;
	}

	ClassName(parameters) {
		// Constructor body
	}

	msgsrv methodName() {
		// Method body
	}
}

main {
	ClassName obj(bindings):(args);
}
```

#### Indentation Rules

- **Tab-based indentation** (configurable via `getIndentString()`)
- **Class body**: +1 indent level
- **Section blocks** (`knownrebecs`, `statevars`): +1 additional level
- **Method bodies**: +1 additional level
- **Control structures**: +1 level per nesting

#### Spacing Rules

- **Operators**: `x = y + z` (spaces around operators)
- **Method calls**: `object.method(arg1, arg2)` (no space before parentheses)
- **Control structures**: `if (condition)` (space before parentheses)
- **Declarations**: `Type variable1, variable2;` (space after commas)

### Property Files

#### Structure

```property
property {
	define {
		var1 = expression1;
		var2 = expression2;
	}

	Assertion {
		Safety: condition;
	}

	LTL {
		Property1: G(F(condition));
		Property2: condition1 && condition2;
	}
}
```

#### Formatting Rules

- **Consistent indentation** within blocks
- **Blank lines** between major sections
- **Normalized spacing** around operators and colons
- **Proper semicolon placement**

## Comment Handling

### Comment Types Supported

1. **Single-line comments**: `// comment text`
2. **Multi-line comments**: `/* comment text */`
3. **Documentation comments**: `/** documentation */`

### Comment Formatting

```rebeca
reactiveclass Example {
	/**
	 * Class documentation
	 * Multiple lines supported
	 */
	knownrebecs {
		Node neighbor; // Inline comment
	}

	msgsrv process() {
		/* Block comment
		   with proper indentation */
		self.continue();
	}
}
```

## Testing

### Test Files

Run the formatter test with sample files:

```java
ASTFormatterTest test = new ASTFormatterTest();
test.testRebecaFormatting();
test.testPropertyFormatting();
test.testCommentHandling();
```

### Sample Files Used

- `samples/DiningPhilosophers.rebeca`
- `samples/DiningPhilosophers.property`
- `samples/LeaderElection.rebeca`
- `samples/ToxicGasLevel.rebeca`
- `samples/DiningPhilosophers-hint-comment.rebeca`

## Configuration

### Indentation

Default: Tab characters (`\t`)
Configurable via `getIndentString()` method

### Line Endings

Uses system default line separator
Normalizes mixed line endings in input

### Fallback Behavior

- AST parsing failure → Simple structure-based formatting
- Compiler unavailable → Legacy formatter
- All errors → Return original content unchanged

## Integration Points

### Eclipse Integration

- Implements `IAfraFormatter` interface
- Compatible with `IFormattingStrategy`
- Works with Eclipse document model

### Rebeca Compiler Integration

- Uses Spring DI for compiler components
- Leverages `RebecaModelCompiler` from https://github.com/rebeca-lang/org.rebecalang.compiler
- Supports all Rebeca extensions (Core, Timed, Probabilistic)

## Performance Considerations

- **Caching**: AST parsing results could be cached for large files
- **Incremental**: Currently formats entire documents; region-specific formatting available
- **Memory**: Temporary files cleaned up automatically
- **Fallback**: Fast fallback ensures UI responsiveness

## Error Handling

- **Graceful degradation**: Never breaks IDE functionality
- **Logging**: Comprehensive error logging for debugging
- **Recovery**: Multiple fallback levels ensure formatting always succeeds

## Future Enhancements

1. **Incremental formatting**: Format only changed regions
2. **Custom formatting rules**: User-configurable formatting preferences
3. **Real-time formatting**: Integration with real-time syntax checker
4. **Performance optimization**: Caching and incremental parsing
5. **Advanced comment handling**: Better comment-to-AST association

## Troubleshooting

### Common Issues

1. **Compiler not initialized**

   - Check Spring context configuration
   - Verify Rebeca compiler dependencies

2. **AST parsing fails**

   - Formatter falls back to simple formatting
   - Check syntax errors in source file

3. **Comments missing**
   - Check comment extraction regex patterns
   - Verify comment positioning logic

### Debug Mode

Enable debug logging to see formatter decisions:

```java
System.setProperty("rebeca.formatter.debug", "true");
```

## API Reference

### Main Classes

- `ASTBasedFormatter`: Main formatter implementation
- `RebecaASTVisitor`: AST traversal and formatting
- `PropertyASTFormatter`: Property file formatting
- `FormatterRegistry`: Formatter access point
- `ASTFormatterTest`: Testing and validation

### Key Methods

- `format(IDocument)`: Format entire document
- `format(IDocument, IRegion)`: Format specific region
- `getIndentString()`: Get indentation string
- `isPropertyFile(String)`: Detect file type
- `extractComments(String, Context)`: Extract and preserve comments

This AST-based formatter provides a solid foundation for high-quality code formatting in the Rebeca IDE, with comprehensive support for the language's features and robust error handling.
