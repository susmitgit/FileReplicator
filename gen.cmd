
rem generate with example code
rem C:\Rti\rti_connext_dds-5.2.0\bin\rtiddsgen -language Java -example i86Win32j2sdk1.3 -replace -d srcJava FileExample.idl

rem generate without example code
C:\Rti\rti_connext_dds-5.2.0\bin\rtiddsgen -xml -language Java -replace -d srcJava -typecode FileReplicator.idl
