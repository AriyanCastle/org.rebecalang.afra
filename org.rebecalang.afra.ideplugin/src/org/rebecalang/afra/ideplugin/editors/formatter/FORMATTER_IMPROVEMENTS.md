# Rebeca Formatter Improvements

## Overview

The Rebeca language formatter has been completely rewritten with a robust token-based approach to address all formatting issues and provide professional-quality code formatting.

## Previous Issues

- **Basic string-based formatting**: Used simple `startsWith("}")` and `endsWith("{")` checks which are unreliable
- **No operator spacing**: Missing spaces around operators like `==`, `&&`, `||`, `=`
- **Incorrect indentation**: Basic brace counting without proper context awareness
- **Poor method call formatting**: No proper spacing in method calls and parameter lists
- **Comment handling**: Limited comment preservation and formatting
- **No token awareness**: Treated code as simple text rather than structured language

## New Token-Based Architecture

### Key Features

1. **Proper Tokenization**: Full lexical analysis that recognizes:

   - Keywords: `reactiveclass`, `msgsrv`, `if`, `else`, `boolean`, etc.
   - Operators: `=`, `==`, `!=`, `&&`, `||`, `<`, `>`, etc.
   - Identifiers and literals
   - Comments (single-line and multi-line)
   - Punctuation: braces, parentheses, semicolons, commas

2. **Context-Aware Formatting**:

   - Understands code structure rather than just text patterns
   - Proper brace handling with nesting level tracking
   - Smart indentation based on syntax context

3. **Comprehensive Spacing Rules**:
   - **Binary operators**: `a = b`, `x == y`, `flag && condition`
   - **Method calls**: `object.method(param1, param2)` (no space before parentheses)
   - **Control structures**: `if (condition)` (space before parentheses)
   - **Semicolons**: No space before: `statement;`
   - **Commas**: Space after: `param1, param2`

## Formatting Rules Implemented

### For .rebeca Files

#### Indentation

- Uses tabs for consistent indentation
- Proper nesting level calculation
- Context-aware brace handling

#### Operator Spacing

```rebeca
// Before (incorrect):
if(x==y&&flag||other)
    result=value+1;

// After (correct):
if (x == y && flag || other)
    result = value + 1;
```

#### Method Calls and Declarations

```rebeca
// Before (incorrect):
object.method(param1,param2);
msgsrv methodName(int param1,boolean param2){

// After (correct):
object.method(param1, param2);
msgsrv methodName(int param1, boolean param2) {
```

#### Brace Placement

```rebeca
// Before (inconsistent):
reactiveclass MyClass(10){
    if(condition)
{
        // code
    }
}

// After (consistent):
reactiveclass MyClass(10) {
    if (condition) {
        // code
    }
}
```

### For .property Files

#### Property Definitions

```property
// Before (poor spacing):
property{
    define{
        p0eat=phil0.eating;
        condition=(node1.leaderId==node2.leaderId)&&
                          (node2.leaderId==node3.leaderId);
    }
}

// After (proper formatting):
property {
    define {
        p0eat = phil0.eating;
        condition = (node1.leaderId == node2.leaderId) &&
                    (node2.leaderId == node3.leaderId);
    }
}
```

## Technical Implementation

### Token Types

- `KEYWORD`: Rebeca language keywords
- `IDENTIFIER`: Variable and method names
- `OPERATOR`: All operators with proper spacing rules
- `BRACE`: `{` and `}` with context-aware handling
- `PARENTHESIS`: `(` and `)` with smart spacing
- `SEMICOLON`: `;` with no-space-before rule
- `COMMA`: `,` with space-after rule
- `COMMENT`: Both single-line and multi-line comments
- `STRING_LITERAL`: Quoted strings preserved as-is
- `NUMBER`: Numeric literals

### Smart Formatting Logic

1. **Tokenization Phase**: Parse code into structured tokens
2. **Context Analysis**: Understand code structure and nesting
3. **Formatting Phase**: Apply spacing and indentation rules
4. **Output Generation**: Reconstruct properly formatted code

### Comment Preservation

- Single-line comments (`//`) properly spaced and preserved
- Multi-line comments (`/* */`) with correct indentation
- Comments within expressions handled correctly

## Examples of Improvements

### Before (Original Formatter)

```rebeca
reactiveclass Philosopher(3){
knownrebecs{
Chopstick chpL,chpR;
}
statevars{
boolean eating;
boolean cL,cR;
}
Philosopher(){
cL=false;
cR=false;
eating=false;
self.arrive();
}
msgsrv arrive(){
chpL.request();
}
msgsrv permit(){
if(sender==chpL){
if(!cL){
cL=true;
chpR.request();
}
}else{
if(cL&&!(cR)){
cR=true;
self.eat();
}
}
}
}
```

### After (New Formatter)

```rebeca
reactiveclass Philosopher(3) {
    knownrebecs {
        Chopstick chpL, chpR;
    }
    statevars {
        boolean eating;
        boolean cL, cR;
    }
    Philosopher() {
        cL = false;
        cR = false;
        eating = false;
        self.arrive();
    }
    msgsrv arrive() {
        chpL.request();
    }
    msgsrv permit() {
        if (sender == chpL) {
            if (!cL) {
                cL = true;
                chpR.request();
            }
        } else {
            if (cL && !(cR)) {
                cR = true;
                self.eat();
            }
        }
    }
}
```

## Edge Cases Handled

1. **Complex Expressions**: Proper spacing in nested expressions
2. **Mixed Comments**: Code with inline and block comments
3. **String Literals**: Preserved exactly without formatting changes
4. **Operator Precedence**: Correct spacing regardless of operator combinations
5. **Method Chaining**: `object.method1().method2()` properly formatted
6. **Type Casts**: `(Customer)sender` with correct spacing
7. **Multi-line Statements**: Proper continuation indentation

## Benefits

1. **Professional Code Quality**: Consistent, readable formatting
2. **No Edge Cases**: Robust tokenization handles all syntax constructs
3. **Language Awareness**: Understands Rebeca syntax rather than treating as generic text
4. **Maintainability**: Clean, well-structured formatter code
5. **Extensibility**: Easy to add new formatting rules
6. **Performance**: Efficient single-pass tokenization and formatting

## Compatibility

- Works with both `.rebeca` and `.property` files
- Maintains Eclipse plugin interface compatibility
- Preserves all existing formatter integration points
- Backward compatible with existing Eclipse formatting infrastructure

The new formatter provides a solid foundation for professional Rebeca code development with consistent, high-quality formatting that eliminates the issues present in the previous implementation.
