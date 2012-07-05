package jd.ide.eclipse.realignment.preferences;

import jd.ide.eclipse.JavaDecompilerPlugin;
import jd.ide.eclipse.realignment.dialogs.BatchDecompilingDialog;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * RealignmentPreferencePage
 * 
 * @project Java Decompiler Eclipse Plugin
 * @author  Alex Kosinsky
 */
public class RealignmentPreferencePage
	extends FieldEditorPreferencePage implements IWorkbenchPreferencePage 
{
	public static final String REALIGNMENT_ON  = JavaDecompilerPlugin.PLUGIN_ID + ".realignment.prefs.REALIGNMENT_ON";	
	public static final String BUTTON_FIELD_EDITOR  = JavaDecompilerPlugin.PLUGIN_ID + ".realignment.prefs.BUTTON_FIELD_EDITOR";
	public static final String PATH_CLASS_FILES = JavaDecompilerPlugin.PLUGIN_ID + ".realignment.prefs.PATH_CLASS_FILES";
	public static final String PATH_JAVA_FILES = JavaDecompilerPlugin.PLUGIN_ID + ".realignment.prefs.PATH_JAVA_FILES";
	
	
	public RealignmentPreferencePage() 
	{
		super(SWT.NONE);
		setDescription("Executing batch realignment");
	}

	public void createFieldEditors() 
	{
		Composite fieldEditorParent = getFieldEditorParent();
		
		new Label(fieldEditorParent, SWT.NONE);
		
		ButtonFieldEditor editor=new  ButtonFieldEditor(BUTTON_FIELD_EDITOR,"Open dialog", fieldEditorParent) {
			protected void changePressed() {
				new BatchDecompilingDialog(getShell()).open();
			}
		};
		addField(editor); 		
	}
	
	
	public void init(IWorkbench workbench) {}
}


