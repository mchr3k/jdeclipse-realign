package jd.ide.eclipse.realignment;

import jd.ide.eclipse.JavaDecompilerPlugin;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class Prefs extends FieldEditorPreferencePage implements
    IWorkbenchPreferencePage
{
  public static final String PREF_DISPLAY_LINE_NUMBERS = JavaDecompilerPlugin.PLUGIN_ID +
                                                         ".prefs.realign.DisplayLineNumbers";

  public Prefs()
  {
    super(SWT.NONE);
    setPreferenceStore(JavaDecompilerPlugin.getDefault().getPreferenceStore());
    setDescription("NOTE: 'Java/Decompiler/Display line numbers' " +
    		           "must be enabled for decompiled source to be " +
    		           "correctly realigned. However, displaying line " +
    		           "numbers can be disabled here.");
  }

  public void init(IWorkbench workbench) {/* do nothing */}

  @Override
  protected void createFieldEditors()
  {
    Composite fieldEditorParent = getFieldEditorParent();

    new Label(fieldEditorParent, SWT.NONE);

    addField(new BooleanFieldEditor(PREF_DISPLAY_LINE_NUMBERS, "Display line numbers", fieldEditorParent));
  }
}
