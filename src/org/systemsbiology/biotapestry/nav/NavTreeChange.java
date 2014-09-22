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

package org.systemsbiology.biotapestry.nav;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.HashMap;

/***************************************************************************
**
** Info needed to undo a nav tree change
**
*/
  
public class NavTreeChange {

  public DefaultMutableTreeNode oldRoot;
  public DefaultMutableTreeNode newRoot;
  public HashMap<String, DefaultMutableTreeNode> oldMap;
  public HashMap<String, DefaultMutableTreeNode> newMap;
  public DefaultMutableTreeNode contentNode;  
  public NavTree.NavNodeContents oldContents;
  public NavTree.NavNodeContents newContents;
}
