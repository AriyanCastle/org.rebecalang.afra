package org.rebecalang.afra.ideplugin.refactoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;

/**
 * Main refactoring engine for Rebeca files that provides semantic-aware
 * renaming
 */
public class RebecaRefactoringParticipant {

	/**
	 * Types of symbols that can be refactored
	 */
	public enum SymbolType {
		CLASS_NAME, METHOD_NAME, VARIABLE_NAME, INSTANCE_NAME, PROPERTY_NAME
	}

	/**
	 * Represents a symbol occurrence in the code
	 */
	public static class SymbolOccurrence {
		public final IFile file;
		public final int offset;
		public final int length;
		public final String originalName;
		public final SymbolType type;
		public final SymbolContext context;

		public SymbolOccurrence(IFile file, int offset, int length, String originalName, SymbolType type,
				SymbolContext context) {
			this.file = file;
			this.offset = offset;
			this.length = length;
			this.originalName = originalName;
			this.type = type;
			this.context = context;
		}
	}

	/**
	 * Context information for symbol analysis
	 */
	public static class SymbolContext {
		public final String className;
		public final String methodName;
		public final boolean isDeclaration;

		public SymbolContext(String className, String methodName, boolean isDeclaration) {
			this.className = className;
			this.methodName = methodName;
			this.isDeclaration = isDeclaration;
		}
	}

	public RebecaRefactoringParticipant(IProject project) {
	}

	/**
	 * Find all occurrences of a symbol in the current file and its corresponding
	 * paired file
	 */
	public List<SymbolOccurrence> findAllOccurrences(String symbolName, SymbolType symbolType, IFile originFile,
			int originOffset) {
		List<SymbolOccurrence> occurrences = new ArrayList<>();

		try {
			System.out.println("[DEBUG] *** RebecaRefactoringParticipant.findAllOccurrences ***");
			System.out.println("[DEBUG] Symbol: '" + symbolName + "', Type: " + symbolType);
			System.out.println("[DEBUG] Origin file: " + originFile.getName());
			
			// Get the current file and its corresponding paired file
			List<IFile> pairedFiles = findPairedFiles(originFile);
			System.out.println("[DEBUG] Found " + pairedFiles.size() + " paired files:");
			for (IFile file : pairedFiles) {
				System.out.println("[DEBUG]   - " + file.getName());
			}

			for (IFile file : pairedFiles) {
				System.out.println("[DEBUG] Processing file: " + file.getName());
				List<SymbolOccurrence> fileOccurrences = findOccurrencesInFile(file, symbolName, symbolType, originFile, originOffset);
				System.out.println("[DEBUG] Found " + fileOccurrences.size() + " occurrences in " + file.getName());
				occurrences.addAll(fileOccurrences);
			}

		} catch (Exception e) {
			System.err.println("[DEBUG] Error finding symbol occurrences: " + e.getMessage());
			e.printStackTrace();
		}

		System.out.println("[DEBUG] Total occurrences found: " + occurrences.size());
		return occurrences;
	}

	/**
	 * Find the current file and its corresponding paired file (same base name,
	 * different extension)
	 */
	private List<IFile> findPairedFiles(IFile originFile) {
		List<IFile> pairedFiles = new ArrayList<>();

		// Always include the origin file
		pairedFiles.add(originFile);

		// Get the base name (filename without extension)
		String fileName = originFile.getName();
		String extension = originFile.getFileExtension();
		String baseName = fileName.substring(0, fileName.lastIndexOf('.'));

		// Determine the paired extension
		String pairedExtension;
		if ("rebeca".equals(extension)) {
			pairedExtension = "property";
		} else if ("property".equals(extension)) {
			pairedExtension = "rebeca";
		} else {
			// Unknown extension, return only the origin file
			return pairedFiles;
		}

		// Look for the paired file in the same folder
		try {
			IContainer parent = originFile.getParent();
			String pairedFileName = baseName + "." + pairedExtension;
			IFile pairedFile = parent.getFile(new org.eclipse.core.runtime.Path(pairedFileName));

			// Only add if the paired file exists
			if (pairedFile.exists()) {
				pairedFiles.add(pairedFile);
			}
		} catch (Exception e) {
			System.err.println("Error finding paired file: " + e.getMessage());
		}

		return pairedFiles;
	}

