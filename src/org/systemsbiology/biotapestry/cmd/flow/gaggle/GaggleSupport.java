/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.cmd.flow.gaggle;

import java.util.HashSet;
import java.util.Iterator;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.gaggle.SelectionSupport;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle Some gaggle ops
*/

public class GaggleSupport {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
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
  
  public GaggleSupport(BTState appState) {
    appState_ = appState;
  }

  /***************************************************************************
  **
  ** Select nodes transmitted from gaggle
  */
  
  public void selectFromGaggle(SelectionSupport.SelectionsForSpecies sfs) {
    DataAccessContext rcx = new DataAccessContext(appState_, appState_.getGenome());
    HashSet<String> found = new HashSet<String>();
    Genome genome = rcx.getGenome();
    Iterator<String> sit = sfs.selections.iterator();
    while (sit.hasNext()) {
      String nextSel = sit.next();
      Iterator<Gene> git = genome.getGeneIterator();
      while (git.hasNext()) {
        Gene gene = git.next();
        if (DataUtil.keysEqual(nextSel, gene.getName())) {
          found.add(gene.getID());
        }
      }
      Iterator<Node> nit = genome.getNodeIterator();
      while (nit.hasNext()) {
        Node node = nit.next();
       if (DataUtil.keysEqual(nextSel, node.getName())) {
          found.add(node.getID());
        }
      }
    }
    if (!found.isEmpty()) {
      appState_.getGenomePresentation().appendToSelectNodes(found, rcx, appState_.getUndoManager());
      appState_.getSUPanel().drawModel(false);
    }
    return;
  }      
  
  /***************************************************************************
  **
  ** Clear nodes per gaggle
  */
  
  public void clearFromGaggle() {
    DataAccessContext rcx = new DataAccessContext(appState_, appState_.getGenome());
    UndoSupport support = new UndoSupport(appState_, "undo.selection");
    appState_.getGenomePresentation().clearSelections(rcx, support);
    support.finish();
    appState_.getSUPanel().drawModel(false);
    return;
  } 
}
