package org.rebecalang.afra.ideplugin.editors.rebecaprop;

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

public class RebecaPropReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {
    private RebecaPropEditor editor;
    private IDocument fDocument;
    protected final ArrayList<Position> fPositions = new ArrayList<Position>();
    protected int fRangeEnd;

    public RebecaPropEditor getEditor() {
        return this.editor;
    }

    public void setEditor(RebecaPropEditor editor) {
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
        ArrayList<Integer> openPositions = new ArrayList<Integer> ();
        for(int pos = 0; pos < fRangeEnd; pos += 1) {
            char ch = fDocument.getChar(pos);
            if(ch == '{') {
                openPositions.add(pos);
            }
            else if(ch == '}') {
                if(openPositions.size() > 0) {
                    int lastIndex = openPositions.size() - 1;
                    int lastOpen = (int) openPositions.get(lastIndex);
                    openPositions.remove(lastIndex);
                    emitPosition(lastOpen, pos - lastOpen);
                }
            }

        }
    }
}
