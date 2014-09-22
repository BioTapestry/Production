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

package org.systemsbiology.biotapestry.ui.layouts;


import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/***************************************************************************
**
** Represents a specialty layout-based plan for layout compression
*/

public class LayoutCompressionFramework {

  private ArrayList<Rectangle> rects_;

   
  /***************************************************************************
  **
  ** Constructor
  */
   
  public LayoutCompressionFramework(List<Rectangle> rectList) {
    rects_ = new ArrayList<Rectangle>(rectList);
  }
  
  /***************************************************************************
  **
  ** Constructor
  */
  
  public LayoutCompressionFramework() {
    rects_ = new ArrayList<Rectangle>();
  }

  /***************************************************************************
  **
  ** Initialize
  */
  
  public void initialize(List<Rectangle> rectList) {
    rects_.clear();
    rects_.addAll(rectList);
    return;
  }
   
  /***************************************************************************
  **
  ** Get the bounds
  */
  
  public Iterator<Rectangle> getBounds() {
    return (rects_.iterator());
  } 
  
  /***************************************************************************
  **
  ** Get the bounds count
  */
  
  public int boundCount() {
    return (rects_.size());
  }  
}
