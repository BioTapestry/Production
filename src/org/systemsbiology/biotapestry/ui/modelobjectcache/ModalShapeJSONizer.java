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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.systemsbiology.biotapestry.cmd.AddCommands;
import org.systemsbiology.biotapestry.ui.AnnotatedFont;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.Arc;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.Ellipse;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.Line;
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
import org.systemsbiology.biotapestry.ui.modelobjectcache.transformer.FontExport;

public class ModalShapeJSONizer {
	public static ArrayList<Integer> pluckRGBA(Color color) {
		ArrayList<Integer> arr = new ArrayList<Integer>();
		arr.add(color.getRed());
		arr.add(color.getGreen());
		arr.add(color.getBlue());
		arr.add(color.getAlpha());
		
		return arr;
	}
	
	private static ArrayList<Object> strokeToArray(BasicStroke stroke) {
		ArrayList<Object> arr = new ArrayList<Object>();
		arr.add(stroke.getLineWidth());
		arr.add(stroke.getLineJoin());
		arr.add(stroke.getEndCap());		
		
		return arr;
	}
	
	private static ArrayList<Object> strokeDashPatternToArray(BasicStroke stroke) {
		float[] dash = stroke.getDashArray();
		
		if (dash != null) {
			ArrayList<Object> arr = new ArrayList<Object>();
			
			for (float f : stroke.getDashArray()) {
				arr.add(f);
			}
			
			return arr;			
		}
		else {
			return null;
		}
	}
	
	public static void processTransformation(AffineTransform trans, Map<String, Object> obj) {
		double matrix[] = new double[6];
		
		trans.getMatrix(matrix);
		
		obj.put("matrix", matrix);
	}
	
	private static void processGeneralPath(GeneralPath path, ArrayList<Map<String, Object>> segmentArray) {
		PathIterator pit = path.getPathIterator(null);

		while(!pit.isDone()) {			
			Map<String, Object> segment = new HashMap<String, Object>();

			// PathIterator puts the points of the segment into an array
			float points[] = new float[6];
			int type = pit.currentSegment(points);
			
			switch(type) {
				case PathIterator.SEG_MOVETO:
					segment.put("type", "MOVETO");
					segment.put("points", Arrays.copyOf(points, 2));
					break;
				case PathIterator.SEG_LINETO:
					segment.put("type", "LINETO");
					segment.put("points", Arrays.copyOf(points, 2));
					break;
				case PathIterator.SEG_CLOSE:
					segment.put("type", "CLOSE");
					break;					
				default:
					segment.put("type", "UNKNOWN");
					break;
			}

			segmentArray.add(segment);
			pit.next();			
		}
	}

	public static void visit(TextShape text, Map<String, Object> obj) {
		
	}

	public static void visit(TextShapeForWeb text, Map<String, Object> obj) {
		obj.put("type", "text");
		obj.put("text", text.text_);
		obj.put("x", text.txtX_);
		obj.put("y", text.txtY_);
		obj.put("w", text.width_);
		obj.put("h", text.height_);
		obj.put("color", pluckRGBA(text.color_));

		// Export only overridden fonts
		if (text.font_.isOverride()) {
			obj.put("font", new FontExport(text.font_.getFont()));
		}
	}
	
	public static void visit(MultiLineTextShape mlt, Map<String, Object> obj) {

	}
	
	public static void visit(MultiLineTextShapeForWeb mlt, Map<String, Object> obj) {
		obj.put("type", "mltext");
		obj.put("color", pluckRGBA(mlt.color_));
		
	    // Export only overridden fonts
	    if (mlt.font_.isOverride()) {
	      obj.put("font", new FontExport(mlt.font_.getFont()));
	    }
	    
		ArrayList<Map<String, Object>> fragments = new ArrayList<Map<String, Object>>();
		
		for (MultiLineTextShapeForWeb.Fragment frag : mlt.fragments_) {
			Map<String, Object> fragObj = new HashMap<String, Object>();
			
			fragObj.put("text", frag.frag_);
			fragObj.put("x", frag.x_);
			fragObj.put("y", frag.y_);
			fragObj.put("w", frag.width_);
			fragObj.put("h", frag.height_);
		
			fragments.add(fragObj);
		}
		
		obj.put("fragments", fragments);
	}	
	
