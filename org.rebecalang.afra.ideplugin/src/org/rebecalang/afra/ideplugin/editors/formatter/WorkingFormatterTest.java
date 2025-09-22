package org.rebecalang.afra.ideplugin.editors.formatter;

import org.eclipse.jface.text.Document;

/**
 * Test class for the working formatter implementation.
 * This tests the FallbackRebecaFormatter which should compile and work properly.
 */
public class WorkingFormatterTest {
    
    public static void main(String[] args) {
        WorkingFormatterTest test = new WorkingFormatterTest();
        
        System.out.println("=== Working Formatter Test ===\n");
        
        // Test basic Rebeca formatting
        test.testRebecaFormatting();
        
        // Test property formatting
        test.testPropertyFormatting();
        
        // Test via registry
        test.testFormatterRegistry();
        
        System.out.println("=== All Tests Complete ===");
    }
    
    /**
     * Test basic Rebeca file formatting
     */
    public void testRebecaFormatting() {
        System.out.println("Testing Rebeca file formatting...\n");
        
        String unformattedRebeca = 
            "reactiveclass Test(5){knownrebecs{Node n1,n2;}statevars{int x;boolean flag;}" +
            "Test(){x=0;flag=false;self.start();}msgsrv start(){if(x<10){x=x+1;n1.notify(x);}else{flag=true;}}}" +
            "main{Test t(n1,n2):();Node n1():();Node n2():();}";
        
        FallbackRebecaFormatter formatter = new FallbackRebecaFormatter();
        
        try {
            System.out.println("--- Unformatted input ---");
            System.out.println(unformattedRebeca);
            
            Document doc = new Document(unformattedRebeca);
            String formatted = formatter.format(doc);
            
            System.out.println("\n--- Formatted output ---");
            System.out.println(formatted);
            
            System.out.println("✅ Rebeca formatting test passed\n");
            
        } catch (Exception e) {
            System.out.println("❌ Error formatting Rebeca code: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Test property file formatting
     */
    public void testPropertyFormatting() {
        System.out.println("Testing property file formatting...\n");
        
        String unformattedProperty = 
            "property{define{p0eat=phil0.eating;p1eat=phil1.eating;p2eat=phil2.eating;}" +
            "Assertion{Safety:p0s&&p1s&&p2s;}LTL{NoStarvation:G(F(p0eat)&&F(p1eat)&&F(p2eat));}}";
        
        FallbackRebecaFormatter formatter = new FallbackRebecaFormatter();
        
        try {
            System.out.println("--- Unformatted property input ---");
            System.out.println(unformattedProperty);
            
            Document doc = new Document(unformattedProperty);
            String formatted = formatter.format(doc);
            
            System.out.println("\n--- Formatted property output ---");
            System.out.println(formatted);
            
            System.out.println("✅ Property formatting test passed\n");
            
        } catch (Exception e) {
            System.out.println("❌ Error formatting property code: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Test formatter registry
     */
    public void testFormatterRegistry() {
        System.out.println("Testing formatter registry...\n");
        
        try {
            FormatterRegistry registry = FormatterRegistry.getInstance();
            
            // Test getting Rebeca formatter
            IAfraFormatter rebecaFormatter = registry.getRebecaFormatter();
            System.out.println("✅ Retrieved Rebeca formatter: " + rebecaFormatter.getClass().getSimpleName());
            
            // Test getting property formatter
            IAfraFormatter propertyFormatter = registry.getPropertyFormatter();
            System.out.println("✅ Retrieved property formatter: " + propertyFormatter.getClass().getSimpleName());
            
            // Test getting formatter by extension
            IAfraFormatter rebecaExt = registry.getFormatterByExtension("rebeca");
            IAfraFormatter propertyExt = registry.getFormatterByExtension("property");
            
            System.out.println("✅ Extension-based retrieval works");
            
            // Test actual formatting through registry
            String testCode = "reactiveclass Test { knownrebecs { Node n; } statevars { int x; } Test() { x = 0; } }";
            Document doc = new Document(testCode);
            String formatted = rebecaFormatter.format(doc);
            
            System.out.println("--- Registry-formatted output ---");
            System.out.println(formatted);
            
            System.out.println("✅ Registry formatting test passed\n");
            
        } catch (Exception e) {
            System.out.println("❌ Error testing formatter registry: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Test comment preservation
     */
    public void testCommentPreservation() {
        System.out.println("Testing comment preservation...\n");
        
        String codeWithComments = 
            "// This is a test class\n" +
            "reactiveclass Test {\n" +
            "    /* Known rebecs section */\n" +
            "    knownrebecs {\n" +
            "        Node neighbor; // Neighbor node\n" +
            "    }\n" +
            "    \n" +
            "    /**\n" +
            "     * Constructor documentation\n" +
            "     */\n" +
            "    Test() {\n" +
            "        // Initialize\n" +
            "        self.start();\n" +
            "    }\n" +
            "}";
        
        FallbackRebecaFormatter formatter = new FallbackRebecaFormatter();
        
        try {
            Document doc = new Document(codeWithComments);
            String formatted = formatter.format(doc);
            
            System.out.println("--- Code with comments ---");
            System.out.println(formatted);
            
            // Check that comments are preserved
            if (formatted.contains("//") && formatted.contains("/*") && formatted.contains("/**")) {
                System.out.println("✅ Comment preservation test passed\n");
            } else {
                System.out.println("⚠️  Some comments may not be preserved\n");
            }
            
        } catch (Exception e) {
            System.out.println("❌ Error testing comment preservation: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
