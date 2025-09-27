package org.rebecalang.afra.ideplugin.editors.rebeca;

public final class RebecaConstants {
    
    public static final String[] KEYWORDS = {
        "reactiveclass", "knownrebecs", "statevars", "msgsrv", "main",
        "if", "else", "switch", "case", "break", "default",
        "for", "while", "continue", "return", "assertion",
        "env", "extends", "abstract", "interface", "implements",
        "externalclass", "sends", "of", "globalvariables"
    };
    
    public static final String[] TYPES = {
        "boolean", "byte", "short", "int", "float", "void", "bitint"
    };
    
    public static final String[] BUILTINS = {
        "self", "sender", "true", "false", "null", "pow",
        "delay", "after", "deadline", "instanceof"
    };
    
    public static final String[] ANNOTATIONS = {
        "@priority", "@globalPriority"
    };
    
    public static final String[] PROPERTY_KEYWORDS = {
        "property", "define", "Assertion", "LTL", "CTL"
    };
    
    public static final String[] TEMPORAL_OPERATORS = {
        "G", "F", "X", "U", "R", "W", "M"
    };
    
    public static final String[] PROPERTY_LITERALS = {
        "true", "false", "Safety", "Liveness"
    };
    
    public static final String[] RESERVED = {
        "constructor", "finalize", "synchronized", "volatile", 
        "transient", "native", "strictfp", "const", "goto"
    };
    
    private RebecaConstants() {
    }
}