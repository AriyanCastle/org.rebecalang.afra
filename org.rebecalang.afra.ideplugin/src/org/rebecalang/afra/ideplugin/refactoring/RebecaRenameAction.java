package org.rebecalang.afra.ideplugin.refactoring;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Handler for renaming Rebeca symbols (Alt+Shift+R)
 */
public class RebecaRenameAction extends AbstractHandler {
    
    private Text inlineRenameText;
    private Shell inlineRenameShell;
    private String originalSymbolName;
    private SymbolAnalysisResult currentAnalysisResult;
    private ITextEditor currentEditor;
    private IDocument currentDocument;
    private IFile currentFile;
    private int currentOffset;
    
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        System.out.println("[RebecaRename DEBUG] Execute method called");
        
        IEditorPart editor = HandlerUtil.getActiveEditor(event);
        System.out.println("[RebecaRename DEBUG] Active editor: " + editor);
        
        if (!(editor instanceof ITextEditor)) {
            System.out.println("[RebecaRename DEBUG] Editor is not ITextEditor: " + (editor != null ? editor.getClass().getName() : "null"));
            return null;
        }
        
        currentEditor = (ITextEditor) editor;
        Shell shell = currentEditor.getSite().getShell();
        System.out.println("[RebecaRename DEBUG] Got text editor and shell");
        
