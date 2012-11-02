package jd.ide.eclipse.realignment.menu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;
import org.eclipse.jdt.ui.IPackagesViewPart;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;

@SuppressWarnings("restriction")
public class SelectUtils
{
  public static Set<PackageFragmentRoot> getSelectedRoots()
  {
    Set<PackageFragmentRoot> roots = new HashSet<PackageFragmentRoot>();

    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    IWorkbenchPage activePage = window.getActivePage();

    if (activePage != null)
    {
      IWorkbenchPart part = activePage.getActivePart();

      if (part != null)
      {
        if (part instanceof IEditorPart)
        {
          IEditorPart editor = (IEditorPart) part;
          IClassFile classFile = (IClassFile)editor.getEditorInput().getAdapter(IClassFile.class);
          if (classFile != null)
          {
            PackageFragmentRoot root = SelectUtils.getRoot(classFile);

            if (root != null)
            {
              roots.add(root);
            }
          }
        }
        else if (part instanceof IViewPart)
        {
          IViewPart view = (IViewPart) part;

          ISelection selection = null;

          if (view instanceof IPackagesViewPart)
          {
            IPackagesViewPart viewPart = (IPackagesViewPart) view;
            TreeViewer treeViewer = viewPart.getTreeViewer();
            selection = treeViewer.getSelection();
          }
          else if (view instanceof CommonNavigator)
          {
            CommonNavigator navigator = (CommonNavigator) view;
            CommonViewer commonViewer = navigator.getCommonViewer();
            selection = commonViewer.getSelection();
          }

          if (selection instanceof IStructuredSelection)
          {
            IStructuredSelection structuredSelection = (IStructuredSelection) selection;

            final List<IJavaElement> elements = SelectUtils.getSelectedElements(
                                                            structuredSelection.iterator(),
                                                            IJavaElement.class);

            for (IJavaElement element : elements)
            {
              PackageFragmentRoot root = SelectUtils.getRoot(element);

              if (root != null)
              {
                roots.add(root);
              }
            }
          }
        }
      }
    }

    return roots;
  }

  @SuppressWarnings("unchecked")
  public static <T> List<T> getSelectedElements(Iterator<?> selections,
                                          Class<T> eleClass)
  {
    List<T> elements = new ArrayList<T>();

    while ((selections != null) && selections.hasNext())
    {
      Object select = selections.next();

      if (eleClass.isInstance(select))
        elements.add((T) select);
    }

    return elements;
  }

  public static PackageFragmentRoot getRoot(IJavaElement javaElement)
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
}
