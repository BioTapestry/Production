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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/***************************************************************************
**
** Class for parsing CSV inputs
*/
  
public class CSVParser {
  private Pattern pat_;
  private Pattern doubq_;
  private Matcher mainMatch_;
  private Matcher doubMatch_;
  private boolean dropTrailing_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////
    
  /***************************************************************************
  ** 
  ** Build it
  */

  public CSVParser(boolean dropTrailing) {
    // Pattern for CSV from "Mastering Regular Expressions 2nd Ed." by Friedl (O'Reilly)
    pat_ = Pattern.compile(
      "\\G(?:^|,) (?: \" ( (?> [^\"]*+ ) (?> \"\" [^\"]*+ )*+ ) \" | ( [^\",]*+ ) )",
      Pattern.COMMENTS);
    doubq_ = Pattern.compile("\"\"");
    mainMatch_ = pat_.matcher("");
    doubMatch_ = doubq_.matcher("");
    dropTrailing_ = dropTrailing;
  } 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
   
  /***************************************************************************
  ** 
  ** Process a csv line into tokens
  */

  public List<String> processCSVLine(String line) {
    ArrayList<String> argList = new ArrayList<String>();
    mainMatch_.reset(line);
    while (mainMatch_.find()) {
      String group = mainMatch_.group(2);
      if (group != null) {
        argList.add(group.trim());
      } else {
        doubMatch_.reset(mainMatch_.group(1));
        argList.add(doubMatch_.replaceAll("\"").trim());
      }
    }
    
    //
    // Chop off trailing empty tokens:
    //
    
    if (dropTrailing_) {
      ArrayList<String> retval = new ArrayList<String>();
      int alnum = argList.size();
      boolean chopping = true;
      for (int i = alnum - 1; i >= 0; i--) {
        String tok = (String)argList.get(i);
        if (chopping) {
          if (tok.trim().equals("")) {
            continue;
          } else {
            chopping = false;
          }
        }
        if (!chopping) {
          retval.add(0, tok);
        }
      }
      return (retval);
    } else {
      return (argList);
    }    
  } 
}
  