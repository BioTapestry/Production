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

import java.awt.geom.Point2D;
import java.util.SortedSet;
import java.util.Map;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Set;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.GenomeItem;
import org.systemsbiology.biotapestry.util.PatternGrid;
import org.systemsbiology.biotapestry.util.LinkPlacementGrid;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** Interface for node renderers in BioTapestry
*/

public interface INodeRenderer extends IRenderer {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  

  //
  // Layout to grid levels of strictness:
  //

  public static final int MODULE_PADDED = 0;  
  public static final int STRICT        = 1;
  public static final int MEDIUM        = 2;
  public static final int LAX           = 3;
  public static final int LAX_FOR_ORTHO = 4;
  
  public static final float NEG_OFFSET = 3.0F;
  public static final float POS_OFFSET = 4.0F;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Get our preferred target color (bubbles want to be white)
  */
    
  public String getTargetColor();

  /***************************************************************************
  ** 
  ** Answer if we can be in a simple fan in
  */
    
  public boolean simpleFanInOK();
   
  /***************************************************************************
  ** 
  ** Answer if assigning landing is consistent with direct landing:
  */
    
  public boolean landOKForDirect(int landing);
  
  /***************************************************************************
  ** 
  ** Answer if node ignores force top directive
  */
    
  public boolean ignoreForceTop();
  
  /***************************************************************************
  ** 
  ** Get the left pad number
  */
    
  public int getLeftPad();
  
  /***************************************************************************
  **
  ** Get the actual available source pad range (not checking for occupancy)
  */
  
  public MinMax getSourcePadRange(GenomeItem item);
     
  /***************************************************************************
  **
  ** Get the actual available target pad range (not checking for occupancy)
  */
  
  public MinMax getTargetPadRange(GenomeItem item);
  
  /***************************************************************************
  **
  ** Suggest a sorted set of PadDotRankings for the location, best to worst
  */
    
  public SortedSet<PadDotRanking> suggestLandingPads(GenomeItem item, Layout layout, Point2D loc);
  
  /***************************************************************************
  **
  ** Suggest a sorted set of PadDotRankings for the location, best to worst
  */
  
  public SortedSet<PadDotRanking> suggestLaunchPad(GenomeItem item, Layout layout, Point2D targPt);
  
  /***************************************************************************
  **
  ** Get the Y offset needed to line up nodes with a through link in a straight line
  */
  
  public double getStraightThroughOffset();
  
  /***************************************************************************
  **
  ** Get the X offset needed to line up nodes with centers aligned
  */
  
  public double getVerticalOffset();  
  
  /***************************************************************************
  **
  ** Get the preferred launch direction from the given pad
  */
  
  public Vector2D getDepartureDirection(int padNum, GenomeItem item, Layout layout);

  /***************************************************************************
  **
  ** Get the preferred arrival direction
  */
  
  public Vector2D getArrivalDirection(int padNum, GenomeItem item, Layout layout);
  
  /***************************************************************************
  **
  ** Get the launch pad offset
  */
  
  public Vector2D getLaunchPadOffset(int padNum, GenomeItem item, DataAccessContext icx);

  
  /***************************************************************************
  **
  ** Get the landing pad offset, for the given pad
  */
  
  public Vector2D getLandingPadOffset(int padNum, GenomeItem item, int sign, 
                                      DataAccessContext icx);

  /***************************************************************************
  **
  ** Get the landing pad width
  */
  
  public double getLandingPadWidth(int padNum, GenomeItem item, Layout layout); 
  
  /***************************************************************************
  **
  ** Figure out which pad we intersect 
  */
  
  public List<Intersection.PadVal> calcPadIntersects(GenomeItem item, 
                                                     Point2D pt, DataAccessContext icx);

  
  /***************************************************************************
  **
  ** Get the maximum number of launch pads (classic fixed limits)
  */
  
