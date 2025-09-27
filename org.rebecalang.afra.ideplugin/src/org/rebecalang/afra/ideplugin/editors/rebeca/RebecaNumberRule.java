package org.rebecalang.afra.ideplugin.editors.rebeca;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

public class RebecaNumberRule implements IRule {
    
    private IToken numberToken;
    
    public RebecaNumberRule(IToken numberToken) {
        this.numberToken = numberToken;
    }
    
    @Override
    public IToken evaluate(ICharacterScanner scanner) {
        int c = scanner.read();
        
        if (!Character.isDigit(c)) {
            scanner.unread();
            return Token.UNDEFINED;
        }
        
        if (c == '0') {
            int next = scanner.read();
            if (next == 'x' || next == 'X') {
                return scanHexadecimal(scanner);
            } else if (next == 'b' || next == 'B') {
                return scanBinary(scanner);
            } else {
                scanner.unread();
            }
        }
        return scanDecimal(scanner);
    }
    
    private IToken scanDecimal(ICharacterScanner scanner) {
        int c = scanner.read();
        while (Character.isDigit(c)) {
            c = scanner.read();
        }
        
        if (c == '.') {
            int next = scanner.read();
            if (Character.isDigit(next)) {
                while (Character.isDigit(next)) {
                    next = scanner.read();
                }
                scanner.unread();
                return numberToken;
            } else {
                scanner.unread();
                scanner.unread();
                return numberToken;
            }
        } else {
            scanner.unread();
            return numberToken;
        }
    }
    
    private IToken scanHexadecimal(ICharacterScanner scanner) {
        int c = scanner.read();
        boolean hasDigits = false;
        
        while (Character.isDigit(c) || 
               (c >= 'a' && c <= 'f') || 
               (c >= 'A' && c <= 'F')) {
            hasDigits = true;
            c = scanner.read();
        }
        
        scanner.unread();
        
        if (hasDigits) {
            return numberToken;
        } else {
            scanner.unread();
            scanner.unread();
            return Token.UNDEFINED;
        }
    }
    
    private IToken scanBinary(ICharacterScanner scanner) {
        int c = scanner.read();
        boolean hasDigits = false;
        
        while (c == '0' || c == '1') {
            hasDigits = true;
            c = scanner.read();
        }
        
        scanner.unread();
        
        if (hasDigits) {
            return numberToken;
        } else {
            scanner.unread();
            scanner.unread();
            return Token.UNDEFINED;
        }
    }
}
