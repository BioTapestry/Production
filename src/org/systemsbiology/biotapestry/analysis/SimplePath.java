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
import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;

import org.systemsbiology.biotapestry.genome.Linkage;

/****************************************************************************
**
** A path is currently just an ordered list of links.  It should be cycle-free,
** unless it specifies a simple single link autoloop (src == trg)
** This differs from the Path Class in that it works with signed links
** instead of a Genome model
*/

public class SimplePath implements Comparable<SimplePath>, Cloneable {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
   
  private ArrayList<SignedLink> links_;
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

  public SimplePath() {
    links_ = new ArrayList<SignedLink>();
    ranking_ = 0;
    simpleLoop_ = false;
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  public SimplePath clone() {
    try {
      SimplePath retval = (SimplePath)super.clone();
      retval.links_ = new ArrayList<SignedLink>();
      int numLink = this.links_.size();
      for (int i = 0; i < numLink; i++) {
        retval.links_.add(this.links_.get(i).clone());
      }
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }    
   
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
  ** Return whether this path is just the link!
  */
  
  public boolean justTheLink(SignedLink link) {
    return ((getDepth() == 1) && 
            getStart().equals(link.getSrc()) && 
            getEnd().equals(link.getTrg()) &&
            getSign() == link.getSign());
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
  ** Add a link without checks DANGER!
 
  
  public void addLinkNoChecks(Linkage link) {
    links_.add(link);
    return;
  }  
  
  /***************************************************************************
  **
  ** Add a single simple loop
  */
  
  public void addSimpleLoopLink(SignedLink link) {

    String srcKey = link.getSrc();
    String trgKey = link.getTrg();
    
    if (!trgKey.equals(srcKey)) {
      throw new IllegalArgumentException();
    }
    if (links_.size() != 0) {
      throw new IllegalStateException();
    }
    links_.add(link);
    simpleLoop_ = true;
    return;
  }  
 
  /***************************************************************************
  **
  ** Add a link to the end of the path.  True iff successful.
  */
  
  public boolean addLink(SignedLink link) {
    //
    // Check the link to see that:
    //  1) source is the target of the previous link
    //  2) target is not a node already in the path (loop avoidance)
    //  3) Link is not a self loop
    //
    
    if (simpleLoop_) {
      throw new IllegalStateException();
    }
    
    String srcKey = link.getSrc();
    String newTarget = link.getTrg(); 
    
    if (newTarget.equals(srcKey)) {
      return (false);
    }
    int size = links_.size();
    if (size == 0) {
      links_.add(link);
      return (true);
    }
    SignedLink lastLink = links_.get(size - 1);
    if (!lastLink.getTrg().equals(srcKey)) {
      throw new IllegalArgumentException();
    }
    for (int i = 0; i < size; i++) {
      SignedLink pathLink = links_.get(i);
      String source = pathLink.getSrc();
      String target = pathLink.getTrg();
      if (newTarget.equals(source) || newTarget.equals(target)) {
        return (false);
      }
    }
    links_.add(link);
    return (true);
  }
  
  /***************************************************************************
  **
  ** Get an iterator over the links
  */
  
  public Iterator<SignedLink> pathIterator() {
    return (links_.iterator());
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
  ** Answer if the given link shows up in the path as an indirect path
  */
  
  public boolean containsLinkIndirectly(SignedLink link) {
    int srcPos = position(link.getSrc());
    if (srcPos == -1) {
      return (false);
    }
    int trgPos = position(link.getTrg());
    if (trgPos == -1) {
      return (false);
    }
    if ((trgPos - srcPos) < 2) {
      return (false);
    }
    if (getSubSign(srcPos, trgPos) != link.getSign()) {
      return (false);
    }
    return (true);
  } 
  
  /***************************************************************************
  **
  ** Answer if the given link shows up in the path
  */
  
  public boolean containsLink(SignedLink link) {
    int size = links_.size();
    for (int i = 0; i < size; i++) {
      SignedLink clink = links_.get(i);
      if (clink.equals(link)) {
        return (true);
      }
    }
    return (false);
  } 
   
  /***************************************************************************
  **
  ** Gives the position of the given node in the path
  */
  
  public int position(String node) {
    int size = links_.size();
    for (int i = 0; i < size; i++) {
      SignedLink link = links_.get(i);
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
  
  public SimplePath tail(String start) {
    int position = position(start);
    int size = links_.size();
    if (position == -1) {
      return (null);
    } else if (position == size) {
      return (new SimplePath());
    } else {
      ArrayList<SignedLink> retList = new ArrayList<SignedLink>(links_.subList(position, size - 1));
      return (new SimplePath(retList));
    }
  }   

  /***************************************************************************
  **
  ** Return the head.  Will be null if the node does not show up.
  */
  
  public SimplePath head(String end) {
    int position = position(end);
    if (position == -1) {
      return (null);
    } else if (position == 0) {
      return (new SimplePath());
    } else {
      ArrayList<SignedLink> retList = new ArrayList<SignedLink>(links_.subList(0, position - 1));
      return (new SimplePath(retList));
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
      SignedLink link = links_.get(i);
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
  
  public SimplePath mergeTail(SimplePath tail) {
    String tailStart = tail.getStart();
    String headEnd = this.getEnd();
    if ((tailStart != null) && (headEnd != null) && (!headEnd.equals(tailStart))) {
      System.err.println("Illegal tail merge: " + this + " to " + tail);
      throw new IllegalArgumentException();
    }
    Set<String> headNodes = pathNodes();
    ArrayList<SignedLink> retlinks = new ArrayList<SignedLink>(links_);
    int tailSize = tail.links_.size();
    for (int i = 0; i < tailSize; i++) {
      SignedLink link = tail.links_.get(i);
      String linkSource = link.getSrc();
      String linkTarget = link.getTrg();      
      if ((i != 0) && headNodes.contains(linkSource)) {
        return (null);
      }
      if (headNodes.contains(linkTarget)) {
        return (null);
      }
      retlinks.add(link);
    }
    return (new SimplePath(retlinks));
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
      SignedLink link = links_.get(i);
      retval = resolveSign(retval, link.getSign());
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Get the sign of a chunk of the path
  */
  
  public int getSubSign(int startPos, int endPos) {   
    int size = links_.size();
    if ((startPos >= endPos) || (startPos > size - 1) || (endPos > size)) {
      throw new IllegalArgumentException();
    }
    int retval = links_.get(startPos).getSign(); 
    for (int i = (startPos + 1); i < endPos; i++) {
      SignedLink link = links_.get(i);
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
      SignedLink link = links_.get(i);
      String display = link.getSrc();
      buf.append(display);
      if (link.getSign() == Linkage.NEGATIVE) {
        buf.append("-|");
      } else if (link.getSign() == Linkage.POSITIVE) {
        buf.append("->");
      } else {
        buf.append("--");
      }
      if (i == (size - 1)) {
        buf.append(link.getTrg());
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
    if (!(o instanceof SimplePath)) {
      return (false);
    }
    SimplePath other = (SimplePath)o;
    
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
      SignedLink thisLink = links_.get(i);
      SignedLink otherLink = other.links_.get(i);
      if (thisLink == otherLink) {
        continue;
      }
      if (!thisLink.equals(otherLink)) {
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
  */
  
  public int compareTo(SimplePath other) {
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
      SignedLink thisLink = links_.get(i);
      SignedLink otherLink = other.links_.get(i);
      if (thisLink == otherLink) {
        continue;
      }
      String thisSource = thisLink.getSrc();
      String otherSource = otherLink.getSrc();       
      if (!thisSource.equals(otherSource)) {
        return (thisSource.compareToIgnoreCase(otherSource));
      }
      String thisTarget = thisLink.getTrg();
      String otherTarget = otherLink.getTrg();       
      if (!thisTarget.equals(otherTarget)) {
        return (thisTarget.compareToIgnoreCase(otherTarget));
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

  private SimplePath(ArrayList<SignedLink> list) {
    links_ = list;
    ranking_ = 0;
    simpleLoop_ = false;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
}
