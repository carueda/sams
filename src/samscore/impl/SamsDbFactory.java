package samscore.impl;

import samscore.ISamsDb;

/** 
 * Creates ISamsDb objects. 
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public final class SamsDbFactory {
	
	/** Opens an existing db. */
	public static ISamsDb open(String dirname) throws Exception {
		return SamsDb.open(dirname);
	}
	
	/** Creates a new db. */
	public static ISamsDb create(String dirname) throws Exception {
		return SamsDb.create(dirname);
	}
	
	private SamsDbFactory() {}
}
