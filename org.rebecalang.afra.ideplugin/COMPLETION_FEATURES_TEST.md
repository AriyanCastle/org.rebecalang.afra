# Rebeca Auto-Completion Features Test Guide

## Summary of Implemented Features

### 1. Method Parameter Context-Aware Completion (.rebeca files)

**Implementation**: Enhanced `RebecaContextAwareCompletionProcessor.java`

- Added `getCurrentMethod()` to detect current method scope
- Added `addMethodParameterCompletions()` to suggest method parameters
- Parameters only appear within the method scope where they are defined

**Test**: In `DiningPhilosophers.rebeca`, within a method like:

```rebeca
msgsrv arrive() {
    // typing "c" should suggest no method parameters (none defined)
}

// Or for a method with parameters:
msgsrv someMethod(int param1, boolean param2) {
    // typing "p" should suggest "param1" and "param2"
    // typing "pa" should suggest "param1" and "param2"
}
```

### 2. Object Method Completion Fix (.rebeca files)

**Implementation**: Enhanced `addFilteredMethodsAndFields()` and self method completion

- Fixed completion when typing "object." without additional text
- Now shows ALL methods/fields when partial text is empty
- Applies to both "self." and "objectInstance." patterns

**Test**: In `DiningPhilosophers.rebeca`:

```rebeca
msgsrv arrive() {
    self.  // Should show: arrive(), permit(), eat(), leave()
    chpL.  // Should show: request(), release() (and any state vars)
}
```

### 3. Context-Aware Completion for .property Files

**Implementation**: New `RebecaPropContextAwareCompletionProcessor.java`

- Suggests defined variables from the same property file
- Cross-file context awareness with corresponding .rebeca files
- Context-aware completion for `define` statements

**Test**: In `DiningPhilosophers.property`:

```property
property {
    define {
        p0eat = phil0.eating;  // "phil0" comes from DiningPhilosophers.rebeca main
        p1eat = phil1.eating;  // "eating" comes from Philosopher class statevars
        // typing "p" should suggest "p0eat", "p1eat", etc. (defined vars)
        // typing "phil" should suggest "phil0", "phil1", "phil2" (from main)
        // typing "phil0." should suggest "eating", "cL", "cR" (statevars)
    }

    LTL {
        NoStarvation: G(F(p0eat));  // "p0eat" should be suggested as defined var
    }
}
```

### 4. Cross-File Context Awareness (.property ↔ .rebeca)

**Implementation**: Integrated in `RebecaPropContextAwareCompletionProcessor.java`

- Automatically finds corresponding .rebeca file (same name, same directory)
- Extracts class instances from main method
- Provides state variables from reactive class definitions

**Test Scenarios**:

#### Scenario A: LeaderElection Example

- `LeaderElection.property` ↔ `LeaderElection.rebeca`
- In property file define: `newLeaderIsElected = node1.leaderId;`
- "node1" should be suggested from main method in rebeca file
- "leaderId" should be suggested from Node class statevars

#### Scenario B: DiningPhilosophers Example

- `DiningPhilosophers.property` ↔ `DiningPhilosophers.rebeca`
- In property file define: `p0eat = phil0.eating;`
- "phil0", "phil1", "phil2" should be suggested from main method
- "eating", "cL", "cR" should be suggested from Philosopher class statevars

## Architecture Changes

### Files Modified:

1. **RebecaContextAwareCompletionProcessor.java** - Enhanced for method parameters and dot completion
2. **RebecaPropSourceViewerConfiguration.java** - Updated to use new context-aware processor

### Files Created:

3. **RebecaPropContextAwareCompletionProcessor.java** - New context-aware processor for .property files

## Key Features Implemented:

### Context Detection:

- **Method Scope Detection**: Determines if cursor is within a method to suggest parameters
- **Define Block Detection**: Recognizes define blocks in property files
- **Dot Completion Context**: Handles "object.field" patterns correctly
- **Cross-File Resolution**: Automatically finds and parses corresponding files

### Smart Filtering:

- **Empty Partial Text**: Shows all options when user types "object." with no additional text
- **Case-Insensitive Matching**: Flexible matching for better user experience
- **Context-Aware Suggestions**: Different suggestions based on current context

### Error Handling:

- **Compilation Fallbacks**: Graceful handling when compilation fails
- **File Not Found**: Safe handling when corresponding files don't exist
- **Malformed Content**: Robust parsing with error recovery

## Usage Instructions:

1. **For .rebeca files**: Use existing auto-completion (Ctrl+Space)

   - Method parameters now appear only within method scope
   - Object completion works with just "object."

2. **For .property files**: Use auto-completion (Ctrl+Space)
   - Defined variables are suggested throughout the file
   - Cross-file suggestions work in define statements
   - Object.field patterns are context-aware

## Expected Behavior:

- **Fast Response**: Completion should be responsive even with compilation
- **Accurate Context**: Suggestions match the current editing context
- **Cross-File Sync**: Changes in .rebeca files reflect in .property completion
- **Graceful Degradation**: Basic completion works even if advanced features fail
