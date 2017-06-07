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

import java.util.Vector;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.util.LinkPlacementGrid;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** A class to specify layout options
*/

public class LayoutOptions {
  
  ////////////////////////////////////////////////////////////////////////////    
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  public static final boolean DO_SEQUENTIAL = false;
  public static final boolean DO_RECURSIVE  = true;

  public static final boolean NO_COMPRESS_TOPO_SORT = false;
  public static final boolean DO_COMPRESS_TOPO_SORT = true;
  
  public static final int COFFMAN_GRAHAM      = 0;
  public static final int MOD_COFFMAN_GRAHAM  = 1;
  public static final int AD_HOC              = 2; 
  private static final int NUM_LAYER_OPTIONS_ = 3; 
  
  public static final boolean NO_REDUCE_CROSSINGS = false;
  public static final boolean DO_REDUCE_CROSSINGS = true;
  
  public static final boolean NO_ROW_NORMALIZE = false;      
  public static final boolean DO_ROW_NORMALIZE = true; 
  
  public static final boolean NO_INCREMENTAL_COMPRESS = false;      
  public static final boolean DO_INCREMENTAL_COMPRESS = true; 
  
  public static final boolean NO_INHERITANCE_SQUASH = false;      
  public static final boolean DO_INHERITANCE_SQUASH = true; 
    
  public static final int MAX_OPT_PASSES = 10;   
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  public boolean firstPass;  // SEQUENTIAL or RECURSIVE
  public boolean topoCompress;
  public int layeringMethod;  //COFFMAN_GRAHAM, MOD_COFFMAN_GRAHAM, or ADD_HOC
  public int maxPerLayer;
  public boolean doCrossingReduction;
  public boolean normalizeRows;
  public boolean incrementalCompress;
  public int optimizationPasses;
  public boolean inheritanceSquash;
  public LinkPlacementGrid.GoodnessParams goodness;
  public int overlayOption;
  public int overlayCpexOption;  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public LayoutOptions() {
    firstPass = DO_RECURSIVE;
    topoCompress = DO_COMPRESS_TOPO_SORT;
    layeringMethod = AD_HOC;
    maxPerLayer = 10;
    doCrossingReduction = DO_REDUCE_CROSSINGS;
    normalizeRows = NO_ROW_NORMALIZE;
    incrementalCompress = NO_INCREMENTAL_COMPRESS;
    inheritanceSquash = DO_INHERITANCE_SQUASH;
    optimizationPasses = 0;
    goodness = new LinkPlacementGrid.GoodnessParams();
    overlayOption = NetOverlayProperties.RELAYOUT_SHIFT_AND_RESIZE_SHAPES;
    overlayCpexOption = NetOverlayProperties.CPEX_LAYOUT_APPLY_ALGORITHM;
  }
  
  /***************************************************************************
  **
  ** Copy Constructor
  */

  public LayoutOptions(LayoutOptions other) {
    this.firstPass = other.firstPass;
    this.topoCompress =  other.topoCompress;
    this.layeringMethod = other.layeringMethod;
    this.maxPerLayer = other.maxPerLayer;
    this.doCrossingReduction = other.doCrossingReduction;
    this.normalizeRows = other.normalizeRows;
    this.incrementalCompress = other.incrementalCompress;
    this.inheritanceSquash = other.inheritanceSquash;
    this.optimizationPasses = other.optimizationPasses;
    this.overlayOption = other.overlayOption;
    this.overlayCpexOption = other.overlayCpexOption;  
    this.goodness = new LinkPlacementGrid.GoodnessParams(other.goodness);
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Useful for JComboBoxes
  */

  public static Vector<ChoiceContent> layerOptions(DataAccessContext dacx) {
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    for (int i = 0; i < NUM_LAYER_OPTIONS_; i++) {
      retval.add(mapLayerOptions(dacx, i));
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Useful for JComboBoxes
  */

  public static ChoiceContent mapLayerOptions(DataAccessContext dacx, int val) {
    ResourceManager rMan = dacx.getRMan();
    switch (val) {
      case COFFMAN_GRAHAM:
        return (new ChoiceContent(rMan.getString("layoutOption.COFFMAN_GRAHAM"), COFFMAN_GRAHAM));
      case MOD_COFFMAN_GRAHAM:
        return (new ChoiceContent(rMan.getString("layoutOption.MOD_COFFMAN_GRAHAM"), MOD_COFFMAN_GRAHAM));
      case AD_HOC:
        return (new ChoiceContent(rMan.getString("layoutOption.ADD_HOC"), AD_HOC));
    }
    throw new IllegalArgumentException();
  }    
}
