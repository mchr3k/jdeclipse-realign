package jd.ide.eclipse.realignment.menu;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import jd.ide.eclipse.JavaDecompilerPlugin;
import jd.ide.eclipse.realignment.editors.RealignmentJDSourceMapper;
import jd.ide.eclipse.realignment.editors.RealignmentJDSourceMapper.SourceAttachmentDetails;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.BufferManager;
import org.eclipse.jdt.internal.core.DeltaProcessor;
import org.eclipse.jdt.internal.core.JavaElementDelta;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;
import org.eclipse.jdt.internal.core.SourceMapper;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.SourceAttachmentBlock;
import org.eclipse.jdt.ui.wizards.BuildPathDialogAccess;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

@SuppressWarnings("restriction")
public class DecompUtils
{
  public static boolean isDecompilerAttached(PackageFragmentRoot root)
  {
    if (root != null)
    {
      if (root.getSourceMapper() instanceof RealignmentJDSourceMapper)
      {
        return true;
      }
    }
    return false;
  }

  public static void doSourceAttach(PackageFragmentRoot root)
  {
    // Source copied from
    // org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditor$SourceAttachmentForm

    IClasspathEntry entry;
    try
    {
      entry = JavaModelUtil.getClasspathEntry(root);
    }
    catch (JavaModelException ex)
    {
      if (ex.isDoesNotExist())
        entry = null;
      else
        return;
    }
    IPath containerPath = null;
    IJavaProject jproject = null;

    try
    {
      if (entry == null || root.getKind() != IPackageFragmentRoot.K_BINARY)
      {
        return;
      }

      jproject = root.getJavaProject();
      if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER)
      {
        containerPath = entry.getPath();
        ClasspathContainerInitializer initializer = JavaCore
            .getClasspathContainerInitializer(containerPath.segment(0));
        IClasspathContainer container = JavaCore.getClasspathContainer(
            containerPath, jproject);
        if (initializer == null || container == null)
        {
          return;
        }
        IStatus status = initializer.getSourceAttachmentStatus(containerPath,
            jproject);
        if (status.getCode() == ClasspathContainerInitializer.ATTRIBUTE_NOT_SUPPORTED)
        {
          return;
        }
        if (status.getCode() == ClasspathContainerInitializer.ATTRIBUTE_READ_ONLY)
        {
          return;
        }
        entry = JavaModelUtil.findEntryInContainer(container, root.getPath());
      }
    }
    catch (JavaModelException e)
    {
      JavaDecompilerPlugin
          .getDefault()
          .getLog()
          .log(
              new Status(Status.ERROR, JavaDecompilerPlugin.PLUGIN_ID, 0, e
                  .getMessage(), e));
      return;
    }

