/*
**    Copyright (C) 2003-2004 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.util;

import java.util.List;
import java.awt.geom.Point2D;

/****************************************************************************
**
** Figures out average horizontal and vertical distances
*/

public class DistanceMeasurer {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  private DistanceMeasurer() {

  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Figure out the average X and Y distance
  */
  
  public static Vector2D calcDistances(List points) {
    //
    // For each pair of points, calculate the vector:
    //
    
    int pSize = points.size();
    Vector2D[][] vecs = new Vector2D[pSize][pSize];
    for (int i = 0; i < pSize; i++) {
      for (int j = i + 1; j < pSize; j++) {
        Point2D pi = (Point2D)points.get(i);
        Point2D pj = (Point2D)points.get(j);
        Vector2D pDiff = new Vector2D(pi, pj);
        vecs[i][j] = pDiff;
        vecs[j][i] = pDiff;
      }
    }
    
    //
    // For each point, find the nearest node "above/below" and
    // "left/right"
    //
    
    Vector2D horzAxis = new Vector2D(1.0, 0.0);
    double cutoff = 1.0 / Math.sqrt(2);
    int numH = 0;
    int numV = 0;
    double sumH = 0.0;
    double sumV = 0.0;
    
    for (int i = 0; i < pSize; i++) {
      double vertMin = Double.POSITIVE_INFINITY;
      Vector2D vertVec = null;
      double horzMin = Double.POSITIVE_INFINITY;
      Vector2D horzVec = null;
      for (int j = 0; j < pSize; j++) {
        if (i == j) {
          continue;
        }
        Vector2D currVec = vecs[i][j];
        double currDist = currVec.length();
        Vector2D norm = currVec.normalized();
        double dot = Math.abs(horzAxis.dot(norm));
        if (dot < cutoff) {
          if (currDist < vertMin) {
            vertMin = currDist;
            vertVec = currVec;
          }
        } else {
          if (currDist < horzMin) {
            horzMin = currDist;
            horzVec = currVec;
          }
        }
      }
      if (vertVec != null) {
        numV++;
        sumV += Math.abs(vertVec.getY());
      }
      if (horzVec != null) {
        numH++;
        sumH += Math.abs(horzVec.getX());
      } 
    }    
    
    double avgH = (numH == 0) ? 0.0 : sumH / (double)numH;
    double avgV = (numV == 0) ? 0.0 : sumV / (double)numV;
    return (new Vector2D(avgH, avgV));
  }
  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
}
