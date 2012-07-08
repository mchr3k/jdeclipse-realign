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
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.BufferManager;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;
import org.eclipse.jdt.internal.core.SourceMapper;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.ui.IEditorInput;


/**
 * RealignmentJDClassFileEditor
 *
 * @project Java Decompiler Eclipse Plugin
 * @author  Alex Kosinsky
 */

@SuppressWarnings("restriction")
public class RealignmentJDClassFileEditor extends JDClassFileEditor
{
	@SuppressWarnings("rawtypes")
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

	public static JDSourceMapper createJDSourceMapper( IPath rootPath, IPath sourcePath, String sourceRootPath, Map<?,?> options)
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

	@Override
	public boolean isEditable() {
		return false;
	}

	@Override
	public void dispose() {

		IEditorInput input = getEditorInput();

		if (input instanceof IClassFileEditorInput)
		{
			BufferManager bufferManager = BufferManager.getDefaultBufferManager();

			IClassFileEditorInput classFileEditorInput =
				((IClassFileEditorInput)input);
			IClassFile file = classFileEditorInput.getClassFile();

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
				System.out.println("foo");
			}

			// Search package fragment root.
			IJavaElement javaElement = file.getParent();
			while ((javaElement != null) &&
				   (javaElement.getElementType() !=
					   IJavaElement.PACKAGE_FRAGMENT_ROOT))
			{
				javaElement = javaElement.getParent();
			}

			// Remove our source mapper
			if ((javaElement != null) &&
				(javaElement instanceof PackageFragmentRoot))
			{
				PackageFragmentRoot root = (PackageFragmentRoot)javaElement;
				try {
					root.setSourceMapper(null);
				} catch (JavaModelException e) {
					JavaDecompilerPlugin.getDefault().getLog().log(new Status(
							Status.ERROR, JavaDecompilerPlugin.PLUGIN_ID,
							0, e.getMessage(), e));
				}
			}
		}
		super.dispose();
	}
}


