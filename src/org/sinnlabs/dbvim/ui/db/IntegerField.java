/**
 * 
 */
package org.sinnlabs.dbvim.ui.db;

import org.sinnlabs.dbvim.db.Value;
import org.sinnlabs.dbvim.db.model.DBField;
import org.zkoss.zul.Intbox;

/**
 * @author peter.liverovsky
 *
 */
public class IntegerField extends BaseField<Integer, Intbox> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5904828500284669425L;

	public IntegerField(DBField dbfield) {
		super("/components/integerfield.zul", dbfield);
	}
	
	public IntegerField() {
		this(null);
	}

	/* (non-Javadoc)
	 * @see org.sinnlabs.dbvim.ui.IField#fromString(java.lang.String)
	 */
	@Override
	public Value<Integer> fromString(String string) {
		if (string == null)
			return new Value<Integer>(null, dbField);
		return new Value<Integer>(Integer.valueOf(string), dbField);
	}
}
