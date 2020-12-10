package org.rebecalang.afra.ideplugin.editors.rebeca;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;



import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Image;
//import org.rebecalang.ide.afra.compiler.CompilerFeature;
//import org.rebecalang.ide.afra.compiler.ExceptionContainer;
//import org.rebecalang.ide.afra.compiler.RebecaCompiler;
//import org.rebecalang.ide.afra.compiler.rebecaobjectmodel.RebecaModel;


public class CompletionProcessor implements IContentAssistProcessor {

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getErrorMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		// TODO Auto-generated method stub
		return null;
	}
/*  
	private int numberClass;
	private int numberMsg;
	private boolean lastFlag = false;
	private static String[] keywords = {"reactiveclass","knownrebecs","statevars","msgsrv"};
	private static String[] types={"boolean","byte","int","short"};
	
	
	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
	   
	   try {
		   
        IDocument document = viewer.getDocument();
        RebecaCompiler FileCompiler = new RebecaCompiler();
        File temp = File.createTempFile("a.rebeca", "a.rebeca");
        FileWriter fstream = new FileWriter(temp);
		BufferedWriter out = new BufferedWriter(fstream);
		out.write(document.get());
		out.close();Set<CompilerFeature> options = new HashSet<CompilerFeature>();
 		options.add(CompilerFeature.CORE_2_0);
 		RebecaModel FileModel = FileCompiler.getRebecaFilesPartialModel(temp,options );
 		ArrayList<ICompletionProposal> result = new ArrayList<ICompletionProposal>(); // output array that proposals are in it.

 		// Check last letter for '{', '(' and '['
 		if( document.getChar(offset-1) == '{' ) 			
 			result.add(new CompletionProposal( "}", offset, 0, 0));
		else if( document.getChar(offset-1) == '(' )
			result.add(new CompletionProposal( ")", offset, 0, 0));
		else if( document.getChar(offset-1) == '[' )
		    result.add(new CompletionProposal( "]", offset, 0, 0));
		else
		{
			//Check for if curser is new line proposal is '}'
			if( document.getChar(offset-1) == 10 )
				result.add(new CompletionProposal( "}", offset, 0, 0));
			else
			{
				ArrayList<ICompletionProposal> result2 = new ArrayList<ICompletionProposal>();
				result2 = findAllProposals(document,offset,FileModel);
				result.addAll(result2);
			}
		}
        return result.toArray(new ICompletionProposal[result.size()]);
      }
	   catch(ExceptionContainer ec) {
			for(Exception e : ec.getExceptions())
				System.out.println(e.getMessage());
				
		}
	   catch (Exception e) {
         return null;
      }
	   return null;
	}
	
	
	 * For get state variables of a class	  
	 
	private Vector<String> getStatevarClassWords(String msg, String rclass,RebecaModel FileModel)
	{
		
		Vector <String> values = new Vector<String>();
		for( int i = 0; i < FileModel.getRebecaCode().getReactiveClassDeclaration().size(); i++)
		{
			if( FileModel.getRebecaCode().getReactiveClassDeclaration().get(i).getName().matches(rclass)==true)
			{
				numberClass = i;
				for( int j = 0; j < FileModel.getRebecaCode().getReactiveClassDeclaration().get(i).getClassBodyDeclaration().getStateVarsDeclaration().getLocalVariableDeclaration().size() ; j++)
				{
					String statevar = FileModel.getRebecaCode().getReactiveClassDeclaration().get(i).getClassBodyDeclaration().getStateVarsDeclaration().getLocalVariableDeclaration().get(j).getVariableDeclarator().get(0).getName();
					statevar = statevar.concat(" : ");
					statevar = statevar.concat(FileModel.getRebecaCode().getReactiveClassDeclaration().get(i).getClassBodyDeclaration().getStateVarsDeclaration().getLocalVariableDeclaration().get(j).getType().getPrimitiveType().getPrimitiveType());
					values.add(statevar);
				}
				break;
			}
		}
		return values;	
	}
	
	
	
	
	 * For get input arguments of a message word.
	 
	
	private Vector<String> getInputargsMsgWords(String msg, String rclass,RebecaModel FileModel )
	{
		
		Vector <String> values = new Vector<String>();
		for( int j = 0; j < FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass).getClassBodyDeclaration().getMethodDeclaration().size() ; j++)
		{
			
			if( FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass).getClassBodyDeclaration().getMethodDeclaration().get(j).getName().matches(msg)==true)
			{
				
				numberMsg = j;
				if( FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass).getClassBodyDeclaration().getMethodDeclaration().get(numberMsg).getFormalParameterDeclaration() != null)
				{
					for( int k = 0; k < FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass).getClassBodyDeclaration().getMethodDeclaration().get(numberMsg).getFormalParameterDeclaration().getNormalParameterDeclaration().size() ; k++)
					{
						String arg = FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass).getClassBodyDeclaration().getMethodDeclaration().get(numberMsg).getFormalParameterDeclaration().getNormalParameterDeclaration().get(k).getName();
						arg = arg.concat(" : ");
						arg = arg.concat(FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass).getClassBodyDeclaration().getMethodDeclaration().get(numberMsg).getFormalParameterDeclaration().getNormalParameterDeclaration().get(k).getType().getPrimitiveType().getPrimitiveType());
						values.add(arg);
					}
					break;
				}
			}
		}
		return values;
	}
	
	
	
	 * For get local variable 
	 
	private Vector<String> getLocalvarMsgWords(String msg, String rclass,RebecaModel FileModel )
	{
	
		Vector <String> values = new Vector<String>();
		try{
		for( int k = 0; k < FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass).getClassBodyDeclaration().getMethodDeclaration().get(numberMsg).getBlock().getBlockStatement().size() ; k++)
		{
			if( FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass).getClassBodyDeclaration().getMethodDeclaration().get(numberMsg).getBlock().getBlockStatement().get(k) != null)
			{
			
				if( FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass).getClassBodyDeclaration().getMethodDeclaration().get(numberMsg).getBlock().getBlockStatement().get(k).getLocalVariableDeclaration()!=null)
				{
					String localvar = FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass).getClassBodyDeclaration().getMethodDeclaration().get(numberMsg).getBlock().getBlockStatement().get(k).getLocalVariableDeclaration().getVariableDeclarator().get(0).getName();
					localvar = localvar.concat(" : ");
					localvar = localvar.concat(FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass).getClassBodyDeclaration().getMethodDeclaration().get(numberMsg).getBlock().getBlockStatement().get(k).getLocalVariableDeclaration().getType().getPrimitiveType().getPrimitiveType());
					values.add(localvar);
				}
			}
		}}
		catch(Exception e)
		{
			return values;
		}
		return values;
		
	}
	
	private Vector<String> getKnownRebecs( String rclass,RebecaModel FileModel )
	{
		
		Vector <String> values = new Vector<String>();
		if( FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass).getClassBodyDeclaration().getKnownRebecsDeclaration() != null)
		{
		
			for( int k = 0; k < FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass).getClassBodyDeclaration().getKnownRebecsDeclaration().getKnownRebecDeclaration().size() ; k++)
			{	
				String rebecs = FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass).getClassBodyDeclaration().getKnownRebecsDeclaration().getKnownRebecDeclaration().get(k).getName();
				rebecs = rebecs.concat(" : ");
				rebecs = rebecs.concat(FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass).getClassBodyDeclaration().getKnownRebecsDeclaration().getKnownRebecDeclaration().get(k).getClassOrInterfaceType().getName());
				
				values.add(rebecs);
			}
		}
			
		return values;
		
	}
	
	private Vector<String> getKnownRebecs2( String rclass,RebecaModel FileModel )
	{
		Vector <String> values = new Vector<String>();
	
	int numberClass2 = -1;
	for( int i = 0; i < FileModel.getRebecaCode().getReactiveClassDeclaration().size(); i++)
	{
		if( FileModel.getRebecaCode().getReactiveClassDeclaration().get(i).getName().matches(rclass)==true)
		{
			numberClass2 = i;
			
		}
	}
	if( numberClass2 != -1){
		
		
		if( FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass2).getClassBodyDeclaration().getKnownRebecsDeclaration() != null)
		{
			for( int k = 0; k < FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass2).getClassBodyDeclaration().getKnownRebecsDeclaration().getKnownRebecDeclaration().size() ; k++)
			{	
				String rebecs = FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass2).getClassBodyDeclaration().getKnownRebecsDeclaration().getKnownRebecDeclaration().get(k).getName();
				rebecs = rebecs.concat(" : ");
				rebecs = rebecs.concat(FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass2).getClassBodyDeclaration().getKnownRebecsDeclaration().getKnownRebecDeclaration().get(k).getClassOrInterfaceType().getName());
				values.add(rebecs);
			}
		}
	}	
		return values;
		
	}
	private Vector<String> getClassKnown( RebecaModel FileModel )
	{
		Vector <String> values = new Vector<String>();
		
			for( int k = 0; k < FileModel.getRebecaCode().getReactiveClassDeclaration().size();k++)
				values.add(FileModel.getRebecaCode().getReactiveClassDeclaration().get(k).getName());
			
		return values;
		
	}
	
	private ArrayList<ICompletionProposal> findAllProposals( IDocument document, int offset, RebecaModel FileModel ) throws Exception
	{
		
		ArrayList<ICompletionProposal> result= new ArrayList<ICompletionProposal>();

		String msg = "";
		String rclass = "";
		int change = offset - 1;
		int changeHelp = offset;
		String previousWord = "";
		boolean whileFlag = true;
		boolean helpWhileFlag = true;
		boolean helpflag = true;
		boolean nblockflag = false; // not block
		boolean blockflag = false;
		int changeHelp2 = change;
	
		// in while loop set state
		int state = -1;
		while( whileFlag && change >= 0)
		{
			
			change--;
			String currentWord = document.get(change + 1, changeHelp-change-1);
			if( document.getChar(change+1) == '{' && nblockflag == false) // check if in a block or no
				blockflag = true;
			if( document.getChar(change+1) == '}' && blockflag == false )
				nblockflag = true;

			if( currentWord.startsWith("knownrebecs")  &&helpWhileFlag   ) 
			{
				state = 0;
				break;
			}
			else if( currentWord.startsWith("statevars") && helpWhileFlag   )
			{
				state = 1;
				break;
			}
			else if( currentWord.startsWith("main")    )
			{
				state = 4;
				break;
			}
			else if( currentWord.matches("msgsrv") && helpWhileFlag )
			{
				state = 2 ;
				msg = previousWord;
				helpWhileFlag = false;
			}
			else if( currentWord.matches("reactiveclass")  && helpflag)
			{
				rclass = previousWord;
				helpflag = false;
				if( helpWhileFlag == false)	state = 2;
				else {state = 3;blockflag=false; break;}
				
			}
			if( change > 0 && (document.getChar(change) == ' ' || document.getChar(change) == '\n' || document.getChar(change) == '\t' || document.getChar(change) == '{'||document.getChar(change) == '('))
			{
				changeHelp = change;
				previousWord = currentWord;
			}
			
			if( !helpWhileFlag && !helpflag){
				whileFlag = false;}
			
		}
		if( blockflag == false){
		//if out of block
			String prefix = lastWord(document, offset);
			
	 		
	 		for (int i= 0; i < keywords.length; i++) {
		           String keyword = keywords[i];
		           if(keyword.startsWith(prefix)){
		        	   IContextInformation info = new ContextInformation(keyword,"XXX");
		        	   ClassLoader classLoader = Thread.currentThread().getContextClassLoader();			        	
		        	   Image image2 = new Image(null,classLoader.getResourceAsStream("icons/khali.gif") );

		       		
		        	   if( lastFlag == true )
		        		   result.add(new CompletionProposal(keyword.substring(prefix.length()+1, keyword.length()),
				       				 offset, 0, keyword.length()
				       				- prefix.length() , image2 , keyword, info, "keyword"));
		        	   else
		        	   result.add(new CompletionProposal(keyword.substring(prefix.length(), keyword.length()),
		       				 offset, 0, keyword.length()
		       				- prefix.length() , image2 , keyword, info, "keyword"));
		           }
	 		}
			
		}
		if( state == 0 && blockflag == true)
		{
			
			Vector<String> classKnown = getClassKnown(FileModel);

			String prefix = lastWord(document, offset);
			for (int i= 0; i < classKnown.size(); i++) {
		           String keyword = classKnown.elementAt(i);
		           if(keyword.startsWith(prefix)){
		        	   IContextInformation info = new ContextInformation(keyword,"XXX");
		        	   ClassLoader classLoader = Thread.currentThread().getContextClassLoader();			        	
		        	   Image image2 = new Image(null,classLoader.getResourceAsStream("icons/classes.gif") );
		       		result.add(new CompletionProposal(keyword.substring(prefix.length(), keyword.length()),
		       				 offset, 0, keyword.length()
		       				- prefix.length() , image2 , keyword, info, "class"));
		        	   }
		        }
		        
		}
		else if ( state == 1 && blockflag == true )
		{
			String prefix = lastWord(document, offset);
			for (int i= 0; i < types.length; i++) {
		           String keyword = types[i];
		           if(keyword.startsWith(prefix)){
		        	   IContextInformation info = new ContextInformation(keyword,"XXX");
		        	   ClassLoader classLoader = Thread.currentThread().getContextClassLoader();			        	
		        	   Image image2 = new Image(null,classLoader.getResourceAsStream("icons/khali.gif") );
		       		result.add(new CompletionProposal(keyword.substring(prefix.length(), keyword.length()),
		       				 offset, 0, keyword.length()
		       				- prefix.length() , image2 , keyword, info, "primitve type"));
		        	   }
		        }
		        
		}
		else if( state == 4 && blockflag == true)
		{
			
			Vector<String> classKnown = getClassKnown(FileModel);
			for( int j = 0; j < classKnown.size(); j++ )
			{
				Vector<String> classKnownRebecs2 = getKnownRebecs2(classKnown.get(j),FileModel);
				
				String prefix = lastWord(document, offset);
				
		           String keyword = classKnown.elementAt(j);
		         
		           keyword = keyword.concat(" arg0(");
		           int k = 0;
		           for(  k = 0; k < classKnownRebecs2.size();k++)
		           {
		        	   keyword = keyword.concat(classKnownRebecs2.get(k).split(" ")[2]);
		        	   keyword = keyword.concat(" arg");
		        	   keyword = keyword.concat(Integer.toString(k+1));
		        	   if( k < classKnownRebecs2.size()-1)
		        		   keyword = keyword.concat(",");
		           }
		           keyword = keyword.concat("):(arg");
		           keyword = keyword.concat(Integer.toString(k+1));
		           keyword = keyword.concat(")");
		           if(keyword.startsWith(prefix)){
		        	   IContextInformation info = new ContextInformation(keyword,"XXX");
		        	   ClassLoader classLoader = Thread.currentThread().getContextClassLoader();			        	
		        	   Image image2 = new Image(null,classLoader.getResourceAsStream("icons/classes.gif") );
		       		result.add(new CompletionProposal(keyword.substring(prefix.length(), keyword.length()),
		       				 offset, 0, keyword.length()
		       				- prefix.length() , image2 , keyword, info, "Get instance of class"));
		       		
		       		
		           }
		        }

		}
		else if( state == 2 )
		{
			
		
		String prefix2 = lastWord(document, offset);
		
		if( document.getChar(offset-1) == '.'||document.getChar(offset-1-prefix2.length()) == '.')
		{
			Vector<String> methodsOfObject = new Vector<String>();
			Vector<String> methodsOfObject2 = new Vector<String>();
			if(document.getChar(offset-1) != '.')
			{
				changeHelp2 = offset-1-prefix2.length();
			}
			
			while(document.getChar(changeHelp2) != ' ' && document.getChar(changeHelp2) != '\n' && document.getChar(changeHelp2) != '\t' && document.getChar(changeHelp2) != '{'&& document.getChar(changeHelp2) != '('&& document.getChar(changeHelp2) != '}' && document.getChar(changeHelp2) != ';')
			{
				changeHelp2--;
			}
			String objectWord = "";
			if(document.getChar(offset-1) != '.')
			{
				objectWord = document.get(changeHelp2+1,offset-2-changeHelp2-prefix2.length());
				
			}
			
			else{
			objectWord = document.get(changeHelp2+1,offset-2-changeHelp2);
			
			}
			
		
			
			String typeObject = "";
			
			
			
			for( int i = 0; i < FileModel.getRebecaCode().getReactiveClassDeclaration().size(); i++)
			{
				if( FileModel.getRebecaCode().getReactiveClassDeclaration().get(i).getName().matches(rclass)==true)
				{
					
					numberClass = i;
				}
			}
			if(objectWord.matches("self"))	typeObject = rclass;
			else{
	 		for( int j = 0; j < FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass).getClassBodyDeclaration().getKnownRebecsDeclaration().getKnownRebecDeclaration().size();j++)
				if(  FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass).getClassBodyDeclaration().getKnownRebecsDeclaration().getKnownRebecDeclaration().get(j).getName().matches(objectWord))
				{
					typeObject = FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass).getClassBodyDeclaration().getKnownRebecsDeclaration().getKnownRebecDeclaration().get(j).getClassOrInterfaceType().getName();
				}
			}
			int numberClass2 = -1;
			if(objectWord.matches("self")) numberClass2 = numberClass;
			else{
			for( int i = 0; i < FileModel.getRebecaCode().getReactiveClassDeclaration().size(); i++)
			{
				if( FileModel.getRebecaCode().getReactiveClassDeclaration().get(i).getName().matches(typeObject) == true)
				{
					numberClass2 = i;
					break;
				}
			}
			}
			
			if( numberClass2 != -1){
			for( int k = 0; k <FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass2).getClassBodyDeclaration().getMethodDeclaration().size();k++)
			{
						String method = FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass2).getClassBodyDeclaration().getMethodDeclaration().get(k).getName();
						String method2 = FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass2).getClassBodyDeclaration().getMethodDeclaration().get(k).getName();
						
						method = method.concat("(");
						method2 = method2.concat("(");
						if( FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass2).getClassBodyDeclaration().getMethodDeclaration().get(k).getFormalParameterDeclaration() != null)
						{
							for( int h = 0; h < FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass2).getClassBodyDeclaration().getMethodDeclaration().get(k).getFormalParameterDeclaration().getNormalParameterDeclaration().size()-1;h++)
							{
								method = method.concat(FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass2).getClassBodyDeclaration().getMethodDeclaration().get(k).getFormalParameterDeclaration().getNormalParameterDeclaration().get(h).getType().getPrimitiveType().getPrimitiveType());
								method = method.concat(" arg");
								method2 = method2.concat("arg");
								method = method.concat(Integer.toString(h));
								method2 = method2.concat(Integer.toString(h));
								method = method.concat(",");
								method2 = method2.concat(",");
								
							}
						
							method = method.concat(FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass2).getClassBodyDeclaration().getMethodDeclaration().get(k).getFormalParameterDeclaration().getNormalParameterDeclaration().get(FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass2).getClassBodyDeclaration().getMethodDeclaration().get(k).getFormalParameterDeclaration().getNormalParameterDeclaration().size()-1).getType().getPrimitiveType().getPrimitiveType());
							method = method.concat(" arg");
							method2 = method2.concat("arg");
							method = method.concat(Integer.toString(FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass2).getClassBodyDeclaration().getMethodDeclaration().get(k).getFormalParameterDeclaration().getNormalParameterDeclaration().size()-1));
							method2 = method2.concat(Integer.toString(FileModel.getRebecaCode().getReactiveClassDeclaration().get(numberClass2).getClassBodyDeclaration().getMethodDeclaration().get(k).getFormalParameterDeclaration().getNormalParameterDeclaration().size()-1));
						}
						method = method.concat(")");	
						method2 = method2.concat(")");	
				
				methodsOfObject.add(method);
				methodsOfObject2.add(method2);
			}
			String prefix = lastWord(document, offset);
			for (int i= 0; i < methodsOfObject.size(); i++) {
	            String keyword = methodsOfObject.elementAt(i);
	            String keyword2 = methodsOfObject2.elementAt(i);
	          
	            if(keyword.startsWith(prefix)){
	               
	            	 IContextInformation info = new ContextInformation(keyword,"XXX");
	            	 ClassLoader classLoader = Thread.currentThread().getContextClassLoader();			        	
		        	   Image image2 = new Image(null,classLoader.getResourceAsStream("icons/method2.gif") );
		       		result.add(new CompletionProposal(keyword2.substring(prefix.length(), keyword2.length()),
		       				 offset, 0, keyword2.length()
		       				- prefix.length(), image2 , keyword, info, "function"));
	            	
	            	}
	         }
		}
		}
		
		else{
			String prefix = lastWord(document, offset);
		Vector<String> statevarClassWords = getStatevarClassWords(msg,rclass,FileModel);
		String self = "self";
		 if(self.startsWith(prefix)){
      	   IContextInformation info = new ContextInformation(self,"XXX");
      	 ClassLoader classLoader = Thread.currentThread().getContextClassLoader();			        	
  	   Image image2 = new Image(null,classLoader.getResourceAsStream("icons/khali.gif") );
     		result.add(new CompletionProposal(self.substring(prefix.length(), self.length()),
     				 offset, 0, self.length()
     				- prefix.length() , image2 , self, info, ""));
      }
		
		Vector<String> inputargsMsgWords = getInputargsMsgWords(msg,rclass,FileModel);
		Vector<String> localvarMsgWords = getLocalvarMsgWords(msg,rclass,FileModel);
		Vector<String> classKnownRebecs = getKnownRebecs(rclass,FileModel);
		
		for (int i= 0; i < classKnownRebecs.size(); i++) {
	           String keyword = classKnownRebecs.elementAt(i);
	           String keyword2 = keyword.split(" ")[0];
	           if(keyword2.startsWith(prefix)){
	        	   IContextInformation info = new ContextInformation(keyword,"XXX");
	        	   ClassLoader classLoader = Thread.currentThread().getContextClassLoader();			        	
	        	   Image image2 = new Image(null,classLoader.getResourceAsStream("icons/classes.gif") );
	       		result.add(new CompletionProposal(keyword2.substring(prefix.length(), keyword2.length()),
	       				 offset, 0, keyword2.length()
	       				- prefix.length() , image2 , keyword, info, "knownrebecs"));
	        }
		}
	        
		for (int i= 0; i < statevarClassWords.size(); i++) {
           String keyword = statevarClassWords.elementAt(i);
           String keyword2 = keyword.split(" ")[0];
           if(keyword2.startsWith(prefix)){
        	   IContextInformation info = new ContextInformation(keyword,"XXX");
        	   ClassLoader classLoader = Thread.currentThread().getContextClassLoader();			        	
        	   Image image2 = new Image(null,classLoader.getResourceAsStream("icons/statevar.gif") );
       		result.add(new CompletionProposal(keyword2.substring(prefix.length(), keyword2.length()),
       				 offset, 0, keyword2.length()
       				- prefix.length(), image2 , keyword, info, "statevar"));
}
		}
        
        for (int i= 0; i < inputargsMsgWords.size(); i++) {
            String keyword = inputargsMsgWords.elementAt(i);
            String keyword2 = keyword.split(" ")[0];
            if(keyword2.startsWith(prefix)){
            	 IContextInformation info = new ContextInformation(keyword,"XXX");
            	  ClassLoader classLoader = Thread.currentThread().getContextClassLoader();			        	
           	   Image image2 = new Image(null,classLoader.getResourceAsStream("icons/local.gif") );
	       		result.add(new CompletionProposal(keyword2.substring(prefix.length(), keyword2.length()),
	       				 offset, 0, keyword2.length()
	       				- prefix.length(), image2 , keyword, info, "input args"));
    	}
         }
        
        for (int i= 0; i < localvarMsgWords.size(); i++) {
            String keyword = localvarMsgWords.elementAt(i);
            String keyword2 = keyword.split(" ")[0];
            if(keyword2.startsWith(prefix)){
            	 IContextInformation info = new ContextInformation(keyword,"XXX");
            	  ClassLoader classLoader = Thread.currentThread().getContextClassLoader();			        	
           	   Image image2 = new Image(null,classLoader.getResourceAsStream("icons/local.gif") );
           	   result.add(new CompletionProposal(keyword2.substring(prefix.length(), keyword2.length()),
	       				 offset, 0, keyword2.length()
	       				- prefix.length(), image2 , keyword, info, "local variables"));
             }
         }
		}
		}
        return result;
	        
	}
	
	private String lastWord(IDocument doc, int offset) {
      try {
    	lastFlag = false;
         for (int n = offset-1; n >= 0; n--) {
           char c = doc.getChar(n);
       
          if( n == 0 ){
        	  lastFlag = true;
        	  return doc.get(n, offset-n-1);
          }
        	 
          if (!Character.isJavaIdentifierPart(c) )
             return doc.get(n + 1, offset-n-1);
         }
      } catch (BadLocationException e) {
         
      }
      return "";
    }
	
	


	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer,
			int offset) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return new char[] { '.', '(','{','['};
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
*/
}