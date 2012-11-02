package jd.ide.eclipse.realignment.menu;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import jd.ide.eclipse.JavaDecompilerPlugin;
import jd.ide.eclipse.realignment.Startup;
import jd.ide.eclipse.realignment.editors.RealignmentJDSourceMapper;

import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.IEvaluationContext;
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
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageFragment;
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
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.SourceAttachmentBlock;
import org.eclipse.jdt.ui.wizards.BuildPathDialogAccess;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.ExtensionContributionFactory;
import org.eclipse.ui.menus.IContributionRoot;
import org.eclipse.ui.services.IServiceLocator;

@SuppressWarnings("restriction")
public class OpenClassWith extends ExtensionContributionFactory
{

  private final class OpenClassesAction extends Action
  {
    private final IEditorDescriptor classEditor;
    private final List<IClassFile> classes;

    private OpenClassesAction(IEditorDescriptor classEditor,
        List<IClassFile> classes)
    {
      this.classEditor = classEditor;
      this.classes = classes;
    }

    @Override
    public String getText()
    {
      return classEditor.getLabel();
    }

    @Override
    public ImageDescriptor getImageDescriptor()
    {
      return classEditor.getImageDescriptor();
    }

    @Override
    public void run()
    {
      // Get UI refs
      IWorkbenchWindow window = PlatformUI.getWorkbench()
          .getActiveWorkbenchWindow();
      if (window == null)
        return;
      IWorkbenchPage page = window.getActivePage();
      if (page == null)
        return;

      // Load each IClassFile into the selected editor
      for (IClassFile classfile : classes)
      {
        // Convert the IClassFile to an IEditorInput
        IEditorInput input = EditorUtility.getEditorInput(classfile);

        try
        {
          IEditorPart openEditor = page.openEditor(input, classEditor.getId(),
              true);

          if ((openEditor != null)
              && (!classEditor.getId().equals(
                  openEditor.getEditorSite().getId())))
          {
            // An existing editor already has this class open. Close it
            // and re-open in the correct editor
            if (!openEditor.isDirty())
            {
              openEditor.getSite().getPage().closeEditor(openEditor, false);
              page.openEditor(input, classEditor.getId(), true);
            }
          }

        }
        catch (PartInitException e)
        {
          JavaDecompilerPlugin
              .getDefault()
              .getLog()
              .log(
                  new Status(Status.ERROR, JavaDecompilerPlugin.PLUGIN_ID, 0, e
                      .getMessage(), e));
        }
      }
    }
  }

  @Override
  public void createContributionItems(IServiceLocator serviceLocator,
      IContributionRoot additions)
  {
    final ISelectionService selService = (ISelectionService) serviceLocator
        .getService(ISelectionService.class);

    // Define a dynamic set of submenu entries
    String dynamicMenuId = "jd.ide.eclipse.realignment.items";
    IContributionItem dynamicItems = new CompoundContributionItem(dynamicMenuId)
    {
      protected IContributionItem[] getContributionItems()
      {
        // Get the list of editors that can open a class file
        IEditorRegistry registry = PlatformUI.getWorkbench()
            .getEditorRegistry();

        // Get the current selections and return if nothing is selected
        Iterator<?> selections = getSelections(selService);
        if (selections == null)
          return new IContributionItem[0];

        // Attempt to find a single root
        final List<IJavaElement> elements = getSelectedElements(selService,
            IJavaElement.class);

        final Set<PackageFragmentRoot> roots = new HashSet<PackageFragmentRoot>();

        if (elements.size() > 0)
        {
          for (IJavaElement element : elements)
          {
            roots.add(getRoot(element));
          }
        }

        // Check which classes are selected
        final List<IClassFile> classes = getSelectedElements(selService,
            IClassFile.class);

        List<IContributionItem> list = new ArrayList<IContributionItem>();
        if (classes.size() > 0)
        {
          // Add an action to open all selected classes
          IEditorDescriptor jdtClassViewer = registry
              .findEditor(Startup.JDT_EDITOR_ID);
          list.add(new ActionContributionItem(new OpenClassesAction(
              jdtClassViewer, classes)));
        }

        if (roots.size() > 0)
        {
          if (list.size() > 0)
          {
            list.add(new Separator());
          }

          if (roots.size() == 1)
          {
            // Single package fragment root
            final PackageFragmentRoot root = roots.iterator().next();

            list.add(new ActionContributionItem(new Action("Decompiled Source",
                IAction.AS_CHECK_BOX)
            {
              {
                setChecked(isDecompilerAttached(root));
              }

              @Override
              public void run()
              {
                if (root != null)
                {
                  doDecompilerAttach(root);
                }
              }
            }));

            list.add(new ActionContributionItem(new Action()
            {
              @Override
              public String getText()
              {
                return "Attach Source...";
              }

              @Override
              public void run()
              {
                if (root != null)
                {
                  doSourceAttach(root);
                }
              }
            }));
          }
          else
          {
            // Multiple package fragment roots
            list.add(new ActionContributionItem(new Action("Enable Decompiled Source")
            {
              @Override
              public void run()
              {
                for (PackageFragmentRoot root : roots)
                {
                  if (!isDecompilerAttached(root))
                  {
                    doDecompilerAttach(root);
                  }
                }
              }
            }));
            list.add(new ActionContributionItem(new Action("Disable Decompiled Source")
            {
              @Override
              public void run()
              {
                for (PackageFragmentRoot root : roots)
                {
                  if (isDecompilerAttached(root))
                  {
                    System.out.println("Disable: " + root);
                    doDecompilerAttach(root);
                  }
                }
              }
            }));
          }
        }

        return list.toArray(new IContributionItem[list.size()]);
      }
    };

    // Determine menu name
    List<IClassFile> selectedClasses = getSelectedElements(selService,
        IClassFile.class);
    boolean openClassWith = (selectedClasses.size() > 0);
    String menuTitle = openClassWith ? "Open Class With" : "Attach Source";

    // Define dynamic submenu
    MenuManager submenu = new MenuManager(menuTitle, dynamicMenuId);
    submenu.add(dynamicItems);

    // Add the submenu and show it when classes are selected
    additions.addContributionItem(submenu, new Expression()
    {
      @Override
      public EvaluationResult evaluate(IEvaluationContext context)
          throws CoreException
      {
        boolean menuVisible = isMenuVisible(selService);

        if (menuVisible)
          return EvaluationResult.TRUE;

        return EvaluationResult.FALSE;
      }
    });
  }

