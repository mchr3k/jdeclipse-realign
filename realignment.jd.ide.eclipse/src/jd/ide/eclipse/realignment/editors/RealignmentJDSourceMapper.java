package jd.ide.eclipse.realignment.editors;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.core.BufferManager;
import org.eclipse.jdt.internal.core.SourceMapper;
import org.eclipse.jface.preference.IPreferenceStore;


/**
 * RealignmentJDSourceMapper
 *
 * @project Java Decompiler Eclipse Plugin
 * @author  Alex Kosinsky
 */
@SuppressWarnings("restriction")
public class RealignmentJDSourceMapper extends JDSourceMapper
{
	private static final Map<JDSourceMapper, Set<IClassFile>> decompiledClasses = new HashMap<JDSourceMapper, Set<IClassFile>>();

	public static synchronized void addDecompiled(JDSourceMapper mapper, IClassFile file)
	{
		Set<IClassFile> set = decompiledClasses.get(mapper);
		if (set == null)
		{
			set = new HashSet<IClassFile>();
			decompiledClasses.put(mapper, set);
		}
		set.add(file);
		System.out.println("Added: " + set);
	}

	public static synchronized void clearDecompiled(JDSourceMapper mapper)
	{
		Set<IClassFile> set = decompiledClasses.remove(mapper);
		System.out.println("Clear: " + set);
		for (IClassFile file : set)
		{
			BufferManager bufferManager = BufferManager.getDefaultBufferManager();

			IBuffer buffer = bufferManager.getBuffer(file);

			if (buffer != null)
			{
				try
				{
					// Remove the buffer
					Method method = BufferManager.class.getDeclaredMethod(
						"removeBuffer", new Class[] {IBuffer.class});
					method.setAccessible(true);
					method.invoke(bufferManager, new Object[] {buffer});
				}
				catch (Exception e)
				{
					JavaDecompilerPlugin.getDefault().getLog().log(new Status(
							Status.ERROR, JavaDecompilerPlugin.PLUGIN_ID,
							0, e.getMessage(), e));
				}
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public static SourceMapper newSourceMapper(
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

	static JDSourceMapper createJDSourceMapper( IPath rootPath, IPath sourcePath, String sourceRootPath, Map<?,?> options)
	throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException{
		Method method = JDClassFileEditor.class.getDeclaredMethod(
				"newSourceMapper", new Class[] {IPath.class,IPath.class,String.class,Map.class});
			method.setAccessible(true);
			JDSourceMapper sourceMapper=(JDSourceMapper)method.invoke(
					getJDClassFileEditorClass().newInstance(),new Object[] {rootPath,sourcePath,sourceRootPath,options});
			return  sourceMapper;
	}

	static Class<?> getJDClassFileEditorClass() throws ClassNotFoundException{
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

	@Override
	public char[] findSource(IType type, IBinaryType info) {
		IClassFile classFile = (IClassFile) type.getParent();
		addDecompiled(this, classFile);
		return super.findSource(type, info);
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
