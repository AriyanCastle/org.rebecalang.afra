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
import org.rebecalang.afra.ideplugin.editors.rebeca.RebecaConstants;

public class WordHighlightingUtil {
	
	public enum FileType {
		REBECA, PROPERTY
	}
	
	private static final Set<String> REBECA_KEYWORDS = new HashSet<>();
	private static final Set<String> REBECA_TYPES = new HashSet<>();
	private static final Set<String> REBECA_BUILTINS = new HashSet<>();
	private static final Set<String> REBECA_ANNOTATIONS = new HashSet<>();
	private static final Set<String> REBECA_RESERVED = new HashSet<>();
	
	private static final Set<String> PROPERTY_KEYWORDS = new HashSet<>();
	private static final Set<String> PROPERTY_LITERALS = new HashSet<>();
	private static final Set<String> TEMPORAL_OPERATORS = new HashSet<>();
	
	static {
		for (String keyword : RebecaConstants.KEYWORDS) {
			REBECA_KEYWORDS.add(keyword);
		}
		for (String type : RebecaConstants.TYPES) {
			REBECA_TYPES.add(type);
		}
		for (String builtin : RebecaConstants.BUILTINS) {
			REBECA_BUILTINS.add(builtin);
		}
		for (String annotation : RebecaConstants.ANNOTATIONS) {
			REBECA_ANNOTATIONS.add(annotation);
		}
		for (String reserved : RebecaConstants.RESERVED) {
			REBECA_RESERVED.add(reserved);
		}
		
		for (String keyword : RebecaConstants.PROPERTY_KEYWORDS) {
			PROPERTY_KEYWORDS.add(keyword);
		}
		for (String literal : RebecaConstants.PROPERTY_LITERALS) {
			PROPERTY_LITERALS.add(literal);
		}
		for (String operator : RebecaConstants.TEMPORAL_OPERATORS) {
			TEMPORAL_OPERATORS.add(operator);
		}
	}
	
	private static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-Z_@][a-zA-Z0-9_@]*");
	
	public static IRegion findWordAt(IDocument document, int offset, FileType fileType) {
		try {
			if (offset < 0 || offset >= document.getLength()) {
				return null;
			}
			
			int lineNumber = document.getLineOfOffset(offset);
			IRegion lineInfo = document.getLineInformation(lineNumber);
			String line = document.get(lineInfo.getOffset(), lineInfo.getLength());
			
			int positionInLine = offset - lineInfo.getOffset();
			int wordStart = findWordStart(line, positionInLine);
			int wordEnd = findWordEnd(line, positionInLine);
			
			if (wordStart >= 0 && wordEnd > wordStart) {
				String word = line.substring(wordStart, wordEnd);
				if (isValidWordForHighlighting(word, fileType) && 
					!isInComment(document, lineInfo.getOffset() + wordStart) && 
					!isAnnotation(word)) {
					return new Region(lineInfo.getOffset() + wordStart, wordEnd - wordStart);
				}
			}
			
		} catch (BadLocationException e) {
		}
		
		return null;
	}
	
	public static List<IRegion> findAllOccurrences(IDocument document, String word, FileType fileType) {
		List<IRegion> occurrences = new ArrayList<>();
		
		if (word == null || word.trim().isEmpty() || !isValidWordForHighlighting(word, fileType)) {
			return occurrences;
		}
		
		try {
			String content = document.get();
			Matcher matcher = WORD_PATTERN.matcher(content);
			
			while (matcher.find()) {
				String foundWord = matcher.group();
				if (word.equals(foundWord)) {
					int start = matcher.start();
					int end = matcher.end();
					
					if (!isInComment(document, start) && !isAnnotation(foundWord)) {
						occurrences.add(new Region(start, end - start));
					}
				}
			}
			
		} catch (Exception e) {
		}
		
		return occurrences;
	}
	
	public static String getWordFromRegion(IDocument document, IRegion region) {
		try {
			return document.get(region.getOffset(), region.getLength());
		} catch (BadLocationException e) {
			return null;
		}
	}
	
	private static int findWordStart(String line, int position) {
		if (position >= line.length()) {
			position = line.length() - 1;
		}
		
		while (position >= 0 && isWordCharacter(line.charAt(position))) {
			position--;
		}
		position++;
		if (position < line.length() && isValidWordStart(line.charAt(position))) {
			return position;
		}
		
		return -1;
	}
	
	private static int findWordEnd(String line, int position) {
		if (position >= line.length()) {
			return line.length();
		}
		
		if (!isWordCharacter(line.charAt(position))) {
			while (position < line.length() && !isWordCharacter(line.charAt(position))) {
				position++;
			}
		}
		while (position < line.length() && isWordCharacter(line.charAt(position))) {
			position++;
		}
		
		return position;
	}
	
	private static boolean isWordCharacter(char c) {
		return Character.isLetterOrDigit(c) || c == '_' || c == '@';
	}
	
	private static boolean isValidWordStart(char c) {
		return Character.isLetter(c) || c == '_' || c == '@';
	}
	
	private static boolean isValidWordForHighlighting(String word, FileType fileType) {
		if (word == null || word.trim().isEmpty()) {
			return false;
		}
		
		if (fileType == FileType.REBECA) {
			return !REBECA_KEYWORDS.contains(word) && 
				   !REBECA_TYPES.contains(word) && 
				   !REBECA_BUILTINS.contains(word) &&
				   !REBECA_RESERVED.contains(word);
		} else if (fileType == FileType.PROPERTY) {
			return !REBECA_KEYWORDS.contains(word) && 
				   !REBECA_TYPES.contains(word) && 
				   !REBECA_BUILTINS.contains(word) &&
				   !REBECA_RESERVED.contains(word) &&
				   !PROPERTY_KEYWORDS.contains(word) &&
				   !PROPERTY_LITERALS.contains(word) &&
				   !TEMPORAL_OPERATORS.contains(word);
		}
		
		return false;
	}
	
	private static boolean isInComment(IDocument document, int offset) {
		try {
			int lineNumber = document.getLineOfOffset(offset);
			IRegion lineInfo = document.getLineInformation(lineNumber);
			String line = document.get(lineInfo.getOffset(), lineInfo.getLength());
			int positionInLine = offset - lineInfo.getOffset();
			
			String beforeWord = line.substring(0, positionInLine);
			
			if (beforeWord.contains("//")) {
				return true;
			}
			
			String content = document.get();
			int blockCommentStart = content.lastIndexOf("/*", offset);
			int blockCommentEnd = content.lastIndexOf("*/", offset);
			
			if (blockCommentStart > blockCommentEnd) {
				return true;
			}
			
			return false;
		} catch (BadLocationException e) {
			return false;
		}
	}
	
	private static boolean isAnnotation(String word) {
		if (word.startsWith("@")) {
			return true;
		}
		
		for (String annotation : RebecaConstants.ANNOTATIONS) {
			if (word.equals(annotation) || word.equals(annotation.substring(1))) {
				return true;
			}
		}
		
		if (word.equals("priority") || word.equals("globalPriority")) {
			return true;
		}
		
		return false;
	}
}
