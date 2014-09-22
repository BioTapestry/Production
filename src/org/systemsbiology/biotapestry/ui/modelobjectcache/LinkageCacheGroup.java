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
import java.util.Set;

import org.systemsbiology.biotapestry.genome.GenomeItem;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.ui.freerender.DrawTreeModelDataSource.ModelDataForTip;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.ModalShape;

public class LinkageCacheGroup extends CacheGroup implements ModalShapeContainer {

	private LinkageExportForWeb export_;
	
	private String sourceTag_;
	private Set<String> sharedItems_;
	
	protected ArrayList<CacheGroupItem> children_;
	
	private int launchpad_;
	private int landingpad_;
	private String target_;	
	private ModelDataForTip tipData_;
	
	private int tipSign_;

	public LinkageCacheGroup(String id, String name, String type, LinkageExportForWeb export, String sourceTag, Set<String>sharedItems, int launchPad, int landingPad, String target) {
		super(id, name, type);
		
		export_ = export;
		sourceTag_ = sourceTag;
		sharedItems_ = sharedItems;
			
		launchpad_ = launchPad;
		landingpad_ = landingPad;
		target_ = target;
		
		tipData_ = null;
		tipSign_ = 0;
		
		children_ = new ArrayList<CacheGroupItem>();
	}

	public LinkageCacheGroup(GenomeItem item, LinkageExportForWeb export,  String sourceTag, Set<String>sharedItems) {
		super(item.getID(), item.getName(), "linkage");
		
		Linkage linkage = (Linkage)item;
		
		export_ = export;
		sourceTag_ = sourceTag;
		sharedItems_ = sharedItems;
			
		launchpad_ = linkage.getLaunchPad();
		landingpad_ = linkage.getLandingPad();
		target_ = linkage.getTarget();
		
		tipData_ = null;
		tipSign_ = 0;
		
		children_ = new ArrayList<CacheGroupItem>();
	}
	
	public void accept(CacheGroupVisitor visitor) {
		visitor.visit(this);
	}

	public void addShape(ModalShape shape, Integer majorLayer, Integer minorLayer) {
		children_.add(new CacheGroupItem(shape, majorLayer, minorLayer, false));
	}

	public Iterator<CacheGroupItem> iterator() {
		return children_.iterator();
	}
	
	public String getSourceTag() {
		return sourceTag_;
	}
	
	public Set<String> getSharedItems() {
		return sharedItems_;
	}
	
	public int getLaunchPad() {
		return launchpad_;		
	}
	
	public int getLandingPad() {
		return landingpad_;
	}
	
	public String getTarget() {
		return target_;
	}
	
	public void setTipData(ModelDataForTip tipData) {
		tipData_ = tipData;
	}
	
	public ModelDataForTip getTipData() {
		return tipData_;
	}
	
	public int getTipSign() {
		return tipSign_;
	}

	public void setTipSign(int tipSign) {
		this.tipSign_ = tipSign;
	}
	
	public LinkageExportForWeb getLinkageExport() {
		return export_;
	}
}
