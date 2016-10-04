/*
**    Copyright (C) 2003-2016 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.genome;

import java.io.PrintWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;

/****************************************************************************
**
** This represents a gene instance in BioTapestry
*/

public class GeneInstance extends NodeInstance implements Gene {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private static final String GENE_TAG_ = "geneInstance";
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Copy Constructor
  */

  public GeneInstance(GeneInstance other) {
    super(other);
  } 
  
  /***************************************************************************
  **
  ** Copy Constructor with instance change
  */

  public GeneInstance(GeneInstance other, int instance) {
    super(other, instance);
  }   
  
  /***************************************************************************
  **
  ** Downcast
  */

  public GeneInstance(NodeInstance node) {
    super(node);
    nodeType_ = GENE;
  }
  
 
  /***************************************************************************
  **
  ** For UI-based creation
  */

  public GeneInstance(BTState appState, DBGenomeItem backing, int instance, Double activityLevel, int activity) {
    super(appState, backing, GENE, instance, activityLevel, activity);
  }  
  
  /***************************************************************************
  **
  ** For xml-based creation
  */

  public GeneInstance(BTState appState, DBGenomeItem backing, String instance, String activityLevel, 
                      String activityType, String override) throws IOException {      
    super(appState, backing, null, instance, activityLevel, activityType, override);
    nodeType_ = GENE;
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

  public GeneInstance clone() {
    return ((GeneInstance)super.clone());
  }  
    
  /***************************************************************************
  **
  ** Get the Backing item (maybe make this more abstract?)
  ** 
  */
  
  public DBGenomeItem getBacking() {
    GenomeSource gSrc = (altSrc_ == null) ? appState_.getDB() : altSrc_;
    DBGenomeItem retval = (DBGenomeItem)gSrc.getGenome().getGene(myItemID_);
    if (retval == null) {
      throw new IllegalStateException();
      // May just be held in temporary holding storage:
      //retval = (DBGenomeItem)appState_.getDB().getHoldingGenome().getGene(myItemID_); 
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return ("GeneInstance: " + " backing = " + getBacking() +
            " instance = " + instanceID_ + " activity = " + activityLevel_ + " activity = " + activity_+
            " override = " + nameOverride_);
  }
  
  /***************************************************************************
  **
  ** Get display string.  Override to pay attention to type preface string
  */

  public String getDisplayString(Genome genome, boolean typePreface) {
    ResourceManager rMan = appState_.getRMan();
    GenomeInstance gi = (GenomeInstance)genome;
    GroupMembership member = gi.getNodeGroupMembership(this);
    String groupID = (member.mainGroups.isEmpty()) ? null : (String)member.mainGroups.iterator().next();
    String groupName = (groupID == null) ? "" : gi.getGroup(groupID).getInheritedDisplayName(gi);
    String retval;
    if (typePreface) { 
      String format = rMan.getString("ncreate.importFormatForInstance");
      String typeDisplay = DBNode.mapTypeToDisplay(rMan, nodeType_);
      retval = MessageFormat.format(format, new Object[] {typeDisplay, getName(), groupName});
    } else {
      String format = rMan.getString("ncreate.importFormatForInstanceNoType");
      retval = MessageFormat.format(format, new Object[] {getName(), groupName});
    }
    return (retval);
  }  
 
  /***************************************************************************
  **
  ** Get a region iterator
  */
  
  public Iterator<DBGeneRegion> regionIterator() {
    return (((DBGene)getBacking()).regionIterator());
  }
  
  /***************************************************************************
  **
  ** Get a region count
  */
  
  public int getNumRegions() {
    return (((DBGene)getBacking()).getNumRegions());
  }
  
  /***************************************************************************
  **
  ** Get the region holding a pad. May be null:
  */
  
  public DBGeneRegion getRegionForPad(int padNum){
    return (((DBGene)getBacking()).getRegionForPad(padNum));
  }
 
  /***************************************************************************
  **
  ** Get the experimental verification level
  */
  
  public int getEvidenceLevel() {
    return (((DBGene)getBacking()).getEvidenceLevel());
  }
  
  /***************************************************************************
  **
  ** Get the pad count
  */
  
  public int getPadCount() {
    return (((DBGene)getBacking()).getPadCount());
  }  
  
  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.print("<");
    out.print(GENE_TAG_);
    out.print(" ref=\"");
    out.print(getBacking().getID());
    out.print("\" instance=\"");
    out.print(instanceID_);
    if (activity_ != ACTIVE) {
      out.print("\" activity=\"");
      out.print(mapActivityTypes(activity_));
    }
    if (activityLevel_ != null) {
      out.print("\" activityLevel=\"");
      out.print(activityLevel_);
    }
    if (nameOverride_ != null) {
      out.print("\" name=\"");
      out.print(CharacterEntityMapper.mapEntities(nameOverride_, false));
    }
    
    if (!urls_.isEmpty() || (description_ != null)) {
      out.println("\" >");
      (new CommonGenomeItemCode()).writeDescUrlToXML(out, ind, urls_, description_, "nodeInstance");
      ind.indent(); 
      out.print("</");
      out.print(GENE_TAG_);
      out.println(">");
    } else {
      out.println("\" />");
    }
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Handle Gene Instance creation
  **
  */
  
  public static GeneInstance buildFromXML(BTState appState, DBGenome rootGenome, Genome genome,
                                          Attributes attrs) throws IOException {
    String ref = null;
    String instance = null;
    String activity = null;  
    String activityLevelStr = null;
     
    String name = null;
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("ref")) {
          ref = val;
        } else if (key.equals("instance")) {
          instance = val;
        } else if (key.equals("activity")) {
          activity = val;
        } else if (key.equals("name")) {
          name = CharacterEntityMapper.unmapEntities(val, false);
        } else if (key.equals("activityLevel")) {
          activityLevelStr = val;
        } 
      }
    }    

    DBGene backing = (DBGene)rootGenome.getGene(ref);
    return (new GeneInstance(appState, backing, instance, activityLevelStr, activity, name));
  }  

  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add(GENE_TAG_);
    return (retval);
  }
}
