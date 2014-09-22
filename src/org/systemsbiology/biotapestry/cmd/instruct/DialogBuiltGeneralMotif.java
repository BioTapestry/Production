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

import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.analysis.Link;

/***************************************************************************
**
** Gene/Link construct for dialog-built networks
*/
  
public class DialogBuiltGeneralMotif extends DialogBuiltMotif {
  
  protected String linkId_;
 
  /***************************************************************************
  **
  ** Null Constructor
  **
  */
  
  public DialogBuiltGeneralMotif() {
  }  
   
  /***************************************************************************
  **
  ** Constructor
  **
  */
  
  public DialogBuiltGeneralMotif(String sourceId, String targetId, String linkId) {
    super(sourceId, targetId);
    linkId_ = linkId;    
  }
  
  /***************************************************************************
  **
  ** Get the link ID
  */
  
  public String getLinkId() {
    return (linkId_);
  }
  
  /***************************************************************************
  **
  ** Set the link ID
  */
  
  public void setLinkId(String linkId) {
    linkId_ = linkId;
    return;
  }
  
  /***************************************************************************
  **
  ** Get display ordering
  */
  
  public String[] ordering() {
    return ((targetId_ == null) ? new String[] {sourceId_} : new String[] {sourceId_, targetId_});
  }
  
  /***************************************************************************
  **
  ** Get empty copy
  */
  
  public DialogBuiltMotif emptyCopy() {
    return (new DialogBuiltGeneralMotif());
  }
  /***************************************************************************
  **
  ** Find out what nodes and links exist in the motif
  */

  public void getExistingNodesAndLinks(Set<String> nodes, Set<String> links) {
    super.getExistingNodesAndLinks(nodes, links);
    if (linkId_ != null) {
      links.add(linkId_);
    }
    return;
  } 
  
  /***************************************************************************
  **
  ** Assign the nodes to source and target regions
  */

  public void getRegionAssignments(Set<String> srcRegionNodes, Set<String> targRegionNodes, Map<String, Integer> linkTupleMap) {
    super.getRegionAssignments(srcRegionNodes, targRegionNodes, linkTupleMap);    
    if (linkId_ != null) {
      linkTupleMap.put(linkId_, new Integer(SOURCE_TARGET));
    }
    return;
  }    

  /***************************************************************************
  **
  ** toString override
  */
  
  public String toString() {
    return ("DialogBuiltGeneralMotif: (" + super.toString() + ")" 
            + " linkId = " + linkId_);
  }  
  
 /***************************************************************************
  **
  ** Get link count
  */
  
  public int getLinkCount() {
    return ((linkId_ == null) ? 0 : 1);
  }
  
 /***************************************************************************
  **
  ** Get given link
  */
  
  public Link getLink(int index) {
    if ((index != 0) || (linkId_ == null)) {
      throw new IllegalArgumentException();
    }
    return (new Link(sourceId_, targetId_));
  }
}
