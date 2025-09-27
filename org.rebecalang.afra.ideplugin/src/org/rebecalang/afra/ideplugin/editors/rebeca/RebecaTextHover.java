package org.rebecalang.afra.ideplugin.editors.rebeca;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.EditorsUI;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RebecaTextHover implements ITextHover, ITextHoverExtension {
    
    private final RebecaEditor editor;
    
    public RebecaTextHover(RebecaEditor editor) {
        this.editor = editor;
    }
    
    @Override
    public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
        try {
            IDocument document = textViewer.getDocument();
            String hoveredText = document.get(hoverRegion.getOffset(), hoverRegion.getLength());
            
            HoverContext context = analyzeHoverContext(document, hoverRegion.getOffset(), hoveredText);
            
            if (context != null) {
                return generateHoverInfo(document, context);
            }
            
        } catch (BadLocationException e) {
        }
        
        return null;
    }
    
    @Override
    public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
        try {
            IDocument document = textViewer.getDocument();
            
            int start = offset;
            int end = offset;
            
            while (start > 0) {
                char c = document.getChar(start - 1);
                if (!Character.isJavaIdentifierPart(c)) {
                    break;
                }
                start--;
            }
            
            int docLength = document.getLength();
            while (end < docLength) {
                char c = document.getChar(end);
                if (!Character.isJavaIdentifierPart(c)) {
                    break;
                }
                end++;
            }
            
            if (end > start) {
                return new Region(start, end - start);
            }
            
        } catch (BadLocationException e) {
        }
        
        return null;
    }

    private HoverContext analyzeHoverContext(IDocument document, int offset, String hoveredText) {
        try {
            int lineNumber = document.getLineOfOffset(offset);
            int lineOffset = document.getLineOffset(lineNumber);
            int lineLength = document.getLineLength(lineNumber);
            String line = document.get(lineOffset, lineLength);
            
            // check if this is a method call (pattern: identifier.methodName or self.methodName)
            Pattern methodCallPattern = Pattern.compile("(\\w+|self)\\s*\\.\\s*" + Pattern.quote(hoveredText) + "\\s*\\(");
            Matcher methodMatcher = methodCallPattern.matcher(line);
            if (methodMatcher.find()) {
                return new HoverContext(HoverType.METHOD_CALL, hoveredText);
            }
                    
            
            // pattern: ClassName identifier(...):(...); (Rebeca class instantiation in main)
            Pattern rebecaClassPattern = Pattern.compile(
                "\\b" + Pattern.quote(hoveredText) + "\\s+\\w+\\s*\\([^)]*\\)\\s*:\\s*\\([^)]*\\)\\s*;"
            );
            if (rebecaClassPattern.matcher(line).find()) {
                return new HoverContext(HoverType.CLASS_USAGE, hoveredText);
            }
            
            // pattern: ClassName identifier (general class usage)
            Pattern classUsagePattern = Pattern.compile(
                "\\b" + Pattern.quote(hoveredText) + "\\s+\\w+\\b"
            );
            if (classUsagePattern.matcher(line).find() && !line.trim().startsWith("reactiveclass")) {
                return new HoverContext(HoverType.CLASS_USAGE, hoveredText);
            }
            
        } catch (BadLocationException e) {
        }
        
        return null;
    }
    
    private String generateHoverInfo(IDocument document, HoverContext context) {
        try {
            String documentText = document.get();
            
            if (context.type == HoverType.METHOD_CALL) {
                return findMethodSignatureAndDoc(documentText, context.elementName);
            } else if (context.type == HoverType.CLASS_USAGE) {
                return findClassSignatureAndDoc(documentText, context.elementName);
            }
            
        } catch (Exception e) {
        }
        
        return null;
    }
    
    private String findMethodSignatureAndDoc(String documentText, String methodName) {
        Pattern methodSignaturePattern = Pattern.compile(
            "msgsrv\\s+" + Pattern.quote(methodName) + "\\s*\\([^)]*\\)"
        );
        
        Matcher signatureMatcher = methodSignaturePattern.matcher(documentText);
        if (signatureMatcher.find()) {
            String signature = signatureMatcher.group(0).replaceAll("\\s+", " ");
            String formattedSignature = formatSignature(signature);
            
            int braceStart = documentText.indexOf('{', signatureMatcher.end());
            if (braceStart != -1) {
                int braceEnd = findMatchingBrace(documentText, braceStart);
                if (braceEnd != -1) {
                    String methodBody = documentText.substring(braceStart + 1, braceEnd);
                    
                    // PATTERN: /** */
                    Pattern docPattern = Pattern.compile("^\\s*/\\*\\*\\s*(.*?)\\s*\\*/", Pattern.DOTALL);
                    Matcher docMatcher = docPattern.matcher(methodBody);
                    
                    StringBuilder hoverInfo = new StringBuilder();
                    hoverInfo.append("<html><body>");
                    hoverInfo.append("<div style=\"font-family: monospace;\">").append(formattedSignature).append("</div>");
                    
                    if (docMatcher.find()) {
                        String documentation = docMatcher.group(1);
                        String cleanDoc = cleanDocumentation(documentation);
                        if (!cleanDoc.isEmpty()) {
                            hoverInfo.append("<br><br>").append(cleanDoc);
                        }
                    }
                    
                    hoverInfo.append("</body></html>");
                    return hoverInfo.toString();
                }
            }
            
            return "<html><body><div style=\"font-family: monospace;\">" + formattedSignature + "</div></body></html>";
        }
        
        return null;
    }
    
    private String findClassSignatureAndDoc(String documentText, String className) {
        Pattern classSignaturePattern = Pattern.compile(
            "reactiveclass\\s+" + Pattern.quote(className) + "\\s*\\([^)]*\\)"
        );
        
        Matcher signatureMatcher = classSignaturePattern.matcher(documentText);
        if (signatureMatcher.find()) {
            String signature = signatureMatcher.group(0).replaceAll("\\s+", " ");
            String formattedSignature = formatSignature(signature);
            
            int braceStart = documentText.indexOf('{', signatureMatcher.end());
            if (braceStart != -1) {
                int braceEnd = findMatchingBrace(documentText, braceStart);
                if (braceEnd != -1) {
                    String classBody = documentText.substring(braceStart + 1, braceEnd);
                    
                    Pattern docPattern = Pattern.compile("^\\s*/\\*\\*\\s*(.*?)\\s*\\*/", Pattern.DOTALL);
                    Matcher docMatcher = docPattern.matcher(classBody);
                    
                    StringBuilder hoverInfo = new StringBuilder();
                    hoverInfo.append("<html><body>");
                    hoverInfo.append("<div style=\"font-family: monospace;\">").append(formattedSignature).append("</div>");
                    
                    if (docMatcher.find()) {
                        String documentation = docMatcher.group(1);
                        String cleanDoc = cleanDocumentation(documentation);
                        if (!cleanDoc.isEmpty()) {
                            hoverInfo.append("<br><br>").append(cleanDoc);
                        }
                    }
                    
                    hoverInfo.append("</body></html>");
                    return hoverInfo.toString();
                }
            }
            
            return "<html><body><div style=\"font-family: monospace;\">" + formattedSignature + "</div></body></html>";
        }
        
        return null;
    }
    
    private int findMatchingBrace(String text, int openBraceIndex) {
        if (openBraceIndex >= text.length() || text.charAt(openBraceIndex) != '{') {
            return -1;
        }
        
        int braceCount = 1;
        int index = openBraceIndex + 1;
        
        while (index < text.length() && braceCount > 0) {
            char c = text.charAt(index);
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
            }
            index++;
        }
        
        return braceCount == 0 ? index - 1 : -1;
    }
  
    
    private String cleanDocumentation(String documentation) {
        if (documentation == null) {
            return "";
        }
        
        String cleaned = documentation.trim();
        cleaned = cleaned.replaceAll("(?m)^\\s*\\*\\s*", "");
        cleaned = cleaned.replaceAll("\\r?\\n", "<br>");
        cleaned = cleaned.replaceAll("[ \\t]+", " ");
        // format @something text as bold
        cleaned = cleaned.replaceAll("(@\\w+)", "<b>$1</b>");

        return cleaned;
    }
    
    private String formatSignature(String signature) {
        if (signature == null) {
            return "";
        }
        
        String formatted = signature.replaceAll("&", "&amp;")
                                   .replaceAll("<", "&lt;")
                                   .replaceAll(">", "&gt;");
        
        Pattern paramPattern = Pattern.compile("\\(([^)]*)\\)");
        Matcher paramMatcher = paramPattern.matcher(formatted);
        
        if (paramMatcher.find()) {
            String params = paramMatcher.group(1).trim();
            if (!params.isEmpty()) {
                String[] paramArray = params.split(",");
                StringBuilder boldParams = new StringBuilder();
                
                for (int i = 0; i < paramArray.length; i++) {
                    String param = paramArray[i].trim();
                    if (!param.isEmpty()) {
                        boldParams.append("<b>").append(param).append("</b>");
                        if (i < paramArray.length - 1) {
                            boldParams.append(", ");
                        }
                    }
                }
                
                formatted = paramMatcher.replaceFirst("(" + boldParams.toString() + ")");
            }
        }
        
        return formatted;
    }
    
    @Override
    public IInformationControlCreator getHoverControlCreator() {
        return new IInformationControlCreator() {
            @Override
            public IInformationControl createInformationControl(Shell parent) {
                return new DefaultInformationControl(parent, EditorsUI.getTooltipAffordanceString());
            }
        };
    }
    

    private static class HoverContext {
        final HoverType type;
        final String elementName;
        
        public HoverContext(HoverType type, String elementName) {
            this.type = type;
            this.elementName = elementName;
        }
    }
    

    private enum HoverType {
        METHOD_CALL,
        CLASS_USAGE
    }
}
