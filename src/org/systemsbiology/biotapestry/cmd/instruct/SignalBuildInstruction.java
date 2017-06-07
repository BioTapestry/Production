/*
**    Copyright (C) 2003-2015 Institute for Systems Biology 
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
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.ResourceManager;


/***************************************************************************
**
** Used to build networks from a dialog description
*/
  
public class SignalBuildInstruction extends BuildInstruction {

  public static final int PROMOTE_SIGNAL   = 0;
  public static final int REPRESS_SIGNAL   = 1;
  public static final int SWITCH_SIGNAL    = 2;
  public static final int NUM_SIGNAL_TYPES = 3;
  
  public static final String PROMOTE_SIGNAL_STR = "promoteSig";
  public static final String REPRESS_SIGNAL_STR = "repressSig";
  public static final String SWITCH_SIGNAL_STR  = "switchSig";

  public static final String CSV_TAG      = "signal";
  public static final int CSV_TOKEN_COUNT = 3;
  public static final int CSV_TARGET_COUNT = 2;
  public static final int CSV_REGION_COUNT = 2;  
  
  protected static final String XML_TAG  = "signalBuildInstruction";

  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private int signalFactorType_;
  private String signalFactorName_;
  private int signalType_;
  
  /***************************************************************************
  **
  ** Constructor
  **
  */
  
  public SignalBuildInstruction(String id, int sourceType, String sourceName, 
                                int targType, String targName, 
                                int signalFactorType, String signalFactorName, 
                                int signalType) {
    super(id, sourceType, sourceName, targType, targName);
    this.signalFactorType_ = signalFactorType;
    this.signalFactorName_ = signalFactorName;
    this.signalType_ = signalType;
  }
  
  /***************************************************************************
  **
  ** Copy Constructor
  **
  */
  
  public SignalBuildInstruction(SignalBuildInstruction other) {
    super(other);
    this.signalFactorType_ = other.signalFactorType_;
    this.signalFactorName_ = other.signalFactorName_;
    this.signalType_ = other.signalType_; 
  }
  
  /***************************************************************************
  **
  ** Clone
  */

  public SignalBuildInstruction clone() {
    return ((SignalBuildInstruction)super.clone());
  }
  
  /***************************************************************************
  **
  ** Get the Signal Factor Type
  */
  
  public int getSignalFactorType() {
    return (signalFactorType_);
  }
  
  /***************************************************************************
  **
  ** Set the Signal Factor Type
  */
  
  public void setSignalFactorType(int signalFactorType) {
    signalFactorType_ = signalFactorType;
    return;
  } 
 
  /***************************************************************************
  **
  ** Get the Signal Factor Name
  */
  
  public String getSignalFactorName() {
    return (signalFactorName_);
  }
  
  /***************************************************************************
  **
  ** Set the Signal Factor Name
  */
  
  public void setSignalFactorName(String signalFactorName) {
    signalFactorName_ = signalFactorName;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the Signal Type
  */
  
  public int getSignalType() {
    return (signalType_);
  }
  
  /***************************************************************************
  **
  ** Set the Signal Type
  */
  
  public void setSignalType(int signalType) {
    signalType_ = signalType;
    return;
  }

  /***************************************************************************
  **
  ** Answer if we have an evidence level
  */
  
  public boolean hasEvidenceLevel() {
    return (false);
  }   
  
  /***************************************************************************
  **
  ** Get the names of all contained named nodes
  */
  
  public Set<String> getNamedNodes() {
    HashSet<String> retval = new HashSet<String>();
    retval.add(sourceName_);
    retval.add(targName_);
    retval.add(signalFactorName_);    
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
    retval.add(signalFactorName_);    
    return (retval);
  }  

  /***************************************************************************
  **
  ** Fill in node lists and types
  */
  
  public void addMotifs(List<DialogBuiltMotifPair> motifs, Map<String, Integer> types) {  
    typeCheckSupport(sourceName_, sourceType_, types);
    typeCheckSupport(targName_, targType_, types);    
    typeCheckSupport(signalFactorName_, signalFactorType_, types);
    DialogBuiltSignalProtoMotif dbspm =
      new DialogBuiltSignalProtoMotif(sourceName_, sourceType_,
                                      targName_, targType_, 
                                      signalFactorName_, signalFactorType_,
                                      signalType_);
    motifs.add(new DialogBuiltMotifPair(id_, dbspm, null));    
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
    if (!typeChecks(signalFactorName_, signalFactorType_, typeTracker)) {
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
    out.print(" factorType=\"");
    out.print(mapToNodeTypeTag(signalFactorType_));
    out.print("\" factorName=\"");
    out.print(CharacterEntityMapper.mapEntities(signalFactorName_, false)); 
    out.print("\" signalType=\"");
    out.print(mapToSignalTypeTag(signalType_));        
    out.println("\" />");
    return;
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
    buf.append(mapToNodeTypeTag(signalFactorType_));
    buf.append("\",\"");  
    buf.append(signalFactorName_); 
    buf.append("\",\""); 
    buf.append(mapToSignalTypeTag(signalType_));
    buf.append("\"");
    return (buf.toString());
  }
  
  /***************************************************************************
  **
  ** Answers if this instruction builds the same thing as another (ids and regions are ignored)
  **
  */
 
  public boolean sameDefinition(BuildInstruction other) {
    if (!(other instanceof SignalBuildInstruction)) {
      return (false);
    }
    if (!super.sameDefinition(other)) {
      return (false);
    }
    SignalBuildInstruction otherSbi = (SignalBuildInstruction)other;
    return ((this.signalFactorType_ == otherSbi.signalFactorType_) &&
            (this.signalType_ == otherSbi.signalType_) &&
            (((this.signalFactorName_ == null) && (otherSbi.signalFactorName_ == null)) ||
             ((this.signalFactorName_ != null) && (otherSbi.signalFactorName_ != null) 
              && DataUtil.keysEqual(this.signalFactorName_, otherSbi.signalFactorName_))));
  }
  
  /***************************************************************************
  **
  ** Standard toString:
  **
  */
  
  public String toString() {
    return (super.toString() + " " + signalFactorType_ + " " + signalFactorName_ + " " + signalType_);
  }

  /***************************************************************************
  **
  ** For UI dialogs:
  ** 
  */
  
  public String displayStringUI(ResourceManager rMan) {
    String dispSign = rMan.getString("buildInstruction." + SignalBuildInstruction.mapToSignalTypeTag(signalType_));
    String retval = MessageFormat.format(rMan.getString("buildInstruction.signalDisplay"),    
                                         new Object[] {sourceName_, signalFactorName_,  targName_, dispSign});    
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
    if (!(other instanceof SignalBuildInstruction)) {
      return (false);
    }
    SignalBuildInstruction otherSbi = (SignalBuildInstruction)other;
    return ((this.signalFactorType_ == otherSbi.signalFactorType_) &&
            (this.signalType_ == otherSbi.signalType_) &&
            (((this.signalFactorName_ == null) && (otherSbi.signalFactorName_ == null)) ||
             ((this.signalFactorName_ != null) && this.signalFactorName_.equals(otherSbi.signalFactorName_))));    
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
    
    String signalFactorName = null;
    String signalFactorTypeStr = null;
    String signalTypeStr = null;    
    
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("factorType")) {
          signalFactorTypeStr = val;
        } else if (key.equals("factorName")) {
          signalFactorName = CharacterEntityMapper.unmapEntities(val, false);
        } else if (key.equals("signalType")) {
          signalTypeStr = val;
        }
      }
    }
    
