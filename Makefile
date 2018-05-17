######################################################################
# Makefile for FileReplicator
#
# (c) Copyright, Real-Time Innovations, 2006.  All rights reserved.
# No duplications, whole or partial, manual or electronic, may be made
# without express written permission.  Any such copies, or
# revisions thereof, must display this notice unaltered.
# This code contains trade secrets of Real-Time Innovations, Inc.
#
# To compile, type:
# 	gmake -f Makefile
#
# Note: This makefile may require alterations to build on your system.
# 
######################################################################

# Make sure these binaries are in your path, or use absolute paths
JAVA = java
JAVAC = javac
JAVADOC = javadoc
JAR = jar
RTIDDSGEN = rtiddsgen

OUT_JAR = lib/FileReplicator.jar
NDDS_CLASSPATH = $(NDDSHOME)/lib/java/nddsjavad.jar

IDL_DEFS= FileReplicator.idl
BIN_ROOT = class
SRC_ROOT = srcJava
DOC_ROOT = docs/javadocs

PACKAGES = com.rti.dds.example.file \
	com.rti.dds.example.file.messages \
	com.rti.dds.example.util
BASE_FILES = com/rti/dds/example/file/messages/FileDescription \
	com/rti/dds/example/file/messages/FileDescriptionSeq \
	com/rti/dds/example/file/messages/FileDescriptionTypeCode \
	com/rti/dds/example/file/messages/FileDescriptionTypeSupport \
	com/rti/dds/example/file/messages/FileSegment \
	com/rti/dds/example/file/messages/FileSegmentDataReader \
	com/rti/dds/example/file/messages/FileSegmentDataWriter \
	com/rti/dds/example/file/messages/FileSegmentSeq \
	com/rti/dds/example/file/messages/FileSegmentTypeCode \
	com/rti/dds/example/file/messages/FileSegmentTypeSupport \
	com/rti/dds/example/file/messages/MAX_FILE_SEGMENT_SIZE \
	com/rti/dds/example/file/messages/MAX_PATH_COMPONENT_SIZE \
	com/rti/dds/example/file/BlockingQueue \
	com/rti/dds/example/file/FileReconstructor \
	com/rti/dds/example/file/FileSegmenter \
	com/rti/dds/example/file/FileSegmentPublisher \
	com/rti/dds/example/file/FileSegmentSubscriber \
	com/rti/dds/example/file/FileSegmentWriteThread \
	com/rti/dds/example/file/ReaderTrackingWriterListener \
	com/rti/dds/example/util/DebugDataReaderListener \
	com/rti/dds/example/util/DebugDataWriterListener \
	com/rti/dds/example/util/DebugDomainParticipantListener \
	com/rti/dds/example/util/DebugSubscriberListener \
	com/rti/dds/example/util/DebugTopicListener \
	com/rti/dds/example/util/ProgramOptions 

CLASS_FILES = $(patsubst %, $(BIN_ROOT)/%.class, $(BASE_FILES))
SOURCE_FILES = $(patsubst %, $(SRC_ROOT)/%.java, $(BASE_FILES))

$(SRC_ROOT)/com/rti/dds/example/file/messages/%: $(IDL_DEFS)
	$(RTIDDSGEN) $(IDL_DEFS) -language Java -replace -d $(SRC_ROOT)

$(BIN_ROOT)/%.class: $(SRC_ROOT)/%.java $(IDL_DEFS)
	$(JAVAC) -d $(BIN_ROOT) -sourcepath $(SRC_ROOT) -classpath "$(NDDS_CLASSPATH)" $<

all: $(CLASS_FILES) jar

$(OUT_JAR): $(CLASS_FILES)
	$(JAR) cf $(OUT_JAR) -C class com

jar: $(OUT_JAR)

docs: $(SOURCE_FILES)
	$(JAVADOC) -quiet -use -private -sourcepath $(SRC_ROOT) -d $(DOC_ROOT) $(PACKAGES)

clean:
	rm -rf $(BIN_ROOT)/*
	rm $(OUT_JAR)
