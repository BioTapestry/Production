/*
**    Copyright (C) 2003-2017 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.simulation;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;


/****************************************************************************
**
** API for extracting model data
*/

public interface ModelSource {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get the nodes
  */
  
  public Iterator<ModelNode> getRootModelNodes();
  
  
  /***************************************************************************
  **
  ** Get the nodes
  */
  
  public Iterator<ModelLink> getRootModelLinks();
  
  /***************************************************************************
  **
  ** Get a node
  */
  
  public ModelNode getNode(String nodeID);

  /***************************************************************************
  **
  ** Get region topology
  */
  
  public Iterator<ModelRegionTopologyForTime> getRegionTopology();
  
  /***************************************************************************
  **
  ** Returns available gene names for expression data
  */
  
  public Set<String> getExpressionGenes();

  /***************************************************************************
  **
  ** Returns available times for expression data
  */
  
  public SortedSet<Integer> getExpressionTimes();

             
  /***************************************************************************
  **
  ** Returns available regions for expression data
  */
  
  public List<ModelRegion> getRegions();
 
  /***************************************************************************
  **
  ** Returns expression entries
  */
  
  public ModelExpressionEntry getExpressionEntry(String geneName, String region, int time);
  
  /***************************************************************************
  **
  ** Go to the given model and select the given nodes and links.  If the
  ** operation is not successful, return false.
  */
  
  public boolean goToModelAndSelect(String modelID, Set<String> nodeIDs, Set<String> linkIDs);

  /***************************************************************************
   **
   ** Finds feedback links
   */

  public Set<ModelLink> findFeedbackEdges();

  /***************************************************************************
   **
   ** Finds root nodes
   */

  public Set<String> findRootModelRootNodeIDs();
}
