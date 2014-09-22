/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.ui;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.NodeInstance;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.Indenter;

/****************************************************************************
**
** A class directing layout propagation
*/

public class LayoutDataSource implements Cloneable {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
     
  private String modelID_;
  private String groupID_;
  private HashSet<String> nodeIDs_;
  private int xOffset_;
  private int yOffset_;
  private int xScale_; // percent expansion...
  private int yScale_;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** UI-based construction
  */   
  
  public LayoutDataSource(String modelID, String groupID) {
    modelID_ = modelID; 
    groupID_ = groupID;
    xOffset_ = 0;
    yOffset_ = 0;      
    xScale_ = 0;
    yScale_ = 0;
    nodeIDs_ = new HashSet<String>();
  }

  /***************************************************************************
  **
  ** UI-based construction
  */   
  
  public LayoutDataSource(String modelID, String groupID,
                          int xOffset, int yOffset, 
                          int xScale, int yScale) {
    modelID_ = modelID; 
    groupID_ = groupID;
    xOffset_ = xOffset;
    yOffset_ = yOffset;      
    xScale_ = xScale;
    yScale_ = yScale;
    nodeIDs_ = new HashSet<String>();
  }

  /***************************************************************************
  **
  ** IO-based construction
  */   
  
  public LayoutDataSource(String modelID, String groupID, 
                          String xOffsetStr, String yOffsetStr, 
                          String xScaleStr, String yScaleStr) throws IOException {
   
    modelID_ = modelID; 
    groupID_ = groupID;    
   
    try {
      xOffset_ = (xOffsetStr == null) ? 0 : Integer.parseInt(xOffsetStr);
      yOffset_ = (yOffsetStr == null) ? 0 : Integer.parseInt(yOffsetStr);      
      xScale_ = (xScaleStr == null) ? 0 : Integer.parseInt(xScaleStr);
      yScale_ = (yScaleStr == null) ? 0 : Integer.parseInt(yScaleStr);   
    } catch (NumberFormatException nfe) {
      throw new IOException();
    }
    
    nodeIDs_ = new HashSet<String>();
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////        

  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public LayoutDataSource clone() {
    try {
      LayoutDataSource retval = (LayoutDataSource)super.clone();
      retval.nodeIDs_ = new HashSet<String>(this.nodeIDs_);
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }  

  /***************************************************************************
  **
  ** Get a string to display.  Make this dynamic, so we don't have to
  ** depend on node names over the long haul
  */ 
  
  public String getDisplayString(GenomeSource gSrc) {
       
    Genome rootGenome = gSrc.getGenome();
    GenomeInstance gi = (GenomeInstance)gSrc.getGenome(modelID_);
    Group newGrp = gi.getGroup(groupID_);
    String baseDisplayString = gi.getName() + ": " + newGrp.getInheritedDisplayName(gi);  
    if (nodeIDs_.isEmpty()) {
      return (baseDisplayString);
    }
    
    ArrayList<String> nodeNames = new ArrayList<String>();
    Iterator<String> nidit = nodeIDs_.iterator();
    while (nidit.hasNext()) {
      String nodeID = nidit.next();
      NodeInstance node = (NodeInstance)gi.getNode(nodeID);
      Node rootNode = (Node)node.getBacking();
      String display = rootNode.getDisplayString(rootGenome, false);
      nodeNames.add(display);
    }
    int numNodes = nodeNames.size();
    Collections.sort(nodeNames);
    StringBuffer buf = new StringBuffer();
    buf.append(baseDisplayString);
    buf.append(":[");
    for (int i = 0; i < numNodes; i++) {
      String nodeName = nodeNames.get(i);
      buf.append(nodeName);
      if (i != (numNodes - 1)) {
        buf.append(", ");
      }
    }    
    buf.append("]");
    return (buf.toString());
  }

  /***************************************************************************
  **
  ** Standard hash code
  */   
  
  @Override
  public int hashCode() {
    int retval = modelID_.hashCode() + groupID_.hashCode() + xOffset_ + yOffset_ + xScale_ + yScale_; 
    if (nodeIDs_.isEmpty()) {
      return (retval);
    }
    retval += nodeIDs_.hashCode();
    return (retval);    
  }

  /***************************************************************************
  **
  ** Standard equals
  */   
  
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return (true);
    }    
    if (other == null) {
      return (false);
    }
    if (!(other instanceof LayoutDataSource)) {
      return (false);
    }

    LayoutDataSource otherLDS = (LayoutDataSource)other;

    if ((this.xOffset_ != otherLDS.xOffset_) ||
        (this.yOffset_ != otherLDS.yOffset_) ||      
        (this.xScale_ != otherLDS.xScale_) ||
        (this.yScale_ != otherLDS.yScale_)) {
      return (false);
    }   

    if (!this.modelID_.equals(otherLDS.modelID_)) {
      return (false);
    }

    if (!this.groupID_.equals(otherLDS.groupID_)) {
      return (false);
    }
    
    return (this.nodeIDs_.equals(otherLDS.nodeIDs_));

  }
  
