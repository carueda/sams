@echo off
@rem DOS Batch file to invoke SAMS
@rem $Id$
SET SAMSDIR=$INSTALL_PATH
java "-Dsams.dir=%SAMSDIR%" -jar "%SAMSDIR%\bin\sams.jar"
@echo on

