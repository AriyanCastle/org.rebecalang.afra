package org.rebecalang.afra.ideplugin.editors.rebeca;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

public class RebecaAnnotationRule implements IRule {
    
    private IToken annotationToken;
    
    public RebecaAnnotationRule(IToken annotationToken) {
        this.annotationToken = annotationToken;
    }
    
    @Override
    public IToken evaluate(ICharacterScanner scanner) {
        int c = scanner.read();
        
        if (c != '@') {
            scanner.unread();
            return Token.UNDEFINED;
        }
        
        StringBuilder annotationName = new StringBuilder();
        c = scanner.read();
        
        if (!Character.isLetter(c)) {
            scanner.unread();
            scanner.unread();
            return Token.UNDEFINED;
        }
        
        while (Character.isLetterOrDigit(c) || c == '_') {
            annotationName.append((char) c);
            c = scanner.read();
        }
        
        scanner.unread();
        
        String annotation = annotationName.toString();
        if (isValidAnnotation(annotation)) {
            return annotationToken;
        }
        
        for (int i = 0; i < annotation.length(); i++) {
            scanner.unread();
        }
        scanner.unread();
        
        return Token.UNDEFINED;
    }
    
    private boolean isValidAnnotation(String annotation) {
        for (String validAnnotation : RebecaConstants.ANNOTATIONS) {
            String annotationName = validAnnotation.substring(1);
            if (annotationName.equals(annotation)) {
                return true;
            }
        }
        return false;
    }
}
