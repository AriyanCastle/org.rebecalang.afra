package org.rebecalang.afra.ideplugin.editors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * Utility class for word detection and highlighting functionality.
 * Provides methods to find words in text while respecting various separators.
 */
public class WordHighlightingUtil {
	
	// Keywords that should be excluded from word highlighting
	private static final Set<String> REBECA_KEYWORDS = new HashSet<>();
	private static final Set<String> REBECA_TYPES = new HashSet<>();
	private static final Set<String> REBECA_BUILTINS = new HashSet<>();
	
	static {
		// Initialize keywords from RebecaScanner
		String[] keywords = {"reactiveclass", "if", "else", "msgsrv", "knownrebecs", 
							"statevars", "main", "self", "sender", "externalclass", 
							"sends", "of", "globalvariables"};
		for (String keyword : keywords) {
			REBECA_KEYWORDS.add(keyword);
		}
		
		// Initialize types
		String[] types = {"boolean", "byte", "short", "int", "bitint"};
		for (String type : types) {
			REBECA_TYPES.add(type);
		}
		
		// Initialize built-ins
		String[] builtins = {"pow", "after", "deadline", "delay", "true", "false"};
		for (String builtin : builtins) {
			REBECA_BUILTINS.add(builtin);
		}
	}
	
	// Pattern to match words: letters, digits, underscore, but not starting with digit
	private static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");
	
	// Word separators include: spaces, tabs, newlines, punctuation, operators
	private static final Pattern SEPARATOR_PATTERN = Pattern.compile("[\\s\\.,;:{}()\\[\\]<>+=\\-*/%&|!~^?]+");
	
	/**
	 * Finds the word at the given offset in the document.
	 * 
	 * @param document The document to search in
	 * @param offset The character offset
	 * @return The word region, or null if no word found
	 */
	public static IRegion findWordAt(IDocument document, int offset) {
		try {
			if (offset < 0 || offset >= document.getLength()) {
				return null;
			}
			
			// Find the line containing the offset
			int lineNumber = document.getLineOfOffset(offset);
			IRegion lineInfo = document.getLineInformation(lineNumber);
			String line = document.get(lineInfo.getOffset(), lineInfo.getLength());
			
			// Calculate position within the line
			int positionInLine = offset - lineInfo.getOffset();
			
			// Find word boundaries around the cursor position
			int wordStart = findWordStart(line, positionInLine);
			int wordEnd = findWordEnd(line, positionInLine);
			
			if (wordStart >= 0 && wordEnd > wordStart) {
				String word = line.substring(wordStart, wordEnd);
				
				// Check if it's a valid word (not a keyword)
				if (isValidWordForHighlighting(word)) {
					return new Region(lineInfo.getOffset() + wordStart, wordEnd - wordStart);
				}
			}
			
		} catch (BadLocationException e) {
			// Handle exception silently
		}
		
		return null;
	}
	
	/**
	 * Finds all occurrences of the given word in the document.
	 * 
	 * @param document The document to search in
	 * @param word The word to find
	 * @return List of regions containing the word
	 */
	public static List<IRegion> findAllOccurrences(IDocument document, String word) {
		List<IRegion> occurrences = new ArrayList<>();
		
		if (word == null || word.trim().isEmpty() || !isValidWordForHighlighting(word)) {
			return occurrences;
		}
		
		try {
			String content = document.get();
			Matcher matcher = WORD_PATTERN.matcher(content);
			
			while (matcher.find()) {
				String foundWord = matcher.group();
				if (word.equals(foundWord)) {
					occurrences.add(new Region(matcher.start(), matcher.end() - matcher.start()));
				}
			}
			
		} catch (Exception e) {
			// Handle exception silently
		}
		
		return occurrences;
	}
	
	/**
	 * Extracts the word from a region in the document.
	 * 
	 * @param document The document
	 * @param region The region containing the word
	 * @return The word as a string, or null if extraction fails
	 */
	public static String getWordFromRegion(IDocument document, IRegion region) {
		try {
			return document.get(region.getOffset(), region.getLength());
		} catch (BadLocationException e) {
			return null;
		}
	}
	
	/**
	 * Finds the start of a word at the given position in the line.
	 */
	private static int findWordStart(String line, int position) {
		if (position >= line.length()) {
			position = line.length() - 1;
		}
		
		// Move backwards to find word start
		while (position >= 0 && isWordCharacter(line.charAt(position))) {
			position--;
		}
		
		// Move forward to the first word character
		position++;
		
		// Make sure we start with a valid word start character
		if (position < line.length() && isValidWordStart(line.charAt(position))) {
			return position;
		}
		
		return -1;
	}
	
	/**
	 * Finds the end of a word at the given position in the line.
	 */
	private static int findWordEnd(String line, int position) {
		if (position >= line.length()) {
			return line.length();
		}
		
		// If we're not on a word character, find the next word
		if (!isWordCharacter(line.charAt(position))) {
			while (position < line.length() && !isWordCharacter(line.charAt(position))) {
				position++;
			}
		}
		
		// Move forward to find word end
		while (position < line.length() && isWordCharacter(line.charAt(position))) {
			position++;
		}
		
		return position;
	}
	
	/**
	 * Checks if a character is part of a word.
	 */
	private static boolean isWordCharacter(char c) {
		return Character.isLetterOrDigit(c) || c == '_';
	}
	
	/**
	 * Checks if a character can start a word.
	 */
	private static boolean isValidWordStart(char c) {
		return Character.isLetter(c) || c == '_';
	}
	
	/**
	 * Checks if a word should be highlighted (not a keyword, type, or built-in).
	 */
	private static boolean isValidWordForHighlighting(String word) {
		if (word == null || word.trim().isEmpty()) {
			return false;
		}
		
		// Don't highlight keywords, types, or built-ins
		return !REBECA_KEYWORDS.contains(word) && 
			   !REBECA_TYPES.contains(word) && 
			   !REBECA_BUILTINS.contains(word);
	}
}
