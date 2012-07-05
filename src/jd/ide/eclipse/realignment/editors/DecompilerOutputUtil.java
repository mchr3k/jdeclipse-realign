package jd.ide.eclipse.realignment.editors;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;


/**
 * DecompilerOutputUtil
 * 
 * @project Java Decompiler Eclipse Plugin
 * @author  Alex Kosinsky
 * 
 */


public class DecompilerOutputUtil {
	private String output;
	private StringBuffer realignOutput=new StringBuffer();
	private ArrayList<OutputLine> outputList=new ArrayList<OutputLine>();//
	private ArrayList<JavaSrcLine> javaSrcList=new ArrayList<JavaSrcLine> ();
	private CompilationUnit unit;
	private String line_separator;
	private int line_separator_len;

	private class OutputLine{
		int startPosition;
		int numLineJavaSrc=-1;
		int calculatedNumLineJavaSrc=-1;
	}

	private class JavaSrcLine{
		ArrayList<Integer> outputLines=new ArrayList<Integer>();
	}
	
	
	public DecompilerOutputUtil(String output){
		line_separator=System.getProperty("line.separator","\r\n");
		line_separator_len=line_separator.length();
		this.output=output+line_separator;
	}
	
	public char[] realign(){
		String line;
		int numLine;
		if(output.length()==0)
			return output.toCharArray();
		if(output==null)
			return null;
		
		fillOutputList();
		javaSrcList.add(null);
		ASTParser parser = ASTParser.newParser(AST.JLS3);  
		parser.setSource(output.toCharArray());
		unit=(CompilationUnit)parser.createAST(null);
		@SuppressWarnings("unchecked")
		List types=unit.types();
		for(int i=0;i<types.size();i++){
			if(types.get(i) instanceof TypeDeclaration)
				processMethods((TypeDeclaration)types.get(i));
		}
		int firstTypeLine=Integer.MAX_VALUE;
		int lastTypeLine=Integer.MIN_VALUE;
		for(int i=0;i<types.size();i++){
			if(!(types.get(i) instanceof TypeDeclaration))
				continue;
			TypeDeclaration type=(TypeDeclaration)types.get(i);
			processTypes(type);
			numLine=unit.getLineNumber(type.getStartPosition());
			if(numLine<firstTypeLine)
				firstTypeLine=numLine;
			numLine=unit.getLineNumber(type.getStartPosition()+type.getLength()-1);
			if(numLine>lastTypeLine)
				lastTypeLine=numLine;
		}
		if(javaSrcList.size()==1)
			return output.toCharArray();
		
		if(firstTypeLine!=Integer.MAX_VALUE)
			addBelow(firstTypeLine-1, 0, 0);
		if(lastTypeLine!=Integer.MIN_VALUE)
			addBelow(outputList.size()-2, lastTypeLine, javaSrcList.size()-1);
		
		for(int i=1;i<javaSrcList.size();i++){
			JavaSrcLine javaSrcLine=initJavaSrcListItem(i);
			if(javaSrcLine!=null){
				for(int j=0;j<javaSrcLine.outputLines.size();j++){
					numLine=javaSrcLine.outputLines.get(j);
					line=output.substring(outputList.get(numLine).startPosition,
							outputList.get(numLine+1).startPosition-line_separator_len);
					realignOutput.append(line);
				}
			}
			realignOutput.append(line_separator);
		}
		return realignOutput.toString().toCharArray();
	}

	private void fillOutputList(){
		int pos=0;
		outputList.add(null);
		while(true){
			OutputLine outputLine=new OutputLine();
			outputLine.startPosition=pos;
			outputList.add(outputLine);
			if(output.length()<=pos)
				break;
			pos=output.indexOf('\n', pos);
			if(pos==-1)
				pos=output.length();
			else
				pos++;
		}
	}
	
	private int parseJavaLineNumber(String line){
		int p1,p2;
		p1=line.indexOf("/*",0);
		p2=line.indexOf("*/",p1);
		if(p1==0 && p2>0){
			try{
				return Integer.parseInt(line.substring(p1+2, p2-1).trim());
			}catch(Exception ex){}
		}
		return -1;
	}
	
	private JavaSrcLine initJavaSrcListItem(int index){
		if(javaSrcList.size()<=index){
			for(int a=javaSrcList.size();a<=index;a++)
				javaSrcList.add(null);
		}
		JavaSrcLine javaSrcLine=javaSrcList.get(index);
		if(javaSrcLine==null){
			javaSrcLine=new JavaSrcLine();
			javaSrcList.set(index,javaSrcLine);
		}
		return javaSrcLine;
	}
	