  /***************************************************************************
  **
  ** Answers if we are overlay compatable with another directive
  */   
  
  public boolean overlayCompatable(LayoutDataSource other) {
    if (this == other) {
      return (true);
    }    
    if (other == null) {
      return (false);
    }
    
    if (!this.modelID_.equals(other.modelID_)) {
      return (false);
    }
    
    //
    // With no scaling, we are OK if offsets match:
    //
    if ((this.xScale_ == 0) && (other.xScale_ == 0) && 
        (this.yScale_ == 0) && (other.yScale_ == 0)) {
      return ((this.xOffset_ == other.xOffset_) && (this.yOffset_ == other.yOffset_));  
    }
    
    //
    // Scaling.  We had better have identical scaling and identical node sets:
    //
      
    if ((this.xScale_ != other.xScale_) || (this.yScale_ != other.yScale_)) {
      return (false);
    }   
    return (this.nodeIDs_.equals(other.nodeIDs_));
  } 
  
    /***************************************************************************
  **
  ** Answers if we are scaled
  */   
  
  public boolean isScaled() {
    return ((xScale_ != 0) || (yScale_ != 0));
  }

  /***************************************************************************
  **
  ** Get the model ID
  **
  */
  
  public String getModelID() {
    return (modelID_);
  }
  
  /***************************************************************************
  **
  ** Get the group iD
  **
  */
  
  public String getGroupID() {
    return (groupID_);
  }
  
  /***************************************************************************
  **
  ** Add a node
  **
  */
  
  public void addNode(String nodeID) {
    nodeIDs_.add(nodeID);
    return;
  }

  /***************************************************************************
  **
  ** Answer if we are node specific
  **
  */
  
  public boolean isNodeSpecific() {
    return (!nodeIDs_.isEmpty());
  }   
  
  /***************************************************************************
  **
  ** Drop all the nodes
  **
  */
  
  public void dropAllNodes() {
    nodeIDs_.clear();
    return;
  } 
  
  /***************************************************************************
  **
  ** Get the node iterator
  **
  */
  
  public Iterator<String> getNodes() {
    return (nodeIDs_.iterator());
  } 
  
  /***************************************************************************
  **
  ** Get the X offset
  **
  */
  
  public int getXOffset() {
    return (xOffset_);
  }
    
  /***************************************************************************
  **
  ** Get the Y offset
  **
  */
  
  public int getYOffset() {
    return (yOffset_);
  }
  
  /***************************************************************************
  **
  ** Get the X scaling (percent 0 - 100)
  **
  */
  
  public int getXScale() {
    return (xScale_);
  }
  
  /***************************************************************************
  **
  ** Get the Y scaling (percent 0 - 100)
  **
  */
  
  public int getYScale() {
    return (yScale_);
  }   
  
  /***************************************************************************
  **
  ** Set the X offset
  **
  */
  
  public void setXOffset(int xOffset) {
    xOffset_ = xOffset;
    return;
  }
    
  /***************************************************************************
  **
  ** Set the Y offset
  **
  */
  
  public void setYOffset(int yOffset) {
    yOffset_ = yOffset;
    return;
  }
  
  /***************************************************************************
  **
  ** Set the X scaling (percent 0 - 100)
  **
  */
  
  public void setXScale(int xScale) {
    xScale_ = xScale;
    return;
  }
  
  /***************************************************************************
  **
  ** Set the Y scaling (percent 0 - 100)
  **
  */
  
  public void setYScale(int yScale) {
    yScale_ = yScale;
    return;
  }
 
  /***************************************************************************
  **
  ** Answer if we have a model dependency
  **
  */
  
  public boolean haveModelDependency(String modelID) {
    return (modelID_.equals(modelID));
  }   
  
  /***************************************************************************
  **
  ** Answer if we have a group dependency
  **
  */
  
  public boolean haveGroupDependency(String modelID, String groupID) {
    if (!modelID_.equals(modelID)) {
      return (false);
    }
    return (groupID_.equals(groupID));
  }   
  
