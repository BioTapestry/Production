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

import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.plugin.InternalNodeDataDisplayPlugIn;

/****************************************************************************
**
** Displays URLS for a gene or node
*/

public class URLNodeDisplayPlugIn extends URLDisplayPlugIn implements InternalNodeDataDisplayPlugIn {
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public URLNodeDisplayPlugIn() {
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
    
    Node node = genome.getNode(itemId);
    return (node.getURLCount() != 0);
  }

  /***************************************************************************
  **
  ** Get URL list
  */
  
  protected List<String> getUrlListFromArgs(String genomeID, String geneId) {
  
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
    
    Node node = genome.getNode(geneId);
    if ((node.getURLCount() == 0) && haveInstance) {
      Genome useGenome = db.getGenome();
      String useID = GenomeItemInstance.getBaseID(geneId);
      node = useGenome.getNode(useID);
    }
    
    ArrayList<String> retval = new ArrayList<String>();   
    Iterator<String> uit = node.getURLs();
    while (uit.hasNext()) {
      String urlString = uit.next();
      retval.add(urlString);
    }
    return (retval);
  }
}