	private void addAbove(int srcLineBegin,int srcNumLine,int destNumLineJavaSrc){
		int l1=1;
		if(destNumLineJavaSrc==1)
			return;
		while((srcNumLine-l1)>=srcLineBegin){
			OutputLine outputLine=outputList.get(srcNumLine-l1);
			if(outputLine.numLineJavaSrc==-1){
				JavaSrcLine javaSrcLine=null;
				if(destNumLineJavaSrc-l1>0)
					javaSrcLine=initJavaSrcListItem(destNumLineJavaSrc-l1);
				if(destNumLineJavaSrc-l1==1 || javaSrcLine.outputLines.size()>0){
					JavaSrcLine javaSrcLinePrev=initJavaSrcListItem(destNumLineJavaSrc-l1+1);
					for(int l2=l1;(srcNumLine-l2)>=srcLineBegin;l2++){
						outputLine=outputList.get(srcNumLine-l2);
						if(outputLine.numLineJavaSrc==-1){
							javaSrcLinePrev.outputLines.add(0,srcNumLine-l2);
							outputLine.calculatedNumLineJavaSrc=destNumLineJavaSrc-l1+1;
						}else
							break;
					}
					break;
				}
				javaSrcLine.outputLines.add(srcNumLine-l1);
				outputLine.calculatedNumLineJavaSrc=destNumLineJavaSrc-l1;
			}else
				break;
			l1++;
		}
	}
	
	private void addBelow(int srcLineEnd,int srcNumLine,int destNumLineJavaSrc){
		int l1=1;
		while((srcNumLine+l1)<=srcLineEnd){
			OutputLine outputLine=outputList.get(srcNumLine+l1);
			if(outputLine.numLineJavaSrc==-1){
				JavaSrcLine javaSrcLine=initJavaSrcListItem(destNumLineJavaSrc+l1);
				if(javaSrcLine.outputLines.size()>0){
					JavaSrcLine javaSrcLinePrev=initJavaSrcListItem(destNumLineJavaSrc+l1-1);
					for(int l2=l1;(srcNumLine+l2)<=srcLineEnd;l2++){
						outputLine=outputList.get(srcNumLine+l2);
						if(outputLine.numLineJavaSrc==-1){
							javaSrcLinePrev.outputLines.add(srcNumLine+l2);
							outputLine.calculatedNumLineJavaSrc=destNumLineJavaSrc+l1-1;
						}else
							break;
					}
					break;
				}
				javaSrcLine.outputLines.add(srcNumLine+l1);
				outputLine.calculatedNumLineJavaSrc=destNumLineJavaSrc+l1;
			}else
				break;
			l1++;
		}
	}
	
	private void processTypes(TypeDeclaration rootType){
		TypeDeclaration[] types=rootType.getTypes();
		for(int i=0;i<types.length;i++){
			processTypes(types[i]);
		}
		int beginTypeLine=Integer.MAX_VALUE;
		int endTypeLine=Integer.MIN_VALUE;
		int firstMethodLine=Integer.MAX_VALUE;
		int lastMethodLine=Integer.MIN_VALUE;
		int beginTypeLineOutput=unit.getLineNumber(rootType.getStartPosition());
		int endTypeLineOutput=unit.getLineNumber(rootType.getStartPosition()+rootType.getLength()-1);
		for(int i=beginTypeLineOutput;i<=endTypeLineOutput;i++){
			OutputLine outputLine=outputList.get(i);
			int numLineJavaSrc=outputLine.numLineJavaSrc;
			if(numLineJavaSrc==-1)
				numLineJavaSrc=outputLine.calculatedNumLineJavaSrc;
			if(numLineJavaSrc!=-1){
				if(beginTypeLine>numLineJavaSrc)
					beginTypeLine=numLineJavaSrc;
				if(endTypeLine<numLineJavaSrc)
					endTypeLine=numLineJavaSrc;
				if(firstMethodLine>i)
					firstMethodLine=i;
				if(lastMethodLine<i)
					lastMethodLine=i;
			}
		}
		if(beginTypeLine!=Integer.MAX_VALUE){
			addAbove( beginTypeLineOutput,firstMethodLine,beginTypeLine);
			addBelow(endTypeLineOutput, lastMethodLine, endTypeLine);
		}
	}
	
	private void processMethods(TypeDeclaration rootType){
		String line;
		MethodDeclaration[] methods=rootType.getMethods();
		for(int m=0;m<methods.length;m++){
			int p=methods[m].getStartPosition();
			int lineBegin=unit.getLineNumber(p);
			int lineEnd=unit.getLineNumber(p+methods[m].getLength()-1);
			int lastNumLineJavaSrc=-1;
			int lastNumLineOutput=-1;
			for(int numLine=lineBegin;numLine<=lineEnd;numLine++){
				OutputLine outputLine=outputList.get(numLine);
				OutputLine outputLineNext=outputList.get(numLine+1);
				line=output.substring(outputLine.startPosition, outputLineNext.startPosition);
				outputLine.numLineJavaSrc=parseJavaLineNumber(line);
				if(outputLine.numLineJavaSrc>1){
					lastNumLineJavaSrc=outputLine.numLineJavaSrc;
					lastNumLineOutput=numLine;
					JavaSrcLine javaSrcLine=initJavaSrcListItem(outputLine.numLineJavaSrc);
					javaSrcLine.outputLines.add(numLine);
					addAbove(lineBegin,numLine,outputLine.numLineJavaSrc);
				}
			}
			if(lastNumLineOutput!=-1 && lastNumLineOutput<lineEnd)
				addBelow(lineEnd,lastNumLineOutput,lastNumLineJavaSrc);
		}
		TypeDeclaration[] types=rootType.getTypes();
		for(int i=0;i<types.length;i++){
			processMethods(types[i]);
		}
	}
}
