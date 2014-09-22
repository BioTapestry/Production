/*
**    Copyright (C) 2003-2011 Institute for Systems Biology 
**                            Seattle, Washington, USA. 
**
**    This library is free software; you can redistribute it and/or
**    modify it under the terms of the GNU Lesser General Public
**    License as published by the Free Software Foundation; either
**    version 2.1 of the License, or (at your option) any later version.
**
**    This library is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
**    Lesser General Public License for more details.
**
**    You should have received a copy of the GNU Lesser General Public
**    License along with this library; if not, write to the Free Software
**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.systemsbiology.biotapestry.cmd.instruct;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.HashSet;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.genome.DBNode;
import org.systemsbiology.biotapestry.genome.DBGene;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.util.ResourceManager;

/***************************************************************************
**
** Used to build networks from a dialog description
*/
  
public abstract class BuildInstruction implements Cloneable {
  
  public static final int MIN_NODE_TYPE = Node.MIN_NODE_TYPE;
  public static final int MAX_NODE_TYPE = Node.MAX_NODE_TYPE; 
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  protected String id_;
  protected int sourceType_;
  protected String sourceName_;
  protected int targType_;
  protected String targName_;
  protected InstructionRegions regions_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  protected static final int BASE_TOKENS = 2;  
  
  /***************************************************************************
  **
  ** Constructor
  **
  */
  
  public BuildInstruction(String id, int sourceType, String sourceName,
                          int targType, String targName) {
    this.id_ = id;
    this.sourceType_ = sourceType;
    this.sourceName_ = sourceName;
    this.targType_ = targType;
    this.targName_ = targName;
    this.regions_ = null;
  }
  
  /***************************************************************************
  **
  ** Copy Constructor
  **
  */
  
  public BuildInstruction(BuildInstruction other) {
    this.id_ = other.id_;
    this.sourceType_ = other.sourceType_;
    this.sourceName_ = other.sourceName_;
    this.targType_ = other.targType_;
    this.targName_ = other.targName_;
    this.regions_ = (other.regions_ == null) ? null : new InstructionRegions(other.regions_);    
  }

  /***************************************************************************
  **
  ** Get the ID
  */
  
  public String getID() {
    return (id_);
  }
  
  /***************************************************************************
  **
  ** Set the ID
  */
  
  public void setID(String id) {
    id_ = id;
    return;
  } 
  
  /***************************************************************************
  **
  ** Get the Source Type
  */
  
  public int getSourceType() {
    return (sourceType_);
  }
  
  /***************************************************************************
  **
  ** Set the Source Type
  */
  
  public void setSourceType(int sourceType) {
    sourceType_ = sourceType;
    return;
  }   
  
  /***************************************************************************
  **
  ** Get the Source Name
  */
  
  public String getSourceName() {
    return (sourceName_);
  }
  
  /***************************************************************************
  **
  ** Set the Source Name
  */
  
  public void setSourceName(String sourceName) {
    sourceName_ = sourceName;
    return;
  }  
 
  /***************************************************************************
  **
  ** Answer if we have a target
  */
  
  public boolean hasTarget() {
    return (targName_ != null);
  }  
  
  /***************************************************************************
  **
  ** Get the Targ Type
  */
  
  public int getTargType() {
    return (targType_);
  }
  
  /***************************************************************************
  **
  ** Set the Targ Type 
  */
  
  public void setTargType(int targType) {
    targType_ = targType;
    return;
  } 
  
  /***************************************************************************
  **
  ** Get the Targ Name
  */
  
  public String getTargName() {
    return (targName_);
  }
  
  /***************************************************************************
  **
  ** Set the Targ Name
  */
  
  public void setTargName(String targName) {
    targName_ = targName;
    return;
  } 
  
  /***************************************************************************
  **
  ** Answer if we have target regions
  */
  
  public boolean hasRegions() {
    return (regions_ != null);
  }
  
  /***************************************************************************
  **
  ** Get the regions
  */
  
  public InstructionRegions getRegions() {
    return (regions_);
  }

  /***************************************************************************
  **
  ** Set the regions
  */
  
