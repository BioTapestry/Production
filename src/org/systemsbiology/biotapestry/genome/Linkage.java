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

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import org.systemsbiology.biotapestry.util.Indenter;

/****************************************************************************
**
** This represents a linkage in BioTapestry
*/

public interface Linkage extends GenomeItem, Cloneable {
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////

  /**
  *** Possible signs:
  **/
  
  public static final int SIGN_UNDEFINED = -3;
  public static final int SIGN_VARIOUS   = -2;
  
  public static final int NEGATIVE = -1;
  public static final int NONE     =  0;
  public static final int POSITIVE =  1;

  public static final String NEGATIVE_STR = "negative";
  public static final String NONE_STR     = "neutral";
  public static final String POSITIVE_STR = "positive";  
  
  /**
  *** Experimental Evidence
  **/
  
  public static final int LEVEL_UNDEFINED = -2;
  public static final int LEVEL_VARIOUS   = -1;
  
  public static final int LEVEL_NONE = 0;
  public static final int LEVEL_1    = 1;
  public static final int LEVEL_2    = 2;
  public static final int LEVEL_3    = 3;
  public static final int LEVEL_4    = 4;
  public static final int LEVEL_5    = 5;
  public static final int LEVEL_6    = 6;
  public static final int LEVEL_7    = 7;
  public static final int LEVEL_8    = 8;
  public static final int LEVEL_9    = 9;
  public static final int LEVEL_10   = 10; 
  public static final int MAX_LEVEL  = 10;   
  
  public static final String LEV_NONE_STR = "none";  
  public static final String LEV_1_STR    = "level1";
  public static final String LEV_2_STR    = "level2";
  public static final String LEV_3_STR    = "level3";   
  public static final String LEV_4_STR    = "level4";   
  public static final String LEV_5_STR    = "level5";   
  public static final String LEV_6_STR    = "level6";   
  public static final String LEV_7_STR    = "level7";   
  public static final String LEV_8_STR    = "level8";   
  public static final String LEV_9_STR    = "level9";
  public static final String LEV_10_STR   = "level10";     
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get the source
  ** 
  */
  
  public String getSource();
  
  /***************************************************************************
  **
  ** Set the source
  ** 
  */
  
  public void setSource(String src);  
  
  /***************************************************************************
  **
  ** Get the target
  ** 
  */
  
  public String getTarget();
  
  /***************************************************************************
  **
  ** Set the target
  ** 
  */
  
  public void setTarget(String trg);  
  
  /***************************************************************************
  **
  ** Get the sign
  ** 
  */
  
  public int getSign();

  /***************************************************************************
  **
  ** Get the landing pad
  */
  
  public int getLandingPad();
  
  /***************************************************************************
  **
  ** Get the launch pad
  */
  
  public int getLaunchPad();
  
  /***************************************************************************
  **
  ** Get display string
  */

  public String getDisplayString(Genome genome, boolean typePreface);  
  
  /***************************************************************************
  **
  ** Set the landing pad
  */
  
  public void setLandingPad(int landingPad);

  /***************************************************************************
  **
  ** Set the landing pad
  */
  
  public void setLaunchPad(int launchPad);
  
  /***************************************************************************
  **
  ** Get the target experimental verification
  */
  
  public int getTargetLevel();
  
  /***************************************************************************
  **
  ** Set the link sign
  */
  
  public void setSign(int newSign);  
  
  /***************************************************************************
  **
  ** Set the target experimental verification
  */
  
  public void setTargetLevel(int newTarget);  
  
  /***************************************************************************
  **
  ** Supports cloning
  */
  
  public Linkage clone();
  
  /***************************************************************************
  **
  ** Get the description
  */
  
  public String getDescription(); 
    
  /***************************************************************************
  **
  ** Get the url iterator
  **
  */
  
  public Iterator<String> getURLs();
  
  /***************************************************************************
  **
  ** Get the url count
  **
  */
  
  public int getURLCount(); 
  
  
  /***************************************************************************
  **
  ** Set all URLs
  **
  */
  
  public void setAllUrls(List<String> urls);
  
  /***************************************************************************
  **
  ** Set the description
  **
  */
  
  public void setDescription(String description);
  
  /***************************************************************************
   **
   ** Write the item to XML
   **
   */
   
   public void writeXML(PrintWriter out, Indenter ind);
   
}
