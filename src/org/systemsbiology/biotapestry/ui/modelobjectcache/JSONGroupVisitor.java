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

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.NetModuleLinkage;
import org.systemsbiology.biotapestry.ui.LinkSegment;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.SuggestedDrawStyle;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree.TaggedShape;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.Arc;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.Ellipse;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.Line;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.ModalShape;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.MultiLineTextShape;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.MultiLineTextShapeForWeb;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.PopTransformOperation;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.PushRotationWithOrigin;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.PushTransformOperation;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.Rectangle;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.SegmentedPathShape;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.SetComposite;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.TextShape;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.TextShapeForWeb;
import org.systemsbiology.biotapestry.ui.modelobjectcache.NetOverlayCacheGroup.ModuleEntry;
import org.systemsbiology.biotapestry.util.Vector2D;

public class JSONGroupVisitor extends CacheGroupVisitor implements
		ModalShapeVisitor {
	private List<Object> groupArray_;
	private Map<String, Object> currentObject_;

	public JSONGroupVisitor() {
		groupArray_ = new ArrayList<Object>();
	}

	public void processBounds(ArrayList<Map<String, Object>> targetArray, ArrayList<ModelObjectCache.ModalShape> bounds) {
		BoundingShapeVisitor bsv = new BoundingShapeVisitor();

		for (ModelObjectCache.ModalShape ms : bounds) {
			Map<String, Object> currentBoundShape = new HashMap<String, Object>();
			bsv.setCurrentObject(currentBoundShape);
			ms.accept(bsv);

			targetArray.add(currentBoundShape);
		}
	}

	private void addLayerInfo(Integer majorLayer, Integer minorLayer) {
		ArrayList<Integer> arr = new ArrayList<Integer>();
		arr.add(majorLayer);
		arr.add(minorLayer);
		currentObject_.put("layer", arr);
	}

	private void processLinkageSegments(Map<String, Object> target, ArrayList<LinkSegmentExport> segmentArray) {
		ArrayList<Map<String, Object>> arr = new ArrayList<Map<String,Object>>();
		
		for (LinkSegmentExport lse : segmentArray) {
			Map<String, Object> obj = new HashMap<String, Object>();
			linkSegmentExportGuts(lse, obj);			
			arr.add(obj);
		}
		
		target.put("segments", arr);
	}

	private void processNetModuleLinkageSegments(Map<String, Object> target, ArrayList<NetModuleLinkSegmentExport> segmentArray) {
		ArrayList<Map<String, Object>> arr = new ArrayList<Map<String, Object>>();

		for (NetModuleLinkSegmentExport lse : segmentArray) {
			Map<String, Object> obj = new HashMap<String, Object>();
			linkSegmentExportGuts(lse, obj);
			
			ArrayList<String> segment_links = new ArrayList<String>();
			for (Iterator<String> link_iter = lse.getLinks().iterator(); link_iter.hasNext();) {
				String segment_link_id = link_iter.next();
				segment_links.add(segment_link_id);
			}

			obj.put("links", segment_links);
			arr.add(obj);
		}

		target.put("segments", arr);
	}
	
	private void linkSegmentExportGuts(LinkSegmentExport lse, Map<String, Object> obj) {
		LinkSegment ls = lse.getSegment();
		LinkSegmentID lsid = lse.getID();
		
		int lineStyle = lse.getLineStyle();
		
		Point2D start = ls.getStart();
		
		if (start != null) {
			obj.put("sx", start.getX());
			obj.put("sy", start.getY());
		}
		else {
			obj.put("sx", "null");
			obj.put("sy", "null");				
		}

		Point2D end = ls.getEnd();
		if (end != null) {
			obj.put("ex", end.getX());
			obj.put("ey", end.getY());
		}
		else {
			obj.put("ex", "null");
			obj.put("ey", "null");				
		}
					
		Vector2D normal = ls.getNormal();
		if (normal != null) {
			obj.put("nx", normal.getX());
			obj.put("ny", normal.getY());
		}
		else {
			obj.put("nx", "null");
			obj.put("ny", "null");				
		}			
		
		Vector2D run = ls.getRun();			
		if (run != null) {
			obj.put("rx", run.getX());
			obj.put("ry", run.getY());
		}
		else {
			obj.put("rx", "null");
			obj.put("ry", "null");				
		}				
		
		obj.put("islink", lsid.isForSegment());
		obj.put("isonly", lsid.isDirect());
		
		obj.put("endid", lsid.getEndID());
		
		String label = lsid.getLabel();
		if (label != null) {
			obj.put("label", label);
		}
		else {
			obj.put("label", "null");
		}
		
		obj.put("thick", lse.getThickness());			
		obj.put("selthick", lse.getSelectionThickness());
		obj.put("color", ModalShapeJSONizer.pluckRGBA(lse.getColor()));
		
		if ((lineStyle != SuggestedDrawStyle.NO_STYLE) && (lineStyle != SuggestedDrawStyle.SOLID_STYLE)) {
			obj.put("style", lse.getLineStyle());
		}
	}

	public void visit(TextShape text) {
		// Do not export
		currentObject_ = null;
	}
	
	public void visit(TextShapeForWeb text) {
		ModalShapeJSONizer.visit(text, currentObject_);
	}	

	public void visit(MultiLineTextShape mlt) {
		// Do not export
		currentObject_ = null;
	}

	public void visit(MultiLineTextShapeForWeb mlt) {
		ModalShapeJSONizer.visit(mlt, currentObject_);
	}
	
	public void visit(Ellipse shape) {
		ModalShapeJSONizer.visit(shape, currentObject_);
	}

	public void visit(Arc shape) {
		ModalShapeJSONizer.visit(shape, currentObject_);
	}

	public void visit(Rectangle shape) {
		ModalShapeJSONizer.visit(shape, currentObject_);
	}

	public void visit(Line shape) {
		ModalShapeJSONizer.visit(shape, currentObject_);
	}

	public void visit(SegmentedPathShape shape) {
		ModalShapeJSONizer.visit(shape, currentObject_);
	}

	public void visit(PushTransformOperation op) {
		ModalShapeJSONizer.visit(op, currentObject_);
	}

	public void visit(PopTransformOperation op) {
		ModalShapeJSONizer.visit(op, currentObject_);
	}

	public void visit(PushRotationWithOrigin op) {
		ModalShapeJSONizer.visit(op, currentObject_);
	}

	public void visit(SetComposite op) {
		// Do not export
		currentObject_ = null;
	}

	public void addObjectGraphToMap(Map<String, Object> map, String key) {
		map.put(key, groupArray_);
	}

	private HashMap<String, Object> getExportableObject() {
		return new HashMap<String, Object>();
	}
		
	public void exportIdTypeAndName(Map<String, Object> obj, CacheGroup group) {
		obj.put("id", group.getId());
		obj.put("type", group.getType());
		obj.put("name", group.getName());
	}
	
	public void exportBoundShapes(Map<String, Object> obj, BoundShapeContainer group) {
		ArrayList<Map<String, Object>> processed_bounds = new ArrayList<Map<String, Object>>();
		processBounds(processed_bounds, group.getBounds());
		obj.put("bounds", processed_bounds);		
	}

	public void exportShapes(Map<String, Object> obj, Boolean selected_flag_value, String field_name, Iterator<CacheGroupItem> iter) {
		ArrayList<Map<String, Object>> shapes = new ArrayList<Map<String, Object>>();

		while (iter.hasNext()) {
			CacheGroupItem item = iter.next();
			currentObject_ = new HashMap<String, Object>();
			item.shape_.accept(this);
			
			if (currentObject_ != null) {
				addLayerInfo(item.majorLayer_, item.minorLayer_);
				
				if (item.selected_ == selected_flag_value) {
					shapes.add(currentObject_);
				}
			}
		}
		
		obj.put(field_name, shapes);
	}
	
	public void exportModalShapeArray(Map<String, Object> obj, String field_name, Iterator<ModalShape> iter) {
		ArrayList<Map<String, Object>> shapes = new ArrayList<Map<String, Object>>();

		while (iter.hasNext()) {
			ModalShape item = iter.next();
			currentObject_ = new HashMap<String, Object>();
			item.accept(this);
			
			shapes.add(currentObject_);
		}
		
		obj.put(field_name, shapes);
	}	
	
	public void exportAllShapes(Map<String, Object> obj, ModalShapeContainer group) {
		exportShapes(obj, false, "shapes", group.iterator());
		exportShapes(obj, true, "selected_shapes", group.iterator());
	}
	
	public void exportLinkages(Map<String, Object> target, LinkageExportForWeb lfew) {
		Map<String, Linkage> linkageMap = lfew.getLinkages();
		Iterator<String> iter = linkageMap.keySet().iterator();
		
		while (iter.hasNext()) {
			HashMap<String, Object> linkageObj = getExportableObject();
			String linkID = iter.next();
			
			Linkage linkage = linkageMap.get(linkID);
			linkageObj.put("sign", linkage.getSign());
			linkageObj.put("launchpad", linkage.getLaunchPad());
			linkageObj.put("landingpad", linkage.getLandingPad());
			linkageObj.put("srctag", linkage.getSource());
			linkageObj.put("trg", linkage.getTarget());
			
			target.put(linkID, linkageObj);
		}
	}
	
	public void exportNetModuleLinkages(Map<String, Object> target, NetModuleLinkageExportForWeb lfew) {
		Map<String, NetModuleLinkage> linkageMap = lfew.getLinkages();
		Iterator<String> iter = linkageMap.keySet().iterator();
		
		while (iter.hasNext()) {
			HashMap<String, Object> linkageObj = getExportableObject();
			String linkID = iter.next();
			
			NetModuleLinkage linkage = linkageMap.get(linkID);
			linkageObj.put("sign", linkage.getSign());
			linkageObj.put("srcmodule", linkage.getSource());
			linkageObj.put("trgmodule", linkage.getTarget());
			
			target.put(linkID, linkageObj);
		}
	}	
	
	private void linkageExportGuts(LinkageCacheGroup group, Map<String, Object> obj) {
		exportIdTypeAndName(obj, group);
		exportShapes(obj, false, "shapes", group.iterator());
		
		LinkageExportForWeb lefw = group.getLinkageExport();
				
		ArrayList<LinkSegmentExport> segmentArray = (ArrayList<LinkSegmentExport>) lefw.getSegmentExports();
		processLinkageSegments(obj, segmentArray);
		
		Map<String, Object> linkages = getExportableObject();
		exportLinkages(linkages, lefw);
		
		obj.put("linkages", linkages);
		obj.put("shared", group.getSharedItems());
		obj.put("srctag", group.getSourceTag());
		obj.put("pad", group.getLaunchPad());
		obj.put("lpad", group.getLandingPad());
		obj.put("trg", group.getTarget());
	}
	
	public void visit(LinkageCacheGroup group) {
		Map<String, Object> obj = getExportableObject();

		linkageExportGuts(group, obj);
		
		groupArray_.add(obj);		
	}
	
	public void visit(NetModuleLinkageCacheGroup group) {
	
	}	
	
	public void netModuleExportGuts(Map<String, Object> obj, ModuleEntry entry){
		
		// ID, type and name are identical in both edges and fills CacheGroups
		NetModuleCacheGroup edges = entry.getEdges();
		
		exportIdTypeAndName(obj, edges);
		exportShapes(obj, false, "edges", edges.iterator());
		exportShapes(obj, false, "fills", entry.getFills().iterator());
		exportShapes(obj, false, "label", entry.getLabel().iterator());
		
		BoundingShapeVisitor bsv = new BoundingShapeVisitor();
		
		// Export the outer bound shape
		Map<String, Object> currentBoundShape = new HashMap<String, Object>();
		bsv.setCurrentObject(currentBoundShape);
		edges.getOuterBoundShape().accept(bsv);
		obj.put("outerbounds", currentBoundShape);
		
		// Export the label bound shape
		currentBoundShape = new HashMap<String, Object>();
		bsv.setCurrentObject(currentBoundShape);
		edges.getNameBoundShape().accept(bsv);
		obj.put("namebounds", currentBoundShape);
		
		ArrayList<Map<String, Object>> exportshape = new ArrayList<Map<String, Object>>();
		List<TaggedShape> shapelist = edges.getShape();
		
		// Export TaggedShapes (output from NetModuleFree.getShape)
		for (Iterator<TaggedShape> shape_iter = shapelist.iterator(); shape_iter.hasNext(); ) {
			TaggedShape tShape = shape_iter.next();
			if (tShape.shapeClass == TaggedShape.FULL_DRAW_SHAPE) {
				continue;
			}
			
			Map<String, Object> shape_obj = getExportableObject();
			
			shape_obj.put("type", tShape.shapeClass);
			
			Map<String, Object> exportrect = getExportableObject();
			Rectangle2D rectangle2d = tShape.rect;
	  		ModelObjectCache.Rectangle rect = ModalShapeFactory.buildRectangle(rectangle2d.getMinX(), rectangle2d.getMinY(), rectangle2d.getWidth(), rectangle2d.getHeight());
			ModalShapeJSONizer.visit(rect, exportrect);
			shape_obj.put("rect", exportrect);
			
			exportshape.add(shape_obj);
		}
		
		obj.put("shape", exportshape);
			
		// Export module member IDs
		obj.put("members", edges.getMembers());
		
		// NetModuleProperties NameFadeMode
		obj.put("nfm", entry.getQuickFade());
	}
	
	public void visit(NetModuleCacheGroup group) {
		Map<String, Object> obj = getExportableObject();	
	}
	
	public void visit(NetOverlayCacheGroup group) {
		Map<String, Object> obj = getExportableObject();
		
		exportIdTypeAndName(obj, group);

		exportShapes(obj, false, "shapes", group.iterator());

		obj.put("ovrtype", group.getOverlayType());
				
		// Export the net modules in this overlay
		ArrayList<Map<String, Object>> overlay_modules = new ArrayList<Map<String, Object>>();
		for (Iterator<ModuleEntry> iter = group.netModuleIterator(); iter.hasNext();) {
			ModuleEntry entry = iter.next();			
			
			// Edges
			Map<String, Object> nmf_obj = getExportableObject();
			netModuleExportGuts(nmf_obj, entry);
			
			overlay_modules.add(nmf_obj);
		}
		
		obj.put("modules", overlay_modules);
		
		ArrayList<Map<String, Object>> real_linkages = new ArrayList<Map<String, Object>>();
		
		// Export NetModuleLinkages of the overlay
		for (Iterator<NetModuleLinkageCacheGroup> iter = group.netModuleLinkageIterator(); iter.hasNext();) {
			Map<String, Object> linkage_obj = getExportableObject();
			NetModuleLinkageCacheGroup lcg = iter.next();
			exportIdTypeAndName(linkage_obj, lcg);
			NetModuleLinkageExportForWeb lefw = lcg.getNetModuleLinkageExport();
			
			ArrayList<NetModuleLinkSegmentExport> segmentArray = (ArrayList<NetModuleLinkSegmentExport>) lefw.getSegmentExports();
			processNetModuleLinkageSegments(linkage_obj, segmentArray);
			
			Map<String, Object> linkages = getExportableObject();			
			exportNetModuleLinkages(linkages, lefw);
			
			linkage_obj.put("linkages", linkages);
			real_linkages.add(linkage_obj);
		}
		
		obj.put("linkages", real_linkages);
		
		groupArray_.add(obj);
	}
	
	public void visit(GroupFreeCacheGroup group) {
		Map<String, Object> obj = getExportableObject();
		
		exportIdTypeAndName(obj, group);
		exportShapes(obj, false, "toggled", group.iteratorForToggledItems());
		exportShapes(obj, false, "nontoggled", group.iteratorForNonToggledItems());		
		exportBoundShapes(obj, group);
		
		obj.put("childid", group.getChildId());
		obj.put("order", group.getOrder());
		obj.put("layer", group.getLayer());
		
		groupArray_.add(obj);
	}	
	
	public void visit(GeneCacheGroup group) {
		Map<String, Object> obj = getExportableObject();		
		exportIdTypeAndName(obj, group);
		exportAllShapes(obj, group);
		exportBoundShapes(obj, group);
		
		exportShapes(obj, false, "padshapes", group.iteratorForPadShapes());
		
		obj.put("lpadx", group.getLaunchPadOffset().getX());
		obj.put("lpady", group.getLaunchPadOffset().getY());
		
		ArrayList<Map<String, Object>> pads = new ArrayList<Map<String, Object>>();
		for (Iterator<GeneCacheGroup.Pad> iter = group.iteratorForPadCoordiantes(); iter.hasNext();) {
			GeneCacheGroup.Pad pad = iter.next();
			
			Map<String, Object> pad_obj = getExportableObject();
			
			pad_obj.put("x", pad.x_);
			pad_obj.put("y", pad.y_);
			
			pads.add(pad_obj);
		}		
		
		obj.put("pads", pads);
		groupArray_.add(obj);
	}
	
	public void visit(CommonCacheGroup group) {
		Map<String, Object> obj = getExportableObject();		
		exportIdTypeAndName(obj, group);
		exportAllShapes(obj, group);
		exportBoundShapes(obj, group);	
		groupArray_.add(obj);
	}
}
