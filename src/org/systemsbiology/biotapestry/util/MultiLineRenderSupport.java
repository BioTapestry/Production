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

package org.systemsbiology.biotapestry.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.font.FontRenderContext;
import java.text.AttributedString;
import java.awt.font.TextAttribute;
import java.text.AttributedCharacterIterator; 
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.AnnotatedFont;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.NodeRenderBase;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalShapeContainer;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalShapeFactory;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactory;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.DrawMode;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.ModalShape;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.MultiLineFragment;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.MultiLineTextShape;

/****************************************************************************
**
** This class support multi-line text rendering
*/

public class MultiLineRenderSupport {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  

  //
  // Justification options
  //
  
  public static final int LEFT_JUST   = 0;  
  public static final int CENTER_JUST = 1;
  public static final int RIGHT_JUST  = 2;   
  private static final int NUM_JUST_  = 3;  
  
  public static final int DEFAULT_JUST = CENTER_JUST;  
 
  private static final String LEFT_JUST_STR_ = "left";
  private static final String CENTER_JUST_STR_ = "center";
  private static final String RIGHT_JUST_STR_ = "right"; 
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private static final int OLD_LABEL_BREAK_LIMIT_ = 12;
  private static final int OLD_GENE_MAX_CHAR_     = 12;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor; do not use
  */

