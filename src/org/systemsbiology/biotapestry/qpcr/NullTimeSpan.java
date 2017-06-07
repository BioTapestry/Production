/*
**    Copyright (C) 2003-2017 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.qpcr;

import java.util.Set;
import java.util.HashSet;
import java.io.IOException;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.util.MinMax;

/****************************************************************************
**
** This holds QPCR Null time spans
*/

class NullTimeSpan implements Cloneable, Comparable<NullTimeSpan> {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private int minTime_;
  private int maxTime_;
  private boolean isSpan_;
  private DataAccessContext dacx_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  NullTimeSpan(DataAccessContext dacx, int min) {
    dacx_ = dacx; 
    minTime_ = min;
    isSpan_ = false;
    maxTime_ = -1;
  }
  
  /***************************************************************************
  **
  ** Constructor
  */

  NullTimeSpan(DataAccessContext dacx, int min, int max) {
    dacx_ = dacx;
    minTime_ = min;
    isSpan_ = true;
    maxTime_ = max;
  }  
   
  /***************************************************************************
  **
  ** Constructor
  */

  NullTimeSpan(DataAccessContext dacx, MinMax mm) {
    dacx_ = dacx;
    minTime_ = mm.min;
    isSpan_ = mm.min < mm.max;
    maxTime_ = isSpan_ ? mm.max : -1;
  }   
    
  /***************************************************************************
  **
  ** Copy Constructor
  */

   NullTimeSpan(NullTimeSpan other) {
    mergeInNewValues(other);    
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

   @Override
   public NullTimeSpan clone() {
    try {
      return ((NullTimeSpan)super.clone());
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }
  }

  /***************************************************************************
  **
  ** Merge in new values
  */
  
   void mergeInNewValues(NullTimeSpan other) {
    this.dacx_ = other.dacx_;
    this.minTime_ = other.minTime_;
    this.isSpan_ = other.isSpan_;
    this.maxTime_ = other.maxTime_;
  }  
 
  /***************************************************************************
  **
  ** Answer if we are a span
  */
  
   boolean isASpan() {
    return (isSpan_);
  }
  
  /***************************************************************************
  **
  ** Return the minimum time
  */
  
   int getMin() {
    return (minTime_);
  }  
  
  /***************************************************************************
  **
  ** Return the single time
  */
  
   int getTime() {
    if (isSpan_) {
      throw new IllegalStateException();
    }
    return (minTime_);
  }     
 
  /***************************************************************************
  **
  ** Return the maximum time
  */
  
   int getMax() {
    if (!isSpan_) {
      throw new IllegalStateException();
    }
    return (maxTime_);
  }
  
  /***************************************************************************
  **
  ** Write the time span
  **
  */
  
   String displayString() {
    StringBuffer buf = new StringBuffer();
    TimeAxisDefinition tad = dacx_.getExpDataSrc().getTimeAxisDefinition();
    boolean namedStages = tad.haveNamedStages();
    String minTime = (namedStages) ? tad.getNamedStageForIndex(minTime_).name : Integer.toString(minTime_);
    buf.append(minTime);
    if (isASpan()) {
      buf.append("-");
      String maxTime = (namedStages) ? tad.getNamedStageForIndex(maxTime_).name : Integer.toString(maxTime_);
      buf.append(maxTime);
    }
    return (buf.toString());
  }    
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
   static Set<String> keywordsOfInterest(boolean forDefault) {
    HashSet<String> retval = new HashSet<String>();
    retval.add((forDefault) ? "defaultNullTimes" : "nullTimes");    
    return (retval);
  }
    
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
   public String toString() {
    return ("NullTimeSpan: isSpan = " + isSpan_ + " min = " + minTime_ + " max = " + maxTime_);
  }

  /***************************************************************************
  **
  ** Standard hash code
  **
  */
  
   public int hashCode() {
    return (minTime_ + ((isSpan_) ? maxTime_ : 0));
  }  

  /***************************************************************************
  **
  ** Standard equals
  **
  */  
  
   public boolean equals(Object other) {
    if (other == this) {
      return (true);
    }
    if (other == null) {
      return (false);
    }
    if (!(other instanceof NullTimeSpan)) {
      return (false);
    }
    NullTimeSpan otherNTS = (NullTimeSpan)other;
    if (this.minTime_ != otherNTS.minTime_) {
      return (false);
    }
    if (this.isSpan_ != otherNTS.isSpan_) {
      return (false);
    }
    if (!this.isSpan_) {
      return (true);
    }
    return (this.maxTime_ == otherNTS.maxTime_);
  }

  /***************************************************************************
  **
  ** Ordering based on min time.
  **
  */  
  
   public int compareTo(NullTimeSpan other) {
    
    //
    // Min time (or only time) rules the show:
    //

    if (this.minTime_ > other.minTime_) {
      return (1);
    } else if (this.minTime_ < other.minTime_) {
      return (-1);
    }

    //
    // Min times are equal.  Single times come before
    // spans.
    //

    if (!this.isSpan_ && !other.isSpan_) {
      return (0);
    } else if (!this.isSpan_ && other.isSpan_) {
      return (-1);
    } else if (this.isSpan_ && !other.isSpan_) {
      return (1);
    }      

    //
    // Have two spans, lower max time comes first.
    // Else equal.
    //    
    
    if (this.maxTime_ < other.maxTime_) {
      return (-1);
    } else if (this.maxTime_ > other.maxTime_) {
      return (1);
    }
    
    return (0);
  }  
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
   static NullTimeSpan buildFromXML(DataAccessContext dacx, String elemName, 
                                    Attributes attrs) throws IOException {

    if (!elemName.equals("defaultNullTimes") && !elemName.equals("nullTimes")) {
      return (null);
    }    
    String minStr = null;
    String maxStr = null;    
    
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("min")) {
          minStr = val;
        } else if (key.equals("max")) {
          maxStr = val;
        } 
      }
    }
    
    if (minStr == null) {
      throw new IOException();
    }
    
    int minTime;
    int maxTime = -1;
    boolean isSpan = false;
    try {
      minTime = Integer.parseInt(minStr);
      if (maxStr != null) {
        maxTime = Integer.parseInt(maxStr);
        isSpan = true;
      }
    } catch (NumberFormatException nfe) {
      throw new IOException();
    }
    
    TimeAxisDefinition tad = dacx.getExpDataSrc().getTimeAxisDefinition();
    if (!tad.spanIsOk(minTime, (isSpan) ? maxTime : minTime)) {      
      throw new IOException();
    }

    return ((isSpan) ? new NullTimeSpan(dacx, minTime, maxTime) : new NullTimeSpan(dacx, minTime));
  }  
}
