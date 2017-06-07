/*
**    Copyright (C) 2003-2017 Institute for Systems Biology 
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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.LinkageInstance;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.Indenter;

/***************************************************************************
**
** Used to build submodels from a dialog description
*/
  
public class BuildInstructionInstance implements Cloneable {
     
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private String baseID_;
  private String sourceRegionID_;
  private String targetRegionID_;
  private Double activityLevel_;  // null unless it is variable...
  private int activityType_;  

  private static final String XML_TAG  = "buildInstructionInstance";    
  
  /***************************************************************************
  **
  ** Constructor
  **
  */
  
  public BuildInstructionInstance(String baseID, String sourceRegionID, String targetRegionID) {
                                 // , Double activityLevel, int activityType) {
    baseID_ = baseID;
    sourceRegionID_ = sourceRegionID;
    targetRegionID_ = targetRegionID;
    activityLevel_ = null; //activityLevel;
    activityType_ = LinkageInstance.ACTIVE; //activityType;    
  }
  
  /***************************************************************************
  **
  ** Copy Constructor
  **
  */
  
  public BuildInstructionInstance(BuildInstructionInstance other) {
    this.baseID_ = other.baseID_;
    this.sourceRegionID_ = other.sourceRegionID_;
    this.targetRegionID_ = other.targetRegionID_;
    this.activityLevel_ = other.activityLevel_;
    this.activityType_ = other.activityType_;    
  }
 
  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public BuildInstructionInstance clone() {
    try {
      BuildInstructionInstance retval = (BuildInstructionInstance)super.clone();
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }   

  /***************************************************************************
  **
  ** Get the base ID
  */
  
  public String getBaseID() {
    return (baseID_);
  }
  
  /***************************************************************************
  **
  ** Set the base ID 
  */
  
  public void setBaseID(String baseID) {
    baseID_ = baseID;
    return;
  } 
  
  /***************************************************************************
  **
  ** Get the Source Region ID
  */
  
  public String getSourceRegionID() {
    return (sourceRegionID_);
  }
  
  /***************************************************************************
  **
  ** Set the Source Region ID
  */
  
  public void setSourceRegionID(String sourceRegionID) {
    sourceRegionID_ = sourceRegionID;
    return;
  } 
  
  /***************************************************************************
  **
  ** Get the Target Region ID
  */
  
  public String getTargetRegionID() {
    return (targetRegionID_);
  }
  
  /***************************************************************************
  **
  ** Answer if we have a target region
  */
  
  public boolean hasTargetRegionID() {
    return (targetRegionID_ != null);
  }  
  
  /***************************************************************************
  **
  ** Set the Target Region ID
  */
  
  public void setTargetRegionID(String targetRegionID) {
    targetRegionID_ = targetRegionID;
    return;
  }
  
  /***************************************************************************
  **
  ** Output for CSV:
  **
  */
  
  public String toCSVString(DataAccessContext dacx, StringBuffer buf, String modelName) {
    BuildInstruction bi = dacx.getInstructSrc().getBuildInstruction(baseID_);
    String base = bi.toCSVString(buf, modelName);
    buf.setLength(0);
    buf.append(base);
    buf.append(",\"");
    buf.append(sourceRegionID_);
    buf.append("\"");
    if (targetRegionID_ != null) {
      buf.append(",\"");
      buf.append(targetRegionID_);
      buf.append("\"");
    }
    return (buf.toString());
  }
   
  /***************************************************************************
  **
  ** Write the instruction to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.print("<");
    out.print(XML_TAG);
    out.print(" ref=\"");
    out.print(baseID_);
    if (sourceRegionID_ != null) {
      out.print("\" srcRegion=\"");    
      out.print(CharacterEntityMapper.mapEntities(sourceRegionID_, false));
    }
    if (targetRegionID_ != null) {
      out.print("\" targRegion=\"");
      out.print(CharacterEntityMapper.mapEntities(targetRegionID_, false));
    }
    if (activityType_ != LinkageInstance.ACTIVE) {
      out.print("\" activity=\"");
      out.print(LinkageInstance.mapActivityTypes(activityType_));
    }
    if (activityLevel_ != null) {
      out.print("\" activityLevel=\"");
      out.print(activityLevel_);
    }
    out.println("\" />");
    return;
  } 
  
  /***************************************************************************
  **
  ** Standard toString:
  **
  */
  
  public String toString() { 
    return ("buildInstructionInstance: " + baseID_ + " " +  sourceRegionID_ + " " + 
             targetRegionID_ + " " + activityType_ + " " + activityLevel_);
  }

  /***************************************************************************
  **
  ** Customized hashcode:
  **
  */
  
  public int hashCode() {
    int retval = baseID_.hashCode();
    if (sourceRegionID_ != null) { 
      retval += sourceRegionID_.hashCode();
    }
    if (targetRegionID_ != null) { 
      retval += targetRegionID_.hashCode();
    }
    
    retval += activityType_;
    
    if (activityLevel_ != null) {
      retval += Math.round(activityLevel_.doubleValue() * 100.0);
    }
   
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Customized equals:
  **
  */
  
  public boolean equals(Object other) {
    if (this == other) {
      return (true);
    }    
    if (other == null) {
      return (false);
    }
    if (!(other instanceof BuildInstructionInstance)) {
      return (false);
    }
    
    BuildInstructionInstance otherBII = (BuildInstructionInstance)other;
    if (!this.baseID_.equals(otherBII.baseID_)) {
      return (false);
    }
    
    if (this.sourceRegionID_ == null) {
      if (otherBII.sourceRegionID_ != null) {
        return (false);
      }
    } else if (!this.sourceRegionID_.equals(otherBII.sourceRegionID_)) {
      return (false);
    }
    
    if (this.targetRegionID_ == null) {
      if (otherBII.targetRegionID_ != null) {
        return (false);
      }
    } else if (!this.targetRegionID_.equals(otherBII.targetRegionID_)) {
      return (false);
    }
    
    if (this.activityType_ != otherBII.activityType_) {
      return (false);
    }
    
    if (this.activityType_ == LinkageInstance.VARIABLE) {
      return (this.activityLevel_.equals(otherBII.activityLevel_));
    }

    return (true);
  }  
  
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
    retval.add(XML_TAG);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static BuildInstructionInstance buildFromXML(String elemName, 
                                                      Attributes attrs) throws IOException {
                                                
    if (!elemName.equals(XML_TAG)) {
      return (null);
    }

    String baseID = null;
    String sourceRegionID = null;
    String targetRegionID = null;
    String activityStr = null;
    String activityLevelStr = null;
    
        
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("ref")) {
          baseID = val;
        } else if (key.equals("srcRegion")) {
          sourceRegionID = CharacterEntityMapper.unmapEntities(val, false);
        } else if (key.equals("targRegion")) {
          targetRegionID = CharacterEntityMapper.unmapEntities(val, false);
        } else if (key.equals("activity")) {
          activityStr = val;
        } else if (key.equals("activityLevel")) {
          activityLevelStr = val;
        }
      }
    }
    
    if (baseID == null) {
      throw new IOException();
    }
    /*  
    int activityType = LinkageInstance.ACTIVE;
    if (activityStr != null) {
      try {
        activityType = LinkageInstance.mapActivityTypeTag(activityStr);
      } catch (IllegalArgumentException iae) {
        throw new IOException();
      }
    }
    
    Double activityLevel = null;
    if (activityLevelStr != null) {
      try {
        activityLevel = new Double(activityLevelStr); 
      } catch (NumberFormatException nfe) {
        throw new IOException();
      }     
    }
     
    if (((activityType == LinkageInstance.VARIABLE) && (activityLevel == null)) ||
        ((activityType != LinkageInstance.VARIABLE) && (activityLevel != null))) {  
      throw new IOException();
    }      
    */
    return (new BuildInstructionInstance(baseID, sourceRegionID, targetRegionID)); //, activityLevel, activityType));

  }
  
}
