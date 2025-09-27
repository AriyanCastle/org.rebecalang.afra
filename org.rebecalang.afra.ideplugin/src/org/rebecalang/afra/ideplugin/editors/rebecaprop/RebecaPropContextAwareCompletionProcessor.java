package org.rebecalang.afra.ideplugin.editors.rebecaprop;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.rebecalang.afra.ideplugin.handler.CompilationAndCodeGenerationProcess;
import org.rebecalang.afra.ideplugin.preference.CoreRebecaProjectPropertyPage;
import org.rebecalang.compiler.CompilerConfig;
import org.rebecalang.compiler.modelcompiler.RebecaModelCompiler;
import org.rebecalang.compiler.modelcompiler.SymbolTable;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.FieldDeclaration;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.MainRebecDefinition;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.ReactiveClassDeclaration;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.RebecaModel;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.Type;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.VariableDeclarator;
import org.rebecalang.compiler.utils.CompilerExtension;
import org.rebecalang.compiler.utils.CoreVersion;
import org.rebecalang.compiler.utils.Pair;
import org.rebecalang.rmc.RMCConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Context-aware completion processor for Rebeca Property files that provides:
 * - Suggestions for defined variables within the same property file
 * - Cross-file context awareness to suggest variables from corresponding .rebeca files
 * - Context-aware completion for object.statevars patterns
 */
public class RebecaPropContextAwareCompletionProcessor implements IContentAssistProcessor {

	@Autowired
	RebecaModelCompiler modelCompiler;
	
	private RebecaPropEditor editor;
	
	// Keywords from RebecaPropScanner
	private static String[] keywords = {
		"define", "CTL", "LTL", "property", "true", "false", "Assertion"
	};
	
	/**
	 * Represents the context of what the user is typing in property files
	 */
	private static class PropCompletionContext {
		public String objectName;      // Part before the dot (e.g., "node1" in "node1.leaderId")
		public String partialText;     // Part after the dot or the current word being typed
		public boolean isDotCompletion; // True if user is completing after a dot
		public boolean isDefineContext; // True if we're inside a define statement
		public int replacementOffset;   // Offset where replacement should start
		public int replacementLength;   // Length of text to replace
		public String defineVariable;   // The variable being defined (left side of =)
	}
	
	public RebecaPropContextAwareCompletionProcessor(RebecaPropEditor editor) {
		this.editor = editor;
		@SuppressWarnings("resource")
		ApplicationContext context = new AnnotationConfigApplicationContext(RMCConfig.class, CompilerConfig.class);
		AutowireCapableBeanFactory factory = context.getAutowireCapableBeanFactory();
		factory.autowireBean(this);
	}

	/**
	 * Determine completion context for property files
	 */
	private PropCompletionContext getCompletionContext(IDocument document, int offset) {
		PropCompletionContext context = new PropCompletionContext();
		
		try {
			if (offset <= 0 || offset > document.getLength()) {
				context.partialText = "";
				context.isDotCompletion = false;
				context.isDefineContext = false;
				context.replacementOffset = offset;
				context.replacementLength = 0;
				return context;
			}
			
			// Get current line to check if we're in a define context
			int lineNumber = document.getLineOfOffset(offset);
			int lineOffset = document.getLineOffset(lineNumber);
			int lineEnd = lineOffset + document.getLineLength(lineNumber);
			String currentLine = document.get(lineOffset, lineEnd - lineOffset);
			
			// Check if we're in a define block
			context.isDefineContext = isInDefineBlock(document, lineNumber);
			
			// Parse the define line if we're in define context
			if (context.isDefineContext && currentLine.contains("=")) {
				// Set initial replacement offset to current cursor position
				context.replacementOffset = offset;
				parseDefineLine(currentLine, offset - lineOffset, context);
			} else {
				// Regular context parsing
				parseRegularContext(document, offset, context);
			}
			
		} catch (BadLocationException e) {
			System.err.println("Error determining completion context: " + e.getMessage());
			context.partialText = "";
			context.isDotCompletion = false;
			context.isDefineContext = false;
			context.replacementOffset = offset;
			context.replacementLength = 0;
		}
		
		// Validate and fix replacement offset and length
		if (context.replacementOffset < 0) {
			context.replacementOffset = 0;
		}
		if (context.replacementOffset > document.getLength()) {
			context.replacementOffset = document.getLength();
		}
		if (context.replacementLength < 0) {
			context.replacementLength = 0;
		}
		if (context.replacementOffset + context.replacementLength > document.getLength()) {
			context.replacementLength = document.getLength() - context.replacementOffset;
		}
		
		return context;
	}
	
