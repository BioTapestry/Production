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

package org.systemsbiology.biotapestry.ui;

import java.awt.Font;
import java.text.MessageFormat;
import java.util.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.awt.geom.Rectangle2D;
import java.awt.font.FontRenderContext;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.Indenter;

/****************************************************************************
**
** Font Manager.
*/

public class FontManager {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /**
  *** Variable Fonts
  **/

  public static final int SMALL       = 0;
  public static final int MEDIUM      = 1;
  public static final int MODULE_NAME = 2;
  public static final int GENE_NAME   = 3;  
  public static final int LINK_LABEL  = 4;  
  public static final int MED_LARGE   = 5;
  public static final int NET_MODULE  = 6;
  public static final int LARGE       = 7;
  public static final int NOTES       = 8;  
  public static final int DATE        = 9;
  public static final int TITLE       = 10;
  private static final int NUM_FONTS_ = 11;
    
  /**
  *** Fixed Fonts
  **/

  public static final int TREE                   = 0;
  public static final int STRIP_CHART            = 1;
  public static final int STRIP_CHART_AXIS       = 2; 
  public static final int WORKSHEET_TITLES_LARGE = 3;
  public static final int WORKSHEET_TITLES_MED   = 4;
  public static final int WORKSHEET_TITLES_SMALL = 5;  
  public static final int TOPO_BUBBLES           = 6;    
  private static final int NUM_FIXED_            = 7;  
  
  /**
  *** Point limits
  **/
  
  public static final int MIN_SIZE    = 6;
  public static final int MAX_SIZE    = 40;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private boolean[] init_;
  private Font[] fonts_;
  private Font[] defaults_;
  private Font[] fixed_;
  private HashMap<FontOverride, Font> overrides_;
  private HashMap<FontOverride, Rectangle2D> overBounds_;
  private String[] tags_;
  private Rectangle2D[] charBounds_;
  private FontRenderContext cacheBasis_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor
  */
    
