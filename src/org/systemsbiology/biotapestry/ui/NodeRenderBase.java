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

package org.systemsbiology.biotapestry.ui;

import java.awt.BasicStroke;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DBNode;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalShapeContainer;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactory;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.Arc.Type;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.DrawMode;
import org.systemsbiology.biotapestry.ui.modelobjectcache.SelectedModalShapeContainer;
import org.systemsbiology.biotapestry.util.PatternGrid;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.LinkPlacementGrid;
import org.systemsbiology.biotapestry.util.Pattern;
import org.systemsbiology.biotapestry.genome.GenomeItem;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.NodeInstance;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.Vector2D;
import org.systemsbiology.biotapestry.util.MultiLineRenderSupport;

/****************************************************************************
**
** This is the abstract base class for rendering a node in BioTapestry
*/

public abstract class NodeRenderBase extends ItemRenderBase implements INodeRenderer {

  ////////////////////////////////////////////////////////////////////////////
  //
  // CLASS MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  public static final Integer NODE_MAJOR_LAYER = new Integer(0);
  public static final Integer NODE_MINOR_LAYER = new Integer(0);
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  protected static final double PIE_RADIUS = 8.0;
  protected static final int PIE_STROKE = 1;  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor
  */

  public NodeRenderBase() {
    super();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Get our preferred target color (bubbles want to be white)
  */
    
  public String getTargetColor() {
    return ("black");
  }

  /***************************************************************************
  ** 
  ** Answer if we can be in a simple fan in
  */
    
  public boolean simpleFanInOK() {
    return (false);
  }
  
  /***************************************************************************
  ** 
  ** Answer if assigning landing is consistent with direct landing:
  */
    
  public boolean landOKForDirect(int landing) {
    // Direct refactor of code, but likely not correct for Gene type (probably never called)
    return (landing == 3);
  }
  
  /***************************************************************************
  ** 
  ** Answer if node ignores force top directive
  */
    
  public boolean ignoreForceTop() {
    return (false);
  }
 
  /***************************************************************************
  ** 
  ** Get the left pad number
  */
    
  public int getLeftPad() {
    return (3);
  }
  
  /***************************************************************************
  **
  ** Get the actual available source pad range (not checking for occupancy)
  */
  
  public MinMax getSourcePadRange(GenomeItem item) {
    
    //
    // Get the actual pad count for the node:
    //
    
    Node theNode = (Node)item;    
    int newPadCount = theNode.getPadCount();
    int minPadNum = 0;
    int defaultPadCount = DBNode.getDefaultPadCount(theNode.getNodeType());
    if (newPadCount > defaultPadCount) {  // we have extra pads:
      minPadNum = defaultPadCount - newPadCount;
    }
    if ((minPadNum < 0) && !landingPadsCanOverflow()) {
      throw new IllegalStateException();
    } 

    int srcMax = getFixedLaunchPadMax() - 1;
    int srcMin = sharedPadNamespaces() ? minPadNum : 0;    
    return (new MinMax(srcMin, srcMax)); 
  }
  
  /***************************************************************************
  **
  ** Get the actual available target pad range (not checking for occupancy)
  */
  
  public MinMax getTargetPadRange(GenomeItem item) {
    
    //
    // Get the actual pad count for the node:
    //
    
    Node theNode = (Node)item;    
    int newPadCount = theNode.getPadCount();
    int minPadNum = 0;
    int defaultPadCount = DBNode.getDefaultPadCount(theNode.getNodeType());
    if (newPadCount > defaultPadCount) {  // we have extra pads:
      minPadNum = defaultPadCount - newPadCount;
    }
    if ((minPadNum < 0) && !landingPadsCanOverflow()) {
      throw new IllegalStateException();
    } 

    int padMax = getFixedLandingPadMax() - 1;  
    return (new MinMax(minPadNum, padMax));
  }
  
  /***************************************************************************
  **
  ** Suggest a launch pad.
  */
  
  public SortedSet<PadDotRanking> suggestLaunchPad(GenomeItem item, Layout layout, Point2D targPt) {
    
    MinMax padRange = getSourcePadRange(item);    
    int srcMax = padRange.max;
    int srcMin = padRange.min;    
 
    TreeSet<PadDotRanking> retval = new TreeSet<PadDotRanking>(Collections.reverseOrder());   
    NodeProperties np = layout.getNodeProperties(item.getID());
    Point2D loc = np.getLocation();
    Vector2D diff = new Vector2D(loc, targPt).normalized();

    for (int i = srcMin; i <= srcMax; i++) {
      Vector2D depDir = getDepartureDirection(i, item, layout);
      double dot = depDir.dot(diff);
      retval.add(new PadDotRanking(i, dot));
    }
    return (retval); 
  }
  
  /***************************************************************************
  **
  ** Suggest a sorted set of PadDotRankings for the location, best to worst
  */
  
  public SortedSet<PadDotRanking> suggestLandingPads(GenomeItem item, Layout layout, Point2D loc) {

    MinMax padRange = getTargetPadRange(item);    
    int padMax = padRange.max;
    int minPadNum = padRange.min;        

    TreeSet<PadDotRanking> retval = new TreeSet<PadDotRanking>(Collections.reverseOrder());
    NodeProperties np = layout.getNodeProperties(item.getID());
    Point2D myLoc = np.getLocation();
    Vector2D diff = new Vector2D(loc, myLoc).normalized();
    
    for (int i = minPadNum; i <= padMax; i++) {
      Vector2D depDir = getArrivalDirection(i, item, layout);
      double dot = depDir.dot(diff);
      retval.add(new PadDotRanking(i, dot));
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Get the bounds for autolayout, where we provide the needed orientation, padcount,
  ** and additional label text if desired.  It is assumed that the growth is horizontal. 
  */
  
  public abstract Rectangle2D getBoundsForLayout(GenomeItem item, DataAccessContext rcx, 
                                                 int orientation, boolean labelToo, Integer topPadCount);
                                        
 
  /***************************************************************************
  **
  ** Return the number of top pads on the node associated with the provided
  ** full pad count:
  */
  
  public abstract int topPadCount(int fullPadCount);
  
  
  /***************************************************************************
  **
  ** Get the launch pad offset for given layout case:
  */
  
  public abstract Vector2D getLaunchPadOffsetForLayout(GenomeItem item, DataAccessContext rcx, 
                                                       int orientation, Integer topPadCount);
                                              
               
   
  /***************************************************************************
  **
  ** Get the Y offset needed to line up nodes with a through link in a straight line
  */
  
  public double getStraightThroughOffset() {
    return (0.0);
  }
  
  /***************************************************************************
  **
  ** Get the X offset needed to line up nodes with centers aligned
  */
  
  public double getVerticalOffset() {
    return (0.0);
  }
  
  /***************************************************************************
  **
  ** Answer if landing pads can overflow
  */
  
  public boolean landingPadsCanOverflow() {
    return (false);
  }
  
  /***************************************************************************
  **
  ** Answer if landing and launching pad namespaces are shared
  */
  
  public boolean sharedPadNamespaces(){
    return (true);
  }
  
  /***************************************************************************
  **
  ** Get the preferred launch direction from the given pad
  */
  
  public Vector2D getDepartureDirection(int padNum, GenomeItem item, Layout layout) {
    Vector2D arrDir = getArrivalDirection(padNum, item, layout);
    arrDir.scale(-1.0);
    return (arrDir);
  }

  /***************************************************************************
  **
  ** Get the preferred arrival direction
  */
  
  public Vector2D getArrivalDirection(int padNum, GenomeItem item, Layout layout) {
    if (padNum >= 0) {
      switch (padNum) {
        case 0:
          return (new Vector2D(0.0, 1.0));
        case 1:
          return (new Vector2D(-1.0, 0.0));
        case 2:
          return (new Vector2D(0.0, -1.0));
        case 3:
          return (new Vector2D(1.0, 0.0));
        default:
          throw new IllegalArgumentException();        
      }
    } else {
      return (getExtraLandingPadArrivalDirection(padNum, item, layout));
    }
  }
  
  /***************************************************************************
  **
  ** Answer how much extra length will result from the given pad increase
  */
  
  public double getExtraLength(Node node, Genome genome, int newCount, boolean oneSideOnly) {
    return (0.0);
  }
   
  /***************************************************************************
  **
  ** Hand out one bottom pad
  */  
  
  public int getBottomPad(GenomeItem item) {
    return (2);
  }
  
  /***************************************************************************
  **
  ** Return a top pad, preferably unused.  Only really relevant for boxes and bubbles.
  ** Others should override.
  */

  public int getBestTopPad(GenomeItem item, Set<Integer> usedPads, int expandDir, 
                           int totalNeeded, boolean startFromLeft) {
    //
    // Top center is the first choice when building from center, else we build
    // from left:
    //
    
    if (startFromLeft) {
      int firstOnLeft = ((totalNeeded - 1) / 2) * -4;
      Integer firstChoice = new Integer(firstOnLeft);
      if (!usedPads.contains(firstChoice)) {
        usedPads.add(firstChoice);
        return (firstOnLeft);
      }            
    } else {  // build from center
      Integer firstChoice = new Integer(0);
      if (!usedPads.contains(firstChoice)) {
        usedPads.add(firstChoice);
        return (0);
      }
    }
    //
    // If we are not growing out horizontally, we are done no matter what:
    //
    
    if (expandDir != NodeProperties.HORIZONTAL_GROWTH) {
      usedPads.add(new Integer(0));
      return (0);      
    }
        
    //
    // Start walking out from the center until we find a free pad:
    //    

    if (startFromLeft) {
      boolean isOnLeft = true;
      int firstOnLeft = ((totalNeeded - 1) / 2) * -4;
      int truePadNum = firstOnLeft;
      while (true) {
        Integer checkChoice = new Integer(truePadNum);
        if (!usedPads.contains(checkChoice)) {
          usedPads.add(checkChoice);
          return (truePadNum);
        }
        if (truePadNum == 0) {
          isOnLeft = false;
          truePadNum = -1;
        } else if (isOnLeft) {
          truePadNum += 4;
        } else {
          truePadNum -= 4;
        }
      }
    } else {
      int truePadNum = -1;
      while (true) {
        int posPadNum = -truePadNum;
        int quadrant = (posPadNum - 1) % 4;
        if ((quadrant == 0) || (quadrant == 3)) {
          Integer checkChoice = new Integer(truePadNum);
          if (!usedPads.contains(checkChoice)) {
            usedPads.add(checkChoice);
            return (truePadNum);
          }
        }
        truePadNum--;
      }
    }
  }
  
  /***************************************************************************
  **
  ** Get a list of pads that are nearby to the given one.  Only goes as far as the 
  ** total bank of pads on the side.  Only use for box and tablet nodes, else override.
  */

  public List<Integer> getNearbyPads(GenomeItem item, int startPad, NodeProperties np) {
    
    
    Node theNode = (Node)item;
    if (!theNode.haveExtraPads()) {
      return (null);
    }
    final int TOP = 0;
    final int RIGHT = 1;
    final int BOTTOM = 2;
    final int LEFT = 3;
    
    Node node = (Node)item;
    boolean horiz = true;
    if (NodeProperties.usesGrowth(node.getNodeType())) {
      horiz = (np.getExtraGrowthDirection() == NodeProperties.HORIZONTAL_GROWTH);
    } 

    MinMax range = getTargetPadRange(item);    
    int side;
    if (startPad >= 0) {
      side = startPad; 
    } else {
      int quadrant = ((-startPad) - 1) % 4;
      switch (quadrant) {
        case 0:  // upper right
          side = (horiz) ? TOP : RIGHT;
          break;
        case 1:  // lower right
          side = (horiz) ? BOTTOM : RIGHT;
          break;
        case 2: // lower left
          side = (horiz) ? BOTTOM : LEFT;
          break;
        case 3: // upper left
          side = (horiz) ? TOP : LEFT;
          break;
        default:
          throw new IllegalStateException();
      }
    }

    int min = range.min; 
    Integer nextPad;
    Integer nextDist;
    int count = 0;
    HashMap<Integer, Integer> distances = new HashMap<Integer, Integer>();
    switch (side) {
      case TOP:
        nextPad = new Integer(0);
        nextDist = new Integer(0);
        distances.put(nextPad, nextDist);
        if (horiz) {
          count = 0;
          for (int i = -1; i >= (min + 3); i -= 4) {
            nextPad = new Integer(i);
            nextDist = new Integer(++count);
            distances.put(nextPad, nextDist);
          }
          count = 0;
          for (int i = -4; i >= min; i -= 4) {
            nextPad = new Integer(i);
            nextDist = new Integer(--count);
            distances.put(nextPad, nextDist);
          }
        }
        break;
      case RIGHT:
        nextPad = new Integer(1);
        nextDist = new Integer(0);
        distances.put(nextPad, nextDist);
        if (!horiz) {
          count = 0;
          for (int i = -1; i >= (min + 3); i -= 4) {
            nextPad = new Integer(i);
            nextDist = new Integer(++count);
            distances.put(nextPad, nextDist);
          }
          count = 0;
          for (int i = -2; i >= (min + 2); i -= 4) {
            nextPad = new Integer(i);
            nextDist = new Integer(--count);
            distances.put(nextPad, nextDist);
          }
        }
        break;
      case BOTTOM:
        nextPad = new Integer(2);
        nextDist = new Integer(0);
        distances.put(nextPad, nextDist);
        if (horiz) {
          count = 0;
          for (int i = -2; i >= (min + 2); i -= 4) {
            nextPad = new Integer(i);
            nextDist = new Integer(++count);
            distances.put(nextPad, nextDist);
          }
          count = 0;
          for (int i = -3; i >= (min + 1); i -= 4) {
            nextPad = new Integer(i);
            nextDist = new Integer(--count);
            distances.put(nextPad, nextDist);
          }
        }
        break;
      case LEFT:
        nextPad = new Integer(3);
        nextDist = new Integer(0);
        distances.put(nextPad, nextDist);
        if (!horiz) {
          count = 0;
          for (int i = -3; i >= (min + 1); i -= 4) {
            nextPad = new Integer(i);
            nextDist = new Integer(++count);
            distances.put(nextPad, nextDist);
          }
          count = 0;
          for (int i = -4; i >= min; i -= 4) {
            nextPad = new Integer(i);
            nextDist = new Integer(--count);
            distances.put(nextPad, nextDist);
          }
        }
        break;
      default:
        throw new IllegalStateException();
    }
    
    return (alternationSupport(distances, startPad));
  }  
  
  /***************************************************************************
  **
  ** Get an alternating list of pads of increasing distance
  */

  protected List<Integer> alternationSupport(Map<Integer, Integer> distances, int startPad) {
    
    int baseDist = distances.get(new Integer(startPad)).intValue();
    TreeMap<Integer, Integer[]> byDist = new TreeMap<Integer, Integer[]>();
    Iterator<Integer> kit = distances.keySet().iterator();
    while (kit.hasNext()) {
      Integer pad = kit.next();
      Integer dist = distances.get(pad);
      int delta = dist.intValue() - baseDist;
      Integer delObj = new Integer(Math.abs(delta));
      Integer[] posNeg = byDist.get(delObj);
      if (posNeg == null) {
        posNeg = new Integer[2];
        byDist.put(delObj, posNeg);
      }
      posNeg[(delta >= 0) ? 0 : 1] = pad;
    }
    
    ArrayList<Integer> retval = new ArrayList<Integer>();   
    Iterator<Integer[]> vit = byDist.values().iterator();
    while (vit.hasNext()) {
      Integer[] posNeg = vit.next();
      if (posNeg[0] != null) {
        retval.add(posNeg[0]);
      }
      if (posNeg[1] != null) {
        retval.add(posNeg[1]);
      }      
    }

    return (retval);  
  }
  
  /***************************************************************************
  **
  ** Compare pads (negative if one is to the left of two).  This version works
  ** for simnple linear numbering schemes (e.g. genes)
  */  
  
  public int comparePads(int padOne, int padTwo) {
    return (padOne - padTwo);
  } 
    
  /***************************************************************************
  **
  ** Get the recommended launch and landing pads
  */
  
  public NodeInsertionDirective getInsertionDirective(Vector2D travel, Point2D insertion) {
    //
    // This approach is useful for most box-type nodes (including bubbles & diamonds).
    // MUST override for things like genes, slashes and intercells
    //
    if (travel.getX() == 0.0) {
      if (travel.getY() < 0.0) {
        return (new NodeInsertionDirective(2, 0));
      } else {
        return (new NodeInsertionDirective(0, 2));
      }
    } else if (travel.getY() == 0.0) {
      if (travel.getX() < 0.0) {
        return (new NodeInsertionDirective(1, 3));
      } else {
        return (new NodeInsertionDirective(3, 1));
      }      
    } else if (Math.abs(travel.getX()) >= Math.abs(travel.getY())) {  // mostly horizontal
      if (travel.getX() > 0) {
        return (new NodeInsertionDirective(3, 1));
      } else {
        return (new NodeInsertionDirective(1, 3));      
      }
    } else {  // mostly vertical
      if (travel.getY() > 0) {
        return (new NodeInsertionDirective(0, 2));
      } else {
        return (new NodeInsertionDirective(2, 0));      
      }     
    }
  } 
  
  /***************************************************************************
  **
  ** Return a Rectangle used to bound this item in a net module rendering.
  */
  
  public Rectangle getModuleBounds(GenomeItem item, DataAccessContext rcx,
                                   int extraPad, Object miscInfo) {
    Rectangle basic = getBounds(item, rcx, miscInfo);
    basic.setBounds(basic.x - extraPad, basic.y - extraPad, 
                    basic.width + (2 * extraPad), basic.height + (2 * extraPad));
    UiUtil.force2DToGrid(basic, UiUtil.GRID_SIZE_INT);
    return (basic);
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Useful for subclasses that spiral
  */  
  
  protected int spiralComparePads(int padOne, int padTwo) {
    if (padOne == padTwo) {
      return (0);
    }
    //
    // Assign an x:
    //
    
    int oneX;
    int oneY;
    if (padOne >= 0) {
      switch (padOne) {
        case 0:
          oneX = 0;
          oneY = Integer.MIN_VALUE;
          break;
        case 2:
          oneX = 0;
          oneY = Integer.MAX_VALUE;
          break;
        case 1:
          oneX = Integer.MAX_VALUE;
          oneY = 0;
          break; 
        case 3:
          oneX = Integer.MIN_VALUE;
          oneY = 0;
          break;
        default:
          throw new IllegalArgumentException();
      }      
    } else {
      Vector2D offOne = getExtraLandingPadOffset(padOne, 0.0, true, 10.0);
      oneX = (int)offOne.getX();
      oneY = (int)offOne.getY();      
    }
    
    int twoX;
    int twoY;
    if (padTwo >= 0) {
      switch (padTwo) {
        case 0:
          twoX = 0;
          twoY = Integer.MIN_VALUE;
          break;
        case 2:
          twoX = 0;
          twoY = Integer.MAX_VALUE;          
          break;
        case 1:
          twoX = Integer.MAX_VALUE;
          twoY = 0;          
          break; 
        case 3:
          twoX = Integer.MIN_VALUE;
          twoY = 0;          
          break;
        default:
          throw new IllegalArgumentException();
      }      
    } else {
      Vector2D offTwo = getExtraLandingPadOffset(padTwo, 0.0, true, 10.0);
      twoX = (int)offTwo.getX();
      twoY = (int)offTwo.getY();      
    }
    //
    // If the two X vaues are the same, but the pads are different, we MUST return
    // different values.
    
    if (twoX == oneX) {
      return ((twoY > oneY) ? -1 : 1);
    }
    
    return ((twoX > oneX) ? -1 : 1);
  }    
  
  /***************************************************************************
  **
  ** Get the arrival direction for extra pads:
  */
  
  protected Vector2D getExtraLandingPadArrivalDirection(int padNum, GenomeItem item, Layout layout) {
    if (padNum >= 0) {
      throw new IllegalArgumentException();
    }
    Node node = (Node)item;
    boolean horiz = true;
    if (NodeProperties.usesGrowth(node.getNodeType())) {
      NodeProperties np = layout.getNodeProperties(item.getID());
      horiz = (np.getExtraGrowthDirection() == NodeProperties.HORIZONTAL_GROWTH);
    }
    
    padNum = -padNum;
    int quadrant = (padNum - 1) % 4;
    switch (quadrant) {
      case 0:  // upper right
        return ((horiz) ? new Vector2D(0.0, 1.0) : new Vector2D(-1.0, 0.0));
      case 1:  // lower right
        return ((horiz) ? new Vector2D(0.0, -1.0) : new Vector2D(-1.0, 0.0));
      case 2: // lower left
        return ((horiz) ? new Vector2D(0.0, -1.0) : new Vector2D(1.0, 0.0));
      case 3: // upper left
        return ((horiz) ? new Vector2D(0.0, 1.0) : new Vector2D(1.0, 0.0));        
      default:
        throw new IllegalStateException();
    }
  }  

  /***************************************************************************
  **
  ** Handle selection highlighting
  */
  
  protected void selectionSupport(SelectedModalShapeContainer group, Intersection selected, 
      int x, int y, int width, int height, boolean forWeb) {
  	
  	Integer majorLayer = NodeRenderBase.NODE_MAJOR_LAYER;
  	Integer minorLayer = NodeRenderBase.NODE_MINOR_LAYER;
  	
		if (selected != null || forWeb) {
      ModelObjectCache.Rectangle rect = new ModelObjectCache.Rectangle(x, y, width, height,
      		DrawMode.FILL, Color.orange, new BasicStroke());
      
      group.addShapeSelected(rect, majorLayer, minorLayer);
		}
  }

  /***************************************************************************
  **
  ** Handle selection highlighting
  */
  
  protected void roundSelectionSupport(SelectedModalShapeContainer group, Intersection selected, double x, double y, double radius, boolean forWeb) {
  	Integer majorLayer = NodeRenderBase.NODE_MAJOR_LAYER;
  	Integer minorLayer = NodeRenderBase.NODE_MINOR_LAYER;
  	
  	if (selected != null || forWeb) {
      // TODO stroke
      ModelObjectCache.Ellipse circ = new ModelObjectCache.Ellipse(x, y, radius, DrawMode.FILL, Color.orange, new BasicStroke());
      group.addShapeSelected(circ, majorLayer, minorLayer);
    }    
    return;
  }
  
 /***************************************************************************
  **
  ** Render the node to a pattern grid
  */
  
  public void renderToPatternGrid(GenomeItem item, DataAccessContext rcx, PatternGrid grid) {
    Rectangle rect = getBounds(item, rcx, null);
    Pattern pat = new Pattern(rect.width / 10, rect.height / 10);
    pat.fillAll(item.getID());    
    grid.place(pat, rect.x / 10, rect.y / 10);
    return;
  }
  
 /***************************************************************************
  **
  ** Render the node to a placement grid
  */
  
  public void renderToPlacementGrid(GenomeItem item, 
                                    DataAccessContext rcx, 
                                    LinkPlacementGrid grid, Map<String, Integer> targetCounts, 
                                    Map<String, Integer> minPads, int strictness) {
    Rectangle rect = getBounds(item, rcx, null);
    Point2D forcedUL = new Point2D.Double();
    UiUtil.forceToGrid(rect.x, rect.y, forcedUL, 10.0);
    Point2D forcedLR = new Point2D.Double();
    UiUtil.forceToGrid(rect.x + rect.width, rect.y + rect.height, forcedLR, 10.0);
    // Off centering requires us to pad more on the left and bottom
    // WJRL 5/1/08 Why 30??? That makes for lots of unplaced links...  reduce it
    int extra;
    int offset;
    if ((strictness == STRICT) || (strictness == MODULE_PADDED)) {
      extra = 30;
      offset = 10;
    } else {
      extra = 10;
      offset = 0;
    }

    Pattern pat = new Pattern((((int)(forcedLR.getX() - forcedUL.getX())) + extra) / 10, 
                              (((int)(forcedLR.getY() - forcedUL.getY())) + extra) / 10);
    pat.fillAll(item.getID());    
    grid.addNode(pat, null, item.getID(), ((int)forcedUL.getX() - offset) / 10,
                                          ((int)forcedUL.getY() - offset) / 10, strictness);    
    return;
  }
  
  /***************************************************************************
  **
  ** Get the landing pad offset
  */
  
  public abstract Vector2D getLandingPadOffset(int padNum, GenomeItem item, int sign, DataAccessContext icx);  
  
  /***************************************************************************
  **
  ** Get dims of _first line_ of text
  */
  
  protected Dimension firstLineBounds(FontRenderContext frc, String text, boolean hideName, Font mFont, String breakDef, FontManager fmgr) {
    return (MultiLineRenderSupport.firstLineBounds(fmgr, frc, text, hideName, mFont, breakDef));
  }  
  
  /***************************************************************************
  **
  ** Render a piece of text, possibly with superscripts, possibly with
  ** line breaks, but not both.  Return the centerline of the end of the text
  */
  
  protected Point2D renderText(ModalShapeContainer group, float x, float y, String text, Color color, 
                               boolean hideName, AnnotatedFont amFont, String breakDef, FontRenderContext frc, FontManager fmgr, ModalTextShapeFactory textFactory) {
    return (MultiLineRenderSupport.renderText(fmgr, frc, textFactory, group, x, y, text, hideName, amFont, color, breakDef));
  }

  /***************************************************************************
  **
  ** Return the Rectangle used by the text
  */
  
  protected Rectangle2D getTextBounds(FontRenderContext frc, float x, float y, String text, 
                                      boolean hideName, AnnotatedFont amFont, String breakDef, FontManager fmgr) {
    return (MultiLineRenderSupport.getTextBounds(fmgr, frc, x, y, text, hideName, amFont, breakDef));
  } 

  /***************************************************************************
  **
  ** Return the dimension used by the text (non-multi-line case)
  */
  
  private Dimension getSimpleTextDims(FontRenderContext frc, String text, boolean hideName, Font mFont, String breakDef, FontManager fmgr) {
    return (MultiLineRenderSupport.getSimpleTextDims(fmgr, frc, text, hideName, mFont, breakDef));
  }    
  
  /***************************************************************************
  **
  ** Return the Rectangle used by the text (non-multi-line case)
  */
  
 // private Rectangle2D getSimpleTextBounds(FontRenderContext frc, float x, float y, String text, boolean hideName, Font mFont, String breakDef) {
  //  Dimension dim = getSimpleTextDims(frc, text, hideName, mFont, breakDef);
   // return (new Rectangle2D.Double(x, y - (dim.getHeight() * 0.8), dim.getWidth(), dim.getHeight()));
 // }  
  
  /***************************************************************************
  **
  ** Get the landing pad offset
  */
  
  protected Vector2D getExtraLandingPadOffset(int padNum, double offVal, boolean horizontal, double padSpacing) {
    if (padNum >= 0) {
      throw new IllegalArgumentException();
    }
    padNum = -padNum;
    int spiral = ((padNum - 1)/ 4) + 1;
    int quadrant = (padNum - 1) % 4;
    double spiralOffset = spiral * padSpacing;
    switch (quadrant) {
      case 0:  // upper right
        return ((horizontal) ? new Vector2D(spiralOffset, -offVal) : new Vector2D(offVal, -spiralOffset));
      case 1:  // lower right
        return ((horizontal) ? new Vector2D(spiralOffset, offVal) : new Vector2D(offVal, spiralOffset));
      case 2: // lower left
        return ((horizontal) ? new Vector2D(-spiralOffset, offVal) : new Vector2D(-offVal, spiralOffset));
      case 3: // upper left
        return ((horizontal) ? new Vector2D(-spiralOffset, -offVal) : new Vector2D(-offVal, -spiralOffset));
      default:
        throw new IllegalStateException();
    }
  } 
  
  /***************************************************************************
  **
  ** Render the launch and landing pads
  */
  
  protected void renderPads(ModalShapeContainer group, Color col, GenomeItem item, 
                            int maxPads, double padWidth, DataAccessContext rcx) {
  	
  	Integer majorLayer = NodeRenderBase.NODE_MAJOR_LAYER;
  	Integer minorLayer = NodeRenderBase.NODE_MINOR_LAYER;
  	
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    Point2D origin = np.getLocation();
        
    double padRadius = (padWidth / 2.0) + 1.0;
    //
    // Regular pads:
    //
    for (int i = 0; i < maxPads; i++) {
      Vector2D lpo = getLandingPadOffset(i, item, Linkage.POSITIVE, rcx);
      double x = origin.getX() + lpo.getX();
      double y = origin.getY() + lpo.getY();
      ModelObjectCache.Ellipse circ = new ModelObjectCache.Ellipse(x, y, padRadius,
                                            DrawMode.DRAW, col, new BasicStroke(1));
      group.addShape(circ, majorLayer, minorLayer);
    }
    //
    // Extra pads:
    //
    Node theBox = (Node)item;
    int numPads = theBox.getPadCount();
    int defaultPads = DBNode.getDefaultPadCount(theBox.getNodeType());
    if (numPads > defaultPads) {
      int extraPads = numPads - defaultPads;
      for (int i = 1; i <= extraPads; i++) {
        int negPadNum = -i;
        Vector2D lpo = getLandingPadOffset(negPadNum, item, Linkage.POSITIVE, rcx);
        double x = origin.getX() + lpo.getX();
        double y = origin.getY() + lpo.getY();
        ModelObjectCache.Ellipse circ = new ModelObjectCache.Ellipse(x, y, padRadius,
                                              DrawMode.DRAW, col, new BasicStroke(1));
        group.addShape(circ, majorLayer, minorLayer);
      }
    }
  }
   
  /***************************************************************************
  **
  ** Figure out which pad we intersect 
  */
  
  protected List<Intersection.PadVal> calcSharedNamespacePadIntersectSupport(GenomeItem item,                                                                             
                                                                             Point2D pt, int maxPads, 
                                                                             double padWidth, 
                                                                             DataAccessContext icx) {

    if (!sharedPadNamespaces()) {
      throw new IllegalStateException();
    }
    ArrayList<Intersection.PadVal> retval = new ArrayList<Intersection.PadVal>();
    NodeProperties np = icx.getLayout().getNodeProperties(item.getID());
    Point2D origin = np.getLocation();
    double padRadius = (padWidth / 2.0) + 1.0;
    double prSq = padRadius * padRadius;
    double px = pt.getX();
    double py = pt.getY();
    for (int i = 0; i < maxPads; i++) {
      Vector2D lpo = getLandingPadOffset(i, item, Linkage.POSITIVE, icx);
      double x = origin.getX() + lpo.getX();
      double y = origin.getY() + lpo.getY();
      double distSq = ((px - x) * (px - x)) + ((py - y) * (py - y));
      if (distSq <= prSq) {
        Intersection.PadVal retpad = new Intersection.PadVal();
        retpad.okEnd = true;
        retpad.okStart = true;
        retpad.padNum = i;
        retpad.distance = Math.sqrt(distSq);
        retval.add(retpad);
      }
    }
    
    //
    // Extra pads:
    //
    Node theBox = (Node)item;
    int numPads = theBox.getPadCount();
    int defaultPads = DBNode.getDefaultPadCount(theBox.getNodeType());
    if (numPads > defaultPads) {
      int extraPads = numPads - defaultPads;
      for (int i = 1; i <= extraPads; i++) {
        int negPadNum = -i;
        Vector2D lpo = getLandingPadOffset(negPadNum, item, Linkage.POSITIVE, icx);
        double x = origin.getX() + lpo.getX();
        double y = origin.getY() + lpo.getY();
        double distSq = ((px - x) * (px - x)) + ((py - y) * (py - y));
        if (distSq <= prSq) {
          Intersection.PadVal retpad = new Intersection.PadVal();
          retpad.okEnd = true;
          retpad.okStart = true;
          retpad.padNum = negPadNum;
          retpad.distance = Math.sqrt(distSq);
          retval.add(retpad);
        }
      }
    }    
    return ((retval.isEmpty()) ? null : retval);
  }  

  /***************************************************************************
  **
  ** Quickly answers if the point is a possible text intersection candidate
  */
  
  protected boolean isTextCandidate(FontRenderContext frc, int fontType, FontManager.FontOverride fo, 
                                    String text, double xPad, double dx, double distSq, String breakDef, 
                                    boolean canBeLeft, boolean nameHidden, FontManager fmgr) {
    return (MultiLineRenderSupport.isTextCandidate(fmgr, frc, fontType, fo, text, xPad, dx, distSq, breakDef, canBeLeft, nameHidden));
  }
  
  /***************************************************************************
  **
  ** Quickly answers if a node name is just a single line, with no subscripts!
  */  
  
  protected boolean isSingleLineText(String text, boolean hideName, String breakDef) {
    return (MultiLineRenderSupport.isSingleLineText(text, hideName, breakDef));
  }
       
  /***************************************************************************
  **
  ** Answer if the given point intersects the text label
  */
  
  protected boolean intersectTextLabel(FontRenderContext frc, float x, float y, 
                                       String text, Point2D intPt, boolean hideName,
                                       int fontType, FontManager.FontOverride fo, String breakDef, FontManager fmgr) {
    return (MultiLineRenderSupport.intersectTextLabel(fmgr, frc, x, y, text, intPt, hideName, fontType, fo, breakDef));    
  }
  
  protected void fillBoundsArrayTextLabel(ArrayList<ModelObjectCache.ModalShape> targetArray, DataAccessContext rcx, float x, float y, 
                                       String text, boolean hideName,
                                       int fontType, FontManager.FontOverride fo, String breakDef, FontManager fmgr) {
  	MultiLineRenderSupport.fillBoundsArrayTextLabel(targetArray, x, y, text, hideName, fontType, breakDef, rcx.getFrc(), rcx.fmgr);
  }

  /***************************************************************************
  **
  ** Return the Rectangle bounding the area where we do not want to allow network
  ** expansion.  May be null.
  */
  
  public Rectangle getNonExpansionRegion(GenomeItem item, DataAccessContext rcx) {
    return (null);
  }
  
  /***************************************************************************
  **
  ** For subclasses that implement extra pad banks
  */
  
  protected Rectangle getExtraPadNonExpansionRegion(GenomeItem item, DataAccessContext rcx) {
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    Point2D origin = np.getLocation();
    double minX = origin.getX();
    double minY = origin.getY();
    double maxX = minX;
    double maxY = minY;
    
    //
    // Disallow any expansion between occupied launch and landing pads
    //
    
    Iterator<Linkage> lit = rcx.getGenome().getLinkageIterator();
    String myId = item.getID();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      // This introduces diagonals on recompression following link additions.
      if (rcx.getLayout().getLinkProperties(link.getID()) == null) {  // can use on incomplete layouts...
        continue;
      }
      String src = link.getSource();
      if (src.equals(myId)) {
        Vector2D lpo = getLaunchPadOffset(link.getLaunchPad(), item, rcx);
        Point2D lPad = lpo.add(origin);
        double padX = lPad.getX();
        double padY = lPad.getY();
        if (padX > maxX) maxX = padX;
        if (padY > maxY) maxY = padY;
        if (padX < minX) minX = padX;
        if (padY < minY) minY = padY;        
      }
      String trg = link.getTarget();
      if (trg.equals(myId)) {
        Vector2D lpo = getLandingPadOffset(link.getLandingPad(), item, link.getSign(), rcx);
        Point2D lPad = lpo.add(origin);
        double padX = lPad.getX();
        double padY = lPad.getY();
        if (padX > maxX) maxX = padX;
        if (padY > maxY) maxY = padY;
        if (padX < minX) minX = padX;
        if (padY < minY) minY = padY;        
      }
    }
    if ((maxX == minX) && (maxY == minY)) {
      return (null);
    }
    return (new Rectangle((int)minX, (int)minY, (int)maxX - (int)minX, (int)maxY - (int)minY));
  }
  
  /***************************************************************************
  **
  ** Get a variable activity color
  */
  
  protected Color getVariableActivityColor(GenomeItem item, Color col, boolean forText, DisplayOptions dopt) {
    
    if (item instanceof DBNode) {  // Root items don't do variable activity
      return (col);
    }
   
    NodeInstance ni = (NodeInstance)item;    
    int activity = ni.getActivity();
    switch (activity) {
      case NodeInstance.INACTIVE:
        return (dopt.getInactiveGray());
      case NodeInstance.ACTIVE:
        return (col);
      case NodeInstance.VARIABLE:
      case NodeInstance.VESTIGIAL:
        if ((activity == NodeInstance.VESTIGIAL) && forText) {
          return (col);
        }
        if (!dopt.changeNodeColorForActivity()) {
          return (col);
        } 
        float[] colHSB = new float[3];    
        Color.RGBtoHSB(col.getRed(), col.getGreen(), col.getBlue(), colHSB);
        double level = (activity == NodeInstance.VESTIGIAL) ? 0.0 : ni.getActivityLevel();
        float[] iag = dopt.getInactiveGrayHSV();
        // Hue stays the same:
        colHSB[1] = iag[1] + ((float)level * (colHSB[1] - iag[1])); 
        colHSB[2] = iag[2] + ((float)level * (colHSB[2] - iag[2]));         
        return (Color.getHSBColor(colHSB[0], colHSB[1], colHSB[2]));        
      default:
        throw new IllegalStateException();
    }    
  }  
  
  /***************************************************************************
  **
  ** Draw variable activity pie
  */
  
  protected void drawVariableActivityPie(ModalShapeContainer group, GenomeItem item, Color col, 
      Point2D pieCenter, DisplayOptions dopt) {
  	
  	Integer majorLayer = NodeRenderBase.NODE_MAJOR_LAYER;
  	Integer minorLayer = NodeRenderBase.NODE_MINOR_LAYER;
  	
    if (item instanceof DBNode) {  // Root items don't do variable activity
      return;
    }
    
    if (!dopt.showNodePieForActivity()) {
      return;
    }
    
    NodeInstance ni = (NodeInstance)item;        
    int activity = ni.getActivity();    
    if (activity != NodeInstance.VARIABLE) {
      return;
    }
    
    double x = pieCenter.getX();
    double y = pieCenter.getY();    

    ModelObjectCache.Ellipse circ = new ModelObjectCache.Ellipse(x, y, PIE_RADIUS, DrawMode.DRAW, col, new BasicStroke(PIE_STROKE));
    
    group.addShape(circ, majorLayer, minorLayer);
    
    double level = ni.getActivityLevel();
    int endArc = (int)Math.round(360.0 * level);
    
    // TODO correct stroke
    ModelObjectCache.Arc arc1 = new ModelObjectCache.Arc(x, y, PIE_RADIUS, 90, -endArc,
                                  Type.PIE, DrawMode.FILL, col, new BasicStroke(PIE_STROKE));

    group.addShape(arc1, majorLayer, minorLayer);
  }
}
