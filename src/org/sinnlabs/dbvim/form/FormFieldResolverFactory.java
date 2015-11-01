/**
 * 
 */
package org.sinnlabs.dbvim.form;

import java.util.concurrent.ConcurrentHashMap;

import org.sinnlabs.dbvim.model.Form;

/**
 * @author peter.liverovsky
 *
 */
public class FormFieldResolverFactory {
	
	private final static ConcurrentHashMap<String, FormFieldResolver> cache;

	static {
		cache = new ConcurrentHashMap<String, FormFieldResolver>();
	}
	
	/**
	 * Returns the resolver instance
	 * @param f Form
	 * @return FormFieldResolver for the Form
	 * @throws Exception
	 */
	public static FormFieldResolver getResolver(Form f) throws Exception {
		if (!cache.containsKey(f.getName())) {
			synchronized(cache) {
				// second check with lock
				if (!cache.containsKey(f.getName())) {
					FormFieldResolver r = new FormFieldResolver(f);
					cache.put(f.getName(), r);
					return r;
				}
				return cache.get(f.getName());
			}
		} else {
			return cache.get(f.getName());
		}
	}
}
