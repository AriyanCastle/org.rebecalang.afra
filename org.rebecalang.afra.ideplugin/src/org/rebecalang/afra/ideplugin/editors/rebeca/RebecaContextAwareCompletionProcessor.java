package org.rebecalang.afra.ideplugin.editors.rebeca;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

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
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.FormalParameterDeclaration;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.MainRebecDefinition;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.MethodDeclaration;
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
 * Enhanced completion processor for Rebeca language that properly handles
 * dot notation completion and filtering of suggestions.
 */
public class RebecaContextAwareCompletionProcessor implements IContentAssistProcessor {

	@Autowired
	RebecaModelCompiler modelCompiler;
	
	private RebecaEditor editor;
	private static String[] keywords = {
		"reactiveclass", "knownrebecs", "statevars", "msgsrv", "main", 
		"if", "else", "self", "true", "false", "for", "while", "break", 
		"after", "deadline", "delay", "sender"
	};
	private static String[] types = {"boolean", "byte", "int", "short"};
	
	/**
	 * Represents the context of what the user is typing
	 */
	private static class CompletionContext {
		public String objectName;      // Part before the dot (e.g., "self" in "self.arrive")
		public String partialText;     // Part after the dot (e.g., "ar" in "self.ar")
		public boolean isDotCompletion; // True if user is completing after a dot
		public int replacementOffset;   // Offset where replacement should start
		public int replacementLength;   // Length of text to replace
	}
	
	public RebecaContextAwareCompletionProcessor(RebecaEditor editor) {
		this.editor = editor;
		@SuppressWarnings("resource")
		ApplicationContext context = new AnnotationConfigApplicationContext(RMCConfig.class, CompilerConfig.class);
		AutowireCapableBeanFactory factory = context.getAutowireCapableBeanFactory();
		factory.autowireBean(this);
	}

	/**
	 * Enhanced method to determine completion context, properly handling dot notation
	 */
	private CompletionContext getCompletionContext(IDocument document, int offset) {
		CompletionContext context = new CompletionContext();
		
		try {
			if (offset <= 0 || offset > document.getLength()) {
				context.partialText = "";
				context.isDotCompletion = false;
				context.replacementOffset = offset;
				context.replacementLength = 0;
				return context;
			}
			
			// Find the start of the current expression (stop at whitespace, operators, etc.)
			int expressionStart = offset - 1;
			while (expressionStart >= 0) {
				char ch = document.getChar(expressionStart);
				if (!Character.isJavaIdentifierPart(ch) && ch != '.') {
					break;
				}
				expressionStart--;
			}
			expressionStart++; // Move to first valid character
			
			// Extract the full expression up to the cursor
			String fullExpression = "";
			if (expressionStart < offset) {
				fullExpression = document.get(expressionStart, offset - expressionStart);
			}
			
			// Check if this is a dot completion
			int lastDotIndex = fullExpression.lastIndexOf('.');
			if (lastDotIndex >= 0) {
				// This is a dot completion
				context.isDotCompletion = true;
				context.objectName = fullExpression.substring(0, lastDotIndex);
				context.partialText = fullExpression.substring(lastDotIndex + 1);
				context.replacementOffset = expressionStart + lastDotIndex + 1;
				context.replacementLength = context.partialText.length();
			} else {
				// Regular completion (no dot)
				context.isDotCompletion = false;
				context.objectName = null;
				context.partialText = fullExpression;
				context.replacementOffset = expressionStart;
				context.replacementLength = context.partialText.length();
			}
			
		} catch (BadLocationException e) {
			System.err.println("Error determining completion context: " + e.getMessage());
			context.partialText = "";
			context.isDotCompletion = false;
			context.replacementOffset = offset;
			context.replacementLength = 0;
		}
		
		return context;
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		ArrayList<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
		
		try {
			System.out.println("RebecaContextAwareCompletionProcessor.computeCompletionProposals called with offset: " + offset);
			
			IDocument document = viewer.getDocument();
			
			// Safety check for offset
			if (offset < 0 || offset > document.getLength()) {
				System.out.println("Invalid offset, returning empty proposals");
				return new ICompletionProposal[0];
			}
			
			// Get enhanced completion context
			CompletionContext context = getCompletionContext(document, offset);
			
			System.out.println("Completion context - isDot: " + context.isDotCompletion + 
					", object: '" + context.objectName + "', partial: '" + context.partialText + "'");
			
			// If this is not a dot completion, provide basic completions
			if (!context.isDotCompletion) {
				addFilteredBasicCompletions(context.partialText, context.replacementOffset, context.replacementLength, proposals);
			}
			
			// Try advanced completions
			try {
				addEnhancedAdvancedCompletions(document, offset, context, proposals);
				System.out.println("Total completions: " + proposals.size());
			} catch (Exception e) {
				System.err.println("Advanced completion failed: " + e.getMessage());
				// Continue with basic completions only
			}
			
			return proposals.toArray(new ICompletionProposal[proposals.size()]);
			
		} catch (Exception e) {
			System.err.println("RebecaContextAwareCompletionProcessor error: " + e.getMessage());
			e.printStackTrace();
			return new ICompletionProposal[0];
		}	
	}
	
