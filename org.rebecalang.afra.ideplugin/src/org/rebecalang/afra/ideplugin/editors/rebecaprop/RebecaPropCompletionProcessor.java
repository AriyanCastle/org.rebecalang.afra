package org.rebecalang.afra.ideplugin.editors.rebecaprop;

import java.util.ArrayList;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

public class RebecaPropCompletionProcessor implements IContentAssistProcessor {
	
	private RebecaPropEditor editor;
	
	// Keywords from RebecaPropScanner
	private static String[] keywords = {
		"define", "CTL", "LTL", "property", "true", "false", "Assertion"
	};
	
	public RebecaPropCompletionProcessor(RebecaPropEditor editor) {
		this.editor = editor;
	}

	private int getWordStartIndex(IDocument document, int offset) throws BadLocationException {
		if (offset <= 0) return 0;
		
		int index = offset - 1;
		while (index >= 0) {
			char ch = document.getChar(index);
			if (!Character.isJavaIdentifierPart(ch)) {
				break;
			}
			index--;
		}
		return index + 1;
	}

	private String getCurrentWord(IDocument document, int offset) {
		try {
			if (offset <= 0 || offset > document.getLength()) {
				return "";
			}
			
			int startIndex = getWordStartIndex(document, offset);
			
			if (startIndex >= offset || startIndex < 0) {
				return "";
			}
			
			return document.get(startIndex, offset - startIndex);
		} catch (Exception e) {
			return "";
		}
	}
	
	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		ArrayList<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
		
		try {
			IDocument document = viewer.getDocument();
			
			// Safety check for offset
			if (offset <= 0 || offset > document.getLength()) {
				return new ICompletionProposal[0];
			}
			
			// Get current word being typed
			String currentWord = getCurrentWord(document, offset);
			if (currentWord == null) {
				currentWord = "";
			}
			
			// Add keyword completions
			for (String keyword : keywords) {
				if (keyword.toLowerCase().startsWith(currentWord.toLowerCase())) {
					proposals.add(new CompletionProposal(keyword, offset - currentWord.length(), 
						currentWord.length(), keyword.length()));
				}
			}
			
			return proposals.toArray(new ICompletionProposal[proposals.size()]);
			
		} catch (Exception e) {
			// Return empty array instead of null to prevent error sound
			return new ICompletionProposal[0];
		}
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		return null;
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
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
