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

package org.systemsbiology.biotapestry.ui.layouts;

import java.util.Vector;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.util.EnumChoiceContent;

////////////////////////////////////////////////////////////////////////////
//
// PUBLIC ENUMS
//
//////////////////////////////////////////////////////////////////////////// 

/***************************************************************************
**
** Color Assignment Options
*/ 

public enum ColorTypes {
  COLOR_BY_GRAPH("colorByGraph"),
  COLOR_BY_CYCLE("colorByCycle"),
  COLOR_IF_NEEDED("colorByGraphIfNeeded"),
  KEEP_COLORS("colorByNoOp"),
  ;

  private String tag_;
  
  ColorTypes(String tag) {
    this.tag_ = tag;  
  }
  
  public EnumChoiceContent<ColorTypes> generateCombo(BTState appState) {
    return (new EnumChoiceContent<ColorTypes>(appState.getRMan().getString("specialtyColor." + tag_), this));
  }

  public static Vector<EnumChoiceContent<ColorTypes>> getChoices(BTState appState) {
    Vector<EnumChoiceContent<ColorTypes>> retval = new Vector<EnumChoiceContent<ColorTypes>>();
    for (ColorTypes ct: values()) {
      retval.add(ct.generateCombo(appState));    
    }
    return (retval);
  }
}

 