    if ((signalFactorTypeStr == null) || (signalFactorName == null) || (signalTypeStr == null)) {
      throw new IOException();
    }
    
    int signalFactorType;
    int signalType;   
    
    try {
      signalFactorType = mapFromNodeTypeTag(signalFactorTypeStr); 
      signalType = mapFromSignalTypeTag(signalTypeStr);
    } catch (IllegalArgumentException iae) {
      System.err.println("bad map");
      throw new IOException();
    }
    
    return (new SignalBuildInstruction(id, sourceType, sourceName,
                                       targType, targName, signalFactorType,
                                       signalFactorName, signalType));
  }


  /***************************************************************************
  **
  ** Map signal types
  */
  
  public static String mapToSignalTypeTag(int val) {
    switch (val) {
      case PROMOTE_SIGNAL:
        return (PROMOTE_SIGNAL_STR);
      case REPRESS_SIGNAL:
        return (REPRESS_SIGNAL_STR); 
      case SWITCH_SIGNAL:
        return (SWITCH_SIGNAL_STR);         
      default:
        throw new IllegalStateException();
    }
  }

  /***************************************************************************
  **
  ** Map signal types to values
  */

  public static int mapFromSignalTypeTag(String tag) {
    if (tag.equalsIgnoreCase(PROMOTE_SIGNAL_STR)) {
      return (PROMOTE_SIGNAL);
    } else if (tag.equalsIgnoreCase(REPRESS_SIGNAL_STR)) {
      return (REPRESS_SIGNAL);
    } else if (tag.equalsIgnoreCase(SWITCH_SIGNAL_STR)) {
      return (SWITCH_SIGNAL);      
    } else {
      throw new IllegalArgumentException();
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

    String signalFactorTypeStr = tokens.get(startIndex + BASE_TOKENS + CSV_TARGET_COUNT + CSV_TOKEN_COUNT - 3).trim().toLowerCase();    
    String signalFactorName = tokens.get(startIndex + BASE_TOKENS + CSV_TARGET_COUNT + CSV_TOKEN_COUNT - 2);
    String signalTypeStr = tokens.get(startIndex + BASE_TOKENS + CSV_TARGET_COUNT + CSV_TOKEN_COUNT - 1).trim().toLowerCase();  
    
    if ((signalFactorTypeStr == null) || (signalFactorTypeStr.trim().equals(""))) {
      throw new IOException();
    }
    
    if ((signalFactorName == null) || (signalFactorName.trim().equals(""))) {
      throw new IOException();
    }
    
    if ((signalTypeStr == null) || (signalTypeStr.trim().equals(""))) {
      throw new IOException();
    } 
    
    int signalFactorType;
    int signalType;   
    
    try {
      signalFactorType = mapFromNodeTypeTag(signalFactorTypeStr); 
      signalType = mapFromSignalTypeTag(signalTypeStr);
    } catch (IllegalArgumentException iae) {
      System.err.println("bad map");
      throw new IOException();
    }
    
    return (new SignalBuildInstruction(id, sourceType, sourceName,
                                       targType, targName, signalFactorType,
                                       signalFactorName, signalType));
  }
}