	/**
	 * Add basic completions (keywords, types) with proper filtering
	 */
	private void addFilteredBasicCompletions(String partialText, int replacementOffset, int replacementLength, 
			ArrayList<ICompletionProposal> proposals) {
		
		// Add keywords - ensure case-insensitive matching for better UX
		for (String keyword : keywords) {
			if (keyword.toLowerCase().startsWith(partialText.toLowerCase())) {
				proposals.add(new CompletionProposal(keyword, replacementOffset, 
					replacementLength, keyword.length()));
			}
		}
		
		// Add types
		for (String type : types) {
			if (type.toLowerCase().startsWith(partialText.toLowerCase())) {
				proposals.add(new CompletionProposal(type, replacementOffset, 
					replacementLength, type.length()));
			}
		}
	}
	
	/**
	 * Enhanced advanced completions with better error handling and context awareness
	 */
	private void addEnhancedAdvancedCompletions(IDocument document, int offset, CompletionContext context, 
			ArrayList<ICompletionProposal> proposals) throws Exception {
		
		File tempRebecaFile = File.createTempFile("AfraTempFile", "model.rebeca");
		try {
			// Create a more robust temporary file for compilation
			String documentContent = document.get();
			
			// If we're in the middle of typing, try to make the document parseable
			// by temporarily completing the current statement
			String modifiedContent = makeDocumentParseable(documentContent, offset);
			
			FileWriter fstream = new FileWriter(tempRebecaFile);
			BufferedWriter tempRebecaFileWriter = new BufferedWriter(fstream);
			tempRebecaFileWriter.write(modifiedContent);
			tempRebecaFileWriter.close();

			IProject project = CompilationAndCodeGenerationProcess.getProject();
			if (project == null) {
				return; // No project context, skip advanced completions
			}
			
			Set<CompilerExtension> compationExtensions = 
					CompilationAndCodeGenerationProcess.retrieveCompationExtension(project);
			
			CoreVersion version = CoreRebecaProjectPropertyPage.getProjectLanguageVersion(project);
			
			Pair<RebecaModel,SymbolTable> compilationResult = null;
			try {
				compilationResult = modelCompiler.compileRebecaFile(tempRebecaFile, compationExtensions, version);
			} catch (Exception e) {
				System.err.println("Compilation failed, attempting fallback: " + e.getMessage());
				// Try with original content as fallback
				fstream = new FileWriter(tempRebecaFile);
				tempRebecaFileWriter = new BufferedWriter(fstream);
				tempRebecaFileWriter.write(documentContent);
				tempRebecaFileWriter.close();
				
				try {
					compilationResult = modelCompiler.compileRebecaFile(tempRebecaFile, compationExtensions, version);
				} catch (Exception e2) {
					System.err.println("Fallback compilation also failed: " + e2.getMessage());
					return; // Give up on advanced completions
				}
			}
			
			if (compilationResult == null) {
				return;
			}
			
			RebecaModel rebecaModel = compilationResult.getFirst();
			SymbolTable symbolTable = compilationResult.getSecond();
			
			if (rebecaModel == null || rebecaModel.getRebecaCode() == null) {
				return; // Compilation failed, skip advanced completions
			}
			
			int lineNumber = document.getLineOfOffset(offset);
			
			// Check for suggestion inside main
			if (rebecaModel.getRebecaCode().getMainDeclaration() != null &&
				lineNumber >= rebecaModel.getRebecaCode().getMainDeclaration().getLineNumber() &&
				lineNumber <= rebecaModel.getRebecaCode().getMainDeclaration().getEndLineNumber()) {
				
				addContextAwareMainCompletions(rebecaModel, symbolTable, context, proposals);
			}
			else {
				// Check for suggestions inside class
				addContextAwareClassCompletions(rebecaModel, symbolTable, context, lineNumber, proposals);
			}
			
		} finally {
			// Clean up temp file
			if (tempRebecaFile.exists()) {
				tempRebecaFile.delete();
			}
		}
	}
	
