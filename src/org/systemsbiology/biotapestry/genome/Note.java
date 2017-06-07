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
import java.util.HashSet;
import java.util.Set;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.MultiLineRenderSupport;

/****************************************************************************
**
** This represents an annotation to a Genome
*/

public class Note implements GenomeItem, Cloneable {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String id_;
  private String name_;
  private String text_;
  private boolean interactive_;
  private String lineBreakDef_;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Copy Constructor
  */

  public Note(Note other) {
    this.id_ = other.id_;
    this.name_ = other.name_;
    this.text_ = other.text_;
    this.interactive_ = other.interactive_;
    this.lineBreakDef_ = other.lineBreakDef_;
  }
  
  /***************************************************************************
  **
  ** Copy Constructor with new ID
  */

  public Note(Note other, String newID) {
    this.id_ = newID;
    this.name_ = other.name_;
    this.text_ = other.text_;
    this.interactive_ = other.interactive_;
    this.lineBreakDef_ = other.lineBreakDef_;
  }  
  
  /***************************************************************************
  **
  ** Make a note for IO
  */

  public Note(String id, String name, String breakDef, boolean interactive) {
    id_ = id.trim();
    name_ = MultiLineRenderSupport.applyLineBreaks(name, breakDef);
    text_ = "";
    interactive_ = interactive;
    lineBreakDef_ = breakDef;
  }
  
  /***************************************************************************
  **
  ** Make a note for UI
  */

  public Note(String id, String name, boolean interactive) {
    id_ = id.trim();
    name_ = name;
    text_ = "";
    interactive_ = interactive;
    lineBreakDef_ = MultiLineRenderSupport.genBreaks(name);
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
  
  public Note clone() { 
    try {
      return ((Note)super.clone());
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** Get the id
  ** 
  */
  
  public String getID() {
    return (id_);
  }
  
  /***************************************************************************
  **
  ** Set the name
  ** 
  */
  
  public void setName(String name) {
    name_ = name;
    lineBreakDef_ = MultiLineRenderSupport.genBreaks(name);
    return;
  }
  
  /***************************************************************************
  **
  ** Get the name
  ** 
  */
  
  public String getName() {
    return (name_);
  }
  
  /***************************************************************************
  **
  ** Set the note text
  ** 
  */
  
  public void setText(String text) {
    text_ = text;
    return;
  }
  
  /***************************************************************************
  **
  ** Get if interactive
  ** 
  */
  
  public boolean isInteractive() {
    return (interactive_);
  }
  
  /***************************************************************************
  **
  ** Set if interactive 
  */
  
  public void setInteractive(boolean interactive) {
    interactive_ = interactive;
    return;
  }  
  
  /***************************************************************************
  **
  ** Append to current note text
  ** 
  */
  
  public void appendToNote(String newText) {
    if (text_ == null) {
      text_ = newText;
    } else {
      text_ = text_.concat(newText);
    }
    text_ = CharacterEntityMapper.unmapEntities(text_, false);
    return;
  }

  /***************************************************************************
  **
  ** Get the note text
  ** 
  */
  
  public String getText() {
    return (text_);
  }  
  
  /***************************************************************************
  **
  ** Get the note text
  ** 
  */
  
  public String getTextWithBreaksReplaced() {
    return (MultiLineRenderSupport.breaksToWhitespace(text_));
  }  
  
  
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return ("Note: id = " + id_ + "name = " + name_ + " text = " + text_);
  }
  
  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind, boolean forDynamic) {
    ind.indent();
    String key = mapKeywords(forDynamic);
    out.print("<");
    out.print(key);
    out.print(" id=\""); 
    out.print(id_);
    if (name_ != null) {
      String oneLineName = MultiLineRenderSupport.stripBreaks(name_);
      out.print("\" name=\"");
      out.print(CharacterEntityMapper.mapEntities(oneLineName, false));
    }
    if (lineBreakDef_ != null) {
      out.print("\" breakDef=\"");
      out.print(lineBreakDef_);
    }
    //
    // Always print out so we can catch legacy cases:
    //
    out.print("\" interactive=\"");
    out.print(interactive_);  
 
    if ((text_ != null) && !text_.trim().equals("")) {
      out.print("\" >");
      out.print(CharacterEntityMapper.mapEntities(text_, false));
      out.print("</");
      out.print(key);
      out.println(">");
    } else {
      out.println("\"/>");
    }
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
 

  /***************************************************************************
  **
  ** Bogus:
  **
  */

  public static String mapKeywords(boolean forDynamic) {
    return ((forDynamic) ? "dpNote" : "note");
  }
  
  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */

  public static Set<String> keywordsOfInterest(boolean forDynamic) {
    HashSet<String> retval = new HashSet<String>();
    retval.add(mapKeywords(forDynamic));
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Handle Note creation
  **
  */
  
  @SuppressWarnings("unused")
  public static Note buildFromXML(String elemName, Attributes attrs) throws IOException {
                                                 
    String id = null;
    String name = null;
    String breakDef = null;
    String interactiveStr = null;
      
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
        } else if (key.equals("name")) {
          name = CharacterEntityMapper.unmapEntities(val, false);
        } else if (key.equals("breakDef")) {
          breakDef = val;
        } else if (key.equals("interactive")) {
          interactiveStr = val;
        } 
      }
    }
    
    if (id == null) {
      throw new IOException();
    }    
 
    boolean interactive = (interactiveStr != null) ? Boolean.valueOf(interactiveStr).booleanValue() : true;  // true for legacy

    return (new Note(id, name, breakDef, interactive));
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor  Do not use
  */

  @SuppressWarnings("unused")
  private Note() {
  }  
    
}
