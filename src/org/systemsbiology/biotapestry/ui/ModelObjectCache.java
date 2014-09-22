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

package org.systemsbiology.biotapestry.ui;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.BasicStroke;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;

import java.util.TreeMap;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import flexjson.JSONSerializer;


/****************************************************************************
 **
 ** Handles rendering of cached drawing objects
 */
public class ModelObjectCache {
	
	////////////////////////////////////////////////////////////////////////////
	//
	// PRIVATE CONSTANTS
	//
	//////////////////////////////////////////////////////////////////////////// 

	////////////////////////////////////////////////////////////////////////////
	//
	// PRIVATE MEMBERS
	//
	////////////////////////////////////////////////////////////////////////////   

	private TreeMap cache_;
	private FontRenderContext frc_;

	////////////////////////////////////////////////////////////////////////////
	//
	// PUBLIC CONSTRUCTORS
	//
	////////////////////////////////////////////////////////////////////////////

	/***************************************************************************
	 **
	 ** Null constructor
	 */

	public ModelObjectCache() {
		cache_ = new TreeMap();
		frc_ = new FontRenderContext(new AffineTransform(), true, true);      
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
		cache_.clear();
		return;
	}  
	
	
	public void addObjectToCache(TheModalShape ms, Integer majorLayer, Integer minorLayer) {
		TreeMap perMajor = (TreeMap)cache_.get(majorLayer);
		if (perMajor == null) {
			perMajor = new TreeMap();
			cache_.put(majorLayer, perMajor);
		}
		ArrayList perMinor = (ArrayList)perMajor.get(minorLayer);
		if (perMinor == null) {
			perMinor = new ArrayList();
			perMajor.put(minorLayer, perMinor);
		}
		perMinor.add(ms);
		return;
	}
	
	public String getJsonRepresentation() {
		JSONVisitor visitor = new JSONVisitor();
		this.accept(visitor);
		return visitor.getJSONString();		
	}
	
	public void addShape(Arc2D shape, TheModalShape.Mode mode, Color color, BasicStroke stroke, Integer majorLayer, Integer minorLayer) {
		TheModalShape ms = new Arc(shape, mode, color, stroke);
		addObjectToCache(ms, majorLayer, minorLayer);
	}
	
	public void addShape(Ellipse2D shape, TheModalShape.Mode mode, Color color, BasicStroke stroke, Integer majorLayer, Integer minorLayer) {
		TheModalShape ms = new Ellipse(shape, mode, color, stroke);
		addObjectToCache(ms, majorLayer, minorLayer);
	}

	public void addShape(Rectangle2D shape, TheModalShape.Mode mode, Color color, BasicStroke stroke, Integer majorLayer, Integer minorLayer) {
		TheModalShape ms = new Rectangle(shape, mode, color, stroke);
		addObjectToCache(ms, majorLayer, minorLayer);
	}

	public void addShape(Line2D shape, TheModalShape.Mode mode, Color color, BasicStroke stroke, Integer majorLayer, Integer minorLayer) {
		TheModalShape ms = new Line(shape, mode, color, stroke);
		addObjectToCache(ms, majorLayer, minorLayer);
	}
	
	public void addShape(TextShape shape) {
		
	}
	
	public void addShape(MultiLineTextShape shape) {
		
	}
	
	public void accept(ModalShapeVisitor visitor) {
		// Every major layer
		for (Object majorKey : cache_.keySet()) {
			visitor.setMajorLayer((Integer) majorKey);
			
			TreeMap perMajor = (TreeMap)cache_.get(majorKey);

			// Every minor layer
		  for (Object minorKey : perMajor.keySet()) {
		  	
		  	visitor.setMinorLayer((Integer) minorKey);
		  	
		  	ArrayList<TheModalShape> shapes = (ArrayList<TheModalShape>)perMajor.get(minorKey);		  	
		  	
		  	for (TheModalShape ms : shapes) {
		  		ms.accept(visitor);
				}
			}
		}	
	}
	
	public interface ModalShapeVisitor {
		void setMajorLayer(Integer num);
		void setMinorLayer(Integer num);

		void visit(TextShape text);
		void visit(MultiLineTextShape mlt);
		
		void visit(Ellipse ellipse);
		void visit(Arc arc);
		void visit(Rectangle rect);
		void visit(Line line);
	}
	
	// //////////////////////////////////////////////////////////////////////////
	//
	// PUBLIC INNER CLASSES
	//
	// //////////////////////////////////////////////////////////////////////////
	
	public class RenderVisitor implements ModalShapeVisitor {

		public void setMajorLayer(Integer num) {
			// TODO Auto-generated method stub
			
		}

		public void setMinorLayer(Integer num) {
			// TODO Auto-generated method stub
			
		}

		public void visit(TextShape text) {
			// TODO Auto-generated method stub
			
		}

		public void visit(MultiLineTextShape mlt) {
			// TODO Auto-generated method stub
			
		}

		public void visit(Ellipse ellipse) {
			// TODO Auto-generated method stub
			
		}

