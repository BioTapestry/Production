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

package org.systemsbiology.biotapestry.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;

import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.db.GenomeSource;

/****************************************************************************
**
** A path is currently just an ordered list of links.  It should be cycle-free,
** unless it specifies a simple single link autoloop (src == trg)
*/

public class Path implements Comparable<Path> {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
   
  private ArrayList<SignedTaggedLink> links_;
  private HashMap<String, String> nameMap_;
  private int ranking_;
  private boolean simpleLoop_;
  
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
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public Path() {
    links_ = new ArrayList<SignedTaggedLink>();
    nameMap_ = new HashMap<String, String>();
    ranking_ = 0;
    simpleLoop_ = false;
  }  

  /***************************************************************************
  **
  ** Copy Constructor
  */

  public Path(Path other) {
    this.links_ = new ArrayList<SignedTaggedLink>(other.links_);
    this.nameMap_ = new HashMap<String, String>(other.nameMap_);
    this.ranking_ = other.ranking_;
    this.simpleLoop_ = other.simpleLoop_;
  }   
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Set the path ranking
  */
  
  public void setRanking(int ranking) {
    ranking_ = ranking;
    return;
  } 
    
  /***************************************************************************
  **
  ** Get the path ranking
  */
  
  public int getRanking() {
    return (ranking_);
  }
  
  /***************************************************************************
  **
  ** Return whether this is a simple loop
  */
  
  public boolean isSimpleLoop() {
    return (simpleLoop_);
  }  

  /***************************************************************************
  **
  ** Add a link without checks (use for reconstruction from IO)
  */
  
  public void addLinkNoChecks(Linkage link, GenomeSource src, String gKey) {
    links_.add(new SignedTaggedLink(link.getSource(), link.getTarget(), link.getSign(), link.getID()));
    addToNameMap(nameMap_, link, src, gKey);
    return;
  }  
  
  /***************************************************************************
  **
  ** Add a single simple loop
  */
  
  public void addSimpleLoopLink(Linkage link, GenomeSource src, String gKey) {
	  
    String srcKey = link.getSource();
    String trgKey = link.getTarget();
    
    if (!trgKey.equals(srcKey)) {
      throw new IllegalArgumentException();
    }
    if (links_.size() != 0) {
      throw new IllegalStateException();
    }
    links_.add(new SignedTaggedLink(link.getSource(), link.getTarget(), link.getSign(), link.getID()));
    addToNameMap(nameMap_, link, src, gKey);
    simpleLoop_ = true;
    return;
  }  
 
  /***************************************************************************
  **
  ** Add a link to the end of the path.  True iff successful.
  */
  
  public boolean addLink(Linkage link, GenomeSource src, String gKey) {
    //
    // Check the link to see that:
    //  1) source is the target of the previous link
    //  2) target is not a node already in the path (loop avoidance)
    //  3) Link is not a self loop
    //
    
    if (simpleLoop_) {
      throw new IllegalStateException();
    }
    
    String srcKey = link.getSource();
    String trgKey = link.getTarget(); 
    
    if (trgKey.equals(srcKey)) {
      return (false);
    }
    SignedTaggedLink newGuy = new SignedTaggedLink(link.getSource(), link.getTarget(), link.getSign(), link.getID());
    
    int size = links_.size();
    if (size == 0) {
      links_.add(newGuy);
      addToNameMap(nameMap_, link, src, gKey);
      return (true);
    }
    SignedTaggedLink lastLink = links_.get(size - 1);
    if (!lastLink.getTrg().equals(link.getSource())) {
      throw new IllegalArgumentException();
    }
    String newTarget = link.getTarget();
    for (int i = 0; i < size; i++) {
      SignedTaggedLink pathLink = links_.get(i);
      String source = pathLink.getSrc();
      String target = pathLink.getTrg();
      if (newTarget.equals(source) || newTarget.equals(target)) {
        return (false);
      }
    }
    addToNameMap(nameMap_, link, src, gKey);
    links_.add(newGuy);
    return (true);
  }
  
  /***************************************************************************
  **
  ** Get an iterator over the links
  */
  
  public Iterator<SignedTaggedLink> pathIterator() {
    return (links_.iterator());
  }  
  
  /**************************************************
   * getLinks
   * 
   * 
   * 
   */
  
  public List<SignedTaggedLink> getLinks() {
	  if(this.links_ != null) {
		  return Collections.unmodifiableList(links_);
	  }
	  return null;
  }

  /***************************************************************************
  **
  ** Get the source start
  */
  
  public String getStart() {
    int size = links_.size();
    if (size == 0) {
      return (null);
    }    
    return (links_.get(0).getSrc());
  }  
  
  /***************************************************************************
  **
  ** Get the target end
  */
  
  public String getEnd() {
    int size = links_.size();
    if (size == 0) {
      return (null);
    }
    return (links_.get(links_.size() - 1).getTrg());
  }
  
  /***************************************************************************
  **
  ** Get the depth
  */
  