	/**
	 * Find occurrences of a symbol in a specific file
	 */
	private List<SymbolOccurrence> findOccurrencesInFile(IFile file, String symbolName, SymbolType symbolType,
			IFile originFile, int originOffset) {
		List<SymbolOccurrence> occurrences = new ArrayList<>();

		try {
			System.out.println("[DEBUG]   findOccurrencesInFile: " + file.getName() + " for '" + symbolName + "' (" + symbolType + ")");
			
			IDocumentProvider provider = new TextFileDocumentProvider();
			provider.connect(file);
			IDocument document = provider.getDocument(file);

			if (document != null) {
				String content = document.get();
				String extension = file.getFileExtension();
				System.out.println("[DEBUG]   File extension: " + extension);

				if ("rebeca".equals(extension)) {
					System.out.println("[DEBUG]   Searching in Rebeca file...");
					List<SymbolOccurrence> rebecaOccurrences = findOccurrencesInRebecaFile(file, content, symbolName, symbolType);
					System.out.println("[DEBUG]   Found " + rebecaOccurrences.size() + " occurrences in Rebeca file");
					occurrences.addAll(rebecaOccurrences);
				} else if ("property".equals(extension)) {
					System.out.println("[DEBUG]   Searching in Property file...");
					List<SymbolOccurrence> propertyOccurrences = findOccurrencesInPropertyFile(file, content, symbolName, symbolType);
					System.out.println("[DEBUG]   Found " + propertyOccurrences.size() + " occurrences in Property file");
					occurrences.addAll(propertyOccurrences);
				}
			} else {
				System.out.println("[DEBUG]   Document is null for file: " + file.getName());
			}

			provider.disconnect(file);

		} catch (Exception e) {
			System.err.println("[DEBUG] Error analyzing file " + file.getName() + ": " + e.getMessage());
			e.printStackTrace();
		}

		return occurrences;
	}

	/**
	 * Find symbol occurrences in a Rebeca file
	 */
	private List<SymbolOccurrence> findOccurrencesInRebecaFile(IFile file, String content, String symbolName,
			SymbolType symbolType) {
		List<SymbolOccurrence> occurrences = new ArrayList<>();

		System.out.println("[DEBUG]     findOccurrencesInRebecaFile: " + symbolName + " (" + symbolType + ")");
		
		switch (symbolType) {
		case CLASS_NAME:
			System.out.println("[DEBUG]     Searching for CLASS_NAME: " + symbolName);
			List<SymbolOccurrence> classOccurrences = findClassNameOccurrences(file, content, symbolName);
			System.out.println("[DEBUG]     Found " + classOccurrences.size() + " class occurrences");
			occurrences.addAll(classOccurrences);
			break;
		case METHOD_NAME:
			System.out.println("[DEBUG]     Searching for METHOD_NAME: " + symbolName);
			List<SymbolOccurrence> methodOccurrences = findMethodNameOccurrences(file, content, symbolName);
			System.out.println("[DEBUG]     Found " + methodOccurrences.size() + " method occurrences");
			occurrences.addAll(methodOccurrences);
			break;
		case VARIABLE_NAME:
			System.out.println("[DEBUG]     Searching for VARIABLE_NAME: " + symbolName);
			List<SymbolOccurrence> variableOccurrences = findVariableNameOccurrences(file, content, symbolName);
			System.out.println("[DEBUG]     Found " + variableOccurrences.size() + " variable occurrences");
			occurrences.addAll(variableOccurrences);
			break;
		case INSTANCE_NAME:
			System.out.println("[DEBUG]     Searching for INSTANCE_NAME: " + symbolName);
			List<SymbolOccurrence> instanceOccurrences = findInstanceNameOccurrences(file, content, symbolName);
			System.out.println("[DEBUG]     Found " + instanceOccurrences.size() + " instance occurrences");
			occurrences.addAll(instanceOccurrences);
			break;
		case PROPERTY_NAME:
			System.out.println("[DEBUG]     Searching for PROPERTY_NAME: " + symbolName);
			List<SymbolOccurrence> propertyOccurrences = findPropertyReferencesInRebeca(file, content, symbolName);
			System.out.println("[DEBUG]     Found " + propertyOccurrences.size() + " property occurrences");
			occurrences.addAll(propertyOccurrences);
			break;
		}

		return occurrences;
	}

