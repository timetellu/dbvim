/**
 * 
 */
package org.dbvim.dbuibuilder.ui.db;

import org.dbvim.dbuibuilder.db.model.DBField;
import org.zkoss.zul.Longbox;

/**
 * @author peter.liverovsky
 *
 */
public class LongField extends BaseField<Long, Longbox> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7510213526283370164L;

	public LongField(DBField field) {
		super("/components/longfield.zul", field);
	}
	
	public LongField() {
		this(null);
	}
}