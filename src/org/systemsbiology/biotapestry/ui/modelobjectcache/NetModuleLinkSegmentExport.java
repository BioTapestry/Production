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

package org.systemsbiology.biotapestry.ui.modelobjectcache;
	
import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

import org.systemsbiology.biotapestry.ui.LinkSegment;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;

public class NetModuleLinkSegmentExport extends LinkSegmentExport {
	private Set<String> links_;
	
	public NetModuleLinkSegmentExport(LinkSegmentID lsid, LinkSegment ls, int thickness, int selectionThickness, Color color, int lineStyle) {
		super(lsid, ls, thickness, selectionThickness, color, lineStyle);
		links_ = new HashSet<String>();
	}
	
	public void addLinkId(String linkID) {
		links_.add(linkID);
	}
	public Set<String> getLinks() {
		return links_;
	}
}
