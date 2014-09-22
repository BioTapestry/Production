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
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;

import org.systemsbiology.biotapestry.ui.AnnotatedFont;

public abstract class ModelObjectCache {
	public enum DrawMode { DRAW, FILL }

	public enum DrawLayer {
		BACKGROUND_REGIONS,
		UNDERLAY,
		VFN_GHOSTED,
		VFN_UNUSED_REGIONS,
		MODEL_NODEGROUPS,
		FOREGROUND_REGIONS,
		OVERLAY,
		MODELDATA
	}

  public enum SelectionType {
    GLYPH,
    LABEL
  }

  /***************************************************************************
  **
  ** Implement in concrete classes
  */	
	
	public abstract void setDrawLayer(DrawLayer layer);
	
	public abstract void addGroup(CacheGroup group);
	
	////////////////////////////////////////////////////////////////////////////
	//
	// PUBLIC INNER CLASSES
	//
	////////////////////////////////////////////////////////////////////////////	
	public static abstract class ModalShape implements ModalShapeVisitor.Visitable {
		protected DrawMode mode_;
		protected Color color_;
		
		public Color getColor() {
			return color_;
		}
	}
	
	public static abstract class PathShape extends ModalShape {
		protected BasicStroke stroke_;
		
		public BasicStroke getStroke() {
			return stroke_;
		}
	}
	
	public static class PushTransformOperation extends ModalShape {
		private AffineTransform trans_;
		
		public PushTransformOperation(AffineTransform trans) {
			trans_ = trans;
		}
		
		public AffineTransform getTransform() {
			return trans_;
		}
		
		public void accept(ModalShapeVisitor visitor) {
			visitor.visit(this);
		}
	}

	public static class PushRotationWithOrigin extends ModalShape {
		private double originX_;
		private double originY_;
		private double theta_;
		
		public PushRotationWithOrigin(double theta, double x, double y) {
			originX_ = x;
			originY_ = y;
			theta_ = theta;
		}
		
		public double getOriginX() {
			return originX_;
		}

		public double getOriginY() {
			return originY_;
		}
		
		public double getTheta() {
			return theta_;
		}

		public void accept(ModalShapeVisitor visitor) {
			visitor.visit(this);
		}
	}
	
	public static class PopTransformOperation extends ModalShape {
		public PopTransformOperation() { }
		
		public void accept(ModalShapeVisitor visitor) {
			visitor.visit(this);
		}
	}

	public static class SetComposite extends ModalShape {
		AlphaComposite alpha_;
		
		public SetComposite(AlphaComposite alpha) {
			alpha_ = alpha;
		}
		
		public void accept(ModalShapeVisitor visitor) {
			visitor.visit(this);
		}
	}
	
	public static class Arc extends PathShape {
		public enum Type {
			CHORD,
			OPEN,
			PIE
			}
		
		private double centerX_;
		private double centerY_;
		private double radius_;
		private double start_;
		private double extent_;
		private Type type_;
		
		public Arc(double cx, double cy, double r, double start, double extent, Type type, DrawMode mode, Color color, BasicStroke stroke) {
			centerX_ = cx;
			centerY_ = cy;
			radius_ = r;
			start_ = start;
			extent_ = extent;
			type_ = type;
			mode_ = mode;
			color_ = color;
			stroke_ = stroke;
		}

		public double getCenterX() {
			return centerX_;
		}
		
		public double getCenterY() {
			return centerY_;
		}
	
		public double getRadius() {
			return radius_;
		}
		
		public double getStart() {
			return start_;
		}
		
		public double getExtent() {
			return extent_;
		}
		
		public Type getType() {
			return type_;
		}
		
		public void accept(ModalShapeVisitor visitor) {
			visitor.visit(this);
		}		
	}
	
	public static class Ellipse extends PathShape {
		private double cx_;
		private double cy_;
		private double radius_;

		public Ellipse(double x, double y, double radius, DrawMode mode, Color color, BasicStroke stroke) {
			cx_ = x;
			cy_ = y;
			radius_ = radius;
			mode_ = mode;
			color_ = color;
			stroke_ = stroke;			
		}
		
		public double getCenterX() {
			return cx_;
		}
		
		public double getCenterY() {
			return cy_;
		}
	
		public double getRadius() {
			return radius_;
		}
		
		public void accept(ModalShapeVisitor visitor) {
			visitor.visit(this);
		}		
	}
	
	public static class Rectangle extends PathShape {
		private double x_;
		private double y_;
		private double w_;
		private double h_;

    private SelectionType selectionType_;

		public Rectangle(double x, double y, double w, double h, DrawMode mode,
				Color color, BasicStroke stroke) {
			x_ = x;
			y_ = y;
			w_ = w;
			h_ = h;
			mode_ = mode;
			color_ = color;
			stroke_ = stroke;
      selectionType_ = null;
		}

    public Rectangle(double x, double y, double w, double h, DrawMode mode,
                     Color color, BasicStroke stroke, SelectionType stype) {
      x_ = x;
      y_ = y;
      w_ = w;
      h_ = h;
      mode_ = mode;
      color_ = color;
      stroke_ = stroke;
      selectionType_ = stype;
    }

