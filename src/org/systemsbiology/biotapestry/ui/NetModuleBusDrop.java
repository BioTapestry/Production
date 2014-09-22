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

package org.systemsbiology.biotapestry.ui;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** Describes terminal connections to link buses
*/

public class NetModuleBusDrop extends LinkBusDrop {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private Point2D end_;
  private int padOffX_;
  private int padOffY_;  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Copy Constructor
  */

  public NetModuleBusDrop(NetModuleBusDrop other) {
    super(other);
    this.end_ = (Point2D)other.end_.clone();
    this.padOffX_ = other.padOffX_;
    this.padOffY_ = other.padOffY_;    
  }
  
  /***************************************************************************
  **
  ** Copy Constructor with link ID changes
  */

  public NetModuleBusDrop(NetModuleBusDrop other, Map<String, String> modLinkIDMap) {
    super(other);
    this.end_ = (Point2D)other.end_.clone();
    this.padOffX_ = other.padOffX_;
    this.padOffY_ = other.padOffY_;    
    mapToNewLink(modLinkIDMap, null);
  }  
    
  /***************************************************************************
  **
  ** Constructor
  */

  public NetModuleBusDrop(Layout layout, Genome genome, String startID, Attributes attrs) throws IOException {   
    super(layout, genome, startID, attrs); 
    
    String xStr = null;
    String yStr = null;
    String xOffStr = null;
    String yOffStr = null;    
    
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("x")) {
          xStr = val;
        } else if (key.equals("y")) {
          yStr = val;
        } else if (key.equals("xoff")) {
          xOffStr = val;
        } else if (key.equals("yoff")) {
          yOffStr = val;
        }
      }
    }
   
    if ((xStr == null) || (yStr == null)) {      
      throw new IOException();
    }
   
    if ((xOffStr == null) || (yOffStr == null)) {
      // This would be optimal, but would trash existing development IO files:     
      //throw new IOException();
      xOffStr = "0";
      yOffStr = "0";
    }    
   
    try {
      double xLoc = Double.parseDouble(xStr);
      double yLoc = Double.parseDouble(yStr);
      end_ = new Point2D.Double(xLoc, yLoc);
      padOffX_ = Integer.parseInt(xOffStr);
      padOffY_ = Integer.parseInt(yOffStr);      
    } catch (NumberFormatException ex) {
      throw new IOException();
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Clone
  */

  public NetModuleBusDrop clone() {
    NetModuleBusDrop retval = (NetModuleBusDrop)super.clone();
    retval.end_ = (Point2D)this.end_.clone();
    // integer vals handled by super
    return (retval);
  }  
    
  /***************************************************************************
  **
  ** To string
  */
  
  public String toString() {
    return ("NetModuleBusDrop: " + super.toString() + " end = " + end_ + " xoff = " + padOffX_ + " yoff = " + padOffY_);
  }
  
  /***************************************************************************
  **
  ** Get the end location
  */
  
  public Point2D getEnd(double padOffsetLen) {
    if (padOffsetLen == 0.0) {
      return (end_);
    }
    Point2D retval = (Point2D)end_.clone();
    double retX = retval.getX() + (((double)padOffX_) * padOffsetLen);
    double retY = retval.getY() + (((double)padOffY_) * padOffsetLen);
    retval.setLocation(retX, retY);
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Set the end location
  */
  
  public void setEnd(Point2D newEnd, Vector2D sideDir) {
    end_ = (Point2D)newEnd.clone();
    setOffset(sideDir);
    return;
  }
  
  /***************************************************************************
  **
  ** Change the end location
  */
  
  public void shiftEnd(double dx, double dy, Vector2D sideDir) {
    end_.setLocation(end_.getX() + dx, end_.getY() + dy);
    setOffset(sideDir);
    return;
  }  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** We actually need to remember our endpoint
  **
  */
  
  protected void writeExtraXML(PrintWriter out, Indenter ind) {  
    out.print("\" x=\"");
    out.print(end_.getX());
    out.print("\" y=\"");
    out.print(end_.getY());
    out.print("\" xoff=\"");
    out.print(padOffX_);
    out.print("\" yoff=\"");
    out.print(padOffY_);    
    return;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PKG VIZ METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Get the offset
  */
  
  Vector2D getOffset() {
    return (new Vector2D((double)padOffX_, (double)padOffY_));
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Set the direction to the module edge
  */
  
  private void setOffset(Vector2D sideDir) {
    if (sideDir != null) {
      double sideX = sideDir.getX();
      sideX = (Math.abs(sideX) < 1.0E-4) ? 0.0 : sideX;
      double sideY = sideDir.getY();
      sideY = (Math.abs(sideY) < 1.0E-4) ? 0.0 : sideY;
      padOffX_ = (sideX == 0.0) ? 0 : ((sideX < 0.0) ? -1 : 1);
      padOffY_ = (sideY == 0.0) ? 0 : ((sideY < 0.0) ? -1 : 1);
    }
    return;
  }    

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PACKAGE VISIBLE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Package visible:
  */

  NetModuleBusDrop(Point2D endpt, Vector2D padDir, String targetRef, String ourConnection, 
                   int dropType, int connectionEnd) {
    super(targetRef, ourConnection, dropType, connectionEnd);
    this.end_ = (Point2D)endpt.clone();
    setOffset(padDir);
  }
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class NetModuleBusDropWorker extends AbstractFactoryClient {
 
    public NetModuleBusDropWorker(BTState appState, FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add(XML_TAG);
      installWorker(new PerLinkDrawStyle.PerLinkDrawStyleWorker(whiteboard), new MyPerLinkGlue());
      installWorker(new SuggestedDrawStyle.SuggestedDrawStyleWorker(whiteboard), new MyStyleGlue());      
    }
  
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals(XML_TAG)) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.nmBusDrop = buildFromXML(elemName, attrs);
        retval = board.nmBusDrop;
      }
      return (retval);     
    }
    
    private NetModuleBusDrop buildFromXML(String elemName, Attributes attrs) throws IOException { 
      FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_; 
      NetModuleLinkageProperties lprops = board.netModLinkProps;
      String startID = lprops.getSourceTag();
      return (new NetModuleBusDrop(board.layout, board.genome, startID, attrs));    
    }
  }
  
  public static class MyPerLinkGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      NetModuleBusDrop drop = board.nmBusDrop;
      PerLinkDrawStyle plds = board.perLinkSty;
      drop.setDrawStyleForLink(plds);
      return (null);
    }
  } 
  
  public static class MyStyleGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      NetModuleBusDrop drop = board.nmBusDrop;
      SuggestedDrawStyle suggSty = board.suggSty;
      drop.setDrawStyleForDrop(suggSty);
      return (null);
    }
  }
}
