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


public class CompletionProcessor implements IContentAssistProcessor {
   
	@Autowired
	RebecaModelCompiler modelCompiler;
	
	private RebecaEditor editor;
	private static final String[] keywords = {
		"reactiveclass", "knownrebecs", "statevars", "msgsrv", "main", 
		"if", "else", "self", "true", "false", "for", "while", "break", 
		"after", "deadline", "delay", "sender"
	};
	private static final String[] types = {"boolean", "byte", "int", "short"};
	
	public CompletionProcessor(RebecaEditor editor) {
		this.editor = editor;
		@SuppressWarnings("resource")
		ApplicationContext context = new AnnotationConfigApplicationContext(RMCConfig.class, CompilerConfig.class);
		AutowireCapableBeanFactory factory = context.getAutowireCapableBeanFactory();
		factory.autowireBean(this);
	}

	private int getWordStartIndex(IDocument document, int offset) throws BadLocationException {
		if (offset <= 0) return 0;
		
		int index = offset - 1;
		while (index >= 0) {
			char ch = document.getChar(index);
			if (!Character.isJavaIdentifierPart(ch) && ch != '.') {
				break;
			}
			index--;
		}
		return index + 1;
	}

	private int getWordEndIndex(IDocument document, int offset) throws BadLocationException {
		int index = offset;
		int length = document.getLength();
		
		while (index < length) {
			char ch = document.getChar(index);
			if (!Character.isJavaIdentifierPart(ch) && ch != '.') {
				break;
			}
			index++;
		}
		return index;
	}
	
	private String getCurrentWord(IDocument document, int offset) {
		try {
			if (offset <= 0 || offset > document.getLength()) {
				return "";
			}
			
			int startIndex = getWordStartIndex(document, offset);
			int endIndex = getWordEndIndex(document, offset);
			
			if (startIndex >= endIndex || startIndex < 0 || endIndex > document.getLength()) {
				return "";
			}
			
			return document.get(startIndex, endIndex - startIndex);
		} catch (Exception e) {
			System.err.println("Error getting current word: " + e.getMessage());
			return "";
		}
	}

