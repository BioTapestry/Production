/*
**    Copyright (C) 2003-2005 Institute for Systems Biology 
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

import javax.swing.JToggleButton;
import javax.swing.ImageIcon;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.Action;

/****************************************************************************
**
** A Toggle Button  FIX ME!! Make the border change when depressed
*/

public class BTToggleButton extends JToggleButton {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public BTToggleButton(Action action) {
    super(action);
   // ImageIcon image, String toolTip) {
    //      AbstractAction pda = mcmd_.getAction(MainCommands.PULLDOWN, true, null);
   // toolBar.add(new BTToggleButton((ImageIcon)pda.getValue(Action.SMALL_ICON), 
     //                              (String)pda.getValue(Action.SHORT_DESCRIPTION)));
      
      
    //super();
	  //setSelectedIcon(image);
	  //setIcon(image);
   // setToolTipText(toolTip);
    this.setText("");
    // This is where we would mess around with borders
    
   // addActionListener(new ActionListener() {
    //  public void actionPerformed(ActionEvent ev) {
    //    try {
         
   //     } catch (Exception ex) {
   //       ExceptionHandler.getHandler().displayException(ex);
   //     }
   //   }
   // });         
  }
}  

