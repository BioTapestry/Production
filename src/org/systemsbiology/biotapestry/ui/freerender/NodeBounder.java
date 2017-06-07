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

package org.systemsbiology.biotapestry.ui.freerender;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.INodeRenderer;
import org.systemsbiology.biotapestry.ui.IRenderer;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.util.Bounds;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** This is a support class to calculate bounding rectangles for node sets
*/

public class NodeBounder {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public static final int DEFAULT_EXTRA_PAD = UiUtil.GRID_SIZE_INT;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor.  Do not use
  */

  private NodeBounder() {
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Figure out bounding rectangle for the nodes
  */
  
  public static Rectangle nodeBounds(Set<String> nodes, StaticDataAccessContext rcx, Rectangle minRect, 
                                     Rectangle extraRect, int extraPad, Map<String, Rectangle> eachRect) {

    Rectangle extent = minRect;

    Iterator<String> mit = nodes.iterator();
    Genome genome = rcx.getCurrentGenome();
    
    while (mit.hasNext()) {
      String nodeID = mit.next();
      Node node = genome.getNode(nodeID);      
      if (node == null) {
        continue;
      }
      NodeProperties np = rcx.getCurrentLayout().getNodeProperties(nodeID);
      if (np == null) { // useful for partial layouts...
        continue;
      }
      INodeRenderer rend = np.getRenderer(); 
      Rectangle bounds = rend.getModuleBounds(node, rcx, extraPad, null);
      if (extent == null) {
        extent = bounds;
      } else {
        Bounds.tweakBounds(extent, bounds);
      }
      if (eachRect != null) {
        eachRect.put(nodeID, bounds);
      }     
    }
    if (extraRect != null) {
      if (extent == null) {
        extent = extraRect;
      } else {
        Bounds.tweakBounds(extent, extraRect);
      }   
    }   
    return (extent);
  }

  /***************************************************************************
  **
  ** Figure out a place to put a label
  */
  
  public static Point2D prelimLabelLocation(String firstNodeID, StaticDataAccessContext rcx, int extraPad) {
    Genome genome = rcx.getCurrentGenome();
    Node node = genome.getNode(firstNodeID);
    NodeProperties np = rcx.getCurrentLayout().getNodeProperties(firstNodeID);
    INodeRenderer rend = np.getRenderer(); 
    Rectangle bounds = rend.getModuleBounds(node, rcx, extraPad, null);
    return (new Point2D.Double(bounds.getCenterX(), bounds.getY() - (UiUtil.GRID_SIZE * 5.0)));
  }  
  
  /***************************************************************************
  **
  ** Figure out set of individual rectangles for the nodes
  */
  
  public static Map<String, Rectangle> nodeRects(Set<String> nodes, StaticDataAccessContext rcx, int extraPad) {

    HashMap<String, Rectangle> retval = new HashMap<String, Rectangle>();
    Genome genome = rcx.getCurrentGenome();
    
    Iterator<String> mit = nodes.iterator();
    while (mit.hasNext()) {
      String nodeID = mit.next();
      Node node = genome.getNode(nodeID);
      if (node == null) {
        continue;
      }
      NodeProperties np = rcx.getCurrentLayout().getNodeProperties(nodeID);
      if (np == null) { // useful for partial layouts...
        continue;
      }
      INodeRenderer rend = np.getRenderer(); 
      Rectangle bounds = rend.getModuleBounds(node, rcx, extraPad, null);
      retval.put(nodeID, bounds);
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Figure out nodes inside the given rectangle
  */
  
  public static Set<String> getNodesInRect(Rectangle rect, StaticDataAccessContext rcx) {  
  
    Genome genome = rcx.getCurrentGenome();
    HashSet<String> retval = new HashSet<String>();
    Iterator<Node> git = genome.getAllNodeIterator();    
    while (git.hasNext()) {
      Node node = git.next();
      String nodeID = node.getID();
      IRenderer render = rcx.getCurrentLayout().getNodeProperties(nodeID).getRenderer();      
      Intersection inter = render.intersects(node, rect, true, rcx, null);
      if (inter != null) {
        retval.add(nodeID);
      }
    }
    if (genome instanceof DynamicGenomeInstance) {
      DynamicGenomeInstance dgi = (DynamicGenomeInstance)genome;
      DynamicInstanceProxy dip = rcx.getGenomeSource().getDynamicProxy(dgi.getProxyID());
      if (dip.hasAddedNodes()) {
        Iterator<DynamicInstanceProxy.AddedNode> ait = dip.getAddedNodeIterator();    
        while (ait.hasNext()) {
          DynamicInstanceProxy.AddedNode node = ait.next();
          String nodeID = node.nodeName;
          IRenderer render = rcx.getCurrentLayout().getNodeProperties(nodeID).getRenderer();      
          Intersection inter = render.intersects(dgi.getNode(nodeID), rect, true, rcx, null);
          if (inter != null) {
            retval.add(nodeID);
          }
        } 
      }
    }
    return (retval);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////   
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
}
