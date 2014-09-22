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


package org.systemsbiology.biotapestry.ui;

import java.awt.Rectangle;
import java.awt.Font;
import java.awt.geom.Point2D;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.awt.font.FontRenderContext;       
import java.awt.font.LineMetrics;

import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.nav.ZoomTarget;

/****************************************************************************
**
** Helps to locate model data
*/

public class DataLocator {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private static final int TOP_RIGHT_    = 0;
  private static final int TOP_CENTER_   = 1;
  private static final int BOTTOM_RIGHT_ = 2;
  private static final int BOTTOM_LEFT_  = 3;  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private BTState appState_;
  private DataAccessContext dacx_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public DataLocator(BTState appState, DataAccessContext dacx) { 
    appState_ = appState;
    dacx_ = dacx;
  }
  
  /***************************************************************************
  **
  ** Set the data locations in the layouts
  ** 
  */
  
  public void setDataLocations(UndoSupport support, String date, String attrib, List<String> key) {

    //
    // Need to set model data locations for root model and every top-level
    // instance
    //
     
    Font mFont = appState_.getFontMgr().getFont(FontManager.DATE);
    Genome gen = dacx_.getDBGenome();
    setLocationsForGenome(support, date, attrib, key, gen, mFont);
    Iterator<GenomeInstance> giit = dacx_.getGenomeSource().getInstanceIterator();
    while (giit.hasNext()) {
      GenomeInstance gi = giit.next();
      if (gi.getVfgParent() == null) {
        setLocationsForGenome(support, date, attrib, key, gi, mFont);
      }
    }
    return;
  }

  /***************************************************************************
  **
  ** Set the title locations in the layouts
  ** 
  */
  
  public void setTitleLocation(UndoSupport support, String genomeID, String title) {

    //
    // Need to set model data locations just for the layout we care about
    //
    
    Font tFont = appState_.getFontMgr().getFont(FontManager.TITLE);
    Genome gen = dacx_.getDBGenome();
    if (!genomeID.equals(gen.getID())) {
      GenomeInstance gi = (GenomeInstance)dacx_.getGenomeSource().getGenome(genomeID);
      if (gi.getVfgParent() == null) {
        gen = gi;
      } else {
        gen = gi.getVfgParentRoot();
      }
    }
    Layout lo = dacx_.lSrc.getLayoutForGenomeKey(gen.getID());
    Rectangle dims = appState_.getSUPanel().getBasicBounds(dacx_, false, false, ZoomTarget.ALL_MODULES);
    ArrayList<String> vals = new ArrayList<String>();
    vals.add(title);
    if (setMultiLocation(support, vals, Layout.TITLE, TOP_CENTER_, lo, tFont, dims)) {
      support.addEvent(new LayoutChangeEvent(lo.getID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));
    }  
    return;
  }  

  /***************************************************************************
  **
  ** Helper to build key bounds
  ** 
  */
  
  public Dimension calcKeyBounds(List<String> vals, Layout lo, Font mFont, FontRenderContext frc) {
    double yTot = 0.0;
    double xMax = 0.0;     
    int valSize = vals.size();
    if (valSize > 0) {

      xMax = Double.NEGATIVE_INFINITY;
      for (int i = 0; i < valSize; i++) {
        String entry = vals.get(i);
        LineMetrics lm = mFont.getLineMetrics(entry, frc);
        yTot += lm.getHeight();
        Rectangle2D bounds = mFont.getStringBounds(entry, frc);
        double width = bounds.getWidth();
        if (width > xMax) {
          xMax = width;
        }
      }
    }
    Dimension retval = new Dimension();
    retval.setSize(xMax, yTot);
    return (retval);
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Set the data locations for a genome
  ** 
  */
  
  private void setLocationsForGenome(UndoSupport support, String date, 
                                     String attrib, List<String> key, Genome genome, Font mFont) {                                   
    Layout lo = dacx_.lSrc.getLayoutForGenomeKey(genome.getID());
    boolean needEvent = false;

    Rectangle dims = null;
    if (genome.isEmpty() && (genome.getNetworkModuleCount() == 0)) {
      Point2D center = appState_.getZoomTarget().getRawCenterPoint();
      dims = new Rectangle((int)center.getX() - 100, (int)center.getY() - 100, 200, 200);
    } else {
      dims = appState_.getSUPanel().getBasicBounds(dacx_, false, false, ZoomTarget.ALL_MODULES);
    }
    ArrayList<String> vals = new ArrayList<String>();
    vals.add(date);
    if (setMultiLocation(support, vals, Layout.DATE, TOP_RIGHT_, lo, mFont, dims)) {
      needEvent = true;
    }
    vals.clear();
    vals.add(attrib);
    if (setMultiLocation(support, vals, Layout.ATTRIB, BOTTOM_RIGHT_, lo, mFont, dims)) {
      needEvent = true;
    }
    if (setMultiLocation(support, key, Layout.KEY, BOTTOM_LEFT_, lo, mFont, dims)) {
      needEvent = true;
    }
    if (needEvent) {
      support.addEvent(new LayoutChangeEvent(lo.getID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));
    }      
    return;
  }
  
  /***************************************************************************
  **
  ** Set the data locations in the layouts
  ** 
  */
  
  private boolean setMultiLocation(UndoSupport support, List<String> vals, String key, 
                                   int location, Layout lo, 
                                   Font mFont, Rectangle dims) {
    int valSize = vals.size();
    if (valSize > 0) {
      Point2D loc = lo.getDataLocation(key);
      if (loc == null) {
        double width = 0.0;
        double height = 0.0;
        FontRenderContext frc = dacx_.getFrc();
        if (valSize == 1) {
          String val = vals.get(0);
          Rectangle2D bounds = mFont.getStringBounds(val, frc);
          width = bounds.getWidth();
          height = bounds.getHeight();
        } else {
          Dimension dim = calcKeyBounds(vals, lo, mFont, frc);
          width = dim.getWidth();
          height = dim.getHeight();
        }
        double xloc;
        double yloc;
        switch (location) {
          case TOP_RIGHT_:                      
            xloc = (dims.x + dims.width) - (width / 2.0);
            yloc = (dims.y) - (height / 2.0);
            break;
          case TOP_CENTER_:          
            xloc = dims.x + (dims.width / 2.0);
            yloc = (dims.y) - (height / 2.0);            
            break;
          case BOTTOM_RIGHT_:
            xloc = (dims.x + dims.width) - (width / 2.0);
            yloc = (dims.y + dims.height) + (height / 2.0);
            break;
          case BOTTOM_LEFT_:
            xloc = (dims.x) + (width / 2.0);
            yloc = (dims.y + dims.height) + (height / 2.0);            
            break;
          default:
            throw new IllegalArgumentException();
        }
        loc = new Point2D.Double(xloc, yloc);
        UiUtil.forceToGrid(xloc, yloc, loc, 10.0);
        Layout.PropChange pc = lo.setDataLocation(key, loc);
        support.addEdit(new PropChangeCmd(appState_, dacx_, pc));
        return (true);
      }
    }
    return (false);
  }  
}
