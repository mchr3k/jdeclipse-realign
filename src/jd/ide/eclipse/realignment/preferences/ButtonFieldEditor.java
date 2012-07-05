package jd.ide.eclipse.realignment.preferences;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public abstract class ButtonFieldEditor extends FieldEditor{
	Button changeButton;
	ButtonFieldEditor(String name,String buttonText,Composite parent){
		super(name,buttonText,parent);
	}

	protected void doFillIntoGrid(Composite parent, int numColumns) {
        changeButton = getChangeControl(parent);
        GridData gd = new GridData();
        gd.horizontalAlignment = GridData.FILL;
        int widthHint = convertHorizontalDLUsToPixels(changeButton,
                IDialogConstants.BUTTON_WIDTH);
        gd.widthHint = Math.max(widthHint, changeButton.computeSize(
                SWT.DEFAULT, SWT.DEFAULT, true).x);
        changeButton.setLayoutData(gd);
	}

    protected Button getChangeControl(Composite parent) {
        if (changeButton == null) {
            changeButton = new Button(parent, SWT.PUSH);
            changeButton.setText(getLabelText());
            changeButton.setFont(parent.getFont());
            changeButton.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent evt) {
                    changePressed();
                }
            });
            changeButton.addDisposeListener(new DisposeListener() {
                public void widgetDisposed(DisposeEvent event) {
                    changeButton = null;
                }
            });
        } else {
            checkParent(changeButton, parent);
        }
        return changeButton;
    }
	public int getNumberOfControls(){return 1;}
	protected void adjustForNumColumns(int numColumns) {}
	protected void doLoad() {}
	protected void doLoadDefault() {}
	protected void doStore() {}
    protected abstract void changePressed();	
}
