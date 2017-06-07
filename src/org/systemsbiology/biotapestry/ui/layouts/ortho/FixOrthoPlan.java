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

package org.systemsbiology.biotapestry.ui.layouts.ortho;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.systemsbiology.biotapestry.ui.LinkProperties;
import org.systemsbiology.biotapestry.ui.LinkSegment;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;


/****************************************************************************
**
** Plan for fixing orthogonality
*/

public class FixOrthoPlan implements Cloneable {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private List<OrthoCommand> commandList_; 

  ////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Constructor
  */
  
  public FixOrthoPlan() {
    commandList_ = new ArrayList<OrthoCommand>();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Gotta be able to clone to achieve a pile of strategies
  */
  
  public FixOrthoPlan clone() {
    try {       
      FixOrthoPlan retval = (FixOrthoPlan)super.clone();

      retval.commandList_ = new ArrayList<OrthoCommand>();
      int numSS = this.commandList_.size();
      for (int i = 0; i < numSS; i++) {  
        OrthoCommand oc = this.commandList_.get(i);
        retval.commandList_.add(oc.clone());
      }
          
      return (retval);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }
  }
  
 
  /***************************************************************************
  **
  ** Add a new command
  */
  
  public void addCommand(OrthoCommand cmd) {
    commandList_.add(cmd);
    return;
  }
  
  /***************************************************************************
  **
  ** Get the commands
  */
  
  public Iterator<OrthoCommand> getCommands() {
    return (commandList_.iterator());
  }
  
  /***************************************************************************
  **
  ** Debug
  */
  
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("FixOrthoPlan:\n");
    int numCmd = commandList_.size();
    for (int i = 0; i < numCmd; i++) {
      OrthoCommand oc = commandList_.get(i);
      buf.append("  ");
      buf.append(oc.toString());
      buf.append("\n");
    }
    return (buf.toString());
  } 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Command to carry out
  */  
      
  public static class OrthoCommand {
   
    public enum Operation {POINT_0, POINT_1, MAKE_SPLIT_0, MAKE_SPLIT_1};
    
    public enum Move {UNDEF, MOVE_X, MOVE_Y};
  
    public Operation op;
    public Move what;
    public double val0;
    public double val1;

    public OrthoCommand(Operation op, Move what, double howMuch) {
      this.op = op;
      this.what = what;
      this.val0 = howMuch;
    }
    
    public OrthoCommand(Operation op, double val0, double val1) {
      this.op = op;
      this.what = Move.UNDEF;
      this.val0 = val0;
      this.val1 = val1;
    }
 
    @Override
    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("OrthoCommand: ");
      if ((op == Operation.POINT_0) || (op == Operation.POINT_1))  {
        buf.append((op == Operation.POINT_0) ? "Point 0: " : "Point 1: ");
        buf.append((what == Move.MOVE_X) ? "Move X " : "Move Y ");
        buf.append(val0);
      } else if ((op == Operation.MAKE_SPLIT_0) || (op == Operation.MAKE_SPLIT_1)) {
        buf.append((op == Operation.MAKE_SPLIT_0) ? "Split 0: " : "Split 1: ");
        buf.append("New Point (");
        buf.append(val0);
        buf.append(", ");
        buf.append(val1);
        buf.append(")");
      }
      return (buf.toString());
    } 

    public void apply(LinkProperties lp, SplitSeg split, LinkSegment geom) {
      if ((op == Operation.POINT_0) || (op == Operation.POINT_1))  {
        LinkSegmentID[] lsid = new LinkSegmentID[1];
        LinkSegmentID toTag = split.forMoveOp(op);
        LinkSegmentID tagged = toTag.clone();
        tagged.tagIDWithEndpoint((op == Operation.POINT_0) ? LinkSegmentID.START : LinkSegmentID.END);
        lsid[0] = tagged;
        Point2D start = (op == Operation.POINT_0) ? geom.getStart() : geom.getEnd();
        if (what == Move.MOVE_X) {
          lp.moveBusLinkSegments(lsid, start, val0, 0.0);           
        } else {
          lp.moveBusLinkSegments(lsid, start, 0.0, val0);
        }
        return;
      } else if ((op == Operation.MAKE_SPLIT_0) || (op == Operation.MAKE_SPLIT_1)) {
        Point2D pt = new Point2D.Double(val0, val1);
        LinkSegmentID toTag = split.forSplitOp(op);
        LinkSegmentID tagged = toTag.clone();
        LinkSegmentID[] lsid = lp.linkSplitSupport(tagged, pt);
        if (lsid != null) {
          // When dealing with root, we want to split the guy downstream of root!
          LinkSegmentID root = lp.getRootSegmentID();
          if (lsid[1].equals(root)) {
            if (lp.getSegmentCount() == 1) { //.isSingleDropTree()) { // Have n
              lsid[1] = LinkSegmentID.buildIDForEndDrop(lp.getSingleLinkage());
            } else {
              List<LinkSegmentID> cseg = lp.getChildSegs(root);
              if (cseg.size() != 1) {
                throw new IllegalStateException();
              }
              lsid[1] = cseg.get(0);
            }
          }
          split.split(lsid);     
        }
        return;
      } else {
        throw new IllegalStateException();
      }           
    }
    
    @Override
    public OrthoCommand clone() {
      try {       
        OrthoCommand retval = (OrthoCommand)super.clone();
        return (retval);
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    }
  }
  
  /***************************************************************************
  **
  ** Original segments may map to 3 segments in the end:
  */  
      
  public static class SplitSeg implements Cloneable {
    
    public LinkSegmentID start;
    public LinkSegmentID middle;
    public LinkSegmentID end;

    public SplitSeg(LinkSegmentID start) {
      this.start = start;
    }
    
    @Override
    public String toString() {
      return ("SplitSeg start = [ " + start + " ] middle = [ " + middle + " ] end = [ " + end);
    }
       
    public void split(LinkSegmentID[] splits) {
      if (end == null) {
        start = splits[0];
        end = splits[1];
      } else {
        middle = splits[0];
        end = splits[1]; 
      }
      return;
    }
     
    public int splitCount() {
      if (end == null) {
        return (0);
      } else if (middle == null) {
        return (1);
      } else {
        return (2);
      }
    }
    
    public LinkSegmentID forMoveOp(OrthoCommand.Operation op) {     
      if (op == OrthoCommand.Operation.POINT_0) {
        return (start);
      } else if (op == OrthoCommand.Operation.POINT_1) {
        if (end == null) {
          return (start);
        } else {
          return (end);
        }      
      } else {
        throw new IllegalArgumentException();
      }
    }
    
    public LinkSegmentID forSplitOp(OrthoCommand.Operation op) { 
      if (op == OrthoCommand.Operation.MAKE_SPLIT_0) {
        return (start);
      } else if (op == OrthoCommand.Operation.MAKE_SPLIT_1) {
        if (end == null) {
          return (start);
        } else {
          return (end);
        }      
      } else {
        throw new IllegalArgumentException();
      }
    }
    
    @Override
    public SplitSeg clone() {
      try {       
        SplitSeg retval = (SplitSeg)super.clone();
        retval.start = (this.start == null) ? null : this.start.clone();
        retval.middle = (this.middle == null) ? null : this.middle.clone();
        retval.end = (this.end == null) ? null : this.end.clone();
        return (retval);
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    } 
  }
}
