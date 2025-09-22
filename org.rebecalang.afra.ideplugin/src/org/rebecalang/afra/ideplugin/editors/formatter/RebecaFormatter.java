package org.rebecalang.afra.ideplugin.editors.formatter;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * Formatter for Rebeca (.rebeca) files that correctly handles comments
 */
public class RebecaFormatter implements IAfraFormatter {
    
    @Override
    public String format(IDocument document) {
        try {
            String content = document.get();
            System.out.println("RebecaFormatter.format(IDocument document): " + content);
            return FormatterUtils.formatContent(content);
        } catch (Exception e) {
            e.printStackTrace();
            return document.get();
        }
    }
    
    @Override
    public String format(IDocument document, IRegion region) {
        try {
            String content = document.get(region.getOffset(), region.getLength());
            System.out.println("RebecaFormatter.format(IDocument document, IRegion region): " + content);
            return FormatterUtils.formatContent(content);
        } catch (BadLocationException e) {
            e.printStackTrace();
            return document.get();
        }
    }
    
    @Override
    public String getIndentString() {
        return FormatterUtils.getIndentString();
    }
}
