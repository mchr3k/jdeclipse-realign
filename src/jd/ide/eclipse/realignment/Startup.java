package jd.ide.eclipse.realignment;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IFileEditorMapping;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.registry.EditorDescriptor;
import org.eclipse.ui.internal.registry.EditorRegistry;
import org.eclipse.ui.internal.registry.FileEditorMapping;

@SuppressWarnings("restriction")
public class Startup implements IStartup {

	// The plug-in IDs
	public static final String EDITOR_ID  = "realignment.editor.jd.ide.eclipse";
	public static final String JD_EDITOR_ID = "jd.ide.eclipse.editors.JDClassFileEditor";

	// External plug-in IDs
	public static final String JDT_EDITOR_ID  = "org.eclipse.jdt.ui.ClassFileEditor";

	// Preferences
	public  static final String PLUGIN_ID      = "jd.ide.eclipse.realignment";
	public static final String PREF_SETUP      = PLUGIN_ID + ".prefs.Setup";

	public void earlyStartup() {
		// Setup ".class" file association
		Display.getDefault().syncExec(new SetupClassFileAssociationRunnable());
	}


	private static class SetupClassFileAssociationRunnable implements Runnable
	{
		public void run()
		{
			EditorRegistry registry =
			           (EditorRegistry) PlatformUI.getWorkbench().getEditorRegistry();

			// Will not work because this will not persist across sessions
			// registry.setDefaultEditor("*.class", id);

			IFileEditorMapping[] mappings = registry.getFileEditorMappings();

			// Search Class file editor mappings
			IFileEditorMapping classNoSource = null;
			IFileEditorMapping classPlain = null;
			for (IFileEditorMapping mapping : mappings)
			{
				if (mapping.getExtension().equals("class without source"))
				{
					classNoSource = mapping;
			    }
				else if (mapping.getExtension().equals("class"))
				{
					classPlain = mapping;
				}
			}

			// On start:
			// * Revert the class mappings to use the JDT Class File Viewer
			// * Delete any file mappings which include the JD or JD-Realign editors

			for (IFileEditorMapping mapping : new IFileEditorMapping[] {classNoSource, classPlain})
			{
				if (mapping != null)
				{
					IEditorDescriptor defaultEditor = mapping.getDefaultEditor();
					if ((defaultEditor == null) ||
					    (defaultEditor.getId().startsWith(JD_EDITOR_ID)) ||
					    (defaultEditor.getId().equals(EDITOR_ID)))
					{
						registry.setDefaultEditor("." + mapping.getExtension(), JDT_EDITOR_ID);
					}

					// Unmap the JD-Eclipse and JD-Eclipse-Realign editors
					for (IEditorDescriptor editor : mapping.getEditors())
					{
						if ((editor.getId().startsWith(JD_EDITOR_ID)) ||
							(editor.getId().equals(EDITOR_ID)))
						{
							((FileEditorMapping)mapping).removeEditor((EditorDescriptor) editor);
						}
					}
				}
			}

			// Save updates
			registry.setFileEditorMappings((FileEditorMapping[]) mappings);
			registry.saveAssociations();
		}
	}

}
