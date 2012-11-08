package jd.ide.eclipse.realignment.editors;

import java.util.Map;

import jd.ide.eclipse.JavaDecompilerPlugin;
import jd.ide.eclipse.editors.JDClassFileEditor;
import jd.ide.eclipse.realignment.menu.SelectUtils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;
import org.eclipse.jdt.internal.core.SourceMapper;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.ui.IEditorInput;

/**
 * Extension of {@link JDClassFileEditor} to use a realigned source mapper.
 */
@SuppressWarnings("restriction")
public class RealignmentJDClassFileEditor extends JDClassFileEditor
{
  /**
   * Only allow a source mapper to be created during editor
   * initialization. If the source is reloaded later (e.g. because
   * the decompiled source is detached) we don't want to automatically
   * create a new mapper.
   */
  private boolean mSeenFirstSourceMapperCall = false;
  private IEditorInput mInput;

  @Override
  protected void doSetInput(IEditorInput input) throws CoreException
  {
    // Save a copy locally so that we can access the value sooner than would otherwise
    // be possible
    this.mInput = input;
    super.doSetInput(input);
  }

  @Override
  @SuppressWarnings("rawtypes")
  protected SourceMapper newSourceMapper(IPath rootPath, IPath sourcePath,
      String sourceRootPath, Map options)
  {
    boolean seenFirstSourceMapperCall = mSeenFirstSourceMapperCall;
    mSeenFirstSourceMapperCall = true;
    boolean attachDecompiled = true;

    if (mInput instanceof IClassFileEditorInput)
    {
      IClassFileEditorInput classFileEditorInput = (IClassFileEditorInput) mInput;
      IClassFile classFile = classFileEditorInput.getClassFile();
      PackageFragmentRoot root = SelectUtils.getRoot(classFile);

      if (root != null)
      {
        try
        {
          if (root.getSourceAttachmentPath() != null)
          {
            attachDecompiled = false;
          }
        }
        catch (JavaModelException e)
        {
          // Ignore exception and allow decompiled source
          // to be attached
        }
      }
    }

    if (!seenFirstSourceMapperCall && attachDecompiled)
    {
      try
      {
        return RealignmentJDSourceMapper.newSourceMapper(rootPath, sourcePath, sourceRootPath, options);
      }
      catch (Exception e)
      {
        JavaDecompilerPlugin
            .getDefault()
            .getLog()
            .log(
                new Status(Status.ERROR, JavaDecompilerPlugin.PLUGIN_ID, 0, e
                    .getMessage(), e));
      }
    }

    return null;
  }

  @Override
  public boolean isEditable()
  {
    return false;
  }
}