  public FontManager() {
    init_ = new boolean[NUM_FONTS_];
    fonts_ = new Font[NUM_FONTS_];
    defaults_ = new Font[NUM_FONTS_];
    charBounds_ = new Rectangle2D[NUM_FONTS_];
    cacheBasis_ = null;    
    fixed_ = new Font[NUM_FIXED_];
    tags_ = new String[] {"small", "medium", "cisRegName", "geneName", "link", "mediumLarge", "netModule", "large", "notes", "date", "title"};
    overrides_ = new HashMap<FontOverride, Font>();
    overBounds_ = new HashMap<FontOverride, Rectangle2D>();
    
    defaults_[MEDIUM] = new Font("Serif", Font.BOLD, 28);
    defaults_[MODULE_NAME] = new Font("Serif", Font.BOLD, 28);    
    defaults_[GENE_NAME] = new Font("Serif", Font.BOLD, 28);     
    defaults_[LINK_LABEL] = new Font("Serif", Font.BOLD, 28);        
    defaults_[MED_LARGE] = new Font("SansSerif", Font.BOLD, 30);
    defaults_[NET_MODULE] = new Font("SansSerif", Font.BOLD, 34); 
    defaults_[LARGE] = new Font("SansSerif", Font.BOLD, 34); 
    defaults_[NOTES] = new Font("SansSerif", Font.BOLD, 34);     
    defaults_[SMALL] = new Font("Serif", Font.BOLD, 20); 
    defaults_[DATE] = new Font("SansSerif", Font.PLAIN, 25); 
    defaults_[TITLE] = new Font("SansSerif", Font.BOLD, 32);     

    for (int i = 0; i < NUM_FONTS_; i++) {
      fonts_[i] = defaults_[i];     
      init_[i] = false;
    }
    
    fixed_[TREE] = new Font("SansSerif", Font.PLAIN, 10);
    fixed_[STRIP_CHART] = new Font("SanSerif", Font.PLAIN, 12);
    fixed_[STRIP_CHART_AXIS] = new Font("Serif", Font.PLAIN, 12); 
    fixed_[WORKSHEET_TITLES_LARGE] = new Font("SanSerif", Font.BOLD, 20);  
    fixed_[WORKSHEET_TITLES_MED] = new Font("SanSerif", Font.BOLD, 16);  
    fixed_[WORKSHEET_TITLES_SMALL] = new Font("SanSerif", Font.BOLD, 14); 
    fixed_[TOPO_BUBBLES] = new Font("Serif", Font.BOLD, 60);     
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Return merge mismatches
  */

  public List<String> getMergeMismatches(FontManager ofm) {
    ArrayList<String> retval = new ArrayList<String>();
    for (int i = 0; i < NUM_FONTS_; i++) {
      if (!fonts_[i].equals(ofm.fonts_[i])) {
        retval.add(tags_[i]);
      }
    }  
    return (retval);
  }  
  
  /***************************************************************************
  ** 
  ** Get the number of variable fonts
  */

  public int getNumFonts() {
    return (NUM_FONTS_);
  }  

  /***************************************************************************
  ** 
  ** Get the given font default
  */

  public Font getDefaultFont(int fontType) {
    return (defaults_[fontType]);
  }  
  
  /***************************************************************************
  ** 
  ** Get the given font
  */

  public Font getFont(int fontType) {
    return (fonts_[fontType]);
  }

  /***************************************************************************
   **
   ** Get a copy of the default fonts array
   */

  public ArrayList<Font> getDefaultFonts() {
    return new ArrayList<Font>(Arrays.asList(defaults_));
  }

  /***************************************************************************
   **
   ** Get a copy of the fixed fonts array
   */

  public ArrayList<Font> getFixedFonts() {
    return new ArrayList<Font>(Arrays.asList(fixed_));
  }

  /***************************************************************************
  ** 
  ** Get a font, or an override if not null:
  */

  public AnnotatedFont getOverrideFont(int fontType, FontOverride over) {
    if (over == null) {
      //return (fonts_[fontType]);
      return new AnnotatedFont(fonts_[fontType], false);
    }

    //
    // FIX ME!  We can accumulate dead fonts over time unless we do reference
    // counting and deregistering!  Could use a weak map instead?
    //
    Font retval = overrides_.get(over);
    if (retval == null) {
      String type = (over.makeSansSerif) ? "SansSerif" : "Serif";
      int style = calcStyle(over.makeBold, over.makeItalic);
      retval = new Font(type, style, over.size);
      overrides_.put(over.clone(), retval);
    }
    //return (retval);
    return new AnnotatedFont(retval, true);
  }

  // TODO remove old implementation
  public Font getOverrideFontOld(int fontType, FontOverride over) {
    if (over == null) {
      return (fonts_[fontType]);
    }
    //
    // FIX ME!  We can accumulate dead fonts over time unless we do reference
    // counting and deregistering!  Could use a weak map instead?
    //
    Font retval = overrides_.get(over);
    if (retval == null) {
      String type = (over.makeSansSerif) ? "SansSerif" : "Serif";
      int style = calcStyle(over.makeBold, over.makeItalic); 
      retval = new Font(type, style, over.size);
      overrides_.put(over.clone(), retval);
    }
    return (retval);
  }  
   
  /***************************************************************************
  ** 
  ** Get the max bounds for the given font
  */

  public Rectangle2D getCharSize(int fontType, FontOverride fo, FontRenderContext frc) {
    if ((cacheBasis_ == null) || !cacheBasis_.equals(frc)) {
      flushSizeCache();
      cacheBasis_ = frc;
    }
    
    Rectangle2D retval;
    if (fo == null) {   
      if (charBounds_[fontType] == null) {
        charBounds_[fontType] = fonts_[fontType].getMaxCharBounds(frc);
      }     
      retval = charBounds_[fontType];    
    } else {
      retval = overBounds_.get(fo);
      if (retval == null) {
        AnnotatedFont overFont = getOverrideFont(fontType, fo);
        retval = overFont.getFont().getMaxCharBounds(frc);
        overBounds_.put(fo.clone(), retval);        
      }        
    }
    return (retval);    
  }  
  
  /***************************************************************************
  ** 
  ** Get the given font tag
  */

  public String getFontTag(int fontType) {
    return (tags_[fontType]);
  }
  
  /***************************************************************************
  ** 
  ** Get the given fixedfont
  */

  public Font getFixedFont(int fontType) {
    return (fixed_[fontType]);
  }  
  
  /***************************************************************************
  ** 
  ** Reset to defaults
  */
  
  public FontChange resetToDefaults() {
    FontChange retval = new FontChange();
    retval.oldFonts = new Font[NUM_FONTS_];
    System.arraycopy(fonts_, 0, retval.oldFonts, 0, NUM_FONTS_);     
    for (int i = 0; i < NUM_FONTS_; i++) {
      fonts_[i] = defaults_[i];
    }
    retval.newFonts = new Font[NUM_FONTS_];
    System.arraycopy(fonts_, 0, retval.newFonts, 0, NUM_FONTS_);
    overrides_.clear();
    flushSizeCache();
    return (retval);
  }

  /***************************************************************************
  ** 
  ** Update the font parameters
  */

  private void setFont(int fontType, int size, boolean makeBold, boolean makeItalic, boolean makeSansSerif) {
    String type = (makeSansSerif) ? "SansSerif" : "Serif";
    int style = calcStyle(makeBold, makeItalic); 
    fonts_[fontType] = new Font(type, style, size);
    flushSizeCache();
    return;
  } 
  
  /***************************************************************************
  ** 
  ** Install the font for IO
  */

  void installFont(IndexedFont iFont) {
    fonts_[iFont.index] = iFont.font;
    init_[iFont.index] = true;
    return;
  } 

  /***************************************************************************
  ** 
  ** Set all the fonts
  */

  public FontChange setFonts(List<FontSpec> fontSpecs) {
    FontChange retval = new FontChange();
    retval.oldFonts = new Font[NUM_FONTS_];
    System.arraycopy(fonts_, 0, retval.oldFonts, 0, NUM_FONTS_);
    int specSize = fontSpecs.size();
    for (int i = 0; i < specSize; i++) {
      FontSpec fs = fontSpecs.get(i);
      setFont(fs.fontType, fs.size, fs.makeBold, fs.makeItalic, fs.makeSansSerif);
    }
    retval.newFonts = new Font[NUM_FONTS_];
    System.arraycopy(fonts_, 0, retval.newFonts, 0, NUM_FONTS_);
    flushSizeCache();
    return (retval);
  }
 
  /***************************************************************************
  ** 
  ** Get the point size of the given font
  */

  public int getPointSize(int fontType) {
    Font currFont = fonts_[fontType];
    return (currFont.getSize());
  }    

  /***************************************************************************
  ** 
  ** Answer if the font is BOLD
  */

  public boolean isFontBold(int fontType) {
    Font currFont = fonts_[fontType];
    return (currFont.isBold());
  }  
  
  /***************************************************************************
  ** 
  ** Answer if the font is italic
  */

  public boolean isFontItalic(int fontType) {
    Font currFont = fonts_[fontType];
    return (currFont.isItalic());
  }  
  
  /***************************************************************************
  ** 
  ** Answer if the font is SansSerif
  */

  public boolean isFontSansSerif(int fontType) {
    Font currFont = fonts_[fontType];
    return (isSansSerif(currFont));
  }
  
  /***************************************************************************
  **
  ** Write the fonts to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();
    out.println("<fonts>");
    ind.up();
    for (int i = 0; i < fonts_.length; i++) {
      ind.indent();
      out.print("<font ");
      out.print("name=\""); 
      out.print(getFontTag(i));
      out.print("\" type=\"");
      out.print(isFontSansSerif(i) ? "sanSerif" : "serif");      
      out.print("\" isBold=\"");
      out.print(isFontBold(i));
      out.print("\" isItalic=\"");
      out.print(isFontItalic(i));      
      out.print("\" size=\"");
      out.print(getPointSize(i));      
      out.println("\" />");
    }
    ind.down().indent();
    out.println("</fonts>");
    return;
  }

  /***************************************************************************
  **
  ** Undo a font change
  */
  
  public void changeUndo(FontChange undo) {
    fonts_ = undo.oldFonts;
    flushSizeCache();
    return;
  }
    
  /***************************************************************************
  **
  ** Redo a font change
  */
  
  public void changeRedo(FontChange undo) {
    fonts_ = undo.newFonts;
    flushSizeCache();
    return;
  } 
  
 /***************************************************************************
  **
  ** Fixup legacy IO
  **
  */
  
  public void fixupLegacyIO() {
    if (!init_[MODULE_NAME]) {
      Font oldFont = fonts_[MEDIUM];
      fonts_[MODULE_NAME] = new Font(oldFont.getName(), oldFont.getStyle(), oldFont.getSize());
      init_[MODULE_NAME] = true;
    }
    if (!init_[GENE_NAME]) {
      Font oldFont = fonts_[MEDIUM];
      fonts_[GENE_NAME] = new Font(oldFont.getName(), oldFont.getStyle(), oldFont.getSize());
      init_[GENE_NAME] = true;
    }
    if (!init_[LINK_LABEL]) {
      Font oldFont = fonts_[MEDIUM];
      fonts_[LINK_LABEL] = new Font(oldFont.getName(), oldFont.getStyle(), oldFont.getSize());
      init_[LINK_LABEL] = true;
    }
    if (!init_[NET_MODULE]) {
      Font oldFont = fonts_[LARGE];
      fonts_[NET_MODULE] = new Font(oldFont.getName(), oldFont.getStyle(), oldFont.getSize());
      init_[NET_MODULE] = true;
    }
    if (!init_[NOTES]) {
      Font oldFont = fonts_[LARGE];
      fonts_[NOTES] = new Font(oldFont.getName(), oldFont.getStyle(), oldFont.getSize());
      init_[NOTES] = true;
    }
    return;
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Calc the combined style
  */

  public static int calcStyle(boolean doBold, boolean doItalic) {  
    int style;
    if (doBold) {
      if (doItalic) {
        style = Font.BOLD + Font.ITALIC;
      } else {
        style = Font.BOLD;
      }      
    } else {
      if (doItalic) {
        style = Font.ITALIC;
      } else {
        style = Font.PLAIN;
      }           
    }
    return (style);
  }
 
  /***************************************************************************
  ** 
  ** Answer if the font is SansSerif
  */

  public static boolean isSansSerif(Font currFont) {
    String name = currFont.getName();
    return (name.equals("SansSerif"));
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Used to transmit font info
  */
  
  public static class FontSpec {
    public int fontType;
    public int size;
    public boolean makeBold;
    public boolean makeItalic;    
    public boolean makeSansSerif; 

    public FontSpec(int fontType, int size, boolean makeBold, boolean makeItalic, boolean makeSansSerif) {
      this.fontType = fontType;
      this.size = size;
      this.makeBold = makeBold;
      this.makeItalic = makeItalic;
      this.makeSansSerif = makeSansSerif;       
    }
  }  
  
  /***************************************************************************
  **
  ** Used to override fonts
  */
  
  public static class FontOverride implements Cloneable {
    public int size;
    public boolean makeBold;
    public boolean makeItalic;    
    public boolean makeSansSerif; 
    
    public FontOverride() {
    	this(-1,false,false,false);
    }

    public FontOverride(int size, boolean makeBold, boolean makeItalic, boolean makeSansSerif) {
      this.size = size;
      this.makeBold = makeBold;
      this.makeItalic = makeItalic;
      this.makeSansSerif = makeSansSerif;       
    }
   
    @Override
    public FontOverride clone() { 
      try {
        FontOverride retval = (FontOverride)super.clone();
        return (retval);
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
    }
    
    @Override    
    public String toString() {
      return ("FontOverride: size=" + size + " bold=" + makeBold + " italic=" + makeItalic  + " sans=" + makeSansSerif);
    }        

    @Override
    public int hashCode() {
      return (size + ((makeBold) ? 200 : 100) + ((makeItalic) ? 2000 : 1000) + ((makeSansSerif) ? 20000 : 10000));
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return (true);
      }
      if (other == null) {
        return (false);
      }
      if (!(other instanceof FontOverride)) {
        return (false);
      }
      FontOverride otherFO = (FontOverride)other;
      return ((this.size == otherFO.size) && 
              (this.makeBold == otherFO.makeBold) && 
              (this.makeItalic == otherFO.makeItalic) && 
              (this.makeSansSerif == otherFO.makeSansSerif));
    }
  }  
 
  /***************************************************************************
  **
  ** For XML I/O
  */ 

  public static class FontManagerWorker extends AbstractFactoryClient {
 
    private boolean isForAppend_;
    private MyFontGlue mfg_;
    private DataAccessContext dacx_;
    
    public FontManagerWorker(FactoryWhiteboard whiteboard, boolean isForAppend) {
      super(whiteboard);
      myKeys_.add("fonts");
      isForAppend_ = isForAppend;
      mfg_ = new MyFontGlue(isForAppend);
      installWorker(new FontWorker(whiteboard), mfg_);
    }
    
   public void setContext(DataAccessContext dacx) {
      dacx_ = dacx;
      mfg_.installContext(dacx_);
      return;
    }

    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("fonts")) {
        if (isForAppend_) {
          FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
          board.appendMgr = new FontManager();
          retval = board.appendMgr;
        } else {
          retval = dacx_.getFontManager();
        }
      }
      return (retval);     
    }
    
   /***************************************************************************
    **
    ** Callback for completion of the element
    **
    */
    
    @Override
    public void localFinishElement(String elemName) throws IOException {
      if (isForAppend_) {
        FontManager existing = dacx_.getFontManager();
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        List<String> mm = existing.getMergeMismatches(board.appendMgr);
        if (!mm.isEmpty()) {
          if (board.mergeIssues == null) {
            board.mergeIssues = new ArrayList<String>();
          }  
          String fmt = dacx_.getRMan().getString("tabMerge.FontConflictFmt");
          for (String failMerge : mm) {
            String userName = dacx_.getRMan().getString("fontDialog.fontLabel_" + failMerge);
            String desc = MessageFormat.format(fmt, new Object[] {userName});
            board.mergeIssues.add(desc);
          }
        }
      }
      return;
    }
  }
 
  public static class IndexedFont {
    public int index;
    public Font font;
    
    public IndexedFont(int index, Font font) {
      this.index = index;
      this.font = font;
    }
  }

  public static class FontWorker extends AbstractFactoryClient {
 
    public FontWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("font");
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("font")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.ifont = buildFromXML(elemName, attrs);
        retval = board.ifont;
      }
      return (retval);     
    }  
    