    if (entry == null)
      return;

    Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
        .getShell();
    IClasspathEntry result = BuildPathDialogAccess.configureSourceAttachment(
        shell, entry);
    if (result != null)
    {
      applySourceAttachment(shell, result, jproject, containerPath,
          entry.getReferencingEntry() != null);
    }
  }

  public static void doDecompilerAttach(PackageFragmentRoot root)
  {
    try
    {
      Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
      SourceMapper existingMapper = root.getSourceMapper();

      if (existingMapper instanceof RealignmentJDSourceMapper)
      {
        // Remove decompiler attachment
        RealignmentJDSourceMapper jdSourceMapper = (RealignmentJDSourceMapper) existingMapper;
        root.setSourceMapper(null);
        RealignmentJDSourceMapper.clearDecompiled(jdSourceMapper);

        // Re-attach any source which was previously attached
        SourceAttachmentDetails details = jdSourceMapper.sourceDetails;
        if (details != null)
        {
          applySourceAttachment(shell, details.newEntry,
              details.project, details.containerPath,
              details.isReferencedEntry);
        }
      }
      else
      {
        SourceAttachmentDetails details = null;
        if (root.getSourceAttachmentPath() != null)
        {
          AtomicReference<IPath> containerPath = new AtomicReference<IPath>();
          IClasspathEntry entry = getClasspathEntry(root, containerPath);

          // Backup existing attachment details
          CPListElement backupCpElement = CPListElement.createFromExisting(entry,
                                                           root.getJavaProject());
          details = new SourceAttachmentDetails(backupCpElement.getClasspathEntry(),
                                                root.getJavaProject(),
                                                containerPath.get(),
                                                entry.getReferencingEntry() != null);
        }

        // Source copied from
        // jd.ide.eclipse.editors.JDClassFileEditor

        // The location of the archive file containing classes.
        IPath classPath = root.getPath();
        // The location of the archive file containing source.
        IPath sourcePath = root.getSourceAttachmentPath();
        if (sourcePath == null)
          sourcePath = classPath;
        // Specifies the location of the package fragment root
        // within the zip (empty specifies the default root).
        IPath sourceAttachmentRootPath = root.getSourceAttachmentRootPath();
        String sourceRootPath;
        if (sourceAttachmentRootPath == null)
        {
          sourceRootPath = null;
        }
        else
        {
          sourceRootPath = sourceAttachmentRootPath.toString();
          if ((sourceRootPath != null) && (sourceRootPath.length() == 0))
            sourceRootPath = null;
        }
        Map<?, ?> options = root.getJavaProject().getOptions(true);

        // Create source mapper
        SourceMapper mapper = RealignmentJDSourceMapper.newSourceMapper(
                       classPath, sourcePath, sourceRootPath, options, details);
        root.setSourceMapper(mapper);
      }

      // Remove any buffers associated with the root being modified
      try
      {
        Method method = BufferManager.class.getDeclaredMethod("removeBuffer",
            new Class[]
            { IBuffer.class });
        method.setAccessible(true);

        Enumeration<?> openBuffers = BufferManager.getDefaultBufferManager()
            .getOpenBuffers();
        while (openBuffers.hasMoreElements())
        {
          IBuffer buffer = (IBuffer) openBuffers.nextElement();
          IOpenable owner = buffer.getOwner();

          if (owner instanceof IClassFile)
          {
            IClassFile bufClassFile = (IClassFile) owner;
            PackageFragmentRoot bufRoot = SelectUtils.getRoot(bufClassFile);

            if (root.equals(bufRoot))
            {
              // Remove any empty buffer
              method.invoke(BufferManager.getDefaultBufferManager(),
                  new Object[]
                  { buffer });
            }
          }
        }
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

      // Construct a delta to indicate that the source attachment has changed
      JavaModelManager modelManager = JavaModelManager.getJavaModelManager();
      JavaElementDelta delta = new JavaElementDelta(modelManager.getJavaModel());
      delta.changed(root.getJavaProject(),
          IJavaElementDelta.F_RESOLVED_CLASSPATH_CHANGED);
      delta.changed(root, IJavaElementDelta.F_SOURCEATTACHED);

      // Notify Eclipse that the source attachment has changed
      DeltaProcessor deltaProcessor = modelManager.getDeltaProcessor();
      deltaProcessor.fire(delta, ElementChangedEvent.POST_CHANGE);
    }
    catch (CoreException e)
    {
      JavaDecompilerPlugin
          .getDefault()
          .getLog()
          .log(
              new Status(Status.ERROR, JavaDecompilerPlugin.PLUGIN_ID, 0, e
                  .getMessage(), e));
    }
  }

  private static IClasspathEntry getClasspathEntry(PackageFragmentRoot root,
      AtomicReference<IPath> path)
  {
    // Source copied from
    // org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditor$SourceAttachmentForm

    IClasspathEntry entry;
    try
    {
      entry = JavaModelUtil.getClasspathEntry(root);
    }
    catch (JavaModelException ex)
    {
      if (ex.isDoesNotExist())
        entry = null;
      else
        return null;
    }
    IPath containerPath = null;
    IJavaProject jproject = null;

    try
    {
      if (entry == null || root.getKind() != IPackageFragmentRoot.K_BINARY)
      {
        return null;
      }

      jproject = root.getJavaProject();
      if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER)
      {
        containerPath = entry.getPath();
        ClasspathContainerInitializer initializer = JavaCore
            .getClasspathContainerInitializer(containerPath.segment(0));
        IClasspathContainer container = JavaCore.getClasspathContainer(
            containerPath, jproject);
        if (initializer == null || container == null)
        {
          return null;
        }
        IStatus status = initializer.getSourceAttachmentStatus(containerPath,
            jproject);
        if (status.getCode() == ClasspathContainerInitializer.ATTRIBUTE_NOT_SUPPORTED)
        {
          return null;
        }
        if (status.getCode() == ClasspathContainerInitializer.ATTRIBUTE_READ_ONLY)
        {
          return null;
        }
        entry = JavaModelUtil.findEntryInContainer(container, root.getPath());
      }
    }
    catch (JavaModelException e)
    {
      JavaDecompilerPlugin
          .getDefault()
          .getLog()
          .log(
              new Status(Status.ERROR, JavaDecompilerPlugin.PLUGIN_ID, 0, e
                  .getMessage(), e));
      return null;
    }

    path.set(containerPath);

    return entry;
  }

  private static void applySourceAttachment(Shell shell, IClasspathEntry newEntry,
      IJavaProject project, IPath containerPath, boolean isReferencedEntry)
  {
    try
    {
      IRunnableWithProgress runnable = SourceAttachmentBlock.getRunnable(shell,
          newEntry, project, containerPath, isReferencedEntry);
      PlatformUI.getWorkbench().getProgressService().run(true, true, runnable);
    }
    catch (InvocationTargetException e)
    {
      JavaDecompilerPlugin
          .getDefault()
          .getLog()
          .log(
              new Status(Status.ERROR, JavaDecompilerPlugin.PLUGIN_ID, 0, e
                  .getMessage(), e));
    }
    catch (InterruptedException e)
    {
      // cancelled
    }
  }
}
