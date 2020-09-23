package samsgui.dbgui;

import samsgui.Controller.Dialogs;
import sig.Signature;
import fileutils.Files;

import ptolemy.plot.PlotPoint;
import ptolemy.plot.PlotBox;
import ptolemy.plot.PlotFormatter;

import java.awt.event.*;
import java.awt.print.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import javax.swing.*;
import java.util.*;
import java.util.List;
import java.awt.print.Printable;

/**
 * A panel with plotting capabilities.
 * Current implementation delegates main operations on a ptolemy.plot.Plot
 * object (which is nice but has some limitations).
 * @author Carlos A. Rueda
 * @version $Id$ 
 */
public class Plot extends JPanel {
	private SubPtolemyPlot ptplot;
	private DbGui guidb;
	private static RenderingHints hints = null;
	
	/** Selected spectrum elements. */
	private Hashtable selected;

	private int currentDataset;

	private MyButtonListener buttonListener;
	
	private JDialog legendsFrame;
	
	private boolean antiAliased;
	
	/**  Constructor */
	public Plot(DbGui guidb) {
		super(new GridLayout(1,1));
		add(ptplot = new SubPtolemyPlot());
		this.guidb = guidb;
		currentDataset = 0;
		selected = new Hashtable();
		ptplot.setXLabel("Wavelength");
		ptplot.setYLabel("Reflectance");
		//setButtons(true);
		buttonListener = new MyButtonListener();
		antiAliased = false;
	}

	private void updateLegends() {
		ptplot.updateLegends();
	}
	
	public void print() throws Exception {
		ptplot.print();
	}
	
	private class SubPtolemyPlot extends ptolemy.plot.Plot { 
		LegendPanel legendPanel;
		
		SubPtolemyPlot() {
			super();
			addMouseMotionListener(new MyMouseMotionListener());
		}
		
		public void paintComponent(Graphics graphics) {
			if ( antiAliased && graphics instanceof Graphics2D )
				((Graphics2D) graphics).addRenderingHints(hints);
			super.paintComponent(graphics);
		}
	
		public void repaint() {
			updateLegends();
			super.repaint();
		}

		synchronized void zoomXRange(double minx, double maxx) {
			double miny = Double.POSITIVE_INFINITY;
			double maxy = Double.NEGATIVE_INFINITY;
			for (int dataset = 0; dataset < _points.size(); dataset++) {
				Vector points = (Vector)_points.elementAt(dataset);
				for (int index = 0; index < points.size(); index++) {
					PlotPoint pt = (PlotPoint)points.elementAt(index);
					if ( minx <= pt.x && pt.x <= maxx ) {
						if (miny > pt.y)
							miny = pt.y;
						if (maxy < pt.y)
							maxy = pt.y;
					}
				}
			}
			setXRange(minx, maxx);
			setYRange(miny, maxy);
			ptplot.repaint();
		}
		
		synchronized void zoomYRange(double miny, double maxy) {
			double minx = Double.POSITIVE_INFINITY;
			double maxx = Double.NEGATIVE_INFINITY;
			for (int dataset = 0; dataset < _points.size(); dataset++) {
				Vector points = (Vector)_points.elementAt(dataset);
				for (int index = 0; index < points.size(); index++) {
					PlotPoint pt = (PlotPoint)points.elementAt(index);
					if ( minx > pt.x )
						minx = pt.x;
					if ( maxx < pt.x )
						maxx = pt.x;
				}
			}
			setXRange(minx, maxx);
			setYRange(miny, maxy);
			ptplot.repaint();
		}
		
		private void reset(boolean format) {
			currentDataset = 0;
			selected.clear();
			clear(format);
			setXLabel("Wavelength");
			setYLabel("Reflectance");
			clearLegends();
			updateLegends();
		}
	
		/** The print method copied from Ptolemy. (class PlotBox.ButtonListener) */
		public void printPtolemyVersion() throws Exception {
			PrinterJob job = PrinterJob.getPrinterJob();
			job.setPrintable(this);
			if (job.printDialog()) 
				job.print();
		}
			
		/**
		 * Alternative print option.
		 * This is trying to overcome a problem with Java printing that
		 * doesn't take the orientation specified by the user by means
		 * of job.printDialog().  
		 * See PlotBox.ButtonListener, DbGui.createPlotPanel.
		 */
		void print() throws Exception {
			PrinterJob job = PrinterJob.getPrinterJob();
			PageFormat defaultpage = job.defaultPage();
			PageFormat format = job.pageDialog(defaultpage);
			if ( format != defaultpage ) {
				job.setPrintable(this, format);
				job.print();
			}
		}
		