	/**
	 * Find class name occurrences
	 */
	private List<SymbolOccurrence> findClassNameOccurrences(IFile file, String content, String className) {
		List<SymbolOccurrence> occurrences = new ArrayList<>();

		// Pattern for class declaration: reactiveclass ClassName
		Pattern classDeclarationPattern = Pattern.compile("\\breactiveclass\\s+(" + Pattern.quote(className) + ")\\b");
		Matcher matcher = classDeclarationPattern.matcher(content);
		while (matcher.find()) {
			int offset = matcher.start(1);
			occurrences.add(new SymbolOccurrence(file, offset, className.length(), className, SymbolType.CLASS_NAME,
					new SymbolContext(className, null, true)));
		}

		// Pattern for class usage in main block: ClassName identifier
		Pattern classUsagePattern = Pattern
				.compile("\\b(" + Pattern.quote(className) + ")\\s+\\w+\\s*\\([^)]*\\)\\s*:\\s*\\([^)]*\\)\\s*;");
		matcher = classUsagePattern.matcher(content);
		while (matcher.find()) {
			int offset = matcher.start(1);
			occurrences.add(new SymbolOccurrence(file, offset, className.length(), className, SymbolType.CLASS_NAME,
					new SymbolContext(null, null, false)));
		}

		// Pattern for class usage in knownrebecs: ClassName varName1, varName2, ...;
		// Restrict search to inside the knownrebecs section and compute absolute
		// offsets correctly
		String knownrebecsBody = getKnownrebecssection(content);
		if (!knownrebecsBody.isEmpty()) {
			int knownrebecsBodyOffset = findKnownrebecsOffset(content);
			
			// Debug: Print knownrebecs content and pattern
			System.out.println("[DEBUG] Knownrebecs body: '" + knownrebecsBody + "'");
			System.out.println("[DEBUG] Looking for class: '" + className + "'");
			System.out.println("[DEBUG] Knownrebecs offset: " + knownrebecsBodyOffset);
			
			// Use a simpler approach: find all instances of the class name as word boundaries
			// then check the context to see if it's used as a class name
			Pattern simplePattern = Pattern.compile("\\b(" + Pattern.quote(className) + ")\\b");
			Matcher simpleMatcher = simplePattern.matcher(knownrebecsBody);
			while (simpleMatcher.find()) {
				int relativeOffset = simpleMatcher.start(1);
				int absoluteOffset = knownrebecsBodyOffset + relativeOffset;
				
				// Check if this looks like a class declaration by examining the context
				String beforeClass = knownrebecsBody.substring(0, relativeOffset);
				String afterClass = knownrebecsBody.substring(simpleMatcher.end());
				
				// Check if it's at the start of a line (after whitespace only)
				// and followed by instance names (whitespace + identifiers + semicolon)
				String[] beforeLines = beforeClass.split("\\n");
				String currentLinePrefix = beforeLines[beforeLines.length - 1];
				
				// If the current line before the class name only contains whitespace,
				// and what comes after matches the pattern for instance names
				if (currentLinePrefix.trim().isEmpty() && afterClass.matches("\\s+[\\w\\s,]+;.*")) {
					System.out.println("[DEBUG] Found class '" + className + "' in knownrebecs at relative offset: " + relativeOffset + ", absolute: " + absoluteOffset);
					occurrences.add(new SymbolOccurrence(file, absoluteOffset, className.length(), className,
							SymbolType.CLASS_NAME, new SymbolContext(null, null, false)));
				}
			}
		} else {
			System.out.println("[DEBUG] Knownrebecs body is empty for class: " + className);
		}

		return occurrences;
	}