		public double getX() {
			return x_;
		}
		
		public double getY() {
			return y_;
		}
	
		public double getWidth() {
			return w_;
		}
	
		public double getHeight() {
			return h_;
		}

    public SelectionType getSelectionType() {
      return selectionType_;
    }

		public void accept(ModalShapeVisitor visitor) {
			visitor.visit(this);
		}
	}
	
	public static class Line extends PathShape {
		private double x1_;
		private double y1_;
		private double x2_;
		private double y2_;
		
		public Line(double x1, double y1, double x2, double y2, DrawMode mode,
				Color color, BasicStroke stroke) {
			x1_ = x1;
			y1_ = y1;
			x2_ = x2;
			y2_ = y2;
			mode_ = mode;
			color_ = color;
			stroke_ = stroke;
		}
		
		public double getX1() {
				return x1_;
		}
		
		public double getY1() {
			return y1_;
		}
	
		public double getX2() {
			return x2_;
		}
	
		public double getY2() {
			return y2_;
		}
	
		public void accept(ModalShapeVisitor visitor) {
			visitor.visit(this);
		}
	}
	
	public static class TextShape extends ModalShape {
		AnnotatedFont font_;
		String text_;
		float txtX_;
		float txtY_;
		AffineTransform trans_;

		public TextShape(String text, AnnotatedFont font, Color color, float txtX, float txtY, AffineTransform trans) {
			text_ = text;
			font_ = font;
			color_ = color;
			txtX_ = txtX;
			txtY_ = txtY;
			trans_ = trans;
		}
		
		public void accept(ModalShapeVisitor visitor) {
			visitor.visit(this);			
		}
	}
	
	public static class TextShapeForWeb extends ModalShape {
		AnnotatedFont font_;
		String text_;
		float txtX_;
		float txtY_;
		AffineTransform trans_;
		double width_;
		double height_;

		public TextShapeForWeb(String text, AnnotatedFont font, Color color, float txtX, float txtY, double width, double height, AffineTransform trans) {
			text_ = text;
			font_ = font;
			color_ = color;
			txtX_ = txtX;
			txtY_ = txtY;
			width_ = width;
			height_ = height;
			trans_ = trans;
		}
		
		public void accept(ModalShapeVisitor visitor) {
			visitor.visit(this);			
		}
	}	
	
	public static class MultiLineFragment {
			TextLayout tl_;
			String frag_;
			float x_;
			float y_;
			
			public MultiLineFragment(String frag, TextLayout tl, float x, float y) {
				frag_ = frag;
				tl_ = tl;
				x_ = x;
				y_ = y;
			}		
	}
	
	public static class MultiLineTextShape extends ModalShape {
		public class Fragment {
			TextLayout tl_;
			String frag_;
			float x_;
			float y_;
			
			public Fragment(String frag, TextLayout tl, float x, float y) {
				frag_ = frag;
				tl_ = tl;
				x_ = x;
				y_ = y;
			}
		}
		
		ArrayList<Fragment> fragments_;
		AnnotatedFont font_;
		
		public MultiLineTextShape(Color color, AnnotatedFont font) {
			color_ = color;
			font_ = font;

			fragments_ = new ArrayList<Fragment>();
		}		
		
		public void addFragment(String frag, TextLayout tl, float x, float y) {
			fragments_.add(new Fragment(frag, tl, x, y));
		}
		
		public void accept(ModalShapeVisitor visitor) {
			visitor.visit(this);
		}
	}
	
	public static class MultiLineTextShapeForWeb extends ModalShape {
		public class Fragment {
			TextLayout tl_;
			String frag_;
			double x_;
			double y_;
			double width_;
			double height_;
			
			public Fragment(String frag, TextLayout tl, double x, double y, double width, double height) {
				frag_ = frag;
				tl_ = tl;
				x_ = x;
				y_ = y;
				width_ = width;
				height_ = height;
			}
		}
		
		ArrayList<Fragment> fragments_;
		AnnotatedFont font_;
		
		public MultiLineTextShapeForWeb(Color color, AnnotatedFont font) {
			color_ = color;
			font_ = font;

			fragments_ = new ArrayList<Fragment>();
		}		
		
		public void addFragment(String frag, TextLayout tl, double x, double y, double width, double height) {
			fragments_.add(new Fragment(frag, tl, x, y, width, height));
		}
		
		public void accept(ModalShapeVisitor visitor) {
			visitor.visit(this);
		}
	}	
	
	public static class SegmentedPathShape extends PathShape {
		Color color_;
		BasicStroke stroke_;
		GeneralPath path_;
		
		public SegmentedPathShape(DrawMode mode, Color color, BasicStroke stroke, GeneralPath path) {
			mode_ = mode;
			color_ = color;
			stroke_ = stroke;
			path_ = path;
		}
		
		public void accept(ModalShapeVisitor visitor) {
			visitor.visit(this);
		}
	}
}
