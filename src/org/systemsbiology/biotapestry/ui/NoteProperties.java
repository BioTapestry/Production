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

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.awt.geom.Point2D;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.ui.freerender.NoteFree;
import org.systemsbiology.biotapestry.util.MultiLineRenderSupport;

/****************************************************************************
**
** Contains the information needed to layout a note
*/

public class NoteProperties implements Cloneable {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
   
  public static final String DEFAULT_COLOR = "EX-red";

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String colorTag_;
  private Point2D location_;
  private String noteID_;
  private IRenderer renderer_;
  private FontManager.FontOverride localFont_;
  private int just_;
  private BTState appState_;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Constructor
  */

  public NoteProperties(BTState appState, Layout layout, String ref, String color, double x, double y) {
    appState_ = appState;
    colorTag_ = color;
    location_ = new Point2D.Double(x, y);      
    noteID_ = ref;
    renderer_ = new NoteFree();
    localFont_ = null;
    just_ = MultiLineRenderSupport.DEFAULT_JUST;
  }  
  
  /***************************************************************************
  **
  ** Constructor supporting Genome Instance copying
  */

  public NoteProperties(NoteProperties other, String newID) {
    this.appState_ = other.appState_;
    this.location_ = (Point2D)other.location_.clone();
    this.colorTag_ = other.colorTag_;
    this.noteID_ = other.noteID_;
    this.renderer_ = other.renderer_;
    this.noteID_ = newID;
    this.localFont_ = (other.localFont_ == null) ? null : (FontManager.FontOverride)other.localFont_.clone();
    this.just_ = other.just_;
  }    
 
  /***************************************************************************
  **
  ** Constructor
  */

  public NoteProperties(BTState appState, Layout layout, String ref, String color, 
                        String noteX, String noteY, int just) throws IOException {

    appState_ = appState;
    if ((noteX != null) && (noteY != null)) {
      try {
        double x = Double.parseDouble(noteX);
        double y = Double.parseDouble(noteY);
        location_ = new Point2D.Double(x, y);
      } catch (NumberFormatException nfe) {
        throw new IOException();
      }
    } else {
      throw new IOException();
    }
    
    colorTag_ = color;
    noteID_ = ref;
    renderer_ = new NoteFree();
    localFont_ = null;
    just_ = just;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Clone support
  ** 
  */  
  
  public NoteProperties clone() { 
    try {
      NoteProperties retval = (NoteProperties)super.clone();
      retval.location_ = (Point2D)this.location_.clone();
      retval.localFont_ = (this.localFont_ == null) ? null : (FontManager.FontOverride)this.localFont_.clone();
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** Get the reference
  */
  
  public String getReference() {
    return (noteID_);
  }  
    
  /***************************************************************************
  **
  ** Get the color
  */
  
  public Color getColor() {
    return (appState_.getDB().getColor(colorTag_));
  }
  
  /***************************************************************************
  **
  ** Get the color name
  */
  
  public String getColorName() {
    return (colorTag_);
  }  
 
  /***************************************************************************
  **
  ** Get the local font (may be null)
  */
  
  public FontManager.FontOverride getFontOverride() {
    return (localFont_);
  }  
  
  /***************************************************************************
  **
  ** Set the local font
  */
  
  public void setFontOverride(FontManager.FontOverride fontover) {
    localFont_ = fontover;
    return;
  }   
  
  /***************************************************************************
  **
  ** Replace color with given value
  */
  
  public boolean replaceColor(String oldID, String newID) {
    boolean retval = false;
    if (colorTag_.equals(oldID)) {
      colorTag_ = newID;
      retval = true;
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Replace color with given value
  */
  
  public void setColor(String newID) {
    colorTag_ = newID;
    return;
  }  

  /***************************************************************************
  **
  ** Get the location.
  */
  
  public Point2D getLocation() {
    return (location_);
  }
  
  /***************************************************************************
  **
  ** Set the location
  */
  
  public void setLocation(Point2D newLocation) {
    location_ = newLocation;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the text justification
  */
  
  public int getJustification() {
    return (just_);
  }
  
  /***************************************************************************
  **
  ** Set the text justification
  */
  
  public void setJustification(int just) {
    just_ = just;
    return;
  }    

  /***************************************************************************
  **
  ** Get the renderer.
  */
  
  public IRenderer getRenderer() {
    return (renderer_);
  }  
   
  /***************************************************************************
  **
  ** Write the property to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();
    out.print("<ntprop");
    out.print(" id=\"");
    out.print(noteID_);    
    out.print("\" color=\"");
    out.print(colorTag_);
    if (location_ != null) {
      out.print("\" x=\"");
      out.print(location_.getX());
      out.print("\" y=\"");
      out.print(location_.getY());    
    }
    if (localFont_ != null) {
      out.print("\" fsize=\"");
      out.print(localFont_.size);
      out.print("\" fbold=\"");
      out.print(localFont_.makeBold);    
      out.print("\" fital=\"");
      out.print(localFont_.makeItalic);    
      out.print("\" fsans=\"");
      out.print(localFont_.makeSansSerif);
    }
    if (just_ != MultiLineRenderSupport.DEFAULT_JUST) {
      out.print("\" just=\"");
      out.print(MultiLineRenderSupport.mapToJustTag(just_));
    }
    
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
  ** Return the element keyword that we are interested in
  **
  */
  
  public static String keywordOfInterest() {
    return ("ntprop");
  }
  
  /***************************************************************************
  **
  ** Handle layout creation
  **
  */
  
  public static NoteProperties buildFromXML(BTState appState, Layout layout, 
                                            Attributes attrs) throws IOException {
                                             
    String ref = null;
    String color = null;
    String noteX = null;
    String noteY = null;
    String fsize = null;
    String fbold = null;
    String fital = null;
    String fsans = null;
    String justStr = null;
            
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("id")) {
          ref = val;
        } else if (key.equals("color")) {
          color = val;
        } else if (key.equals("x")) {
          noteX = val;
        } else if (key.equals("y")) {
          noteY = val;
        } else if (key.equals("fsize")) {
          fsize = val;
        } else if (key.equals("fbold")) {
          fbold = val;
        } else if (key.equals("fital")) {
          fital = val;
        } else if (key.equals("fsans")) {
          fsans = val;
        } else if (key.equals("just")) {
          justStr = val;
        }
      }
    }
    
    if ((ref == null) || (color == null) ||
        (noteX == null) || (noteY == null)) {
      throw new IOException();
    }
    
    int just = MultiLineRenderSupport.DEFAULT_JUST;
    try {
      if (justStr != null) {
        just = MultiLineRenderSupport.mapFromJustTag(justStr);
      }
    } catch (IllegalArgumentException iex) {
      throw new IOException();
    }  
    
    NoteProperties retval = new NoteProperties(appState, layout, ref, color, noteX, noteY, just);
    if (fsize != null) {      
      boolean makeBold = Boolean.valueOf(fbold).booleanValue();
      boolean makeItalic = Boolean.valueOf(fital).booleanValue();
      boolean makeSansSerif = Boolean.valueOf(fsans).booleanValue();
    
      int size;
      try {
        size = Integer.parseInt(fsize); 
      } catch (NumberFormatException nfe) {
        throw new IOException();
      }    
      if ((size < FontManager.MIN_SIZE) || (size > FontManager.MAX_SIZE)) {
        throw new IOException();
      }
      FontManager.FontOverride fo = new FontManager.FontOverride(size, makeBold, makeItalic, makeSansSerif);
      retval.setFontOverride(fo);
    }
    
    return (retval);
  }
}
