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
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import org.systemsbiology.biotapestry.ui.AnnotatedFont;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.ModalShape;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.MultiLineFragment;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.MultiLineTextShapeForWeb;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.TextShapeForWeb;

public class ModalTextShapeFactoryForWeb implements ModalTextShapeFactory {
		private FontRenderContext frc_;
			
		public ModalTextShapeFactoryForWeb(FontRenderContext frc) {
			frc_ = frc;
		}
		
		public TextShapeForWeb buildTextShape(String text, AnnotatedFont font, Color color, float txtX, float txtY, AffineTransform trans) {
				Rectangle2D bounds = font.getFont().getStringBounds(text, frc_);
				return new TextShapeForWeb(text, font, color, txtX, txtY, bounds.getWidth(), bounds.getHeight(), trans);
		}
		
		public MultiLineTextShapeForWeb buildMultiLineTextShape(Color color, AnnotatedFont font, ArrayList<MultiLineFragment> fragments) {			
				MultiLineTextShapeForWeb mlts = new MultiLineTextShapeForWeb(color, font);
				
				for (MultiLineFragment fragment : fragments) {
						TextLayout textLayout = fragment.tl_;	
						Rectangle2D bounds = textLayout.getBounds();
						
						// TextLayout.getBounds() gives the bounds of rendered characters only.
						// To preserve alignment of text fragments when rendered in the client, the x-coordinate of
						// the first non-whitespace character is exported and leading and trailing whitespace trimmed from the text fragment.
						// Setting the fragment width and exporting it with the whitespaces intact was found to not preserve alignment
						// in the client.
						String newFrag = fragment.frag_.trim();
						double textStartX = fragment.x_ + bounds.getMinX();
						
						mlts.addFragment(newFrag, fragment.tl_, textStartX, fragment.y_, bounds.getWidth(), bounds.getHeight());
				}
				
				return mlts;
		}
}
