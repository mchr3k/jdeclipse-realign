package jd.ide.eclipse.realignment.editors;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;

import jd.ide.eclipse.JavaDecompilerPlugin;
import jd.ide.eclipse.editors.JDSourceMapper;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;


/**
 * RealignmentJDSourceMapper
 * 
 * @project Java Decompiler Eclipse Plugin
 * @author  Alex Kosinsky
 */
public class RealignmentJDSourceMapper extends JDSourceMapper 
{
	String libraryPath=null;
	
	public RealignmentJDSourceMapper(
		IPath classePath, IPath sourcePath, String sourceRootPath, Map<?,?> options,JDSourceMapper sourceMapper) 
	{
		super(classePath, sourcePath, sourceRootPath,options);
		Method method;
		try {
			method = sourceMapper.getClass().getDeclaredMethod(
					"getLibraryPath", new Class[] {});
			method.setAccessible(true);
			libraryPath=(String)method.invoke(sourceMapper,new Object[] {});	
		} catch (Exception e) {
			JavaDecompilerPlugin.getDefault().getLog().log(new Status(
					Status.ERROR, JavaDecompilerPlugin.PLUGIN_ID, 
					0, e.getMessage(), e));
		}
	}
	
	public char[] findSource(IPath path, String javaClassPath) 
	{
		IPreferenceStore store = JavaDecompilerPlugin.getDefault().getPreferenceStore();
		boolean saveValLineNumbers= store.getBoolean(JavaDecompilerPlugin.PREF_DISPLAY_LINE_NUMBERS);
		char[] output=super.findSource(path,javaClassPath);
		store.setValue(JavaDecompilerPlugin.PREF_DISPLAY_LINE_NUMBERS,saveValLineNumbers);
		DecompilerOutputUtil decompilerOutputUtil=new DecompilerOutputUtil(new String(output));
		output=decompilerOutputUtil.realign();
		return output;
	}

	protected String getLibraryPath() throws IOException {
		return libraryPath;
	}
	public String doDecompiling(String baseName, String qualifiedName) throws IOException{
		loadLibrary();
		return super.decompile(baseName,qualifiedName);
	}
	
	
}
