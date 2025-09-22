package org.rebecalang.afra.ideplugin.editors.formatter;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

/**
 * Simple test class to verify AST formatter functionality
 * This is not a JUnit test but a simple main method for manual testing
 */
public class ASTFormatterTest {
    
    public static void main(String[] args) {
        testRebecaFormatter();
        testPropertyFormatter();
    }
    
    private static void testRebecaFormatter() {
        System.out.println("=== Testing Rebeca AST Formatter ===");
        
        String testRebecaCode = 
            "reactiveclass Test(5) {\n" +
            "knownrebecs {\n" +
            "Test other;\n" +
            "}\n" +
            "statevars {\n" +
            "boolean flag;\n" +
            "}\n" +
            "Test() {\n" +
            "flag = false;\n" +
            "}\n" +
            "msgsrv test() {\n" +
            "// This is a comment\n" +
            "flag = true;\n" +
            "}\n" +
            "}\n" +
            "main {\n" +
            "Test t1(t2):();\n" +
            "Test t2(t1):();\n" +
            "}\n";
        
        try {
            ASTRebecaFormatter formatter = new ASTRebecaFormatter();
            IDocument document = new Document(testRebecaCode);
            String formatted = formatter.format(document);
            
            System.out.println("Original:");
            System.out.println(testRebecaCode);
            System.out.println("\nFormatted:");
            System.out.println(formatted);
            
        } catch (Exception e) {
            System.err.println("Rebeca formatter test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testPropertyFormatter() {
        System.out.println("\n=== Testing Property AST Formatter ===");
        
        String testPropertyCode = 
            "property {\n" +
            "define {\n" +
            "test = true;\n" +
            "}\n" +
            "Assertion {\n" +
            "Safety: test;\n" +
            "}\n" +
            "}\n";
        
        try {
            ASTPropertyFormatter formatter = new ASTPropertyFormatter();
            IDocument document = new Document(testPropertyCode);
            String formatted = formatter.format(document);
            
            System.out.println("Original:");
            System.out.println(testPropertyCode);
            System.out.println("\nFormatted:");
            System.out.println(formatted);
            
        } catch (Exception e) {
            System.err.println("Property formatter test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
