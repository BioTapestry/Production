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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.util.MinMax;

/****************************************************************************
**
** Degree of freedom
*/

public class DegreeOfFreedom implements Cloneable {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  public static final int UNDEF_AXIS = -1;
  public static final int X_AXIS = 0;
  public static final int Y_AXIS = 1;
 
  public static final int UNCONDITIONAL = 0;
  public static final int CONDITIONAL   = 1;
  public static final int FIXED         = 2;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private int axis_;
  private int type_;
  private MinMax range_;
  private ArrayList<LinkSegmentID> dependencies_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Constructor
  */
  
  public DegreeOfFreedom(int axis, int type, MinMax range, List<LinkSegmentID> dependencies) {          
    axis_ = axis;
    type_ = type;
    range_ = (range == null) ? null : (MinMax)range.clone();
    if (dependencies != null) {
      dependencies_ = new ArrayList<LinkSegmentID>(dependencies);
    }
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
  
  @Override
  public DegreeOfFreedom clone() {
    try {       
      DegreeOfFreedom retval = (DegreeOfFreedom)super.clone();
      
      retval.range_ = (this.range_ != null) ? (MinMax)this.range_.clone() : null;
      
      if (this.dependencies_ != null) {
        retval.dependencies_ = new ArrayList<LinkSegmentID>();
        int numPl = this.dependencies_.size();
        for (int i = 0; i < numPl; i++) {        
          LinkSegmentID lsid = this.dependencies_.get(i);
          retval.dependencies_.add(lsid.clone());
        }
      }      
      return (retval);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }
  }
 
  /***************************************************************************
  **
  ** Get the axis
  */
  
  public int getAxis() {
    return (axis_);
  }
  
  /***************************************************************************
  **
  ** Get the type
  */
  
  public int getType() {
    return (type_);
  }
  
  /***************************************************************************
  **
  ** Get the dependencies
  */
  
  public Iterator<LinkSegmentID> getDependencies() {
    return ((dependencies_ == null) ? null : dependencies_.iterator());
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
}