	/**
	 * Check if the current line is within a define block
	 */
	private boolean isInDefineBlock(IDocument document, int lineNumber) {
		try {
			// Look backwards for "define {" and forwards for "}"
			boolean foundDefine = false;
			
			for (int i = lineNumber; i >= 0; i--) {
				String line = getLineContent(document, i).trim();
				if (line.contains("define") && line.contains("{")) {
					foundDefine = true;
					break;
				}
				if (line.contains("}") && !line.contains("define")) {
					break; // Found closing brace before define
				}
			}
			
			if (!foundDefine) return false;
			
			// Look forward for closing brace
			for (int i = lineNumber; i < document.getNumberOfLines(); i++) {
				String line = getLineContent(document, i).trim();
				if (line.contains("}")) {
					return true; // Found closing brace after define
				}
			}
			
		} catch (Exception e) {
			System.err.println("Error checking define block: " + e.getMessage());
		}
		
		return false;
	}
	
	private String getLineContent(IDocument document, int lineNumber) throws BadLocationException {
		int lineOffset = document.getLineOffset(lineNumber);
		int lineLength = document.getLineLength(lineNumber);
		return document.get(lineOffset, lineLength);
	}
	
	/**
	 * Parse a define line to extract context
	 */
	private void parseDefineLine(String line, int positionInLine, PropCompletionContext context) {
		int equalsPos = line.indexOf('=');
		if (equalsPos == -1) return;
		
		if (positionInLine <= equalsPos) {
			// We're on the left side of equals (variable definition)
			// Find the start of the current word
			int wordStart = positionInLine - 1;
			while (wordStart >= 0 && Character.isJavaIdentifierPart(line.charAt(wordStart))) {
				wordStart--;
			}
			wordStart++;
			
			String leftSide = line.substring(wordStart, positionInLine);
			context.partialText = leftSide;
			context.defineVariable = leftSide;
			context.replacementOffset = context.replacementOffset - positionInLine + wordStart;
			context.replacementLength = leftSide.length();
		} else {
			// We're on the right side of equals (value assignment)
			String beforeCursor = line.substring(equalsPos + 1, positionInLine);
			context.defineVariable = line.substring(0, equalsPos).trim();
			
			// Find the start of the current expression (including dots)
			int expressionStart = equalsPos + 1;
			while (expressionStart < positionInLine && Character.isWhitespace(line.charAt(expressionStart))) {
				expressionStart++; // Skip whitespace after =
			}
			
			// Find the actual start of the current word/expression
			int currentStart = positionInLine - 1;
			while (currentStart >= expressionStart) {
				char ch = line.charAt(currentStart);
				if (!Character.isJavaIdentifierPart(ch) && ch != '.') {
					break;
				}
				currentStart--;
			}
			currentStart++;
			
			String currentExpression = line.substring(currentStart, positionInLine);
			
			int lastDotIndex = currentExpression.lastIndexOf('.');
			if (lastDotIndex >= 0) {
				context.isDotCompletion = true;
				context.objectName = currentExpression.substring(0, lastDotIndex).trim();
				context.partialText = currentExpression.substring(lastDotIndex + 1);
				context.replacementOffset = context.replacementOffset - positionInLine + currentStart + lastDotIndex + 1;
				context.replacementLength = context.partialText.length();
			} else {
				context.partialText = currentExpression;
				context.replacementOffset = context.replacementOffset - positionInLine + currentStart;
				context.replacementLength = currentExpression.length();
			}
		}
	}
	