    private IndexedFont buildFromXML(String elemName, Attributes attrs) throws IOException {
      String name = AttributeExtractor.extractAttribute(elemName, attrs, "font", "name", true); 
      String type = AttributeExtractor.extractAttribute(elemName, attrs, "font", "type", true); 
      String isBold = AttributeExtractor.extractAttribute(elemName, attrs, "font", "isBold", true); 
      String isItalic = AttributeExtractor.extractAttribute(elemName, attrs, "font", "isItalic", false); 
      String size = AttributeExtractor.extractAttribute(elemName, attrs, "font", "size", true); 
      
      int index = -1;
      name = name.trim();
      if (name.equalsIgnoreCase("small")) {
        index = SMALL;
      } else if (name.equalsIgnoreCase("medium")) {
        index = MEDIUM;
      } else if (name.equalsIgnoreCase("cisRegName")) {
        index = MODULE_NAME;        
      } else if (name.equalsIgnoreCase("geneName")) {
        index = GENE_NAME;      
      } else if (name.equalsIgnoreCase("link")) {
        index = LINK_LABEL;            
      } else if (name.equalsIgnoreCase("netModule")) {
        index = NET_MODULE;     
      } else if (name.equalsIgnoreCase("mediumLarge")) {
        index = MED_LARGE;           
      } else if (name.equalsIgnoreCase("large")) {
        index = LARGE;
      } else if (name.equalsIgnoreCase("notes")) {
        index = NOTES;      
      } else if (name.equalsIgnoreCase("date")) {
        index = DATE;
      } else if (name.equalsIgnoreCase("title")) {
        index = TITLE;
      } else { 
        throw new IOException();
      }
  
      type = type.trim();
      if (type.equalsIgnoreCase("serif")) {
        type = "Serif";
      } else if (type.equalsIgnoreCase("sanSerif")) {
        type = "SansSerif";
      } else { 
        throw new IOException();
      }
      
      boolean doItalic = (isItalic == null) ? false : Boolean.valueOf(isItalic).booleanValue();
      boolean doBold = Boolean.valueOf(isBold).booleanValue();
      
      int style = calcStyle(doBold, doItalic);  
      
      int pts;
      try {
        pts = Integer.parseInt(size); 
      } catch (NumberFormatException nfe) {
        throw new IOException();
      }
      
      if ((pts < MIN_SIZE) || (pts > MAX_SIZE)) {
        throw new IOException();
      }     
      return (new IndexedFont(index, new Font(type, style, pts)));    
    }
  }
  
  public static class MyFontGlue implements GlueStick {
     
    private boolean isForAppend_;
    private DataAccessContext dacx_;
     
    
    public MyFontGlue(boolean isForAppend) {
      isForAppend_ = isForAppend;
    }
   
    public void installContext(DataAccessContext dacx) {
      dacx_ = dacx;
      return;
    }

    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      IndexedFont ifont = board.ifont;
      try {
        FontManager mgr = (isForAppend_) ? board.appendMgr : dacx_.getFontManager();
        mgr.installFont(ifont);
      } catch (IllegalArgumentException iaex) {
        throw new IOException();
      }
      return (null);
    }
  } 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Flush the size cache
  **
  */
  
  private void flushSizeCache() {
    for (int i = 0; i < NUM_FONTS_; i++) {
      charBounds_[i] = null;
    }
    overBounds_.clear();
    cacheBasis_ = null;
    return;
  }
}
