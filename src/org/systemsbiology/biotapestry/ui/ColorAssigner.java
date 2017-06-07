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

package org.systemsbiology.biotapestry.ui;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collections;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.util.LinkPlacementGrid;
import org.systemsbiology.biotapestry.analysis.GraphColorer;
import org.systemsbiology.biotapestry.analysis.Link;


/****************************************************************************
**
** Used to color genes and links
*/

public class ColorAssigner {
  
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
  
  public static final int COLORING_OK           = 0x00;    
  public static final int COLOR_COLLISION       = 0x01;
  public static final int COLOR_REASSIGNMENT    = 0x02;
  public static final int TOO_FEW_COLORS        = 0x04;
  private static final int MAYBE_COLOR_COLLISION = 0x08;  
  
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

  public ColorAssigner() {
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  
  /***************************************************************************
  **
  ** Assign colors from simple map directives. If grid is null, we do not check
  ** to issue ambiguous crossing warnings.
  */
  
  public ColorIssues assignViaMap(DataAccessContext rcx, LinkPlacementGrid grid,
                                  Map<String, String> nodeColorMap, Map<String, String> linkColorMap) {

    ColorIssues issues = new ColorIssues(); // OK
    
    Iterator<String> ncmkit = nodeColorMap.keySet().iterator(); 
    while (ncmkit.hasNext()) {
      String nodeID = ncmkit.next();
      NodeProperties np = rcx.getCurrentLayout().getNodeProperties(nodeID);
      String mapColor = nodeColorMap.get(nodeID); 
      np.setColor(mapColor);
    }
    
    Iterator<String> lcmkit = linkColorMap.keySet().iterator(); 
    while (lcmkit.hasNext()) {
      String linkID = lcmkit.next();
      LinkProperties lp = rcx.getCurrentLayout().getLinkProperties(linkID);        
      String mapColor = linkColorMap.get(linkID); 
      lp.setColor(mapColor);
    }    

    if (grid != null) {
      ColorIssues newIssues = hasAmbiguousCrossing(rcx, grid);
      if (newIssues != null) {
        issues = newIssues;
      } 
    }
    return (issues);
  }  

  /***************************************************************************
  **
  ** Assign colors
  */
  
  public ColorIssues assignColors(DataAccessContext rcx, LinkPlacementGrid grid, 
                                  Map<String, String> colorMap, boolean keepColors, boolean incremental,
                                  Set<String> needColors) {

    ColorIssues issues = new ColorIssues(); // OK
    
    if (keepColors) {
      keepColors(rcx, colorMap);
      ColorIssues newIssues = hasAmbiguousCrossing(rcx, grid);
      if (newIssues != null) {
        issues = newIssues;
      }    
    } else {
      int assignRetval = assignUniqueColors(rcx, needColors, grid, colorMap, incremental);
      if ((assignRetval & MAYBE_COLOR_COLLISION) != 0x00) {
        ColorIssues newIssues = hasAmbiguousCrossing(rcx, grid);
        if (newIssues != null) {
          issues.status |= COLOR_COLLISION;
          issues.collisionSrc1 = newIssues.collisionSrc1;
          issues.collisionSrc2 = newIssues.collisionSrc2;          
        }
        assignRetval &= ~MAYBE_COLOR_COLLISION;
      }
      issues.status |= assignRetval;
    }
    return (issues);
  }
  
  /***************************************************************************
  **
  ** Assign colors
  */
  
  public ColorIssues hasAmbiguousCrossing(DataAccessContext rcx, LinkPlacementGrid grid) {
                
    Set<Link> crossings = grid.getAllCrossings();
    Iterator<Link> crit = crossings.iterator();
    while (crit.hasNext()) {
      Link gcl = crit.next();
      LinkProperties lp1 = rcx.getCurrentLayout().getLinkPropertiesForSource(gcl.getSrc());
      LinkProperties lp2 = rcx.getCurrentLayout().getLinkPropertiesForSource(gcl.getTrg());      
      if (lp1.getColorName().equals(lp2.getColorName())) {
        String nodeName1 = rcx.getCurrentGenome().getNode(gcl.getSrc()).getName();
        if (nodeName1 == null) {
          nodeName1 = "";
        }
        String nodeName2 = rcx.getCurrentGenome().getNode(gcl.getTrg()).getName();
        if (nodeName2 == null) {
          nodeName2 = "";
        }        
        return (new ColorIssues(nodeName1, nodeName2));
      }
    }    

    return (null);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Used to report on problems
  */ 
    
  public static final class ColorIssues {      
    public int status;
    public String collisionSrc1;
    public String collisionSrc2;
    
    public ColorIssues() {
      status = COLORING_OK;
    }
    
    public ColorIssues(String src1, String src2) {
      status = COLOR_COLLISION;
      collisionSrc1 = src1;
      collisionSrc2 = src2;
    }
  }
  
  /***************************************************************************
  **
  ** Change the colors from black
  */
  @SuppressWarnings("unused")
  private void changeColors(String linkID, DataAccessContext rcx, Map<String, String> colorMap) {
    Linkage link = rcx.getCurrentGenome().getLinkage(linkID);    
    LinkProperties lp = rcx.getCurrentLayout().getLinkProperties(linkID);
    String src = link.getSource();
    NodeProperties np = rcx.getCurrentLayout().getNodeProperties(src);
    String colTag = null;
    if (colorMap != null) {
      colTag = colorMap.get(src);
    }
    if (colTag == null) {
      colTag = rcx.getColorResolver().getNextColor(rcx.getGenomeSource(), rcx.getLayoutSource());
      np.setColor(colTag);
      lp.setColor(colTag);
    } else if (colTag.equals("white")) {
      colTag = rcx.getColorResolver().getNextColor(rcx.getGenomeSource(), rcx.getLayoutSource());
      np.setColor(colTag);
      lp.setColor(colTag);
    } else {
      lp.setColor(colTag);      
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Retain colors
  */
  
  private void keepColors(DataAccessContext rcx, Map<String, String> colorMap) {
    Iterator<Linkage> lit = rcx.getCurrentGenome().getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String linkID = link.getID();
      LinkProperties lp = rcx.getCurrentLayout().getLinkProperties(linkID);
      String color = lp.getColorName();
      String mappedColor = colorMap.get(linkID);
      if (!color.equals(mappedColor)) {
        lp.setColor(mappedColor);
      }
    }
    return;
  }  

  /***************************************************************************
  **
  ** Change the colors from black
  */
  
  private int assignUniqueColors(DataAccessContext rcx, Set<String> needColors,
                                 LinkPlacementGrid grid, Map<String, String> colorMapOrig,
                                 boolean incremental) {
                       
    

    int retval = COLORING_OK;

    //
    // Color map tells us two things: 1) how to assign colors to nodes and links
    // even if we have trashed the layout, in an attempt at some continuity, and
    // 2) how to assign colors to links from existing nodes, even those retained
    // in an incremental layout.
    // Duplicate the color map; we may need to trash it:
    //
    
    HashMap<String, String> colorMap = (colorMapOrig == null) ? null : new HashMap<String, String>(colorMapOrig);    
    
    //
    // If we have previous color assignments, build up a list of nodes with
    // outbound links.  We don't care about existing color assignments to
    // nodes with no outbound links.
    //
    
    HashSet<String> nodesWithLinksOut = null;
    if ((colorMap != null) && (colorMap.size() > 0)) {    
      nodesWithLinksOut = new HashSet<String>();
      Iterator<Linkage> lit = rcx.getCurrentGenome().getLinkageIterator();
      while (lit.hasNext()) {
        Linkage link = lit.next();
        nodesWithLinksOut.add(link.getSource());
      }
    }
    
    //
    // Build a set of available colors:
    //
    
    int numColors = rcx.getColorResolver().getNumColors();    
    HashSet<String> availColors = new HashSet<String>();
    HashSet<String> legacyUnused = new HashSet<String>();       
    for (int i = 0; i < numColors; i++) {
      String color = rcx.getColorResolver().getGeneColor(i);
      availColors.add(color);
      legacyUnused.add(color);
    }
    
    //
    // Create a map of src->link, and the set of sources needing colors
    //
                       
    HashMap<String, String> linkForSource = new HashMap<String, String>();                   
    Iterator<String> ncit = needColors.iterator();
    while (ncit.hasNext()) {
      String linkID = ncit.next();
      Linkage link = rcx.getCurrentGenome().getLinkage(linkID);    
      String src = link.getSource();
      linkForSource.put(src, linkID);
    }
    Set<String> needySources = linkForSource.keySet();
                
    //
    // Get a graph "color" assignment
    //
                       
    GraphColorer colorer = grid.buildCrossingGraph(); 
    Map<String, Integer> colorAssignment = new HashMap<String, Integer>();
    colorer.color(colorAssignment);

    //
    // Track existing color assignments to current groups
    //
    
    HashMap<String, List<Integer>> colorToGroups = null;
    if ((colorMap != null) && (colorMap.size() > 0)) {
      colorToGroups = new HashMap<String, List<Integer>>();
    }    
    
    //
    // The "color" assignment actually divides the nodes into equivalence classes,
    // where two nodes from different classes cannot share the same color
    // 
    // Invert the map: colorClass -> needy source node list
    //
    
    HashMap<Integer, Set<String>> classes = new HashMap<Integer, Set<String>>();
    Iterator<String> cait = colorAssignment.keySet().iterator();
    while (cait.hasNext()) {
      String nodeID = cait.next();
      Integer colorClass = colorAssignment.get(nodeID);
      Set<String> nodeSet = classes.get(colorClass);
      if (nodeSet == null) {
        nodeSet = new HashSet<String>();
        classes.put(colorClass, nodeSet);
      }
      if (needySources.contains(nodeID)) {
        nodeSet.add(nodeID);
      }
      //
      // Remember which color classes are using each legacy color, but only if the
      // node has an outbound link:
      //
      
      if ((colorToGroups != null) && 
          (nodesWithLinksOut != null) &&
          (nodesWithLinksOut.contains(nodeID))) {
        String preAssigned = colorMap.get(nodeID);
        if (preAssigned != null) {
          List<Integer> groupList = colorToGroups.get(preAssigned);
          if (groupList == null) {
            groupList = new ArrayList<Integer>();
            colorToGroups.put(preAssigned, groupList);
          }
          if (!groupList.contains(colorClass)) {
            groupList.add(colorClass);
          }
        }
      }
    }
    
    //
    // Now start assigning colors to color groups
    //
   
    //
    // If we have legacy color assignments, assign those to color classes first.  If
    // we have a color used by two classes, only assign it to the first class we
    // see.
    //
    
    // FIX ME!!!!! Concept of forced legacy (from layout) vs. optional (node is new to incremental load)
    // Forced legacy should have favor in getting it.
    
    HashMap<Integer, List<String>> assignments = new HashMap<Integer, List<String>>();
    boolean bigGroupList = false;
 
    if (colorToGroups != null) {
      Iterator<String> ctgit = colorToGroups.keySet().iterator();
      while (ctgit.hasNext()) {
        String color = ctgit.next();
        if (color.equals("black") || color.equals("white")) {
          continue;
        }
        List<Integer> groupList = colorToGroups.get(color);
        if (groupList != null) {
          if (groupList.size() > 1) {
            bigGroupList = true;
          }
          Integer groupNum = groupList.get(0);
          List<String> colorList = assignments.get(groupNum);
          if (colorList == null) {
            colorList = new ArrayList<String>();
            assignments.put(groupNum, colorList);
          }
          colorList.add(color);
          legacyUnused.remove(color);
          availColors.remove(color);
        }
      }
    }
        
    //
    // We now probably have color groups that still need at least ONE color
    // assigned.  If we have more groups than colors, we will warn the user
    // and assign the colors we have to the more crowded groups until we
    // run out, then cycle through colors not used in legacy cases, or black
    // if that set is empty (i.e. all colors are present in legacy).  
    //
       
    int needyClasses = needyClassCount(classes, assignments);
    
    if (needyClasses > availColors.size()) {
      retval |= TOO_FEW_COLORS;
    }
    List<Integer> assignOrder = sortEmptyBySize(classes, assignments);
    int numAssign = assignOrder.size();
    ArrayList<String> unusedList = new ArrayList<String>(legacyUnused);
    int currLegacyUnused = 0;
    int unusedSize = unusedList.size();
    for (int i = 0; i < numAssign; i++) {
      Integer groupNum = assignOrder.get(i);
      List<String> colorList = assignments.get(groupNum);
      if (colorList == null) {
        colorList = new ArrayList<String>();
        assignments.put(groupNum, colorList);
      }
      if (!availColors.isEmpty()) {
        String color = availColors.iterator().next();
        availColors.remove(color);
        colorList.add(color);
      } else if (unusedSize > 0) {
        String color = unusedList.get(currLegacyUnused++);
        currLegacyUnused %= unusedSize;
        colorList.add(color);
      } else {
        colorList.add("black");
      }
    }
    
    //
    // Every group has at least one color, maybe more.  Take the group
    // with the worst link/color ratio and give it a color.  Continue
    // until colors are exhausted.
    //
 
    Iterator<String> acit = availColors.iterator();
    while (acit.hasNext()) {
      String color = acit.next();
      Integer groupNum = worstLinksPerColor(classes, assignments);
      // Cheap hack for BT-08-24-07:1
      if (groupNum == null) {
        continue;
      }
      List<String> colorList = assignments.get(groupNum);
      if (colorList == null) {
        throw new IllegalStateException();
      }
      colorList.add(color);
    }
    availColors.clear();
    
    //
    // Now assign colors to needy links and nodes.
    //
    // If we are not incremental, we assign colors to needy links based on the color
    // present in the layout.  If not there, we only assign from the map if we have
    // the color.
    // If we are incremental, we only assign color to node from map if the color is present
    // for the group.
    // With no map, we assign the colors in round robin fashion.
    //
   
    HashMap<Integer, List<String>> colorCycles = new HashMap<Integer, List<String>>();
    Iterator<Integer> clait = classes.keySet().iterator();
    while (clait.hasNext()) {
      Integer colorClass = clait.next();
      List<String> colorList = assignments.get(colorClass);
      Set<String> nodeSet = classes.get(colorClass);
      if (nodeSet.size() == 0) {
        continue;
      }
      Iterator<String> nsit = nodeSet.iterator();
      ArrayList<String> remainingColors = new ArrayList<String>();
      colorCycles.put(colorClass, remainingColors);
      while (nsit.hasNext()) {
        if (remainingColors.isEmpty()) {
          rcx.getColorResolver().getRarestColors(colorList, remainingColors, rcx.getDBGenome(), rcx.getRootLayout());
        }
        String nodeID = nsit.next();
        NodeProperties np = rcx.getCurrentLayout().getNodeProperties(nodeID);
        String linkID = linkForSource.get(nodeID);
        LinkProperties lp = rcx.getCurrentLayout().getLinkProperties(linkID);        
        if (!incremental && (colorMap != null)) {
          if (bigGroupList) {
            retval |= COLOR_REASSIGNMENT;
          }         
          String nodeColor = np.getColorName();
          String mapColor = colorMap.remove(nodeID);
          // Carryover of old layout; force link color no matter what (unless black)
          if (nodeColor.equals(mapColor) && (!mapColor.equals("black") && (!mapColor.equals("white")))) {
            lp.setColor(nodeColor);
            remainingColors.remove(nodeColor);  // may not actually be present
          } else {
            // New to layout.  Only assign by map if we can 
            if (colorList.contains(mapColor) && (!mapColor.equals("white"))) {
              np.setColor(mapColor);
              lp.setColor(mapColor);
              remainingColors.remove(mapColor);  // may not actually be present
            } else { // New to layout, not allowed to use suggestion; use another
              String useColor = remainingColors.get(0);
              np.setColor(useColor);
              lp.setColor(useColor);
              remainingColors.remove(useColor);
            }
          }
        } else if (incremental && (colorMap != null)) {
          if (bigGroupList) {
            retval |= MAYBE_COLOR_COLLISION;
          }
          String mapColor = colorMap.remove(nodeID);
          if (colorList.contains(mapColor) && (!mapColor.equals("black") && (!mapColor.equals("white")))) {
            lp.setColor(mapColor);
            np.setColor(mapColor);
            remainingColors.remove(mapColor);  // may not actually be present
          } else { // New to layout, not allowed to use suggestion; use another
            String useColor = remainingColors.get(0);
            np.setColor(useColor);
            lp.setColor(useColor);
            remainingColors.remove(useColor);
          }
        } else { // Assign by available colors            
          String useColor = remainingColors.get(0);
          np.setColor(useColor);
          lp.setColor(useColor);
          remainingColors.remove(useColor);
        }
      }
    }
    
    //
    // Assign colors to guys remaining in the map who do not match the layout.  This
    // should only be previously colored nodes with no outbound links?
    //
    
    if (colorMap != null) {
      Iterator<String> cmit = colorMap.keySet().iterator();
      while (cmit.hasNext()) {
        String nodeID = cmit.next();
        String useColor = colorMap.get(nodeID);
        NodeProperties np = rcx.getCurrentLayout().getNodeProperties(nodeID);   
        String nodeColor = np.getColorName();
        if (!nodeColor.equals(useColor)) {
          if (nodesWithLinksOut.contains(nodeID)) {
            throw new IllegalStateException();
          }
          np.setColor(useColor);
        }
      }
    }
    return (retval);
  }  

  /***************************************************************************
  **
  ** Return list of groups sorted by decreasing size
  */
  
  private List<Integer> sortEmptyBySize(Map<Integer, Set<String>> classes, Map<Integer, List<String>> assignments) {
    //
    // If the class has no assignments, add to list based on number of
    // members.  Ignore classes with assignments.
    //
    TreeMap<Integer, List<Integer>> sorter = new TreeMap<Integer, List<Integer>>(Collections.reverseOrder());
    Iterator<Integer> ckit = classes.keySet().iterator();
    while (ckit.hasNext()) {
      Integer colorClass = ckit.next();
      List<String> colorList = assignments.get(colorClass);
      if ((colorList != null) && (colorList.size() > 0)) {
        continue;
      }
      Set<String> nodeSet = classes.get(colorClass);
      int numNodes = (nodeSet == null) ? 0 : nodeSet.size();
      Integer key = new Integer(numNodes);
      List<Integer> listForSize = sorter.get(key);
      if (listForSize == null) {
        listForSize = new ArrayList<Integer>();
        sorter.put(key, listForSize);
      } 
      listForSize.add(colorClass);
    }
      
    ArrayList<Integer> retval = new ArrayList<Integer>();
    Iterator<Integer> sit = sorter.keySet().iterator();
    while (sit.hasNext()) {
      Integer key = sit.next();
      List<Integer> listForSize = sorter.get(key);
      retval.addAll(listForSize);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Return the group with the worst ratio of links to colors
  */
  
  private Integer worstLinksPerColor(Map<Integer, Set<String>> classes, Map<Integer, List<String>> assignments) {
    //
    // If the class has no assignments, add to list based on number of
    // members.  Ignore classes with assignments.
    //
    TreeMap<Double, List<Integer>> sorter = new TreeMap<Double, List<Integer>>(Collections.reverseOrder());
    Iterator<Integer> ckit = classes.keySet().iterator();
    while (ckit.hasNext()) {
      Integer colorClass = ckit.next();
      List<String> colorList = assignments.get(colorClass);
      int colorCount = (colorList == null) ? 0 : colorList.size();
      Set<String> nodeSet = classes.get(colorClass);
      int numNodes = (nodeSet == null) ? 0 : nodeSet.size();
      double ratio = (double)numNodes / (double)colorCount;
      Double key = new Double(ratio);
      List<Integer> listForSize = sorter.get(key);
      if (listForSize == null) {
        listForSize = new ArrayList<Integer>();
        sorter.put(key, listForSize);
      } 
      listForSize.add(colorClass);
    }
    
    // See BT-08-24-07:1
    
    if (sorter.keySet().isEmpty()) {
      return (null);
    }
    
    Double firstKey = sorter.keySet().iterator().next();
    List<Integer> listForSize = sorter.get(firstKey);
    return (listForSize.get(0));
  }  

  /***************************************************************************
  **
  ** Return the count of classes needing a color
  */
  
  private int needyClassCount(Map<Integer, Set<String>> classes, Map<Integer, List<String>> assignments) {
    int retval = 0;
    Iterator<Integer> ckit = classes.keySet().iterator();    
    while (ckit.hasNext()) {
      Integer colorClass = ckit.next();
      List<String> colorList = assignments.get(colorClass);
      if ((colorList != null) && (colorList.size() > 0)) {
        continue;
      }
      Set<String> nodeSet = classes.get(colorClass);
      if ((nodeSet == null) || (nodeSet.size() == 0)) {
        continue;
      }
      retval++;
    }
    
    return (retval);
  }  
}