  private MultiLineRenderSupport() {
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Get dims of _first line_ of text
  */
  
  public static Dimension firstLineBounds(FontManager fmgr, FontRenderContext frc, String text, boolean hideName, Font mFont, String breakDef) { 
    
    if (hideName || (text.indexOf('^') != -1) || (breakDef == null)) {
      return (getSimpleTextDims(fmgr, frc, text, hideName, mFont, breakDef));
    }
    
    //
    // Multiline case:
    //
    
    List<String> frags = applyLineBreaksForFragments(text, breakDef); 
    String frag = frags.get(0);
    Rectangle2D bounds = mFont.getStringBounds(frag, frc);
    return (new Dimension((int)bounds.getWidth(), (int)bounds.getHeight()));
  }  
  
  /***************************************************************************
  **
  ** Render a piece of text, possibly with superscripts, possibly with
  ** line breaks, but not both.  Return the centerline of the end of the text
  */
  
  public static Point2D renderText(FontManager fmgr, FontRenderContext frc, ModalTextShapeFactory textFactory, ModalShapeContainer group, float x, float y, 
                                   String text, boolean hideName, AnnotatedFont amFont, Color color, String breakDef) {

  	Integer majorLayer = NodeRenderBase.NODE_MAJOR_LAYER;
  	Integer minorLayer = NodeRenderBase.NODE_MINOR_LAYER;
  	
    if (hideName) {
      Rectangle2D bounds = amFont.getFont().getStringBounds(":", frc);
      // 3.0 instead of 2.0 is a hack:
      return (new Point2D.Double(x + bounds.getWidth(), y - (bounds.getHeight() / 3.0)));
    }
    
    //
    // Superscript case
    //

 
    Font sFont = fmgr.getFont(FontManager.SMALL);
    int superIndex = text.indexOf('^');
    if (superIndex != -1) {
      //
      // This should work, but according to bug #4156394, this has been broken for
      // over five years!
      //
      //String display = text.substring(0, superIndex) + text.substring(superIndex + 1);
      //AttributedString asd = new AttributedString(display);
      //asd.addAttribute(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUB, 
      //                 superIndex, display.length() - 1);
      //g2.drawString(asd.getIterator(), x, y);
      String regular = text.substring(0, superIndex);      
      String superscript = text.substring(superIndex + 1);
      Rectangle2D bounds = amFont.getFont().getStringBounds(regular, frc);
      
      // TODO transform
      ModalShape text1 = textFactory.buildTextShape(regular, amFont, color, x, y, new AffineTransform());
      
      // Tag the superscript font as an override, so that it will always be exported in the web application.
      ModalShape text2 = textFactory.buildTextShape(superscript, new AnnotatedFont(sFont, true), color,
      		x + (float)bounds.getWidth(), y - ((float)bounds.getHeight() / 2), new AffineTransform());
      
      group.addShape(text1, majorLayer, minorLayer);
      group.addShape(text2, majorLayer, minorLayer);
      
      Rectangle2D fullBounds = amFont.getFont().getStringBounds(text, frc);
      return (new Point2D.Double(x + fullBounds.getWidth(), y - (fullBounds.getHeight() / 3.0)));
    }
    
    //
    // Simple case:
    //
      
    if (breakDef == null) {
      if (text.length() > 0) {
      	// TODO transform
      	ModalShape text1 = textFactory.buildTextShape(text, amFont, color, x, y, new AffineTransform());
        group.addShape(text1, majorLayer, minorLayer);
      }
      Rectangle2D fullBounds = amFont.getFont().getStringBounds(text, frc);
      return (new Point2D.Double(x + fullBounds.getWidth(), y - (fullBounds.getHeight() / 3.0)));
    }
   
    //
    // Multiline case:
    //
    List<String> frags = applyLineBreaksForFragments(text, breakDef); 
    int numFrags = frags.size();
    Rectangle2D bounds = null;
    float currY = y;
    //MultiLineTextShape mlts = new MultiLineTextShape(color, amFont);
		ArrayList<MultiLineFragment> fragments = new ArrayList<MultiLineFragment>();
					
    for (int i = 0; i < numFrags; i++) {
      String frag = frags.get(i);
      if (bounds == null) {
        bounds = amFont.getFont().getStringBounds(frag, frc);
      }
      // Safety valve: TextLayout hates zero-length frags
      if (frag.length() == 0) {
        frag = " ";
      }
      
      TextLayout tl = new TextLayout(frag, amFont.getFont(), frc);
      // mlts.addFragment(frag, tl, x, currY);
      
      
			fragments.add(new MultiLineFragment(frag, tl, x, currY));

					
      currY += tl.getAscent() + tl.getDescent() + tl.getLeading();
    }
    
		ModalShape mlts = textFactory.buildMultiLineTextShape(color, amFont, fragments);
    
    group.addShape(mlts, majorLayer, minorLayer);
    return (new Point2D.Double(x + bounds.getWidth(), y - (bounds.getHeight() / 3.0)));
  }

  /***************************************************************************
  **
  ** Return the Rectangle used by the text
  */
  
  public static Rectangle2D getTextBounds(FontManager fmgr, FontRenderContext frc, float x, float y, String text, 
                                          boolean hideName, AnnotatedFont amFont, String breakDef) {
 
    if (hideName || (text.indexOf('^') != -1) || (breakDef == null)) {
      return (getSimpleTextBounds(fmgr, frc, x, y, text, hideName, amFont.getFont(), breakDef));
    }
    
    List<String> frags = applyLineBreaksForFragments(text, breakDef);
    String[] toks = frags.toArray(new String[frags.size()]);
    Point2D center = new Point2D.Double();
    Rectangle2D rect2D = multiLineGuts(frc, toks, amFont, center, null, null, null, null);
    Rectangle2D bounds1 = amFont.getFont().getStringBounds(frags.get(0), frc);  // for first line height...
    double mly = (double)y - (bounds1.getHeight() * 0.8);
    return (new Rectangle2D.Double(x, mly, rect2D.getWidth(), rect2D.getHeight()));
  } 

  /***************************************************************************
  **
  ** Return the dimension used by the text (non-multi-line case)
  */
  
  public static Dimension getSimpleTextDims(FontManager fmgr, FontRenderContext frc, String text, boolean hideName, Font mFont, String breakDef) {
 
    if (hideName) {
      Rectangle2D bounds = mFont.getStringBounds(":", frc);
      return (new Dimension((int)bounds.getWidth(), (int)bounds.getHeight()));
    }
    
    //
    // Superscript case.  Not really exact...
    //
 
    Font sFont = fmgr.getFont(FontManager.SMALL);     
    int superIndex = text.indexOf('^');
    if (superIndex != -1) {
      String regular = text.substring(0, superIndex);      
      String superscript = text.substring(superIndex + 1);
      Rectangle2D bounds = mFont.getStringBounds(regular, frc);
      Rectangle2D superBounds = sFont.getStringBounds(superscript, frc);
      double height1 = bounds.getHeight() / 2.0 + superBounds.getHeight();
      double height2 = bounds.getHeight();
      double retHeight = (height1 > height2) ? height1 : height2;    
      return (new Dimension((int)(bounds.getWidth() + superBounds.getWidth()), (int)retHeight));
    }
    
    //
    // Simple case:
    //
      
    if (breakDef == null) {
      Rectangle2D bounds = mFont.getStringBounds(text, frc);
      return (new Dimension((int)bounds.getWidth(), (int)bounds.getHeight()));
    } 
    
    return (null);
  }    
  
  /***************************************************************************
  **
  ** Return the Rectangle used by the text (non-multi-line case)
  */
  
  private static Rectangle2D getSimpleTextBounds(FontManager fmgr, FontRenderContext frc, float x, float y, String text, boolean hideName, Font mFont, String breakDef) {
    Dimension dim = getSimpleTextDims(fmgr, frc, text, hideName, mFont, breakDef);
    return (new Rectangle2D.Double(x, y - (dim.getHeight() * 0.8), dim.getWidth(), dim.getHeight()));
  }  
  
  /***************************************************************************
  **
  ** Quickly answers if the point is a possible text intersection candidate
  */
  
  public static boolean isTextCandidate(FontManager fmgr, FontRenderContext frc, int fontType, FontManager.FontOverride fo, 
                                        String text, double xPad, double dx, double distSq, String breakDef, 
                                        boolean canBeLeft, boolean nameHidden) {
    if ((!canBeLeft) && (dx > 0.0)) {
      return (false);  // Points to left of node cannot intersect
    }
    if (nameHidden) {
      return (false);
    }
    Rectangle2D rect = fmgr.getCharSize(fontType, fo, frc);
    int length;
    int textLength = text.length();
    int superIndex = text.indexOf('^');
    double sizeSq;
    if (superIndex != -1) {
      length = textLength;
      double radius = (rect.getWidth() * length) + xPad;
      sizeSq = radius * radius;
    } else {
      Dimension estRect = textRect(breakDef, text, rect);
      sizeSq = (estRect.getWidth() * estRect.getWidth()) + (estRect.getHeight() * estRect.getHeight()) + (xPad * xPad); 
    }
    return (distSq < sizeSq);
  }
  
  /***************************************************************************
  **
  ** Quickly answers if a node name is just a single line, with no superscripts!
  */  
  
  public static boolean isSingleLineText(String text, boolean hideName, String breakDef) {

    if (hideName) {
      return (true);
    }
    int superIndex = text.indexOf('^');
    if (superIndex != -1) {
      return (false);
    }
    return (breakDef == null);
  }
       
  /***************************************************************************
  **
  ** Answer if the given point intersects the text label
  */
  
  public static boolean intersectTextLabel(FontManager fmgr, FontRenderContext frc, float x, float y, 
                                           String text, Point2D intPt, boolean hideName,
                                           int fontType, FontManager.FontOverride fo, String breakDef) {

    if (hideName) {
      return (false);
    }
    
    //
    // Superscript case
    //
    
    Font mFont = fmgr.getFont(fontType); 
    int superIndex = text.indexOf('^');
    if (superIndex != -1) {
      String regular = text.substring(0, superIndex);      
      String superscript = text.substring(superIndex + 1);
      Rectangle2D bounds = mFont.getStringBounds(regular, frc);
      bounds.setRect(x, y - bounds.getHeight(), bounds.getWidth(), bounds.getHeight());
      if (bounds.contains(intPt)) {
        return (true);
      }
      Rectangle2D supBounds = mFont.getStringBounds(superscript, frc);
      supBounds.setRect(x + (float)bounds.getWidth(), 
                        y - ((float)bounds.getHeight() / 2) - supBounds.getHeight(),
                        supBounds.getWidth(), supBounds.getHeight());
      return (supBounds.contains(intPt));
    }
    
    //
    // Simple case:
    //
      
    if (breakDef == null) {
      Rectangle2D bounds = mFont.getStringBounds(text, frc);
      bounds.setRect(x, y - bounds.getHeight(), bounds.getWidth(), bounds.getHeight());
      return (bounds.contains(intPt));
    }
   
    //
    // Multiline case:
    //
    
    List<String> frags = applyLineBreaksForFragments(text, breakDef); 
    int numFrags = frags.size();
    Rectangle2D bounds = null;
    float currY = y;
    for (int i = 0; i < numFrags; i++) {
      String frag = frags.get(i);
      if (bounds == null) {
        bounds = mFont.getStringBounds(frag, frc);
      }
      // Safety valve: TextLayout hates zero-length frags
      if (frag.length() == 0) {
        frag = " ";
      }
      TextLayout tl = new TextLayout(frag, mFont, frc);
      bounds = tl.getBounds();
      bounds.setRect(x, currY - bounds.getHeight(), bounds.getWidth(), bounds.getHeight());
      if (bounds.contains(intPt)) {
        return (true);
      }
      currY += tl.getAscent() + tl.getDescent() + tl.getLeading();
    }
    return (false);
  }

  /***************************************************************************
   **
   ** Put possible text label bounds into an array
   */

  public static void fillBoundsArrayTextLabel(ArrayList<ModelObjectCache.ModalShape> targetArray, float x, float y,
                                              String text, boolean hideName, int fontType, String breakDef, FontRenderContext frc, FontManager fmgr) {

    if (hideName) {
      return;
    }

    //
    // Superscript case
    //

    Font mFont = fmgr.getFont(fontType);
    int superIndex = text.indexOf('^');
    if (superIndex != -1) {
      String regular = text.substring(0, superIndex);
      String superscript = text.substring(superIndex + 1);
      Rectangle2D bounds = mFont.getStringBounds(regular, frc);
      bounds.setRect(x, y - bounds.getHeight(), bounds.getWidth(), bounds.getHeight());

      ModelObjectCache.Rectangle boundsRect = ModalShapeFactory.buildBoundRectangleForLabel(bounds);
      targetArray.add(boundsRect);

      Rectangle2D supBounds = mFont.getStringBounds(superscript, frc);
      supBounds.setRect(x + (float)bounds.getWidth(),
              y - ((float)bounds.getHeight() / 2) - supBounds.getHeight(),
              supBounds.getWidth(), supBounds.getHeight());

      ModelObjectCache.Rectangle supBoundsRect = ModalShapeFactory.buildBoundRectangleForLabel(supBounds);
      targetArray.add(supBoundsRect);
    }

    //
    // Simple case:
    //

    else if (breakDef == null) {
      Rectangle2D bounds = mFont.getStringBounds(text, frc);
      bounds.setRect(x, y - bounds.getHeight(), bounds.getWidth(), bounds.getHeight());

      ModelObjectCache.Rectangle boundsRect = ModalShapeFactory.buildBoundRectangleForLabel(bounds);
      targetArray.add(boundsRect);

      //return (bounds.contains(intPt));
    }

    //
    // Multiline case:
    //
    
    else {
	    List<String> frags = applyLineBreaksForFragments(text, breakDef);
	    int numFrags = frags.size();
	    Rectangle2D bounds = null;
	    float currY = y;
	    for (int i = 0; i < numFrags; i++) {
	      String frag = frags.get(i);
	      if (bounds == null) {
	        bounds = mFont.getStringBounds(frag, frc);
	      }
	      // Safety valve: TextLayout hates zero-length frags
	      if (frag.length() == 0) {
	        frag = " ";
	      }
	      TextLayout tl = new TextLayout(frag, mFont, frc);
	      bounds = tl.getBounds();
	      bounds.setRect(x, currY - bounds.getHeight(), bounds.getWidth(), bounds.getHeight());
	
	      ModelObjectCache.Rectangle boundsRect = ModalShapeFactory.buildBoundRectangleForLabel(bounds);
	      targetArray.add(boundsRect);
	
	      currY += tl.getAscent() + tl.getDescent() + tl.getLeading();
	    }
    }
  }

  /***************************************************************************
  **
  ** Keep this around as a way of breaking old names into lines to 
  ** generate new break definitions
  */
  
  public static String legacyLineBreaks(boolean hideLabel, int nodeType, FontRenderContext frc, String text, FontManager fmgr) {

    //
    // If hidden, don't bother:
    //
    
    if (hideLabel) {
      return (null);
    }
    
    Font mFont = fmgr.getFont(FontManager.MEDIUM);
    // these types didn't use autobreaks:
    if ((nodeType == Node.BARE) || (nodeType == Node.BOX)) {
      return (null);
    }
    
    int limit = (nodeType != Node.GENE) ?  OLD_LABEL_BREAK_LIMIT_ : OLD_GENE_MAX_CHAR_;
    int superIndex = text.indexOf('^');
    if (superIndex != -1) {
      return (null);  // No legacy breaks on superscripts
    }
 
    //
    // Simple case:
    //
    
    if ((text.length() <= limit) || (text.indexOf(' ') == -1)) {
      return (null);  // No legacy breaks
    }
    
    //
    // Use Java 2D infrastructure to do the breaking / wrapping:
    //
    
    StringBuffer buf = new StringBuffer();
    AttributedString as = new AttributedString(text);
    as.addAttribute(TextAttribute.FONT, mFont);
    AttributedCharacterIterator aci = as.getIterator();
    LineBreakMeasurer lbm = new LineBreakMeasurer(aci, frc);
    Rectangle2D bounds = mFont.getStringBounds(text.substring(0, limit), frc);
    float wrap = (float)bounds.getWidth();
    int lastPos = 0;
    while (lbm.getPosition() < aci.getEndIndex()) {
      if (lastPos != 0) {
        buf.append("\n");
      }
      int nextPos = lbm.nextOffset(wrap);  // no update...
      lbm.nextLayout(wrap); // does update...
      buf.append(text.substring(lastPos, nextPos));
      lastPos = nextPos;
    }
    return (genBreaks(buf.toString()));
  }
  
  /***************************************************************************
  **
  ** Process a name for line breaks
  **
  */
  
  public static String applyLineBreaks(String name, String breakDef) {
    List<Integer> breaks = parseBreaks(breakDef);
    if ((breaks == null) || (breaks.isEmpty())) {
      return (name);
    }
    StringBuffer buf = new StringBuffer();
    int nameLen = name.length();
    int lastBreak = 0;
    int size = breaks.size();
    for (int i = 0; i < size; i++) {
      int nextBreak = breaks.get(i).intValue();
      //
      // Safety valve:
      //
      if (nextBreak >= nameLen) {
        buf.append(name.substring(lastBreak, nameLen));
        return (buf.toString());
      }
      buf.append(name.substring(lastBreak, nextBreak));
      buf.append("\n");
      lastBreak = nextBreak;
    }
    buf.append(name.substring(lastBreak, name.length()));
    return (buf.toString());
  }  
  
  /***************************************************************************
  **
  ** Process a name for line breaks
  **
  */
  
  public static List<String> applyLineBreaksForFragments(String name, String breakDef) {
    ArrayList<String> retval = new ArrayList<String>();
    List<Integer> breaks = parseBreaks(breakDef);
    if ((breaks == null) || (breaks.isEmpty())) {
      retval.add(name);
      return (retval);
    }
    int nameLen = name.length();
    int lastBreak = 0;
    int size = breaks.size();
    for (int i = 0; i < size; i++) {
      int nextBreak = breaks.get(i).intValue();
      //
      // Safety valve:
      //
      if (nextBreak >= nameLen) {
        retval.add(name.substring(lastBreak, nameLen));
        return (retval);
      }
      retval.add(name.substring(lastBreak, nextBreak));
      lastBreak = nextBreak;
    }
    retval.add(name.substring(lastBreak, name.length()));
    return (retval);
  }    
  
  /***************************************************************************
  **
  ** Parse a break def
  */
  
  public static List<Integer> parseBreaks(String breakDef) {
    if (breakDef == null) {
      return (null);
    }
    String[] toks = breakDef.split(":");
    ArrayList<Integer> retval = new ArrayList<Integer>();
    for (int i = 0; i < toks.length; i++) {
      Integer val;
      try {
        val = Integer.valueOf(toks[i]);
      } catch (NumberFormatException nfe) {
        throw new IllegalArgumentException();
      } 
      retval.add(val);
    } 
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Estimate of broken text bounds
  */
  
  public static Dimension textRect(String breakDef, String text, Rectangle2D charRect) {
    List<Integer> breaks = parseBreaks(breakDef);
    int textLen = text.length();
    if (breaks == null) {
      Dimension dim = new Dimension(((int)charRect.getWidth()) * textLen, (int)charRect.getHeight());
      return (dim);
    }
    int numBr = breaks.size();
    int lastBr = 0;
    int maxDiff = Integer.MIN_VALUE; 
    for (int i = 0; i < numBr; i++) {
      Integer brk = breaks.get(i);
      int brkVal = brk.intValue();
      int diff = brkVal - lastBr;
      if (diff > maxDiff) {
        maxDiff = diff;
      }
      lastBr = brkVal;
    }
    
    int lastDiff = (textLen - lastBr - 1);
    if (lastDiff > maxDiff) {
      maxDiff = lastDiff;
    }
    
    Dimension dim = new Dimension(((int)charRect.getWidth()) * maxDiff, (int)charRect.getHeight() * (numBr + 1));
    return (dim);        
  } 
  
  
  /***************************************************************************
  **
  ** Guts of multi-line render cases:
  */
  
  
  public static Rectangle2D multiLineGuts(FontRenderContext frc, String[] toks, AnnotatedFont amFont, Point2D origin,
                                          TextLayout[] tls, float[] ylocs, float[] widths, Point2D startPt) {
    
    //
    // Build up the lines to display first:
    //
    
    if (toks.length == 0) {
      return (null);
    }
    float currY = 0;
    float maxWidth = Float.NEGATIVE_INFINITY;
    float midY = 0.0F;
    boolean isOdd = ((toks.length % 2) == 1);
    int midi = (isOdd) ? (toks.length / 2) - 1 : (toks.length / 2);
    float firstY = 0.0F;
    for (int i = 0; i < toks.length; i++) {
      String frag = toks[i];
      // Safety valve: TextLayout hates zero-length frags!
      if (frag.length() == 0) {
        frag = " ";
      }
      TextLayout tl = new TextLayout(frag, amFont.getFont(), frc);
      //
      // 12/10/09:  I've been getting frequent internal errors on V1.4.2
      // with out of bounds exceptions emerging from TextLayout.  Do
      // a hackish workaround!
      //
      float width;
      float nextY;
      try {  
        width = tl.getAdvance();      
        nextY = tl.getAscent() + tl.getDescent() + tl.getLeading();
      } catch (Exception ex) {
        Rectangle2D bounds = amFont.getFont().getStringBounds(frag, frc);
        width = (float)bounds.getWidth();
        nextY = (float)bounds.getHeight();
      }
      if (width > maxWidth) {
        maxWidth = width;
      }
      if (i == midi) {
        midY = (isOdd) ? currY + (nextY / 2.0F) : currY + (nextY / 3.0F); 
      }
      currY += nextY;
      if (i == 0) {
        firstY = currY;
      }
  
      if (tls != null) tls[i] = tl;
      if (widths != null) widths[i] = width;
      if (ylocs != null) ylocs[i] = currY;
    }
    
    //
    // Figure out rect:
    //
  
    float ox = (float)origin.getX();
    float oy = (float)origin.getY();    
    
    float rx = ox - maxWidth / 2.0F;
    float ul = oy - midY;
    if (startPt != null) startPt.setLocation(rx, ul);
    Rectangle2D rect = new Rectangle2D.Float(rx, ul + (firstY / 3.0F), maxWidth, currY);  // ul: HACKTASTIC! 
 
    return (rect);      
  }  
 
  /***************************************************************************
  **
  ** Render this note for multiple-line cases
  */
  
	public static Rectangle2D multiLineRender(ModalShapeContainer group, FontRenderContext frc, String[] toks, AnnotatedFont amFont, Point2D origin,
			boolean selected, int justification, Color col, ModalTextShapeFactory textFactory) {

  	Integer majorLayer = NodeRenderBase.NODE_MAJOR_LAYER;
  	Integer minorLayer = NodeRenderBase.NODE_MINOR_LAYER;
  	
		//
		// Build up the lines to display first:
		//

		TextLayout[] tls = new TextLayout[toks.length];
		float[] ylocs = new float[toks.length];
		float[] widths = new float[toks.length];
		Point2D startPt = new Point2D.Double();
		Rectangle2D rect = MultiLineRenderSupport.multiLineGuts(frc, toks, amFont,
				origin, tls, ylocs, widths, startPt);

		if (selected) {
			// TODO fix stroke
			// TODO check if getMinX should be used instead of getX
			ModelObjectCache.Rectangle rectShape = new ModelObjectCache.Rectangle(rect.getX(), rect.getY(),
					rect.getWidth(), rect.getHeight(), DrawMode.FILL, Color.orange, new BasicStroke());
			group.addShape(rectShape, majorLayer, minorLayer);
		}

		//
		// Draw it:
		//
		
		//MultiLineTextShape mlts = new MultiLineTextShape(col, amFont);		
		ArrayList<MultiLineFragment> fragments = new ArrayList<MultiLineFragment>();
		
		float ox = (float) origin.getX();
		float maxWidth = (float) rect.getWidth();
		float ul = (float) startPt.getY();
		for (int i = 0; i < toks.length; i++) {
			float drawX;
			switch (justification) {
			case (CENTER_JUST):
				drawX = ox - (widths[i] / 2.0F);
				break;
			case (RIGHT_JUST):
				drawX = ox + (maxWidth / 2.0F) - widths[i];
				break;
			case (LEFT_JUST):
				drawX = ox - (maxWidth / 2.0F);
				break;
			default:
				throw new IllegalArgumentException();
			}
			
			//mlts.addFragment(toks[i], tls[i], drawX, ul + ylocs[i]);
			fragments.add(new MultiLineFragment(toks[i], tls[i], drawX, ul + ylocs[i]));
		}

		ModalShape mlts = textFactory.buildMultiLineTextShape(col, amFont, fragments);
		
    group.addShape(mlts, majorLayer, minorLayer);
    return (rect);
	}   
    
  /***************************************************************************
  **
  ** Build a string-based break def
  */
  
  public static String buildBreaks(List<Integer> breakVals) {
    if (breakVals.isEmpty()) {
      return (null);
    }
    int num = breakVals.size();
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < num; i++) {
      Integer bVal = breakVals.get(i);
      buf.append(bVal);
      if (i != (num - 1)) {
        buf.append(":");
      } 
    }
    return (buf.toString());
  }  
  
  /***************************************************************************
  **
  ** Strip out line breaks
  */
  
  public static String stripBreaks(String brokenName) {
    return (brokenName.replaceAll("\n", ""));
  }
  
  /***************************************************************************
  **
  ** Replace breaks with whitespace
  */
  
  public static String breaksToWhitespace(String brokenName) {
    return (brokenName.replaceAll("\n", " "));
  }  
  
  /***************************************************************************
  **
  ** Generate a break def
  */
  
  public static String genBreaks(String brokenName) {
    StringBuffer buf = new StringBuffer();
    String[] toks = brokenName.split("\n");
    int count = 0;
    for (int i = 0; i < toks.length - 1; i++) {
      count += toks[i].length();
      buf.append(count);
      if (i != (toks.length - 2)) {
        buf.append(":");
      } 
    }
    if (buf.length() == 0) {
      return (null);
    }
    return (buf.toString());
  }
  
  /***************************************************************************
  **
  ** Return possible justification types
  */
  
  public static Vector<ChoiceContent> getJustifyTypes(ResourceManager rMan) {
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    for (int i = 0; i < NUM_JUST_; i++) {
      retval.add(justForCombo(rMan, i));
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Get a combo box element
  */
  
  public static ChoiceContent justForCombo(ResourceManager rMan, int type) {
    return (new ChoiceContent(mapJustToDisplay(rMan, type), type));
  }  

  /***************************************************************************
  **
  ** Map justification types
  */

  public static String mapJustToDisplay(ResourceManager rMan, int just) {
    String justTag = mapToJustTag(just);
    return (rMan.getString("ntProp." + justTag));
  }  
  
  /***************************************************************************
  **
  ** Map justs to just tags
  */

  public static String mapToJustTag(int val) {
    switch (val) {
      case LEFT_JUST:
        return (LEFT_JUST_STR_);
      case CENTER_JUST:
        return (CENTER_JUST_STR_);
      case RIGHT_JUST:
        return (RIGHT_JUST_STR_);        
      default:
        throw new IllegalStateException();
    }
  } 
  
  /***************************************************************************
  **
  ** Map just tags to justs
  */

  public static int mapFromJustTag(String tag) {
    if (tag.equals(LEFT_JUST_STR_)) {
      return (LEFT_JUST);
    } else if (tag.equals(CENTER_JUST_STR_)) {
      return (CENTER_JUST);
    } else if (tag.equals(RIGHT_JUST_STR_)) {
      return (RIGHT_JUST);
    } else {
      throw new IllegalArgumentException();
    }
  }  
}
