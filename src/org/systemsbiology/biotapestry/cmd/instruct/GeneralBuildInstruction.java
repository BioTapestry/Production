/*
**    Copyright (C) 2003-2013 Institute for Systems Biology 
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
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.HashSet;
import java.text.MessageFormat;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.util.ResourceManager;

/***************************************************************************
**
** Used to build networks from a dialog description
*/
  
public class GeneralBuildInstruction extends BuildInstruction {
  
  public static final int POSITIVE       = 0;
  public static final int NEGATIVE       = 1;
  public static final int NEUTRAL        = 2;
  public static final int NUM_SIGN_TYPES = 3;
  
  public static final String POSITIVE_STR = "positive";
  public static final String NEGATIVE_STR = "negative";
  public static final String NEUTRAL_STR  = "neutral";
  
  public static final String CSV_TAG       = "general";
  public static final int CSV_TOKEN_COUNT  = 1;
  public static final int CSV_TARGET_COUNT = 2;
  public static final int CSV_REGION_COUNT = 2;  
  public static final int CSV_ADVANCED_COUNT = 0;  
  //public static final int CSV_ADVANCED_COUNT = 2;
  
  protected static final String XML_TAG  = "generalBuildInstruction";

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
    
  private int linkSign_;
  private int evidenceLevel_;
  
  /***************************************************************************
  **
  ** Constructor
  **
  */
  
  public GeneralBuildInstruction(String id, int sourceType, String sourceName, int linkSign, 
                                 int targType, String targName) {
    this(id, sourceType, sourceName, linkSign, targType, targName, Linkage.LEVEL_NONE);
  }
  
  /***************************************************************************
  **
  ** Constructor with advanced fields
  **
  */
  
  public GeneralBuildInstruction(String id, int sourceType, String sourceName, int linkSign, 
                                 int targType, String targName, int evidenceLevel) {
    super(id, sourceType, sourceName, targType, targName);
    this.linkSign_ = linkSign;
    this.evidenceLevel_ = evidenceLevel;
  }  
  
  /***************************************************************************
  **
  ** Copy Constructor
  **
  */
  
  public GeneralBuildInstruction(GeneralBuildInstruction other) {
    super(other);
    this.linkSign_ = other.linkSign_;
    this.evidenceLevel_ = other.evidenceLevel_;
  }
  
  /***************************************************************************
  **
  ** Clone
  */

  public GeneralBuildInstruction clone() {
    return ((GeneralBuildInstruction)super.clone());
  }
  
  /***************************************************************************
  **
  ** Get the link sign
  */
  
  public int getLinkSign() {
    return (linkSign_);
  }
  
  /***************************************************************************
  **
  ** Set the link sign
  */
  
  public void setLinkSign(int linkSign) {
    linkSign_ = linkSign;
    return;
  }

  /***************************************************************************
  **
  ** Answer if we have an evidence level
  */
  
  public boolean hasEvidenceLevel() {
    return (evidenceLevel_ != Linkage.LEVEL_NONE);
  }  
  
  /***************************************************************************
  **
  ** Get the names of all contained named nodes
  */
  
