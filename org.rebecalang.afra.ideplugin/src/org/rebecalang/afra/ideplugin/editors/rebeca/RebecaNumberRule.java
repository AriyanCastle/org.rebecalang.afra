package org.rebecalang.afra.ideplugin.editors.rebeca;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/**
 * Rule for highlighting numeric literals in Rebeca code.
 * Supports:
 * - Integer literals: 123, 0, 42
 * - Decimal literals: 3.14, 0.5, 123.456
 * - Hexadecimal literals: 0xFF, 0x1A2B
 * - Binary literals: 0b101, 0b1010
 */
public class RebecaNumberRule implements IRule {
    
    private IToken numberToken;
    
    public RebecaNumberRule(IToken numberToken) {
        this.numberToken = numberToken;
    }
    
    @Override
    public IToken evaluate(ICharacterScanner scanner) {
        int c = scanner.read();
        
        // Check if we're starting with a digit
        if (!Character.isDigit(c)) {
            scanner.unread();
            return Token.UNDEFINED;
        }
        
        // Handle special prefixes for hex (0x) and binary (0b)
        if (c == '0') {
            int next = scanner.read();
            if (next == 'x' || next == 'X') {
                // Hexadecimal number
                return scanHexadecimal(scanner);
            } else if (next == 'b' || next == 'B') {
                // Binary number
                return scanBinary(scanner);
            } else {
                // Put back the character and continue with decimal scanning
                scanner.unread();
            }
        }
        
        // Scan decimal number (integer or floating-point)
        return scanDecimal(scanner);
    }
    
    private IToken scanDecimal(ICharacterScanner scanner) {
        // Continue reading digits
        int c = scanner.read();
        while (Character.isDigit(c)) {
            c = scanner.read();
        }
        
        // Check for decimal point
        if (c == '.') {
            int next = scanner.read();
            if (Character.isDigit(next)) {
                // Read remaining decimal digits
                while (Character.isDigit(next)) {
                    next = scanner.read();
                }
                scanner.unread(); // Put back the non-digit character
                return numberToken;
            } else {
                // Not a decimal number, put back both characters
                scanner.unread();
                scanner.unread();
                return numberToken;
            }
        } else {
            // Integer number, put back the non-digit character
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
        
        scanner.unread(); // Put back the non-hex character
        
        if (hasDigits) {
            return numberToken;
        } else {
            // Invalid hex number, backtrack
            scanner.unread(); // Put back 'x'
            scanner.unread(); // Put back '0'
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
        
        scanner.unread(); // Put back the non-binary character
        
        if (hasDigits) {
            return numberToken;
        } else {
            // Invalid binary number, backtrack
            scanner.unread(); // Put back 'b'
            scanner.unread(); // Put back '0'
            return Token.UNDEFINED;
        }
    }
}
