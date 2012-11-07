package jd.ide.eclipse.realignment.menu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import jd.ide.eclipse.JavaDecompilerPlugin;
import jd.ide.eclipse.realignment.Startup;

import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
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
            PackageFragmentRoot root = SelectUtils.getRoot(element);
            if (root != null)
            {
              roots.add(root);
            }
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
                setChecked(DecompUtils.isDecompilerAttached(root));
              }

              @Override
              public void run()
              {
                if (root != null)
                {
                  DecompUtils.doDecompilerAttach(root);
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
                  DecompUtils.doSourceAttach(root);
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
                  if (!DecompUtils.isDecompilerAttached(root))
                  {
                    DecompUtils.doDecompilerAttach(root);
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
                  if (DecompUtils.isDecompilerAttached(root))
                  {
                    DecompUtils.doDecompilerAttach(root);
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
        return true;
      }
    }

    return false;
  }

  private <T> List<T> getSelectedElements(ISelectionService selService,
      Class<T> eleClass)
  {
    Iterator<?> selections = getSelections(selService);
    return SelectUtils.getSelectedElements(selections, eleClass);
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