		public void visit(Arc arc) {
			// TODO Auto-generated method stub
			
		}

		public void visit(Rectangle rect) {
			// TODO Auto-generated method stub
			
		}

		public void visit(Line line) {
			// TODO Auto-generated method stub
			
		}
	}
	
	public class JSONVisitor implements ModalShapeVisitor {
		private int majorLayerIndex_;
		private int minorLayerIndex_;

		private HashMap<Integer, HashMap<Integer, ArrayList<Object>>> majorLayersMap_;
		
		private ArrayList<Integer> pluckRGBA(Color color) {
			ArrayList<Integer> arr = new ArrayList<Integer>();
			arr.add(color.getRed());
			arr.add(color.getGreen());
			arr.add(color.getBlue());
			arr.add(color.getAlpha());
			
			return arr;
		}
		
		public JSONVisitor() {
			majorLayersMap_ = new HashMap<Integer, HashMap<Integer, ArrayList<Object>>>();
		}
		
		public void setMajorLayer(Integer num) {
			if (!majorLayersMap_.containsKey(num)) {
				majorLayersMap_.put(num, new HashMap<Integer, ArrayList<Object>>());
			}
			
			majorLayerIndex_ = num;			
		}

		public void setMinorLayer(Integer num) {
			if (!majorLayersMap_.get(majorLayerIndex_).containsKey(num)) {
				ArrayList<Object> minorLayer = new ArrayList<Object>();
				majorLayersMap_.get(majorLayerIndex_).put(num, minorLayer);
			}
			
			minorLayerIndex_ = num;
		}
		
		private ArrayList<Object> getMinorLayerArray() {
			return majorLayersMap_.get(majorLayerIndex_).get(minorLayerIndex_);
		}
		
		public String getJSONString() {
			return new JSONSerializer().include("*").exclude("*.class").serialize(majorLayersMap_);
		}

		public void visit(TextShape text) {
			Map<String, Object> obj = new HashMap<String, Object>();
			
			obj.put("type", "text");
			obj.put("text", text.text_);
			obj.put("x", text.txtX_);
			obj.put("y", text.txtY_);
			obj.put("color", pluckRGBA(text.color_));
			
			getMinorLayerArray().add(obj);
		}

		public void visit(MultiLineTextShape mlt) {
			Map<String, Object> obj = new HashMap<String, Object>();	
			obj.put("type", "mltext");
			
			getMinorLayerArray().add(obj);
		}
		
		public void visit(Ellipse ellipse) {
			Map<String, Object> obj = new HashMap<String, Object>();
			Ellipse2D shape = ellipse.getShape();			
			
			obj.put("type", "ellipse");
			obj.put("x", shape.getX());
			obj.put("y", shape.getY());
			obj.put("w", shape.getWidth());
			obj.put("h", shape.getHeight());
			obj.put("color", pluckRGBA(ellipse.getColor()));
			
			getMinorLayerArray().add(obj);			
		}

		public void visit(Arc arc) {
			Map<String, Object> obj = new HashMap<String, Object>();
			Arc2D shape = arc.getShape();
			
			obj.put("type", "arc");
			obj.put("x", shape.getX());
			obj.put("y", shape.getY());
			obj.put("h", shape.getHeight());
			obj.put("extent", shape.getAngleExtent());
			obj.put("color", pluckRGBA(arc.getColor()));
			
			getMinorLayerArray().add(obj);
		}

		public void visit(Rectangle rect) {
			Map<String, Object> obj = new HashMap<String, Object>();
			Rectangle2D shape = rect.getShape();
			
			obj.put("type", "rect");
			obj.put("x", shape.getX());
			obj.put("y", shape.getY());
			obj.put("w", shape.getWidth());
			obj.put("h", shape.getHeight());
			obj.put("color", pluckRGBA(rect.getColor()));
			
			getMinorLayerArray().add(obj);
		}
		
		public void visit(Line line) {
			Map<String, Object> obj = new HashMap<String, Object>();			
			Line2D shape = line.getShape();
			
			obj.put("type", "line");
			obj.put("x1", shape.getX1());
			obj.put("y1", shape.getY1());
			obj.put("x2", shape.getX2());
			obj.put("y2", shape.getY2());
			obj.put("color", pluckRGBA(line.getColor()));
			
			getMinorLayerArray().add(obj);
		}		
	}
	
	public interface Renderable {
		public void accept(ModalShapeVisitor visitor);
	}
		
	public interface KnowsShape {
		public java.awt.Shape getShape();
	}
	
	public interface AcceptsVisitor {
		public void visit();
	}
	
	///////////////////////////
	// New stuff starts here //
	///////////////////////////
	public abstract static class TheModalShape implements Renderable {
		public enum Mode { DRAW, FILL }

		protected Mode mode_;
		protected Color color_;
		protected AffineTransform trans_;
		
		public Color getColor() {
			return color_;
		}
	}

	public static abstract class PathShape extends TheModalShape implements KnowsShape {
		protected BasicStroke stroke_;
		