	/**
	 * Try to make the document more parseable by fixing obvious syntax issues
	 */
	private String makeDocumentParseable(String content, int offset) {
		// This is a simple heuristic - in a real implementation, you might want more sophisticated logic
		try {
			// Check if we're in the middle of a line and the line doesn't end with ';'
			String[] lines = content.split("\n");
			int lineIndex = 0;
			int currentPos = 0;
			
			// Find which line the offset is on
			for (int i = 0; i < lines.length; i++) {
				if (currentPos + lines[i].length() >= offset) {
					lineIndex = i;
					break;
				}
				currentPos += lines[i].length() + 1; // +1 for newline
			}
			
			// If the current line doesn't end with ';' or '}' and looks like a statement, add ';'
			if (lineIndex < lines.length) {
				String currentLine = lines[lineIndex].trim();
				if (!currentLine.isEmpty() && 
					!currentLine.endsWith(";") && 
					!currentLine.endsWith("{") && 
					!currentLine.endsWith("}") &&
					!currentLine.startsWith("//") &&
					!currentLine.contains("//") &&
					(currentLine.contains(".") || currentLine.contains("="))) {
					
					// Insert a semicolon at the end of the current line
					StringBuilder sb = new StringBuilder(content);
					int lineEnd = currentPos + lines[lineIndex].length();
					if (lineEnd <= content.length()) {
						sb.insert(lineEnd, ";");
						return sb.toString();
					}
				}
			}
		} catch (Exception e) {
			// If anything goes wrong, just return the original content
			System.err.println("Error making document parseable: " + e.getMessage());
		}
		
		return content;
	}
	
	/**
	 * Context-aware main completions with proper filtering
	 */
	private void addContextAwareMainCompletions(RebecaModel rebecaModel, SymbolTable symbolTable, 
			CompletionContext context, ArrayList<ICompletionProposal> proposals) {
		
		if (rebecaModel.getRebecaCode().getMainDeclaration() == null || 
			rebecaModel.getRebecaCode().getMainDeclaration().getMainRebecDefinition() == null) {
			// If we're in main but no objects defined yet, suggest class names for instantiation
			if (!context.isDotCompletion && isInInstantiationContext(context.partialText)) {
				addFilteredClassNames(rebecaModel, context, proposals);
			}
			return;
		}
		
		// Handle dot completions
		if (context.isDotCompletion) {
			for (MainRebecDefinition mrd : rebecaModel.getRebecaCode().getMainDeclaration().getMainRebecDefinition()) {
				if (mrd.getName() != null && mrd.getName().equals(context.objectName)) {
					addFilteredMethodsAndFields(symbolTable, mrd.getType(), context, proposals);
				}
			}
		}
		// Regular completion (no dot)
		else {
			// Check for local variables (rebec instances in main)					
			for (MainRebecDefinition mrd : rebecaModel.getRebecaCode().getMainDeclaration().getMainRebecDefinition()) {
				if (mrd.getName() != null && mrd.getName().toLowerCase().startsWith(context.partialText.toLowerCase())) {
					proposals.add(new CompletionProposal(mrd.getName(), context.replacementOffset, 
						context.replacementLength, mrd.getName().length()));
				}
			}
			
			// Add class names for instantiation in main section
			if (isInInstantiationContext(context.partialText)) {
				addFilteredClassNames(rebecaModel, context, proposals);
			}
		}
	}
	
