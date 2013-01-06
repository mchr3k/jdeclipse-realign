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
  public static final String PREF_STRIP_LINE_NUMBERS = JavaDecompilerPlugin.PLUGIN_ID +
                                                         ".prefs.HideLineNumbers";

  public Prefs()
  {
    super(SWT.NONE);
    setPreferenceStore(JavaDecompilerPlugin.getDefault().getPreferenceStore());
    setDescription("NOTE: 'Java/Decompiler/Display line numbers' " +
    		           "must be enabled for decompiled source to be " +
    		           "correctly realigned. However, line numbers can " +
    		           "be stripped from decompiled source here.");
  }

  public void init(IWorkbench workbench) {/* do nothing */}

  @Override
  protected void createFieldEditors()
  {
    Composite fieldEditorParent = getFieldEditorParent();

    new Label(fieldEditorParent, SWT.NONE);

    addField(new BooleanFieldEditor(PREF_STRIP_LINE_NUMBERS, "Strip line numbers", fieldEditorParent));
  }
}
