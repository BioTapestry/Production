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

package org.systemsbiology.biotapestry.perturb;

import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

/****************************************************************************
**
** Two filters with an operation
**
*/

public class PertFilterExpression implements Cloneable, PertFilterOpTarget {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////

  //
  // Categories:
  //
  
  public final static int NEVER_OP  = 0;
  public final static int ALWAYS_OP = 1;
  public final static int NO_OP     = 2;
  public final static int AND_OP    = 3;
  public final static int OR_OP     = 4; 
  public final static int NOT_OP    = 5;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private int op_;
  private PertFilterOpTarget target1_;
  private PertFilterOpTarget target2_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */  
  
  public PertFilterExpression(int op, PertFilterOpTarget targ1, PertFilterOpTarget targ2) {    
    op_ = op;
    target1_ = targ1;
    target2_ = targ2;
  }
  
  /***************************************************************************
  **
  ** Constructor
  */  
  
  public PertFilterExpression(PertFilter filter) {    
    op_ = NO_OP;
    target1_ = filter;
    target2_ = null;
  }
  
  /***************************************************************************
  **
  ** Constructor for a null or never filter
  */  
  
  public PertFilterExpression(int useop) {
    if ((useop == NEVER_OP) || (useop == ALWAYS_OP)) {
      op_ = useop;
      target1_ = null;
      target2_ = null;
    } else {
      throw new IllegalArgumentException();
    }
  }
     
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Answer if this is an always filter
  */
  
  public boolean isAlwaysFilter() {
    return (op_ == ALWAYS_OP);
  }
  
  /***************************************************************************
  **
  ** Answer if this is a never filter
  */
  
  public boolean isNeverFilter() {
    return (op_ == NEVER_OP);
  }
   
  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public PertFilterExpression clone() {
    try {
      PertFilterExpression newVal = (PertFilterExpression)super.clone();
      // BOGUS!!
      if (this.target1_ instanceof PertFilter) {
        newVal.target1_ = ((PertFilter)this.target1_).clone();
      } else {
        newVal.target1_ = ((PertFilterExpression)this.target1_).clone();
      }
      if (this.target2_ != null) {
        if (this.target2_ instanceof PertFilter) {
          newVal.target2_ = ((PertFilter)this.target2_).clone();
        } else {
          newVal.target2_ = ((PertFilterExpression)this.target2_).clone();
        }
      }
      return (newVal);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }
  }

  /***************************************************************************
  **
  ** Get the operator
  */
  
  public int getOperator() {
    return (op_);
  }
  
  /***************************************************************************
  **
  ** Get the filter
  */
  
  public PertFilterOpTarget getTarget1() {
    return (target1_);
  }
  
  /***************************************************************************
  **
  ** Get the filter
  */
  
  public PertFilterOpTarget getTarget2() {
    return (target2_);
  }
  
  /***************************************************************************
  **
  ** Get string
  */
  
  public String toString() {
    return ("(pertExp: " + op_ + "," + target1_ + "," + target2_ + ")");
  }
  
  /***************************************************************************
  **
  ** Get the filter
  */
  
  public SortedSet<String> getFilteredResult(SortedSet<String> input, SortedMap<String, ? extends PertFilterTarget> source, SourceSrc ss) {
    
    if (isAlwaysFilter()) {
      return (input);
    }
    if (isNeverFilter()) {
      return (new TreeSet<String>());
    }

    SortedSet<String> t1Result = target1_.getFilteredResult(input, source, ss);
    
    if (op_ == NO_OP) {
      return (t1Result);
    } else if (op_ == NOT_OP) {
      TreeSet<String> retval = new TreeSet<String>(input);
      retval.removeAll(t1Result);
      return (retval);
    }
      
    SortedSet<String> t2Result = target2_.getFilteredResult(input, source, ss);
    
    TreeSet<String> retval = new TreeSet<String>(t1Result);
    if (op_ == AND_OP) { 
      retval.retainAll(t2Result); // Intersection
    } else if (op_ == OR_OP) {
      retval.addAll(t2Result);  // Union
    } else {
      throw new IllegalStateException();
    }
    return (retval);
  } 
}
