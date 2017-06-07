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

package org.systemsbiology.biotapestry.ui.dialogs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.nav.ZoomChangeTracker;
import org.systemsbiology.biotapestry.nav.ZoomCommandSupport;
import org.systemsbiology.biotapestry.nav.ZoomTarget;
import org.systemsbiology.biotapestry.ui.FreezeDriedOverlayOracle;
import org.systemsbiology.biotapestry.ui.GenomePresentation;
import org.systemsbiology.biotapestry.ui.IRenderer;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.IntersectionChooser;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.ZoomTargetSupport;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogModelDisplay;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ConcreteGraphicsCache;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.DrawLayer;
import org.systemsbiology.biotapestry.util.UndoSupport;

/***************************************************************************
** 
** Viewer for a model
*/

public class ModelViewPanel extends JPanel implements ZoomTarget, ZoomChangeTracker, DialogModelDisplay {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // protected MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  protected GenomePresentation myGenomePre_;
  protected ZoomTargetSupport zoomer_;
  protected ZoomCommandSupport zcs_;
  protected Rectangle backRect_;
  protected Color backCol_;
  protected TipGenerator tipGen_;
  protected StaticDataAccessContext rcx_;
  protected UIComponentSource uics_;
  
  private static final long serialVersionUID = 1L;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Constructor
  */
  
