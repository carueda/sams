package envifile;

import sig.Signature;

import java.io.*;
import java.util.*;

/**
 * Exporter to binary format.
 *
 * @author Carlos Rueda
 * @version $Id$
 */
public final class BinaryExporter {
	/**
	 * Exports a list of signatures to Standard Envi format.
	 *
	 * @param sigs  Signatures to be exported.
	 * @param filename Destination file name. Also a header file, with
	 *                 name filename + ".hdr", will be written.
	 * @param header_description
	 *                 To be included in the header file.
	 * @param type
	 *             The data type
	 */
	public static void exportBIP(
		Signature[] sigs,
		String filename,
		String header_description,
		EnviDataType type
	) throws IOException, Exception {
		String header_filename = filename + ".hdr";
		
		int size = sigs.length;
		double sqrt_size = Math.sqrt(size);
		int lines = (int) sqrt_size;
		int samples = (int) Math.ceil((float) size / lines);
		
		int total_size = lines * samples; // could be > size
		
		DataOutputStream dos = null;
		try {
			Signature sig = sigs[0];
			int bands = sig.getSize();
			
			EnviStandardFile.Header header = new EnviStandardFile.Header();
			header.description = header_description;
			header.samples = samples;
			header.lines = lines;
			header.bands = bands;
			header.data_type = type.code();
			header.interleave = "bip";
			header.x_start = 0;
			header.y_start = 0;
			
			header.wavelengths = new float[bands];
			for ( int i = 0; i < bands; i++ ) {
				Signature.Datapoint p = sig.getDatapoint(i);
				header.wavelengths[i] = (float) p.x;
			}
			
			EnviStandardFile.writeHeader(header, header_filename);
			
			// now, write the binary file:
			File file = new File(filename);
			dos = new DataOutputStream(
				new BufferedOutputStream(
					new FileOutputStream(file), 100*1024
				)
			);
			for ( int i = 0; i < size; i++ ) {
				sig = sigs[i];
				int nbands = sig.getSize();
				if ( nbands != bands )
					throw new Exception("Different number of bands!");

				for ( int k = 0; k < bands; k++ ) {
					Signature.Datapoint p = sig.getDatapoint(k);
					type.write(p.y, dos);
				}
			}
			
			// pad with zeros to complete image:			
			for ( int i = size; i < total_size; i++ ) {
				for ( int k = 0; k < bands; k++ ) {
					type.write(0, dos);
				}
			}
		}
		finally {
			if ( dos != null )
				try { dos.close(); } catch (Exception ex ) {}
		}
	}
	
	/**
	 * Exports a list of spectra elements to a Envi Spectral Library.
	 * From the Envi Online Help:
	 * "ENVI Spectral Library files are stored in ENVI binary image format with the
	 * number of samples equal to the number of bands and the number of lines equal
	 * to the number of spectra in the library. The file type is set to spectral
	 * library in the header and there are associated wavelengths."
	 *
	 * @param sigs  Signatures to be exported.
	 * @param filename Destination file name. Also a header file, with
	 *                 name filename + ".hdr", will be written.
	 * @param header_description
	 *                 To be included in the header file.
	 * @param type
	 *             The data type
	 */
	public static void exportToEnviSpectralLibrary(
		String[] sig_names,
		Signature[] sigs,
		String filename,
		String header_description,
		EnviDataType type
	) throws IOException, Exception {
		String header_filename = filename + ".hdr";
		
		int lines = sigs.length;
		DataOutputStream dos = null;
		
		try {
			Signature sig = sigs[0];
			int samples = sig.getSize();
			int bands = 1; 
			
			EnviStandardFile.Header header = new EnviStandardFile.Header();
			header.description = header_description;
			header.samples = samples;
			header.lines = lines;
			header.bands = bands;
			header.data_type = type.code();
			header.file_type = "ENVI Spectral Library";
			header.interleave = "bip";
			
			header.spectra_names = sig_names;
			
			header.x_start = 0;
			header.y_start = 0;
			
			header.wavelengths = new float[samples];
			for ( int i = 0; i < samples; i++ ) {
				Signature.Datapoint p = sig.getDatapoint(i);
				header.wavelengths[i] = (float) p.x;
			}
			
			EnviStandardFile.writeHeader(header, header_filename);
			
			// now, write the binary file:
			File file = new File(filename).getAbsoluteFile();
			dos = new DataOutputStream(
				new BufferedOutputStream(
					new FileOutputStream(file), 100*1024
				)
			);

			for ( int i = 0; i < lines; i++ ) {
				sig = sigs[i];
				int nbands = sig.getSize();
				if ( nbands != samples )
					throw new Exception("Different number of bands!");

				for ( int k = 0; k < samples; k++ ) {
					Signature.Datapoint p = sig.getDatapoint(k);
					type.write(p.y, dos);
				}
			}
		}
		finally {
			if ( dos != null )
				try { dos.close(); } catch (Exception ex ) {}
		}
	}
	
	// Non-instanceable.
	private BinaryExporter() {}
}