	/**
	 * Find method name occurrences
	 */
	private List<SymbolOccurrence> findMethodNameOccurrences(IFile file, String content, String methodName) {
		List<SymbolOccurrence> occurrences = new ArrayList<>();

		// Pattern for method declaration: msgsrv methodName
		Pattern methodDeclarationPattern = Pattern.compile("\\bmsgsrv\\s+(" + Pattern.quote(methodName) + ")\\s*\\(");
		Matcher matcher = methodDeclarationPattern.matcher(content);
		while (matcher.find()) {
			int offset = matcher.start(1);
			occurrences.add(new SymbolOccurrence(file, offset, methodName.length(), methodName, SymbolType.METHOD_NAME,
					new SymbolContext(null, methodName, true)));
		}

		// Pattern for method calls: identifier.methodName() or self.methodName()
		Pattern methodCallPattern = Pattern.compile("\\b(\\w+|self)\\.(" + Pattern.quote(methodName) + ")\\s*\\(");
		matcher = methodCallPattern.matcher(content);
		while (matcher.find()) {
			int offset = matcher.start(2);
			occurrences.add(new SymbolOccurrence(file, offset, methodName.length(), methodName, SymbolType.METHOD_NAME,
					new SymbolContext(null, methodName, false)));
		}

		return occurrences;
	}

	/**
	 * Find variable name occurrences
	 */
	private List<SymbolOccurrence> findVariableNameOccurrences(IFile file, String content, String variableName) {
		List<SymbolOccurrence> occurrences = new ArrayList<>();

		// Pattern for variable declarations in statevars
		Pattern statevarPattern = Pattern.compile("\\b\\w+\\s+([^;,=]+)");
		Matcher statevarMatcher = statevarPattern.matcher(getStatevarsSection(content));
		while (statevarMatcher.find()) {
			String vars = statevarMatcher.group(1);
			String[] varNames = vars.split("\\s*,\\s*");
			for (String varName : varNames) {
				varName = varName.trim();
				if (varName.equals(variableName)) {
					// Find offset in original content
					int relativeOffset = statevarMatcher.start(1) + vars.indexOf(varName);
					int absoluteOffset = findStatevarsOffset(content) + relativeOffset;
					occurrences.add(new SymbolOccurrence(file, absoluteOffset, variableName.length(), variableName,
							SymbolType.VARIABLE_NAME, new SymbolContext(null, null, true)));
				}
			}
		}

		// Pattern for variable usage: standalone variable references
		Pattern variableUsagePattern = Pattern.compile("\\b(" + Pattern.quote(variableName) + ")\\b");
		Matcher matcher = variableUsagePattern.matcher(content);
		while (matcher.find()) {
			int offset = matcher.start(1);
			// Skip if it's in a declaration context or part of another identifier
			if (!isInDeclarationContext(content, offset) && !isPartOfLargerIdentifier(content, offset, variableName)) {
				occurrences.add(new SymbolOccurrence(file, offset, variableName.length(), variableName,
						SymbolType.VARIABLE_NAME, new SymbolContext(null, null, false)));
			}
		}

		return occurrences;
	}

