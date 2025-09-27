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

public class RebecaRefactoringParticipant {

	public enum SymbolType {
		CLASS_NAME, METHOD_NAME, VARIABLE_NAME, INSTANCE_NAME, PROPERTY_NAME
	}

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

	public List<SymbolOccurrence> findAllOccurrences(String symbolName, SymbolType symbolType, IFile originFile,
			int originOffset) {
		List<SymbolOccurrence> occurrences = new ArrayList<>();

		try {
			List<IFile> pairedFiles = findPairedFiles(originFile);
		
			for (IFile file : pairedFiles) {
				List<SymbolOccurrence> fileOccurrences = findOccurrencesInFile(file, symbolName, symbolType, originFile, originOffset);
				occurrences.addAll(fileOccurrences);
			}

		} catch (Exception e) {
			System.err.println("Error finding symbol occurrences: " + e.getMessage());
			e.printStackTrace();
		}

		return occurrences;
	}

	private List<IFile> findPairedFiles(IFile originFile) {
		List<IFile> pairedFiles = new ArrayList<>();

		pairedFiles.add(originFile);

		String fileName = originFile.getName();
		String extension = originFile.getFileExtension();
		String baseName = fileName.substring(0, fileName.lastIndexOf('.'));

		String pairedExtension;
		if ("rebeca".equals(extension)) {
			pairedExtension = "property";
		} else if ("property".equals(extension)) {
			pairedExtension = "rebeca";
		} else {
			return pairedFiles;
		}

		try {
			IContainer parent = originFile.getParent();
			String pairedFileName = baseName + "." + pairedExtension;
			IFile pairedFile = parent.getFile(new org.eclipse.core.runtime.Path(pairedFileName));

			if (pairedFile.exists()) {
				pairedFiles.add(pairedFile);
			}
		} catch (Exception e) {
			System.err.println("Error finding paired file: " + e.getMessage());
		}

		return pairedFiles;
	}

	private List<SymbolOccurrence> findOccurrencesInFile(IFile file, String symbolName, SymbolType symbolType,
			IFile originFile, int originOffset) {
		List<SymbolOccurrence> occurrences = new ArrayList<>();

		try {
			
			IDocumentProvider provider = new TextFileDocumentProvider();
			provider.connect(file);
			IDocument document = provider.getDocument(file);

			if (document != null) {
				String content = document.get();
				String extension = file.getFileExtension();

				if ("rebeca".equals(extension)) {
					List<SymbolOccurrence> rebecaOccurrences = findOccurrencesInRebecaFile(file, content, symbolName, symbolType);
					occurrences.addAll(rebecaOccurrences);
				} else if ("property".equals(extension)) {
					List<SymbolOccurrence> propertyOccurrences = findOccurrencesInPropertyFile(file, content, symbolName, symbolType);
					occurrences.addAll(propertyOccurrences);
				}
			}

			provider.disconnect(file);

		} catch (Exception e) {
			System.err.println("[DEBUG] Error analyzing file " + file.getName() + ": " + e.getMessage());
			e.printStackTrace();
		}

		return occurrences;
	}

	private List<SymbolOccurrence> findOccurrencesInRebecaFile(IFile file, String content, String symbolName,
			SymbolType symbolType) {
		List<SymbolOccurrence> occurrences = new ArrayList<>();

		
		switch (symbolType) {
		case CLASS_NAME:
			List<SymbolOccurrence> classOccurrences = findClassNameOccurrences(file, content, symbolName);
			occurrences.addAll(classOccurrences);
			break;
		case METHOD_NAME:
			List<SymbolOccurrence> methodOccurrences = findMethodNameOccurrences(file, content, symbolName);
			occurrences.addAll(methodOccurrences);
			break;
		case VARIABLE_NAME:
			List<SymbolOccurrence> variableOccurrences = findVariableNameOccurrences(file, content, symbolName);
			occurrences.addAll(variableOccurrences);
			break;
		case INSTANCE_NAME:
			List<SymbolOccurrence> instanceOccurrences = findInstanceNameOccurrences(file, content, symbolName);
			occurrences.addAll(instanceOccurrences);
			break;
		case PROPERTY_NAME:
			List<SymbolOccurrence> propertyOccurrences = findPropertyReferencesInRebeca(file, content, symbolName);
			occurrences.addAll(propertyOccurrences);
			break;
		}

		return occurrences;
	}

