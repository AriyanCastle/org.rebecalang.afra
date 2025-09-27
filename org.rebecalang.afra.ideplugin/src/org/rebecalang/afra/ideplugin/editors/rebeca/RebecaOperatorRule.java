package org.rebecalang.afra.ideplugin.editors.rebeca;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

public class RebecaOperatorRule implements IRule {
    
    private IToken operatorToken;
    
    public RebecaOperatorRule(IToken operatorToken) {
        this.operatorToken = operatorToken;
    }
    
    @Override
    public IToken evaluate(ICharacterScanner scanner) {
        int c = scanner.read();
        
        switch (c) {
            case '+':
            case '-':
            case '*':
            case '/':
            case '%':
            case '?':
            case ':':
                return operatorToken;
                
            case '=':
                int next = scanner.read();
                if (next == '=') {
                    return operatorToken;
                } else {
                    scanner.unread();
                    return operatorToken;
                }
                
            case '!':
                next = scanner.read();
                if (next == '=') {
                    return operatorToken;
                } else {
                    scanner.unread();
                    return operatorToken;
                }
                
            case '<':
                next = scanner.read();
                if (next == '=') {
                    return operatorToken;
                } else {
                    scanner.unread();
                    return operatorToken;
                }
                
            case '>':
                next = scanner.read();
                if (next == '=') {
                    return operatorToken;
                } else {
                    scanner.unread();
                    return operatorToken;
                }
                
            case '&':
                next = scanner.read();
                if (next == '&') {
                    return operatorToken;
                } else {
                    scanner.unread();
                    scanner.unread();
                    return Token.UNDEFINED;
                }
                
            case '|':
                next = scanner.read();
                if (next == '|') {
                    return operatorToken;
                } else {
                    scanner.unread();
                    scanner.unread();
                    return Token.UNDEFINED;
                }
                
            default:
                scanner.unread();
                return Token.UNDEFINED;
        }
    }
}
