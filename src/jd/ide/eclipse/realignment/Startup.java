package jd.ide.eclipse.realignment;

import jd.ide.eclipse.JavaDecompilerPlugin;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IFileEditorMapping;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;

public class Startup implements IStartup {

	// The plug-in IDs
	private static final String EDITOR_ID  = "realignment.editor.jd.ide.eclipse";

	// External plug-in IDs
	private static final String JDT_EDITOR_ID  = "org.eclipse.jdt.ui.ClassFileEditor";

	// Preferences
	public  static final String PLUGIN_ID      = "jd.ide.eclipse.realignment";
	public static final String PREF_SETUP      = PLUGIN_ID + ".prefs.Setup";

	public void earlyStartup() {

		IPreferenceStore store = JavaDecompilerPlugin.getDefault().getPreferenceStore();

		// Is the first launch ?
		if (store.getBoolean(PREF_SETUP) == false)
		{
			// Setup ".class" file association
			Display.getDefault().syncExec(new SetupClassFileAssociationRunnable());
			store.setValue(PREF_SETUP, true);
		}
	}


	private static class SetupClassFileAssociationRunnable implements Runnable
	{
		public void run()
		{
			IEditorRegistry registry =
			           PlatformUI.getWorkbench().getEditorRegistry();

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
			IEditorDescriptor jdtClassViewer = registry.findEditor(JDT_EDITOR_ID);

			if (classNoSource != null)
			{
				// Got a "class without source" type - default to handle this and un-default from
				// the "class" type
				registry.setDefaultEditor("." + classNoSource.getExtension(), EDITOR_ID);

				if ((classPlain != null) && (jdtClassViewer != null))
				{
					// Restore the default class viewer as the default "class with source" viewer
					registry.setDefaultEditor("." + classPlain.getExtension(), JDT_EDITOR_ID);
				}
			}
			else if (classPlain != null)
			{
				// Only got a class file type - default to decompile this
				registry.setDefaultEditor("." + classPlain.getExtension(), EDITOR_ID);
			}
		}
	}

}