	private List<SymbolOccurrence> findClassNameOccurrences(IFile file, String content, String className) {
		List<SymbolOccurrence> occurrences = new ArrayList<>();

		// pattern for class declaration: reactiveclass ClassName
		Pattern classDeclarationPattern = Pattern.compile("\\breactiveclass\\s+(" + Pattern.quote(className) + ")\\b");
		Matcher matcher = classDeclarationPattern.matcher(content);
		while (matcher.find()) {
			int offset = matcher.start(1);
			occurrences.add(new SymbolOccurrence(file, offset, className.length(), className, SymbolType.CLASS_NAME,
					new SymbolContext(className, null, true)));
		}

		// pattern for constructor: ClassName()
		Pattern constructorPattern = Pattern.compile("^\\s*(" + Pattern.quote(className) + ")\\s*\\(", Pattern.MULTILINE);
		matcher = constructorPattern.matcher(content);
		while (matcher.find()) {
			int offset = matcher.start(1);
			occurrences.add(new SymbolOccurrence(file, offset, className.length(), className, SymbolType.CLASS_NAME,
					new SymbolContext(className, null, true)));
		}

		// pattern for class usage in main block: ClassName identifier
		Pattern classUsagePattern = Pattern
				.compile("\\b(" + Pattern.quote(className) + ")\\s+\\w+\\s*\\([^)]*\\)\\s*:\\s*\\([^)]*\\)\\s*;");
		matcher = classUsagePattern.matcher(content);
		while (matcher.find()) {
			int offset = matcher.start(1);
			occurrences.add(new SymbolOccurrence(file, offset, className.length(), className, SymbolType.CLASS_NAME,
					new SymbolContext(null, null, false)));
		}

		// pattern for class usage in knownrebecs: ClassName varName1, varName2, ...;
		Pattern allKnownrebecsPattern = Pattern.compile("knownrebecs\\s*\\{([^}]*)\\}", Pattern.DOTALL);
		Matcher allKnownrebecsMatcher = allKnownrebecsPattern.matcher(content);
		while (allKnownrebecsMatcher.find()) {
			String knownrebecsBody = allKnownrebecsMatcher.group(1);
			int knownrebecsBodyOffset = allKnownrebecsMatcher.start(1);
			
			Pattern classUsageInKnownrebecs = Pattern.compile("\\b(" + Pattern.quote(className) + ")\\b(?=\\s+\\w+)");
			Matcher knownrebecsMatcher = classUsageInKnownrebecs.matcher(knownrebecsBody);
			while (knownrebecsMatcher.find()) {
		        int matchStart = knownrebecsMatcher.start(1);
		        int lineStart = knownrebecsBody.lastIndexOf('\n', matchStart) + 1;
		        String textBefore = knownrebecsBody.substring(lineStart, matchStart);

		        if (textBefore.trim().isEmpty()) {
		            int relativeOffset = knownrebecsMatcher.start(1);
		            int absoluteOffset = knownrebecsBodyOffset + relativeOffset;
		            occurrences.add(new SymbolOccurrence(file, absoluteOffset, className.length(), className,
		                    SymbolType.CLASS_NAME, new SymbolContext(null, null, false)));
		        }
			}
		}

		return occurrences;
	}

	private List<SymbolOccurrence> findMethodNameOccurrences(IFile file, String content, String methodName) {
		List<SymbolOccurrence> occurrences = new ArrayList<>();

		// pattern for method declaration: msgsrv methodName
		Pattern methodDeclarationPattern = Pattern.compile("\\bmsgsrv\\s+(" + Pattern.quote(methodName) + ")\\s*\\(");
		Matcher matcher = methodDeclarationPattern.matcher(content);
		while (matcher.find()) {
			int offset = matcher.start(1);
			occurrences.add(new SymbolOccurrence(file, offset, methodName.length(), methodName, SymbolType.METHOD_NAME,
					new SymbolContext(null, methodName, true)));
		}

		// pattern for method calls: identifier.methodName() or self.methodName()
		Pattern methodCallPattern = Pattern.compile("\\b(\\w+|self)\\.(" + Pattern.quote(methodName) + ")\\s*\\(");
		matcher = methodCallPattern.matcher(content);
		while (matcher.find()) {
			int offset = matcher.start(2);
			occurrences.add(new SymbolOccurrence(file, offset, methodName.length(), methodName, SymbolType.METHOD_NAME,
					new SymbolContext(null, methodName, false)));
		}

		return occurrences;
	}

