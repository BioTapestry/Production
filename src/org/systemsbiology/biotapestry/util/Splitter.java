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

package org.systemsbiology.biotapestry.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/****************************************************************************
**
** Utility for getting an array list from a string split
*/

public class Splitter {
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Break into strings
  */
  
  public static ArrayList<String> stringBreak(String entry, String regex, 
                                              int offset, boolean doTrim) {
    //
    // We break the list
    //
    
    ArrayList<String> retval = new ArrayList<String>();
    Pattern plus = Pattern.compile(regex);
    Matcher m = plus.matcher(entry);
    int pstart = 0;
    while (m.find()) {
      String next = entry.substring(pstart, m.start() + offset);
      retval.add((doTrim) ? next.trim() : next);
      pstart = m.end();
    }
    String last = entry.substring(pstart, entry.length());
    retval.add((doTrim) ? last.trim() : last);
    return (retval);    
  }
  
  /***************************************************************************
  **
  ** Join into a string
  */
  
  public static String tokenJoin(List<String> join, String delim) {
    StringBuffer buf = new StringBuffer();
    int num = join.size();
    int lastIndex = num - 1;
    for (int i = 0; i < num; i++) {
      String tok = join.get(i);
      buf.append(tok);
      if (i < lastIndex) {
        buf.append(delim);
      }
    }
    return (buf.toString());
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor.  Do not use
  */

  private Splitter() {
  }
}
