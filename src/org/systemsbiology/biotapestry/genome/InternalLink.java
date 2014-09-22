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

package org.systemsbiology.biotapestry.genome;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.HashSet;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.util.Indenter;

/****************************************************************************
**
** This represents a linkage between internal functional nodes 
*/

public class InternalLink extends DBGenomeItem {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String src_;
  private String targ_;
  private int sign_;
  private int tpad_;
  private String signTag_;
  private int spad_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////

  /**
  *** Possible signs:
  **/

  public static final int NEGATIVE = -1;
  public static final int NONE     =  0;
  public static final int POSITIVE =  1;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Copy Constructor
  */

  public InternalLink(InternalLink other) {
    super(other);
    this.src_ = other.src_;
    this.targ_ = other.targ_;
    this.sign_ = other.sign_;
    this.signTag_ = other.signTag_;
    this.tpad_ = other.tpad_;
    this.spad_ = other.spad_; 
  }
  
  /***************************************************************************
  **
  ** UI-based contructor.  Null source or targ refers to a connection to the parent
  ** node.
  */

  public InternalLink(BTState appState, String id, String src, String targ, int sign, int tpad, int spad) {   
    super(appState, null, id);
    src_ = src;
    targ_ = targ;
    sign_ = sign;
    if (sign_ == NEGATIVE) {
      signTag_ = "-";
    } else if (sign_ == POSITIVE) {
      signTag_ = "+";
    } else {
      signTag_ = null;
    }
    tpad_ = tpad;
    spad_ = spad;
  }
  
  /***************************************************************************
  **
  ** XML-based contructor
  */

  public InternalLink(BTState appState, String id, String src, String targ, String sign, String tpad, String spad) throws IOException {   
    super(appState, null, id);
    if ((src == null) || (targ == null)) {
      throw new IOException();
    }
    src = src.trim();
    src_ = (src.equals("parent")) ? null : src;
    targ = targ.trim();
    targ_ = (targ.equals("parent")) ? null : targ;
    
    if (sign != null) {
      sign = sign.trim();
      if (sign.equals("+")) {
        sign_ = POSITIVE;
      } else if (sign.equals("-")) {
        sign_ = NEGATIVE;
      } else if (sign.equals("")) {
        sign_ = NONE;
      } else {
        throw new IOException();
      }
    } else {
      sign_ = NONE;
    }
    signTag_ = sign;
    
    tpad_ = 0;
    if (tpad != null) {
      try {
        tpad_ = Integer.parseInt(tpad);    
      } catch (NumberFormatException nfe) {
        throw new IOException();
      }
    }
    spad_ = 0;
    if (spad != null) {
      try {
        spad_ = Integer.parseInt(spad);    
      } catch (NumberFormatException nfe) {
        throw new IOException();
      }
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

  public InternalLink clone() {
    InternalLink retval = (InternalLink)super.clone();
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the source.  May be null if the source is the parent node.
  ** 
  */
  
  public String getSource() {
    return (src_);
  }  
  
  /***************************************************************************
  **
  ** Get the target. May be null if the target is the parent node.
  ** 
  */
  
  public String getTarget() {
    return (targ_);
  }
  
  /***************************************************************************
  **
  ** Get the sign
  ** 
  */
  
  public int getSign() {
    return (sign_);
  }
  
  /***************************************************************************
  **
  ** Get the landing pad
  */
  
  public int getLandingPad() {
    return (tpad_);
  }

  /***************************************************************************
  **
  ** Get the launch pad
  */
  
  public int getLaunchPad() {
    return (spad_);
  }
  
  /***************************************************************************
  **
  ** Set the landing pad
  */
  
  public void setLandingPad(int landingPad) {
    tpad_ = landingPad;
    return;
  }
  
  /***************************************************************************
  **
  ** Set the launch pad
  */
  
  public void setLaunchPad(int launchPad) {
    spad_ = launchPad;
    return;
  }
    
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return ("InternalLink: id = " + id_ + " source = " + src_ + 
            " target = " + targ_ + " sign = " + sign_ + " spad = " + spad_ + " tpad = " + tpad_);
  }
  
  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.print("<internalLink id=\"");
    out.print(id_);    
    out.print("\" src=\"");
    out.print((src_ == null) ? "parent" : src_);
    out.print("\" targ=\"");
    out.print((targ_ == null) ? "parent" : targ_);
    if (signTag_ != null) {
      out.print("\" sign=\"");
      out.print(signTag_);
    }

    out.print("\" targPad=\"");
    out.print(tpad_);
    
    out.print("\" srcPad=\"");
    out.print(spad_);
    
    out.println("\" />");
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Handle Internal Link creation
  **
  */
  
  public static InternalLink buildFromXML(BTState appState, Genome genome,
                                          Attributes attrs) throws IOException {
    String id = null;
    String src = null;
    String targ = null;
    String sign = null;
    String tpad = null;
    String spad = null;

    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("id")) {
          id = val;
        } else if (key.equals("src")) {
          src = val;
        } else if (key.equals("targ")) {
          targ = val;
        } else if (key.equals("sign")) {
          sign = val;
        } else if (key.equals("targPad")) {
          tpad = val;          
        } else if (key.equals("srcPad")) {
          spad = val;          
        }
      }
    }
    
    return (new InternalLink(appState, id, src, targ, sign, tpad, spad));
  }  

  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add("internalLink");
    return (retval);
  }
}
