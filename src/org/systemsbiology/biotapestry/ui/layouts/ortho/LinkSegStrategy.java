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

package org.systemsbiology.biotapestry.ui.layouts.ortho;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** A way of achieving orthogonality
*/

public class LinkSegStrategy implements Cloneable {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  // OPERATIONS
  
  public static final int POINT_0_MOVE_X = 0x01;
  public static final int POINT_0_MOVE_Y = 0x02;
  public static final int POINT_1_MOVE_X = 0x04;
  public static final int POINT_1_MOVE_Y = 0x08;
  public static final int MOVE_A_POINT = POINT_0_MOVE_X | POINT_0_MOVE_Y | POINT_1_MOVE_X | POINT_1_MOVE_Y;
    
  public static final int CREATE_SPLIT_0 = 0x10;
  public static final int CREATE_SPLIT_1 = 0x20;  
  
  // GOALS
  
  public enum Goal {
    NO_GOAL,
    P_0_X,
    P_0_X_NEW,
    P_0_Y,
    P_0_Y_NEW,
    P_1_X,
    P_1_X_NEW,
    P_1_Y,
    P_1_Y_NEW,

    S_0_X,
    S_0_Y,
    S_1_X,
    S_1_Y;
  };
 
  public static final double ZERO_TOL_ = 1.0E-1;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private LinkSegmentID segID_;
  private int operations_;
  private List<Requirement> suchThat_;
  private boolean varyMoves_;
  private PointDegreesOfFreedom p0d_;
  private PointDegreesOfFreedom p1d_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Constructor
  */
  
