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

package org.systemsbiology.biotapestry.ui.freerender;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.GenomeItem;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.NodeInstance;
import org.systemsbiology.biotapestry.ui.AnnotatedFont;
import org.systemsbiology.biotapestry.ui.DisplayOptions;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.ui.NodeRenderBase;
import org.systemsbiology.biotapestry.ui.modelobjectcache.CommonCacheGroup;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactory;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactoryForDesktop;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactoryForWeb;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.DrawMode;
import org.systemsbiology.biotapestry.util.LinkPlacementGrid;
import org.systemsbiology.biotapestry.util.Pattern;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** This renders a bare node
*/

public class BareNodeFree extends AbstractRectangleNodeFree {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private static final Color backupCol = new Color(230, 230, 230);
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor
  */

  public BareNodeFree() {
    super();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Render the bare node using the provided layout
  */
  
  public void render(ModelObjectCache cache, GenomeItem item, Intersection selected, DataAccessContext rcx, Object miscInfo) {
  	ModalTextShapeFactory textFactory = null;
  	
  	if (rcx.forWeb) {
  		textFactory = new ModalTextShapeFactoryForWeb(rcx.getFrc());
  	}
  	else {
  		textFactory = new ModalTextShapeFactoryForDesktop();
  	}
  	
  	Integer majorLayer = NodeRenderBase.NODE_MAJOR_LAYER;
  	Integer minorLayer = new Integer(0);
  	
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    
    boolean isGhosted = rcx.isGhosted();
    Point2D origin = np.getLocation(); 
    if (item instanceof NodeInstance) {
      int activityLevel = ((NodeInstance)item).getActivity();
      isGhosted = isGhosted || (activityLevel == NodeInstance.VESTIGIAL) || (activityLevel == NodeInstance.INACTIVE);
    }
    DisplayOptions dop = rcx.getDisplayOptsSource().getDisplayOptions();
    Color vac = getVariableActivityColor(item, np.getColor(), false, dop);
    Color col = (isGhosted) ? dop.getInactiveGray() : vac;
    AnnotatedFont mFont = rcx.fmgr.getOverrideFont(FontManager.MEDIUM, np.getFontOverride());

    CommonCacheGroup group = new CommonCacheGroup(item.getID(), item.getName(), "bare");
    
    Rectangle2D textBounds = new Rectangle2D.Double(); 
    Rectangle2D bounds = renderSupportA(group, item, selected, textBounds, rcx);    
    
    //
    // If pads go beyond text, we don't want this floating around in nothingness:
    //
    
    if (mightHaveExtraPadWidth(item) && haveExtraPadWidth(item, rcx.getFrc(), np, rcx.fmgr)) {
    	// TODO fix stroke
    	ModelObjectCache.Rectangle rect = new ModelObjectCache.Rectangle(bounds.getMinX(), bounds.getMinY(),
    			bounds.getWidth(), bounds.getHeight(), DrawMode.FILL, backupCol, new BasicStroke());
    	group.addShape(rect, majorLayer, minorLayer);
    }
    
    renderSupportB(group, item, isGhosted, isGhosted, mFont, origin, textBounds, col, col, textFactory, rcx);
    
    setGroupBounds(group, item, rcx);
    
    cache.addGroup(group);
    
    return;  	
  }
 
  /*
  public void renderOld(Graphics2D g2, RenderObjectCache cache, GenomeItem item, Intersection selected, 
                       DataAccessContext rcx, Object miscInfo, ModelObjectCache moc) {
    
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    Point2D origin = np.getLocation(); 
    boolean isGhosted = rcx.isGhosted();
    if (item instanceof NodeInstance) {
      int activityLevel = ((NodeInstance)item).getActivity();
      isGhosted = isGhosted || (activityLevel == NodeInstance.VESTIGIAL) || (activityLevel == NodeInstance.INACTIVE);
    }
    Color vac = getVariableActivityColor(item, np.getColor(), false, rcx.dopt);
    Color col = (isGhosted) ? dop.getInactiveGray() : vac;
    AnnotatedFont mFont = rcx.fmgr.getOverrideFont(FontManager.MEDIUM, np.getFontOverride());
    Rectangle2D textBounds = new Rectangle2D.Double(); 
    Rectangle2D bounds = renderSupportA(null, item, selected, textBounds, rcx);    

    //
    // If pads go beyond text, we don't want this floating around in nothingness:
    //
    
    if (mightHaveExtraPadWidth(item) && haveExtraPadWidth(item, g2.getFontRenderContext(), np, rcx.fmgr)) {
      g2.setPaint(backupCol);
      g2.fill(bounds);
    }
    
    renderSupportB(null, item, isGhosted, isGhosted,
                   mFont, origin, textBounds, col, col, rcx);    
    return;
  }
  */
  
  /***************************************************************************
  **
  ** Render the node to a placement grid.  With bare nodes, LAX rendering
  ** makes it invisible, except for pads.
  */
  
  @Override
  public void renderToPlacementGrid(GenomeItem item, 
                                    DataAccessContext rcx, 
                                    LinkPlacementGrid grid, Map<String, Integer> targetCounts, 
                                    Map<String, Integer> minPads, int strictness) {
     
    if ((strictness == STRICT) || (strictness == MODULE_PADDED)) {
      super.renderToPlacementGrid(item, rcx, grid, targetCounts, minPads, strictness);
      return;
    }
      
    HashSet<Integer> usedPads = new HashSet<Integer>();    
    String myID = item.getID();
    Iterator<Linkage> lit = rcx.getGenome().getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String targ = link.getTarget();
      if (targ.equals(myID)) {
        int pad = link.getLandingPad();
        usedPads.add(new Integer(pad));
      }
      String src = link.getSource();
      if (src.equals(myID)) {
        int pad = link.getLaunchPad();
        usedPads.add(new Integer(pad));
      } 
    }
    
    if (usedPads.isEmpty()) {
      return;
    }
 
    Rectangle rect = getBounds(item, rcx, null);
    Point2D forcedUL = new Point2D.Double();
    UiUtil.forceToGrid(rect.x, rect.y, forcedUL, 10.0);
    Point2D forcedLR = new Point2D.Double();
    UiUtil.forceToGrid(rect.x + rect.width, rect.y + rect.height, forcedLR, 10.0);
    // Off centering requires us to pad more on the left and bottom
    // WJRL 5/1/08 Why 30??? That makes for lots of unplaced links...  reduce it
    Pattern pat = new Pattern((((int)(forcedLR.getX() - forcedUL.getX())) + 30) / 10, 
                              (((int)(forcedLR.getY() - forcedUL.getY())) + 30) / 10);
    
    int padCenterX = pat.getWidth() / 2;
    int padCenterY = pat.getHeight() / 2;
    Iterator<Integer> pit = usedPads.iterator();
    while (pit.hasNext()) {
      Integer pad = pit.next(); 
      Vector2D offset = getLandingPadOffset(pad.intValue(), item, Linkage.POSITIVE, rcx);
      int xval = ((int)UiUtil.forceToGridValue(offset.getX(), 10.0)) / 10;
      int yval = ((int)UiUtil.forceToGridValue(offset.getY(), 10.0)) / 10;      
      pat.fill(padCenterX + xval, padCenterY + yval, item.getID());
    }  
    grid.addNode(pat, null, item.getID(), ((int)forcedUL.getX() - 10) / 10,
                                          ((int)forcedUL.getY() - 10) / 10, strictness);
    return;
  }
}
