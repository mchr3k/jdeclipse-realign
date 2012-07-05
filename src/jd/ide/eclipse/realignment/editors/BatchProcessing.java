package jd.ide.eclipse.realignment.editors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.CRC32;

import jd.ide.eclipse.JavaDecompilerPlugin;
import jd.ide.eclipse.realignment.preferences.RealignmentPreferencePage;

import org.eclipse.jface.preference.IPreferenceStore;

public class BatchProcessing {
	private ArrayList<File> listFiles=new ArrayList<File>();
	private String line_separator;
	private IPreferenceStore store;
	public volatile int progressBarSelection=-1;
	public volatile int progressBarMin=-1;
	public volatile int progressBarMax=-1;
	public volatile boolean abort=false;
	
	String charsetName="UTF-8";
	public BatchProcessing(){
		line_separator=System.getProperty("line.separator","\r\n");
		store =	JavaDecompilerPlugin.getDefault().getPreferenceStore();
	}

	public void perform(String pathClassFiles,String pathJavaFiles) throws Exception{
		String result="";
		FileOutputStream writer;
		JarFile jarFile=null;
		pathClassFiles=pathClassFiles.trim();
		pathJavaFiles=pathJavaFiles.trim();
		File filePathClassFiles=new File(pathClassFiles);
		File filePathJavaFiles=new File(pathJavaFiles);
		if(!filePathClassFiles.exists())
			throw new Exception("Directory: "+pathClassFiles+" not exists.");
		if(!filePathJavaFiles.exists())
			throw new Exception("Directory: "+pathJavaFiles+" not exists.");
		if(!filePathClassFiles.isDirectory()){
			jarFile=new JarFile(filePathClassFiles);
		}
		if(!filePathJavaFiles.isDirectory())
			throw new Exception("Not directory: "+pathJavaFiles);
		
		pathClassFiles=filePathClassFiles.getCanonicalPath();
		pathJavaFiles=filePathJavaFiles.getCanonicalPath();
		if(jarFile==null)
			getListFilesRecursive(filePathClassFiles);
		else{
		   for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements();){
			   JarEntry jarEntry=e.nextElement();
			   if(jarEntry.isDirectory())
				   continue;
			   File file=new File(jarEntry.getName());
				if(file.getName().indexOf(".java")!=-1 && file.getName().indexOf('$')==-1)
					listFiles.add(file);
		   }
		}
		byte[] buffer;
		FileInputStream reader;
		File all_classes_file=new File(pathJavaFiles+File.separator+"all_classes.txt");
		writer=new FileOutputStream(all_classes_file);
		writer.write(pathClassFiles.getBytes(charsetName));
		writer.write(line_separator.getBytes(charsetName));
		for(int i=0;i<listFiles.size();i++){
			if(jarFile==null)
				writer.write(listFiles.get(i).getCanonicalPath().getBytes(charsetName));
			else
				writer.write(listFiles.get(i).getPath().getBytes(charsetName));
			writer.write(line_separator.getBytes(charsetName));
		}
		writer.close();
		buffer=new byte[(int)all_classes_file.length()];
		reader=new FileInputStream(all_classes_file);
		reader.read(buffer);
		reader.close();
		CRC32 crc32=new CRC32();
		crc32.update(buffer);
		File decompiled_classes_file=new File(pathJavaFiles+File.separator+"decompiled_classes.txt");
		boolean crc32Ok=false;
		if(decompiled_classes_file.exists()){
			BufferedReader reader2=new BufferedReader(new FileReader(decompiled_classes_file));
			String line=reader2.readLine();
			reader2.close();
			int p=line.indexOf(' ');
			try{
				if(p!=-1 && Long.parseLong(line.substring(0, p).trim())==crc32.getValue() &&
						line.substring(p).trim().equals(pathClassFiles)){
					crc32Ok=true;
				}
			}catch(Exception e){}
		}
		
