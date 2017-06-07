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

import java.util.HashMap;

import javax.swing.JPanel;

import org.systemsbiology.biotapestry.db.Database;


/***************************************************************************
**
** Info needed to undo a tab change
**
*/
  
public class TabChange {
  public boolean forUI;
  public CommonView.PerTab oldPerTab;
  public CommonView.PerTab newPerTab;
  public int oldChangeIndex;
  public int newChangeIndex;
  public int oldCurrIndexPre;
  public int oldCurrIndexPost;
  public int newCurrIndexPre;
  public int newCurrIndexPost;
  public Database oldDB;
  public Database newDB;
  public String oldDbId;
  public String newDbId;
  public BTState.PerTab oldBTPerTab;
  public BTState.PerTab newBTPerTab;
  public JPanel oldTabUI;
  public JPanel newTabUI;
  public boolean didTabChange;
  public int changeTabPreIndex;
  public int changeTabPostIndex;
  public HashMap<String, Integer[]> reindex;
  
  public TabChange(boolean forUI) {
    this.forUI = forUI;
    this.reindex = new HashMap<String, Integer[]>();
  }
  
  public String toString() {
    return (forUI + " " + didTabChange + " " + oldChangeIndex + " " + newChangeIndex + " " + changeTabPreIndex + " " + changeTabPostIndex);    
  }
  
  
  
}