  public int getFixedLaunchPadMax(); 
  
  
  /***************************************************************************
  **
  ** Get the maximum number of landing pads (classic fixed limits)
  */
  
  public int getFixedLandingPadMax();
  
  /***************************************************************************
  **
  ** Answer if landing pads can overflow
  */
  
  public boolean landingPadsCanOverflow();
  
  /***************************************************************************
  **
  ** Answer if landing and launching pad namespaces are shared
  */
  
  public boolean sharedPadNamespaces();  
  
 /***************************************************************************
  **
  ** Render the node to a pattern grid
  */
  
  public void renderToPatternGrid(GenomeItem item, DataAccessContext rcx, PatternGrid grid);
  
 /***************************************************************************
  **
  ** Render the node to a link placement grid
  */ 
  
  public void renderToPlacementGrid(GenomeItem item, DataAccessContext rcx,                                  
                                    LinkPlacementGrid grid, Map<String, Integer> targetCounts, 
                                    Map<String, Integer> minPads, int strictness); 
  
  /***************************************************************************
  **
  ** Return the Rectangle bounding the area where we do not want to allow network
  ** expansion.  May be null.
  */
  
  public Rectangle getNonExpansionRegion(GenomeItem item, DataAccessContext rcx);
  
  /***************************************************************************
  **
  ** Return the height that is applicable for horizontal-stretched applications
  */
  
  public double getGlyphHeightForLayout(GenomeItem item, DataAccessContext rcx);
  
  /***************************************************************************
  **
  ** Get the bounds for autolayout, where we provide the needed orientation, padcount,
  ** and additional label text if desired.  It is assumed that the growth is horizontal.
  ** The bounds are returned with the placement point at (0,0).  If topPadCount argument
  ** is null, it uses the current node pad state instead.
  */
  
  public Rectangle2D getBoundsForLayout(GenomeItem item, DataAccessContext rcx, 
                                        int orientation, boolean labelToo, Integer topPadCount);
  
  /***************************************************************************
  **
  ** Get the launch pad offset for given layout case.  If topPadCount argument
  ** is null, it uses the current node pad state instead.
  */
  
  public Vector2D getLaunchPadOffsetForLayout(GenomeItem item, DataAccessContext rcx, 
                                              int orientation, Integer topPadCount);
 
  /***************************************************************************
  **
  ** Return the number of top pads on the node associated with the provided
  ** full pad count:
  */
  
  public int topPadCount(int fullPadCount);
  
  /***************************************************************************
  **
  ** Return the width (used for layout templates)
  */
  
  public double getWidth(GenomeItem item, DataAccessContext rcx);

  /***************************************************************************
  **
  ** Answer how much extra length will result from the given pad increase
  */
  
  public double getExtraLength(Node node, Genome genome, int newCount, boolean oneSideOnly);  

  /***************************************************************************
  **
  ** Hand out top pads
  */  
  
  public int getBestTopPad(GenomeItem item, Set<Integer> usedPads, int expandDir, 
                           int totalNeeded, boolean startFromLeft);
 
  /***************************************************************************
  **
  ** Hand out one bottom pad
  */  
  
  public int getBottomPad(GenomeItem item);
  
  /***************************************************************************
  **
  ** Get the recommended launch and landing pads
  */
  
  public NodeInsertionDirective getInsertionDirective(Vector2D travel, Point2D insertion);

  /***************************************************************************
  **
  ** Compare pads (negative if one is to the left of two).
  */  
  
  public int comparePads(int padOne, int padTwo); 
  
  
  /***************************************************************************
  **
  ** Get a list of pads near the given one.  Ordered by distance
  */
  
  public List<Integer> getNearbyPads(GenomeItem item, int startPad, NodeProperties np);  

  /***************************************************************************
  **
  ** Return a Rectangle used to bound this item in a net module rendering
  */
  
  public Rectangle getModuleBounds(GenomeItem item, DataAccessContext rcx,
                                   int extraPad, Object miscInfo);  
   
}