	private void parseRegularContext(IDocument document, int offset, PropCompletionContext context) {
		try {
			int expressionStart = offset - 1;
			while (expressionStart >= 0) {
				char ch = document.getChar(expressionStart);
				if (!Character.isJavaIdentifierPart(ch) && ch != '.') {
					break;
				}
				expressionStart--;
			}
			expressionStart++;
			
			String fullExpression = "";
			if (expressionStart < offset) {
				fullExpression = document.get(expressionStart, offset - expressionStart);
			}
			
			int lastDotIndex = fullExpression.lastIndexOf('.');
			if (lastDotIndex >= 0) {
				context.isDotCompletion = true;
				context.objectName = fullExpression.substring(0, lastDotIndex);
				context.partialText = fullExpression.substring(lastDotIndex + 1);
				context.replacementOffset = expressionStart + lastDotIndex + 1;
				context.replacementLength = context.partialText.length();
			} else {
				context.partialText = fullExpression;
				context.replacementOffset = expressionStart;
				context.replacementLength = context.partialText.length();
			}
			
		} catch (BadLocationException e) {
			System.err.println("Error parsing regular context: " + e.getMessage());
		}
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		ArrayList<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
		
		try {
			
			IDocument document = viewer.getDocument();
			
			if (offset <= 0 || offset > document.getLength()) {
				return new ICompletionProposal[0];
			}
			
			PropCompletionContext context = getCompletionContext(document, offset);
			
			
			if (!context.isDefineContext || !context.isDotCompletion) {
				addFilteredKeywordCompletions(context.partialText, context.replacementOffset, 
					context.replacementLength, proposals);
			}
			
			if (context.isDefineContext) {
				addDefineContextCompletions(document, context, proposals);
			}
			
			if (!context.isDotCompletion) {
				addDefinedVariables(document, context, proposals);
			}
			
			return proposals.toArray(new ICompletionProposal[proposals.size()]);
			
		} catch (Exception e) {
			System.err.println("RebecaPropContextAwareCompletionProcessor error: " + e.getMessage());
			e.printStackTrace();
			return new ICompletionProposal[0];
		}
	}
	

	private void addFilteredKeywordCompletions(String partialText, int replacementOffset, int replacementLength,
			ArrayList<ICompletionProposal> proposals) {
		for (String keyword : keywords) {
			if (keyword.toLowerCase().startsWith(partialText.toLowerCase())) {
				proposals.add(new CompletionProposal(keyword, replacementOffset, 
					replacementLength, keyword.length()));
			}
		}
	}
	
	/**
	 * Add completions for define context (cross-file context awareness)
	 */
	private void addDefineContextCompletions(IDocument document, PropCompletionContext context, 
			ArrayList<ICompletionProposal> proposals) {
		
		if (context.isDotCompletion) {
			addCrossFileCompletions(context, proposals);
		} else {
			addMainMethodInstanceCompletions(context, proposals);
		}
	}
	

	private void addCrossFileCompletions(PropCompletionContext context, ArrayList<ICompletionProposal> proposals) {
		try {
			String rebecaFilePath = getCorrespondingRebecaFile();
			if (rebecaFilePath == null) return;
			
			File rebecaFile = new File(rebecaFilePath);
			if (!rebecaFile.exists()) return;
			
			RebecaModel rebecaModel = compileRebecaFile(rebecaFile);
			if (rebecaModel == null) return;
			
			// Find the object type in main method
			Type objectType = findObjectTypeInMain(rebecaModel, context.objectName);
			if (objectType == null) return;
			
			// Find the reactive class and suggest its state variables
			addStateVariablesFromClass(rebecaModel, objectType, context, proposals);
			
		} catch (Exception e) {
			System.err.println("Error adding cross-file completions: " + e.getMessage());
		}
	}
	
	/**
	 * Add class instances from main method
	 */
	private void addMainMethodInstanceCompletions(PropCompletionContext context, ArrayList<ICompletionProposal> proposals) {
		try {
			String rebecaFilePath = getCorrespondingRebecaFile();
			if (rebecaFilePath == null) return;
			
			File rebecaFile = new File(rebecaFilePath);
			if (!rebecaFile.exists()) return;
			
			RebecaModel rebecaModel = compileRebecaFile(rebecaFile);
			if (rebecaModel == null) return;
			
			if (rebecaModel.getRebecaCode().getMainDeclaration() != null &&
				rebecaModel.getRebecaCode().getMainDeclaration().getMainRebecDefinition() != null) {
				
				for (MainRebecDefinition mrd : rebecaModel.getRebecaCode().getMainDeclaration().getMainRebecDefinition()) {
					if (mrd.getName() != null) {
						if (context.partialText.isEmpty() || 
							mrd.getName().toLowerCase().startsWith(context.partialText.toLowerCase())) {
							proposals.add(new CompletionProposal(mrd.getName(), 
								context.replacementOffset, context.replacementLength, mrd.getName().length()));
						}
					}
				}
			}
			
		} catch (Exception e) {
			System.err.println("Error adding main method completions: " + e.getMessage());
		}
	}
	

