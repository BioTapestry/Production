/*
**    Copyright (C) 2003-2015 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.db;

import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.io.PrintWriter;
import java.io.IOException;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;

/****************************************************************************
**
** This holds general model data
*/

public class ModelData implements Cloneable {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String date_;
  private String attribution_;
  private ArrayList<String> key_;  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor
  */

  public ModelData() {
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

  public ModelData clone() {
    try {
      ModelData retval = (ModelData)super.clone();
      // Shallow copy is OK
      retval.key_ = new ArrayList<String>(this.key_);
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }

  /***************************************************************************
  **
  ** Set the date
  **
  */
  
  public void setDate(String date) {
    date_ = date;
    return;
  }
  
  /***************************************************************************
  **
  ** Append to the date
  */
  
  public void appendDate(String date) {
    if (date_ == null) {
      date_ = date;
    } else {
      date_ = date_.concat(date);
    }
    date_ = CharacterEntityMapper.unmapEntities(date_, false);
    return;
  }
  
  /***************************************************************************
  **
  ** Get the date
  **
  */
  
  public String getDate() {
    return (date_);
  }

  /***************************************************************************
  **
  ** Set the attribution
  */
  
  public void setAttribution(String attribution) {
    attribution_ = attribution;
    return;
  }
  
  /***************************************************************************
  **
  ** Append to the attribution
  */
  
  public void appendAttribution(String attribution) {
    if (attribution_ == null) {
      attribution_ = attribution;
    } else {
      attribution_ = attribution_.concat(attribution);
    }
    attribution_ = CharacterEntityMapper.unmapEntities(attribution_, false);
    return;
  }
  
  /***************************************************************************
  **
  ** Get the attribution
  */
  
  public String getAttribution() {
    return (attribution_);
  } 
  
  /***************************************************************************
  **
  ** Set the model key
  */
  
  public void setKey(List<String> entries) {
    if (entries == null) {
      key_ = null;
      return;
    }
    key_ = new ArrayList<String>(entries);
    return;
  }

  /***************************************************************************
  **
  ** Start a new key entry
  */
  
  public void startKey(String key) {
    if (key_ == null) {
      key_ = new ArrayList<String>();
    }
    key = CharacterEntityMapper.unmapEntities(key, false);
    key_.add(key);    
    return;
  }  
  
  /***************************************************************************
  **
  ** Append to the last key
  */
  
  public void appendKey(String fragment) {
    int num = key_.size() - 1;
    String currLast = key_.get(num);
    currLast = currLast.concat(fragment);
    currLast = CharacterEntityMapper.unmapEntities(currLast, false);
    key_.set(num, currLast);
    return;
  }

  /***************************************************************************
  **
  ** Get the size of the key
  */
  
  public int getKeySize() {
    return ((key_ == null) ? 0 : key_.size());
  }    
 
  /***************************************************************************
  **
  ** Get the given key entry
  */
  
  public String getKey(int i) {
    return (key_.get(i));
  }  
  
  /***************************************************************************
  **
  ** Write the model data to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.println("<modelData>");
    ind.up();
    if (date_ != null) {
      ind.indent();
      out.print("<modelDate>");
      out.print(CharacterEntityMapper.mapEntities(date_, false));
      out.println("</modelDate>");
    }
    if (attribution_ != null) {
      ind.indent();
      out.print("<modelAttribution>");
      out.print(CharacterEntityMapper.mapEntities(attribution_, false));
      out.println("</modelAttribution>");
    }  
    if (key_ != null) {
      ind.indent();
      out.println("<modelKey>");
      ind.up();
      int numKey = key_.size();
      for (int i = 0; i < numKey; i++) {
        ind.indent();
        out.print("<modelKeyEntry>");
        out.print(CharacterEntityMapper.mapEntities(key_.get(i), false));
        out.println("</modelKeyEntry>");
      }
      ind.down().indent();
      out.println("</modelKey>");
    }
    ind.down().indent();       
    out.println("</modelData>");
    return;
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
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add("modelData");
    return (retval);
  }

  /***************************************************************************
  **
  ** Return the attribution keyword
  **
  */
  
  public static String attributionKeyword() {
    return ("modelAttribution");
  }

  /***************************************************************************
  **
  ** Return the date keyword
  **
  */
  
  public static String dateKeyword() {
    return ("modelDate");
  }
  
  /***************************************************************************
  **
  ** Return the key entry keyword
  **
  */
  
  public static String keyEntryKeyword() {
    return ("modelKeyEntry");
  }
    
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  @Override
  public String toString() {
    return ("ModelData: date = " + date_ + " attribution = " + attribution_);
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */

  @SuppressWarnings("unused")
  public static ModelData buildFromXML(String elemName, 
                                       Attributes attrs) throws IOException {
    if (!elemName.equals("modelData")) {
      return (null);
    }
    
    return (new ModelData());
  }  
}
