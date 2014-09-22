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

package org.systemsbiology.biotapestry.ui.modelobjectcache;
	
import java.awt.Color;

import org.systemsbiology.biotapestry.cmd.instruct.SignalBuildInstruction;
import org.systemsbiology.biotapestry.ui.LinkSegment;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;

public class LinkSegmentExport {
	  private LinkSegmentID lsid_;
	  private LinkSegment ls_;
	  private int thickness_;	  
	  private int selectionThickness_;
	  private Color color_;
	  private int lineStyle_;
	  
	  public LinkSegmentExport(LinkSegmentID lsid, LinkSegment ls, int thickness, int selectionThickness, Color color, int lineStyle) {
		  lsid_ = lsid;
		  ls_ = ls;
		  thickness_ = thickness;
		  selectionThickness_ = selectionThickness;
		  color_ = color;
		  lineStyle_ = lineStyle;
	  }
	  
	  public LinkSegmentID getID() {
		  return lsid_;
	  }
	  
	  public LinkSegment getSegment() {
		  return ls_;
	  }
	  
	  public int getThickness() {
		  return thickness_;
	  }
	  
	  public int getSelectionThickness() {
		  return selectionThickness_;
	  }
	  
	  public Color getColor() {
		  return color_;
	  }
	  
	  public int getLineStyle() {
		  return lineStyle_;
	  }
}