		int num_lines_decompiled=0;
		if(crc32Ok){
			reader=new FileInputStream(decompiled_classes_file);
			buffer=new byte[(int)decompiled_classes_file.length()];
			reader.read(buffer);
			reader.close();
			int index_n=0;
			for(int i=0;i<buffer.length;i++){
				if(buffer[i]=='\n'){
					num_lines_decompiled++;
					index_n=i+1;
				}
			}
			if(num_lines_decompiled<listFiles.size()){
				writer=new FileOutputStream(decompiled_classes_file,true);
				if(index_n<buffer.length && (new String(buffer,index_n,buffer.length-index_n,charsetName)).equals("CANCELED: ")){
					
				}else{
					writer.write("ERROR: ".getBytes(charsetName));
					writer.write(listFiles.get(num_lines_decompiled).getCanonicalPath().getBytes(charsetName));
					writer.write(line_separator.getBytes(charsetName));
					num_lines_decompiled++;
				}
			}
		}
		else{
			writer=new FileOutputStream(decompiled_classes_file);
			writer.write((crc32.getValue()+" "+pathClassFiles).getBytes(charsetName));
			writer.write(line_separator.getBytes(charsetName));
		}
		progressBarMin=0;
		progressBarMax=listFiles.size()-1;
		progressBarSelection=num_lines_decompiled;
		for(int i=num_lines_decompiled;i<listFiles.size();i++){
			if(abort){
				writer.write(("CANCELED: ").getBytes(charsetName));
				break;
			}
			progressBarSelection=i;
			File resultFile=null;
			String javaFile=null;
			
			if(jarFile==null){
				result=doDecompiling(listFiles.get(i),null);
				javaFile=pathJavaFiles+listFiles.get(i).getCanonicalPath().substring(pathClassFiles.length());
			}
			else{
				result=doDecompiling(listFiles.get(i),filePathClassFiles);
				javaFile=pathJavaFiles+File.separator+listFiles.get(i).getPath();
			}
			
			resultFile=new File(javaFile);
			javaFile=resultFile.getParent()+File.separator+resultFile.getName().replaceAll(".java", ".java");
			resultFile=new File(javaFile);
			createDirectory(resultFile.getParentFile());
			
			FileOutputStream writer2=new FileOutputStream(resultFile);
			writer2.write(result.getBytes(charsetName));
			writer2.close();
			if(jarFile==null)
				writer.write(listFiles.get(i).getCanonicalPath().getBytes(charsetName));
			else
				writer.write(listFiles.get(i).getPath().getBytes(charsetName));
			writer.write(line_separator.getBytes(charsetName));
		}
		writer.close();
		abort=true;
	}
	
	private void createDirectory(File subdir) throws Exception{
		if(subdir.exists())
			return;
		if(!subdir.getParentFile().exists())
			createDirectory(subdir.getParentFile());
		subdir.mkdir();
		
		subdir=null;
	}
	
	private void getListFilesRecursive(File dir){
		File[] list=dir.listFiles();
		for(int i=0;i<list.length;i++){
			if(list[i].isDirectory())
				getListFilesRecursive(list[i]);
			else{
				if(list[i].getName().indexOf(".java")!=-1 && list[i].getName().indexOf('$')==-1)
					listFiles.add(list[i]);
			}
		}
	}
	
	private String doDecompiling(File file,File jarFile) throws Exception{
		String result="";
		if(jarFile==null){
			char[] buffer=new char[(int)file.length()];
			FileReader reader=new FileReader(file);
			reader.read(buffer);
			result=new String(buffer);
		}else{
			JarFile jar=new JarFile(jarFile);
			JarEntry entry=jar.getJarEntry(file.getPath().replaceAll("\\\\", "/"));
			InputStream is=jar.getInputStream(entry);
			byte[] buf=new byte[(int)entry.getSize()];
			is.read(buf);
			result=new String(buf,"UTF-8");
		}
		if (result != null){
			DecompilerOutputUtil decompilerOutputUtil=new DecompilerOutputUtil(result);
			result= new String( decompilerOutputUtil.realign());
		}
		if(result==null)
			result="Null Decompiler Output.";
		return result;
	}
	

}

