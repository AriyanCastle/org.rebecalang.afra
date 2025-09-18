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
	private static String[] keywords = {"reactiveclass", "knownrebecs", "statevars", "msgsrv"};
	private static String[] types = {"boolean", "byte", "int", "short"};
	
	public CompletionProcessor(RebecaEditor editor) {
		this.editor = editor;
		@SuppressWarnings("resource")
		ApplicationContext context = new AnnotationConfigApplicationContext(RMCConfig.class, CompilerConfig.class);
		AutowireCapableBeanFactory factory = context.getAutowireCapableBeanFactory();
		factory.autowireBean(this);
	}

	private int getWordStartIndex(IDocument document, int offset) throws BadLocationException {
		int index = offset - 1;
		while (index >= 0 && document.getChar(index) != ' ' && document.getChar(index) != '\t' && document.getChar(index) != '\n') {
			index--;
		}
		return index+1;
	}

	private int getWordEndIndex(IDocument document, int offset) throws BadLocationException {
		int index = offset - 1;
		while (index < document.getLength() && document.getChar(index) != ' ' && document.getChar(index) != '\t' && document.getChar(index) != '\n') index++;
		return index;
	}
	
	private String getCurrentWord(IDocument document, int offset) throws BadLocationException {
		int startIndex = getWordStartIndex(document, offset);
		int endIndex = getWordEndIndex(document, offset);
		return document.get(startIndex, endIndex - startIndex);
	}

	private void addAllMethodsAndFields(SymbolTable symbolTable, Type classType, int offset, ArrayList<ICompletionProposal> proposals) {
		String suggestion;
		Enumeration<String> keys = symbolTable.getmethodSymbolTable().get(classType).keys();
		while (keys.hasMoreElements()) {
			suggestion = keys.nextElement();
			proposals.add(new CompletionProposal(suggestion + "()", offset, 0, suggestion.length() + 1));
		}
		
		keys = symbolTable.getVariableSymbolTable().get(classType).keys();
		while (keys.hasMoreElements()) {
			suggestion = keys.nextElement();
			proposals.add(new CompletionProposal(suggestion, offset, 0, suggestion.length()));
		}
	}
	
	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
	   
		try {
			IDocument document = viewer.getDocument();
			File tempRebecaFile = File.createTempFile("AfraTempFile", "model.rebeca");
			FileWriter fstream = new FileWriter(tempRebecaFile);
			BufferedWriter tempRebecaFileWriter = new BufferedWriter(fstream);
			tempRebecaFileWriter.write(document.get());
			tempRebecaFileWriter.close();

			IProject project = CompilationAndCodeGenerationProcess.getProject();
			Set<CompilerExtension> compationExtensions = 
					CompilationAndCodeGenerationProcess.retrieveCompationExtension(project);
			
			CoreVersion version = CoreRebecaProjectPropertyPage.getProjectLanguageVersion(project);
			
			Pair<RebecaModel,SymbolTable> compilationResult = 
					modelCompiler.compileRebecaFile(tempRebecaFile, compationExtensions, version);
			
			RebecaModel rebecaModel = compilationResult.getFirst();
			SymbolTable symbolTable = compilationResult.getSecond();
			int lineNumber = document.getLineOfOffset(offset);			
			ArrayList<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();

			// Check last letter for '{', '(' and '['
			if( document.getChar(offset-1) == '{' ) {	
				document.replace(offset-1, 1, "{}");
				this.editor.selectAndReveal(offset, 0);
			}
			else if( document.getChar(offset-1) == '(' ) {
				document.replace(offset-1, 1, "()");
				this.editor.selectAndReveal(offset, 0);
			}
			else if( document.getChar(offset-1) == '[' ) {
				document.replace(offset-1, 1, "[]");
				this.editor.selectAndReveal(offset, 0);
			}
			else {
				String currentWord = getCurrentWord(document, offset);
				
				// Check for types
				for (String s : types) {
					if (s.startsWith(currentWord)) {
						proposals.add(new CompletionProposal( s, offset-currentWord.length(), currentWord.length(), s.length()));
					}
				}
				
				// Check for suggestion inside main
				if (lineNumber >= rebecaModel.getRebecaCode().getMainDeclaration().getLineNumber() &&
					lineNumber <= rebecaModel.getRebecaCode().getMainDeclaration().getEndLineNumber()) {
						// Check for local variables						
						for (MainRebecDefinition mrd : rebecaModel.getRebecaCode().getMainDeclaration().getMainRebecDefinition()) {
							if (mrd.getName() != null && mrd.getName().startsWith(currentWord)) {
								proposals.add(new CompletionProposal( mrd.getName(), offset-currentWord.length(), currentWord.length(), mrd.getName().length()));
							}
						}

						// Check for methods and fields 
						if (currentWord.charAt(currentWord.length()-1) == '.') {
							currentWord = currentWord.substring(0, currentWord.length() - 1);
							for (MainRebecDefinition mrd : rebecaModel.getRebecaCode().getMainDeclaration().getMainRebecDefinition()) {
								if (mrd.getName() != null && mrd.getName().equals(currentWord)) {
									addAllMethodsAndFields(symbolTable, mrd.getType(), offset, proposals);
								}
							}
						}
				}
				else {// Check for suggestions inside class
					for(ReactiveClassDeclaration rcd : rebecaModel.getRebecaCode().getReactiveClassDeclaration()) {
						if (lineNumber >= rcd.getLineNumber() && lineNumber <= rcd.getEndLineNumber()) {
							// Check for keywords
							for (String s : keywords) {
								if (s.startsWith(currentWord)) {
									proposals.add(new CompletionProposal( s, offset-currentWord.length(), currentWord.length(), s.length()));
								}
							}

							// Getting all statevars and kownrebecs
							List<FieldDeclaration> fields = new ArrayList<>();
							fields.addAll(rcd.getKnownRebecs());
							fields.addAll(rcd.getStatevars());
							
							// Check for statevars and knownrebecs
							for (FieldDeclaration fd : fields) {
								for (VariableDeclarator vd : fd.getVariableDeclarators()) {	
									if (vd.getVariableName().startsWith(currentWord)) {
										proposals.add(new CompletionProposal(vd.getVariableName(), offset - currentWord.length(), currentWord.length(), vd.getVariableName().length()));
									}								
								}
							}
							
							// Check for method calls
							if (currentWord.charAt(currentWord.length()-1) == '.') {
								currentWord = currentWord.substring(0, currentWord.length() - 1);

								// Check for self methods
								if (currentWord.equals("self")) {
									List<MethodDeclaration> methods = new ArrayList<>();
									methods.addAll(rcd.getMsgsrvs());
									methods.addAll(rcd.getSynchMethods());
									
									for (MethodDeclaration md : methods) {
										proposals.add(new CompletionProposal(md.getName() + "()", offset, 0, md.getName().length() + 1));
									}
								}

								// Check for methods for other classes
								else {								
									for (FieldDeclaration fd : fields) {
										for (VariableDeclarator vd : fd.getVariableDeclarators()) {	
											if (vd.getVariableName().equals(currentWord)) {
												addAllMethodsAndFields(symbolTable, fd.getType(), offset, proposals);
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
			return proposals.toArray(new ICompletionProposal[proposals.size()]);
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
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