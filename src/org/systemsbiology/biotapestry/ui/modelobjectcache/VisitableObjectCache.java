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
import java.util.Map;
import java.util.TreeMap;

public class VisitableObjectCache extends ModelObjectCache {
	
	////////////////////////////////////////////////////////////////////////////
	//
	// PRIVATE MEMBERS
	//
	////////////////////////////////////////////////////////////////////////////   
	
	// TODO remove
	private Map<String, CacheGroup> groupMap_;
	private Map<Integer, Map<Integer, ArrayList<ModalShape>>> layerCache_;
	
	private Map<DrawLayer, ArrayList<CacheGroup>> layeredGroupCache_;

	private DrawLayer currentDrawLayer_;
	
	////////////////////////////////////////////////////////////////////////////
	//
	// PUBLIC CONSTRUCTORS
	//
	////////////////////////////////////////////////////////////////////////////

	public VisitableObjectCache() {
		groupMap_ = new TreeMap<String, CacheGroup>();	
		layerCache_ = new TreeMap<Integer, Map<Integer, ArrayList<ModalShape>>>();
		layeredGroupCache_ = new TreeMap<DrawLayer, ArrayList<CacheGroup>>();
		
		for (DrawLayer layer : DrawLayer.values()) {
			layeredGroupCache_.put(layer, new ArrayList<CacheGroup>());
		}
	}	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// PRIVATE METHODS
	//
	////////////////////////////////////////////////////////////////////////////
	
	private void addToLayers(ModalShape ms, Integer majorLayer, Integer minorLayer) {
		Map<Integer, ArrayList<ModelObjectCache.ModalShape>> perMajor = layerCache_.get(majorLayer);
		if (perMajor == null) {
			perMajor = new TreeMap<Integer, ArrayList<ModelObjectCache.ModalShape>>();
			layerCache_.put(majorLayer, perMajor);
		}
		ArrayList<ModalShape> perMinor = perMajor.get(minorLayer);
		if (perMinor == null) {
			perMinor = new ArrayList<ModalShape>();
			perMajor.put(minorLayer, perMinor);
		}
		perMinor.add(ms);
	}
	
	////////////////////////////////////////////////////////////////////////////
	//
	// PUBLIC METHODS
	//
	////////////////////////////////////////////////////////////////////////////

	/***************************************************************************
	 **
	 ** Clear the object cache
	 */
	public void clear() {
		layerCache_.clear();
		return;
	}

	public void setDrawLayer(DrawLayer layer) {
		currentDrawLayer_ = layer;
	}
	
	public void addGroup(CacheGroup group) {
		Iterator<CacheGroupItem> iter = group.iterator();
		
		while (iter.hasNext()) {
			CacheGroupItem item = iter.next();
			addToLayers(item.shape_, item.majorLayer_, item.minorLayer_);
		}
		
		groupMap_.put(group.getId(), group);
		
		layeredGroupCache_.get(currentDrawLayer_).add(group);
	}

	public Iterator<CacheGroup> getDrawLayerIterator(DrawLayer drawLayer) {
		return layeredGroupCache_.get(drawLayer).iterator();
	}

	public void accept(CacheGroupVisitor visitor) {
		for (String groupID : groupMap_.keySet()) {
			CacheGroup group = groupMap_.get(groupID);
			group.accept(visitor);
		}
	}
	
	public void accept(DrawLayer layer, CacheGroupVisitor visitor) {
		ArrayList<CacheGroup> layerGroups = layeredGroupCache_.get(layer);  
		Iterator<CacheGroup> iter = layerGroups.iterator();
		
		while (iter.hasNext()) {
			CacheGroup group = iter.next();
			group.accept(visitor);
		}
	}		
	
	public void accept(CacheLayerVisitor visitor) {
		// Every major layer
		for (Integer majorKey : layerCache_.keySet()) {
			visitor.setMajorLayer(majorKey);
			Map<Integer, ArrayList<ModalShape>> perMajor = layerCache_.get(majorKey);

			for (Integer minorKey : perMajor.keySet()) {
				visitor.setMinorLayer(minorKey);
				ArrayList<ModalShape> shapes = perMajor.get(minorKey);		  	

				for (ModalShape ms : shapes) {
					ms.accept(visitor);
				}
			}
		}
	}
}