  public void setRegions(InstructionRegions regions) {
    regions_ = regions;
    return;
  }  

  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public BuildInstruction clone() {
    try {
      BuildInstruction retval = (BuildInstruction)super.clone();
      retval.regions_ = (this.regions_ == null) ? null : (InstructionRegions)this.regions_.clone();
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }  

  /***************************************************************************
  **
  ** Answer if we have an evidence level
  */
  
  public abstract boolean hasEvidenceLevel();
  
  /***************************************************************************
  **
  ** Get the names of all contained named nodes
  */
  
  public abstract Set<String> getNamedNodes();  

  /***************************************************************************
  **
  ** Get the names of all contained named nodes in the source region
  */
  
  public abstract Set<String> getNamedSourceRegionNodes();  
  
  /***************************************************************************
  **
  ** Get the names of all contained named nodes in the target region
  */
  
  public abstract Set<String> getNamedTargetRegionNodes();    
  
  /***************************************************************************
  **
  ** Handle motif adds
  */
  
  public abstract void addMotifs(List<DialogBuiltMotifPair> motifs, Map<String, Integer> typeTracker);
  
  /***************************************************************************
  **
  ** Check for global type consistency
  */
  
  public abstract boolean typesAreConsistent(Map<String, Integer> typeTracker);
  
  /***************************************************************************
  **
  ** Help for checking types
  */
  
  protected void typeCheckSupport(String name, int type, Map<String, Integer> typeTracker) {
    if (!typeChecks(name, type, typeTracker)) {
      throw new IllegalStateException();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Help for checking types
  */
  
  protected boolean typeChecks(String name, int type, Map<String, Integer> typeTracker) {
    String norm = DataUtil.normKey(name);
    Integer oldType = (Integer)typeTracker.get(norm);
    if ((oldType != null) && (oldType.intValue() != type)) {
      return (false);
    }
    typeTracker.put(norm, new Integer(type));
    return (true);
  }  

  /***************************************************************************
  **
  ** Write the instruction to XML
  **
  */
  
  protected void writeXMLSupport(PrintWriter out, Indenter ind) {
    if (regions_ != null) {
      throw new IllegalStateException();
    }
    out.print(" id=\"");
    out.print(id_);    
    out.print("\" srcName=\"");
    out.print(CharacterEntityMapper.mapEntities(sourceName_, false));
    out.print("\" srcType=\"");    
    out.print(mapToNodeTypeTag(sourceType_));
    if (targName_ != null) {
      out.print("\" targName=\"");
      out.print(CharacterEntityMapper.mapEntities(targName_, false));
      out.print("\" targType=\"");
      out.print(mapToNodeTypeTag(targType_));
    }
    out.print("\"");
    return;
  }
  
  /***************************************************************************
  **
  ** Write the instruction to XML
  **
  */ 
  
  public abstract void writeXML(PrintWriter out, Indenter ind);
 
  /***************************************************************************
  **
  ** Output for CSV:
  **
  */
  
  public abstract String toCSVString(StringBuffer buf, String modelName); 
 
  /***************************************************************************
  **
  ** Answers if this instruction builds the same thing as another (ids and regions are ignored)
  **
  */
 
  public boolean sameDefinition(BuildInstruction other) {
    return ((this.sourceType_ == other.sourceType_) &&
            (this.targType_ == other.targType_) &&
            (DataUtil.keysEqual(this.sourceName_, other.sourceName_)) &&
            (((this.targName_ == null) && (other.targName_ == null)) ||
             ((this.targName_ != null) && (other.targName_ != null) 
              && DataUtil.keysEqual(this.targName_, other.targName_))));
  }
  
  /***************************************************************************
  **
  ** Standard equals:
  **
  */
  
  public boolean equals(Object other) {
    if (this == other) {
      return (true);
    }
    if (other == null) {
      return (false);
    }
    if (!(other instanceof BuildInstruction)) {
      return (false);
    }
    BuildInstruction otherBI = (BuildInstruction)other;
    if (!this.id_.equals(otherBI.id_)) {
      return (false);
    }
    if (!this.sameDefinition(otherBI)) {
      return (false);
    }
    
    if (this.regions_ == null) {
      return (otherBI.regions_ == null);
    }
    
    return (this.regions_.equals(otherBI.regions_));
  }   

  /***************************************************************************
  **
  ** Standard hashCode:
  **
  */
  
  @Override
  public int hashCode() { 
    return (id_.hashCode() + sourceType_ + DataUtil.normKey(sourceName_).hashCode() 
                           + targType_ + ((targName_ != null) ?  DataUtil.normKey(targName_).hashCode() : 0) 
                           + ((regions_ != null) ? regions_.hashCode() : 0));
  }
  
  /***************************************************************************
  **
  ** "Relaxed" hashCode for wrappers:
  **
  */
  
  public int relaxedHashCode() { 
    return (sourceType_ + DataUtil.normKey(sourceName_).hashCode() 
                        + targType_ + ((targName_ != null) ?  DataUtil.normKey(targName_).hashCode() : 0));
  }
  
  /***************************************************************************
  **
  ** Standard toString:
  **
  */
  
  @Override
  public String toString() { 
    return ("BuildInstruction: " + id_ + " " + sourceType_ + " " +  sourceName_ + " " + 
                                   targType_ + " " + targName_ + " " + regions_);
  }

  /***************************************************************************
  **
  ** For UI dialogs:
  ** 
  */
  
  public abstract String displayStringUI(ResourceManager rMan);

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add(GeneralBuildInstruction.XML_TAG);
    retval.add(SignalBuildInstruction.XML_TAG); 
    retval.add(LoneNodeBuildInstruction.XML_TAG);    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static BuildInstruction buildFromXML(String elemName, 
                                              Attributes attrs) throws IOException {
                                                
    if (!elemName.equals(GeneralBuildInstruction.XML_TAG) &&
        !elemName.equals(LoneNodeBuildInstruction.XML_TAG) &&            
        !elemName.equals(SignalBuildInstruction.XML_TAG)) {
      return (null);
    }
    
    boolean noTargs = (elemName.equals(LoneNodeBuildInstruction.XML_TAG));    
    
    
    String id = null;
    String sourceTypeStr = null;
    String sourceName = null;
    String targTypeStr = null;
    String targName = null;
    
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("id")) {
          id = val;
        } else if (key.equals("srcType")) {
          sourceTypeStr = val;          
        } else if (key.equals("srcName")) {
          sourceName = CharacterEntityMapper.unmapEntities(val, false);
        } else if (key.equals("targType")) {
          targTypeStr = val;
        } else if (key.equals("targName")) {
          targName = CharacterEntityMapper.unmapEntities(val, false);
        }
      }
    }
    
    //
    // Legacy IO support: we allow null id string from first-generation
    // instruction IO:
    //
    
    if ((sourceTypeStr == null) || (sourceName == null)) {
      throw new IOException();
    }
    
     if (!noTargs && ((targTypeStr == null) || (targName == null))) {
      throw new IOException();
    }
    
    
    int sourceType;
    int targType;
    
    try {
      sourceType = mapFromNodeTypeTag(sourceTypeStr);
      if (!noTargs) {
        targType = mapFromNodeTypeTag(targTypeStr);
      } else {
        targType = Node.NO_NODE_TYPE;
      }
    } catch (IllegalArgumentException iae) {
      System.err.println("bad map");
      throw new IOException();
    }
   
    if (elemName.equals(GeneralBuildInstruction.XML_TAG)) {
      return (GeneralBuildInstruction.buildFromXML(elemName, attrs, id, sourceType, 
                                                   sourceName, targType, targName));
    } else if (elemName.equals(SignalBuildInstruction.XML_TAG)) {
      return (SignalBuildInstruction.buildFromXML(elemName, attrs, id, sourceType, 
                                                  sourceName, targType, targName));
    } else if (elemName.equals(LoneNodeBuildInstruction.XML_TAG)) {
      return (LoneNodeBuildInstruction.buildFromXML(elemName, attrs, id, sourceType, sourceName));
    } else {
      throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** Build from csv-derived tokens
  **
  */
  
  public static BuildInstruction buildFromTokens(List<String> tokens, String typeStr,
                                                 int startIndex, String id) throws IOException {
    
    if (!typeStr.equals(GeneralBuildInstruction.CSV_TAG) &&
        !typeStr.equals(LoneNodeBuildInstruction.CSV_TAG) &&            
        !typeStr.equals(SignalBuildInstruction.CSV_TAG)) {
      throw new IOException();
    }
    
    int baseCount;
    if (typeStr.equals(GeneralBuildInstruction.CSV_TAG)) {
      baseCount = BASE_TOKENS + GeneralBuildInstruction.CSV_TARGET_COUNT;
    } else if (typeStr.equals(SignalBuildInstruction.CSV_TAG)) {
      baseCount = BASE_TOKENS + SignalBuildInstruction.CSV_TARGET_COUNT;  
    } else if (typeStr.equals(LoneNodeBuildInstruction.CSV_TAG)) {
      baseCount = BASE_TOKENS + LoneNodeBuildInstruction.CSV_TARGET_COUNT;
    } else {
      throw new IllegalStateException();
    }

    if (tokens.size() < (startIndex + baseCount)) {
      throw new IOException();
    }
    
    String sourceTypeStr = ((String)tokens.get(startIndex)).toLowerCase();
    String sourceName = (String)tokens.get(startIndex + 1);
    String targTypeStr = null;
    String targName = null;
    if (baseCount == 4) {
      targTypeStr = ((String)tokens.get(startIndex + 2)).toLowerCase();
      targName = (String)tokens.get(startIndex + 3);
    }
        
    if ((sourceTypeStr == null) || (sourceName == null)) {
      throw new IOException();
    }
    
    if ((baseCount == 4) && ((targTypeStr == null) || (targName == null))) {
      throw new IOException();
    }    
   
    if (sourceTypeStr.trim().equals("")) {
      throw new IOException();
    }
    if (sourceName.trim().equals("")) {
      throw new IOException();
    }
    if (baseCount == 4) {
      if (targTypeStr.trim().equals("")) {
        throw new IOException();
      }
      if (targName.trim().equals("")) {
        throw new IOException();
      } 
    }
    
    int sourceType;
    int targType;
    
    try {
      sourceType = mapFromNodeTypeTag(sourceTypeStr); 
      targType = (baseCount == 4) ? mapFromNodeTypeTag(targTypeStr) : Node.NO_NODE_TYPE;       
    } catch (IllegalArgumentException iae) {
      throw new IOException();
    }
   
    int tokCount;
    int regCount;
    BuildInstruction retval;
    if (typeStr.equals(GeneralBuildInstruction.CSV_TAG)) {
      tokCount = GeneralBuildInstruction.CSV_TOKEN_COUNT;
      regCount = GeneralBuildInstruction.CSV_REGION_COUNT; 
      retval = GeneralBuildInstruction.buildFromCSV(tokens, startIndex, id, sourceType, 
                                                    sourceName, targType, targName);
    } else if (typeStr.equals(SignalBuildInstruction.CSV_TAG)) {
      tokCount = SignalBuildInstruction.CSV_TOKEN_COUNT;
      regCount = SignalBuildInstruction.CSV_REGION_COUNT;      
      retval = SignalBuildInstruction.buildFromCSV(tokens, startIndex, id, sourceType, 
                                                   sourceName, targType, targName);
    } else if (typeStr.equals(LoneNodeBuildInstruction.CSV_TAG)) {
      tokCount = LoneNodeBuildInstruction.CSV_TOKEN_COUNT;
      regCount = LoneNodeBuildInstruction.CSV_REGION_COUNT;     
      retval = LoneNodeBuildInstruction.buildFromCSV(tokens, startIndex, id, sourceType, sourceName);
    } else {
      throw new IllegalStateException();
    }
  
    if (tokens.size() == (startIndex + baseCount + tokCount)) {
      return (retval);
    }  

    if (tokens.size() < (startIndex + baseCount + tokCount + regCount)) {
      throw new IOException();
    }    
    
    String srcRegion = (String)tokens.get(startIndex + baseCount + tokCount);
    String targRegion = (regCount > 1) ? (String)tokens.get(startIndex + baseCount + tokCount + 1) : null;
    InstructionRegions ir = new InstructionRegions();
    InstructionRegions.RegionTuple tup = 
      new InstructionRegions.RegionTuple(srcRegion, targRegion);              
    ir.addRegionTuple(tup);
    retval.setRegions(ir);
    return (retval);
  }  

  /***************************************************************************
  **
  ** Map node types
  */

  public static String mapToNodeTypeTag(int val) {
    return ((val == DBNode.GENE) ? DBGene.GENE_TAG : DBNode.mapToTag(val));
  }
  
  /***************************************************************************
  **
  ** Map node types to values
  */

  public static int mapFromNodeTypeTag(String tag) {
    return (tag.equals(DBGene.GENE_TAG) ? DBNode.GENE : DBNode.mapFromTag(tag));
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** Used for creating sets of BI with identical definitions
  */
  
  public static class BIWrapper {
    
    public BuildInstruction canonical;

    public BIWrapper(BuildInstruction bi) {
      canonical = bi;
    }
    public String toString() {
      return ("Wrapper: " + canonical.toString());
    }
    
    public boolean equals(Object other) {
      if (this == other) {
        return (true);
      }
      if (other == null) {
        return (false);
      }
      if (!(other instanceof BIWrapper)) {
        return (false);
      }
      BIWrapper otherBIW = (BIWrapper)other;
         
      return (this.canonical.sameDefinition(otherBIW.canonical));
    }
    
    public int hashCode() {
      return (this.canonical.relaxedHashCode());
    }
  }
}
