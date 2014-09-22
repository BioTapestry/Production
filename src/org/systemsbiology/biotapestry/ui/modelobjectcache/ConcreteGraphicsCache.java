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

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ConcreteGraphicsCache extends ModelObjectCache {
	private static final int NUM_MAJOR_LAYERS = 9;
	private static final int NUM_MINOR_LAYERS = 9;
	
	private Map<Integer, Map<Integer, List<ConcreteRenderCommand>>> layerCache_;
	private LinkedList<AffineTransform> transformStack_;
	
	private ArrayList<ArrayList<ArrayList<ConcreteRenderCommand>>> groupCacheArr_;
	private Map<DrawLayer, ArrayList<ArrayList<ArrayList<ConcreteRenderCommand>>>> layeredGroupCacheArrMap_;
	
	private DrawLayer currentDrawLayer_;
	
	////////////////////////////////////////////////////////////////////////////
	//
	// PUBLIC CONSTRUCTORS
	//
	////////////////////////////////////////////////////////////////////////////

	public ConcreteGraphicsCache() {
		layerCache_ = new TreeMap<Integer, Map<Integer, List<ConcreteRenderCommand>>>();	
		transformStack_ = new LinkedList<AffineTransform>();
		
		groupCacheArr_ = new ArrayList<ArrayList<ArrayList<ConcreteRenderCommand>>>();
		layeredGroupCacheArrMap_ = new TreeMap<DrawLayer, ArrayList<ArrayList<ArrayList<ConcreteRenderCommand>>>>();
		
		for (int major = 0; major < NUM_MAJOR_LAYERS; major++) {
			ArrayList<ArrayList<ConcreteRenderCommand>> majorArr = new ArrayList<ArrayList<ConcreteRenderCommand>>();
			
			for (int minor = 0; minor < NUM_MINOR_LAYERS; minor++) {
				majorArr.add(new ArrayList<ConcreteRenderCommand>());
			}
			
			groupCacheArr_.add(majorArr);
		}
		
		for (DrawLayer layer : DrawLayer.values()) {
			layeredGroupCacheArrMap_.put(layer, new ArrayList<ArrayList<ArrayList<ConcreteRenderCommand>>>());
			
			for (int major = 0; major < NUM_MAJOR_LAYERS; major++) {
				ArrayList<ArrayList<ConcreteRenderCommand>> majorArr = new ArrayList<ArrayList<ConcreteRenderCommand>>();
				
				for (int minor = 0; minor < NUM_MINOR_LAYERS; minor++) {
					majorArr.add(new ArrayList<ConcreteRenderCommand>());
				}
				
				layeredGroupCacheArrMap_.get(layer).add(majorArr);
			}
		}
	}

	////////////////////////////////////////////////////////////////////////////
	//
	// PUBLIC METHODS
	//
	////////////////////////////////////////////////////////////////////////////

	public void setDrawLayer(DrawLayer layer) {
		currentDrawLayer_ = layer;
	}
	
	public void addGroup(CacheGroup group) {
		Integer currentMajorLayer = -1;
		Integer currentMinorLayer = -1;
		Iterator<CacheGroupItem> iter = group.iterator();
		
		while (iter.hasNext()) {
			CacheGroupTranslator cgt = new CacheGroupTranslator();
			CacheGroupItem item = iter.next();
			item.shape_.accept(cgt);
			
			Iterator<ConcreteRenderCommand> command_iter = cgt.iterator();
			
			while (command_iter.hasNext()) {
				ConcreteRenderCommand cmd = command_iter.next();
				Integer itemMajorLayer = item.majorLayer_;
				Integer itemMinorLayer = item.minorLayer_;
				
				if (itemMajorLayer != currentMajorLayer || itemMinorLayer != currentMinorLayer) {
					currentMajorLayer = itemMajorLayer;
					currentMinorLayer = itemMinorLayer;
					
					// TODO / ERROR fix transforms when rendering by layers, for example when calling ConcreteGraphicsCache.renderAllLayers()
					// Java2DPushTransformCommand transCmd = new Java2DPushTransformCommand(group.getTransform());
					
					// addCommandToLayerCache(transCmd, item.majorLayer_, item.minorLayer_);
					addCommandToLayerCache(cmd, item.majorLayer_, item.minorLayer_);
					
					// addCommandToGroupCacheArray(transCmd, itemMajorLayer, itemMinorLayer);
					addCommandToGroupCacheArray(cmd, itemMajorLayer, itemMinorLayer);
					addCommandToLayeredGroupCacheArray(cmd, currentDrawLayer_, itemMajorLayer, itemMinorLayer);
				}
				else {
					addCommandToLayerCache(cmd, itemMajorLayer, itemMinorLayer);
					addCommandToGroupCacheArray(cmd, itemMajorLayer, itemMinorLayer);
					addCommandToLayeredGroupCacheArray(cmd, currentDrawLayer_, itemMajorLayer, itemMinorLayer);
				}
			}
		}
	}

	public void renderAllLayers(Graphics2D g2) {
		// Every major layer
		for (Integer majorKey : layerCache_.keySet()) {
			Map<Integer, List<ConcreteRenderCommand>> perMajor = layerCache_.get(majorKey);

		  for (Integer minorKey : perMajor.keySet()) {		  	
		  	List<ConcreteRenderCommand> commandArray = perMajor.get(minorKey);		  	
		  	
		  	for (ConcreteRenderCommand cmd : commandArray) {
		  		cmd.execute(g2, transformStack_);
		  	}
			}
		}
	}
	
	public void renderMajorLayer(Graphics2D g2, DrawLayer drawLayer, int majorLayer) {
		for (Integer majorKey : layerCache_.keySet()) {
			if (majorKey != majorLayer) {
				continue;
			}
			
			Map<Integer, List<ConcreteRenderCommand>> perMajor = layerCache_.get(majorKey);

		  for (Integer minorKey : perMajor.keySet()) {		  	
		  	List<ConcreteRenderCommand> commandArray = perMajor.get(minorKey);		  	
		  	
		  	for (ConcreteRenderCommand cmd : commandArray) {
		  		cmd.execute(g2, transformStack_);
		  	}
			}
		}
	}

	public void renderAllGroupsByLayers(Graphics2D g2) {
		for (int major = 0; major < NUM_MAJOR_LAYERS; major++) {
			for (int minor = 0; minor < NUM_MINOR_LAYERS; minor++) {
				ArrayList<ConcreteRenderCommand> commandArray = groupCacheArr_.get(major).get(minor);
				
				for (ConcreteRenderCommand cmd : commandArray) {
					cmd.execute(g2, transformStack_);
				}
			}
		}
	}
	
	public void renderAllGroupsInMajorLayer(Graphics2D g2, DrawLayer drawLayer, int majorLayer) {
		ArrayList<ArrayList<ArrayList<ConcreteRenderCommand>>> drawLayerArrays = layeredGroupCacheArrMap_.get(drawLayer);
		
		ArrayList<ArrayList<ConcreteRenderCommand>> majorArr = drawLayerArrays.get(majorLayer);
		
		for (int minor = 0; minor < NUM_MINOR_LAYERS; minor++) {
			ArrayList<ConcreteRenderCommand> commandArray = majorArr.get(minor);
			
			for (ConcreteRenderCommand cmd : commandArray) {
				cmd.execute(g2, transformStack_);
			}
		}
	}
	
	public void renderAllGroupsInDrawLayer(Graphics2D g2, DrawLayer layer) {
		ArrayList<ArrayList<ArrayList<ConcreteRenderCommand>>> drawLayerArrays = layeredGroupCacheArrMap_.get(layer);
		
		for (int major = 0; major < NUM_MAJOR_LAYERS; major++) {
			ArrayList<ArrayList<ConcreteRenderCommand>> majorArr = drawLayerArrays.get(major);			
			
			for (int minor = 0; minor < NUM_MINOR_LAYERS; minor++) {
				ArrayList<ConcreteRenderCommand> commandArray = majorArr.get(minor);
				
				for (ConcreteRenderCommand cmd : commandArray) {
					cmd.execute(g2, transformStack_);
				}
			}
		}
	}
		
	public void addCommandToGroupCacheArray(ConcreteRenderCommand cmd, Integer majorLayer, Integer minorLayer) {
    groupCacheArr_.get(majorLayer).get(minorLayer).add(cmd);
  }
	
	public void addCommandToLayeredGroupCacheArray(ConcreteRenderCommand cmd, DrawLayer layer, Integer majorLayer, Integer minorLayer) {
		ArrayList<ArrayList<ArrayList<ConcreteRenderCommand>>> arr = layeredGroupCacheArrMap_.get(layer);
    
    arr.get(majorLayer).get(minorLayer).add(cmd);
  }
	
	public void addCommandToGroupCache(Map<Integer, Map<Integer, List<ConcreteRenderCommand>>> cache,
			ConcreteRenderCommand cmd, Integer majorLayer, Integer minorLayer) {
		
    Map<Integer, List<ConcreteRenderCommand>> perMajor = cache.get(majorLayer);
    if (perMajor == null) {
      perMajor = new TreeMap<Integer, List<ConcreteRenderCommand>>();
      cache.put(majorLayer, perMajor);
    }
    List<ConcreteRenderCommand> perMinor = perMajor.get(minorLayer);
    if (perMinor == null) {
      perMinor = new ArrayList<ConcreteRenderCommand>();
      perMajor.put(minorLayer, perMinor);
    }
    perMinor.add(cmd);   
  }
	
  public void addCommandToLayerCache(ConcreteRenderCommand cmd, Integer majorLayer, Integer minorLayer) {   
    Map<Integer, List<ConcreteRenderCommand>> perMajor = layerCache_.get(majorLayer);
    if (perMajor == null) {
      perMajor = new TreeMap<Integer, List<ConcreteRenderCommand>>();
      layerCache_.put(majorLayer, perMajor);
    }
    List<ConcreteRenderCommand> perMinor = perMajor.get(minorLayer);
    if (perMinor == null) {
      perMinor = new ArrayList<ConcreteRenderCommand>();
      perMajor.put(minorLayer, perMinor);
    }
    perMinor.add(cmd);   
  }
  
	private static class CacheGroupTranslator implements ModalShapeVisitor {
		private ArrayList<ConcreteRenderCommand> commands_;
		
		public CacheGroupTranslator() {
			commands_ = new ArrayList<ConcreteRenderCommand>();			
		}
		
		public void visit(ModelObjectCache.Arc modalShape) {
			int type;
			
			switch(modalShape.getType()) {
				case CHORD: {
					type = Arc2D.CHORD;
					break;
				}
				case OPEN: {
					type = Arc2D.OPEN;
					break;
				}
				case PIE: {
					type = Arc2D.PIE;
					break;
				}
				default: {
					throw new IllegalArgumentException();
				}
			}
			
			double cx = modalShape.getCenterX();
			double cy = modalShape.getCenterY();
			double radius = modalShape.getRadius();
			
			Arc2D.Double arc = new Arc2D.Double(cx - radius, cy - radius, 2.0 * radius, 2.0 * radius,
					modalShape.getStart(), modalShape.getExtent(), type);
			
			commands_.add(new Java2DSetStrokeCommand(modalShape.stroke_));
			commands_.add(new Java2DSetColorCommand(modalShape.color_));
			commands_.add(new Java2DDrawShapeCommand(arc, modalShape.mode_));
		}
		
		public void visit(ModelObjectCache.Ellipse modalShape) {
			double cx = modalShape.getCenterX();
			double cy = modalShape.getCenterY();
			double radius = modalShape.getRadius();
			
			Ellipse2D.Double circ = new Ellipse2D.Double(cx - radius, cy - radius, 2.0 * radius, 2.0 * radius);

			commands_.add(new Java2DSetStrokeCommand(modalShape.stroke_));
			commands_.add(new Java2DSetColorCommand(modalShape.color_));
			commands_.add(new Java2DDrawShapeCommand(circ, modalShape.mode_));
		}

		public void visit(ModelObjectCache.Line modalShape) {
			Line2D.Double line = new Line2D.Double(modalShape.getX1(), modalShape.getY1(), modalShape.getX2(), modalShape.getY2());
			
			commands_.add(new Java2DSetStrokeCommand(modalShape.stroke_));
			commands_.add(new Java2DSetColorCommand(modalShape.color_));
			commands_.add(new Java2DDrawShapeCommand(line, modalShape.mode_));
		}

		public void visit(ModelObjectCache.Rectangle modalShape) {
			Rectangle2D.Double rect = new Rectangle2D.Double(modalShape.getX(), modalShape.getY(), modalShape.getWidth(), modalShape.getHeight());
		
			commands_.add(new Java2DSetStrokeCommand(modalShape.stroke_));
			commands_.add(new Java2DSetColorCommand(modalShape.color_));
			commands_.add(new Java2DDrawShapeCommand(rect, modalShape.mode_));
		}

		public void visit(TextShape text) {
			commands_.add(new Java2DDrawTextCommand(text));
		}

		public void visit(TextShapeForWeb text) {

		}
		
		public void visit(MultiLineTextShape mlt) {
			commands_.add(new Java2DSetColorCommand(mlt.color_));			
			commands_.add(new Java2DDrawMultiLineTextCommand(mlt));
		}
		
		public void visit(MultiLineTextShapeForWeb mlt) {

		}		

		public void visit(SegmentedPathShape modalShape) {
			commands_.add(new Java2DSetStrokeCommand(modalShape.stroke_));
			commands_.add(new Java2DSetColorCommand(modalShape.color_));
			commands_.add(new Java2DDrawPathCommand(modalShape.path_, modalShape.mode_));
		}
		
		public void visit(PushTransformOperation op) {
			commands_.add(new Java2DPushTransformCommand(op.getTransform()));
		}
		
		public void visit(PopTransformOperation op) {
			commands_.add(new Java2DPopTransformCommand());
		}
		
		public void visit(PushRotationWithOrigin op) {
			double x = op.getOriginX();
			double y = op.getOriginY();
			double thetaDegrees = op.getTheta();
			double thetaRadias = thetaDegrees * Math.PI / 180.0;
			
			AffineTransform trans = new AffineTransform();			
			trans.rotate(thetaRadias, x, y);
			commands_.add(new Java2DPushTransformCommand(trans));
		}
		
		public void visit(ModelObjectCache.SetComposite op) {
			commands_.add(new Java2DSetCompositeCommand(op.alpha_));
		}
		
		public Iterator<ConcreteRenderCommand> iterator() {
			return commands_.iterator();
		}
	}
}
