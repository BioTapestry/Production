/*
**    Copyright (C) 2003-2008 Institute for Systems Biology 
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

import java.awt.Dimension;
import javax.swing.JLabel;

/****************************************************************************
**
** Fixed Size Label
*/

public class FixedJLabel extends JLabel {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public FixedJLabel() {
    super();
  }     
  
  
  /***************************************************************************
  **
  ** Constructor
  */

  public FixedJLabel(String text) {
    super(text);
  }   
  
  /***************************************************************************
  **
  ** Constructor
  */

  public FixedJLabel(String text, int center) {
    super(text, center);
  }     
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Make pictures square
  */
  
  public Dimension getPreferredSize() {
    Dimension ps = super.getPreferredSize();
    return (ps);
  }  
 
  /***************************************************************************
  **
  ** Fixed minimum
  */
  
  public Dimension getMinimumSize() {
    return (getPreferredSize());
  }
  
  /***************************************************************************
  **
  ** Fixed Maximum
  */
  
  public Dimension getMaximumSize() {
    return (getPreferredSize());
  }
}
