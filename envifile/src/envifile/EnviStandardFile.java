package envifile;

import java.io.*;
import java.util.*;

/**
 * Represents a file in ENVI standard format.
 *
 * @author Carlos Rueda
 * @version $Id$
 */
public class EnviStandardFile {
	/** Header for a envi file. */
	public static class Header {
		/** The map used to create this header. Can be null. */
		public Map map;
		
		public String description;
		public int samples;
		public int lines;
		public int bands;
		public String file_type;
		public int data_type;
		public String interleave;
		public int byte_order;
		public int x_start;
		public int y_start;
		public float[] wavelengths;
		public String[] spectra_names;
		
		/** Creates a header with default nul-values */
		public Header(){}
		
		/** Creates a header by taking values from a map.*/
		public Header(Map map) throws InvalidEnviFormatException{
			this.map = map;
			try {
				if ( map.get("description") != null )
					description = (String) map.get("description");
				samples = Integer.parseInt((String) map.get("samples"));
				lines = Integer.parseInt((String) map.get("lines"));
				bands = Integer.parseInt((String) map.get("bands"));
				data_type = Integer.parseInt((String) map.get("data type"));
				interleave = (String) map.get("interleave");
				file_type = (String) map.get("file_type");
				spectra_names = (String[]) map.get("spectra_names");
				byte_order = Integer.parseInt((String) map.get("byte order"));
				if ( map.get("x start") != null )
					x_start = Integer.parseInt((String) map.get("x start"));
				if ( map.get("y start") != null )
					y_start = Integer.parseInt((String) map.get("y start"));
				
				wavelengths = new float[bands];
				
				String wls = (String) map.get("wavelength");  // NOTE: "wavelength"
				if ( wls != null ) {
					StringTokenizer st = new StringTokenizer(wls, ", {}\r\n\t");
					for ( int i = 0; i < bands && st.hasMoreTokens(); i++ ) {
						String token = st.nextToken();
						wavelengths[i] = Float.parseFloat(token);
						//System.out.print(wavelengths[i]+ " | ");
					}
				}
			}
			catch ( RuntimeException ex ) {
				throw new InvalidEnviFormatException("Error reading header: " +ex.getMessage());
			}
		}
	} // class Header
		

	/** Writes a Standard Envi header file.
	 *
	 * @param header   The header to write.
	 * @param filename The name of the file to write to.
	 */
	public static void writeHeader(Header header, String filename) throws IOException {
		header.byte_order = 1; // in Java
		
		PrintWriter pw = new PrintWriter(new FileOutputStream(filename)); 
		pw.print(
			"ENVI\n" +
			"description = {\n" +
			header.description+ "}\n" +
			"samples = " +header.samples+ "\n" +
			"lines   = " +header.lines+ "\n" +
			"bands   = " +header.bands+ "\n" +
			"header offset = 0\n" +
			"file type = " +(header.file_type == null ? "ENVI Standard" : header.file_type)+ "\n" +
			"data type = " +header.data_type+ "\n" +
			"interleave = " +header.interleave+ "\n" +
			"sensor type = Unknown\n" +
			"byte order = " +header.byte_order+ "\n"
		);
		
		if ( header.spectra_names != null ) {
			pw.print("spectra names = {");
			for ( int i = 0; i < header.spectra_names.length; i++ ) {
				if ( i > 0 )
					pw.print(", ");
		
				if ( i % 5 == 0 )
					pw.print("\n ");
		
				pw.print(header.spectra_names[i]);
			}
			pw.println("}");
		}
		
		pw.print(
			"x start = " +header.x_start+ "\n" +
			"y start = " +header.y_start+ "\n" +
			"default stretch = 2.0 gaussian\n"
		);
		
		if ( header.wavelengths != null ) {
			pw.print("wavelength = {");
			for ( int i = 0; i < header.wavelengths.length; i++ ) {
				if ( i > 0 )
					pw.print(", ");
		
				if ( i % 10 == 0 )
					pw.print("\n ");
		
				pw.print(header.wavelengths[i]);
			}
			pw.println("}");
		}
	
		pw.close();
	}

	/** Reads a Standard Envi header file.
	 *
	 * @param filename The name of the file to read from.
	 *
	 * @return  The header contained in the file.
	 */
	public static Header readHeader(String filename)
	throws IOException, InvalidEnviFormatException {
		String header_filename = filename + ".hdr";
		if ( ! new File(header_filename).exists() )
			header_filename = filename + ".HDR";
		
		BufferedReader br = new BufferedReader(
			new InputStreamReader(
				new FileInputStream(header_filename)
			)
		);
		String line = br.readLine();
		if ( ! line.trim().equalsIgnoreCase("ENVI") ) {
			throw new InvalidEnviFormatException(
				header_filename+ " doesn't start with ENVI\n" +
				"First line=[" +line+ "]"
			);
		}
		
		Map map = new HashMap();
		int c;
		StringBuffer sb = new StringBuffer();
		String key = null;
		String val;
		boolean in_value = false;
		int nesting = 0;   // for '{' in a value
		
		while ( (c = br.read()) != -1 ) {
			switch ( c ) {
				case '=':
					if ( in_value ) {
						// just take it
						sb.append((char)c);
					}
					else {
						// take key:
						key = sb.toString().trim();
						sb.setLength(0);
						
						// prepare to read value
						in_value = true;
						nesting = 0;
					}
					break;
					
				case '{':
					sb.append((char)c);
					if ( in_value )
						nesting++;
					break;
					
				case '}':
					sb.append((char)c);
					if ( in_value ) {
						if ( --nesting <= 0 ) {
							in_value = false;
							// a value has finished:
							val = sb.toString().trim();
							sb.setLength(0);
							map.put(key.toLowerCase(), val);
						}
					}
					break;
					
				case '\n':
					if ( in_value ) {
						if ( nesting > 0 ) {
							sb.append((char)c);
						}
						else {
							in_value = false;
							// a value has finished:
							val = sb.toString().trim();
							sb.setLength(0);
							map.put(key.toLowerCase(), val);
						}
					}
					break;
					
				default:
					sb.append((char)c);
					break;
			}
		}
		br.close();

		//System.out.println(map);
		Header header = new Header(map);

		return header;
	}



	// Non-instanceable.
	private EnviStandardFile() {}

	public static void main(String[] args) throws Exception {
		readHeader(args[0]);
	}
	
}
