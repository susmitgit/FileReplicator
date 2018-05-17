
/*
WARNING: THIS FILE IS AUTO-GENERATED. DO NOT MODIFY.

This file was generated from .idl using "rtiddsgen".
The rtiddsgen tool is part of the RTI Connext distribution.
For more information, type 'rtiddsgen -help' at a command shell
or consult the RTI Connext manual.
*/

package com.rti.dds.example.file.messages;

import com.rti.dds.typecode.*;

public class  FileSegmentTypeCode {
    public static final TypeCode VALUE = getTypeCode();

    private static TypeCode getTypeCode() {
        TypeCode tc = null;
        int __i=0;
        StructMember sm[]=new StructMember[4];

        sm[__i]=new  StructMember("fileDescription", false, (short)-1,  false,(TypeCode) com.rti.dds.example.file.messages.FileDescriptionTypeCode.VALUE,0 , false);__i++;
        sm[__i]=new  StructMember("segmentNumber", false, (short)-1,  false,(TypeCode) TypeCode.TC_LONG,1 , false);__i++;
        sm[__i]=new  StructMember("totalSegmentCount", false, (short)-1,  false,(TypeCode) TypeCode.TC_LONG,2 , false);__i++;
        sm[__i]=new  StructMember("contents", false, (short)-1,  false,(TypeCode) new TypeCode((com.rti.dds.example.file.messages.MAX_FILE_SEGMENT_SIZE.VALUE), TypeCode.TC_OCTET),3 , false);__i++;

        tc = TypeCodeFactory.TheTypeCodeFactory.create_struct_tc("com::rti::dds::example::file::messages::FileSegment",ExtensibilityKind.EXTENSIBLE_EXTENSIBILITY,  sm);        
        return tc;
    }
}

