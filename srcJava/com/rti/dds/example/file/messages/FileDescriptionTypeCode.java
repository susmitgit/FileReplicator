
/*
WARNING: THIS FILE IS AUTO-GENERATED. DO NOT MODIFY.

This file was generated from .idl using "rtiddsgen".
The rtiddsgen tool is part of the RTI Connext distribution.
For more information, type 'rtiddsgen -help' at a command shell
or consult the RTI Connext manual.
*/

package com.rti.dds.example.file.messages;

import com.rti.dds.typecode.*;

public class  FileDescriptionTypeCode {
    public static final TypeCode VALUE = getTypeCode();

    private static TypeCode getTypeCode() {
        TypeCode tc = null;
        int __i=0;
        StructMember sm[]=new StructMember[4];

        sm[__i]=new  StructMember("name", false, (short)-1,  false,(TypeCode) new TypeCode(TCKind.TK_STRING,(com.rti.dds.example.file.messages.MAX_PATH_COMPONENT_SIZE.VALUE)),0 , false);__i++;
        sm[__i]=new  StructMember("path", false, (short)-1,  false,(TypeCode) new TypeCode(TCKind.TK_STRING,(com.rti.dds.example.file.messages.MAX_PATH_COMPONENT_SIZE.VALUE)),1 , false);__i++;
        sm[__i]=new  StructMember("size", false, (short)-1,  false,(TypeCode) TypeCode.TC_LONGLONG,2 , false);__i++;
        sm[__i]=new  StructMember("lastModifiedDate", false, (short)-1,  false,(TypeCode) TypeCode.TC_LONGLONG,3 , false);__i++;

        tc = TypeCodeFactory.TheTypeCodeFactory.create_struct_tc("com::rti::dds::example::file::messages::FileDescription",ExtensibilityKind.EXTENSIBLE_EXTENSIBILITY,  sm);        
        return tc;
    }
}