	private void addAllMethodsAndFields(SymbolTable symbolTable, Type classType, int offset, ArrayList<ICompletionProposal> proposals) {
		try {
			if (symbolTable == null || classType == null) {
				return;
			}
			
			if (symbolTable.getmethodSymbolTable() != null && symbolTable.getmethodSymbolTable().get(classType) != null) {
				Enumeration<String> keys = symbolTable.getmethodSymbolTable().get(classType).keys();
				while (keys.hasMoreElements()) {
					String suggestion = keys.nextElement();
					if (suggestion != null && !suggestion.isEmpty()) {
						proposals.add(new CompletionProposal(suggestion + "()", offset, 0, suggestion.length() + 1));
					}
				}
			}
			
			if (symbolTable.getVariableSymbolTable() != null && symbolTable.getVariableSymbolTable().get(classType) != null) {
				Enumeration<String> keys = symbolTable.getVariableSymbolTable().get(classType).keys();
				while (keys.hasMoreElements()) {
					String suggestion = keys.nextElement();
					if (suggestion != null && !suggestion.isEmpty()) {
						proposals.add(new CompletionProposal(suggestion, offset, 0, suggestion.length()));
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Error adding methods and fields: " + e.getMessage());
		}
	}
	
	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		ArrayList<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
		
		try {			
			IDocument document = viewer.getDocument();
			if (offset <= 0 || offset > document.getLength()) {
				System.out.println("Invalid offset, returning empty proposals");
				return new ICompletionProposal[0];
			}
						String currentWord = getCurrentWord(document, offset);
			if (currentWord == null) {
				currentWord = "";
			}
			
			
			addBasicCompletions(currentWord, offset, proposals);
						
			try {
				addAdvancedCompletions(document, offset, currentWord, proposals);
			} catch (Exception e) {
				System.err.println("Advanced completion failed: " + e.getMessage());
			}
			
			return proposals.toArray(new ICompletionProposal[proposals.size()]);
			
		} catch (Exception e) {
			System.err.println("CompletionProcessor error: " + e.getMessage());
			e.printStackTrace();
			return new ICompletionProposal[0];
		}	
	}
	
	private void addBasicCompletions(String currentWord, int offset, ArrayList<ICompletionProposal> proposals) {
		for (String keyword : keywords) {
			if (keyword.toLowerCase().startsWith(currentWord.toLowerCase())) {
				proposals.add(new CompletionProposal(keyword, offset - currentWord.length(), 
					currentWord.length(), keyword.length()));
			}
		}
		
		for (String type : types) {
			if (type.toLowerCase().startsWith(currentWord.toLowerCase())) {
				proposals.add(new CompletionProposal(type, offset - currentWord.length(), 
					currentWord.length(), type.length()));
			}
		}
	}
	
	private void addAdvancedCompletions(IDocument document, int offset, String currentWord, 
			ArrayList<ICompletionProposal> proposals) throws Exception {
		
		File tempRebecaFile = File.createTempFile("AfraTempFile", "model.rebeca");
		try {
			FileWriter fstream = new FileWriter(tempRebecaFile);
			BufferedWriter tempRebecaFileWriter = new BufferedWriter(fstream);
			tempRebecaFileWriter.write(document.get());
			tempRebecaFileWriter.close();

			IProject project = CompilationAndCodeGenerationProcess.getProject();
			if (project == null) {
				return;
			}
			
			Set<CompilerExtension> compationExtensions = 
					CompilationAndCodeGenerationProcess.retrieveCompationExtension(project);
			
			CoreVersion version = CoreRebecaProjectPropertyPage.getProjectLanguageVersion(project);
			
			Pair<RebecaModel,SymbolTable> compilationResult = 
					modelCompiler.compileRebecaFile(tempRebecaFile, compationExtensions, version);
			
			RebecaModel rebecaModel = compilationResult.getFirst();
			SymbolTable symbolTable = compilationResult.getSecond();
			
			if (rebecaModel == null || rebecaModel.getRebecaCode() == null) {
				return;
			}
			
			int lineNumber = document.getLineOfOffset(offset);
			
			// Check for suggestion inside main
			if (rebecaModel.getRebecaCode().getMainDeclaration() != null &&
				lineNumber >= rebecaModel.getRebecaCode().getMainDeclaration().getLineNumber() &&
				lineNumber <= rebecaModel.getRebecaCode().getMainDeclaration().getEndLineNumber()) {
				
				addMainContextCompletions(rebecaModel, symbolTable, currentWord, offset, proposals);
			}
			else {
				// Check for suggestions inside class
				addClassContextCompletions(rebecaModel, symbolTable, currentWord, offset, lineNumber, proposals);
			}
			
		} finally {
			if (tempRebecaFile.exists()) {
				tempRebecaFile.delete();
			}
		}
	}
	
	private void addMainContextCompletions(RebecaModel rebecaModel, SymbolTable symbolTable, 
			String currentWord, int offset, ArrayList<ICompletionProposal> proposals) {
		
		if (rebecaModel.getRebecaCode().getMainDeclaration() == null || 
			rebecaModel.getRebecaCode().getMainDeclaration().getMainRebecDefinition() == null) {
			if (isInInstantiationContext(currentWord)) {
				addClassNames(rebecaModel, currentWord, offset, proposals);
			}
			return;
		}
		
		//check for methods and fields with dot notation
		if (currentWord.length() > 0 && currentWord.charAt(currentWord.length()-1) == '.') {
			String objectName = currentWord.substring(0, currentWord.length() - 1);
			for (MainRebecDefinition mrd : rebecaModel.getRebecaCode().getMainDeclaration().getMainRebecDefinition()) {
				if (mrd.getName() != null && mrd.getName().equals(objectName)) {
					addAllMethodsAndFields(symbolTable, mrd.getType(), offset, proposals);
				}
			}
		}
		//regular completion (no dot)
		else {
			// check for local variables (rebec instances in main)					
			for (MainRebecDefinition mrd : rebecaModel.getRebecaCode().getMainDeclaration().getMainRebecDefinition()) {
				if (mrd.getName() != null && mrd.getName().toLowerCase().startsWith(currentWord.toLowerCase())) {
					proposals.add(new CompletionProposal(mrd.getName(), offset - currentWord.length(), 
						currentWord.length(), mrd.getName().length()));
				}
			}
			
			// add class names for instantiation in main section
			if (isInInstantiationContext(currentWord)) {
				addClassNames(rebecaModel, currentWord, offset, proposals);
			}
		}
	}
	
	private void addClassContextCompletions(RebecaModel rebecaModel, SymbolTable symbolTable, 
			String currentWord, int offset, int lineNumber, ArrayList<ICompletionProposal> proposals) {
		
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
				
				// check for method calls with dot notation
				if (currentWord.length() > 0 && currentWord.charAt(currentWord.length()-1) == '.') {
					String objectName = currentWord.substring(0, currentWord.length() - 1);

					// check for self methods
					if (objectName.equals("self")) {
						List<MethodDeclaration> methods = new ArrayList<>();
						if (rcd.getMsgsrvs() != null) {
							methods.addAll(rcd.getMsgsrvs());
						}
						if (rcd.getSynchMethods() != null) {
							methods.addAll(rcd.getSynchMethods());
						}
						
						for (MethodDeclaration md : methods) {
							if (md.getName() != null) {
								proposals.add(new CompletionProposal(md.getName() + "()", offset, 0, 
									md.getName().length() + 1));
							}
						}
					}
					//check for methods for other classes
					else {								
						for (FieldDeclaration fd : fields) {
							if (fd.getVariableDeclarators() != null) {
								for (VariableDeclarator vd : fd.getVariableDeclarators()) {	
									if (vd.getVariableName() != null && vd.getVariableName().equals(objectName)) {
										addAllMethodsAndFields(symbolTable, fd.getType(), offset, proposals);
									}								
								}
							}
						}
					}
				}
				// regular variable and method name completion (no dot)
				else {
					// check for statevars and knownrebecs variables
					for (FieldDeclaration fd : fields) {
						if (fd.getVariableDeclarators() != null) {
							for (VariableDeclarator vd : fd.getVariableDeclarators()) {	
								if (vd.getVariableName() != null && vd.getVariableName().toLowerCase().startsWith(currentWord.toLowerCase())) {
									proposals.add(new CompletionProposal(vd.getVariableName(), 
										offset - currentWord.length(), currentWord.length(), vd.getVariableName().length()));
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
						if (md.getName() != null && md.getName().toLowerCase().startsWith(currentWord.toLowerCase())) {
							proposals.add(new CompletionProposal(md.getName(), 
								offset - currentWord.length(), currentWord.length(), md.getName().length()));
						}
					}
					
					if (rcd.getName() != null && rcd.getName().toLowerCase().startsWith(currentWord.toLowerCase())) {
						proposals.add(new CompletionProposal(rcd.getName(), 
							offset - currentWord.length(), currentWord.length(), rcd.getName().length()));
					}
					
					if (isInInstantiationContext(currentWord)) {
						addClassNames(rebecaModel, currentWord, offset, proposals);
					}
				}
				break;
			}
		}
	}
	
	private boolean isInInstantiationContext(String currentWord) {
		return currentWord.length() > 0 && Character.isUpperCase(currentWord.charAt(0));
	}
	
	private void addClassNames(RebecaModel rebecaModel, String currentWord, int offset, ArrayList<ICompletionProposal> proposals) {
		if (rebecaModel.getRebecaCode().getReactiveClassDeclaration() != null) {
			for (ReactiveClassDeclaration rcd : rebecaModel.getRebecaCode().getReactiveClassDeclaration()) {
				if (rcd.getName() != null && rcd.getName().toLowerCase().startsWith(currentWord.toLowerCase())) {
					proposals.add(new CompletionProposal(rcd.getName(), 
						offset - currentWord.length(), currentWord.length(), rcd.getName().length()));
				}
			}
		}
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer,
			int offset) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return "abcdefghijklmnopqrstuvwxyz.({[".toCharArray();
	}
	

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	public String getErrorMessage() {
		// TODO Auto-generated method stub
		return null;
	}

}