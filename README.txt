SAMS Readme
Carlos A. Rueda
$Id$

Installer
	Izpack, http://izforge.com/izpack/, is used to create the SAMS installer.
	Izpack is assumed to be installed in /home/carueda/prg/IzPack/
	Change the "izdir" property in samsgui/build.xml to reflect the
	proper location in your machine.
	Currently I'm using Izpack version 3.5.4 (build 2004.06.05).
	The Izpack tool is the only external required tool (well, besides the
	Apache Ant build tool) that you need to provide.
	
Third-part libraries
	Directory lib/ contains the third-part libraries used
	by the SAMS application. 
	For convenience these libraries are already included in the CVS 
	repository. Currently:
		bsh-1.3b2.jar       -- BeanShell, http://beanshell.org
		plotapplication.jar -- http://ptolemy.eecs.berkeley.edu/java/ptplot/
		kunststoff.jar      -- Look&Feel, http://www.incors.org/  
		
Compilation

All build.xml files define and use a ${generated} property denoting
the base directory for generated files. Currently ${generated} is
defined as "../_GENERATED/sams" relative to this README. 

The master build.xml in this directory can be used to create the
complete SAMS system:
	ant
	
To create the SAMS installer for end users:
	cd samsgui
	ant installer
The created installer gets the name install-sams-${VERSION}.jar
where version is defined in samsgui/build.xml.

To test launching the SAMS installer (under samsgui/):
	ant install


