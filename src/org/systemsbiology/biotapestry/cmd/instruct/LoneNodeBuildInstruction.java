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
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.HashSet;
import java.text.MessageFormat;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.util.ResourceManager;

/***************************************************************************
**
** Used to build networks from a dialog description
*/
  
public class LoneNodeBuildInstruction extends BuildInstruction {
    
  public static final String CSV_TAG      = "nodeOnly";
  public static final int CSV_TOKEN_COUNT = 0;
  public static final int CSV_TARGET_COUNT = 0;
  public static final int CSV_REGION_COUNT = 1;
  
  protected static final String XML_TAG  = "loneNodeBuildInstruction";

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  **
  */
  
  public LoneNodeBuildInstruction(String id, int nodeType, String nodeName) {
    super(id, nodeType, nodeName, Node.NO_NODE_TYPE, null);
  }
  
  /***************************************************************************
  **
  ** Copy Constructor
  **
  */
  
  public LoneNodeBuildInstruction(LoneNodeBuildInstruction other) {
    super(other);
  }

  /***************************************************************************
  **
  ** Clone
  */

  public LoneNodeBuildInstruction clone() {
    return ((LoneNodeBuildInstruction)super.clone());
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
    return (retval);
  }  

  /***************************************************************************
  **
  ** Fill in node lists and types
  */
  
  public void addMotifs(List<DialogBuiltMotifPair> motifs, Map<String, Integer> typeTracker) {
    typeCheckSupport(sourceName_, sourceType_, typeTracker);
    DialogBuiltLoneNodeProtoMotif dbgpm = 
      new DialogBuiltLoneNodeProtoMotif(sourceName_, sourceType_);
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
    out.println(" />");
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
    buf.append("\"");
    return (buf.toString());
  }
  
  /***************************************************************************
  **
  ** Answers if this instruction builds the same thing as another (ids and regions are ignored)
  **
  */
 
  public boolean sameDefinition(BuildInstruction other) {
    if (!(other instanceof LoneNodeBuildInstruction)) {
      return (false);
    }
    if (!super.sameDefinition(other)) {
      return (false);
    }
    return (true);
  } 
  
  /***************************************************************************
  **
  ** Standard toString:
  **
  */
  
  public String toString() { 
    return (super.toString());
  }
  
  /***************************************************************************
  **
  ** For UI dialogs:
  **
  */
    
  public String displayStringUI(ResourceManager rMan) {   
    String dispSrc = rMan.getString("buildInstruction." + BuildInstruction.mapToNodeTypeTag(sourceType_));   
    String retval = MessageFormat.format(rMan.getString("buildInstruction.loneNodeDisplay"),    
                                         new Object[] {dispSrc, sourceName_});    
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
    if (!(other instanceof LoneNodeBuildInstruction)) {
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
                                              String sourceName) throws IOException {
    if (!elemName.equals(XML_TAG)) {
      return (null);
    }
       
    return (new LoneNodeBuildInstruction(id, sourceType, sourceName));
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
                                              String sourceName) throws IOException {

    if (tokens.size() < (startIndex + BASE_TOKENS + CSV_TARGET_COUNT + CSV_TOKEN_COUNT)) {
      throw new IOException();
    }                                                
    
    return (new LoneNodeBuildInstruction(id, sourceType, sourceName));
  } 
}