		public synchronized int print(Graphics graphics, PageFormat format, int index) 
		throws PrinterException  {
			if (graphics == null)
				return Printable.NO_SUCH_PAGE;
				
			if (index >= 1) {      // We only print on one page.
				return Printable.NO_SUCH_PAGE;
			}
			
			Graphics2D g2d = (Graphics2D) graphics;
			if ( antiAliased )
				g2d.addRenderingHints(hints);
			
			g2d.translate((int)format.getImageableX(), (int)format.getImageableY());
			Rectangle rect = new Rectangle(0,0, 800, 600);
			double xscale = format.getImageableWidth() / rect.width;
			//double yscale = format.getImageableHeight() / rect.height;
			double yscale = xscale;
			g2d.scale(xscale, yscale);
			_drawPlot(graphics, true, rect);
			return Printable.PAGE_EXISTS;
		}
	
		void showLegendsWindow() {
			if ( legendsFrame == null ) {
				legendsFrame = new JDialog(guidb.parentFrame, false);
				legendsFrame.setTitle("Legends");
				legendPanel = new LegendPanel();
				JScrollPane scroller = new JScrollPane(legendPanel);
				scroller.setPreferredSize(new Dimension(220,200));
				legendsFrame.getContentPane().add(scroller);
				legendsFrame.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent e) {
						legendsFrame.setVisible(false);
					}
				});
				legendsFrame.pack();
				legendsFrame.setLocation(200, 100);
			}
			else if ( legendsFrame.isShowing() ) {
				legendsFrame.setVisible(false);
				return;
			}
			legendsFrame.setVisible(true);
			legendPanel.updateLegends();
		}
	
		void updateLegends() {
			if ( legendPanel != null )
				legendPanel.updateLegends();
		}
		
		/** A panel to show the current plot legends. */
		class LegendPanel extends JPanel {
			/** Updates the panel contents an calls this.revalidate() and this.repaint(). */
			void updateLegends() {
				// subjective values:
				//	200: enough width for legend texts
				//	 20: enough height for each legend text
				
				int nds = getNumDataSets();
				Dimension size = new Dimension(200, 10 + 20*nds + 10);
				setPreferredSize(size);
				this.revalidate();
				this.repaint();
			}
			
			public void paint(Graphics graphics) {
				if ( antiAliased && graphics instanceof Graphics2D )
					((Graphics2D) graphics).addRenderingHints(hints);
				super.paint(graphics);
				Color curr_color = graphics.getColor();
				int nds = getNumDataSets();
				for ( int dataset = 0; dataset < nds; dataset++ ) {
					if (_usecolor) {
						int color = dataset % _colors.length;
						graphics.setColor(_colors[color]);
					}
					int xpos = 10;
					int ypos = 10 + dataset * 20;
					boolean clip = false;
					_drawPoint(graphics, dataset, xpos, ypos, clip);
					graphics.setColor(curr_color);
					graphics.drawString(getLegend(dataset), xpos + 10, ypos + 3);
				}
			}
		}
		
		/** Now, to show the current location in term of actual units. */
		class MyMouseMotionListener implements MouseMotionListener  {
			private void move(MouseEvent ev) {
				int x = ev.getX();
				int y = ev.getY();
				if ( x < _ulx || x > _lrx  ||   y < _uly || y > _lry )
					return;
				
				// translate:
				x -= _ulx;
				y -= _uly;
				double xx = (double) x / _xscale + _xMin;
				double yy = (double) _yMax - y / _yscale;
				
				xx = (double) Math.round(xx * 1000) / 1000;
				yy = (double) Math.round(yy * 1000) / 1000;
				
				guidb.updateLocation(xx, yy);
			}
			
			public void mouseDragged(MouseEvent ev) {
				move(ev);
			}
			
			public void mouseMoved(MouseEvent ev) {
				move(ev);
			}
		}
		
	}
	
	public void showLegendsWindow() {
		ptplot.showLegendsWindow();
	}
	
	/** Clears and resets all parameters to their original values. */
	public void reset() {
		ptplot.reset(true);
		updateLegends();
	}
	
	public void repaint() {
		if ( ptplot != null )
			ptplot.repaint();
	}
	
	/** Clears the plot. */
	public void clearSignatures() {
		ptplot.reset(false);
	}
	
	/** Draws a signature making it the only one in the plot. */
	public void setSignature(Signature sig, String legend) {
		ptplot.reset(false);
		addSignature(sig, legend);
		updateLegends();
	}

	/** Tells if a signature is currently plotted. */
	public boolean isPlotted(String legend) {
		return selected.get(legend) != null;
	}
		
	/** Adds a signature to the plot. */
	public void addSignature(Signature sig, String legend) {
		if ( selected.get(legend) != null )
			return;
		if ( sig == null )
			return;

		ptplot.addLegend(currentDataset, legend);
		selected.put(legend, new Integer(currentDataset));
		int size = sig.getSize();
		Signature.Datapoint point;
		for ( int i = 0; i < size; i++ ) {
			point = sig.getDatapoint(i);
			ptplot.addPoint(currentDataset, point.x, point.y, true);
		}
		currentDataset++;
		
		updateLegends();
	}

	/**
	 * Adds (resp. removes) a signature if the signature is
	 * not yet (resp. already) plotted.
	 */
	public void toggleSignature(Signature sig, String legend) {
		if ( selected.get(legend) != null )
			removeSignature(legend);
		else
			addSignature(sig, legend);
	}

	/** Removes a signature from the plot. */
	public void removeSignature(String legend) {
		Integer i = (Integer) selected.get(legend);
		if ( i == null )
			return;

		ptplot.clear(i.intValue());

		// PENDING: Synchronize legends
		// this method doesn't exist!  ;-(
		//	clearLegend(i.intValue());

		selected.remove(legend);
		updateLegends();
	}

	/** Zooms the current X range. This calls repaint(); */
	public void zoomXRange() {
		double[] r = ptplot.getXRange();
		zoomXRange(r[0], r[1]);
	}

	/** Zooms the specified X range. This calls repaint(); */
	public void zoomXRange(double minx, double maxx) {
		ptplot.zoomXRange(minx, maxx);
	}
	
	/** Zooms the current Y range. This calls repaint(); */
	public void zoomYRange() {
		double[] r = ptplot.getYRange();
		zoomYRange(r[0], r[1]);
	}

	/** Zooms the specified Y range. This calls repaint(); */
	public void zoomYRange(double miny, double maxy) {
		ptplot.zoomYRange(miny, maxy);
	}
	
	public JButton[] getButtons() {
		return new JButton[] {
			getButton("print", "Prints the plot"),
			getButton("reset", "Resets X and Y ranges to their original values"),
			getButton("format", "sets the plot format"),
			getButton("fill", "Rescales the plot to fit the data"),
		};
	}
	private JButton getButton(String name, String tooltip) {
		java.net.URL img = getClass().getResource("/ptolemy/plot/img/" +name+ ".gif");
		JButton button;
		if ( img != null ){
			button = new JButton(new ImageIcon(img));
			button.setBorderPainted(false);
			button.setPreferredSize(new Dimension(20,20));
		}
		else {
			button = new JButton(name);
		}
		button.setActionCommand(name);
		button.setToolTipText(tooltip);
		button.addActionListener(buttonListener);
		return button;
	}

	public void showPlotFormatter() {
		PlotFormatter fmt = new PlotFormatter(ptplot);
		fmt.openModal();
	}
	
	public void exportToEPS() {
		String filename = Dialogs.selectExportFile("Please give a file name to export the plot");
		if ( filename == null )
			return;
		
		try {
			java.io.BufferedOutputStream out =
				new java.io.BufferedOutputStream(
					new java.io.FileOutputStream(filename)
			);
			ptplot.export(out);
			out.close();
		}
		catch ( Exception ex ) {
			System.err.println(ex.getMessage());
		}
	}

	/** Returns true if the plot will be anti aliased. */
	public boolean isAntiAliased(){
		return antiAliased;
	}
	
	/** Sets antialiasing on or off based on the boolean value. */
	public void setAntiAliased(boolean newValue) {
		this.antiAliased = newValue;
		if ( antiAliased && hints == null ) {
			hints = new RenderingHints(null);
			hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}
	}	

	public void fillPlot() {
		ptplot.fillPlot();
	}
		
    private class MyButtonListener implements ActionListener  {
        public void actionPerformed(ActionEvent event) {
			String cmd = event.getActionCommand();
            if ( cmd.equals("fill") ) 
                ptplot.fillPlot();
			else if ( cmd.equals("print") ) {
				//guidb.printPlot();
            } 
			else if ( cmd.equals("reset") )
                ptplot.resetAxes();
			else if ( cmd.equals("format") )
				showPlotFormatter();
        }
    }
	
}