  public LinkSegStrategy(LinkSegmentID segID, int operations, List<Requirement> requirements, 
                         PointDegreesOfFreedom p0d, PointDegreesOfFreedom p1d, 
                         boolean varyMoves) {
    segID_ = segID;
    operations_ = operations;
    suchThat_ = requirements;
    p0d_ = p0d;
    p1d_ = p1d;
    varyMoves_ = varyMoves;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Answer if we have variations
  */
  
  public boolean haveVariations() {
    return (varyMoves_);
  }
  
  /***************************************************************************
  **
  ** Get the variations
  */
  
  public int getVariations() {
    return (varyMoves_) ? 3 : 1;
  }
    
  /***************************************************************************
  **
  ** Std toString
  */
  
  public String toString() {
    return (segID_  + " " + operations_);
  }
   
  /***************************************************************************
  **
  ** Gotta be able to clone to achieve a pile of strategies
  */
  
  public LinkSegStrategy clone() {
    try {       
      LinkSegStrategy retval = (LinkSegStrategy)super.clone();

      retval.segID_ = this.segID_.clone();
      retval.p0d_ = this.p0d_.clone();
      retval.p1d_ = this.p1d_.clone();
      
      retval.suchThat_ = new ArrayList<Requirement>();
      int numSS = this.suchThat_.size();
      for (int i = 0; i < numSS; i++) {  
        Requirement req = this.suchThat_.get(i);
        retval.suchThat_.add(req.clone());
      }
          
      return (retval);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }
  }
 
  /***************************************************************************
  **
  ** Get segment ID
  */
  
  public LinkSegmentID getSegID() {
    return (segID_);
  }

  /***************************************************************************
  **
  ** get the plan (with variation varNum)
  */
  
  public FixOrthoPlan getPlan(Point2D pt1, Point2D pt2, int varNum, int totVar) {
    if ((operations_ & CREATE_SPLIT_0) != 0x00) {
      if ((operations_ & CREATE_SPLIT_1) != 0x00) {
        //System.out.println("do double split");
        return (doDoubleSplit(pt1, pt2, varNum, totVar));
      } else {
        //System.out.println("do single split");
        return (doSingleSplit(pt1, pt2));
      }
    } else {
      //System.out.println("do moves");
      return (doMoves(pt1, pt2));
    }
  }
   
  /***************************************************************************
  **
  ** Get the variation count
  */
  
  public int getVarNum(Point2D pt1, Point2D pt2) {
  
    if (!varyMoves_) {
      throw new IllegalArgumentException();
    }
    
    int xCount = Math.abs((int)((pt2.getX() - pt1.getX()) / UiUtil.GRID_SIZE));
    int yCount = Math.abs((int)((pt2.getY() - pt1.getY()) / UiUtil.GRID_SIZE));
   
    int varCount = 1;
    Iterator<Requirement> rit = suchThat_.iterator();
    while (rit.hasNext()) {
      Requirement req = rit.next();
      if (req.op != Requirement.ReqType.EQUALS) {
        throw new IllegalStateException();
      }        
      if ((req.first == LinkSegStrategy.Goal.S_0_Y) && (req.second == LinkSegStrategy.Goal.S_1_Y)) {
        varCount = yCount;
        break;
      } else if ((req.first == LinkSegStrategy.Goal.S_0_X) && (req.second == LinkSegStrategy.Goal.S_1_X)) {
        varCount = xCount;
        break;
      }
    }
    return (varCount);  
  }

  /***************************************************************************
  **
  ** Do double split
  */
  
  private FixOrthoPlan doDoubleSplit(Point2D pt1, Point2D pt2, int varNum, int totVar) {
  
    if (!varyMoves_ && (varNum != 0)) {
      throw new IllegalArgumentException();
    }
     
    //
    // special case!  First variation is the midpoint!
    //
       
    int midPoint = totVar / 2;     
    int useVar;
    if (varNum == 0) {
      useVar = midPoint;
    } else if (varNum < midPoint) {  
      useVar = varNum - 1;
    } else { // if (varNum >= midPoint) {
      useVar = varNum;
    }
  
    double split1X = pt1.getX();
    double split1Y = pt1.getY();
    double split2X = pt2.getX();
    double split2Y = pt2.getY();
    
    FixOrthoPlan fop = new FixOrthoPlan();
    double midRawX = pt1.getX() + (((double)useVar * UiUtil.GRID_SIZE) * ((pt2.getX() > pt1.getX()) ? 1.0 : -1.0)); 
    double midRawY = pt1.getY() + (((double)useVar * UiUtil.GRID_SIZE) * ((pt2.getY() > pt1.getY()) ? 1.0 : -1.0)); 
    double midX = UiUtil.forceToGridValue(midRawX, UiUtil.GRID_SIZE);
    double midY = UiUtil.forceToGridValue(midRawY, UiUtil.GRID_SIZE);
    Iterator<Requirement> rit = suchThat_.iterator();
    while (rit.hasNext()) {
      Requirement req = rit.next();
      if (req.op != Requirement.ReqType.EQUALS) {
        throw new IllegalStateException();
      }        
      if ((req.first == LinkSegStrategy.Goal.S_0_X) && (req.second == LinkSegStrategy.Goal.P_0_X)) {
        split1X = pt1.getX();
      } else if ((req.first == LinkSegStrategy.Goal.S_0_Y) && (req.second == LinkSegStrategy.Goal.S_1_Y)) {
        split1Y = midY;
        split2Y = midY;
      } else if ((req.first == LinkSegStrategy.Goal.S_1_X) && (req.second == LinkSegStrategy.Goal.P_1_X)) {
        split2X = pt2.getX();
      } else if ((req.first == LinkSegStrategy.Goal.S_0_Y) && (req.second == LinkSegStrategy.Goal.P_0_Y)) {
        split1Y = pt1.getY();
      } else if ((req.first == LinkSegStrategy.Goal.S_0_X) && (req.second == LinkSegStrategy.Goal.S_1_X)) {
        split1X = midX;
        split2X = midX;
      } else if ((req.first == LinkSegStrategy.Goal.S_1_Y) && (req.second == LinkSegStrategy.Goal.P_1_Y)) {
        split2Y = pt2.getY();
      }
    }
    fop.addCommand(new FixOrthoPlan.OrthoCommand(FixOrthoPlan.OrthoCommand.Operation.MAKE_SPLIT_0, split1X, split1Y));
    fop.addCommand(new FixOrthoPlan.OrthoCommand(FixOrthoPlan.OrthoCommand.Operation.MAKE_SPLIT_1, split2X, split2Y));
    // FIXME!  Should really handle empty commands intead of passing them forward!
    if (!fop.getCommands().hasNext()) {
     // System.out.println("Handle empty commands");
     // throw new IllegalStateException();
    }
    return (fop);  
  }
  
  /***************************************************************************
  **
  ** Do a single point split, with an optional move of a second point:
  */
  
  private FixOrthoPlan doSingleSplit(Point2D pt1, Point2D pt2) {
    FixOrthoPlan fop = new FixOrthoPlan();
    double splitX = 0.0;
    double splitY = 0.0;
    double deltaX = 0.0;
    double deltaY = 0.0;
    Iterator<Requirement> rit = suchThat_.iterator();
    while (rit.hasNext()) {
      Requirement req = rit.next();
      if (req.op != Requirement.ReqType.EQUALS) {
        throw new IllegalStateException();
      }
      //System.out.println("------------- Next Requirement:");
      if (req.first == LinkSegStrategy.Goal.S_0_X) {
        //System.out.println("S_0_X");
        if (req.second == LinkSegStrategy.Goal.P_0_X) {       
          splitX = pt1.getX();
          //System.out.println("P_0_X " + splitX);
        } else if (req.second == LinkSegStrategy.Goal.P_1_X) {
          splitX = pt2.getX();
          //System.out.println("P_1_X " + splitX);
        } else if ((req.second == LinkSegStrategy.Goal.P_0_X_NEW) || (req.second == LinkSegStrategy.Goal.P_1_X_NEW)) {
          deltaX = UiUtil.forceToGridValue((pt2.getX() - pt1.getX()) / 2.0, UiUtil.GRID_SIZE);
          if (req.second == LinkSegStrategy.Goal.P_0_X_NEW) {
            splitX = pt1.getX() + deltaX;
            //System.out.println("P_0_X_NEW  " + splitX);
          } else {
            splitX = pt2.getX() - deltaX;
            //System.out.println("P_1_X_NEW " + splitX);
          }
        }
      } else if (req.first == LinkSegStrategy.Goal.S_0_Y) {
        //System.out.println("S_0_Y");
        if (req.second == LinkSegStrategy.Goal.P_0_Y) {         
          splitY = pt1.getY();
          //System.out.println("P_0_Y " + splitY);
        } else if (req.second == LinkSegStrategy.Goal.P_1_Y) {          
          splitY = pt2.getY();
          //System.out.println("P_1_Y " + splitY);
        } else if ((req.second == LinkSegStrategy.Goal.P_0_Y_NEW) || (req.second == LinkSegStrategy.Goal.P_1_Y_NEW)) {
          //System.out.println("S_0_X");
          deltaY = UiUtil.forceToGridValue((pt2.getY() - pt1.getY()) / 2.0, UiUtil.GRID_SIZE);
          if (req.second == LinkSegStrategy.Goal.P_0_Y_NEW) {
            splitY = pt1.getY() + deltaY;
            //System.out.println("P_0_Y_NEW  " + splitY);
          } else {
            splitY = pt2.getY() - deltaY;
            //System.out.println("P_1_Y_NEW " + splitY);
          }
        } 
      }
    }
    fop.addCommand(new FixOrthoPlan.OrthoCommand(FixOrthoPlan.OrthoCommand.Operation.MAKE_SPLIT_0, splitX, splitY));
    
    //System.out.println("------------- MOVE OPS:");
    if ((operations_ & MOVE_A_POINT) != 0x00) {  
      if ((operations_ & POINT_0_MOVE_X) != 0x00) {
        //System.out.println("POINT_0_MOVE_X " + deltaX);
        if (Math.abs(deltaX) > ZERO_TOL_) {
          fop.addCommand(new FixOrthoPlan.OrthoCommand(FixOrthoPlan.OrthoCommand.Operation.POINT_0,
                                                       FixOrthoPlan.OrthoCommand.Move.MOVE_X,
                                                       deltaX));
         }
      } else if  ((operations_ & POINT_0_MOVE_Y) != 0x00) {
        //System.out.println("POINT_0_MOVE_Y " + deltaY);
        if (Math.abs(deltaY) > ZERO_TOL_) {
          fop.addCommand(new FixOrthoPlan.OrthoCommand(FixOrthoPlan.OrthoCommand.Operation.POINT_0,
                                                       FixOrthoPlan.OrthoCommand.Move.MOVE_Y,
                                                       deltaY));
         }
      } else if ((operations_ & POINT_1_MOVE_X) != 0x00) {
        //System.out.println("POINT_1_MOVE_X " + (-deltaX));
        if (Math.abs(deltaX) > ZERO_TOL_) {
          fop.addCommand(new FixOrthoPlan.OrthoCommand(FixOrthoPlan.OrthoCommand.Operation.POINT_1,
                                                       FixOrthoPlan.OrthoCommand.Move.MOVE_X,
                                                       -deltaX));
         }
      } else if ((operations_ & POINT_1_MOVE_Y) != 0x00) {
        //System.out.println("POINT_1_MOVE_Y " + (-deltaY));
        if (Math.abs(deltaY) > ZERO_TOL_) {
          fop.addCommand(new FixOrthoPlan.OrthoCommand(FixOrthoPlan.OrthoCommand.Operation.POINT_1,
                                                       FixOrthoPlan.OrthoCommand.Move.MOVE_Y,
                                                       -deltaY));
         }
      } else {
        throw new IllegalStateException();
      } 
    }
     // FIXME!  Should really handle empty commands intead of passing them forward!
    if (!fop.getCommands().hasNext()) {
      //System.out.println("Handle empty commands");
      //throw new IllegalStateException();
    }
    return (fop);
  }
  
  /***************************************************************************
  **
  ** Do pure point moves
  */
  
  private FixOrthoPlan doMoves(Point2D pt1, Point2D pt2) {
  
    FixOrthoPlan fop = new FixOrthoPlan();
    
    //
    // Pure moves have only one requirement:
    //
    if (suchThat_.size() != 1) {
      throw new IllegalStateException();
    }
    
    Requirement req = (Requirement)suchThat_.get(0);
    if (req.op != Requirement.ReqType.EQUALS) {
      throw new IllegalStateException();
    }
    if ((operations_ & POINT_0_MOVE_X) != 0x00) {
     // System.out.println("POINT_0_MOVE_X");
      if ((req.first == LinkSegStrategy.Goal.P_0_X_NEW) && (req.second == LinkSegStrategy.Goal.P_1_X)) {
      //  System.out.println("P_0_X_NEW == P_1_X");
        double deltaX = pt2.getX() - pt1.getX();
        if (Math.abs(deltaX) > ZERO_TOL_) {
          fop.addCommand(new FixOrthoPlan.OrthoCommand(FixOrthoPlan.OrthoCommand.Operation.POINT_0,
                                                       FixOrthoPlan.OrthoCommand.Move.MOVE_X,
                                                       deltaX));
        }        
      } else if ((req.first == LinkSegStrategy.Goal.P_0_X_NEW) && (req.second == LinkSegStrategy.Goal.P_1_X_NEW)) {
      //  System.out.println("P_0_X_NEW == P_1_X");
        double deltaX = UiUtil.forceToGridValue((pt2.getX() - pt1.getX()) / 2.0, UiUtil.GRID_SIZE);
        if (Math.abs(deltaX) > ZERO_TOL_) {
          fop.addCommand(new FixOrthoPlan.OrthoCommand(FixOrthoPlan.OrthoCommand.Operation.POINT_0,
                                                       FixOrthoPlan.OrthoCommand.Move.MOVE_X,
                                                       deltaX)); 
        }
      }
    }
    if ((operations_ & POINT_0_MOVE_Y) != 0x00) {
     //  System.out.println("POINT_0_MOVE_Y");
      if ((req.first == LinkSegStrategy.Goal.P_0_Y_NEW) && (req.second == LinkSegStrategy.Goal.P_1_Y)) {
     //   System.out.println("P_0_Y_NEW == P_1_Y");
        double deltaY = pt2.getY() - pt1.getY();
        if (Math.abs(deltaY) > ZERO_TOL_) {
          fop.addCommand(new FixOrthoPlan.OrthoCommand(FixOrthoPlan.OrthoCommand.Operation.POINT_0,
                                                       FixOrthoPlan.OrthoCommand.Move.MOVE_Y,
                                                       deltaY));
        }
         
      } else if ((req.first == LinkSegStrategy.Goal.P_0_Y_NEW) && (req.second == LinkSegStrategy.Goal.P_1_Y_NEW)) {
      //  System.out.println("P_0_Y_NEW == P_1_Y_NEW");
        double deltaY = UiUtil.forceToGridValue((pt2.getY() - pt1.getY()) / 2.0, UiUtil.GRID_SIZE);
        if (Math.abs(deltaY) > ZERO_TOL_) {
          fop.addCommand(new FixOrthoPlan.OrthoCommand(FixOrthoPlan.OrthoCommand.Operation.POINT_0,
                                                       FixOrthoPlan.OrthoCommand.Move.MOVE_Y,
                                                       deltaY));
        }
      }
    }
    if ((operations_ & POINT_1_MOVE_X) != 0x00) {
    //  System.out.println("POINT_1_MOVE_X");
      if ((req.first == LinkSegStrategy.Goal.P_1_X_NEW) && (req.second == LinkSegStrategy.Goal.P_0_X)) {
    //    System.out.println("P_1_X_NEW == P_0_X");
        double deltaX = pt2.getX() - pt1.getX();
        if (Math.abs(deltaX) > ZERO_TOL_) {
          fop.addCommand(new FixOrthoPlan.OrthoCommand(FixOrthoPlan.OrthoCommand.Operation.POINT_1,
                                                       FixOrthoPlan.OrthoCommand.Move.MOVE_X,
                                                       -deltaX));
        }      
      } else if ((req.first == LinkSegStrategy.Goal.P_0_X_NEW) && (req.second == LinkSegStrategy.Goal.P_1_X_NEW)) {
    //    System.out.println("P_0_X_NEW == P_1_X_NEW");
        double deltaX = UiUtil.forceToGridValue((pt2.getX() - pt1.getX()) / 2.0, UiUtil.GRID_SIZE);
        double midX = pt1.getX() + deltaX;
        if (Math.abs(midX - pt2.getX()) > ZERO_TOL_) {
          fop.addCommand(new FixOrthoPlan.OrthoCommand(FixOrthoPlan.OrthoCommand.Operation.POINT_1,
                                                       FixOrthoPlan.OrthoCommand.Move.MOVE_X,
                                                       midX - pt2.getX()));
        }
      }
    }
    if ((operations_ & POINT_1_MOVE_Y) != 0x00) {
    //  System.out.println("POINT_1_MOVE_Y");
      if ((req.first == LinkSegStrategy.Goal.P_1_Y_NEW) && (req.second == LinkSegStrategy.Goal.P_0_Y)) {
    //    System.out.println("P_1_Y_NEW == P_0_Y");
        double deltaY = pt2.getY() - pt1.getY();
        if (Math.abs(deltaY) > ZERO_TOL_) {
          fop.addCommand(new FixOrthoPlan.OrthoCommand(FixOrthoPlan.OrthoCommand.Operation.POINT_1,
                                                       FixOrthoPlan.OrthoCommand.Move.MOVE_Y,
                                                       -deltaY));
        }
        
      } else if ((req.first == LinkSegStrategy.Goal.P_0_Y_NEW) && (req.second == LinkSegStrategy.Goal.P_1_Y_NEW)) {
   //     System.out.println("P_0_Y_NEW == P_1_Y_NEW");
        double deltaY = UiUtil.forceToGridValue((pt2.getY() - pt1.getY()) / 2.0, UiUtil.GRID_SIZE);
        double midY = pt1.getY() + deltaY;
        if (Math.abs(midY - pt2.getY()) > ZERO_TOL_) {
          fop.addCommand(new FixOrthoPlan.OrthoCommand(FixOrthoPlan.OrthoCommand.Operation.POINT_1,
                                                       FixOrthoPlan.OrthoCommand.Move.MOVE_Y,
                                                       midY - pt2.getY()));      
        }
      }
    }
     // FIXME!  Should really handle empty commands intead of passing them forward!
    if (!fop.getCommands().hasNext()) {
      //System.out.println("Handle empty commands");
      //throw new IllegalStateException();
    }
    return (fop);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Defines a Requirement
  */  
      
  public static class Requirement implements Cloneable {
    
    public enum ReqType {EQUALS};
 
    public LinkSegStrategy.Goal first;
    public ReqType op;
    public LinkSegStrategy.Goal second;

    public Requirement(LinkSegStrategy.Goal first, ReqType op, LinkSegStrategy.Goal second) {
      this.first = first;
      this.op = op;
      this.second = second;
    }
    
    @Override
    public Requirement clone() {
      try {       
        Requirement retval = (Requirement)super.clone();
        return (retval);
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    }

  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
}