  public ModelViewPanel(UIComponentSource uics, StaticDataAccessContext rcx) {
    uics_ = uics;
    rcx_ = rcx;
    myGenomePre_ = new GenomePresentation(uics_, false, 1.0, false, rcx_);
    zoomer_ = new ZoomTargetSupport(myGenomePre_, this, rcx_);
    zcs_ = new ZoomCommandSupport(this, uics, null, -1);
    rcx_.setZoomTargetSupport(zoomer_);
    setBackground(Color.white);
    backRect_ = null;
    backCol_ = null;
    tipGen_ = null;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** Tell us the zoom state has changed
  */ 
  
  public void zoomStateChanged(boolean scrollOnly) {
    setCurrClipRect(zcs_.getCurrClipRect());
    return;
  }  

  /***************************************************************************
  ** 
  ** Let us know what the current clip rect is
  */

  public void setCurrClipRect(Rectangle2D clipRect) { 
    zoomer_.setCurrClipRect(clipRect);
    return;
  }
 
  /***************************************************************************
  ** 
  ** Bump to next selection
  */

  public void incrementToNextSelection() { 
    myGenomePre_.bumpNextSelection(rcx_);
    return;
  } 
 
  /***************************************************************************
  **
  ** Answer if we have a current selection
  */
  
  public boolean haveCurrentSelectionForBounds() {
    return (myGenomePre_.haveCurrentSelection());    
  }
 
  /***************************************************************************
  **
  ** Answer if we have multiple selections to cycle through
  */
  
  public boolean haveMultipleSelectionsForBounds() {
    return (myGenomePre_.haveMultipleSelections());    
  }
  
  /***************************************************************************
  ** 
  ** Bump to previous selection
  */

  public void decrementToPreviousSelection() { 
    myGenomePre_.bumpPreviousSelection(rcx_);
    return;
  } 
 
  /***************************************************************************
  **
  ** Set the tool tip generator
  */
  
  public void setToolTipGenerator(TipGenerator tipGen) {
    tipGen_ = tipGen;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the tool tip
  */
  
  @Override
  public String getToolTipText(MouseEvent event) {
    if (tipGen_ == null) {
      return (null);
    }
    Point tipPoint = event.getPoint();
    zoomer_.transformPoint(tipPoint);
    StaticDataAccessContext rcxC = new StaticDataAccessContext(rcx_);
    rcxC.setPixDiam(zoomer_.currentPixelDiameter()); 
    rcxC.setOSO(new FreezeDriedOverlayOracle(null, null, NetModuleFree.CurrentSettings.NOTHING_MASKED, null));
    List<Intersection.AugmentedIntersection> augs = myGenomePre_.intersectItem(tipPoint.x, tipPoint.y, rcxC, false, false);
    Intersection.AugmentedIntersection ai = (new IntersectionChooser(true, rcxC)).selectionRanker(augs);    
    if ((ai == null) || (ai.intersect == null)) {
      return (null);
    }
    String tipVal = tipGen_.generateTip(rcx_.getCurrentGenome(), rcx_.getCurrentLayout(), ai.intersect);
    return (tipVal);
  }
  
  /***************************************************************************
  **
  ** Drawing routine
  */
  
  public void registerScrollPane(JScrollPane jsp) {
    zcs_.registerScrollPaneAndZoomTarget(jsp, this);
    return;
  }  

  /***************************************************************************
  **
  ** Drawing routine
  */
  
  @Override
  public void paintComponent(Graphics g) {
    try {
      super.paintComponent(g);
      drawingGuts(g);
    } catch (Exception ex) {
      uics_.getExceptionHandler().displayPaintException(ex);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** This is what needs to be overridden to handle window resize operations
  */
  
  @Override
  public void setBounds(int x, int y, int width, int height) {
    //myGenomePre_.setSize(width, height);
    super.setBounds(x, y, width, height);
    return;
  }

  /***************************************************************************
  **
  ** Show a background region
  */
  
  public void showBackgroundRegion(Color col, Rectangle rect) {
    backRect_ = rect;
    backCol_ = col;
    return;
  }  
   
  /***************************************************************************
  **
  ** Show the full model
  */
  
  public void showFullModel() {
    if (rcx_.getCurrentGenome() == null) {
      return;
    }
    zcs_.zoomToModel();
    return;
  }
  
  /***************************************************************************
  **
  ** Select nodes and links with no zoom operation
  */
  
  public void selectNodesAndLinks(Set<String> nodeIDs, List<Intersection> selections) {
    myGenomePre_.selectNodesAndLinks(uics_, nodeIDs, rcx_, selections, true, null);
    return;
  }
  
  /***************************************************************************
  **
  ** Select nodes
  */
  
  public void selectNodes(Set<String> nodeIDs) {
    myGenomePre_.selectNodesAndLinks(uics_, nodeIDs, rcx_, null, true, null);
    zcs_.zoomToSelected();
    // Kinda bogus hack:
    zcs_.bumpZoomWrapper('-');
    zcs_.bumpZoomWrapper('-');
    return;
  }
  
  /***************************************************************************
  **
  ** Select link pieces
  */
  
  public void selectLinks(List<Intersection> selections) {
    myGenomePre_.selectNodesAndLinks(uics_, new HashSet<String>(), rcx_, selections, true, null);
    zcs_.zoomToSelected();
    // Kinda bogus hack:
    zcs_.bumpZoomWrapper('-');
    zcs_.bumpZoomWrapper('-');
    return;
  }
  
  /***************************************************************************
  **
  ** Zoom to selected
  */
  
  public void zoomToSelected() {
    zcs_.zoomToSelected();
    return;
  }  
 
  /***************************************************************************
  **
  ** Get zoom support
  */
  
  public ZoomCommandSupport getZoomController() {
    return (zcs_);
  }  
  
  /***************************************************************************
  **
  ** Zoom support
  */  
  
  public Point pointToViewport(Point world) {
    return (zoomer_.pointToViewport(world));    
  }
  
  public Point2D getRawCenterPoint() {
    return (zoomer_.getRawCenterPoint());    
  }
  
  public Rectangle getSelectedBounds() {
    return (zoomer_.getSelectedBounds());    
  }

  public Rectangle getCurrentSelectedBounds() {
    return (zoomer_.getCurrentSelectedBounds());    
  }

  public Rectangle getWorkspaceBounds() {
    return (zoomer_.getWorkspaceBounds());    
  }
  
  public Dimension getPreferredSize() {
    return (zoomer_.getPreferredSize());    
  }
  
  public void fixCenterPoint(boolean doComplete, UndoSupport support, boolean closeIt) {
    zoomer_.fixCenterPoint(doComplete, support, closeIt);
    //throw new UnsupportedOperationException();
  }

  public Dimension getBasicSize(boolean doComplete, boolean doBuffer, int moduleHandling) {
    return (zoomer_.getBasicSize(doComplete, doBuffer, moduleHandling));    
  }
  
  public Point2D viewToWorld(Point vPt) {
    return (zoomer_.viewToWorld(vPt));    
  }
  
  public void setWideZoomFactor(double newZoomVal, Dimension vDim) {
    zoomer_.setWideZoomFactor(newZoomVal, vDim);
    return;
  }
  
  public double getWorkspaceZoom(Dimension viewportDim, double percent) {
    return (zoomer_.getWorkspaceZoom(viewportDim, percent));    
  }
  
  public Rectangle getCurrentBasicBounds(boolean doComplete, boolean doBuffer, int moduleHandling) {
    return (zoomer_.getCurrentBasicBounds(doComplete, doBuffer, moduleHandling));    
  }
  
  public Rectangle getAllModelBounds() {
    return (zoomer_.getAllModelBounds());    
  }
  
  public void adjustWideZoomForSize(Dimension dims) {
    zoomer_.adjustWideZoomForSize(dims);
    return;
  }
  
  public Point getCenterPoint() {
    return (zoomer_.getCenterPoint());    
  }
  
  public void setZoomFactor(double zoom) {
    zoomer_.setZoomFactor(zoom);
    return;
  }
  
  public double getZoomFactor() {
    return (zoomer_.getZoomFactor());    
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INTERFACES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  
  /***************************************************************************
  **
  ** Interface for tooltip generators
  */
  
  public interface TipGenerator {
   public String generateTip(Genome genome, Layout layout, Intersection intersected);
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
    
  /***************************************************************************
  **
  ** Drawing guts
  */
  
  protected void drawingGuts(Graphics g) {
    if (rcx_.getCurrentGenome() != null) {
      Graphics2D g2 = (Graphics2D)g;   
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
      AffineTransform saveTrans = g2.getTransform();
      zoomer_.installTransform(g2, null);
      StaticDataAccessContext rcxC = new StaticDataAccessContext(rcx_);
      rcxC.setPixDiam(zoomer_.currentPixelDiameter());
      rcxC.setShowBubbles(false);
      rcxC.pushGhosted(false);
      rcxC.setOSO(null);
      
      if (backRect_ != null) {
        g2.setPaint(backCol_);
        g2.fill(backRect_);      
      }
      
      ConcreteGraphicsCache cgc = new ConcreteGraphicsCache();
      myGenomePre_.presentGenome(cgc, rcxC, false, IRenderer.Mode.NORMAL);

      cgc.renderAllGroupsInDrawLayer(g2, DrawLayer.BACKGROUND_REGIONS);
      cgc.renderAllGroupsInDrawLayer(g2, DrawLayer.UNDERLAY);
      cgc.renderAllGroupsInDrawLayer(g2, DrawLayer.VFN_GHOSTED);
      cgc.renderAllGroupsInDrawLayer(g2, DrawLayer.VFN_UNUSED_REGIONS);
      cgc.renderAllGroupsInDrawLayer(g2, DrawLayer.MODEL_NODEGROUPS);
      cgc.renderAllGroupsInDrawLayer(g2, DrawLayer.FOREGROUND_REGIONS);
      cgc.renderAllGroupsInDrawLayer(g2, DrawLayer.OVERLAY);
      cgc.renderAllGroupsInDrawLayer(g2, DrawLayer.MODELDATA);
      
      g2.setTransform(saveTrans);
    }
    return;
  } 
}
