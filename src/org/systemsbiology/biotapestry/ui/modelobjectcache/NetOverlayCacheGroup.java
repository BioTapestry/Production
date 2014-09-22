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
import java.util.Iterator;

import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.ui.NetModuleProperties;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties.OvrType;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.ModalShape;

public class NetOverlayCacheGroup extends CacheGroup implements ModalShapeContainer {
	OvrType overlayType_;
	
	protected ArrayList<CacheGroupItem> children_;		
	protected ArrayList<ModuleEntry> modules_;    
	private ArrayList<NetModuleLinkageCacheGroup> linkages_;
	
	public class ModuleEntry {
		private NetModuleCacheGroup fills_;
		private NetModuleCacheGroup edges_;
		private NetModuleCacheGroup label_;
		
		protected int quickFade_;
		
		public ModuleEntry(NetModuleCacheGroup edges, NetModuleCacheGroup fills, NetModuleCacheGroup label, NetModuleProperties nmp) {
			fills_ = fills;
			edges_ = edges;
			label_ = label;
			
			quickFade_ = nmp.getNameFadeMode();
			
			linkages_ = new ArrayList<NetModuleLinkageCacheGroup>();
		}
		
		public NetModuleCacheGroup getFills() {
			return fills_;
		}
		
		public NetModuleCacheGroup getEdges() {
			return edges_;
		}
		
		public NetModuleCacheGroup getLabel() {
			return label_;
		}
		
		public int getQuickFade() {
			return quickFade_;
		}
	};
	
	public NetOverlayCacheGroup(String id, String name, String groupType, OvrType overlayType) {
		super(id,  name, groupType);
		
		modules_ = new ArrayList<ModuleEntry>();
		children_ = new ArrayList<CacheGroupItem>();
		
		overlayType_ = overlayType;
	}
	
	public NetOverlayCacheGroup(NetworkOverlay netOverlay, OvrType overlayType) {
		this(netOverlay.getID(), netOverlay.getName(), "net_overlay", overlayType);  
	}
	
	public OvrType getOverlayType() {
		return overlayType_;
	}
	
	public void addNetModule(NetModuleCacheGroup moduleEdges, NetModuleCacheGroup moduleFills, NetModuleCacheGroup moduleLabel, NetModuleProperties nmp) {
		modules_.add(new ModuleEntry(moduleEdges, moduleFills, moduleLabel, nmp));
	}
	
	public Iterator<ModuleEntry> netModuleIterator() {
		return modules_.iterator();
	}	
	
	public void addShape(ModalShape shape, Integer majorLayer, Integer minorLayer) {
		children_.add(new CacheGroupItem(shape, majorLayer, minorLayer, false));
	}

	public Iterator<CacheGroupItem> iterator() {
		return children_.iterator();
	}
	
	public void accept(CacheGroupVisitor visitor) {
		visitor.visit(this);
	}
	
	public void addNetModuleLinkage(NetModuleLinkageCacheGroup group) {
		linkages_.add(group);
	}
	
	public Iterator<NetModuleLinkageCacheGroup> netModuleLinkageIterator() {
		return linkages_.iterator();
	}
}