	private String getCorrespondingRebecaFile() {
		try {
			if (editor.getEditorInput() != null) {
				String propertyPath = editor.getEditorInput().getName();
				if (propertyPath.endsWith(".property")) {
					String baseName = propertyPath.substring(0, propertyPath.lastIndexOf(".property"));
					String directory = "";
					if (editor.getEditorInput() instanceof org.eclipse.ui.part.FileEditorInput) {
						org.eclipse.ui.part.FileEditorInput fileInput = (org.eclipse.ui.part.FileEditorInput) editor.getEditorInput();
						directory = fileInput.getFile().getParent().getLocation().toOSString();
					}
					return directory + File.separator + baseName + ".rebeca";
				}
			}
		} catch (Exception e) {
			System.err.println("Error getting corresponding rebeca file: " + e.getMessage());
		}
		return null;
	}
	

	private RebecaModel compileRebecaFile(File rebecaFile) {
		try {
			IProject project = CompilationAndCodeGenerationProcess.getProject();
			if (project == null) return null;
			
			Set<CompilerExtension> compilationExtensions = 
					CompilationAndCodeGenerationProcess.retrieveCompationExtension(project);
			
			CoreVersion version = CoreRebecaProjectPropertyPage.getProjectLanguageVersion(project);
			
			Pair<RebecaModel,SymbolTable> compilationResult = 
					modelCompiler.compileRebecaFile(rebecaFile, compilationExtensions, version);
			
			return compilationResult.getFirst();
			
		} catch (Exception e) {
			System.err.println("Error compiling rebeca file: " + e.getMessage());
			return null;
		}
	}
	

	private Type findObjectTypeInMain(RebecaModel rebecaModel, String objectName) {
		if (rebecaModel.getRebecaCode().getMainDeclaration() != null &&
			rebecaModel.getRebecaCode().getMainDeclaration().getMainRebecDefinition() != null) {
			
			for (MainRebecDefinition mrd : rebecaModel.getRebecaCode().getMainDeclaration().getMainRebecDefinition()) {
				if (objectName.equals(mrd.getName())) {
					return mrd.getType();
				}
			}
		}
		return null;
	}
	

	private void addStateVariablesFromClass(RebecaModel rebecaModel, Type classType, PropCompletionContext context,
			ArrayList<ICompletionProposal> proposals) {
		
		if (rebecaModel.getRebecaCode().getReactiveClassDeclaration() != null) {
			for (ReactiveClassDeclaration rcd : rebecaModel.getRebecaCode().getReactiveClassDeclaration()) {
				if (classType.getTypeName().equals(rcd.getName())) {
					if (rcd.getStatevars() != null) {
						for (FieldDeclaration fd : rcd.getStatevars()) {
							if (fd.getVariableDeclarators() != null) {
								for (VariableDeclarator vd : fd.getVariableDeclarators()) {
									if (vd.getVariableName() != null) {
										if (context.partialText.isEmpty() || 
											vd.getVariableName().toLowerCase().startsWith(context.partialText.toLowerCase())) {
											proposals.add(new CompletionProposal(vd.getVariableName(), 
												context.replacementOffset, context.replacementLength, vd.getVariableName().length()));
										}
									}
								}
							}
						}
					}
					break;
				}
			}
		}
	}
	

	private void addDefinedVariables(IDocument document, PropCompletionContext context, 
			ArrayList<ICompletionProposal> proposals) {
		
		if (context.isDefineContext && context.isDotCompletion) {
			return;
		}
		
		try {
			String documentContent = document.get();
			
			// match define statements: variableName = ...;
			Pattern definePattern = Pattern.compile("\\s*(\\w+)\\s*=\\s*[^;]+;", Pattern.MULTILINE);
			Matcher matcher = definePattern.matcher(documentContent);
			
			while (matcher.find()) {
				String definedVar = matcher.group(1);
				if (definedVar != null && !definedVar.equals(context.defineVariable)) {
					if (context.partialText.isEmpty() || 
						definedVar.toLowerCase().startsWith(context.partialText.toLowerCase())) {
						proposals.add(new CompletionProposal(definedVar, 
							context.replacementOffset, context.replacementLength, definedVar.length()));
					}
				}
			}
			
		} catch (Exception e) {
			System.err.println("Error adding defined variables: " + e.getMessage());
		}
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		return null;
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ.".toCharArray();
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

	@Override
	public String getErrorMessage() {
		return null;
	}
}
