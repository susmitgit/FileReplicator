

/*
WARNING: THIS FILE IS AUTO-GENERATED. DO NOT MODIFY.

This file was generated from .idl using "rtiddsgen".
The rtiddsgen tool is part of the RTI Connext distribution.
For more information, type 'rtiddsgen -help' at a command shell
or consult the RTI Connext manual.
*/

package com.rti.dds.example.file.messages;

import com.rti.dds.infrastructure.*;
import com.rti.dds.infrastructure.Copyable;
import java.io.Serializable;
import com.rti.dds.cdr.CdrHelper;

public class FileFragment   implements Copyable, Serializable{

    public com.rti.dds.example.file.messages.FileDescription fileDescription = (com.rti.dds.example.file.messages.FileDescription)com.rti.dds.example.file.messages.FileDescription.create();
    public int segmentNumber= 0;
    public int totalSegmentCount= 0;
    public ByteSeq contents =  new ByteSeq(((com.rti.dds.example.file.messages.MAX_FILE_SEGMENT_SIZE.VALUE)));

    public FileFragment() {

    }
    public FileFragment (FileFragment other) {

        this();
        copy_from(other);
    }

    public static Object create() {

        FileFragment self;
        self = new  FileFragment();
        self.clear();
        return self;

    }

    public void clear() {

        if (fileDescription != null) {
            fileDescription.clear();
        }
        segmentNumber= 0;
        totalSegmentCount= 0;
        if (contents != null) {
            contents.clear();
        }
    }

    public boolean equals(Object o) {

        if (o == null) {
            return false;
        }        

        if(getClass() != o.getClass()) {
            return false;
        }

        FileFragment otherObj = (FileFragment)o;

        if(!fileDescription.equals(otherObj.fileDescription)) {
            return false;
        }
        if(segmentNumber != otherObj.segmentNumber) {
            return false;
        }
        if(totalSegmentCount != otherObj.totalSegmentCount) {
            return false;
        }
        if(!contents.equals(otherObj.contents)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int __result = 0;
        __result += fileDescription.hashCode(); 
        __result += (int)segmentNumber;
        __result += (int)totalSegmentCount;
        __result += contents.hashCode(); 
        return __result;
    }

    /**
    * This is the implementation of the <code>Copyable</code> interface.
    * This method will perform a deep copy of <code>src</code>
    * This method could be placed into <code>FileSegmentTypeSupport</code>
    * rather than here by using the <code>-noCopyable</code> option
    * to rtiddsgen.
    * 
    * @param src The Object which contains the data to be copied.
    * @return Returns <code>this</code>.
    * @exception NullPointerException If <code>src</code> is null.
    * @exception ClassCastException If <code>src</code> is not the 
    * same type as <code>this</code>.
    * @see com.rti.dds.infrastructure.Copyable#copy_from(java.lang.Object)
    */
    public Object copy_from(Object src) {

        FileFragment typedSrc = (FileFragment) src;
        FileFragment typedDst = this;

        typedDst.fileDescription = (com.rti.dds.example.file.messages.FileDescription) typedDst.fileDescription.copy_from(typedSrc.fileDescription);
        typedDst.segmentNumber = typedSrc.segmentNumber;
        typedDst.totalSegmentCount = typedSrc.totalSegmentCount;
        typedDst.contents.copy_from(typedSrc.contents);

        return this;
    }

    public String toString(){
        return toString("", 0);
    }

    public String toString(String desc, int indent) {
        StringBuffer strBuffer = new StringBuffer();        

        if (desc != null) {
            CdrHelper.printIndent(strBuffer, indent);
            strBuffer.append(desc).append(":\n");
        }

        strBuffer.append(fileDescription.toString("fileDescription ", indent+1));
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("segmentNumber: ").append(segmentNumber).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("totalSegmentCount: ").append(totalSegmentCount).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);
        strBuffer.append("contents: ");
        for(int i__ = 0; i__ < contents.size(); ++i__) {
            if (i__!=0) strBuffer.append(", ");
            strBuffer.append(contents.get(i__));
        }
        strBuffer.append("\n"); 

        return strBuffer.toString();
    }

}
