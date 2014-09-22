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

import java.util.ArrayList;
import java.util.HashMap;

import org.systemsbiology.biotapestry.genome.NetModuleLinkage;

public class NetModuleLinkageExportForWeb {
	private ArrayList<NetModuleLinkSegmentExport> segmentExports_;
	private HashMap<String, NetModuleLinkage> linkages_;
	
	public NetModuleLinkageExportForWeb() {
		segmentExports_ = new ArrayList<NetModuleLinkSegmentExport>();
		linkages_ = new HashMap<String, NetModuleLinkage>();
	}
	
	public void addSegmentExport(NetModuleLinkSegmentExport lse) {
		segmentExports_.add(lse);
	}
	
	public ArrayList<NetModuleLinkSegmentExport> getSegmentExports() {
		return segmentExports_;
	}
	
	public boolean addLinkage(String linkID, NetModuleLinkage linkage) {
		boolean haveLinkage = linkages_.containsKey(linkID);
		
		if (!haveLinkage) {
			linkages_.put(linkID, linkage);
		}
		
		return haveLinkage;
	}
	
	public HashMap<String, NetModuleLinkage> getLinkages() {
		return linkages_;
	}
}
