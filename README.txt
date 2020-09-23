SAMS - Spectral Analysis and Management System Readme
Carlos A. Rueda

**NOTE**: OUT OF DATE  (as of release 3.2.1)

Installer

	Izpack, http://izforge.com/izpack/, is used to create
	the SAMS installer. Change the "izdir" property in
	samsgui/build.properties to reflect the proper location
	of Izpack in your machine. Currently I'm using Izpack
	version 3.5.4 (build 2004.06.05). The Izpack tool is the
	only external required tool that needs to be provided.

Third-part libraries

	Directory lib/ contains the third-part libraries used by
	the SAMS application. For convenience these libraries
	are already included in the CVS repository. Currently:
	bsh-1.3b2.jar:
	       BeanShell, http://beanshell.org
	plotapplication.jar:
	       http://ptolemy.eecs.berkeley.edu/java/ptplot/
	kunststoff.jar:
	       Look&Feel, http://www.incors.org/

Compilation

All build.xml files define and use a ${generated} property
denoting the base directory for generated files. Currently
${generated} is defined as "../_GENERATED/sams" relative to
this README.

The master build.xml in this directory can be used to create
the complete SAMS system:
	ant

To create the SAMS installer for end users:
	cd samsgui
	ant installer

The created installer gets the name install-sams-${VERSION}.jar
where version is defined in samsgui/build.properties.

To test launching the SAMS installer (under samsgui/):
	ant install