  public int getDepth() {
    return (links_.size());
  }

  /***************************************************************************
  **
  ** Remove the last link from the path
  */
  
  public void pop() {
    links_.remove(links_.size() - 1);
    return;
  }  

  /***************************************************************************
  **
  ** Answer if the given node shows up in the path
  */
  
  public boolean contains(String node) {
    return (position(node) != -1);
  }  
  
  /***************************************************************************
  **
  ** Gives the position of the given node in the path
  */
  
  public int position(String node) {
    int size = links_.size();
    for (int i = 0; i < size; i++) {
      SignedTaggedLink link = links_.get(i);
      if (link.getSrc().equals(node)) {
        return (i);
      } else if ((i == (size - 1)) && (link.getTrg().equals(node))) {
        return (size);
      }
    }
    return (-1);
  }  

  /***************************************************************************
  **
  ** Return the tail.  Will be null if the node does not show up.
  */
  
  public Path tail(String start) {
    int position = position(start);
    int size = links_.size();
    if (position == -1) {
      return (null);
    } else if (position == size) {
      return (new Path());
    } else {
      ArrayList<SignedTaggedLink> retList = new ArrayList<SignedTaggedLink>(links_.subList(position, size - 1));
      HashMap<String, String> retMap = new HashMap<String, String>();
      int rSize = retList.size();
      for (int i = 0; i < rSize; i++) {
        SignedTaggedLink link = retList.get(i);
        String linkSource = link.getSrc();
        String linkTarget = link.getTrg(); 
        if (retMap.get(linkSource) == null) {
          retMap.put(linkSource, nameMap_.get(linkSource));
        }
        if (retMap.get(linkTarget) == null) {
          retMap.put(linkTarget, nameMap_.get(linkTarget));
        }
      }
      return (new Path(retList, retMap));
    }
  }   

  /***************************************************************************
  **
  ** Return IDs of nodes in path 
  */
  
