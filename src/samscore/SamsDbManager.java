package samscore;

import specfile.ISpectrumFile;
import sig.Signature;
import sigoper.OpUtil;
import sigoper.DomainException;
import fileutils.Files;
import envifile.BinaryExporter;
import envifile.EnviDataType;

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

	ExportListener defaultExportListener = new ExportListener() {	
		public void exporting(int file_number, String relative_filename) {
			println("(" +file_number+ ") " +relative_filename);
		}
	};

	
	/** @param w Some operations write messages to this stream only if not null. */
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
	
	public String importFile(String filename, String filetype, String groupPath) throws Exception {
		ISpectrumFile sf = Sams.readSignatureFile(filename, filetype);
		sig.Signature sig = sf.getSignature();
		print(filename+ ":  recognized as a '" +sf.getFormatName()+ "' file. ");
		String path = groupPath+ "/" +new File(filename).getName();
		path = db.addSpectrum(path, sig);
		db.save();
		println(" => '" +path+ "'");
		return path;
	}
	
	public void importDirectory(final String dirname, boolean recurse, String tryfiletype, String groupPath)
	throws Exception {
		importDirectory(dirname, recurse, tryfiletype, groupPath, new ImportDirectoryListener() {
			public void importing(int file_number, String relative_filename, String filetype) {
				println("(" +file_number+ ") " +relative_filename+ ": '" +filetype+ "' file. ");
			}
		});
	}
	
	public interface ImportDirectoryListener {
		public void importing(int file_number, String relative_filename, String filetype);
	}

	public void importDirectory(String dirname, boolean recurse, String tryfiletype, 
		String groupPath, ImportDirectoryListener lis
	) throws Exception {
		new DirectoryImporter(dirname, recurse, tryfiletype, groupPath, lis).importFiles();
	}
	
	public DirectoryImporter createDirectoryImporter(String dirname, boolean recurse, String tryfiletype, 
		String groupPath, ImportDirectoryListener lis
	) throws Exception {
		return new DirectoryImporter(dirname, recurse, tryfiletype, groupPath, lis);
	}
	
	public class DirectoryImporter {
		final String dirname;
		final boolean recurse;
		final String tryfiletype;
		final String groupPath;
		final ImportDirectoryListener lis;
		int estimated_files;
		int file_number;
		
		public DirectoryImporter(
			String dirname, boolean recurse, String tryfiletype, 
			String groupPath,
			final ImportDirectoryListener lis
		) {
			this.dirname = dirname;
			this.recurse = recurse;
			this.tryfiletype = tryfiletype;
			this.groupPath = groupPath;
			this.lis = lis;
		}
		
		/** gets a estimated number of files to be scanned. */
		public int getEstimatedFiles() throws Exception {
			estimated_files = 0;
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
						estimated_files++;
					}
				}
			);
			return estimated_files;
		}
		
		public void importFiles() throws Exception {
			file_number = 0;
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
							ISpectrumFile sf = Sams.readSignatureFile(filename, tryfiletype);
							lis.importing(++file_number, relative_filename, sf.getFormatName());
							sig.Signature sig = sf.getSignature();
							String path = groupPath+ "/" +relative_filename;
							db.addSpectrum(path, sig);
						}
						catch(Exception ex) {
							lis.importing(++file_number, relative_filename, null);
						}
					}
				}
			);
			db.save();
		}
	}

	public interface ExportListener {
		public void exporting(int file_number, String relative_filename);
	}

	/** exports elements to an ASCII (CSV) file. */
	public void exportAscii(List paths, String filename, ExportListener lis) throws Exception {
		if ( paths.size() == 0 ) {
			println("No paths were given");
			return;
		}
		if ( lis == null )
			lis = defaultExportListener;
		Signature[] sigs = new Signature[paths.size()];
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileOutputStream(filename));
			// write header and notify listener:
			out.print("Wavelength");
			for ( int i = 0; i < sigs.length; i++ ) {
				String path = (String) paths.get(i);
				out.print(", " +path);
				sigs[i] = db.getSignature(path);
				lis.exporting(i+1, path);
			}
			out.println();
			int size = OpUtil.minSize(sigs);
			for ( int i = 0; i < size; i++ ) {
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
	}
	
	/** exports elements to an envi standard file. */
	public void exportEnvi(
		List paths, String filename, 
		ExportListener lis, EnviDataType type
	) throws Exception {
		
		if ( paths.size() == 0 ) {
			println("No paths were given");
			return;
		}
		if ( lis == null )
			lis = defaultExportListener;
		
		Signature[] sigs = new Signature[paths.size()];
		for ( int i = 0; i < sigs.length; i++ ) {
			String path = (String) paths.get(i);
			sigs[i] = db.getSignature(path);
			lis.exporting(i+1, path);
		}

		String header_description = 
			"  SAMS2 " +Sams.getVersion()+ " - Spectral Management and Analysis System\n" +
			"  " +sigs.length+ " signatures exported on " +(new java.util.Date())
		;
		BinaryExporter.exportBIP(sigs, filename, header_description, type);
	}

	/** exports elements to an envi spectral library. */
	public void exportEnviLibrary(
		List paths, String filename, 
		ExportListener lis, EnviDataType type
	) throws Exception {
		
		if ( paths.size() == 0 ) {
			println("No paths were given");
			return;
		}
		if ( lis == null )
			lis = defaultExportListener;
		final ExportListener elis = lis;
		
		String[] sig_names = new String[paths.size()];
		Signature[] sigs = new Signature[paths.size()];
		for ( int i = 0; i < sigs.length; i++ ) {
			String path = (String) paths.get(i);
			sig_names[i] = path;
			sigs[i] = db.getSignature(path);
			lis.exporting(i+1, path);
		}
		
		String header_description = 
			"  SAMS2 " +Sams.getVersion()+ " - Spectral Management and Analysis System\n" +
			"  " +sigs.length+ " signatures exported on " +(new java.util.Date())
		;
		BinaryExporter.exportToEnviSpectralLibrary(sig_names, sigs, filename, header_description, type); 
	}
}
