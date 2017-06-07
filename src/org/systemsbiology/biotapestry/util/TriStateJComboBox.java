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

package org.systemsbiology.biotapestry.util;

import java.util.Vector;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import org.systemsbiology.biotapestry.db.DataAccessContext;

/****************************************************************************
**
** Mixed/Yes/No JComboBox
*/

public class TriStateJComboBox extends JComboBox {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public static final int UNDEFINED_STATE = -2; 
  public static final int MIXED_STATE     = -1;   
  public static final int FALSE           = 0;   
  public static final int TRUE            = 1; 

  private static final long serialVersionUID = 1L;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public TriStateJComboBox(DataAccessContext dacx) {
    super();
    Vector<ChoiceContent> vals = new Vector<ChoiceContent>();
    ResourceManager rMan = dacx.getRMan();
    vals.add(new ChoiceContent(rMan.getString("triState.mixed"), MIXED_STATE));
    vals.add(new ChoiceContent(rMan.getString("triState.false"), FALSE));
    vals.add(new ChoiceContent(rMan.getString("triState.true"), TRUE));
    DefaultComboBoxModel dcbm = new DefaultComboBoxModel(vals);
    setModel(dcbm);
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
   public int getSetting() {
     return (((ChoiceContent)getSelectedItem()).val);
   } 
   
   public void setSetting(int val) {
     switch (val) {
       case MIXED_STATE:
         setSelectedIndex(0);
         return;
       case FALSE:
         setSelectedIndex(1);
         return;
       case TRUE:
         setSelectedIndex(2);
         return;
       default:
         throw new IllegalArgumentException();
     }
   } 
}
