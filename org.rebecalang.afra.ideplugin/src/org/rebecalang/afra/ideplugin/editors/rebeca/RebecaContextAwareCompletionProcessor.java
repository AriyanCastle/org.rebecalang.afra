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

public class RebecaContextAwareCompletionProcessor implements IContentAssistProcessor {

	@Autowired
	RebecaModelCompiler modelCompiler;
	
	private RebecaEditor editor;
	private static final String[] keywords = {
		"reactiveclass", "knownrebecs", "statevars", "msgsrv", "main", 
		"if", "else", "self", "true", "false", "for", "while", "break", 
		"after", "deadline", "delay", "sender"
	};
	private static final String[] types = {"boolean", "byte", "int", "short"};
	
	private static class CompletionContext {
		public String objectName;      // part before the dot (e.g., "self" in "self.arrive")
		public String partialText;     // part after the dot (e.g., "ar" in "self.ar")
		public boolean isDotCompletion; // true if user is completing after a dot
		public int replacementOffset;   // offset where replacement should start
		public int replacementLength;   // length of text to replace
	}
	
	public RebecaContextAwareCompletionProcessor(RebecaEditor editor) {
		this.editor = editor;
		@SuppressWarnings("resource")
		ApplicationContext context = new AnnotationConfigApplicationContext(RMCConfig.class, CompilerConfig.class);
		AutowireCapableBeanFactory factory = context.getAutowireCapableBeanFactory();
		factory.autowireBean(this);
	}

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
				// a dot completion
				context.isDotCompletion = true;
				context.objectName = fullExpression.substring(0, lastDotIndex);
				context.partialText = fullExpression.substring(lastDotIndex + 1);
				context.replacementOffset = expressionStart + lastDotIndex + 1;
				context.replacementLength = context.partialText.length();
			} else {
				// normal completion (no dot)
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
			IDocument document = viewer.getDocument();
			if (offset < 0 || offset > document.getLength()) {
				System.out.println("Invalid offset, returning empty proposals");
				return new ICompletionProposal[0];
			}
			
			CompletionContext context = getCompletionContext(document, offset);
			
			// if not dot, basic completions
			if (!context.isDotCompletion) {
				addFilteredBasicCompletions(context.partialText, context.replacementOffset, context.replacementLength, proposals);
			}
			
			// advanced completions
			try {
				addEnhancedAdvancedCompletions(document, offset, context, proposals);
			} catch (Exception e) {
				System.err.println("Advanced completion failed: " + e.getMessage());
			}
			
			return proposals.toArray(new ICompletionProposal[proposals.size()]);
			
		} catch (Exception e) {
			System.err.println("RebecaContextAwareCompletionProcessor error: " + e.getMessage());
			e.printStackTrace();
			return new ICompletionProposal[0];
		}	
	}

	private void addFilteredBasicCompletions(String partialText, int replacementOffset, int replacementLength, 
			ArrayList<ICompletionProposal> proposals) {
		
		for (String keyword : keywords) {
			if (keyword.toLowerCase().startsWith(partialText.toLowerCase())) {
				proposals.add(new CompletionProposal(keyword, replacementOffset, 
					replacementLength, keyword.length()));
			}
		}
		
		for (String type : types) {
			if (type.toLowerCase().startsWith(partialText.toLowerCase())) {
				proposals.add(new CompletionProposal(type, replacementOffset, 
					replacementLength, type.length()));
			}
		}
	}
	
	private void addEnhancedAdvancedCompletions(IDocument document, int offset, CompletionContext context, 
			ArrayList<ICompletionProposal> proposals) throws Exception {
		
		File tempRebecaFile = File.createTempFile("AfraTempFile", "model.rebeca");
		try {
			String documentContent = document.get();
			
			String modifiedContent = makeDocumentParseable(documentContent, offset);
			
			FileWriter fstream = new FileWriter(tempRebecaFile);
			BufferedWriter tempRebecaFileWriter = new BufferedWriter(fstream);
			tempRebecaFileWriter.write(modifiedContent);
			tempRebecaFileWriter.close();

			IProject project = CompilationAndCodeGenerationProcess.getProject();
			if (project == null) {
				return;
			}
			
			Set<CompilerExtension> compationExtensions = 
					CompilationAndCodeGenerationProcess.retrieveCompationExtension(project);
			
			CoreVersion version = CoreRebecaProjectPropertyPage.getProjectLanguageVersion(project);
			
			Pair<RebecaModel,SymbolTable> compilationResult = null;
			try {
				compilationResult = modelCompiler.compileRebecaFile(tempRebecaFile, compationExtensions, version);
			} catch (Exception e) {
				System.err.println("Compilation failed, attempting fallback: " + e.getMessage());
				fstream = new FileWriter(tempRebecaFile);
				tempRebecaFileWriter = new BufferedWriter(fstream);
				tempRebecaFileWriter.write(documentContent);
				tempRebecaFileWriter.close();
				
				try {
					compilationResult = modelCompiler.compileRebecaFile(tempRebecaFile, compationExtensions, version);
				} catch (Exception e2) {
					System.err.println("Fallback compilation also failed: " + e2.getMessage());
					return;
				}
			}
			
			if (compilationResult == null) {
				return;
			}
			
			RebecaModel rebecaModel = compilationResult.getFirst();
			SymbolTable symbolTable = compilationResult.getSecond();
			
			if (rebecaModel == null || rebecaModel.getRebecaCode() == null) {
				return;
			}
			
			int lineNumber = document.getLineOfOffset(offset);
			
			// check for suggestion inside main
			if (rebecaModel.getRebecaCode().getMainDeclaration() != null &&
				lineNumber >= rebecaModel.getRebecaCode().getMainDeclaration().getLineNumber() &&
				lineNumber <= rebecaModel.getRebecaCode().getMainDeclaration().getEndLineNumber()) {
				
				addContextAwareMainCompletions(rebecaModel, symbolTable, context, proposals);
			}
			else {
				// check for suggestions inside class
				addContextAwareClassCompletions(rebecaModel, symbolTable, context, lineNumber, proposals);
			}
			
		} finally {
			if (tempRebecaFile.exists()) {
				tempRebecaFile.delete();
			}
		}
	}
	
	
	//try to make the document more parseable by fixing obvious syntax issues
	private String makeDocumentParseable(String content, int offset) {
		try {
			String[] lines = content.split("\n");
			int lineIndex = 0;
			int currentPos = 0;
			
			for (int i = 0; i < lines.length; i++) {
				if (currentPos + lines[i].length() >= offset) {
					lineIndex = i;
					break;
				}
				currentPos += lines[i].length() + 1;
			}
			
			// the current line doesn't end with ; or } possibly a statement, add ;
			if (lineIndex < lines.length) {
				String currentLine = lines[lineIndex].trim();
				if (!currentLine.isEmpty() && 
					!currentLine.endsWith(";") && 
					!currentLine.endsWith("{") && 
					!currentLine.endsWith("}") &&
					!currentLine.startsWith("//") &&
					!currentLine.contains("//") &&
					(currentLine.contains(".") || currentLine.contains("="))) {
					
					StringBuilder sb = new StringBuilder(content);
					int lineEnd = currentPos + lines[lineIndex].length();
					if (lineEnd <= content.length()) {
						sb.insert(lineEnd, ";");
						return sb.toString();
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Error making document parseable: " + e.getMessage());
		}
		
		return content;
	}
	
	
	private void addContextAwareMainCompletions(RebecaModel rebecaModel, SymbolTable symbolTable, 
			CompletionContext context, ArrayList<ICompletionProposal> proposals) {
		
		if (rebecaModel.getRebecaCode().getMainDeclaration() == null || 
			rebecaModel.getRebecaCode().getMainDeclaration().getMainRebecDefinition() == null) {
			if (!context.isDotCompletion && isInInstantiationContext(context.partialText)) {
				addFilteredClassNames(rebecaModel, context, proposals);
			}
			return;
		}
		
		if (context.isDotCompletion) {
			for (MainRebecDefinition mrd : rebecaModel.getRebecaCode().getMainDeclaration().getMainRebecDefinition()) {
				if (mrd.getName() != null && mrd.getName().equals(context.objectName)) {
					addFilteredMethodsAndFields(symbolTable, mrd.getType(), context, proposals);
				}
			}
		}
		else {
			for (MainRebecDefinition mrd : rebecaModel.getRebecaCode().getMainDeclaration().getMainRebecDefinition()) {
				if (mrd.getName() != null && mrd.getName().toLowerCase().startsWith(context.partialText.toLowerCase())) {
					proposals.add(new CompletionProposal(mrd.getName(), context.replacementOffset, 
						context.replacementLength, mrd.getName().length()));
				}
			}
			
			if (isInInstantiationContext(context.partialText)) {
				addFilteredClassNames(rebecaModel, context, proposals);
			}
		}
	}
	
	private void addContextAwareClassCompletions(RebecaModel rebecaModel, SymbolTable symbolTable, 
			CompletionContext context, int lineNumber, ArrayList<ICompletionProposal> proposals) {
		
		if (rebecaModel.getRebecaCode().getReactiveClassDeclaration() == null) {
			return;
		}
		
		for(ReactiveClassDeclaration rcd : rebecaModel.getRebecaCode().getReactiveClassDeclaration()) {
			if (lineNumber >= rcd.getLineNumber() && lineNumber <= rcd.getEndLineNumber()) {
				
				List<FieldDeclaration> fields = new ArrayList<>();
				if (rcd.getKnownRebecs() != null) {
					fields.addAll(rcd.getKnownRebecs());
				}
				if (rcd.getStatevars() != null) {
					fields.addAll(rcd.getStatevars());
				}
				
				MethodDeclaration currentMethod = getCurrentMethod(rcd, lineNumber);
				
				if (context.isDotCompletion) {
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
								if (context.partialText.isEmpty() || 
									md.getName().toLowerCase().startsWith(context.partialText.toLowerCase())) {
									proposals.add(new CompletionProposal(md.getName() + "()", 
										context.replacementOffset, context.replacementLength, 
										md.getName().length() + 1));
								}
							}
						}
					}

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

				else {
					if (currentMethod != null) {
						addMethodParameterCompletions(currentMethod, context, proposals);
					}
					
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
					
					if (rcd.getName() != null && 
						rcd.getName().toLowerCase().startsWith(context.partialText.toLowerCase())) {
						proposals.add(new CompletionProposal(rcd.getName(), 
							context.replacementOffset, context.replacementLength, rcd.getName().length()));
					}
					
					if (isInInstantiationContext(context.partialText)) {
						addFilteredClassNames(rebecaModel, context, proposals);
					}
				}
				break;
			}
		}
	}
	
	private void addFilteredMethodsAndFields(SymbolTable symbolTable, Type classType, CompletionContext context,
			ArrayList<ICompletionProposal> proposals) {
		try {
			if (symbolTable == null || classType == null) {
				return;
			}
			
			if (symbolTable.getmethodSymbolTable() != null && symbolTable.getmethodSymbolTable().get(classType) != null) {
				Enumeration<String> keys = symbolTable.getmethodSymbolTable().get(classType).keys();
				while (keys.hasMoreElements()) {
					String methodName = keys.nextElement();
					if (methodName != null && !methodName.isEmpty()) {
						if (context.partialText.isEmpty() || 
							methodName.toLowerCase().startsWith(context.partialText.toLowerCase())) {
							proposals.add(new CompletionProposal(methodName + "()", 
								context.replacementOffset, context.replacementLength, methodName.length() + 1));
						}
					}
				}
			}
			
			if (symbolTable.getVariableSymbolTable() != null && symbolTable.getVariableSymbolTable().get(classType) != null) {
				Enumeration<String> keys = symbolTable.getVariableSymbolTable().get(classType).keys();
				while (keys.hasMoreElements()) {
					String fieldName = keys.nextElement();
					if (fieldName != null && !fieldName.isEmpty()) {
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
		return partialText.length() > 0 && Character.isUpperCase(partialText.charAt(0));
	}
	
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
		if (rcd.getConstructors() != null) {
			for (MethodDeclaration md : rcd.getConstructors()) {
				if (md.getLineNumber() <= lineNumber && lineNumber <= md.getEndLineNumber()) {
					return md;
				}
			}
		}
		return null;
	}
	
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
