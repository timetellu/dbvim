/**
 * 
 */
package org.dbvim.dbuibuilder.ui;

import java.sql.SQLException;
import java.util.List;

import org.dbvim.dbuibuilder.db.model.DBField;
import org.dbvim.dbuibuilder.db.model.DBModel;
import org.dbvim.dbuibuilder.model.Form;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.MouseEvent;
import org.zkoss.zk.ui.select.Selectors;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

/**
 * @author peter.liverovsky
 *
 */
public class FormPropertiesDialog extends Window {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4348225150602100111L;
	
	@Wire
	Textbox txtFormName;
	
	@Wire
	Listbox lstAvailableFields;
	
	@Wire
	Listbox lstResultFields;
	
	@Wire
	Button btnAdd;
	
	@Wire
	Button btnRemove;
	
	@Wire
	Button btnOK;
	
	@Wire
	Button btnCancel;
	
	Form form;
	
	List<DBField> fields;

	public FormPropertiesDialog(Form form) throws ClassNotFoundException, SQLException {
		super();
		
		this.form = form;
		
		/* create the ui */
		Executions.createComponents("/components/FormProperties.zul", this, null);
		Selectors.wireVariables(this, this, null);
		Selectors.wireComponents(this, this, false);
		Selectors.wireEventListeners(this, this);
		setBorder("normal");
		setWidth("50%");
		setHeight("50%");
		setClosable(false);
		setTitle("Form name");
		txtFormName.setValue(form.getTitle());
		
		addEventListeners();
		
		initUI();
	}
	
	private void initUI() throws ClassNotFoundException, SQLException {
		DBModel model = new DBModel(form.getDBConnection().getConnectionString(), 
				form.getDBConnection().getClassName());
		
		fields = model.getFields(form.getCatalog(), form.getTableName());
		
		String[] resList = form.getResultList();
		
		// clear listbox
		lstAvailableFields.getItems().clear();
		lstResultFields.getItems().clear();
		
		for(DBField f : fields) {
			Listitem item = new Listitem();
			item.setValue(f.getName());
			Listcell cell = new Listcell();
			cell.setLabel(f.getName());
			item.appendChild(cell);
			// if field in form result list
			if (strContains(resList, f.getName()))
				lstResultFields.getItems().add(item);
			else
				lstAvailableFields.getItems().add(item);
		}
	}
	
	private boolean strContains(String[] arr, String str) {
		for (String s : arr) {
			if (s.equals(str))
				return true;
		}
		return false;
	}
	
	private void addEventListeners() {
		btnOK.addEventListener(Events.ON_CLICK, new EventListener<MouseEvent>() {

			@Override
			public void onEvent(MouseEvent arg0) throws Exception {
				btnOK_onClick();
			}
			
		});
		
		btnCancel.addEventListener(Events.ON_CLICK, new EventListener<MouseEvent>() {

			@Override
			public void onEvent(MouseEvent arg0) throws Exception {
				btnCancel_onClick();
			}
			
		});
		
		btnAdd.addEventListener(Events.ON_CLICK, new EventListener<MouseEvent>() {

			@Override
			public void onEvent(MouseEvent arg0) throws Exception {
				btnAdd_onClick();
			}
			
		});
		
		btnRemove.addEventListener(Events.ON_CLICK, new EventListener<MouseEvent>() {

			@Override
			public void onEvent(MouseEvent arg0) throws Exception {
				btnRemove_onClick();
			}
			
		});
	}
	
	protected void btnRemove_onClick() {
		if (lstResultFields.getSelectedItem() != null) {
			Listitem item = lstResultFields.getSelectedItem();
			lstResultFields.getItems().remove(item);
			lstAvailableFields.getItems().add(item);
		}
	}

	protected void btnAdd_onClick() {
		if (lstAvailableFields.getSelectedItem() != null) {
			Listitem item = lstAvailableFields.getSelectedItem();
			lstAvailableFields.getItems().remove(item);
			lstResultFields.getItems().add(item);
		}
	}

	protected void btnCancel_onClick() {
		Event closeEvent = new Event(Events.ON_CLOSE, this);
		Events.postEvent(closeEvent);
		detach();
	}

	private void btnOK_onClick() {
		if (lstResultFields.getItemCount() == 0) {
			Messagebox.show("Result list can not be empty.");
			return;
		}
		String resLst = "";
		for (int i=0; i<lstResultFields.getItemCount(); i++) {
			resLst += (String) lstResultFields.getItems().get(i).getValue();
			if (i!=lstResultFields.getItemCount()-1)
				resLst += ";";
		}
		form.setsResultList(resLst);
		form.setTitle(txtFormName.getValue());
		
		// Close dialog
		Event closeEvent = new Event(Events.ON_CLOSE, this);
		Events.postEvent(closeEvent);
		detach();
	}
}