  /***************************************************************************
  **
  ** Answer if we have a node dependency
  **
  */
  
  public boolean haveNodeDependency(String modelID, String nodeID) {
    if (!modelID_.equals(modelID)) {
      return (false);
    }
    // Don't need to know group ID to answer the question...
    
    return (nodeIDs_.contains(nodeID));
  }     
  
  /***************************************************************************
  **
  ** Remove any dependencies.  If this means we lose all node dependencies,
  ** return true so we are deleted from the list
  **
  */
  
  public boolean removeNodeDependency(String modelID, String nodeID) {
    if (!modelID_.equals(modelID)) {
      return (false);
    }
    // Don't need to know group ID to answer the question...
    
    if (nodeIDs_.contains(nodeID)) {
      nodeIDs_.remove(nodeID);
      return (nodeIDs_.isEmpty());
    } else {
      return (false);
    }
  }
  
  /***************************************************************************
  **
  ** Ignore transforms when checking equal.
  */   
  
  public boolean equalsExceptTransforms(LayoutDataSource otherLDS) {
    if (this == otherLDS) {
      return (true);
    }    
    if (otherLDS == null) {
      return (false);
    }

    if (!this.modelID_.equals(otherLDS.modelID_)) {
      return (false);
    }

    if (!this.groupID_.equals(otherLDS.groupID_)) {
      return (false);
    }
    
    return (this.nodeIDs_.equals(otherLDS.nodeIDs_));
  }  

  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {

    ind.indent();
    out.print("<layoutDataSource");
    out.print(" model=\"");
    out.print(modelID_);    
    out.print("\" group=\"");
    out.print(groupID_);
    if (xOffset_ != 0) {
      out.print("\" xOff=\"");
      out.print(xOffset_);
    }
    if (yOffset_ != 0) {
      out.print("\" yOff=\"");
      out.print(yOffset_);
    }
    if (xScale_ != 0) {
      out.print("\" xScale=\"");
      out.print(xScale_);
    }
    if (yScale_ != 0) {
      out.print("\" yScale=\"");
      out.print(yScale_);    
    }
    if (nodeIDs_.isEmpty()) {
      out.println("\" />");
      return;
    } else {
      out.println("\" >");
    }
    
    ind.up().indent();
    out.println("<ldsNodes>");
    ind.up();
    TreeSet<String> sorted = new TreeSet<String>(nodeIDs_);
    Iterator<String> nidit = sorted.iterator();
    while (nidit.hasNext()) {
      String nID = nidit.next();
      ind.indent();
      out.print("<ldsNode");
      out.print(" nodeID=\"");
      out.print(nID);    
      out.println("\" />");
    }
    ind.down().indent();
    out.println("</ldsNodes>");
    ind.down().indent();
    out.println("</layoutDataSource>");
    return;
  } 
  
  /***************************************************************************
  **
  ** Return the element keyword that we are interested in
  **
  */
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add("layoutDataSource");
    retval.add("ldsNodes");
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Return the nodeID keyword
  **
  */
  
  public static String nodeIDKeyword() {
    return ("ldsNode");
  }  
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static String extractNodeID(String elemName, 
                                     Attributes attrs) throws IOException {
    return (AttributeExtractor.extractAttribute(elemName, attrs, "ldsNode", "nodeID", true));
  }  
   
  /***************************************************************************
  **
  ** Handle creation from XML
  **
  */
  
  public static LayoutDataSource buildFromXML(String elemName, Attributes attrs) throws IOException {
    
    if (!elemName.equals("layoutDataSource")) {
      return (null);
    }
 
    String modelID = null;
    String groupID = null; 
    String xOffsetStr = null; 
    String yOffsetStr = null; 
    String xScaleStr = null;
    String yScaleStr = null;

    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("model")) {
          modelID = val;
        } else if (key.equals("group")) {
          groupID = val;
        } else if (key.equals("xOff")) {
          xOffsetStr = val;
        } else if (key.equals("yOff")) {
          yOffsetStr = val;
        } else if (key.equals("xScale")) {
          xScaleStr = val;
        } else if (key.equals("yScale")) {
          yScaleStr = val;   
        }   
      }
    }
    
    if ((modelID == null) || (groupID == null)) {
      throw new IOException();
    }
    
    LayoutDataSource lds = new LayoutDataSource(modelID, groupID, xOffsetStr, yOffsetStr, xScaleStr, yScaleStr);
    return (lds);
  }  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
}
