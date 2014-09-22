/*
**    Copyright (C) 2003-2012 Institute for Systems Biology 
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

import java.awt.geom.Point2D;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.ui.LinkProperties;
import org.systemsbiology.biotapestry.ui.PerLinkDrawStyle;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** Source of model data for draw trees
*/

public interface DrawTreeModelDataSource {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get information on potential line style modulation for a model
  */
  
  public ModelLineStyleModulation getModelLineStyleModulation(DataAccessContext icx);
  
  public static class ModelLineStyleModulation {
    int linkModulation;
    boolean checkForActive;
    int branchRenderMode; 
    boolean forModules;
  }
 
  /***************************************************************************
  **
  ** Get information on line style modulation for a link
  */
  
  public LinkLineStyleModulation getLinkLineStyleModulation(DataAccessContext icx, String linkID, LinkProperties lp, 
                                                            ModelLineStyleModulation modulationInfo);
    
  public static class LinkLineStyleModulation {
    PerLinkDrawStyle perLinkForEvidence;
    Double perLinkActivity;
    int sign;
    boolean isActive;
    double targetOffset;
  } 
  
  /***************************************************************************
  **
  ** Get model data for tip drawing
  */
  
  public ModelDataForTip getModelDataForTip(DataAccessContext icx, String linkID, LinkProperties lp);  
     
  public static class ModelDataForTip {
    int sign;
    double padWidth;
    Vector2D arrival;
    Point2D lanLoc;
    double negLength;
    int negThick;
    boolean isActive; 
    double plusArrowDepth;
    double positiveDropOffset;
    double plusArrowHalfWidth; 
    double thickThick; 
    double tipFudge;
    double levelFudge;
    boolean hasDiamond;
  }  
}
