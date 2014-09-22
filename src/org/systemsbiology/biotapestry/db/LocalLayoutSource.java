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

package org.systemsbiology.biotapestry.db;

import java.util.HashMap;
import java.util.Iterator;

import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.ui.Layout;

/****************************************************************************
**
** Serves as a layout source for non-database layouts
*/
      
public class LocalLayoutSource implements LayoutSource {

  private Layout rootLayout_;
  private GenomeSource gSrc_;
  private HashMap<String, Layout> keyedLayouts_;

  public LocalLayoutSource(Layout layout, GenomeSource gSrc) {
    rootLayout_ = layout;
    gSrc_ = gSrc;
    keyedLayouts_ = new HashMap<String, Layout>();
    if (rootLayout_ != null) {
      keyedLayouts_.put(layout.getID(), layout);
    }
  }

  public Layout getLayout(String key) {
    return (keyedLayouts_.get(key));
  }
  
  public void setRootLayout(Layout lo) {
    rootLayout_ = lo;
    keyedLayouts_.put(lo.getID(), lo);
    return;
  }
  
  public void addInstanceLayout(Layout lo) {
    keyedLayouts_.put(lo.getID(), lo);
    return;
  }
  
  public void dropAll() {
    rootLayout_ = null;
    keyedLayouts_.clear();
    return;
  }
 
  /***************************************************************************
  ** 
  ** Get the layout key used for the given genome key
  */
  
  public String mapGenomeKeyToLayoutKey(String genomeID) {
    if (genomeID == null) {
      return (null);
    }
    Genome gen = gSrc_.getGenome(genomeID);
   // For subset instances, look for the layout to the source
    if (gen instanceof GenomeInstance) {  // FIX ME
      GenomeInstance parent = ((GenomeInstance)gen).getVfgParentRoot();
      if (parent != null) {
        genomeID = parent.getID();
      }
    } 
    Iterator<Layout> lit = keyedLayouts_.values().iterator();
    while (lit.hasNext()) {
      Layout lay = lit.next();
      if (lay.getTarget().equals(genomeID)) {
        return (lay.getID());
      }
    }
    return (null);
  }

  /***************************************************************************
  ** 
  ** Get the given layout.
  */

  public Layout getLayoutForGenomeKey(String genomeID) {
    if (genomeID == null) {
      return (null);
    }
    Genome gen = gSrc_.getGenome(genomeID);
   // For subset instances, look for the layout to the source
    if (gen instanceof GenomeInstance) {  // FIX ME
      GenomeInstance parent = ((GenomeInstance)gen).getVfgParentRoot();
      if (parent != null) {
        genomeID = parent.getID();
      }
    } 

    Iterator<Layout> lit = keyedLayouts_.values().iterator();
    while (lit.hasNext()) {
      Layout lay = lit.next();
      if (lay.getTarget().equals(genomeID)) {
        return (lay);
      }
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Get an Iterator over the layouts:
  **
  */
  
  public Iterator<Layout> getLayoutIterator() {
    return (keyedLayouts_.values().iterator());
  }
  
  /***************************************************************************
  **
  ** Get the layout for the DBGenome:
  **
  */
  
  public Layout getRootLayout() {
    return (rootLayout_);  // Kinda BOGUS solution!
  }

  /***************************************************************************
  **
  ** Start an undo transaction
  */
  
  public DatabaseChange startLayoutUndoTransaction(String key) {
    throw new UnsupportedOperationException();
    
  }
 
  /***************************************************************************
  **
  ** Finish an undo transaction
  */
  
  public DatabaseChange finishLayoutUndoTransaction(DatabaseChange change) {
    throw new UnsupportedOperationException();
  }

  /***************************************************************************
  **
  ** Rollback an undo transaction
  */
  
  public void rollbackLayoutUndoTransaction(DatabaseChange change) {
    throw new UnsupportedOperationException();
  }

  /***************************************************************************
  ** 
  ** Add the given layout
  */

  public DatabaseChange addLayout(String key, Layout layout) {
    throw new UnsupportedOperationException();
  }

  /***************************************************************************
  ** 
  ** Remove a layout
  */

  public DatabaseChange removeLayout(String key) {
    throw new UnsupportedOperationException();
  }
}