	/**
	 * Context-aware class completions with proper filtering
	 */
	private void addContextAwareClassCompletions(RebecaModel rebecaModel, SymbolTable symbolTable, 
			CompletionContext context, int lineNumber, ArrayList<ICompletionProposal> proposals) {
		
		if (rebecaModel.getRebecaCode().getReactiveClassDeclaration() == null) {
			return;
		}
		
		for(ReactiveClassDeclaration rcd : rebecaModel.getRebecaCode().getReactiveClassDeclaration()) {
			if (lineNumber >= rcd.getLineNumber() && lineNumber <= rcd.getEndLineNumber()) {
				
				// Getting all statevars and knownrebecs
				List<FieldDeclaration> fields = new ArrayList<>();
				if (rcd.getKnownRebecs() != null) {
					fields.addAll(rcd.getKnownRebecs());
				}
				if (rcd.getStatevars() != null) {
					fields.addAll(rcd.getStatevars());
				}
				
				// Add method parameter completions if we're inside a method
				MethodDeclaration currentMethod = getCurrentMethod(rcd, lineNumber);
				
				// Handle dot completions
				if (context.isDotCompletion) {
					// Check for self methods
					if ("self".equals(context.objectName)) {
						List<MethodDeclaration> methods = new ArrayList<>();
						if (rcd.getMsgsrvs() != null) {
							methods.addAll(rcd.getMsgsrvs());
						}
						if (rcd.getSynchMethods() != null) {
							methods.addAll(rcd.getSynchMethods());
						}
						
						for (MethodDeclaration md : methods) {
							if (md.getName() != null) {
								// If partial text is empty (user typed "self." with no additional text), show all methods
								// Otherwise filter by partial text
								if (context.partialText.isEmpty() || 
									md.getName().toLowerCase().startsWith(context.partialText.toLowerCase())) {
									proposals.add(new CompletionProposal(md.getName() + "()", 
										context.replacementOffset, context.replacementLength, 
										md.getName().length() + 1));
								}
							}
						}
					}
					// Check for methods for other objects
					else {								
						for (FieldDeclaration fd : fields) {
							if (fd.getVariableDeclarators() != null) {
								for (VariableDeclarator vd : fd.getVariableDeclarators()) {	
									if (vd.getVariableName() != null && vd.getVariableName().equals(context.objectName)) {
										addFilteredMethodsAndFields(symbolTable, fd.getType(), context, proposals);
									}								
								}
							}
						}
					}
				}
				// Regular variable and method name completion (no dot)
				else {
					// Add method parameters if inside a method
					if (currentMethod != null) {
						addMethodParameterCompletions(currentMethod, context, proposals);
					}
					
					// Check for statevars and knownrebecs variables
					for (FieldDeclaration fd : fields) {
						if (fd.getVariableDeclarators() != null) {
							for (VariableDeclarator vd : fd.getVariableDeclarators()) {	
								if (vd.getVariableName() != null && 
									vd.getVariableName().toLowerCase().startsWith(context.partialText.toLowerCase())) {
									proposals.add(new CompletionProposal(vd.getVariableName(), 
										context.replacementOffset, context.replacementLength, vd.getVariableName().length()));
								}								
							}
						}
					}
					
					// Add method names (msgsrv methods) for completion
					List<MethodDeclaration> methods = new ArrayList<>();
					if (rcd.getMsgsrvs() != null) {
						methods.addAll(rcd.getMsgsrvs());
					}
					if (rcd.getSynchMethods() != null) {
						methods.addAll(rcd.getSynchMethods());
					}
					
					for (MethodDeclaration md : methods) {
						if (md.getName() != null && 
							md.getName().toLowerCase().startsWith(context.partialText.toLowerCase())) {
							proposals.add(new CompletionProposal(md.getName(), 
								context.replacementOffset, context.replacementLength, md.getName().length()));
						}
					}
					
					// Add constructor name (same as class name)
					if (rcd.getName() != null && 
						rcd.getName().toLowerCase().startsWith(context.partialText.toLowerCase())) {
						proposals.add(new CompletionProposal(rcd.getName(), 
							context.replacementOffset, context.replacementLength, rcd.getName().length()));
					}
					
					// Add class names for instantiation (only if we're in appropriate context)
					if (isInInstantiationContext(context.partialText)) {
						addFilteredClassNames(rebecaModel, context, proposals);
					}
				}
				break;
			}
		}
	}
	
