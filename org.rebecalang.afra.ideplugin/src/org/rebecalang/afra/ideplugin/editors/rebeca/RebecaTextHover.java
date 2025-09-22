package org.rebecalang.afra.ideplugin.editors.rebeca;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension2;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.BadLocationException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides hover information for Rebeca code elements (methods and classes)
 */
public class RebecaTextHover implements ITextHover, ITextHoverExtension2 {
    
    private final RebecaEditor editor; // Reserved for future editor integration features
    
    public RebecaTextHover(RebecaEditor editor) {
        this.editor = editor;
    }
    
    @Override
    public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
        Object info = getHoverInfo2(textViewer, hoverRegion);
        return info instanceof String ? (String) info : null;
    }
    
    @Override
    public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion) {
        try {
            IDocument document = textViewer.getDocument();
            String hoveredText = document.get(hoverRegion.getOffset(), hoverRegion.getLength());
            
            // Determine what kind of element we're hovering over
            HoverContext context = analyzeHoverContext(document, hoverRegion.getOffset(), hoveredText);
            
            if (context != null) {
                return generateHoverInfo(document, context);
            }
            
        } catch (BadLocationException e) {
            // Return null on error
        }
        
        return null;
    }
    
    @Override
    public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
        try {
            IDocument document = textViewer.getDocument();
            
            // Find the word boundaries around the offset
            int start = offset;
            int end = offset;
            
            // Move start backwards to find word start
            while (start > 0) {
                char c = document.getChar(start - 1);
                if (!Character.isJavaIdentifierPart(c)) {
                    break;
                }
                start--;
            }
            
            // Move end forwards to find word end
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
            // Return null on error
        }
        
        return null;
    }
    
    /**
     * Analyzes the context around the hovered text to determine what kind of element it is
     */
    private HoverContext analyzeHoverContext(IDocument document, int offset, String hoveredText) {
        try {
            // Get the line containing the hover position
            int lineNumber = document.getLineOfOffset(offset);
            int lineOffset = document.getLineOffset(lineNumber);
            int lineLength = document.getLineLength(lineNumber);
            String line = document.get(lineOffset, lineLength);
            
            // Check if this is a method call (pattern: identifier.methodName or self.methodName)
            Pattern methodCallPattern = Pattern.compile("(\\w+|self)\\s*\\.\\s*" + Pattern.quote(hoveredText) + "\\s*\\(");
            Matcher methodMatcher = methodCallPattern.matcher(line);
            if (methodMatcher.find()) {
                return new HoverContext(HoverType.METHOD_CALL, hoveredText);
            }
            
            // Check if this is a class instantiation or usage
        
            
            // Pattern 1: ClassName identifier(...):(...); (Rebeca class instantiation in main)
            Pattern rebecaClassPattern = Pattern.compile(
                "\\b" + Pattern.quote(hoveredText) + "\\s+\\w+\\s*\\([^)]*\\)\\s*:\\s*\\([^)]*\\)\\s*;"
            );
            if (rebecaClassPattern.matcher(line).find()) {
                return new HoverContext(HoverType.CLASS_USAGE, hoveredText);
            }
            
            // Pattern 2: ClassName identifier (general class usage)
            Pattern classUsagePattern = Pattern.compile(
                "\\b" + Pattern.quote(hoveredText) + "\\s+\\w+\\b"
            );
            if (classUsagePattern.matcher(line).find() && !line.trim().startsWith("reactiveclass")) {
                // Make sure we're not matching the class declaration itself
                return new HoverContext(HoverType.CLASS_USAGE, hoveredText);
            }
            
        } catch (BadLocationException e) {
            // Handle error
        }
        
        return null;
    }
    
    /**
     * Generates hover information based on the context
     */
    private String generateHoverInfo(IDocument document, HoverContext context) {
        try {
            String documentText = document.get();
            
            if (context.type == HoverType.METHOD_CALL) {
                return findMethodSignatureAndDoc(documentText, context.elementName);
            } else if (context.type == HoverType.CLASS_USAGE) {
                return findClassSignatureAndDoc(documentText, context.elementName);
            }
            
        } catch (Exception e) {
            // Handle error
        }
        
        return null;
    }
    
    /**
     * Finds method signature and documentation
     */
    private String findMethodSignatureAndDoc(String documentText, String methodName) {
        // First, find the method signature
        Pattern methodSignaturePattern = Pattern.compile(
            "msgsrv\\s+" + Pattern.quote(methodName) + "\\s*\\([^)]*\\)"
        );
        
        Matcher signatureMatcher = methodSignaturePattern.matcher(documentText);
        if (signatureMatcher.find()) {
            String signature = signatureMatcher.group(0).replaceAll("\\s+", " ");
            String formattedSignature = formatSignature(signature);
            
            // Now look for documentation within the method body
            int braceStart = documentText.indexOf('{', signatureMatcher.end());
            if (braceStart != -1) {
                // Find the end of the method by matching braces
                int braceEnd = findMatchingBrace(documentText, braceStart);
                if (braceEnd != -1) {
                    String methodBody = documentText.substring(braceStart + 1, braceEnd);
                    
                    // Look for /** */ documentation at the beginning of the method body
                    Pattern docPattern = Pattern.compile("^\\s*/\\*\\*\\s*(.*?)\\s*\\*\\*/", Pattern.DOTALL);
                    Matcher docMatcher = docPattern.matcher(methodBody);
                    
                    StringBuilder hoverInfo = new StringBuilder();
                    hoverInfo.append(formattedSignature);
                    
                    if (docMatcher.find()) {
                        String documentation = docMatcher.group(1);
                        String cleanDoc = cleanDocumentation(documentation);
                        if (!cleanDoc.isEmpty()) {
                            hoverInfo.append("\n\n").append(cleanDoc);
                        }
                    }
                    
                    return hoverInfo.toString();
                }
            }
            
            // If no documentation found, just return the signature
            return formattedSignature;
        }
        
        return null;
    }
    
    /**
     * Finds class signature and documentation
     */
    private String findClassSignatureAndDoc(String documentText, String className) {
        // First, find the class signature
        Pattern classSignaturePattern = Pattern.compile(
            "reactiveclass\\s+" + Pattern.quote(className) + "\\s*\\([^)]*\\)"
        );
        
        Matcher signatureMatcher = classSignaturePattern.matcher(documentText);
        if (signatureMatcher.find()) {
            String signature = signatureMatcher.group(0).replaceAll("\\s+", " ");
            String formattedSignature = formatSignature(signature);
            
            // Now look for documentation within the class body
            int braceStart = documentText.indexOf('{', signatureMatcher.end());
            if (braceStart != -1) {
                // Find the end of the class by matching braces
                int braceEnd = findMatchingBrace(documentText, braceStart);
                if (braceEnd != -1) {
                    String classBody = documentText.substring(braceStart + 1, braceEnd);
                    
                    // Look for /** */ documentation at the beginning of the class body
                    Pattern docPattern = Pattern.compile("^\\s*/\\*\\*\\s*(.*?)\\s*\\*\\*/", Pattern.DOTALL);
                    Matcher docMatcher = docPattern.matcher(classBody);
                    
                    StringBuilder hoverInfo = new StringBuilder();
                    hoverInfo.append(formattedSignature);
                    
                    if (docMatcher.find()) {
                        String documentation = docMatcher.group(1);
                        String cleanDoc = cleanDocumentation(documentation);
                        if (!cleanDoc.isEmpty()) {
                            hoverInfo.append("\n\n").append(cleanDoc);
                        }
                    }
                    
                    return hoverInfo.toString();
                }
            }
            
            // If no documentation found, just return the signature
            return formattedSignature;
        }
        
        return null;
    }
    
    /**
     * Finds the matching closing brace for an opening brace
     */
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
    
    /**
     * Cleans up documentation comment formatting and adds semantic formatting
     */
    private String cleanDocumentation(String documentation) {
        if (documentation == null) {
            return "";
        }
        
        // Remove leading/trailing whitespace
        String cleaned = documentation.trim();
        
        // Remove asterisks from the beginning of lines (common in /** */ comments)
        cleaned = cleaned.replaceAll("(?m)^\\s*\\*\\s*", "");
        
        // Remove extra leading/trailing whitespace from lines but preserve line structure
        String[] lines = cleaned.split("\\r?\\n");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!line.isEmpty()) {
                result.append(line);
                if (i < lines.length - 1) {
                    result.append("\n");
                }
            }
        }
        
        cleaned = result.toString();
        
        // Format @something text to make it stand out using text formatting
        cleaned = cleaned.replaceAll("(@\\w+)", "**$1**");
        
        return cleaned;
    }
    
    /**
     * Formats method/class signatures to make parameters stand out
     */
    private String formatSignature(String signature) {
        if (signature == null) {
            return "";
        }
        
        // Find and format parameters within parentheses
        Pattern paramPattern = Pattern.compile("\\(([^)]*)\\)");
        Matcher paramMatcher = paramPattern.matcher(signature);
        
        if (paramMatcher.find()) {
            String params = paramMatcher.group(1).trim();
            if (!params.isEmpty()) {
                // Split parameters by comma and format them to stand out
                String[] paramArray = params.split(",");
                StringBuilder formattedParams = new StringBuilder();
                
                for (int i = 0; i < paramArray.length; i++) {
                    String param = paramArray[i].trim();
                    if (!param.isEmpty()) {
                        formattedParams.append("[").append(param).append("]");
                        if (i < paramArray.length - 1) {
                            formattedParams.append(", ");
                        }
                    }
                }
                
                return paramMatcher.replaceFirst("(" + formattedParams.toString() + ")");
            }
        }
        
        return signature;
    }
    
    /**
     * Inner class to hold hover context information
     */
    private static class HoverContext {
        final HoverType type;
        final String elementName;
        
        public HoverContext(HoverType type, String elementName) {
            this.type = type;
            this.elementName = elementName;
        }
    }
    
    /**
     * Enum for different types of hover contexts
     */
    private enum HoverType {
        METHOD_CALL,
        CLASS_USAGE
    }
}