  public Set<String> getNamedNodes() {
    HashSet<String> retval = new HashSet<String>();
    retval.add(sourceName_);
    retval.add(targName_);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the names of all contained named nodes in the source region
  */
  
  public Set<String> getNamedSourceRegionNodes() { 
    HashSet<String> retval = new HashSet<String>();
    retval.add(sourceName_);
    return (retval);
  }

  /***************************************************************************
  **
  ** Get the names of all contained named nodes in the target region
  */
  
  public Set<String> getNamedTargetRegionNodes() {   
    HashSet<String> retval = new HashSet<String>();
    retval.add(targName_);
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Fill in node lists and types
  */
  
  public void addMotifs(List<DialogBuiltMotifPair> motifs, Map<String, Integer> typeTracker) {
    typeCheckSupport(sourceName_, sourceType_, typeTracker);
    typeCheckSupport(targName_, targType_, typeTracker);
    DialogBuiltGeneralProtoMotif dbgpm = 
      new DialogBuiltGeneralProtoMotif(sourceName_, sourceType_, 
                                       targName_, targType_, linkSign_, evidenceLevel_);    
    motifs.add(new DialogBuiltMotifPair(id_, dbgpm, null)); 
    return;
  }
  
  /***************************************************************************
  **
  ** Check for global type consistency
  */
  
  public boolean typesAreConsistent(Map<String, Integer> typeTracker) {
    if (!typeChecks(sourceName_, sourceType_, typeTracker)) {
      return (false);
    }
    if (!typeChecks(targName_, targType_, typeTracker)) {
      return (false);
    }
    return (true);
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
    writeXMLSupport(out, ind); 
    out.print(" linkSign=\"");
    out.print(mapToSignTag(linkSign_));
    if (evidenceLevel_ != Linkage.LEVEL_NONE) {
      out.print("\" evidence=\"");
      out.print(evidenceLevel_);
    }        
    out.println("\" />");
    return;
  }
  
  /***************************************************************************
  **
  ** Answers if this instruction builds the same thing as another (ids and regions are ignored)
  **
  */
 
  public boolean sameDefinition(BuildInstruction other) {
    if (!(other instanceof GeneralBuildInstruction)) {
      return (false);
    }
    if (!super.sameDefinition(other)) {
      return (false);
    }
    GeneralBuildInstruction otherGbi = (GeneralBuildInstruction)other;
    if (this.linkSign_ != otherGbi.linkSign_) {
      return (false);
    }
    return (this.evidenceLevel_ == otherGbi.evidenceLevel_);
  } 
  
  /***************************************************************************
  **
  ** Standard toString:
  **
  */
  
  public String toString() { 
    return (super.toString() + " " + linkSign_ + " " + evidenceLevel_);
  }
  
  /***************************************************************************
  **
  ** Output for CSV:
  **
  */
  
  public String toCSVString(StringBuffer buf, String modelName) { 
    buf.setLength(0);
    buf.append("\"");
    buf.append(CSV_TAG);
    buf.append("\",\"");
    buf.append(modelName);
    buf.append("\",\"");
    buf.append(mapToNodeTypeTag(sourceType_));
    buf.append("\",\"");
    buf.append(sourceName_);
    buf.append("\",\"");
    buf.append(mapToNodeTypeTag(targType_));
    buf.append("\",\"");
    buf.append(targName_);
    buf.append("\",\"");
    buf.append(mapToSignTag(linkSign_));
    buf.append("\"");
    return (buf.toString());
  }
   
  /***************************************************************************
  **
  ** For UI dialogs:
  **
  */
    
  public String displayStringUI(ResourceManager rMan) {   
    String dispSrc = rMan.getString("buildInstruction." + BuildInstruction.mapToNodeTypeTag(sourceType_));
    String dispTrg = rMan.getString("buildInstruction." + BuildInstruction.mapToNodeTypeTag(targType_));       
    String dispSign = rMan.getString("buildInstruction.general" + GeneralBuildInstruction.mapToSignTag(linkSign_));
    String retval = MessageFormat.format(rMan.getString("buildInstruction.generalDisplay"),    
                                         new Object[] {dispSrc, sourceName_, dispSign, dispTrg, targName_});    
    return (retval);
  }

  /***************************************************************************
  **
  ** Standard equals:
  **
  */
  
  public boolean equals(Object other) { 
    if (!super.equals(other)) {
      return (false);
    }
    if (!(other instanceof GeneralBuildInstruction)) {
      return (false);
    }
    GeneralBuildInstruction otherGbi = (GeneralBuildInstruction)other;
   
    if (this.linkSign_ != otherGbi.linkSign_) {
      return (false);
    } 
    
    if (this.evidenceLevel_ != otherGbi.evidenceLevel_) {
      return (false);
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
  ** Handle the attributes for the keyword
  **
  */
  
  public static BuildInstruction buildFromXML(String elemName, 
                                              Attributes attrs,
                                              String id,
                                              int sourceType,
                                              String sourceName,
                                              int targType,
                                              String targName) throws IOException {
    if (!elemName.equals(XML_TAG)) {
      return (null);
    }
    
    String linkSignStr = null; 
    String evidenceStr = null; 
    
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("linkSign")) {
          linkSignStr = val;
        } else if (key.equals("evidence")) {
          evidenceStr = val;
        }
      } 
    }
    
    if (linkSignStr == null) {
      throw new IOException();
    }
    
    int linkSign;    
    try {
      linkSign = mapFromSignTag(linkSignStr);
    } catch (IllegalArgumentException iae) {
      System.err.println("bad map");
      throw new IOException();
    }
    
    /* Belongs in separate processing block!
     
    int activityType = LinkageInstance.ACTIVE;
    if (activityStr != null) {
      try {
        activityType = LinkageInstance.mapActivityTypeTag(activityStr);
      } catch (IllegalArgumentException iae) {
        System.err.println("bad map");
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
   
    int evidenceLevel = Linkage.LEVEL_NONE;
    if (evidenceStr != null) {
      try {
        evidenceLevel = Integer.parseInt(evidenceStr);
      } catch (NumberFormatException nfe) {
        throw new IOException();
      }
    }

    return (new GeneralBuildInstruction(id, sourceType, sourceName, linkSign, targType, targName, 
                                        evidenceLevel));
  }

  /***************************************************************************
  **
  ** Map sign types
  */

  public static String mapToSignTag(int val) {
    switch (val) {
      case POSITIVE:
        return (POSITIVE_STR);
      case NEGATIVE:
        return (NEGATIVE_STR);
      case NEUTRAL:
        return (NEUTRAL_STR); 
      default:
        throw new IllegalStateException();
    }
  }
  
 /***************************************************************************
  **
  ** Map sign types
  */

  public static int invertSign(int val) {
    switch (val) {
      case POSITIVE:
        return (NEGATIVE);
      case NEGATIVE:
        return (POSITIVE);
      case NEUTRAL:
        return (NEUTRAL); 
      default:
        throw new IllegalStateException();
    }
  }

  /***************************************************************************
  **
  ** Map sign types to values
  */

  public static int mapFromSignTag(String tag) {
    if (tag.equals(POSITIVE_STR)) {
      return (POSITIVE);
    } else if (tag.equals(NEGATIVE_STR)) {
      return (NEGATIVE);
    } else if (tag.equals(NEUTRAL_STR)) {
      return (NEUTRAL);  
    } else {
      throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Map sign types
  */

  public static int mapToLinkageSign(int val) {
    switch (val) {
      case POSITIVE:
        return (Linkage.POSITIVE);
      case NEGATIVE:
        return (Linkage.NEGATIVE);
      case NEUTRAL:
        return (Linkage.NONE); 
      default:
        throw new IllegalStateException();
    }
  }

  /***************************************************************************
  **
  ** Map Linkage signs to our sign
  */

  public static int mapFromLinkageSign(int linkageSign) {
    switch (linkageSign) {
      case Linkage.POSITIVE:
        return (POSITIVE);
      case Linkage.NEGATIVE:
        return (NEGATIVE);
      case Linkage.NONE:
        return (NEUTRAL); 
      default:
        throw new IllegalStateException();
    }
  } 
  
  /***************************************************************************
  **
  ** Build from csv-derived tokens
  **
  */
  
  public static BuildInstruction buildFromCSV(List<String> tokens,
                                              int startIndex, 
                                              String id,
                                              int sourceType,
                                              String sourceName,
                                              int targType,
                                              String targName) throws IOException {

    if (tokens.size() < (startIndex + BASE_TOKENS + CSV_TARGET_COUNT + CSV_TOKEN_COUNT)) {
      throw new IOException();
    }                                                
                                                                                                
    String linkSignStr = ((String)tokens.get(startIndex + BASE_TOKENS + CSV_TARGET_COUNT + CSV_TOKEN_COUNT - 1)).trim().toLowerCase();
    
    if ((linkSignStr == null) || (linkSignStr.trim().equals(""))) {
      throw new IOException();
    }
    
    int linkSign;    
    try {
      linkSign = mapFromSignTag(linkSignStr);
    } catch (IllegalArgumentException iae) {
      System.err.println("bad map: " + linkSignStr);
      throw new IOException();
    }
 
    int evidenceLevel = Linkage.LEVEL_NONE;
    int advancedBase = startIndex + BASE_TOKENS + CSV_TARGET_COUNT + CSV_TOKEN_COUNT + CSV_REGION_COUNT;

    if (tokens.size() > advancedBase) {
      int evidenceIndex = advancedBase;
      if (tokens.size() > evidenceIndex) {
        String evidenceStr = ((String)tokens.get(evidenceIndex)).trim().toLowerCase();
        if (!evidenceStr.equals("")) {
          try {
            evidenceLevel = Integer.parseInt(evidenceStr);
            if ((evidenceLevel < Linkage.LEVEL_NONE) || (evidenceLevel > Linkage.MAX_LEVEL)) {
              throw new IOException();
            }
          } catch (NumberFormatException nfe) {
            throw new IOException();
          }
        } 
      }
      
      //
      // We overload this field so that a double value == variable
      //
      /*
      int activityIndex = advancedBase + 1;
      if (tokens.size() > activityIndex) {
        String origActivityStr = ((String)tokens.get(activityIndex)).trim().toLowerCase();
        if (!origActivityStr.equals("")) {
          try {
            activityLevel = new Double(origActivityStr);
            double doubVal = activityLevel.doubleValue();
            if ((doubVal < 0.0) || (doubVal > 1.0)) {
              throw new IOException(); 
            }
            activityType = LinkageInstance.VARIABLE;
          } catch (NumberFormatException nfe) {
            try {
              activityType = LinkageInstance.mapActivityTypeTag(origActivityStr);
            } catch (IllegalArgumentException iae) {
              throw new IOException();
            }
            if (activityType == LinkageInstance.VARIABLE) {
              throw new IOException();
            }
          }     
        }
      } */
    }
    return (new GeneralBuildInstruction(id, sourceType, sourceName, linkSign, targType, targName,
                                        evidenceLevel));
       
  } 
}