        try {
            // Get current selection/cursor position
            ISelection selection = currentEditor.getSelectionProvider().getSelection();
            System.out.println("[RebecaRename DEBUG] Selection: " + selection);
            
            if (!(selection instanceof ITextSelection)) {
                System.out.println("[RebecaRename DEBUG] Selection is not ITextSelection");
                showError(shell, "Please place cursor on a symbol to rename.");
                return null;
            }
            
            ITextSelection textSelection = (ITextSelection) selection;
            currentDocument = currentEditor.getDocumentProvider().getDocument(currentEditor.getEditorInput());
            currentOffset = textSelection.getOffset();
            System.out.println("[RebecaRename DEBUG] Current offset: " + currentOffset);
            
            // Get the file and project
            currentFile = (IFile) currentEditor.getEditorInput().getAdapter(IFile.class);
            System.out.println("[RebecaRename DEBUG] Current file: " + currentFile);
            if (currentFile == null) {
                System.out.println("[RebecaRename DEBUG] Could not determine current file");
                showError(shell, "Could not determine current file.");
                return null;
            }
            
            // Analyze symbol at cursor position
            System.out.println("[RebecaRename DEBUG] Analyzing symbol at position");
            currentAnalysisResult = analyzeSymbolAtPosition(currentDocument, currentOffset);
            System.out.println("[RebecaRename DEBUG] Analysis result: " + currentAnalysisResult);
            
            if (currentAnalysisResult == null) {
                System.out.println("[RebecaRename DEBUG] No renameable symbol found");
                showError(shell, "No renameable symbol found at cursor position.");
                return null;
            }
            
            originalSymbolName = currentAnalysisResult.symbolName;
            System.out.println("[RebecaRename DEBUG] Original symbol name: " + originalSymbolName);
            
            // Show inline rename widget
            System.out.println("[RebecaRename DEBUG] Calling showInlineRenameWidget");
            showInlineRenameWidget();
            System.out.println("[RebecaRename DEBUG] showInlineRenameWidget completed");
            
        } catch (Exception e) {
            System.out.println("[RebecaRename DEBUG] Exception in execute: " + e.getMessage());
            e.printStackTrace();
            showError(shell, "Rename operation failed: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Show the inline rename widget near the cursor position
     */
    private void showInlineRenameWidget() {
        System.out.println("[RebecaRename DEBUG] showInlineRenameWidget started");
        try {
            // Get the source viewer from the editor using reflection or alternative methods
            System.out.println("[RebecaRename DEBUG] Getting source viewer from editor");
            ISourceViewer sourceViewer = getSourceViewerFromEditor(currentEditor);
            System.out.println("[RebecaRename DEBUG] Source viewer: " + sourceViewer);
            
            if (sourceViewer == null) {
                System.out.println("[RebecaRename DEBUG] Source viewer is null, trying alternative approach");
                showInlineRenameWidgetAlternative();
                return;
            }
            
            // Get cursor position in screen coordinates
            System.out.println("[RebecaRename DEBUG] Getting cursor screen location");
            Point cursorLocation = getCursorScreenLocation(sourceViewer);
            System.out.println("[RebecaRename DEBUG] Cursor location: " + cursorLocation);
            
            if (cursorLocation == null) {
                System.out.println("[RebecaRename DEBUG] Cursor location is null, returning");
                return;
            }
            
            // Create a small shell for the inline text
            System.out.println("[RebecaRename DEBUG] Creating inline rename shell");
            inlineRenameShell = new Shell(currentEditor.getSite().getShell(), SWT.NO_TRIM | SWT.ON_TOP);
            inlineRenameShell.setLayout(new org.eclipse.swt.layout.FillLayout());
            System.out.println("[RebecaRename DEBUG] Shell created: " + inlineRenameShell);
            
            // Create the text widget
            System.out.println("[RebecaRename DEBUG] Creating text widget with symbol: " + originalSymbolName);
            inlineRenameText = new Text(inlineRenameShell, SWT.BORDER);
            inlineRenameText.setText(originalSymbolName);
            inlineRenameText.selectAll();
            System.out.println("[RebecaRename DEBUG] Text widget created: " + inlineRenameText);
            
            // Calculate size and position
            Point textSize = inlineRenameText.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            textSize.x = Math.max(textSize.x, originalSymbolName.length() * 8 + 20); // minimum width
            System.out.println("[RebecaRename DEBUG] Text size: " + textSize);
            
            inlineRenameShell.setSize(textSize.x, textSize.y);
            inlineRenameShell.setLocation(cursorLocation.x, cursorLocation.y + 20); // slightly below cursor
            System.out.println("[RebecaRename DEBUG] Shell positioned at: " + cursorLocation.x + ", " + (cursorLocation.y + 20));
            
            // Add event handlers
            System.out.println("[RebecaRename DEBUG] Setting up event handlers");
            setupInlineRenameEventHandlers();
            
            // Show the shell and focus the text
            System.out.println("[RebecaRename DEBUG] Opening shell and setting focus");
            inlineRenameShell.open();
            inlineRenameText.setFocus();
            System.out.println("[RebecaRename DEBUG] Shell opened and focused");
            
        } catch (Exception e) {
            System.out.println("[RebecaRename DEBUG] Exception in showInlineRenameWidget: " + e.getMessage());
            e.printStackTrace();
            hideInlineRenameWidget();
        }
    }
    
    /**
     * Get the source viewer from the editor using multiple approaches
     */
    private ISourceViewer getSourceViewerFromEditor(ITextEditor editor) {
        System.out.println("[RebecaRename DEBUG] getSourceViewerFromEditor started");
        
        // Try 1: Standard adapter approach
        ISourceViewer sourceViewer = (ISourceViewer) editor.getAdapter(ISourceViewer.class);
        if (sourceViewer != null) {
            System.out.println("[RebecaRename DEBUG] Got source viewer via adapter");
            return sourceViewer;
        }
        
        // Try 2: If editor extends AbstractTextEditor, use reflection to get the source viewer
        try {
            if (editor instanceof org.eclipse.ui.texteditor.AbstractTextEditor) {
                System.out.println("[RebecaRename DEBUG] Editor is AbstractTextEditor, trying reflection");
                
                // Use reflection to access the protected getSourceViewer method
                java.lang.reflect.Method method = org.eclipse.ui.texteditor.AbstractTextEditor.class.getDeclaredMethod("getSourceViewer");
                method.setAccessible(true);
                Object result = method.invoke(editor);
                
                if (result instanceof ISourceViewer) {
                    System.out.println("[RebecaRename DEBUG] Got source viewer via reflection");
                    return (ISourceViewer) result;
                }
            }
        } catch (Exception e) {
            System.out.println("[RebecaRename DEBUG] Reflection approach failed: " + e.getMessage());
        }
        
        // Try 3: Check if it's a specific Rebeca editor with known methods
        try {
            // Try to call getSourceViewer() method if it exists on the editor
            java.lang.reflect.Method method = editor.getClass().getMethod("getSourceViewer");
            Object result = method.invoke(editor);
            if (result instanceof ISourceViewer) {
                System.out.println("[RebecaRename DEBUG] Got source viewer via editor method");
                return (ISourceViewer) result;
            }
        } catch (Exception e) {
            System.out.println("[RebecaRename DEBUG] Editor method approach failed: " + e.getMessage());
        }
        
        System.out.println("[RebecaRename DEBUG] All source viewer approaches failed");
        return null;
    }
    
    /**
     * Alternative approach when source viewer is not available
     */
    private void showInlineRenameWidgetAlternative() {
        System.out.println("[RebecaRename DEBUG] showInlineRenameWidgetAlternative started");
        try {
            // Get the editor's control to position the widget
            org.eclipse.swt.widgets.Control editorControl = (org.eclipse.swt.widgets.Control) currentEditor.getAdapter(org.eclipse.swt.widgets.Control.class);
            if (editorControl == null) {
                System.out.println("[RebecaRename DEBUG] Could not get editor control, using shell positioning");
                showInlineRenameWidgetAtCenter();
                return;
            }
            
            System.out.println("[RebecaRename DEBUG] Got editor control: " + editorControl);
            
            // Get the bounds of the editor control
            org.eclipse.swt.graphics.Rectangle bounds = editorControl.getBounds();
            org.eclipse.swt.graphics.Point location = editorControl.toDisplay(bounds.x + 100, bounds.y + 100);
            
            System.out.println("[RebecaRename DEBUG] Editor control bounds: " + bounds);
            System.out.println("[RebecaRename DEBUG] Calculated location: " + location);
            
            // Create the inline rename widget at calculated position
            createInlineRenameWidget(location);
            
        } catch (Exception e) {
            System.out.println("[RebecaRename DEBUG] Alternative approach failed: " + e.getMessage());
            e.printStackTrace();
            showInlineRenameWidgetAtCenter();
        }
    }
    
    /**
     * Show the widget at the center of the editor as fallback
     */
    private void showInlineRenameWidgetAtCenter() {
        System.out.println("[RebecaRename DEBUG] showInlineRenameWidgetAtCenter started");
        try {
            Shell parentShell = currentEditor.getSite().getShell();
            org.eclipse.swt.graphics.Rectangle bounds = parentShell.getBounds();
            
            // Position at center of the parent shell
            org.eclipse.swt.graphics.Point location = new org.eclipse.swt.graphics.Point(
                bounds.x + bounds.width / 2, 
                bounds.y + bounds.height / 2
            );
            
            System.out.println("[RebecaRename DEBUG] Center location: " + location);
            createInlineRenameWidget(location);
            
        } catch (Exception e) {
            System.out.println("[RebecaRename DEBUG] Center positioning failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create the inline rename widget at the specified location
     */
    private void createInlineRenameWidget(org.eclipse.swt.graphics.Point location) {
        System.out.println("[RebecaRename DEBUG] createInlineRenameWidget at: " + location);
        try {
            // Create a shell for the inline rename widget
            inlineRenameShell = new Shell(currentEditor.getSite().getShell(), SWT.NO_TRIM | SWT.ON_TOP);
            inlineRenameShell.setLayout(new org.eclipse.swt.layout.GridLayout(1, false));
            inlineRenameShell.setBackground(inlineRenameShell.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
            System.out.println("[RebecaRename DEBUG] Shell created");
            
            // Create the label
            org.eclipse.swt.widgets.Label instructionLabel = new org.eclipse.swt.widgets.Label(inlineRenameShell, SWT.NONE);
            instructionLabel.setText("Enter new name, press Enter to rename:");
            instructionLabel.setBackground(inlineRenameShell.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
            instructionLabel.setForeground(inlineRenameShell.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
            org.eclipse.swt.layout.GridData labelData = new org.eclipse.swt.layout.GridData(SWT.FILL, SWT.CENTER, true, false);
            labelData.horizontalIndent = 4;
            labelData.verticalIndent = 2;
            instructionLabel.setLayoutData(labelData);
            
            // Create the text widget
            inlineRenameText = new Text(inlineRenameShell, SWT.BORDER);
            inlineRenameText.setText(originalSymbolName);
            inlineRenameText.selectAll();
            System.out.println("[RebecaRename DEBUG] Text widget created with: " + originalSymbolName);
            
            // Make the text field larger
            org.eclipse.swt.layout.GridData textData = new org.eclipse.swt.layout.GridData(SWT.FILL, SWT.CENTER, true, false);
            textData.horizontalIndent = 4;
            textData.verticalIndent = 2;
            textData.minimumWidth = Math.max(200, originalSymbolName.length() * 12 + 40); // larger minimum width
            textData.widthHint = textData.minimumWidth;
            inlineRenameText.setLayoutData(textData);
            
            // Calculate shell size
            Point shellSize = inlineRenameShell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            inlineRenameShell.setSize(shellSize.x + 8, shellSize.y + 4); // add padding
            inlineRenameShell.setLocation(location.x, location.y);
            System.out.println("[RebecaRename DEBUG] Widget size: " + shellSize);
            
            // Add event handlers
            setupInlineRenameEventHandlers();
            
            // Show the shell and focus the text
            inlineRenameShell.open();
            inlineRenameText.setFocus();
            System.out.println("[RebecaRename DEBUG] Widget opened and focused");
            
        } catch (Exception e) {
            System.out.println("[RebecaRename DEBUG] createInlineRenameWidget failed: " + e.getMessage());
            e.printStackTrace();
            hideInlineRenameWidget();
        }
    }
    
    /**
     * Get the cursor location in screen coordinates
     */
    private Point getCursorScreenLocation(ISourceViewer sourceViewer) {
        System.out.println("[RebecaRename DEBUG] getCursorScreenLocation started");
        try {
            // Get the text widget
            org.eclipse.swt.custom.StyledText textWidget = sourceViewer.getTextWidget();
            System.out.println("[RebecaRename DEBUG] Text widget: " + textWidget);
            if (textWidget == null) {
                System.out.println("[RebecaRename DEBUG] Text widget is null");
                return null;
            }
            
            // Get current caret position in the text widget
            int caretOffset = textWidget.getCaretOffset();
            System.out.println("[RebecaRename DEBUG] Caret offset: " + caretOffset);
            
            // Get the location of the caret in the text widget
            Point location = textWidget.getLocationAtOffset(caretOffset);
            System.out.println("[RebecaRename DEBUG] Location in widget: " + location);
            
            // Convert to screen coordinates
            Point screenLocation = textWidget.toDisplay(location);
            System.out.println("[RebecaRename DEBUG] Screen location: " + screenLocation);
            return screenLocation;
            
        } catch (Exception e) {
            System.out.println("[RebecaRename DEBUG] Exception in getCursorScreenLocation: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Setup event handlers for the inline rename widget
     */
    private void setupInlineRenameEventHandlers() {
        // Handle Enter key to confirm rename
        inlineRenameText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
                    // Enter pressed - perform rename
                    performInlineRename();
                } else if (e.keyCode == SWT.ESC) {
                    // Escape pressed - cancel rename
                    hideInlineRenameWidget();
                }
            }
        });
        
        // Hide widget when focus is lost
        inlineRenameText.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                hideInlineRenameWidget();
            }
        });
    }
    
    /**
     * Perform the actual rename operation when Enter is pressed
     */
    private void performInlineRename() {
        try {
            String newName = inlineRenameText.getText().trim();
            
            // Validate the new name
            if (newName.isEmpty() || newName.equals(originalSymbolName)) {
                hideInlineRenameWidget();
                return;
            }
            
            if (!isValidName(newName)) {
                showError(inlineRenameShell, "Invalid identifier name: " + newName);
                return;
            }
            
            // Hide the widget first
            hideInlineRenameWidget();
            
            // Find all occurrences and perform rename
            IProject project = currentFile.getProject();
            RebecaRefactoringParticipant refactoring = new RebecaRefactoringParticipant(project);
            
            System.out.println("[RebecaRename DEBUG] *** Starting findAllOccurrences ***");
            System.out.println("[RebecaRename DEBUG] Symbol: '" + currentAnalysisResult.symbolName + "'");
            System.out.println("[RebecaRename DEBUG] Type: " + currentAnalysisResult.symbolType);
            System.out.println("[RebecaRename DEBUG] File: " + currentFile.getName());
            System.out.println("[RebecaRename DEBUG] Offset: " + currentOffset);
            
            List<RebecaRefactoringParticipant.SymbolOccurrence> occurrences = refactoring.findAllOccurrences(
                currentAnalysisResult.symbolName, 
                currentAnalysisResult.symbolType, 
                currentFile, 
                currentOffset
            );
            
            System.out.println("[RebecaRename DEBUG] *** findAllOccurrences completed ***");
            System.out.println("[RebecaRename DEBUG] Found " + occurrences.size() + " occurrences:");
            for (RebecaRefactoringParticipant.SymbolOccurrence occ : occurrences) {
                System.out.println("[RebecaRename DEBUG]   - " + occ.file.getName() + " at offset " + occ.offset + " ('" + occ.originalName + "')");
            }
            
            if (occurrences.isEmpty()) {
                showError(currentEditor.getSite().getShell(), "No occurrences found for symbol '" + originalSymbolName + "'.");
                return;
            }
            
            // Perform the rename operation
            performRename(currentEditor.getSite().getShell(), newName, occurrences);
            
        } catch (Exception e) {
            hideInlineRenameWidget();
            showError(currentEditor.getSite().getShell(), "Rename operation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Hide the inline rename widget
     */
    private void hideInlineRenameWidget() {
        if (inlineRenameShell != null && !inlineRenameShell.isDisposed()) {
            inlineRenameShell.dispose();
        }
        inlineRenameShell = null;
        inlineRenameText = null;
    }
    
    /**
     * Validate if the given name is a valid identifier
     */
    private boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        // Check if it's a valid identifier
        name = name.trim();
        if (!Character.isJavaIdentifierStart(name.charAt(0))) {
            return false;
        }
        
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) {
                return false;
            }
        }
        
        // Check if it's not a reserved keyword
        return !isReservedKeyword(name);
    }
    
    /**
     * Check if the name is a reserved keyword
     */
    private boolean isReservedKeyword(String name) {
        String[] keywords = {
            "reactiveclass", "msgsrv", "knownrebecs", "statevars", "main", 
            "if", "else", "while", "for", "true", "false", "self", "sender",
            "boolean", "int", "byte", "short", "long", "float", "double",
            "after", "deadline", "delay"
        };
        
        for (String keyword : keywords) {
            if (keyword.equals(name)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Analyze the symbol at the given position
     */
    private SymbolAnalysisResult analyzeSymbolAtPosition(IDocument document, int offset) {
        System.out.println("[RebecaRename DEBUG] analyzeSymbolAtPosition started, offset: " + offset);
        try {
            // Get the word at the cursor position
            IRegion wordRegion = getWordRegion(document, offset);
            System.out.println("[RebecaRename DEBUG] Word region: " + wordRegion);
            if (wordRegion == null) {
                System.out.println("[RebecaRename DEBUG] Word region is null");
                return null;
            }
            
            String word = document.get(wordRegion.getOffset(), wordRegion.getLength());
            System.out.println("[RebecaRename DEBUG] Found word: '" + word + "'");
            if (word.trim().isEmpty()) {
                System.out.println("[RebecaRename DEBUG] Word is empty");
                return null;
            }
            
        // Determine symbol type based on context
        System.out.println("[RebecaRename DEBUG] Determining symbol type");
        RebecaRefactoringParticipant.SymbolType symbolType = determineSymbolType(document, wordRegion.getOffset(), word);
        System.out.println("[RebecaRename DEBUG] Symbol type: " + symbolType);
        if (symbolType == null) {
            System.out.println("[RebecaRename DEBUG] Symbol type is null");
            return null;
        }
        
        SymbolAnalysisResult result = new SymbolAnalysisResult(word, symbolType);
        System.out.println("[RebecaRename DEBUG] Analysis result created: " + result.symbolName + " (" + result.symbolType + ")");
        return result;
            
        } catch (BadLocationException e) {
            System.out.println("[RebecaRename DEBUG] BadLocationException in analyzeSymbolAtPosition: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            System.out.println("[RebecaRename DEBUG] Exception in analyzeSymbolAtPosition: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Get the word region at the given offset
     */
    private IRegion getWordRegion(IDocument document, int offset) throws BadLocationException {
        int start = offset;
        int end = offset;
        
        // Move start backwards
        while (start > 0) {
            char c = document.getChar(start - 1);
            if (!Character.isJavaIdentifierPart(c)) {
                break;
            }
            start--;
        }
        
        // Move end forwards
        while (end < document.getLength()) {
            char c = document.getChar(end);
            if (!Character.isJavaIdentifierPart(c)) {
                break;
            }
            end++;
        }
        
        if (end > start) {
            return new org.eclipse.jface.text.Region(start, end - start);
        }
        
        return null;
    }
    
    /**
     * Determine the type of symbol based on context
     */
    private RebecaRefactoringParticipant.SymbolType determineSymbolType(IDocument document, int offset, String word) {
        try {
            String content = document.get();
            
            // Get surrounding context
            int lineStart = document.getLineOffset(document.getLineOfOffset(offset));
            int lineEnd = lineStart + document.getLineLength(document.getLineOfOffset(offset));
            String line = content.substring(lineStart, lineEnd);
            
            int positionInLine = offset - lineStart;
            
            // Check for class declaration: reactiveclass ClassName
            if (line.matches(".*\\breactiveclass\\s+" + java.util.regex.Pattern.quote(word) + "\\b.*")) {
                return RebecaRefactoringParticipant.SymbolType.CLASS_NAME;
            }
            
            // Check for method declaration: msgsrv methodName
            if (line.matches(".*\\bmsgsrv\\s+" + java.util.regex.Pattern.quote(word) + "\\s*\\(.*")) {
                return RebecaRefactoringParticipant.SymbolType.METHOD_NAME;
            }
            
            // Check for method call: something.methodName(
            if (line.matches(".*\\w+\\." + java.util.regex.Pattern.quote(word) + "\\s*\\(.*") ||
                line.matches(".*\\bself\\." + java.util.regex.Pattern.quote(word) + "\\s*\\(.*")) {
                return RebecaRefactoringParticipant.SymbolType.METHOD_NAME;
            }
            
            // Check for class usage in main block or knownrebecs
            String beforeWord = line.substring(0, positionInLine);
            String afterWord = line.substring(positionInLine + word.length());
            
            if (beforeWord.trim().isEmpty() && afterWord.matches("\\s+\\w+.*")) {
                // Pattern: ClassName instanceName
                return RebecaRefactoringParticipant.SymbolType.CLASS_NAME;
            }
            
            // Check if it's in knownrebecs section
            if (isInSection(content, offset, "knownrebecs")) {
                System.out.println("[RebecaRename DEBUG] In knownrebecs section, word: '" + word + "'");
                System.out.println("[RebecaRename DEBUG] beforeWord: '" + beforeWord + "'");
                System.out.println("[RebecaRename DEBUG] afterWord: '" + afterWord + "'");
                System.out.println("[RebecaRename DEBUG] full line: '" + line + "'");
                
                // Check if this is a class name by looking at the pattern: ClassName varName1, varName2, ...;
                // beforeWord should only contain whitespace (indentation), and afterWord should have variables
                if (beforeWord.trim().isEmpty() && afterWord.matches("\\s+[\\w\\s,]+;.*")) {
                    // Pattern: ClassName varName1, varName2, ...;
                    System.out.println("[RebecaRename DEBUG] Detected as CLASS_NAME");
                    return RebecaRefactoringParticipant.SymbolType.CLASS_NAME;
                } 
                // Check if it's before a comma or semicolon (variable/instance name)
                else if (afterWord.matches("\\s*[,;].*") || afterWord.matches("\\s+\\w+.*[,;].*")) {
                    // Pattern: varName1, varName2, ... or varName;
                    System.out.println("[RebecaRename DEBUG] Detected as INSTANCE_NAME");
                    return RebecaRefactoringParticipant.SymbolType.INSTANCE_NAME;
                }
                // If preceding by a class name, it's an instance name
                else if (beforeWord.matches(".*\\w\\s*$")) {
                    System.out.println("[RebecaRename DEBUG] Detected as INSTANCE_NAME (after class)");
                    return RebecaRefactoringParticipant.SymbolType.INSTANCE_NAME;
                } else {
                    // Default to instance name in knownrebecs
                    System.out.println("[RebecaRename DEBUG] Defaulting to INSTANCE_NAME");
                    return RebecaRefactoringParticipant.SymbolType.INSTANCE_NAME;
                }
            }
            
            // Check if it's in main section  
            if (isInSection(content, offset, "main")) {
                System.out.println("[RebecaRename DEBUG] In main section, word: '" + word + "'");
                System.out.println("[RebecaRename DEBUG] beforeWord: '" + beforeWord + "'");
                System.out.println("[RebecaRename DEBUG] afterWord: '" + afterWord + "'");
                System.out.println("[RebecaRename DEBUG] full line: '" + line + "'");
                
                // Check if this is a class usage: ClassName instanceName(params):();
                if (beforeWord.trim().isEmpty() && afterWord.matches("\\s+\\w+\\s*\\([^\\)]*\\)\\s*:\\s*\\([^\\)]*\\)\\s*;.*")) {
                    // Pattern: ClassName instanceName(params):();
                    System.out.println("[RebecaRename DEBUG] Detected as CLASS_NAME in main");
                    return RebecaRefactoringParticipant.SymbolType.CLASS_NAME;
                }
                // Check if this is an instance name: ClassName instanceName(params):();
                else if (beforeWord.matches(".*\\w\\s+$") && afterWord.matches("\\s*\\([^\\)]*\\)\\s*:\\s*\\([^\\)]*\\)\\s*;.*")) {
                    // Pattern: instanceName after class name
                    System.out.println("[RebecaRename DEBUG] Detected as INSTANCE_NAME in main");
                    return RebecaRefactoringParticipant.SymbolType.INSTANCE_NAME;
                }
                // Check if it's a parameter reference inside parentheses
                else if (beforeWord.matches(".*\\(.*") && afterWord.matches(".*\\).*")) {
                    System.out.println("[RebecaRename DEBUG] Detected as INSTANCE_NAME (parameter reference) in main");
                    return RebecaRefactoringParticipant.SymbolType.INSTANCE_NAME;
                }
            }
            
            // Check if it's in statevars section (variable declaration)
            if (isInSection(content, offset, "statevars")) {
                return RebecaRefactoringParticipant.SymbolType.VARIABLE_NAME;
            }
            
            // Check for property definition in .property files
            if (line.matches("\\s*" + java.util.regex.Pattern.quote(word) + "\\s*=.*")) {
                return RebecaRefactoringParticipant.SymbolType.PROPERTY_NAME;
            }
            
            // Default: treat as variable or instance name
            if (line.contains(".")) {
                // If it's part of object.field, likely an instance or variable
                if (beforeWord.endsWith(".")) {
                    return RebecaRefactoringParticipant.SymbolType.VARIABLE_NAME; // field access
                } else if (afterWord.startsWith(".")) {
                    return RebecaRefactoringParticipant.SymbolType.INSTANCE_NAME; // object access
                }
            }
            
            // Default to variable name for standalone identifiers
            return RebecaRefactoringParticipant.SymbolType.VARIABLE_NAME;
            
        } catch (BadLocationException e) {
            return null;
        }
    }
    
    /**
     * Check if the offset is within a specific section (statevars, knownrebecs, etc.)
     */
    private boolean isInSection(String content, int offset, String sectionName) {
        // Find the nearest section before the offset
        String beforeOffset = content.substring(0, offset);
        
        int lastSectionStart = beforeOffset.lastIndexOf(sectionName);
        if (lastSectionStart == -1) {
            return false;
        }
        
        // Check if we're within the section's braces
        int braceStart = content.indexOf('{', lastSectionStart);
        if (braceStart == -1 || braceStart > offset) {
            return false;
        }
        
        // Find matching closing brace
        int braceCount = 1;
        int pos = braceStart + 1;
        while (pos < content.length() && braceCount > 0) {
            if (content.charAt(pos) == '{') {
                braceCount++;
            } else if (content.charAt(pos) == '}') {
                braceCount--;
            }
            pos++;
        }
        
        return offset < pos - 1;
    }
    
    /**
     * Perform the actual rename operation
     */
    private void performRename(Shell shell, String newName, List<RebecaRefactoringParticipant.SymbolOccurrence> occurrences) {
        try {
            // Group occurrences by file
            java.util.Map<IFile, List<RebecaRefactoringParticipant.SymbolOccurrence>> fileOccurrences = new java.util.HashMap<>();
            for (RebecaRefactoringParticipant.SymbolOccurrence occ : occurrences) {
                fileOccurrences.computeIfAbsent(occ.file, k -> new java.util.ArrayList<>()).add(occ);
            }
            
            // Process each file
            int totalRenamed = 0;
            for (java.util.Map.Entry<IFile, List<RebecaRefactoringParticipant.SymbolOccurrence>> entry : fileOccurrences.entrySet()) {
                totalRenamed += renameInFile(entry.getKey(), entry.getValue(), newName);
            }
            
            // Show success message
            MessageDialog.openInformation(shell, "Rename Complete", 
                String.format("Successfully renamed %d occurrence(s) in %d file(s).", 
                            totalRenamed, fileOccurrences.size()));
            
        } catch (Exception e) {
            showError(shell, "Rename operation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Rename occurrences in a single file
     */
    private int renameInFile(IFile file, List<RebecaRefactoringParticipant.SymbolOccurrence> occurrences, String newName) throws Exception {
        org.eclipse.ui.texteditor.IDocumentProvider provider = new org.eclipse.ui.editors.text.TextFileDocumentProvider();
        provider.connect(file);
        
        try {
            IDocument document = provider.getDocument(file);
            
            // Sort occurrences by offset in reverse order (to maintain offsets while replacing)
            occurrences.sort((a, b) -> Integer.compare(b.offset, a.offset));
            
            // Replace each occurrence
            for (RebecaRefactoringParticipant.SymbolOccurrence occ : occurrences) {
                document.replace(occ.offset, occ.length, newName);
            }
            
            // Save the document
            provider.saveDocument(null, file, document, true);
            
            return occurrences.size();
            
        } finally {
            provider.disconnect(file);
        }
    }
    
    private void showError(Shell shell, String message) {
        MessageDialog.openError(shell, "Rename Error", message);
    }
    
    /**
     * Result of symbol analysis
     */
    private static class SymbolAnalysisResult {
        final String symbolName;
        final RebecaRefactoringParticipant.SymbolType symbolType;
        
        SymbolAnalysisResult(String symbolName, RebecaRefactoringParticipant.SymbolType symbolType) {
            this.symbolName = symbolName;
            this.symbolType = symbolType;
        }
    }
}