  public Set<String> pathNodes() {
    HashSet<String> retval = new HashSet<String>();
    int size = links_.size();
    for (int i = 0; i < size; i++) {
      SignedTaggedLink link = links_.get(i);
      retval.add(link.getSrc());
      if (i == (size - 1)) {
        retval.add(link.getTrg());
      }
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Merge a tail onto this network.  Returns null if the resulting path
  ** contains a cycle.
  */
  
  public Path mergeTail(Path tail) {
    String tailStart = tail.getStart();
    String headEnd = this.getEnd();
    if ((tailStart != null) && (headEnd != null) && (!headEnd.equals(tailStart))) {
      throw new IllegalArgumentException();
    }
    Set<String> headNodes = pathNodes();
    ArrayList<SignedTaggedLink> retlinks = new ArrayList<SignedTaggedLink>(links_);
    HashMap<String, String> retMap = new HashMap<String, String>(nameMap_);
    int tailSize = tail.links_.size();
    for (int i = 0; i < tailSize; i++) {
      SignedTaggedLink link = tail.links_.get(i);
      String linkSource = link.getSrc();
      String linkTarget = link.getTrg();      
      if (headNodes.contains(linkSource) || headNodes.contains(linkTarget)) {
        return (null);
      }
      retlinks.add(link);
      if (retMap.get(linkSource) == null) {
        retMap.put(linkSource, tail.nameMap_.get(linkSource));
      }
      if (retMap.get(linkTarget) == null) {
        retMap.put(linkTarget, tail.nameMap_.get(linkTarget));
      }      
    }
    return (new Path(retlinks, retMap));
  }

  /***************************************************************************
  **
  ** Get the sign of the link
  */
  
  public int getSign() {
    
    int size = links_.size();
    if (size == 0) {
      return (Linkage.NONE);
    }
    int retval = links_.get(0).getSign(); 
    for (int i = 1; i < size; i++) {
      SignedTaggedLink link = links_.get(i);
      retval = resolveSign(retval, link.getSign());
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Get the sign of the link
  */
  
  private int resolveSign(int current, int newSign) {
    
    if (newSign == Linkage.NONE) {
      return (Linkage.NONE);
    }
    
    switch (current) {
      case Linkage.NEGATIVE:
        return ((newSign == Linkage.POSITIVE) ? Linkage.NEGATIVE : Linkage.POSITIVE);
      case Linkage.POSITIVE:
        return ((newSign == Linkage.NEGATIVE) ? Linkage.NEGATIVE : Linkage.POSITIVE);        
      case Linkage.NONE:
        return (Linkage.NONE);
      default:
        throw new IllegalArgumentException();
    }
  }  

  /***************************************************************************
  **
  ** Get string representation
  */
  
  public String toString() {
    StringBuffer buf = new StringBuffer();
    
    int size = links_.size();
    for (int i = 0; i < size; i++) {
      SignedTaggedLink link = links_.get(i);
      String display = nameMap_.get(link.getSrc());
      buf.append(display);
      if (link.getSign() == Linkage.NEGATIVE) {
        buf.append("-|");
      } else if (link.getSign() == Linkage.POSITIVE) {
        buf.append("->");
      } else {
        buf.append("--");
      }
      if (i == (size - 1)) {
        buf.append(nameMap_.get(link.getTrg()));
      }
    }
    return (buf.toString());
  }  

  /***************************************************************************
  **
  ** Equals
  */
  
  public boolean equals(Object o) {
    if (this == o) {
      return (true);
    }
    if (!(o instanceof Path)) {
      return (false);
    }
    Path other = (Path)o;
    
    //
    // We are considering ranking!
    //
    
    if (this.ranking_ != other.ranking_) {
      return (false);
    }
    
    //
    // All links have to match
    //
    
    int size = links_.size();
    if (other.links_.size() != size) {
      return (false);
    }
    for (int i = 0; i < size; i++) {
      SignedTaggedLink thisLink = links_.get(i);
      SignedTaggedLink otherLink = other.links_.get(i);
      if (thisLink == otherLink) {
        continue;
      }
      if (!thisLink.getTag().equals(otherLink.getTag())) {
        return (false);
      }
    }
    return (true);
  }  

  /***************************************************************************
  **
  ** HashCode
  */
  
  public int hashCode() {
    int size = links_.size();
    if (size == 0) {
      return (0);
    }
    String val = getStart() + "-" + size + "-" + getEnd();
    return (val.hashCode());
  }  
  
  /***************************************************************************
  **
  ** Compares two paths
  **
  ** @return  a negative integer, zero, or a positive integer as this object
  ** 		is less than, equal to, or greater than the specified object.
  **
  ** @throws ClassCastException if the specified object's type prevents it
  **         from being compared to this Object.
  **
  */
  
  public int compareTo(Path other) {
    if (this == other) {
      return (0);
    }
    //
    // Paths with lower ranking come first:
    //
   
    int rdiff = this.ranking_ - other.ranking_;
    if (rdiff != 0) {
      return (rdiff);
    }    
    
    //
    // Paths of shorter length are less than paths of longer length
    //
    
    int size = this.links_.size();
    int otherSize = other.links_.size();
    int diff = size - otherSize;
    if (diff != 0) {
      return (diff);
    }

    //
    // If equal length, we sort on the path node names and
    // link signs
    //
    
    for (int i = 0; i < size; i++) {
      SignedTaggedLink thisLink = links_.get(i);
      SignedTaggedLink otherLink = other.links_.get(i);
      if (thisLink == otherLink) {
        continue;
      }
      String thisSource = thisLink.getSrc();
      String otherSource = otherLink.getSrc();       
      if (!thisSource.equals(otherSource)) {
        String thisDisplay = this.nameMap_.get(thisSource);        
        String otherDisplay = other.nameMap_.get(otherSource);
        return (thisDisplay.compareToIgnoreCase(otherDisplay));
      }
      String thisTarget = thisLink.getTrg();
      String otherTarget = otherLink.getTrg();       
      if (!thisTarget.equals(otherTarget)) {
        String thisDisplay = this.nameMap_.get(thisTarget);        
        String otherDisplay = other.nameMap_.get(otherTarget);
        return (thisDisplay.compareToIgnoreCase(otherDisplay));
      }
      int thisSign = thisLink.getSign();
      int otherSign = otherLink.getSign();
      if (thisSign != otherSign) {
        if (thisSign == Linkage.NONE) {
          return (1);
        } else if (otherSign == Linkage.NONE) {
          return (-1);
        } else {
          return (thisSign - otherSign);
        }
      }
    }    
    
    //
    // If still equal, we have identical display strings (maybe two nodes are named the
    // same, or two identical links connect the same two nodes).  Sort on the link ID
    // difference.
    //
    
    for (int i = 0; i < size; i++) {
      SignedTaggedLink thisLink = links_.get(i);
      SignedTaggedLink otherLink = other.links_.get(i);
      String thisID = thisLink.getTag();
      String otherID = otherLink.getTag(); 
      if (!thisID.equals(otherID)) {
        return (thisID.compareTo(otherID));
      }
    }
    
    return (0);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTOR
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** Constructor with list
  */

  private Path(ArrayList<SignedTaggedLink> list, HashMap<String, String> nameMap) {
    links_ = list;
    nameMap_ = nameMap;
    ranking_ = 0;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Add a names to name map
  */
  
  private void addToNameMap(HashMap<String, String> nameMap, Linkage link, GenomeSource src, String gKey) {
    String srcKey = link.getSource();
    Genome gen = src.getGenome(gKey);
    if (nameMap.get(srcKey) == null) {
      String srcName = gen.getNode(srcKey).getDisplayString(gen, false);
      nameMap.put(srcKey, srcName);
    }
    String trgKey = link.getTarget(); 
    if (nameMap.get(trgKey) == null) {
      String trgName = gen.getNode(trgKey).getDisplayString(gen, false);
      nameMap.put(trgKey, trgName);
    }
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
}
