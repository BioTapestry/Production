/*
**    Copyright (C) 2003-2013 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.cmd.flow.move;

import java.awt.geom.Point2D;

import org.systemsbiology.biotapestry.ui.Intersection;

/****************************************************************************
**
** This encapsulates the bundle of info needed for a move
*/

public class RunningMove {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////

  public enum MoveType {
    LINK_LABEL,
    MODEL_DATA,
    INTERSECTIONS,
    NET_MODULE_EDGE,
    NET_MODULE_WHOLE,
    NET_MODULE_NAME,
    NET_MODULE_LINK;
  }  

  /***************************************************************************
  **
  ** Figure out what pad fixups are needed for multi moves
  */
  
  public enum PadFixup {
    NO_PAD_FIXUP,
    PAD_FIXUP_THIS_OVERLAY,
    PAD_FIXUP_FULL_LAYOUT;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  public boolean        notPermitted;
  public String         linkID;
  public String         modelKey;
  public String         ovrId;
  public Intersection   modIntersect;
  public Intersection[] toMove;
  public MoveType       type;
  public Point2D        start;
  public int            augInterType;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  public RunningMove(MoveType type, Point2D start) {
    this.notPermitted = false;
    this.type = type;
    this.start = (Point2D)start.clone();
    this.augInterType = Intersection.IS_NONE;
  }
    
  public RunningMove(MoveType type, Point2D start, int augInter) {
    this.notPermitted = false;
    this.type = type;
    this.start = (Point2D)start.clone();
    this.augInterType = augInter;
  }
 
  public RunningMove() {
    notPermitted = true;
    type = null;
    augInterType = Intersection.IS_NONE;
  }      
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
   
  public void resetStart(Point2D start) {
    this.start = (Point2D)start.clone();     
  }

  public String toString() {
      
    StringBuffer buf = new StringBuffer();
    buf.append("RunningMove type = ");
    buf.append(type);
    buf.append("start = ");
    buf.append(start);
    if (toMove != null) {
      for (int i = 0; i < toMove.length; i++) {
        buf.append(toMove[i]);
        buf.append("  ");
      }
    }
    return (buf.toString());
  }
}