		public abstract java.awt.Shape getShape();
	}

	public static class TextShape extends TheModalShape {
		private Font font_;
		private String text_;
		private float txtX_;
		private float txtY_;

		public TextShape(String text, Font font, Color color, AffineTransform trans, float txtX, float txtY) {
			text_ = text;
			font_ = font;
			color_ = color;
			txtX_ = txtX;
			txtY_ = txtY;
		}

		public void render(Graphics2D g2) {
			g2.setFont(font_);
			AffineTransform saveTrans = g2.getTransform();
			g2.transform(trans_);
			g2.drawString(text_, txtX_, txtY_);
			g2.setTransform(saveTrans);
		}
		
		public String toJSONString() {
			StringBuffer sb = new StringBuffer();
			
			return sb.toString();
		}

		public void accept(ModalShapeVisitor visitor) {
			visitor.visit(this);			
		}
	}
	
	public static class MultiLineTextShape extends TheModalShape {
		private Font font_;
		private String text_;
		private float txtX_;
		private float txtY_;

		public MultiLineTextShape(String text, Font font, Color color, AffineTransform trans, float txtX, float txtY) {
			text_ = text;
			font_ = font;
			color_ = color;
			txtX_ = txtX;
			txtY_ = txtY;
		}
		
		public void render(Graphics2D g2) {
			
		}
		
		public void accept(ModalShapeVisitor visitor) {
			visitor.visit(this);
		}
	}
	
	public static class Arc extends PathShape {
		private Arc2D shape_;
		
		public Arc(double x, double y, double w, double h, double start, double extent, int type, Mode mode, Color color, BasicStroke stroke) {
			this(new Arc2D.Double(x, y, w, h, start, extent, type), mode, color, stroke);
		}

		public Arc(Arc2D arc, Mode mode, Color color, BasicStroke stroke) {
			this(arc, mode, color, stroke, new AffineTransform());
		}

		public Arc(Arc2D arc, Mode mode, Color color, BasicStroke stroke, AffineTransform trans) {
			mode_ = mode;
			color_ = color;
			stroke_ = stroke;
			shape_ = arc;
			trans_ = trans;
		}

		@Override
		public Arc2D getShape() {
			return shape_;
		}

		public void accept(ModalShapeVisitor visitor) {
			visitor.visit(this);
		}		
	}

	public static class Ellipse extends PathShape {
		private Ellipse2D shape_;
		
		public Ellipse(double x, double y, double w, double h, Mode mode, Color color, BasicStroke stroke) {
			this(new Ellipse2D.Double(x, y, w, h), mode, color, stroke);
		}

		public Ellipse(Ellipse2D ellipse, Mode mode, Color color, BasicStroke stroke) {
			this(ellipse, mode, color, stroke, new AffineTransform());
		}

		public Ellipse(Ellipse2D ellipse, Mode mode, Color color, BasicStroke stroke, AffineTransform trans) {
			mode_ = mode;
			color_ = color;
			stroke_ = stroke;
			shape_ = ellipse;
			trans_ = trans;
		}

		@Override
		public Ellipse2D getShape() {
			return shape_;
		}

		public void accept(ModalShapeVisitor visitor) {
			visitor.visit(this);
		}		
	}
	
	public static class Rectangle extends PathShape {
		private Rectangle2D shape_;
		
		public Rectangle(double x, double y, double w, double h, Mode mode,
				Color color, BasicStroke stroke) {
			this(new Rectangle2D.Double(x, y, w, h), mode, color, stroke);
		}

		public Rectangle(Rectangle2D rect, Mode mode, Color color,
				BasicStroke stroke) {
			this(rect, mode, color, stroke, new AffineTransform());
		}

		public Rectangle(Rectangle2D rect, Mode mode, Color color, BasicStroke stroke, AffineTransform trans) {
			mode_ = mode;
			color_ = color;
			stroke_ = stroke;
			shape_ = rect;
			trans_ = trans;
		}

		@Override
		public Rectangle2D getShape() {
			return shape_;
		}

		public void accept(ModalShapeVisitor visitor) {
			visitor.visit(this);
		}
	}
	
	public static class Line extends PathShape {
		private Line2D shape_;
		
		public Line(double x1, double y1, double x2, double y2, Mode mode,
				Color color, BasicStroke stroke) {
			this(new Line2D.Double(x1, y1, x2, y2), mode, color, stroke);
		}

		public Line(Line2D line, Mode mode, Color color,
				BasicStroke stroke) {
			this(line, mode, color, stroke, new AffineTransform());
		}

		public Line(Line2D line, Mode mode, Color color, BasicStroke stroke, AffineTransform trans) {
			mode_ = mode;
			color_ = color;
			stroke_ = stroke;
			shape_ = line;
			trans_ = trans;
		}

		@Override
		public Line2D getShape() {
			return shape_;
		}

		public void accept(ModalShapeVisitor visitor) {
			visitor.visit(this);
		}
	}		
}