	public static void visit(Ellipse shape, Map<String, Object> obj) {
		obj.put("type", "ellipse");
		obj.put("cx", shape.getCenterX());
		obj.put("cy", shape.getCenterY());
		obj.put("r", shape.getRadius());
		obj.put("color", pluckRGBA(shape.color_));
		obj.put("mode", shape.mode_);
		obj.put("stroke", strokeToArray(shape.stroke_));
	}

	public static void visit(Arc shape, Map<String, Object> obj) {
		obj.put("type", "arc");
		obj.put("cx", shape.getCenterX());
		obj.put("cy", shape.getCenterY());
		obj.put("r", shape.getRadius());
		obj.put("start", shape.getStart());		
		obj.put("extent", shape.getExtent());
		obj.put("draw_type", shape.getType());
		obj.put("color", pluckRGBA(shape.color_));
		obj.put("mode", shape.mode_);
		obj.put("stroke", strokeToArray(shape.stroke_));
	}

	public static void visit(Rectangle shape, Map<String, Object> obj) {
		obj.put("type", "rect");
		obj.put("x", shape.getX());
		obj.put("y", shape.getY());
		obj.put("w", shape.getWidth());
		obj.put("h", shape.getHeight());
		obj.put("color", pluckRGBA(shape.color_));
		obj.put("mode", shape.mode_);		
		obj.put("stroke", strokeToArray(shape.stroke_));
	}
	
	public static void visit(Line shape, Map<String, Object> obj) {
		obj.put("type", "line");
		obj.put("x1", shape.getX1());
		obj.put("y1", shape.getY1());	
		obj.put("x2", shape.getX2());
		obj.put("y2", shape.getY2());
		obj.put("color", pluckRGBA(shape.color_));
		obj.put("stroke", strokeToArray(shape.stroke_));
	}
	
	public static void visit(SegmentedPathShape shape, Map<String, Object> obj) {
		obj.put("type", "path");
		obj.put("mode", shape.mode_);		
		obj.put("color", pluckRGBA(shape.color_));
		obj.put("stroke", strokeToArray(shape.stroke_));
		
		ArrayList<Object> dash = strokeDashPatternToArray(shape.stroke_);
		if (dash != null) {
			obj.put("style", dash);
		}
		
		ArrayList<Map<String, Object>> segmentArray = new ArrayList<Map<String, Object>>();
		processGeneralPath(shape.path_, segmentArray);
		
		obj.put("segments", segmentArray);
	}
	
	public static void visit(PushTransformOperation op, Map<String, Object> obj) {
		obj.put("type", "push_transform");
		processTransformation(op.getTransform(), obj);
	}
	
	public static void visit(PopTransformOperation op, Map<String, Object> obj) {
		obj.put("type", "pop_transform");
	}
	
	public static void visit(PushRotationWithOrigin op, Map<String, Object> obj) {
		obj.put("type", "push_rot_orig");
		obj.put("x", op.getOriginX());
		obj.put("y", op.getOriginY());
		obj.put("t", op.getTheta());
	}
	
	public static void visit(SetComposite op, Map<String, Object> obj) {
		obj.put("type", "set_comp");
		
		int rule = op.alpha_.getRule();
		
		switch (rule) {
			case AlphaComposite.CLEAR: 
				obj.put("rule","CLEAR");			
				break;
			case AlphaComposite.SRC:
				obj.put("rule","SRC");			
				break;				
			case AlphaComposite.DST:
				obj.put("rule","DST");			
				break;
		}
	}
}
