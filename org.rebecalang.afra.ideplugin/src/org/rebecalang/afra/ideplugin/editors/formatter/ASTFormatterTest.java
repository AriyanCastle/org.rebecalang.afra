package org.rebecalang.afra.ideplugin.editors.formatter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.jface.text.Document;

/**
 * Test class for the AST-based formatter.
 * This class demonstrates how to use the formatter and tests it with sample files.
 */
public class ASTFormatterTest {
    
    public static void main(String[] args) {
        ASTFormatterTest test = new ASTFormatterTest();
        
        System.out.println("=== AST-Based Formatter Test ===\n");
        
        // Test with sample Rebeca files
        test.testRebecaFormatting();
        
        // Test with sample property files
        test.testPropertyFormatting();
        
        // Test comment handling
        test.testCommentHandling();
        
        System.out.println("=== Test Complete ===");
    }
    
    /**
     * Test Rebeca file formatting
     */
    public void testRebecaFormatting() {
        System.out.println("Testing Rebeca file formatting...\n");
        
        String[] testFiles = {
            "samples/DiningPhilosophers.rebeca",
            "samples/LeaderElection.rebeca",
            "samples/ToxicGasLevel.rebeca"
        };
        
        ASTBasedFormatter formatter = new ASTBasedFormatter();
        
        for (String filename : testFiles) {
            try {
                System.out.println("--- Formatting: " + filename + " ---");
                
                // Read the original file
                String originalContent = readFileContent(filename);
                if (originalContent == null) {
                    System.out.println("Could not read file: " + filename);
                    continue;
                }
                
                // Format using AST formatter
                Document doc = new Document(originalContent);
                String formattedContent = formatter.format(doc);
                
                // Display results
                System.out.println("Original length: " + originalContent.length());
                System.out.println("Formatted length: " + formattedContent.length());
                System.out.println("First 200 chars of formatted output:");
                System.out.println(formattedContent.substring(0, Math.min(200, formattedContent.length())));
                System.out.println("...\n");
                
            } catch (Exception e) {
                System.out.println("Error formatting " + filename + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Test property file formatting
     */
    public void testPropertyFormatting() {
        System.out.println("Testing property file formatting...\n");
        
        String[] testFiles = {
            "samples/DiningPhilosophers.property",
            "samples/LeaderElection.property",
            "samples/ToxicGasLevel.property"
        };
        
        ASTBasedFormatter formatter = new ASTBasedFormatter();
        
        for (String filename : testFiles) {
            try {
                System.out.println("--- Formatting: " + filename + " ---");
                
                // Read the original file
                String originalContent = readFileContent(filename);
                if (originalContent == null) {
                    System.out.println("Could not read file: " + filename);
                    continue;
                }
                
                // Format using AST formatter
                Document doc = new Document(originalContent);
                String formattedContent = formatter.format(doc);
                
                // Display results
                System.out.println("Original:");
                System.out.println(originalContent);
                System.out.println("\nFormatted:");
                System.out.println(formattedContent);
                System.out.println("---\n");
                
            } catch (Exception e) {
                System.out.println("Error formatting " + filename + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Test comment handling
     */
    public void testCommentHandling() {
        System.out.println("Testing comment handling...\n");
        
        // Test with the comment-heavy sample file
        String filename = "samples/DiningPhilosophers-hint-comment.rebeca";
        
        ASTBasedFormatter formatter = new ASTBasedFormatter();
        
        try {
            System.out.println("--- Testing comments in: " + filename + " ---");
            
            // Read the original file
            String originalContent = readFileContent(filename);
            if (originalContent == null) {
                System.out.println("Could not read file: " + filename);
                return;
            }
            
            // Format using AST formatter
            Document doc = new Document(originalContent);
            String formattedContent = formatter.format(doc);
            
            // Count comments in original vs formatted
            int originalSingleComments = countOccurrences(originalContent, "//");
            int originalMultiComments = countOccurrences(originalContent, "/*");
            int originalDocComments = countOccurrences(originalContent, "/**");
            
            int formattedSingleComments = countOccurrences(formattedContent, "//");
            int formattedMultiComments = countOccurrences(formattedContent, "/*");
            int formattedDocComments = countOccurrences(formattedContent, "/**");
            
            System.out.println("Comment preservation check:");
            System.out.println("Single-line comments: " + originalSingleComments + " -> " + formattedSingleComments);
            System.out.println("Multi-line comments: " + originalMultiComments + " -> " + formattedMultiComments);
            System.out.println("Doc comments: " + originalDocComments + " -> " + formattedDocComments);
            
            System.out.println("\nFormatted output preview:");
            System.out.println(formattedContent.substring(0, Math.min(500, formattedContent.length())));
            System.out.println("...\n");
            
        } catch (Exception e) {
            System.out.println("Error testing comments: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Read file content
     */
    private String readFileContent(String filename) {
        try {
            return new String(Files.readAllBytes(Paths.get(filename)));
        } catch (IOException e) {
            System.out.println("Could not read file " + filename + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Count occurrences of a substring
     */
    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
    
    /**
     * Test with unformatted code to see improvement
     */
    public void testUnformattedCode() {
        System.out.println("Testing with poorly formatted code...\n");
        
        String unformattedCode = 
            "reactiveclass Test(5){knownrebecs{Node n1,n2;}statevars{int x;boolean flag;}" +
            "Test(){x=0;flag=false;self.start();}msgsrv start(){if(x<10){x=x+1;n1.notify(x);}else{flag=true;}}}" +
            "main{Test t(n1,n2):();Node n1():();Node n2():();}";
        
        ASTBasedFormatter formatter = new ASTBasedFormatter();
        
        try {
            System.out.println("--- Unformatted input ---");
            System.out.println(unformattedCode);
            
            Document doc = new Document(unformattedCode);
            String formatted = formatter.format(doc);
            
            System.out.println("\n--- Formatted output ---");
            System.out.println(formatted);
            
        } catch (Exception e) {
            System.out.println("Error formatting unformatted code: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
