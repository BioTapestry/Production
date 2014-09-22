/*
**    Copyright (C) 2003-2010 Institute for Systems Biology 
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
import java.util.List;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.Splitter;

/****************************************************************************
**
** This holds QPCR Footnotes
*/

class Footnote {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String number_;
  private String body_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

   Footnote(String number) {
    number_ = number;
  }
  
  /***************************************************************************
  **
  ** Copy Constructor
  */

   Footnote(Footnote other) {
    this.number_ = other.number_;
    this.body_ = other.body_;
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get the number
  **
  */
  
   String getNumber() {
    return (number_);
  }
  
  /***************************************************************************
  **
  ** Get the note
  **
  */
  
   String getNote() {
    return (body_);
  }
  
  /***************************************************************************
  **
  ** Set the note
  **
  */
  
   void setNote(String note) {
    body_ = note;
    return;
  }
  
  /***************************************************************************
  **
  ** Write the perturb to HTML
  **
  */
  
   void writeHTML(PrintWriter out, Indenter ind, QpcrTablePublisher qtp) {
    ind.indent();
    qtp.paragraph(false);
    out.print("<sup>");
    out.print(number_);
    out.print("</sup>");
    out.print(body_);
    out.println("</p>");
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
  
   static Set<String> keywordsOfInterest() {
    HashSet<String>retval = new HashSet<String>();
    retval.add("footnote");
    return (retval);
  }
    
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
   public String toString() {
    return ("Footnote: number = " + number_ + " note = " + body_);
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
   static Footnote buildFromXML(String elemName, 
                                      Attributes attrs) throws IOException {
    if (!elemName.equals("footnote")) {
      return (null);
    }
    
    String number = null;
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("number")) {
          number = val;
        }
      }
    }
    
    if (number == null) {
      throw new IOException();
    }
    
    return (new Footnote(number));
  } 

  /***************************************************************************
  **
  ** Answer if this a valid comma-separated list of footnote IDs
  */
  
   static boolean isValidList(String list) {
    Pattern plus = Pattern.compile("([ ]*[\\w\\*]+[ ]*,)*[ ]*[\\w\\*]+");
    Matcher m = plus.matcher(list);
    return (m.matches());
  }  
  
  /***************************************************************************
  **
  ** Answer if this a valid footnote ID
  */
  
   static boolean isValidID(String id) {
    Pattern plus = Pattern.compile("[\\w\\*]+");
    Matcher m = plus.matcher(id.trim());
    return (m.matches());
  }    
  
  /***************************************************************************
  **
  ** Break the string into a set of footnotes
  */
  
   static List<String> extractNotes(String list) {
    
    if (!isValidList(list)) {
      throw new IllegalArgumentException();
    }
    
    //
    // We break the list
    //
    
    return (Splitter.stringBreak(list, ",", 0, true));  
  }
}
