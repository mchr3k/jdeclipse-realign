package jd.ide.eclipse.realignment.menu;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jd.ide.eclipse.JavaDecompilerPlugin;
import jd.ide.eclipse.realignment.Startup;

import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.SourceAttachmentBlock;
import org.eclipse.jdt.ui.wizards.BuildPathDialogAccess;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
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
public class OpenClassWith extends ExtensionContributionFactory {

	@Override
	public void createContributionItems(IServiceLocator serviceLocator,
			IContributionRoot additions) {

		final ISelectionService selService = (ISelectionService) serviceLocator.getService(ISelectionService.class);

		// Define a dynamic set of submenu entries
        IContributionItem dynamicItems = new CompoundContributionItem(
                "jd.ide.eclipse.realignment.test.items") {
            protected IContributionItem[] getContributionItems() {

            	// Get the list of editors that can open a class file
            	IEditorRegistry registry =
 			           PlatformUI.getWorkbench().getEditorRegistry();
            	IEditorDescriptor[] classEditors = registry.getEditors("example.class");

            	// Work out if both the default JD and Realign-JD entries are in the list
            	int jdEditors = 0;
            	for (IEditorDescriptor classEditor : classEditors)
            	{
            		if (Startup.EDITOR_ID.equals(classEditor.getId()))
            			jdEditors++;
            		if (classEditor.getId().startsWith(Startup.JD_EDITOR_ID))
            			jdEditors++;
            	}

            	// Work out how many entries we will generate and subtract one if we
            	// saw both editors
            	int size = classEditors.length;
            	if (jdEditors >= 2)
            		size--;

            	// If a single class has been selected - add space for a separator
            	// and an attach source dialog
            	boolean singleSelection = isSingleClassSelection(selService);
            	if (singleSelection)
            		size += 2;

            	// Fill in editor items
                IContributionItem[] list = new IContributionItem[size];
                int offset = 0;
                for (int ii = 0; ii < classEditors.length; ii++)
                {
                	final IEditorDescriptor classEditor = classEditors[ii];

                	// Skip default JD Editor if we also saw Realign-JD editor
                	if ((size >= 2) && (classEditor.getId().startsWith(Startup.JD_EDITOR_ID)))
                	{
                		offset = 1;
                		continue;
                	}

                	// Create Action item
                    list[ii - offset] = new ActionContributionItem(new Action() {
                    	@Override
                    	public String getText() {
                    		return classEditor.getLabel();
                    	}
                    	@Override
                    	public ImageDescriptor getImageDescriptor() {
                    		return classEditor.getImageDescriptor();
                    	}
						@Override
                    	public void run() {
                			// Get UI refs
							IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                			if (window == null)
                				return;
                			IWorkbenchPage page = window.getActivePage();
                			if (page == null)
                				return;

                			// Get selection
							List<IClassFile> classes = getSelectedClasses(selService);

                    		// Load each IClassFile into the selected editor
                    		for (IClassFile classfile : classes)
                    		{
                    			// Convert the IClassFile to an IEditorInput
                    			IEditorInput input = EditorUtility.getEditorInput(classfile);

                    			try {
									page.openEditor(input, classEditor.getId(), true);
								} catch (PartInitException e) {
									JavaDecompilerPlugin.getDefault().getLog().log(new Status(
											Status.ERROR, JavaDecompilerPlugin.PLUGIN_ID,
											0, e.getMessage(), e));
								}
                    		}
                    	}
                    });
                }

                if (singleSelection)
                {
	                list[list.length - 2] = new Separator();
	                list[list.length - 1] = new ActionContributionItem(new Action() {
	                	@Override
	                	public String getText() {
	                		return "Attach Source...";
	                	}
	                	@Override
	                	public void run() {
	                		List<IClassFile> classes = getSelectedClasses(selService);
	                		IClassFile classFile = classes.get(0);

	            			// Search package fragment root.
	            			IJavaElement javaElement = classFile.getParent();
	            			while ((javaElement != null) &&
	            				   (javaElement.getElementType() !=
	            					   IJavaElement.PACKAGE_FRAGMENT_ROOT))
	            			{
	            				javaElement = javaElement.getParent();
	            			}

	            			// Attach source to the root
	            			if ((javaElement != null) &&
	            				(javaElement instanceof PackageFragmentRoot))
	            			{
	            				PackageFragmentRoot root = (PackageFragmentRoot)javaElement;
	            				doSourceAttach(root);
	            			}
	                	}
					});
                }

                return list;
            }
        };

		// Define dynamic submenu
        MenuManager submenu = new MenuManager("Open Class With",
                "jd.ide.eclipse.realignment.test.menu");
        submenu.add(dynamicItems);

        // Add the submenu and show it when classes are selected
        additions.addContributionItem(submenu, new Expression() {
			@Override
			public EvaluationResult evaluate(IEvaluationContext context)
					throws CoreException {
				boolean classSelection = isMenuVisible(selService);

				if (classSelection)
					return EvaluationResult.TRUE;

			    return EvaluationResult.FALSE;
			}
		});
	}