	/**
	 * Find instance name occurrences
	 */
	private List<SymbolOccurrence> findInstanceNameOccurrences(IFile file, String content, String instanceName) {
		List<SymbolOccurrence> occurrences = new ArrayList<>();

		// Pattern for instance declarations in knownrebecs
		Pattern knownrebecPattern = Pattern.compile("\\w+\\s+([^;,]+)");
		Matcher knownrebecMatcher = knownrebecPattern.matcher(getKnownrebecssection(content));
		while (knownrebecMatcher.find()) {
			String instances = knownrebecMatcher.group(1);
			String[] instanceNames = instances.split("\\s*,\\s*");
			for (String instName : instanceNames) {
				instName = instName.trim();
				if (instName.equals(instanceName)) {
					// Find offset in original content
					int relativeOffset = knownrebecMatcher.start(1) + instances.indexOf(instName);
					int absoluteOffset = findKnownrebecsOffset(content) + relativeOffset;
					occurrences.add(new SymbolOccurrence(file, absoluteOffset, instanceName.length(), instanceName,
							SymbolType.INSTANCE_NAME, new SymbolContext(null, null, true)));
				}
			}
		}

		// Pattern for instance declarations in main section: ClassName instanceName(params):();
		Pattern mainInstancePattern = Pattern.compile("\\w+\\s+(" + Pattern.quote(instanceName) + ")\\s*\\([^\\)]*\\)\\s*:\\s*\\([^\\)]*\\)\\s*;");
		Matcher mainMatcher = mainInstancePattern.matcher(getMainSection(content));
		while (mainMatcher.find()) {
			int relativeOffset = mainMatcher.start(1);
			int absoluteOffset = findMainOffset(content) + relativeOffset;
			System.out.println("[DEBUG] Found instance '" + instanceName + "' in main section at relative offset: " + relativeOffset + ", absolute: " + absoluteOffset);
			occurrences.add(new SymbolOccurrence(file, absoluteOffset, instanceName.length(), instanceName,
					SymbolType.INSTANCE_NAME, new SymbolContext(null, null, true)));
		}

		// Pattern for instance usage: instanceName.method() or standalone instanceName
		Pattern instanceUsagePattern = Pattern.compile("\\b(" + Pattern.quote(instanceName) + ")(?:\\.|\\b)");
		Matcher matcher = instanceUsagePattern.matcher(content);
		while (matcher.find()) {
			int offset = matcher.start(1);
			if (!isInDeclarationContext(content, offset)) {
				occurrences.add(new SymbolOccurrence(file, offset, instanceName.length(), instanceName,
						SymbolType.INSTANCE_NAME, new SymbolContext(null, null, false)));
			}
		}

		return occurrences;
	}

	/**
	 * Find symbol occurrences in a property file
	 */
	private List<SymbolOccurrence> findOccurrencesInPropertyFile(IFile file, String content, String symbolName,
			SymbolType symbolType) {
		List<SymbolOccurrence> occurrences = new ArrayList<>();

		System.out.println("[DEBUG]     findOccurrencesInPropertyFile: " + symbolName + " (" + symbolType + ")");
		
		switch (symbolType) {
		case PROPERTY_NAME:
			System.out.println("[DEBUG]     Searching for PROPERTY_NAME in property file: " + symbolName);
			List<SymbolOccurrence> propertyOccurrences = findPropertyNameOccurrences(file, content, symbolName);
			System.out.println("[DEBUG]     Found " + propertyOccurrences.size() + " property occurrences");
			occurrences.addAll(propertyOccurrences);
			break;
		case INSTANCE_NAME:
			System.out.println("[DEBUG]     Searching for INSTANCE_NAME in property file: " + symbolName);
			List<SymbolOccurrence> instanceOccurrences = findInstanceReferencesInProperty(file, content, symbolName);
			System.out.println("[DEBUG]     Found " + instanceOccurrences.size() + " instance occurrences");
			occurrences.addAll(instanceOccurrences);
			break;
		case VARIABLE_NAME:
			System.out.println("[DEBUG]     Searching for VARIABLE_NAME in property file: " + symbolName);
			List<SymbolOccurrence> variableOccurrences = findVariableReferencesInProperty(file, content, symbolName);
			System.out.println("[DEBUG]     Found " + variableOccurrences.size() + " variable occurrences");
			occurrences.addAll(variableOccurrences);
			break;
		case CLASS_NAME:
			System.out.println("[DEBUG]     Searching for CLASS_NAME in property file: " + symbolName);
			List<SymbolOccurrence> classOccurrences = findClassReferencesInProperty(file, content, symbolName);
			System.out.println("[DEBUG]     Found " + classOccurrences.size() + " class occurrences");
			occurrences.addAll(classOccurrences);
			break;
		case METHOD_NAME:
			System.out.println("[DEBUG]     Searching for METHOD_NAME in property file: " + symbolName);
			List<SymbolOccurrence> methodOccurrences = findMethodReferencesInProperty(file, content, symbolName);
			System.out.println("[DEBUG]     Found " + methodOccurrences.size() + " method occurrences");
			occurrences.addAll(methodOccurrences);
			break;
		}

		return occurrences;
	}

