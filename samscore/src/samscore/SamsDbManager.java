package samscore;

import specfile.ISpectrumFile;
import sig.Signature;
import sigoper.OpUtil;
import sigoper.DomainException;
import fileutils.Files;
import envifile.BinaryExporter;

import java.util.*;
import java.io.*;

/**
 * Performs non-simple operations on a database.
 * @author Carlos A. Rueda
 * @version $Id$
 */
public class SamsDbManager {
	ISamsDb db;
	PrintWriter printWriter;
	
	/**
		@param w Some operations write messages to this stream only if not null.
	 */
	public SamsDbManager(ISamsDb db, Writer w) {
		setDatabase(db);
		if ( w != null ) {
			if ( w instanceof PrintWriter )
				printWriter = (PrintWriter) w;
			else
				printWriter = new PrintWriter(w, true);
		}
	}
	
	private void print(String msg) {
		if ( printWriter != null )
			printWriter.print(msg);
	}
	private void println(String msg) {
		if ( printWriter != null )
			printWriter.println(msg);
	}

	public void setDatabase(ISamsDb db) {
		this.db = db;
	}
	
	public ISamsDb getDatabase() {
		return db;
	}
	
	public void addAttributeDefinition(String attrName, String defaultValue) throws Exception {
		ISamsDb.IMetadataDef mddef = db.getMetadata();
		mddef.add(attrName, defaultValue);
	}
	
	public void importFile(String filename) throws Exception {
		ISpectrumFile sf = Sams.readSignatureFile(filename);
		sig.Signature sig = sf.getSignature();
		print(filename+ ":  recognized as a '" +sf.getFormatName()+ "' file. ");
		String path = new File(filename).getName();
		ISamsDb.ISpectrum s = db.addSpectrum(path, sig);
		db.save();
		println(" => '" +s.getPath()+ "'");
	}
	
	public void importDirectory(final String dirname, boolean recurse) throws Exception {
		File dirfile = new File(dirname);
		Files.traverse(
			dirfile,
			false,	//boolean absolute,
			false,	//boolean parent,
			false,	//boolean inc_dirs,
			true,	//boolean inc_files,
			recurse ? Integer.MAX_VALUE : 1,	//int level,
			new Files.IFileVisitor() {
				public void visit(String relative_filename) {
					String filename = dirname+ "/" +relative_filename;
					try {
						ISpectrumFile sf = Sams.readSignatureFile(filename);
						sig.Signature sig = sf.getSignature();
						print(filename+ ": recognized as a '" +sf.getFormatName()+ "' file. ");
						String path = relative_filename;
						ISamsDb.ISpectrum s = db.addSpectrum(path, sig);
						println(" => '" +s.getPath()+ "'");
					}
					catch(Exception ex) {
						println("Exception: " +ex.getMessage());
					}
				}
			}
		);
		db.save();
	}

	/** exports elements to an ASCII (CSV) file. */
	public void exportAscii(List paths, String filename) throws Exception {
		if ( paths.size() == 0 ) {
			println("No paths were given");
			return;
		}
		Signature[] sigs = new Signature[paths.size()];
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileOutputStream(filename));
			int i = 0;
			for ( Iterator it = paths.iterator(); it.hasNext(); ){
				String path = (String) it.next();
				if ( i > 0 )
					out.print(", ");
				out.print(path);
				sigs[i++] = db.getSignature(path);
			}
			out.println();
			int size = OpUtil.minSize(sigs);
			for ( i = 0; i < size; i++ ) {
				Signature.Datapoint dp = sigs[0].getDatapoint(i);
				out.print(dp.x+ ", " +dp.y);
				double x = dp.x;			
				for ( int k = 1; k < sigs.length; k++ ) {
					dp = sigs[k].getDatapoint(i);
					if ( !OpUtil.equalAbscissas(dp.x, x) ) {
						throw new DomainException(
							"Signatures are not defined on the same abscissas!"
						);
					}
					out.print(", " +dp.y);
				}
				out.println();
			}
		}
		finally {
			if ( out != null )
				try { out.close(); } catch (Exception ex ) {}
		}

		println("Exported " +sigs.length+ " element(s)");
	}
	
	/** exports elements to an envi standar file. */
	public void exportEnvi(List paths, String filename) throws Exception {
		if ( paths.size() == 0 ) {
			println("No paths were given");
			return;
		}
		Signature[] sigs = new Signature[paths.size()];
		int i = 0;
		for ( Iterator it = paths.iterator(); it.hasNext(); ) {
			String path = (String) it.next();
			sigs[i++] = db.getSignature(path);
		}

		String header_description = 
			"  SAMS2 " +Sams.getVersion()+ " - Spectral Management and Analysis System\n" +
			"  " +sigs.length+ " signatures exported on " +(new java.util.Date())
		;
		BinaryExporter.exportBIP(sigs, filename, header_description, printWriter);
		println("Exported " +sigs.length+ " element(s)");
	}

	/** exports elements to an envi spectral library. */
	public void exportEnviLibrary(List paths, String filename) throws Exception {
		if ( paths.size() == 0 ) {
			println("No paths were given");
			return;
		}
		String[] sig_names = new String[paths.size()];
		Signature[] sigs = new Signature[paths.size()];
		int i = 0;
		for ( Iterator it = paths.iterator(); it.hasNext(); ) {
			String path = (String) it.next();
			sig_names[i] = path;
			sigs[i++] = db.getSignature(path);
		}

		String header_description = 
			"  SAMS2 " +Sams.getVersion()+ " - Spectral Management and Analysis System\n" +
			"  " +sigs.length+ " signatures exported on " +(new java.util.Date())
		;
		BinaryExporter.exportToEnviSpectralLibrary(sig_names, sigs, filename, header_description, printWriter);
		println("Exported " +sigs.length+ " element(s)");
	}
}
