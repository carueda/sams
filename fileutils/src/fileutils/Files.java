package fileutils;

import java.io.*;
import java.util.*;
import javax.swing.JFrame;
import javax.swing.JFileChooser;

/** Some File utilities.
 *
 * @author Carlos Rueda
 * @version $Id$
 */
public class Files {
	public interface IFileVisitor {
		public void visit(String filename);
	}
	
	/**
	 * Traverses a directory.
	 *
	 * @param file             Directory to search.
	 * @param absolute         true to visit with absolute string paths.
	 * @param parent           true to include parent name in paths.
	 *                         Meaningless if absolute == true.
	 * @param inc_dirs         true to include directory elements. 
	 * @param inc_files        true to include normal file elements. 
	 * @param level            The level of recursion. 1 to just list the
	 *                         given directory.
	 * @param visitor          The visitor.
	 */
	public static void traverse(
		File file,
		boolean absolute,
		boolean parent,
		boolean inc_dirs,
		boolean inc_files,
		int level,
		IFileVisitor visitor
	) {
		if ( level > 0 && file.isDirectory() ) {
			String prefix = "";
			if ( absolute ) {
				try {
					// canonical form, preferably
					prefix = file.getCanonicalPath();
				}
				catch(java.io.IOException ex) {
					// oh, absolute will be ok:
					prefix = file.getAbsolutePath();
				}
				
				if ( ! prefix.endsWith(File.separator) )
					prefix += File.separator;
			}
			else if ( parent ) {
				prefix = file.getName();
				if ( ! prefix.endsWith(File.separator) )
					prefix += File.separator;
			}
			
			File[] dir = file.listFiles();
			if ( dir != null ) {
				for ( int i = 0; i < dir.length; i++ )
					_listFiles(dir[i], inc_dirs, inc_files, level, visitor, prefix);
			}
		}
	}


	/**
	 * Auxiliary function to listFiles().
	 *
	 * @param file             File to examine.
	 * @param inc_dirs         true to include directory elements. 
	 * @param inc_files        true to include normal file elements. 
	 * @param level            The level of recursion.
	 *                         If level &lt;= 0, it does nothing.
	 */
	private static void _listFiles(
		File file,
		boolean inc_dirs,
		boolean inc_files,
		int level,
		IFileVisitor visitor,
		String prefix
	)
	{
		if ( level <= 0 )
			return;
		
		if ( inc_dirs && file.isDirectory()
		||   inc_files && file.isFile() )   {
			String name = prefix + file.getName();
			visitor.visit(name);
		}
		
		if ( level > 1 && file.isDirectory() ) {
			prefix += file.getName() + File.separator; 
			File[] dir = file.listFiles();
			if ( dir != null ) {
				for ( int i = 0; i < dir.length; i++ )
					_listFiles(dir[i], inc_dirs, inc_files, level - 1, visitor, prefix);
			}
		}
	}

	/** USE CAREFULLY Deletes recursively an entire directory. */
	public static void deleteDirectory(String directory) {
		File file = new File(directory);
		if ( file.isDirectory() ) {
			// recurse
			String[] list = file.list();
			if ( list != null ) {
				for (int i = 0; i < list.length; i++)
					deleteDirectory(directory+ File.separator +list[i]);
			}
		}

		// and remove this entry:
		file.delete();
	}

	/**
	 * Copies a file.
	 *
	 * @param file        Source file.
	 * @param dest_file   Destination file.
	 */
	public static void copyFile(File file, File dest_file)
	throws FileNotFoundException, IOException
	{
		DataInputStream in = new DataInputStream(new BufferedInputStream(
			new FileInputStream(file))
		);
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
			new FileOutputStream(dest_file))
		);
		byte[] buffer = new byte[1024];
		int read;
		while ( (read = in.read(buffer)) > 0 )
			out.write(buffer, 0, read);
		in.close();
		out.close();
	}
	
	public static class FileFilter extends javax.swing.filechooser.FileFilter {
		String extension;
		String description;

		FileFilter(String extension, String description) {
			this.extension = extension;
			this.description = description;
		}

		public boolean accept(File f) {
			return f != null && ( f.isDirectory() || f.getName().endsWith("." + extension) ) ;
		}

		public String getDescription() {
			return description;
		}
	}

	// a test program
	public static void main(String[] args) throws Exception {
		System.out.println("listFiles test");
		int arg = 0;
		String directory = args[arg++];
		boolean names = args[arg++].equals("names");
		boolean absolute = args[arg++].equals("absolute");
		boolean parent = args[arg++].equals("parent");
		boolean inc_dirs = args[arg++].equals("inc_dirs");
		boolean inc_files = args[arg++].equals("inc_files");
		int level = Integer.parseInt(args[arg++]);
		
		System.out.println("directory  = " +directory);
		System.out.println("names      = " +names);
		System.out.println("absolute   = " +absolute);
		System.out.println("parent     = " +parent);
		System.out.println("inc_dirs   = " +inc_dirs);
		System.out.println("inc_files  = " +inc_files);
		System.out.println("level      = " +level);
		
		traverse(new File(directory), absolute, parent, inc_dirs, inc_files, level, 
			new IFileVisitor() {
				public void visit(String filename) {
					System.out.println(filename);
				}
			}
		);
	}
}