	/**
	 * Add methods and fields with proper filtering based on partial text
	 */
	private void addFilteredMethodsAndFields(SymbolTable symbolTable, Type classType, CompletionContext context,
			ArrayList<ICompletionProposal> proposals) {
		try {
			if (symbolTable == null || classType == null) {
				return;
			}
			
			// Add methods
			if (symbolTable.getmethodSymbolTable() != null && symbolTable.getmethodSymbolTable().get(classType) != null) {
				Enumeration<String> keys = symbolTable.getmethodSymbolTable().get(classType).keys();
				while (keys.hasMoreElements()) {
					String methodName = keys.nextElement();
					if (methodName != null && !methodName.isEmpty()) {
						// If partial text is empty (user typed "object." with no additional text), show all methods
						// Otherwise filter by partial text
						if (context.partialText.isEmpty() || 
							methodName.toLowerCase().startsWith(context.partialText.toLowerCase())) {
							proposals.add(new CompletionProposal(methodName + "()", 
								context.replacementOffset, context.replacementLength, methodName.length() + 1));
						}
					}
				}
			}
			
			// Add fields
			if (symbolTable.getVariableSymbolTable() != null && symbolTable.getVariableSymbolTable().get(classType) != null) {
				Enumeration<String> keys = symbolTable.getVariableSymbolTable().get(classType).keys();
				while (keys.hasMoreElements()) {
					String fieldName = keys.nextElement();
					if (fieldName != null && !fieldName.isEmpty()) {
						// If partial text is empty (user typed "object." with no additional text), show all fields
						// Otherwise filter by partial text
						if (context.partialText.isEmpty() || 
							fieldName.toLowerCase().startsWith(context.partialText.toLowerCase())) {
							proposals.add(new CompletionProposal(fieldName, 
								context.replacementOffset, context.replacementLength, fieldName.length()));
						}
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Error adding filtered methods and fields: " + e.getMessage());
		}
	}
	
	/**
	 * Add class names with proper filtering
	 */
	private void addFilteredClassNames(RebecaModel rebecaModel, CompletionContext context, 
			ArrayList<ICompletionProposal> proposals) {
		if (rebecaModel.getRebecaCode().getReactiveClassDeclaration() != null) {
			for (ReactiveClassDeclaration rcd : rebecaModel.getRebecaCode().getReactiveClassDeclaration()) {
				if (rcd.getName() != null && 
					rcd.getName().toLowerCase().startsWith(context.partialText.toLowerCase())) {
					proposals.add(new CompletionProposal(rcd.getName(), 
						context.replacementOffset, context.replacementLength, rcd.getName().length()));
				}
			}
		}
	}
	
	private boolean isInInstantiationContext(String partialText) {
		// Simple heuristic: if the current word starts with uppercase, it might be a class instantiation
		// In Rebeca, class names typically start with uppercase (Node, Customer, Agent, etc.)
		return partialText.length() > 0 && Character.isUpperCase(partialText.charAt(0));
	}
	
	/**
	 * Find the method that contains the given line number
	 */
	private MethodDeclaration getCurrentMethod(ReactiveClassDeclaration rcd, int lineNumber) {
		if (rcd.getMsgsrvs() != null) {
			for (MethodDeclaration md : rcd.getMsgsrvs()) {
				if (md.getLineNumber() <= lineNumber && lineNumber <= md.getEndLineNumber()) {
					return md;
				}
			}
		}
		if (rcd.getSynchMethods() != null) {
			for (MethodDeclaration md : rcd.getSynchMethods()) {
				if (md.getLineNumber() <= lineNumber && lineNumber <= md.getEndLineNumber()) {
					return md;
				}
			}
		}
		// Check constructor
		if (rcd.getConstructors() != null) {
			for (MethodDeclaration md : rcd.getConstructors()) {
				if (md.getLineNumber() <= lineNumber && lineNumber <= md.getEndLineNumber()) {
					return md;
				}
			}
		}
		return null;
	}
	
	/**
	 * Add method parameter completions for the current method context
	 */
	private void addMethodParameterCompletions(MethodDeclaration method, CompletionContext context, 
			ArrayList<ICompletionProposal> proposals) {
		if (method.getFormalParameters() != null) {
			for (FormalParameterDeclaration param : method.getFormalParameters()) {
				if (param.getName() != null) {
					String paramName = param.getName();
					if (paramName.toLowerCase().startsWith(context.partialText.toLowerCase())) {
						proposals.add(new CompletionProposal(paramName, 
							context.replacementOffset, context.replacementLength, paramName.length()));
					}
				}
			}
		}
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		return null;
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return "abcdefghijklmnopqrstuvwxyz.({[".toCharArray();
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
