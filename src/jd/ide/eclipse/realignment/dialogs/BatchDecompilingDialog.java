package jd.ide.eclipse.realignment.dialogs;

import jd.ide.eclipse.JavaDecompilerPlugin;
import jd.ide.eclipse.realignment.editors.BatchProcessing;
import jd.ide.eclipse.realignment.editors.RealignmentJDClassFileEditor;
import jd.ide.eclipse.realignment.editors.RealignmentJDSourceMapper;
import jd.ide.eclipse.realignment.preferences.RealignmentPreferencePage;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;


/**
 * BatchDecompilingDialog
 * 
 * @project Java Decompiler Eclipse Plugin
 * @author  Alex Kosinsky
 * 
 */

public class BatchDecompilingDialog extends Dialog{
	private Text textPathClassFiles;
	private Text textPathJavaFiles;
	private Button btnJar;
	private Button buttonRealigment;
	private Button button_OK;
	private Label lblPathClassFiles;
	private Label lblPathJavaFiles;
	private ProgressBar progressBar;
	private BatchProcessing batchProcessing;
	private Label lblProcessing;
	private Text txtProcessing;
	
    public int open () {

    	setBlockOnOpen(false);
		super.open();

		runEventLoop(getShell());

		return 0;
    }
	
	private void runEventLoop(Shell loopShell) {

		Display display = loopShell.getDisplay();

		int counter=0;
		while (loopShell != null && !loopShell.isDisposed()) {
			try {
				
				if(counter>100){
					if(batchProcessing!=null && batchProcessing.abort){
						button_OK.setEnabled(true);
						getShell().setCursor(new Cursor(getShell().getDisplay(), SWT.CURSOR_ARROW));
		    			txtProcessing.setText("");
		    			batchProcessing.abort=false;
					}
					if(batchProcessing!=null && batchProcessing.progressBarMin!=-1){
						progressBar.setMinimum(batchProcessing.progressBarMin);
						batchProcessing.progressBarMin=-1;
					}
					if(batchProcessing!=null && batchProcessing.progressBarMax!=-1){
						progressBar.setMaximum(batchProcessing.progressBarMax);
						batchProcessing.progressBarMax=-1;
					}
					if(batchProcessing!=null && batchProcessing.progressBarSelection!=-1){
						progressBar.setSelection(batchProcessing.progressBarSelection);
						counter=0;
					}
				}
				counter++;
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			} catch (Throwable ex) {
				JavaDecompilerPlugin.getDefault().getLog().log(new Status(
						Status.ERROR, JavaDecompilerPlugin.PLUGIN_ID, 
						0, ex.getMessage(), ex));
			}
		}
		if (!display.isDisposed()) display.update();
		batchProcessing.abort=true;
	}
	
	/**
	 * Create the dialog.
	 * @param parentShell
	 */
	public BatchDecompilingDialog(Shell parentShell) {
		super(parentShell);
	}

	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		this.getShell().setText("Batch Realignment");
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(null);
		
		lblPathClassFiles = new Label(container, SWT.NONE);
		lblPathClassFiles.setBounds(10, 10, 331, 18);
		lblPathClassFiles.setText("JD-GUI output zip-file:");
		
		textPathClassFiles = new Text(container, SWT.BORDER);
		textPathClassFiles.setBounds(10, 31, 367, 21);
		
		lblPathJavaFiles = new Label(container, SWT.NONE);
		lblPathJavaFiles.setBounds(10, 67, 331, 18);
		lblPathJavaFiles.setText("Path of java files after batch processing:");
		
		textPathJavaFiles = new Text(container, SWT.BORDER);
		textPathJavaFiles.setBounds(10, 89, 367, 21);
		
		progressBar = new ProgressBar(container, SWT.NONE);
		progressBar.setBounds(28, 195, 400, 18);
		
		lblProcessing = new Label(container, SWT.NONE);
		lblProcessing.setBounds(10, 150, 80, 18);
		lblProcessing.setText("Processing:");
		
		txtProcessing = new Text(container, SWT.BORDER);
		txtProcessing.setEditable(false);
		txtProcessing.setBounds(90, 149, 335, 21);

		
		IPreferenceStore store = 
			JavaDecompilerPlugin.getDefault().getPreferenceStore();
		if(store.contains(RealignmentPreferencePage.PATH_CLASS_FILES))
			textPathClassFiles.setText(store.getString(RealignmentPreferencePage.PATH_CLASS_FILES));
		if(store.contains(RealignmentPreferencePage.PATH_JAVA_FILES))
			textPathJavaFiles.setText(store.getString(RealignmentPreferencePage.PATH_JAVA_FILES));
		return container;
	}

	/**
	 * Create contents of the button bar.
	 * @param parent
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		button_OK = createButton(parent, IDialogConstants.YES_ID, IDialogConstants.OK_LABEL,
				true);
		button_OK.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				do_button_OK_widgetSelected(e);
			}
		});
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	protected Point getInitialSize() {
		return new Point(450, 300);
	}
	protected void do_button_OK_widgetSelected(SelectionEvent e) {
    	try {
    		final IPreferenceStore store = 
    			JavaDecompilerPlugin.getDefault().getPreferenceStore();
    		store.setValue(RealignmentPreferencePage.PATH_CLASS_FILES,textPathClassFiles.getText());
    		store.setValue(RealignmentPreferencePage.PATH_JAVA_FILES,textPathJavaFiles.getText());
    		batchProcessing=new BatchProcessing();
    		final String strPathClassFiles=textPathClassFiles.getText();
    		final String strPathJavaFiles=textPathJavaFiles.getText();
			txtProcessing.setText(strPathClassFiles);
    		Thread thread=new Thread(){
			   public void run() {
		    		try {
						batchProcessing.perform(strPathClassFiles, strPathJavaFiles);
					} catch (Exception e) {
						batchProcessing.abort=true;
						JavaDecompilerPlugin.getDefault().getLog().log(new Status(
								Status.ERROR, JavaDecompilerPlugin.PLUGIN_ID, 
								0, e.getMessage(), e));
					}
			   }
    		};
    		button_OK.setEnabled(false);
    		getShell().setCursor(new Cursor(getShell().getDisplay(), SWT.CURSOR_WAIT));
    		thread.start(); 
		} catch (Exception ex) {
			JavaDecompilerPlugin.getDefault().getLog().log(new Status(
					Status.ERROR, JavaDecompilerPlugin.PLUGIN_ID, 
					0, ex.getMessage(), ex));
		}
	}
	
	
	
	
	
}
