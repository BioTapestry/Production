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

package org.systemsbiology.biotapestry.ui.xplat;

import org.systemsbiology.biotapestry.ui.xplat.XPlatUIElementLayout.UILayoutType;

/**
 * Convenience class for making layouts (which will often be repetative and 
 * numerous).
 * 
 * 
 * 
 * 
 *
 */

public class XPlatLayoutFactory {
	
	// This is a static factory
	private XPlatLayoutFactory(){}
	
	
	public static XPlatUIElementLayout makeGridLayout(int col, int width, int row) {
		XPlatUIElementLayout gridLayout = new XPlatUIElementLayout(UILayoutType.GRID);
		gridLayout.setLayoutParameter("col", Integer.toString(col));
		gridLayout.setLayoutParameter("row", Integer.toString(row));
		gridLayout.setLayoutParameter("width", Integer.toString(width));
		return gridLayout;
	}

	
	public static XPlatUIElementLayout makeRegionalLayout(
		RegionType thisRegion,
		int ordinal
	) {
		XPlatUIElementLayout regionalLayout = new XPlatUIElementLayout(UILayoutType.REGIONAL);
		regionalLayout.setLayoutParameter("region", thisRegion.toString().toLowerCase());
		regionalLayout.setLayoutParameter("ordinal", Integer.toString(ordinal));		
		return regionalLayout;
	}
	
	public enum RegionType {
		BOTTOM,
		LEFT,
		RIGHT,
		CENTER,
		TOP
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
