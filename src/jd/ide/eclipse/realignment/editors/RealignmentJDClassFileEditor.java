package jd.ide.eclipse.realignment.editors;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import jd.ide.eclipse.JavaDecompilerPlugin;
import jd.ide.eclipse.editors.JDClassFileEditor;
import jd.ide.eclipse.editors.JDSourceMapper;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.internal.core.SourceMapper;


/**
 * RealignmentJDClassFileEditor
 * 
 * @project Java Decompiler Eclipse Plugin
 * @author  Alex Kosinsky
 */

@SuppressWarnings("restriction")
public class RealignmentJDClassFileEditor extends JDClassFileEditor
{
	@SuppressWarnings("unchecked") 
	protected SourceMapper newSourceMapper(  
		IPath rootPath, IPath sourcePath, String sourceRootPath, Map options)
	{
		try 
		{
			return  new RealignmentJDSourceMapper(    
				rootPath, sourcePath, sourceRootPath, options,
				createJDSourceMapper(rootPath, sourcePath, sourceRootPath, options));
		} 
		catch (Exception e) 
		{
			JavaDecompilerPlugin.getDefault().getLog().log(new Status(
					Status.ERROR, JavaDecompilerPlugin.PLUGIN_ID, 
					0, e.getMessage(), e));
		}
		return null;
		
	}
	
	@SuppressWarnings("unchecked") 
	public static JDSourceMapper createJDSourceMapper( IPath rootPath, IPath sourcePath, String sourceRootPath, Map options)
	throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException{
		Method method = JDClassFileEditor.class.getDeclaredMethod(
				"newSourceMapper", new Class[] {IPath.class,IPath.class,String.class,Map.class});
			method.setAccessible(true);
			JDSourceMapper sourceMapper=(JDSourceMapper)method.invoke(
					getJDClassFileEditorClass().newInstance(),new Object[] {rootPath,sourcePath,sourceRootPath,options});	
			return  sourceMapper;
	}
	
	@SuppressWarnings("unchecked") 
	static Class getJDClassFileEditorClass() throws ClassNotFoundException{
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint point = registry.getExtensionPoint("org.eclipse.ui.editors");
		if (point == null) 
			return null;
		IExtension[] extensions = point.getExtensions();
		for (int i = 0; i < extensions.length; i++){
			IConfigurationElement[] elements=extensions[i].getConfigurationElements();
			for(int j=0;j<elements.length;j++){
				String id=elements[j].getAttribute("id");
				if(id.indexOf("jd.ide.eclipse.editors.JDClassFileEditor")==0)
					return Class.forName(elements[j].getAttribute("class"));
			}
		}
		return null;
	}
}


