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


package org.systemsbiology.biotapestry.app;

import java.awt.Component;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Genome;

/****************************************************************************
**
** A Class for giving tooltips
*/

public class PopupCellRenderer extends DefaultTreeCellRenderer {
                                                                                                                   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private DefaultMutableTreeNode myNode_;
  private static final long serialVersionUID = 1L;
  private BTState appState_;
  private DataAccessContext dacx_;
  private Icon vfgIcon_;
  private Icon vfaIcon_;
  private Icon vfnIcon_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public PopupCellRenderer(BTState appState, DataAccessContext dacx) {
    super();
    appState_ = appState;
    dacx_ = dacx;
    URL vfgi = getClass().getResource( "/org/systemsbiology/biotapestry/images/VfGIconB.gif");
    URL vfai = getClass().getResource( "/org/systemsbiology/biotapestry/images/VfAIconB.gif");
    URL vfni = getClass().getResource( "/org/systemsbiology/biotapestry/images/VfNIconB.gif");
    vfgIcon_ = new ImageIcon(vfgi);  
    vfaIcon_ = new ImageIcon(vfai);
    vfnIcon_ = new ImageIcon(vfni);
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  
  /***************************************************************************
  **
  ** Called when the tree is selected
  */
  
  @Override
  public String getToolTipText() {
    TreeNode[] tn = myNode_.getPath();
    TreePath tp = new TreePath(tn);
    String genomeID = dacx_.getGenomeSource().getModelHierarchy().getGenomeID(tp);
    if (genomeID == null) {
      return (null);
    }
    Genome currGenome = dacx_.getGenomeSource().getGenome(genomeID);
    return (currGenome.getLongName());
  }
  
  /***************************************************************************
  **
  ** Return the desired cell
  */
  
  @Override
  public Component getTreeCellRendererComponent(JTree tree, Object value, 
                                                boolean selected, boolean expanded, 
                                                boolean leaf, int row, boolean hasFocus) {
    Component retval = null;                                              
    try {
      myNode_ = (DefaultMutableTreeNode)value;

      //
      // Getting weird behavior if we acquire retval BEFORE we do these steps. So do these,
      // then get the renderer...
      //
      
      TreeNode[] tn = myNode_.getPath();
      Icon icon;
      if (tn.length <= 2) {
        icon = vfgIcon_;
      } else if (leaf) {
        icon = (tn.length == 3) ? vfaIcon_ : vfnIcon_;
      } else {
        icon = vfaIcon_;
      }
      if (leaf) {
       setLeafIcon(icon);
      } else {
       setOpenIcon(icon);
       setClosedIcon(icon);
      }
      
      retval = super.getTreeCellRendererComponent(tree, value, selected, expanded, 
                                                  leaf, row, hasFocus);    
      
    } catch (Exception ex) {
      appState_.getExceptionHandler().displayException(ex);
    }
    return (retval);
  }
}
