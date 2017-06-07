/*
**    Copyright (C) 2003-2017 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.cmd.flow.modelTree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.tree.TreePath;

import org.systemsbiology.biotapestry.app.ExpansionChange;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.app.VirtualModelTree;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.nav.NavTree;

/****************************************************************************
**
** Handle support for model tree
*/

public class TreeSupport {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  private UIComponentSource uics_;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public TreeSupport(UIComponentSource uics) {
    uics_ = uics;
  }

  /***************************************************************************
  **
  ** Calculate expansion changes
  */
  
  public ExpansionChange buildExpansionChange(boolean forUndo, DataAccessContext dacx) {
    VirtualModelTree vmTree = uics_.getTree();
    NavTree nt = dacx.getGenomeSource().getModelHierarchy();      
    List<TreePath> nonleafPaths = nt.getAllPathsToNonLeaves();
    Iterator<TreePath> nlpit = nonleafPaths.iterator();
    ExpansionChange ec = new ExpansionChange();
    ec.isForUndo = forUndo;
    ec.tree = vmTree;
    ec.expanded = new ArrayList<TreePath>();
    ec.selected = vmTree.getTreeSelectionPath();
    while (nlpit.hasNext()) {
      TreePath tp = nlpit.next();
      if (vmTree.isTreeExpanded(tp)) {
        ec.expanded.add(tp);
      }
    }
    return (ec);
  }
}