  private boolean isDecompilerAttached(PackageFragmentRoot root)
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

  private void doDecompilerAttach(PackageFragmentRoot root)
  {
    try
    {
      SourceMapper existingMapper = root.getSourceMapper();

      if (existingMapper instanceof RealignmentJDSourceMapper)
      {
        // Remove decompiler attachment
        RealignmentJDSourceMapper jdSourceMapper = (RealignmentJDSourceMapper) existingMapper;
        root.setSourceMapper(null);
        RealignmentJDSourceMapper.clearDecompiled(jdSourceMapper);
      }
      else
      {
        // Prepare to add decompiler attachment by removing any existing source
        // attachment
        if (root.getSourceAttachmentPath() != null)
        {
          AtomicReference<IPath> containerPath = new AtomicReference<IPath>();
          IClasspathEntry entry = getClasspathEntry(root, containerPath);
          CPListElement cpElement = CPListElement.createFromExisting(entry,
              root.getJavaProject());
          cpElement.setAttribute(CPListElement.SOURCEATTACHMENT, null);

          Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
              .getShell();
          applySourceAttachment(shell, cpElement.getClasspathEntry(),
              root.getJavaProject(), containerPath.get(),
              entry.getReferencingEntry() != null);
        }

        // Source copied from
        // jd.ide.eclipse.editors.JDClassFileEditor

        // The location of the archive file containing classes.
        IPath classePath = root.getPath();
        // The location of the archive file containing source.
        IPath sourcePath = root.getSourceAttachmentPath();
        if (sourcePath == null)
          sourcePath = classePath;
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
            classePath, sourcePath, sourceRootPath, options);
        root.setSourceMapper(mapper);

        // Remove empty buffer
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
              PackageFragmentRoot bufRoot = getRoot(bufClassFile);

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

  private void doSourceAttach(PackageFragmentRoot root)
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

  private IClasspathEntry getClasspathEntry(PackageFragmentRoot root,
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

  private void applySourceAttachment(Shell shell, IClasspathEntry newEntry,
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

  private boolean isMenuVisible(ISelectionService selService)
  {

    Iterator<?> selections = getSelections(selService);

    boolean atLeastOneSelection = false;
    boolean allClasses = true;
    boolean singlePackageOrRoot = false;

    while ((selections != null) && selections.hasNext())
    {
      atLeastOneSelection = true;

      Object select = selections.next();

      if (!(select instanceof IClassFile))
      {
        allClasses = false;
      }

      if (((select instanceof IPackageFragment) || (select instanceof IPackageFragmentRoot)
          && (!selections.hasNext())))
      {
        singlePackageOrRoot = true;
      }
    }

    if (atLeastOneSelection)
    {
      if (allClasses || singlePackageOrRoot)
      {
        System.out.println("Menu visible: true");
        return true;
      }
    }

    System.out.println("Menu visible: false");
    return false;
  }

  @SuppressWarnings("unchecked")
  private <T> List<T> getSelectedElements(ISelectionService selService,
      Class<T> eleClass)
  {

    Iterator<?> selections = getSelections(selService);
    List<T> elements = new ArrayList<T>();

    while ((selections != null) && selections.hasNext())
    {
      Object select = selections.next();

      if (eleClass.isInstance(select))
        elements.add((T) select);
    }

    return elements;
  }

  private PackageFragmentRoot getRoot(IJavaElement javaElement)
  {

    PackageFragmentRoot root = null;

    // Search package fragment root.
    while ((javaElement != null)
        && (javaElement.getElementType() != IJavaElement.PACKAGE_FRAGMENT_ROOT))
    {
      javaElement = javaElement.getParent();
    }

    // Return the root
    if ((javaElement != null) && (javaElement instanceof PackageFragmentRoot))
    {
      root = (PackageFragmentRoot) javaElement;
    }
    return root;
  }

  private Iterator<?> getSelections(ISelectionService selService)
  {
    ISelection selection = selService.getSelection();

    if (selection != null)
    {
      if (selection instanceof IStructuredSelection)
      {
        IStructuredSelection structuredSelection = (IStructuredSelection) selection;
        return structuredSelection.iterator();
      }
    }

    return null;
  }

}
