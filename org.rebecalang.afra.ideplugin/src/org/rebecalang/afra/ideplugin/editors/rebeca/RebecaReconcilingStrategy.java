package org.rebecalang.afra.ideplugin.editors.rebeca;

import java.util.ArrayList;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.swt.widgets.Display;

public class RebecaReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {
    private RebecaEditor editor;
    private IDocument fDocument;
    protected final ArrayList<Position> fPositions = new ArrayList<Position>();
    protected int fRangeEnd;

    public RebecaEditor getEditor() {
        return this.editor;
    }

    public void setEditor(RebecaEditor editor) {
        this.editor = editor;
    }

    public void setDocument(IDocument document) {
        this.fDocument = document;
    }

    public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
        initialReconcile();
    }

    public void reconcile(IRegion partition) {
        initialReconcile();
    }

    public void setProgressMonitor(IProgressMonitor monitor) {
        // TODO Auto-generated method stub
    }

    public void initialReconcile() {
        fRangeEnd = fDocument.getLength();
        calculatePositions();
    }

    protected void calculatePositions() {
        fPositions.clear();
        try {
            getTokens();
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                editor.updateFoldingStructure(fPositions);
            }
        });
    }

    protected void emitPosition(int startOffset, int length) {
        fPositions.add(new Position(startOffset, length));
    }

    protected void getTokens() throws BadLocationException {
        ArrayList<Integer> openPositions = new ArrayList<Integer>();
        boolean inString = false;
        boolean inSingleLineComment = false;
        boolean inMultiLineComment = false;
        
        for(int pos = 0; pos < fRangeEnd; pos += 1) {
            char ch = fDocument.getChar(pos);
            char nextCh = (pos + 1 < fRangeEnd) ? fDocument.getChar(pos + 1) : '\0';
            
            if (!inString && !inSingleLineComment && !inMultiLineComment) {
                if (ch == '"') {
                    inString = true;
                } else if (ch == '/' && nextCh == '/') {
                    inSingleLineComment = true;
                } else if (ch == '/' && nextCh == '*') {
                    inMultiLineComment = true;
                } else if (ch == '{') {
                    openPositions.add(pos);
                } else if (ch == '}') {
                    if (openPositions.size() > 0) {
                        int lastIndex = openPositions.size() - 1;
                        int lastOpen = openPositions.get(lastIndex);
                        openPositions.remove(lastIndex);
                        emitPosition(lastOpen, pos - lastOpen + 1);
                    }
                }
            } else if (inString) {
                if (ch == '"' && (pos == 0 || fDocument.getChar(pos - 1) != '\\')) {
                    inString = false;
                }
            } else if (inSingleLineComment) {
                if (ch == '\n' || ch == '\r') {
                    inSingleLineComment = false;
                }
            } else if (inMultiLineComment) {
                if (ch == '*' && nextCh == '/') {
                    inMultiLineComment = false;
                    pos++;
                }
            }
        }
    }
}
