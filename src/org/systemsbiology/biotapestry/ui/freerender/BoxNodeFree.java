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
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

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

/****************************************************************************
**
** This renders a box
*/

public class BoxNodeFree extends AbstractRectangleNodeFree {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  private static final int BOX_LINE_ = 4;   

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor
  */

  public BoxNodeFree() {
    super();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Render the box node using the provided layout
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
  	Integer minorLayer = NodeRenderBase.NODE_MINOR_LAYER;
 
    //
    // Modify the ghosted state to include inactive nodes:
    //
        
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    boolean isGhosted = rcx.isGhosted();
    boolean textGhosted = isGhosted;
    if (item instanceof NodeInstance) {
      int activityLevel = ((NodeInstance)item).getActivity();
      isGhosted = isGhosted || (activityLevel == NodeInstance.VESTIGIAL) || (activityLevel == NodeInstance.INACTIVE);
      textGhosted = isGhosted && (activityLevel != NodeInstance.VESTIGIAL);
    }
    
    DisplayOptions dop = rcx.getDisplayOptsSource().getDisplayOptions();
    Color vac = getVariableActivityColor(item, np.getColor(), false, dop);
    Color col = (isGhosted) ? dop.getInactiveGray() : vac;
    Color textCol = getVariableActivityColor(item, Color.BLACK, true, dop);

    AnnotatedFont mFont = rcx.fmgr.getOverrideFont(FontManager.MEDIUM, np.getFontOverride());
    Point2D origin = np.getLocation();

    CommonCacheGroup group = new CommonCacheGroup(item.getID(), item.getName(), "box");
   
    Rectangle2D textBounds = new Rectangle2D.Double();
    Rectangle2D bounds = renderSupportA(group, item, selected, textBounds, rcx);
    
    ModelObjectCache.Rectangle boundsModalShape = new ModelObjectCache.Rectangle(bounds.getX(), bounds.getY(),
    		bounds.getWidth(), bounds.getHeight(),
    		DrawMode.DRAW, col, new BasicStroke(BOX_LINE_));
    
    group.addShape(boundsModalShape, majorLayer, minorLayer);
    
    renderSupportB(group, item, isGhosted, textGhosted,
        mFont, origin, textBounds, col, textCol, textFactory, rcx);
    
    cache.addGroup(group);
    
    setGroupBounds(group, item, rcx);
  }
  
  /*
  public void renderOld(Graphics2D g2, RenderObjectCache cache, Genome genome, GenomeItem item, 
                     Layout layout, Intersection selected, 
                     boolean isGhosted, boolean showBubbles, 
                     Rectangle2D clipRect, double pixDiam, Object miscInfo, ModelObjectCache moc) {
         
    DisplayOptionsManager dopmgr = appState_.getDisplayOptMgr();
    DisplayOptions dopt = dopmgr.getDisplayOptions(); 
    
    //
    // Modify the ghosted state to include inactive nodes:
    //
        
    NodeProperties np = layout.getNodeProperties(item.getID());
    boolean textGhosted = isGhosted;
    if (item instanceof NodeInstance) {
      int activityLevel = ((NodeInstance)item).getActivity();
      isGhosted = isGhosted || (activityLevel == NodeInstance.VESTIGIAL) || (activityLevel == NodeInstance.INACTIVE);
      textGhosted = isGhosted && (activityLevel != NodeInstance.VESTIGIAL);
    }
    Color vac = getVariableActivityColor(item, np.getColor(), false, dopt);
    Color col = (isGhosted) ? dop.getInactiveGray() : vac;
    Color textCol = getVariableActivityColor(item, Color.BLACK, true, dopt);
           
    Font mFont = appState_.getFontMgr().getOverrideFont(FontManager.MEDIUM, np.getFontOverride());
    Point2D origin = np.getLocation();

    Rectangle2D textBounds = new Rectangle2D.Double(); 
    Rectangle2D bounds = renderSupportA(null, item, layout, selected, textBounds);
    g2.setStroke(new BasicStroke(BOX_LINE_));
    g2.setPaint(col);
    g2.draw(bounds);    
    
    renderSupportB(null, item, layout, isGhosted, textGhosted, showBubbles, 
                   mFont, origin, textBounds, col, textCol, dopt);
    return;
  }
  */
  
  /***************************************************************************
  **
  ** Get the pad tweak
  */  
  
  protected double padTweak(int sign) {
  
    float negPosTweak = 0.0F;
    if (sign == Linkage.NEGATIVE) {
      negPosTweak = NEG_OFFSET;
    } else if (sign == Linkage.POSITIVE) {
      negPosTweak = POS_OFFSET;
    }
    return ((BOX_LINE_ / 2.0) + negPosTweak);
  }
}