	/**
	 * Find property name occurrences in property files
	 */
	private List<SymbolOccurrence> findPropertyNameOccurrences(IFile file, String content, String propertyName) {
		List<SymbolOccurrence> occurrences = new ArrayList<>();

		// Pattern for property definition: propertyName = expression
		Pattern propertyDefPattern = Pattern.compile("\\b(" + Pattern.quote(propertyName) + ")\\s*=");
		Matcher matcher = propertyDefPattern.matcher(content);
		while (matcher.find()) {
			int offset = matcher.start(1);
			occurrences.add(new SymbolOccurrence(file, offset, propertyName.length(), propertyName,
					SymbolType.PROPERTY_NAME, new SymbolContext(null, null, true)));
		}

		// Pattern for property usage: standalone propertyName
		Pattern propertyUsagePattern = Pattern.compile("\\b(" + Pattern.quote(propertyName) + ")\\b");
		matcher = propertyUsagePattern.matcher(content);
		while (matcher.find()) {
			int offset = matcher.start(1);
			if (!isInPropertyDefinition(content, offset)) {
				occurrences.add(new SymbolOccurrence(file, offset, propertyName.length(), propertyName,
						SymbolType.PROPERTY_NAME, new SymbolContext(null, null, false)));
			}
		}

		return occurrences;
	}

	/**
	 * Find instance references in property files (instance.field and standalone instance)
	 */
	private List<SymbolOccurrence> findInstanceReferencesInProperty(IFile file, String content, String instanceName) {
		List<SymbolOccurrence> occurrences = new ArrayList<>();

		// Pattern for instance.field references
		Pattern instanceRefPattern = Pattern.compile("\\b(" + Pattern.quote(instanceName) + ")\\.");
		Matcher matcher = instanceRefPattern.matcher(content);
		while (matcher.find()) {
			int offset = matcher.start(1);
			occurrences.add(new SymbolOccurrence(file, offset, instanceName.length(), instanceName,
					SymbolType.INSTANCE_NAME, new SymbolContext(null, null, false)));
		}

		// Pattern for standalone instance references (not followed by dot)
		Pattern standaloneInstancePattern = Pattern.compile("\\b(" + Pattern.quote(instanceName) + ")\\b(?!\\.)");
		matcher = standaloneInstancePattern.matcher(content);
		while (matcher.find()) {
			int offset = matcher.start(1);
			// Check if this is not already found by the previous pattern
			boolean alreadyFound = false;
			for (SymbolOccurrence existing : occurrences) {
				if (existing.offset == offset) {
					alreadyFound = true;
					break;
				}
			}
			if (!alreadyFound) {
				occurrences.add(new SymbolOccurrence(file, offset, instanceName.length(), instanceName,
						SymbolType.INSTANCE_NAME, new SymbolContext(null, null, false)));
			}
		}

		return occurrences;
	}

	/**
	 * Find variable references in property files (instance.variableName)
	 */
	private List<SymbolOccurrence> findVariableReferencesInProperty(IFile file, String content, String variableName) {
		List<SymbolOccurrence> occurrences = new ArrayList<>();

		// Pattern for instance.variableName references
		Pattern variableRefPattern = Pattern.compile("\\w+\\.(" + Pattern.quote(variableName) + ")\\b");
		Matcher matcher = variableRefPattern.matcher(content);
		while (matcher.find()) {
			int offset = matcher.start(1);
			occurrences.add(new SymbolOccurrence(file, offset, variableName.length(), variableName,
					SymbolType.VARIABLE_NAME, new SymbolContext(null, null, false)));
		}

		return occurrences;
	}

	/**
	 * Find property references in rebeca files (typically none, but included for completeness)
	 */
	private List<SymbolOccurrence> findPropertyReferencesInRebeca(IFile file, String content, String propertyName) {
		List<SymbolOccurrence> occurrences = new ArrayList<>();
		
		// Property names typically don't appear in rebeca files, but search for any potential references
		Pattern propertyRefPattern = Pattern.compile("\\b(" + Pattern.quote(propertyName) + ")\\b");
		Matcher matcher = propertyRefPattern.matcher(content);
		while (matcher.find()) {
			int offset = matcher.start(1);
			occurrences.add(new SymbolOccurrence(file, offset, propertyName.length(), propertyName,
					SymbolType.PROPERTY_NAME, new SymbolContext(null, null, false)));
		}
		
		return occurrences;
	}

