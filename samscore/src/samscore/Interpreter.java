package samscore;

import samscore.ISamsDb;
import samscore.ISamsDb.*;
import samscore.ISamsDb.IMetadataDef.IAttributeDef;
import envifile.EnviDataType;
import specfile.ISpectrumFile;
import sig.Signature;
import sigoper.OpUtil;
import fileutils.Files;
import sfsys.Shell;
import sfsys.ISfsys;
import sfsys.ISfsys.INode;

import java.util.*;
import java.io.*;

/**
 * Interpret commands to run database operations.
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class Interpreter {
	SamsDbManager dbman;
	BufferedReader br;
	PrintWriter pw;
	
	Shell current_grouping_shell;
	
	public Interpreter(SamsDbManager dbman, Reader r, Writer w) {
		this.dbman = dbman;
		if ( r instanceof BufferedReader )
			br = (BufferedReader) r;
		else
			br = new BufferedReader(r);
		if ( w instanceof PrintWriter )
			pw = (PrintWriter) w;
		else
			pw = new PrintWriter(w, true);
	}

	public void info() { 
		pw.println(dbman.getDatabase().getInfo()); 
	}
	
	public void gc() throws Exception {
		System.gc();
		pw.println("free memory = " +Runtime.getRuntime().freeMemory()+ "  " +
			  "total memory = " +Runtime.getRuntime().totalMemory()
		);
	}
	
	public void save() throws Exception {
		dbman.getDatabase().save();
		pw.println("saved");
	}
	
	public void metadata() {
		ISamsDb.IMetadataDef mddef = dbman.getDatabase().getMetadata();
		pw.println("  Name / Default value");
		for ( Iterator it = mddef.getDefinitions().iterator(); it.hasNext(); ) {
			 IAttributeDef attrDef = (IAttributeDef) it.next();
			 pw.println("  '" +attrDef.getName()+ "' / '" +attrDef.getDefaultValue()+ "'");
		}
	}
	
	public void addattr(String[] args) throws Exception {
		dbman.addAttributeDefinition(args[1], args[2]);
		save();
	}
	
	public void specs(String[] args) throws Exception {
		String condition_text = args.length >= 2 ? args[1] : null;
		String orderBy = args.length >= 3 ? args[3] : null;
		
		ISamsDb db = dbman.getDatabase();
		ISamsDb.IMetadataDef mddef = db.getMetadata();
		for ( Iterator it = mddef.getDefinitions().iterator(); it.hasNext(); ) {
			 IAttributeDef attrDef = (IAttributeDef) it.next();
			 String attrName = attrDef.getName();
			 pw.print("  " +attrName);
		}
		pw.println();
		ICondition condition = condition_text == null ? null : db.createCondition(condition_text);
		IOrder order = db.createOrder(orderBy);
		for ( Iterator it2 = db.selectSpectrums(condition, order); it2.hasNext(); ) {
			ISamsDb.ISpectrum s = (ISamsDb.ISpectrum) it2.next();
			for ( Iterator it = mddef.getDefinitions().iterator(); it.hasNext(); ) {
				 IAttributeDef attrDef = (IAttributeDef) it.next();
				 String attrName = attrDef.getName();
				 pw.print("  " +s.getString(attrName));
			}
			pw.println();
		}
	}

	public void opers() throws Exception {
		pw.println("Available operators:");
		for ( Iterator it = Sams.getOperationNames().iterator(); it.hasNext(); ) {
			 String opername = (String) it.next();
			 if ( opername == null )
				 pw.println("  " +"----------------------------"+ "");
			 else
				 pw.println("  '" +opername+ "'");
		}
	}

	public void setval(String[] args) throws Exception {
		String path = args[1];
		String attrName = args[2];
		String attrValue = args[3];
		ISamsDb.ISpectrum s = dbman.getDatabase().getSpectrum(path);
		if ( s == null ) {
			pw.println(path+ ":  Not found");
			return;
		}
		s.setString(attrName, attrValue);
		s.save();
	}
	
	public void getval(String[] args) throws Exception {
		String path = args[1];
		String attrName = args[2];
		ISamsDb db = dbman.getDatabase();
		ISamsDb.ISpectrum s = db.getSpectrum(path);
		if ( s == null ) {
			pw.println(path+ ":  Not found");
			return;
		}
		String attrVal = s.getString(attrName);
		if ( attrVal == null )
			attrVal = "undefined attribute";
		else
			attrVal = "'" +attrVal+ "'";
		pw.println("   " +attrName+ " = " +attrVal);
	}
	
	public void getspec(String[] args) throws Exception {
		String path = args[1];
		pw.println("Path: '" +path+ "'");
		ISamsDb db = dbman.getDatabase();
		ISamsDb.ISpectrum s = db.getSpectrum(path);
		if ( s == null ) {
			pw.println("  Not found");
			return;
		}
		ISamsDb.IMetadataDef mddef = db.getMetadata();
		for ( Iterator it = mddef.getDefinitions().iterator(); it.hasNext(); ) {
			 IAttributeDef attrDef = (IAttributeDef) it.next();
			 String attrName = attrDef.getName();
			 pw.println("  " +attrName+ ": '" +s.getString(attrName)+ "'");
		}
	}
	
	public void getsig(String[] args) throws Exception {
		String path = args[1];
		pw.println("Path: '" +path+ "'");
		ISamsDb db = dbman.getDatabase();
		Signature sig = db.getSignature(path);
		for ( int i = 0; i < Math.min(10, sig.getSize()); i++ ) {
			Signature.Datapoint p = sig.getDatapoint(i);
			pw.println("     " +p.x+ " , " +p.y);
		}
	}
	
	public void valat(String[] args) throws Exception {
		String path = args[1];
		double x = Double.parseDouble(args[2]);
		ISamsDb db = dbman.getDatabase();
		Signature sig = db.getSignature(path);
		double y = OpUtil.valueAt(sig, x);
		pw.println("     " +x+ " -> " +y);
	}
	
	public void addspec(String[] args) throws Exception {
		String path = args[1];
		String filename = args[2];

		ISpectrumFile sf = Sams.readSignatureFile(filename, null);
		sig.Signature sig = sf.getSignature();
		pw.println(filename+ ":");
		pw.println("  recognized as a '" +sf.getFormatName()+ "' file. " +
			"  Size: " +sig.getSize()+ " values"
		);

		dbman.getDatabase().addSpectrum(path, sig);
		save();
	}
	
	public void importfile(String[] args) throws Exception {
		String filename = args[1];
		dbman.importFile(filename, null, "/");
	}
	
	public void importdir(String[] args) throws Exception {
		String dirname = args[1];
		boolean recurse = args.length == 3 && args[2].equals("-r");
		dbman.importDirectory(dirname, recurse, null, "/");
	}
	
	public void export(String[] args) throws Exception {
		String format = args[1];
		String filename = args[2];
		List paths = new ArrayList();
		for ( int i = 3; i < args.length; i++ ) {
			String path = args[i];
			paths.add(path);
		}
		if ( format.equals("ascii") )
			dbman.exportAscii(paths, filename, null);
		else if ( format.equals("envi") )
			dbman.exportEnvi(paths, filename, null, EnviDataType.FLOAT32);
		else if ( format.equals("envilib") )
			dbman.exportEnviLibrary(paths, filename, null, EnviDataType.FLOAT32);
		else
			pw.println(format+ ": unrecognized export format. Use one of ascii, envi, envilib");
	}
	
	public void grouping(String[] args) throws Exception {
		String[] attrNames = new String[args.length - 1];
		System.arraycopy(args, 1, attrNames, 0, attrNames.length);
		INode dir = dbman.getDatabase().getGroupingBy(attrNames);
		if ( current_grouping_shell == null )
			current_grouping_shell = new Shell(dir, br, pw);
		else
			current_grouping_shell.setDirectory(dir);
	}
	
	public void grouping_command(String[] args) throws Exception {
		if ( current_grouping_shell == null ) {
			pw.println("No grouping set. Use ``grouping attrName''");
			return;
		}
		String[] argsshell = new String[args.length - 1];
		System.arraycopy(args, 1, argsshell, 0, argsshell.length); 
		current_grouping_shell.process(argsshell);
	}
	
	public void clipboard(String[] args) throws Exception {
		IClipboard clipboard = dbman.getDatabase().getClipboard();
		if ( args[1].equals("copy") ) {
			List paths = new ArrayList();
			for ( int i = 2; i < args.length; i++ )
				paths.add(args[i]);
			clipboard.copy(paths);
		}
		else if ( args[1].equals("paste") )
			clipboard.paste(args[2]);
		else
			pw.println("clipboard: unrecognized command");
	}
	
	protected boolean process(String[] args) throws Exception {
		if ( args[0].equals("info") )
			info();
		else if ( args[0].equals("save") )
			save();
		else if ( args[0].equals("metadata") )
			metadata();
		else if ( args[0].equals("addattr") )
			addattr(args);
		else if ( args[0].equals("addspec") )
			addspec(args);
		else if ( args[0].equals("getspec") )
			getspec(args);
		else if ( args[0].equals("getsig") )
			getsig(args);
		else if ( args[0].equals("specs") )
			specs(args);
		else if ( args[0].equals("opers") )
			opers();
		else if ( args[0].equals("getval") )
			getval(args);
		else if ( args[0].equals("setval") )
			setval(args);
		else if ( args[0].equals("valat") )
			valat(args);
		else if ( args[0].equals("importfile") )
			importfile(args);
		else if ( args[0].equals("importdir") )
			importdir(args);
		else if ( args[0].equals("export") )
			export(args);
		else if ( args[0].equals("grouping") )
			grouping(args);
		else if ( args[0].equals("grp") )
			grouping_command(args);
		else if ( args[0].equals("gc") )
			gc();
		else if ( args[0].equals("clipboard") )
			clipboard(args);
		else
			return false;
		
		return true;
	}
}
