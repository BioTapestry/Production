/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.ui.dialogs;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.util.List;
import java.util.Set;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.FreezeDriedOverlayOracle;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.IntersectionChooser;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ConcreteGraphicsCache;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.DrawLayer;

/***************************************************************************
** 
** Adds object selection on the simple model view panel
*/

public class ClickableModelViewPanel extends ModelViewPanel implements MouseClickHandlerTarget {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private ClickableClient client_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private boolean allGhosted_;
  private String linkTargetKey_;
  private static final long serialVersionUID = 1L;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Constructor
  */
  
  public ClickableModelViewPanel(BTState appState, ClickableClient client, DataAccessContext rcx) {
    super(appState, rcx);
    client_ = client;
    allGhosted_ = false;
    addMouseListener(new MouseClickHandler(appState_, this));
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

  public void setTargets(Set targetSet, String linkTargetKey) {
    if (targetSet.isEmpty() && (linkTargetKey == null)) {
      allGhosted_ = true;
      myGenomePre_.clearTargets();
      linkTargetKey_ = null;
    } else {
      allGhosted_ = false;
      myGenomePre_.setTargets(targetSet);
      linkTargetKey_ = linkTargetKey;
    }
    return;
  }    
  
  /***************************************************************************
  **
  ** Handle clicks
  */
  
  public void clickResult(int x, int y) {
    Point pt = new Point(x, y);
    zoomer_.transformClick(x, y, pt);
    x = (int)(Math.round(pt.x / 10.0) * 10.0);
    y = (int)(Math.round(pt.y / 10.0) * 10.0);
    DataAccessContext rcxC = new DataAccessContext(rcx_);
    rcxC.pixDiam = zoomer_.currentPixelDiameter(); 
    rcxC.oso = new FreezeDriedOverlayOracle(null, null, NetModuleFree.CurrentSettings.NOTHING_MASKED, null);
    
    List<Intersection.AugmentedIntersection>  augs = myGenomePre_.intersectItem(x, y, rcxC, false, false);
    Intersection.AugmentedIntersection ai = (new IntersectionChooser(true, rcxC)).selectionRanker(augs); 
    Intersection inter = ((ai == null) || (ai.intersect == null)) ? null : ai.intersect;
    if (inter == null) {
      return;
    }
    String startID = inter.getObjectID();
    Node node = rcx_.getGenome().getNode(startID);
    if (node != null) {
      client_.nodeIntersected(inter);
      return;
    }    
    Linkage link = rcx_.getGenome().getLinkage(startID);
    if (link != null) {
      client_.linkIntersected(inter);
      return;
    }
    return;
  }

  /***************************************************************************
  **
  ** Handle drags
  */
  
  public void dragResult(int sx, int sy) {
  } 
  
  /***************************************************************************
  **
  ** Drawing guts
  */
  
  protected void drawingGuts(Graphics g) {
    if (rcx_.getGenome() != null) {
      Graphics2D g2 = (Graphics2D)g;   
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
      AffineTransform saveTrans = g2.getTransform();
      zoomer_.installTransform(g2, null);
           
      if (backRect_ != null) {
        g2.setPaint(backCol_);
        g2.fill(backRect_);      
      }
      ConcreteGraphicsCache cgc = new ConcreteGraphicsCache();
      if (!allGhosted_) {
        if (linkTargetKey_ != null) {
        	
        	// Set the draw layer explicitly for each presentRootGenome-call. presetGenome calls
        	// GenomePresentation.presentGenomeWithOverlay, in which draw layers are always set.
        	
        	cgc.setDrawLayer(DrawLayer.VFN_GHOSTED);
        	DataAccessContext rcxR = new DataAccessContext(rcx_, rcx_.getDBGenome());
        	rcxR.pushGhosted(true);
        	rcxR.pixDiam = zoomer_.currentPixelDiameter();
          myGenomePre_.presentRootGenome(cgc, rcxR);

          // show bub, show root, oso, pixdiam
          DataAccessContext rcxT = new DataAccessContext(rcx_, rcx_.getGenomeSource().getGenome(linkTargetKey_), rcx_.getLayout());
          rcxT.pixDiam = zoomer_.currentPixelDiameter();
          rcxT.showBubbles = false;
          rcxT.oso = null;
          
          myGenomePre_.presentGenome(cgc, rcxT, false);
        } else {
          DataAccessContext rcxT = new DataAccessContext(rcx_);
          rcxT.pixDiam = zoomer_.currentPixelDiameter();
          rcxT.showBubbles = false;
          rcxT.oso = null;
          
          myGenomePre_.presentGenome(cgc, rcxT, false);
        }
      } else {
      	cgc.setDrawLayer(DrawLayer.VFN_GHOSTED);
      	DataAccessContext rcxR = new DataAccessContext(rcx_, rcx_.getDBGenome());
        rcxR.pushGhosted(true);
        rcxR.pixDiam = zoomer_.currentPixelDiameter();
        myGenomePre_.presentRootGenome(cgc, rcxR);
      }
      
      // TODO are empty layers being drawn here?
      cgc.renderAllGroupsInDrawLayer(g2, DrawLayer.BACKGROUND_REGIONS);
      cgc.renderAllGroupsInDrawLayer(g2, DrawLayer.UNDERLAY);
      cgc.renderAllGroupsInDrawLayer(g2, DrawLayer.VFN_GHOSTED);
      cgc.renderAllGroupsInDrawLayer(g2, DrawLayer.VFN_UNUSED_REGIONS);
      cgc.renderAllGroupsInDrawLayer(g2, DrawLayer.MODEL_NODEGROUPS);
      cgc.renderAllGroupsInDrawLayer(g2, DrawLayer.FOREGROUND_REGIONS);      
      
      g2.setTransform(saveTrans);
    }
  
    return;
  }   
}