	private void doSourceAttach(PackageFragmentRoot root)
	{
		// Source copied from
		// org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditor$SourceAttachmentForm

		IClasspathEntry entry;
		try {
			entry= JavaModelUtil.getClasspathEntry(root);
		} catch (JavaModelException ex) {
			if (ex.isDoesNotExist())
				entry= null;
			else
				return;
		}
		IPath containerPath= null;
		IJavaProject jproject = null;

		try
		{
			if (entry == null || root.getKind() != IPackageFragmentRoot.K_BINARY) {
				return;
			}

			jproject= root.getJavaProject();
			if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
				containerPath= entry.getPath();
				ClasspathContainerInitializer initializer= JavaCore.getClasspathContainerInitializer(containerPath.segment(0));
				IClasspathContainer container= JavaCore.getClasspathContainer(containerPath, jproject);
				if (initializer == null || container == null) {
					return;
				}
				IStatus status= initializer.getSourceAttachmentStatus(containerPath, jproject);
				if (status.getCode() == ClasspathContainerInitializer.ATTRIBUTE_NOT_SUPPORTED) {
					return;
				}
				if (status.getCode() == ClasspathContainerInitializer.ATTRIBUTE_READ_ONLY) {
					return;
				}
				entry= JavaModelUtil.findEntryInContainer(container, root.getPath());
			}
		}
		catch (JavaModelException e)
		{
			JavaDecompilerPlugin.getDefault().getLog().log(new Status(
					Status.ERROR, JavaDecompilerPlugin.PLUGIN_ID,
					0, e.getMessage(), e));
			return;
		}

		if (entry == null)
			return;

		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		IClasspathEntry result= BuildPathDialogAccess.configureSourceAttachment(shell, entry);
		if (result != null) {
			applySourceAttachment(shell, result, jproject, containerPath, entry.getReferencingEntry() != null);
		}
	}

	private void applySourceAttachment(Shell shell, IClasspathEntry newEntry, IJavaProject project, IPath containerPath, boolean isReferencedEntry) {
		try {
			IRunnableWithProgress runnable= SourceAttachmentBlock.getRunnable(shell, newEntry, project, containerPath, isReferencedEntry);
			PlatformUI.getWorkbench().getProgressService().run(true, true, runnable);
		} catch (InvocationTargetException e) {
			JavaDecompilerPlugin.getDefault().getLog().log(new Status(
					Status.ERROR, JavaDecompilerPlugin.PLUGIN_ID,
					0, e.getMessage(), e));
		} catch (InterruptedException e) {
			// cancelled
		}
	}

	private boolean isMenuVisible(ISelectionService selService) {
		ISelection selection = selService.getSelection();

		if (selection != null)
		{
			if (selection instanceof IStructuredSelection)
			{
				IStructuredSelection structuredSelection = (IStructuredSelection) selection;
				Iterator<?> selections = structuredSelection.iterator();

				while (selections.hasNext())
				{
					Object select = selections.next();

					if (!(select instanceof IClassFile))
						return false;
				}
			}
		}

    	IEditorRegistry registry =
		           PlatformUI.getWorkbench().getEditorRegistry();
		IEditorDescriptor[] classEditors = registry.getEditors("example.class");
		if (classEditors.length == 0)
			return false;

		return true;
	}

	private List<IClassFile> getSelectedClasses(ISelectionService selService) {
		ISelection selection = selService.getSelection();

		List<IClassFile> classes = new ArrayList<IClassFile>();

		if (selection != null)
		{
			if (selection instanceof IStructuredSelection)
			{
				IStructuredSelection structuredSelection = (IStructuredSelection) selection;
				Iterator<?> selections = structuredSelection.iterator();

				while (selections.hasNext())
				{
					Object select = selections.next();

					if (select instanceof IClassFile)
						classes.add((IClassFile)select);
				}
			}
		}

		return classes;
	}

	private boolean isSingleClassSelection(ISelectionService selService) {
		ISelection selection = selService.getSelection();

		if (selection != null)
		{
			if (selection instanceof IStructuredSelection)
			{
				IStructuredSelection structuredSelection = (IStructuredSelection) selection;
				Iterator<?> selections = structuredSelection.iterator();

				while (selections.hasNext())
				{
					Object select = selections.next();

					if ((select instanceof IClassFile) && (!selections.hasNext()))
						return true;
					else
						return false;
				}
			}
		}

		return false;
	}

}