	private List<SymbolOccurrence> findVariableNameOccurrences(IFile file, String content, String variableName) {
		List<SymbolOccurrence> occurrences = new ArrayList<>();

		// pattern for variable declarations in statevars
		Pattern allStatevarsPattern = Pattern.compile("statevars\\s*\\{([^}]*)\\}", Pattern.DOTALL);
		Matcher allStatevarsMatcher = allStatevarsPattern.matcher(content);
		while (allStatevarsMatcher.find()) {
			String statevarsBody = allStatevarsMatcher.group(1);
			int statevarsBodyOffset = allStatevarsMatcher.start(1);

			Pattern statevarPattern = Pattern.compile("\\b\\w+\\s+([^;,=]+)");
			Matcher statevarMatcher = statevarPattern.matcher(statevarsBody);
			while (statevarMatcher.find()) {
				String vars = statevarMatcher.group(1);
				String[] varNames = vars.split("\\s*,\\s*");
				for (String varName : varNames) {
					varName = varName.trim();
					if (varName.equals(variableName)) {
						int relativeOffset = statevarMatcher.start(1) + vars.indexOf(varName);
						int absoluteOffset = statevarsBodyOffset + relativeOffset;
						occurrences.add(new SymbolOccurrence(file, absoluteOffset, variableName.length(), variableName,
								SymbolType.VARIABLE_NAME, new SymbolContext(null, null, true)));
					}
				}
			}
		}

		// pattern for variable usage: standalone variable references
		Pattern variableUsagePattern = Pattern.compile("\\b(" + Pattern.quote(variableName) + ")\\b");
		Matcher variableMatcher = variableUsagePattern.matcher(content);
		while (variableMatcher.find()) {
			int offset = variableMatcher.start(1);
			// skip if it's in a declaration context or part of another identifier
			if (!isInDeclarationContext(content, offset) && !isPartOfLargerIdentifier(content, offset, variableName)) {
				occurrences.add(new SymbolOccurrence(file, offset, variableName.length(), variableName,
						SymbolType.VARIABLE_NAME, new SymbolContext(null, null, false)));
			}
		}

		return occurrences;
	}

	
	private List<SymbolOccurrence> findInstanceNameOccurrences(IFile file, String content, String instanceName) {
		List<SymbolOccurrence> occurrences = new ArrayList<>();
		java.util.Set<Integer> declarationOffsets = new java.util.HashSet<>();

		// pattern for instance declarations in knownrebecs
		Pattern allKnownrebecsPattern = Pattern.compile("knownrebecs\\s*\\{([^}]*)\\}", Pattern.DOTALL);
		Matcher allKnownrebecsMatcher = allKnownrebecsPattern.matcher(content);
		while (allKnownrebecsMatcher.find()) {
			String knownrebecsBody = allKnownrebecsMatcher.group(1);
			int knownrebecsBodyOffset = allKnownrebecsMatcher.start(1);
			Pattern knownrebecPattern = Pattern.compile("\\w+\\s+([^;,]+)");
			Matcher knownrebecMatcher = knownrebecPattern.matcher(knownrebecsBody);
			while (knownrebecMatcher.find()) {
				String instances = knownrebecMatcher.group(1);
				String[] instanceNames = instances.split("\\s*,\\s*");
				for (String instName : instanceNames) {
					instName = instName.trim();
					if (instName.equals(instanceName)) {
						int relativeOffset = knownrebecMatcher.start(1) + instances.indexOf(instName);
						int absoluteOffset = knownrebecsBodyOffset + relativeOffset;
						occurrences.add(new SymbolOccurrence(file, absoluteOffset, instanceName.length(), instanceName,
								SymbolType.INSTANCE_NAME, new SymbolContext(null, null, true)));
						declarationOffsets.add(absoluteOffset);
					}
				}
			}
		}

		// pattern for instance declarations in main section: ClassName instanceName(params):();
		Pattern mainInstancePattern = Pattern.compile("\\w+\\s+(" + Pattern.quote(instanceName) + ")\\s*\\([^\\)]*\\)\\s*:\\s*\\([^\\)]*\\)\\s*;");
		Matcher mainMatcher = mainInstancePattern.matcher(getMainSection(content));
		while (mainMatcher.find()) {
			int relativeOffset = mainMatcher.start(1);
			int absoluteOffset = findMainOffset(content) + relativeOffset;
			System.out.println("[DEBUG] Found instance '" + instanceName + "' in main section at relative offset: " + relativeOffset + ", absolute: " + absoluteOffset);
			occurrences.add(new SymbolOccurrence(file, absoluteOffset, instanceName.length(), instanceName,
					SymbolType.INSTANCE_NAME, new SymbolContext(null, null, true)));
			declarationOffsets.add(absoluteOffset);
		}

		// pattern for instance usage: instanceName.method() or standalone instanceName
		Pattern instanceUsagePattern = Pattern.compile("\\b(" + Pattern.quote(instanceName) + ")(?:\\.|\\b)");
		Matcher instanceMatcher = instanceUsagePattern.matcher(content);
		while (instanceMatcher.find()) {
			int offset = instanceMatcher.start(1);
			if (!declarationOffsets.contains(offset)) {
				occurrences.add(new SymbolOccurrence(file, offset, instanceName.length(), instanceName,
						SymbolType.INSTANCE_NAME, new SymbolContext(null, null, false)));
			}
		}

		return occurrences;
	}

