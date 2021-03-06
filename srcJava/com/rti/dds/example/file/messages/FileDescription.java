

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

public class FileDescription   implements Copyable, Serializable{

    public String name=  "" ; /* maximum length = ((com.rti.dds.example.file.messages.MAX_PATH_COMPONENT_SIZE.VALUE)) */
    public String path=  "" ; /* maximum length = ((com.rti.dds.example.file.messages.MAX_PATH_COMPONENT_SIZE.VALUE)) */
    public long size= 0;
    public long lastModifiedDate= 0;

    public FileDescription() {

    }
    public FileDescription (FileDescription other) {

        this();
        copy_from(other);
    }

    public static Object create() {

        FileDescription self;
        self = new  FileDescription();
        self.clear();
        return self;

    }

    public void clear() {

        name=  ""; 
        path=  ""; 
        size= 0;
        lastModifiedDate= 0;
    }

    public boolean equals(Object o) {

        if (o == null) {
            return false;
        }        

        if(getClass() != o.getClass()) {
            return false;
        }

        FileDescription otherObj = (FileDescription)o;

        if(!name.equals(otherObj.name)) {
            return false;
        }
        if(!path.equals(otherObj.path)) {
            return false;
        }
        if(size != otherObj.size) {
            return false;
        }
        if(lastModifiedDate != otherObj.lastModifiedDate) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int __result = 0;
        __result += name.hashCode(); 
        __result += path.hashCode(); 
        __result += (int)size;
        __result += (int)lastModifiedDate;
        return __result;
    }

    /**
    * This is the implementation of the <code>Copyable</code> interface.
    * This method will perform a deep copy of <code>src</code>
    * This method could be placed into <code>FileDescriptionTypeSupport</code>
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

        FileDescription typedSrc = (FileDescription) src;
        FileDescription typedDst = this;

        typedDst.name = typedSrc.name;
        typedDst.path = typedSrc.path;
        typedDst.size = typedSrc.size;
        typedDst.lastModifiedDate = typedSrc.lastModifiedDate;

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

        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("name: ").append(name).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("path: ").append(path).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("size: ").append(size).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("lastModifiedDate: ").append(lastModifiedDate).append("\n");  

        return strBuffer.toString();
    }

}
