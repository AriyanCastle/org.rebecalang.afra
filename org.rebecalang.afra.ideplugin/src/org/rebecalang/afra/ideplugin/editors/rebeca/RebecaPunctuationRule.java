package org.rebecalang.afra.ideplugin.editors.rebeca;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/**
 * Rule for highlighting punctuation and delimiters in Rebeca code.
 * Supports:
 * - Braces: {, }
 * - Parentheses: (, )
 * - Brackets: [, ]
 * - Semicolons: ;
 * - Commas: ,
 * - Dots: .
 */
public class RebecaPunctuationRule implements IRule {
    
    private IToken punctuationToken;
    
    public RebecaPunctuationRule(IToken punctuationToken) {
        this.punctuationToken = punctuationToken;
    }
    
    @Override
    public IToken evaluate(ICharacterScanner scanner) {
        int c = scanner.read();
        
        switch (c) {
            case '{':
            case '}':
            case '(':
            case ')':
            case '[':
            case ']':
            case ';':
            case ',':
            case '.':
                return punctuationToken;
                
            default:
                scanner.unread();
                return Token.UNDEFINED;
        }
    }
}