	private List<SymbolOccurrence> findOccurrencesInPropertyFile(IFile file, String content, String symbolName,
			SymbolType symbolType) {
		List<SymbolOccurrence> occurrences = new ArrayList<>();

		
		switch (symbolType) {
		case PROPERTY_NAME:
			List<SymbolOccurrence> propertyOccurrences = findPropertyNameOccurrences(file, content, symbolName);
			occurrences.addAll(propertyOccurrences);
			break;
		case INSTANCE_NAME:
			List<SymbolOccurrence> instanceOccurrences = findInstanceReferencesInProperty(file, content, symbolName);
			occurrences.addAll(instanceOccurrences);
			break;
		case VARIABLE_NAME:
			List<SymbolOccurrence> variableOccurrences = findVariableReferencesInProperty(file, content, symbolName);
			occurrences.addAll(variableOccurrences);
			break;
		case CLASS_NAME:
			List<SymbolOccurrence> classOccurrences = findClassReferencesInProperty(file, content, symbolName);
			occurrences.addAll(classOccurrences);
			break;
		case METHOD_NAME:
			List<SymbolOccurrence> methodOccurrences = findMethodReferencesInProperty(file, content, symbolName);
			occurrences.addAll(methodOccurrences);
			break;
		}

		return occurrences;
	}

	private List<SymbolOccurrence> findPropertyNameOccurrences(IFile file, String content, String propertyName) {
		List<SymbolOccurrence> occurrences = new ArrayList<>();

		// pattern for property definition: propertyName = expression
		Pattern propertyDefPattern = Pattern.compile("\\b(" + Pattern.quote(propertyName) + ")\\s*=");
		Matcher propertyMatcher = propertyDefPattern.matcher(content);
		while (propertyMatcher.find()) {
			int offset = propertyMatcher.start(1);
			occurrences.add(new SymbolOccurrence(file, offset, propertyName.length(), propertyName,
					SymbolType.PROPERTY_NAME, new SymbolContext(null, null, true)));
		}

		// pattern for property usage: standalone propertyName
		Pattern propertyUsagePattern = Pattern.compile("\\b(" + Pattern.quote(propertyName) + ")\\b");
		propertyMatcher = propertyUsagePattern.matcher(content);
		while (propertyMatcher.find()) {
			int offset = propertyMatcher.start(1);
			if (!isInPropertyDefinition(content, offset)) {
				occurrences.add(new SymbolOccurrence(file, offset, propertyName.length(), propertyName,
						SymbolType.PROPERTY_NAME, new SymbolContext(null, null, false)));
			}
		}

		return occurrences;
	}

