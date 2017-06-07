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

package org.systemsbiology.biotapestry.app;

import java.io.IOException;
import java.util.List;

import org.systemsbiology.biotapestry.db.TabNameData;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Working to get the kitchen-sink dependency on BTState object across the
** program. This provides info on tabs in the system.
*/

public class TabSource { 
                                                             
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private BTState appState_;
  private UndoSupport taSupport_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  TabSource(BTState appState) {
    appState_ = appState;
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  ** 
  ** Not efficient, but tiny, and this way we don't have to maintain a 
  ** separate data structure:
  */  
  
  public int getTabIndexFromId(String id) {
    return (appState_.getTabIndexFromIdX(id)); 
  }

  /***************************************************************************
  ** 
  ** Need to be able to rename a tab that is not current; need info to fill in
  */
  
  public TabNameData getTabNameDataForIndex(int index) { 
    return (appState_.getTabNameDataForIndexX(index)); 
  }
  
  /***************************************************************************
  ** 
  ** Add a tab
  */

  public TabChange addATab(boolean forLoad, String dbID, int tabNum) {
     return (appState_.addATabX(forLoad, dbID, tabNum));
  }

  /***************************************************************************
  ** 
  ** Remove a tab
  */
  
  public TabChange removeATab(int tabIndex, int currentCurrent, int newCurrent) {
    return (appState_.removeATabX(tabIndex, currentCurrent, newCurrent));
  }

  /***************************************************************************
  ** 
  ** Set current tab
  */

  public TabChange setCurrentTabIndex(int cTab) {
    return (appState_.setCurrentTabIndexX(cTab));
  }

  /***************************************************************************
  ** 
  ** Set current tab
  */

  TabChange setCurrentTabIndex(String tabId) {
    return (appState_.setCurrentTabIndexX(tabId));
  }

  /***************************************************************************
  ** 
  ** Get current tab
  */
  
  public String getCurrentTab() {
    return (appState_.getCurrentTabX());  
  }  
  
  /***************************************************************************
  ** 
  ** Get current tab
  */
  
  public int getCurrentTabIndex() {
    return (appState_.getCurrentTabIndexX());
  } 
  
  /***************************************************************************
  ** 
  ** Get number of tabs
  */
  
  public int getNumTab() {
    return (appState_.getNumTabX());  
  } 
  
  /***************************************************************************
  **
  ** Get list of tabs
  */ 
   
  public List<AnnotatedTabData> getTabs() {  
    return (appState_.getTabsX());
  }
  
  /***************************************************************************
  ** 
  ** reset data for our one tab on load
  */
  
  public void resetTabForLoad(String dbID, int tabNum) throws IOException {
    appState_.resetTabForLoadX(dbID, tabNum);
    return;  
  } 
  
  /***************************************************************************
  ** 
  ** On reset, we need to get ourselves back in sync with the Metabase state.
  */
  
  public void clearCurrentTabID(String id) {
    appState_.clearCurrentTabIDX(id);
    return;
  } 
  
  /***************************************************************************
  ** 
  ** Set current tab
  */
  
  public void setCurrentTabID(String id) {
    appState_.setCurrentTabIDX(id);
    return;
  } 
  
  /***************************************************************************
  ** 
  ** Get ongoing undo support for when we are doing tab append operations.
  */   
  
  public UndoSupport getTabAppendUndoSupport() {
    return (taSupport_);
  }
  
  /***************************************************************************
  ** 
  ** Set ongoing undo support for when we are doing tab append operations.
  */   
  
  public void setTabAppendUndoSupport(UndoSupport taSupport) {
    taSupport_ = taSupport;
    return;
  }
  
  /***************************************************************************
  **
  ** Undo a tab change
  */
  
  public void changeTabUndo(TabChange undo) {
    appState_.changeTabUndoX(undo);
    return;
  }
  
  /***************************************************************************
  **
  ** Redo a tab change
  */
  
  public void changeTabRedo(TabChange redo) {
    appState_.changeTabRedoX(redo);
    return;
  }
   
  /***************************************************************************
  ** 
  ** Need to get dbid for given index
  */
  
  public String getDbIdForIndex(int index) {  
    return (appState_.getDbIdForIndexX(index));
  }
  
  /***************************************************************************
  **
  ** Database tab naming info, annotated by DBID
  */ 
    
  public static class AnnotatedTabData {
    public String dbID; // Per Tab
    public TabNameData tnd;
    
    public AnnotatedTabData(String dbID, TabNameData tnd) {
      this.dbID = dbID;
      this.tnd = tnd;
    }    
  }
}
