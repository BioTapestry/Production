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
  
public class DialogBuiltLoneNodeMotif extends DialogBuiltMotif {
  
  /***************************************************************************
  **
  ** Null Constructor
  **
  */
  
  public DialogBuiltLoneNodeMotif() {
  }  
   
  /***************************************************************************
  **
  ** Constructor
  **
  */
  
  public DialogBuiltLoneNodeMotif(String sourceId) {
    super(sourceId, null);
  }
  
  /***************************************************************************
  **
  ** Get display ordering
  */
  
  public String[] ordering() {
    return (new String[] {sourceId_});
  }
  
  /***************************************************************************
  **
  ** Get empty copy
  */
  
  public DialogBuiltMotif emptyCopy() {
    return (new DialogBuiltLoneNodeMotif());
  }
  
  /***************************************************************************
  **
  ** Find out what nodes and links exist in the motif
  */

  public void getExistingNodesAndLinks(Set<String> nodes, Set<String> links) {
    super.getExistingNodesAndLinks(nodes, links);
    return;
  } 
  
  /***************************************************************************
  **
  ** Assign the nodes to source and target regions
  */

  public void getRegionAssignments(Set<String> srcRegionNodes, Set<String> targRegionNodes, Map<String, Integer> linkTupleMap) {
    super.getRegionAssignments(srcRegionNodes, targRegionNodes, linkTupleMap);    
    return;
  }    

  /***************************************************************************
  **
  ** toString override
  */
  
  public String toString() {
    return ("DialogBuiltLoneNodeMotif: (" + super.toString() + ")");
  }  
  
 /***************************************************************************
  **
  ** Get link count
  */
  
  public int getLinkCount() {
    return (0);
  }
  
 /***************************************************************************
  **
  ** Get given link
  */
  
  public Link getLink(int index) {
    throw new IllegalStateException();
  }
}