	private List<SymbolOccurrence> findInstanceReferencesInProperty(IFile file, String content, String instanceName) {
		List<SymbolOccurrence> occurrences = new ArrayList<>();

		// pattern for instance.field references
		Pattern instanceRefPattern = Pattern.compile("\\b(" + Pattern.quote(instanceName) + ")\\.");
		Matcher instanceMatcher = instanceRefPattern.matcher(content);
		while (instanceMatcher.find()) {
			int offset = instanceMatcher.start(1);
			occurrences.add(new SymbolOccurrence(file, offset, instanceName.length(), instanceName,
					SymbolType.INSTANCE_NAME, new SymbolContext(null, null, false)));
		}

		return occurrences;
	}

	private List<SymbolOccurrence> findVariableReferencesInProperty(IFile file, String content, String variableName) {
		List<SymbolOccurrence> occurrences = new ArrayList<>();

		// pattern for instance.variableName references
		Pattern variableRefPattern = Pattern.compile("\\w+\\.(" + Pattern.quote(variableName) + ")\\b");
		Matcher variableMatcher = variableRefPattern.matcher(content);
		while (variableMatcher.find()) {
			int offset = variableMatcher.start(1);
			occurrences.add(new SymbolOccurrence(file, offset, variableName.length(), variableName,
					SymbolType.VARIABLE_NAME, new SymbolContext(null, null, false)));
		}

		return occurrences;
	}

	private List<SymbolOccurrence> findPropertyReferencesInRebeca(IFile file, String content, String propertyName) {
		List<SymbolOccurrence> occurrences = new ArrayList<>();
		
		Pattern propertyRefPattern = Pattern.compile("\\b(" + Pattern.quote(propertyName) + ")\\b");
		Matcher propertyMatcher = propertyRefPattern.matcher(content);
		while (propertyMatcher.find()) {
			int offset = propertyMatcher.start(1);
			occurrences.add(new SymbolOccurrence(file, offset, propertyName.length(), propertyName,
					SymbolType.PROPERTY_NAME, new SymbolContext(null, null, false)));
		}
		
		return occurrences;
	}

	private List<SymbolOccurrence> findClassReferencesInProperty(IFile file, String content, String className) {
		List<SymbolOccurrence> occurrences = new ArrayList<>();
		
		Pattern classRefPattern = Pattern.compile("\\b(" + Pattern.quote(className) + ")\\b");
		Matcher classMatcher = classRefPattern.matcher(content);
		while (classMatcher.find()) {
			int offset = classMatcher.start(1);
			occurrences.add(new SymbolOccurrence(file, offset, className.length(), className,
					SymbolType.CLASS_NAME, new SymbolContext(null, null, false)));
		}
		
		return occurrences;
	}

	private List<SymbolOccurrence> findMethodReferencesInProperty(IFile file, String content, String methodName) {
		List<SymbolOccurrence> occurrences = new ArrayList<>();
		
		Pattern methodRefPattern = Pattern.compile("\\b(" + Pattern.quote(methodName) + ")\\b");
		Matcher methodMatcher = methodRefPattern.matcher(content);
		while (methodMatcher.find()) {
			int offset = methodMatcher.start(1);
			occurrences.add(new SymbolOccurrence(file, offset, methodName.length(), methodName,
					SymbolType.METHOD_NAME, new SymbolContext(null, null, false)));
		}
		
		return occurrences;
	}

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
		String beforeOffset = content.substring(0, offset);

		if (beforeOffset.lastIndexOf("statevars") > beforeOffset.lastIndexOf("}")
				|| beforeOffset.lastIndexOf("knownrebecs") > beforeOffset.lastIndexOf("}")) {
			return true;
		}

		return false;
	}

	private boolean isPartOfLargerIdentifier(String content, int offset, String name) {
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
		String lineStart = content.substring(0, offset);
		int lastNewline = lineStart.lastIndexOf('\n');
		String currentLine = lineStart.substring(lastNewline + 1);
		return !currentLine.contains("=");
	}
}
