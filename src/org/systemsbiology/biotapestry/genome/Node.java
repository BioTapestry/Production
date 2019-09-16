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
** This represents a node
*/

public interface Node extends GenomeItem, Cloneable {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /**
  *** Different types of Nodes.  It's not worth it to subclass until these
  *** things behave differently.
  **/

  public static final int NO_NODE_TYPE = -1;  // used in lone node build instructions
  
  public static final int MIN_NODE_TYPE = 1;  // (Not zero-based. WHY???)
  public static final int BARE      = 1;
  public static final int BOX       = 2;
  public static final int BUBBLE    = 3;
  public static final int GENE      = 4;
  public static final int INTERCELL = 5;
  public static final int SLASH     = 6;  
  public static final int DIAMOND   = 7;
  public static final int MAX_NODE_TYPE = 7;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
   
  /***************************************************************************
  **
  ** Get the type
  */
  
  public int getNodeType();
  
  /***************************************************************************
  **
  ** Set the type
  */
  
  public void setNodeType(int type);
  
  /***************************************************************************
  **
  ** Get the root name
  */

  public String getRootName();
  
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
  ** Add a URL (I/O usage)
  **
  */
  
  public void addUrl(String url);
  
  /***************************************************************************
  **
  ** Append to the description
  **
  */
  
  public void appendDescription(String description);
  
  /***************************************************************************
  **
  ** Get display string
  */

  public String getDisplayString(Genome genome, boolean typePreface);    
  
  /***************************************************************************
  **
  ** Supports cloning
  */
  
  public Node clone(); 
  
  /***************************************************************************
  **
  ** Get the pad count
  */
  
  public int getPadCount();

  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind);
  
  /***************************************************************************
  **
  ** Check for extra pads:
  */
  
  public boolean haveExtraPads();
 
}