	/**
	 * Find class references in property files (typically none, but included for completeness)
	 */
	private List<SymbolOccurrence> findClassReferencesInProperty(IFile file, String content, String className) {
		List<SymbolOccurrence> occurrences = new ArrayList<>();
		
		// Class names typically don't appear in property files, but search for any potential references
		Pattern classRefPattern = Pattern.compile("\\b(" + Pattern.quote(className) + ")\\b");
		Matcher matcher = classRefPattern.matcher(content);
		while (matcher.find()) {
			int offset = matcher.start(1);
			occurrences.add(new SymbolOccurrence(file, offset, className.length(), className,
					SymbolType.CLASS_NAME, new SymbolContext(null, null, false)));
		}
		
		return occurrences;
	}

	/**
	 * Find method references in property files (typically none, but included for completeness)
	 */
	private List<SymbolOccurrence> findMethodReferencesInProperty(IFile file, String content, String methodName) {
		List<SymbolOccurrence> occurrences = new ArrayList<>();
		
		// Method names typically don't appear in property files, but search for any potential references
		Pattern methodRefPattern = Pattern.compile("\\b(" + Pattern.quote(methodName) + ")\\b");
		Matcher matcher = methodRefPattern.matcher(content);
		while (matcher.find()) {
			int offset = matcher.start(1);
			occurrences.add(new SymbolOccurrence(file, offset, methodName.length(), methodName,
					SymbolType.METHOD_NAME, new SymbolContext(null, null, false)));
		}
		
		return occurrences;
	}

	// Helper methods for context analysis

	private String getStatevarsSection(String content) {
		Pattern pattern = Pattern.compile("statevars\\s*\\{([^}]*)\\}", Pattern.DOTALL);
		Matcher matcher = pattern.matcher(content);
		return matcher.find() ? matcher.group(1) : "";
	}

	private String getKnownrebecssection(String content) {
		Pattern pattern = Pattern.compile("knownrebecs\\s*\\{([^}]*)\\}", Pattern.DOTALL);
		Matcher matcher = pattern.matcher(content);
		return matcher.find() ? matcher.group(1) : "";
	}

	private String getMainSection(String content) {
		Pattern pattern = Pattern.compile("main\\s*\\{([^}]*)\\}", Pattern.DOTALL);
		Matcher matcher = pattern.matcher(content);
		return matcher.find() ? matcher.group(1) : "";
	}

	private int findStatevarsOffset(String content) {
		Pattern pattern = Pattern.compile("statevars\\s*\\{");
		Matcher matcher = pattern.matcher(content);
		return matcher.find() ? matcher.end() : 0;
	}

	private int findKnownrebecsOffset(String content) {
		Pattern pattern = Pattern.compile("knownrebecs\\s*\\{");
		Matcher matcher = pattern.matcher(content);
		return matcher.find() ? matcher.end() : 0;
	}

	private int findMainOffset(String content) {
		Pattern pattern = Pattern.compile("main\\s*\\{");
		Matcher matcher = pattern.matcher(content);
		return matcher.find() ? matcher.end() : 0;
	}

	private boolean isInDeclarationContext(String content, int offset) {
		// Check if the offset is within a declaration block
		String beforeOffset = content.substring(0, offset);

		// Check for statevars or knownrebecs context
		if (beforeOffset.lastIndexOf("statevars") > beforeOffset.lastIndexOf("}")
				|| beforeOffset.lastIndexOf("knownrebecs") > beforeOffset.lastIndexOf("}")) {
			return true;
		}

		return false;
	}

	private boolean isPartOfLargerIdentifier(String content, int offset, String name) {
		// Check if this name is part of a larger identifier
		if (offset > 0 && Character.isJavaIdentifierPart(content.charAt(offset - 1))) {
			return true;
		}
		if (offset + name.length() < content.length()
				&& Character.isJavaIdentifierPart(content.charAt(offset + name.length()))) {
			return true;
		}
		return false;
	}

	private boolean isInPropertyDefinition(String content, int offset) {
		// Check if this is in a property definition (before =)
		String lineStart = content.substring(0, offset);
		int lastNewline = lineStart.lastIndexOf('\n');
		String currentLine = lineStart.substring(lastNewline + 1);
		return !currentLine.contains("=");
	}
}
