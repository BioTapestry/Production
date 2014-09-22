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


/***************************************************************************
**
** Used to identify Link Segments inside a LinkProperties
*/
  
public class LinkSegmentID implements Cloneable, Comparable<LinkSegmentID> { 
  
  public static final String START = "S";
  public static final String END   = "E";
  
  public static final int START_DROP  = 0;
  public static final int END_DROP    = 1;
  public static final int DIRECT_LINK = 2;  
  public static final int SEGMENT     = 3;  
      
  private boolean isLink;
  private boolean isOnly;
  private String label;
  private String endID;  //NOT PART OF EQUIVALENCE; ADVISORY ONLY

  	public LinkSegmentID() {
  		this.endID = null;
  		this.label = null;
    }
  
  private LinkSegmentID(boolean isLink, boolean isOnly, String label, String endID) {
    this.isLink = isLink;
    this.isOnly = isOnly;
    this.label = label;
    this.endID = endID;
  }
  
  public LinkSegmentID(LinkSegmentID other) {
    this.isLink = other.isLink;
    this.isOnly = other.isOnly;
    this.label = other.label;
    this.endID = other.endID;
  }

  @Override
  public LinkSegmentID clone() {
    try {
      LinkSegmentID retval = (LinkSegmentID)super.clone();
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }     
  
	public String getLabel() {
		return this.label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public boolean getIsLink() {
		return this.isLink;
	}

	public void setIsLink(boolean isLink) {
		this.isLink = isLink;
	}
	
	public boolean getIsOnly() {
		return this.isOnly;
	}
	
	public void setIsOnly(boolean isOnly) {
		this.isOnly = isOnly;
	}

@Override
  public String toString() {
    return ("isLink = " + isLink + " isOnly = " + isOnly + " label = " + label + " endID = " + endID);
  }
  
  @Override
  public int hashCode() {
    int linkInc = (isLink) ? 0 : 1;
    int onlyInc = (isOnly) ? 0 : 1;    
    return ((label == null) ? linkInc + onlyInc : label.hashCode() + linkInc + onlyInc);
  }  
    
  public boolean isForDrop() {
    return (!isLink);
  }
  
  public boolean isForSegment() {
    return (isLink);
  }  
  
  public boolean isForEndDrop() {
    return ((!isLink) && (!isOnly) && (label != null));
  }  
  
  public boolean isForStartDrop() {
    return ((!isLink) && (!isOnly) && (label == null));
  }    
  
  public String getLinkSegTag() {
    if (!isLink) {
      throw new IllegalStateException();
    }
    return (label);
  }  
  
  public String getEndDropLinkRef() {
    if (!isForEndDrop()) {
      throw new IllegalStateException();
    }
    return (label);
  }
  
  public String getDirectLinkRef() {
    if (!isDirect()) {
      throw new IllegalStateException();
    }
    return (label);
  }
  
  
  public String getEndID() {
	  return (endID);
  }
  

  public boolean isDirect() {
    return (isOnly);
  }
  
  public void clearTaggedEndpoint() {
    endID = null;
    return;
  }   
   
  public boolean isTaggedWithEndpoint() {
    return (endID != null);
  } 
  
  public boolean startEndpointIsTagged() {
    return ((endID != null) && endID.equals(START));
  }  
  
  public boolean endEndpointIsTagged() {
    return ((endID != null) && endID.equals(END));
  }  
 
  public boolean isDirectOrEndDropForTarget(String ref) {
    return ((isForEndDrop() || isDirect()) && (label != null) && (label.equals(ref)));
  }
  
  public boolean isDirectOrEndDrop() {
    return (isForEndDrop() || isDirect());
  }  
    
  public boolean isBusNodeConnection() {
    return ((isOnly && (endID != null)) ||
            ((!isLink) &&
             (label == null) && 
             (endID != null) &&
             (endID.equals(START))) ||
            ((!isLink) &&
             (label != null) && 
             (!label.equals(END)) && // do we care about this case anymore?
             (endID != null) &&
             (endID.equals(END))));
  }  
  
  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return (true);
    }
    if (other == null) {
      return (false);
    }
    if (!(other instanceof LinkSegmentID)) {
      return (false);
    }
    LinkSegmentID otherSI = (LinkSegmentID)other;
    if (this.isLink != otherSI.isLink) {
      return (false);
    }
    if (this.isOnly != otherSI.isOnly) {
      return (false);
    }
    if (this.label == null) {
      return (otherSI.label == null);
    } else {
      return (this.label.equals(otherSI.label));
    }
  }
  
  // No real meaning, but allows consistent iterator ordering...
  
  public int compareTo(LinkSegmentID other) {
    if (this.isOnly != other.isOnly) {
      return ((this.isOnly) ? -1 : 1);
    }
    if ((this.isOnly) && (other.isOnly)) {
      return (0);
    }
    if (this.isLink != other.isLink) {
      return ((this.isLink) ? -1 : 1);
    }
    //
    // This is to fix BT-03-16-10:2
    //
    if (this.label == null) {
      return ((other.label == null) ? 0 : 1);
    // Apparently compareTo for strings does not like null arguments:
    } else if (other.label == null) {
      return (-1);
    } else {
      return (this.label.compareTo(other.label));
    }
  } 
  
  public void tagIDWithEndpoint(String endID) {
    this.endID = endID;
    return;
  }    
  
  //
  // Factory methods
  //

  public static LinkSegmentID buildIDForType(String idTag, int type) {
    switch (type) {
      case START_DROP:
        return (LinkSegmentID.buildIDForStartDrop());
      case END_DROP:
        return (LinkSegmentID.buildIDForEndDrop(idTag));
      case DIRECT_LINK:
        return (LinkSegmentID.buildIDForDirect(idTag));
      case SEGMENT:
        return (LinkSegmentID.buildIDForSegment(idTag));
      default:
        throw new IllegalArgumentException();
    }      
  }

  public static LinkSegmentID buildIDForSegment(String segID) {
    return (new LinkSegmentID(true, false, segID, null));
  }
  
  public static LinkSegmentID buildIDForDirect(String linkRef) {
    if (linkRef == null) {
      throw new IllegalArgumentException();
    }
    return (new LinkSegmentID(false, true, linkRef, null));
  }  
  
  public static LinkSegmentID buildIDForEndDrop(String linkRef) {
    if (linkRef == null) {
      throw new IllegalArgumentException();
    }
    return (new LinkSegmentID(false, false, linkRef, null));
  }
  
  // Will be start (if null) or end:
  public static LinkSegmentID buildIDForDrop(String linkRef) {
    return (new LinkSegmentID(false, false, linkRef, null));
  }    
  
  public static LinkSegmentID buildIDForStartDrop() {
    return (new LinkSegmentID(false, false, null, null));
  }
    

}
