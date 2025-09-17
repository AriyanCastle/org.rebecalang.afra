package org.rebecalang.afra.ideplugin.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.rebecalang.afra.ideplugin.editors.rebeca.RebecaEditor;
import org.rebecalang.afra.ideplugin.editors.rebecaprop.RebecaPropEditor;

/**
 * Handler for format document command
 */
public class FormatHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        try {
            IEditorPart activeEditor = PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow()
                    .getActivePage()
                    .getActiveEditor();
            
            if (activeEditor instanceof RebecaEditor) {
                formatRebecaEditor((RebecaEditor) activeEditor);
            } else if (activeEditor instanceof RebecaPropEditor) {
                formatPropertyEditor((RebecaPropEditor) activeEditor);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    private void formatRebecaEditor(RebecaEditor editor) {
        try {
            ISourceViewer sourceViewer = editor.getSourceViewer();
            if (sourceViewer != null) {
                IContentFormatter formatter = editor.getSourceViewerConfiguration()
                        .getContentFormatter(sourceViewer);
                
                if (formatter != null) {
                    IDocument document = sourceViewer.getDocument();
                    ISelection selection = sourceViewer.getSelectionProvider().getSelection();
                    
                    if (selection instanceof ITextSelection && !selection.isEmpty()) {
                        // Format selection
                        ITextSelection textSelection = (ITextSelection) selection;
                        formatter.format(document, 
                            new org.eclipse.jface.text.Region(textSelection.getOffset(), textSelection.getLength()));
                    } else {
                        // Format entire document
                        formatter.format(document, 
                            new org.eclipse.jface.text.Region(0, document.getLength()));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error formatting Rebeca file: " + e.getMessage());
        }
    }
    
    private void formatPropertyEditor(RebecaPropEditor editor) {
        try {
            ISourceViewer sourceViewer = editor.getSourceViewer();
            if (sourceViewer != null) {
                IContentFormatter formatter = editor.getSourceViewerConfiguration()
                        .getContentFormatter(sourceViewer);
                
                if (formatter != null) {
                    IDocument document = sourceViewer.getDocument();
                    ISelection selection = sourceViewer.getSelectionProvider().getSelection();
                    
                    if (selection instanceof ITextSelection && !selection.isEmpty()) {
                        // Format selection
                        ITextSelection textSelection = (ITextSelection) selection;
                        formatter.format(document, 
                            new org.eclipse.jface.text.Region(textSelection.getOffset(), textSelection.getLength()));
                    } else {
                        // Format entire document
                        formatter.format(document, 
                            new org.eclipse.jface.text.Region(0, document.getLength()));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error formatting Property file: " + e.getMessage());
        }
    }
}
