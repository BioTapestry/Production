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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.systemsbiology.biotapestry.plugin.InternalLinkDataDisplayPlugIn;
import org.systemsbiology.biotapestry.db.Database;

/****************************************************************************
**
** Displays URLS for a link
*/

public class URLLinkDisplayPlugIn extends URLDisplayPlugIn implements InternalLinkDataDisplayPlugIn {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public URLLinkDisplayPlugIn() {
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Answer if we are per-instance:
  */
  
  protected boolean isUrlListPerInstance(String genomeID, String itemId) {
    //
    // Have to work with the root instance:
    //
    
    TopOfTheHeap tooth = new TopOfTheHeap(appState_, genomeID);
    Genome genome = tooth.getGenome();   
    boolean haveInstance = tooth.isInstance();
 
    //
    // If we have our own URLS, we display them.  If not, we display the root
    // URLs. 
    //
    
    if (!haveInstance) {
      return (false);
    }
    
    Linkage link = genome.getLinkage(itemId);
    return (link.getURLCount() != 0);
  }

  /***************************************************************************
  **
  ** Get URL list
  */
  
  protected List<String> getUrlListFromArgs(String genomeID, String linkID) {
  
    //
    // Have to work with the root instance:
    //
    
    TopOfTheHeap tooth = new TopOfTheHeap(appState_, genomeID);
    Genome genome = tooth.getGenome();   
    boolean haveInstance = tooth.isInstance();
    Database db = appState_.getDB();
    
    //
    // If we have our own URLS, we display them.  If not, we display the root
    // URLs. 
    //   
 
    ArrayList<String> retval = new ArrayList<String>(); 
    Linkage link = genome.getLinkage(linkID);
    if ((link.getURLCount() == 0) && haveInstance) {
      Genome useGenome = db.getGenome();
      String useID = GenomeItemInstance.getBaseID(linkID);
      link = useGenome.getLinkage(useID);
    }    
    
    Iterator<String> uit = link.getURLs();
    while (uit.hasNext()) {
      String urlString = uit.next();
      retval.add(urlString);
    }
    return (retval);
  }
}
