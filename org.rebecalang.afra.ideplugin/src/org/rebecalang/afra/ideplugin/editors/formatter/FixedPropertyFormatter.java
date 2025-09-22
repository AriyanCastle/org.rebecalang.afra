package org.rebecalang.afra.ideplugin.editors.formatter;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * Fixed formatter for Property (.property) files that correctly handles comments
 */
public class FixedPropertyFormatter implements IAfraFormatter {
    
    private static final String NEW_LINE = System.getProperty("line.separator");
    private String lastIndent = "\t";
    
    @Override
    public String format(IDocument document) {
        try {
            String content = document.get();
            return formatContent(content);
        } catch (Exception e) {
            e.printStackTrace();
            return document.get();
        }
    }
    
    @Override
    public String format(IDocument document, IRegion region) {
        try {
            String content = document.get(region.getOffset(), region.getLength());
            return formatContent(content);
        } catch (BadLocationException e) {
            e.printStackTrace();
            return document.get();
        }
    }
    
    @Override
    public String getIndentString() {
        return lastIndent;
    }
    
    private String formatContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }
        PropertyFormatterEngine engine = new PropertyFormatterEngine(lastIndent, NEW_LINE);
        String result = engine.format(content);
        lastIndent = engine.getIndentUnit();
        return result;
    }
}
