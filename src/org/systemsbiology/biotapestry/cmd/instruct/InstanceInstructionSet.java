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

import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Iterator;
import java.io.PrintWriter;
import java.io.IOException;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;

/****************************************************************************
**
** This holds a set of build instructions for a genome instance
*/

public class InstanceInstructionSet implements Cloneable {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private ArrayList<BuildInstructionInstance> instruct_;
  private ArrayList<RegionInfo> regions_;
  private String instanceID_;
  
  private static final String XML_TAG = "instanceInstructionSet";

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public InstanceInstructionSet(String instanceID) {
    instruct_ = new ArrayList<BuildInstructionInstance>();
    regions_ = new ArrayList<RegionInfo>();
    instanceID_ = instanceID;
  }
  
  /***************************************************************************
  **
  ** Copy Constructor
  */

  public InstanceInstructionSet(InstanceInstructionSet other) {
    copyGuts(other);
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
  public InstanceInstructionSet clone() {
    try {
      InstanceInstructionSet retval = (InstanceInstructionSet)super.clone();
      retval.copyGuts(this);
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }

  /***************************************************************************
  **
  ** Copy guts
  */

  private void copyGuts(InstanceInstructionSet other) {
    this.instruct_ = new ArrayList<BuildInstructionInstance>();
    int size = other.instruct_.size();
    for (int i = 0; i < size; i++) {
      BuildInstructionInstance otherBii = other.instruct_.get(i);
      this.instruct_.add(otherBii.clone());  
    }
    this.regions_ = new ArrayList<RegionInfo>();
    int rSize = other.regions_.size();
    for (int i = 0; i < rSize; i++) {
      RegionInfo otherRI = other.regions_.get(i);
      this.regions_.add(otherRI.clone());
    }
    this.instanceID_ = other.instanceID_;
  }

  /***************************************************************************
  **
  ** Get the id of the corresponding instance
  */
  
  public String getInstanceID() {
    return (instanceID_);
  }
  
  /***************************************************************************
  **
  ** Add an instruction
  **
  */
  
  public void addInstruction(BuildInstructionInstance inst) {
    instruct_.add(inst);
    return;
  }
  
  /***************************************************************************
  **
  ** Delete an instruction.  Does nothing if not present
  **
  */
  
  public void deleteInstruction(BuildInstructionInstance inst) {
    int size = instruct_.size();
    for (int i = 0; i < size; i++) {
      BuildInstructionInstance bii = instruct_.get(i);
      if (bii.equals(inst)) {
        instruct_.remove(i);
        return;
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Delete all instructions matching the core ID
  **
  */
  
  public void deleteInstructions(String id) {
    int size = instruct_.size();
    int count = 0;
    while (count < size) {
      BuildInstructionInstance bii = instruct_.get(count);
      if (bii.getBaseID().equals(id)) {
        instruct_.remove(count);
        size--;
      } else {
        count++;
      }
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Delete all instructions
  **
  */
  
  public void deleteAllInstructions() {
    instruct_.clear();
    return;
  }   
  
  /***************************************************************************
  **
  ** Get an Iterator over the instructions:
  **
  */
  
  public int getInstructionCount() {
    return (instruct_.size());
  }
  
  /***************************************************************************
  **
  ** Get an Iterator over the instructions:
  **
  */
  
  public Iterator<BuildInstructionInstance> getInstructionIterator() {
    return (instruct_.iterator());
  }
  
  /***************************************************************************
  **
  ** Add a region
  **
  */
  
  public void addRegion(String name, String abbrev) {
    regions_.add(new RegionInfo(name, abbrev));
    return;
  }
  
  /***************************************************************************
  **
  ** Delete a region.  Does nothing if not present.
  **
  */
  
  public void deleteRegion(String abbrev) {
    int size = regions_.size();
    for (int i = 0; i < size; i++) {
      RegionInfo ri = regions_.get(i);
      if (ri.abbrev.equals(abbrev)) {
        regions_.remove(i);
        return;
      }
    }
    return;
  } 

  /***************************************************************************
  **
  ** Add a region
  **
  */
  
  public void addRegion(RegionInfo newRI) {
    regions_.add(new RegionInfo(newRI));
    return;
  }  
  
  /***************************************************************************
  **
  ** Replace the list of regions with the given list
  **
  */
  
  public void replaceRegions(List<RegionInfo> newRegions) {
    this.regions_ = new ArrayList<RegionInfo>();
    int size = newRegions.size();
    for (int i = 0; i < size; i++) {
      RegionInfo newRI = newRegions.get(i);
      this.regions_.add(new RegionInfo(newRI));  
    }
    return;
  }  

  /***************************************************************************
  **
  ** Get a count of regions:
  **
  */
  
  public int getRegionCount() {
    return (regions_.size());
  }  
  
  /***************************************************************************
  **
  ** Get an Iterator over the regions:
  **
  */
  
  public Iterator<RegionInfo> getRegionIterator() {
    return (regions_.iterator());
  }
  
  /***************************************************************************
  **
  ** Get the region  for the abbreviation
  **
  */
  
  public RegionInfo getRegionForAbbreviation(String abbrev) {
    int size = regions_.size();
    for (int i = 0; i < size; i++) {
      RegionInfo ri = regions_.get(i);
      if (ri.abbrev.equalsIgnoreCase(abbrev)) {
        return (ri);
      }
    }    
    return (null);
  }  
  
  /***************************************************************************
  **
  ** Write the instruction set to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.print("<");
    out.print(XML_TAG);    
    out.print(" forInstance=\"");
    out.print(instanceID_);
    out.println("\" >");
    ind.up();
    writeRegions(out, ind);
    writeInstructions(out, ind);
    ind.down().indent();       
    out.print("</");
    out.print(XML_TAG);
    out.println(">");
    return;
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
  ** Return the region keyword
  **
  */
  
  public static String regionKeyword() {
    return ("instructionRegion");
  } 
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static InstanceInstructionSet buildFromXML(String elemName, 
                                                    Attributes attrs) throws IOException {
    if (!elemName.equals(XML_TAG)) {
      return (null);
    }
    
    String instanceID = null;

    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("forInstance")) {
          instanceID = val;
        }
      }
    }
    
    if (instanceID == null) {
      throw new IOException();
    }
    
    return (new InstanceInstructionSet(instanceID));
  } 
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static RegionInfo extractRegion(String elemName, 
                                         Attributes attrs) throws IOException {
    String regionName = AttributeExtractor.extractAttribute(
                         elemName, attrs, "instructionRegion", "id", true);
    regionName = CharacterEntityMapper.unmapEntities(regionName, false);
    String regionAbbrev = AttributeExtractor.extractAttribute(
                           elemName, attrs, "instructionRegion", "abbrev", true);
    regionAbbrev = CharacterEntityMapper.unmapEntities(regionAbbrev, false);
    return (new RegionInfo(regionName, regionAbbrev));
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Used for each region
  */
  
  public static class RegionInfo implements Cloneable {
    public String name;
    public String abbrev;
    
    public RegionInfo(String name, String abbrev) {
      this.name = name;
      this.abbrev = abbrev;
    }
    
    public RegionInfo(RegionInfo other) {
      this.name = other.name;
      this.abbrev = other.abbrev;
    }

    /***************************************************************************
    **
    ** Clone
    */
  
    @Override
    public RegionInfo clone() {
      try {
        RegionInfo retval = (RegionInfo)super.clone();
        return (retval);
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
    }

    @Override
    public int hashCode() {
      return (name.hashCode() + abbrev.hashCode());
    }    

    @Override
    public boolean equals(Object other) {
      if (other == this) {
        return (true);
      }
      if (!(other instanceof RegionInfo)) {
        return (false);
      }
      RegionInfo otherRI = (RegionInfo)other;
      return ((otherRI.name.equals(this.name)) &&
              (otherRI.abbrev.equals(this.abbrev)));
    }
    
    // consistent: If either field matches, both must match.
    public boolean consistent(RegionInfo other) {
      boolean equalNames = DataUtil.keysEqual(other.name, this.name);
      boolean equalAbbrev = DataUtil.keysEqual(other.abbrev, this.abbrev);
      
      return ((equalNames && equalAbbrev) || (!equalNames && !equalAbbrev));
    }
    
    public String toString() {
      return (name + " (" + abbrev + ")"); 
    }

  }   
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Write the regions to XML
  **
  */
  
  private void writeRegions(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.println("<regionTags>");
    Iterator<RegionInfo> regions = getRegionIterator();
    ind.up();    
    while (regions.hasNext()) {
      ind.indent();
      RegionInfo region = regions.next();
      out.print("<instructionRegion");    
      out.print(" id=\"");
      out.print(CharacterEntityMapper.mapEntities(region.name, false));
      out.print("\" abbrev=\"");
      out.print(CharacterEntityMapper.mapEntities(region.abbrev, false));      
      out.println("\" />");
    }
    ind.down().indent(); 
    out.println("</regionTags>");
    return;
  }
  
  /***************************************************************************
  **
  ** Write the instructions to XML
  **
  */
  
  private void writeInstructions(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.println("<instructionInstances>");
    Iterator<BuildInstructionInstance> iit = getInstructionIterator();

    ind.up();
    while (iit.hasNext()) {
      BuildInstructionInstance bii = iit.next();
      bii.writeXML(out, ind);
    }
    
    ind.down().indent(); 
    out.println("</instructionInstances>");
    return;
  }
}
