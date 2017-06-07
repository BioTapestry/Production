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

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Map;
import javax.swing.JPanel;

import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.cmd.undo.DatabaseChangeCmd;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.db.DatabaseChange;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.Workspace;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.util.TaggedSet;

/***************************************************************************
** 
** Supports operations used in zooming
*/

public class ZoomTargetSupport extends BasicZoomTargetSupport {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private TaggedSet currentNetMods_;
  private String currentOverlay_;
  
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
  ** Constructor. Yes, this class can take either dynamic DACs (full application) or static DACs (model dialogs):
  */
  
  public ZoomTargetSupport(ZoomPresentation genomePre, JPanel paintTarget, DataAccessContext rcx) {
    super(genomePre, paintTarget, rcx);
    currentOverlay_ = null;
    currentNetMods_ = new TaggedSet();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Set the current net overlay
  */
  
  public void setOverlay(String key) {
    currentOverlay_ = key;
    return;
  }  
  
  /***************************************************************************
  **
  ** Set the current network module
  */
  
  public void setCurrentNetModules(TaggedSet keys) {
    currentNetMods_ = new TaggedSet(keys);
    return;
  }
 
  /***************************************************************************
  **
  ** Fixes the center point
  */
  
  @Override
  public void fixCenterPoint(boolean doComplete, UndoSupport support, boolean closeIt) {
    Genome genome = rcx_.getCurrentGenome();
    if ((genome != null) && genome.isEmpty() && (genome.getNetworkModuleCount() == 0)) {
      if (genome instanceof DBGenome) {         
        setRawOrigin(new Point2D.Double(0.0, 0.0), support, closeIt);
        return;
      }
      genome = rcx_.getDBGenome();
      DataAccessContext rcxR = new StaticDataAccessContext(rcx_).getContextForRoot();
     
      if ((genome != null) && genome.isEmpty() && (genome.getNetworkModuleCount() == 0)) {
        setRawOrigin(new Point2D.Double(0.0, 0.0), support, closeIt);
        return;
      }  
      boolean doModules = genome.isEmpty();
      Map<String, Layout.OverlayKeySet> allKeys = (doModules) ? rcxR.getFGHO().fullModuleKeysPerLayout() : null;
      Rectangle rect = myGenomePre_.getRequiredSize(rcxR, doComplete, false, 
                                                    doModules, doModules, null, null, allKeys);
      double cx = rect.getX() + (rect.getWidth() / 2.0);
      double cy = rect.getY() + (rect.getHeight() / 2.0);
      setRawCenterPoint(new Point2D.Double(cx, cy), support, closeIt);
      return;
    }
    //
    // If modules are visible, use the visible ones for centering. If not, use them as a backup
    // if the genome is empty
    //
    boolean doModules = ((genome != null) && genome.isEmpty()) || ((currentOverlay_ != null) && !currentNetMods_.set.isEmpty());
    Map<String, Layout.OverlayKeySet> allKeys = (doModules) ? rcx_.getFGHO().fullModuleKeysPerLayout() : null;
    Rectangle rect = myGenomePre_.getRequiredSize(rcx_, doComplete, false, 
                                                  doModules, doModules, currentOverlay_, currentNetMods_, allKeys);
    double cx = rect.getX() + (rect.getWidth() / 2.0);
    double cy = rect.getY() + (rect.getHeight() / 2.0);
    setRawCenterPoint(new Point2D.Double(cx, cy), support, closeIt);   
    return;
  }
  
  
  /***************************************************************************
  **
  ** Sets raw center point in world coordinates
  */
  
  public void setRawCenterPoint(Point2D rawPt, UndoSupport support, boolean closeIt) {
    Workspace currWS = rcx_.getWorkspaceSource().getWorkspace();
    Workspace ws = new Workspace(currWS, rawPt);
    DatabaseChange dc = rcx_.getWorkspaceSource().setWorkspace(ws);      
    if (support != null) {   
      if (dc != null) {
        DatabaseChangeCmd dcc = new DatabaseChangeCmd(rcx_, dc);
        support.addEdit(dcc);
        if (closeIt) {
          support.addEvent(new LayoutChangeEvent(rcx_.getCurrentLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));  
          support.finish();
        }
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Sets raw origin point in world coordinates
  */
  
  public void setRawOrigin(Point2D rawPt, UndoSupport support, boolean closeIt) {
    Workspace currWS = rcx_.getWorkspaceSource().getWorkspace();
    Workspace ws = currWS.clone();
    ws.setOrigin(rawPt);
    DatabaseChange dc = rcx_.getWorkspaceSource().setWorkspace(ws);      
    if (support != null) {   
      if (dc != null) {
        DatabaseChangeCmd dcc = new DatabaseChangeCmd(rcx_, dc);
        support.addEdit(dcc);
        if (closeIt) {
          support.addEvent(new LayoutChangeEvent(rcx_.getCurrentLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));  
          support.finish();
        }
      }
    }
    return;
  }     

  /***************************************************************************
  **
  ** Get the basic (unzoomed) image size: X
  */
  
  @Override
  public Dimension getBasicSize(boolean doComplete, boolean doBuffer, int moduleHandling) {
    boolean doModules = false;
    String useOverlay = null;
    TaggedSet useModules = null;
    switch (moduleHandling) {
      case NO_MODULES:
        break;
      case VISIBLE_MODULES:
        if ((currentOverlay_ != null) && !currentNetMods_.set.isEmpty()) {
          doModules = true;
          useOverlay = currentOverlay_;
          useModules = currentNetMods_;          
        }
        break;
      case ALL_MODULES:
        doModules = true;
        break;
      default:
        throw new IllegalArgumentException();
    }
    Map<String, Layout.OverlayKeySet> allKeys = (doModules) ? rcx_.getFGHO().fullModuleKeysPerLayout() : null;
    Rectangle origRect = myGenomePre_.getRequiredSize(rcx_, doComplete, 
                                                      doBuffer, doModules, doModules, 
                                                      useOverlay, useModules, allKeys);
    return (new Dimension(origRect.width, origRect.height));
  }
  
  /***************************************************************************
  **
  ** Get the basic model bounds X
  */
  
  @Override
  public Rectangle getCurrentBasicBounds(boolean doComplete, boolean doBuffer, int moduleHandling) {
    boolean doModules = false;
    String useOverlay = null;
    TaggedSet useModules = null;
    switch (moduleHandling) {
      case NO_MODULES:
        break;
      case VISIBLE_MODULES:
        if ((currentOverlay_ != null) && !currentNetMods_.set.isEmpty()) {
          doModules = true;
          useOverlay = currentOverlay_;
          useModules = currentNetMods_;          
        }
        break;
      case ALL_MODULES:
        doModules = true;
        break;
      default:
        throw new IllegalArgumentException();
    }
    Map<String, Layout.OverlayKeySet> allKeys = (doModules) ? rcx_.getFGHO().fullModuleKeysPerLayout() : null;
    return (myGenomePre_.getRequiredSize(rcx_, doComplete, doBuffer, doModules, doModules, 
                                         useOverlay, useModules, allKeys));  
  }  
 
  /***************************************************************************
  **
  ** Get the bounds of all models X
  */ 
  
  @Override
  public Rectangle getAllModelBounds() {  
    Genome root = rcx_.getDBGenome();
    DataAccessContext rcxR = new StaticDataAccessContext(rcx_, root);
    Rectangle rect = getBasicBounds(rcxR, true, false);
    Iterator<GenomeInstance> giit = rcx_.getGenomeSource().getInstanceIterator();
    while (giit.hasNext()) {
      GenomeInstance gi = giit.next();
      DataAccessContext rcxI = new StaticDataAccessContext(rcx_, gi);
      Rectangle modRect = getBasicBounds(rcxI, true, false);
      rect = rect.union(modRect);      
    }    
    return (rect);
  } 
  
  /***************************************************************************
  **
  ** Get the basic model bounds
  */
  
  private Rectangle getBasicBounds(DataAccessContext rcx, boolean doComplete, boolean doBuffer) {  
    Map<String, Layout.OverlayKeySet> allKeys = rcx.getFGHO().fullModuleKeysPerLayout();
    return (myGenomePre_.getRequiredSize(rcx, doComplete, doBuffer, true, true, null, null, allKeys));
  }   
}
