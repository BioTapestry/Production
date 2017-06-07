/*
**    Copyright (C) 2003-2017 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.db;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UniqueLabeller;
import org.systemsbiology.biotapestry.ui.DisplayOptions;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NamedColor;

/****************************************************************************
**
** Color generator
*/

public class ColorGenerator {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private UniqueLabeller colorLabels_;
  private Map<String, NamedColor> colors_;

  private String[] geneCol_ = new String[] {  
    "EX-blue",
    "EX-orange",
    "EX-dark-cyan",
    "EX-red",
    "EX-dark-orange",
    "EX-dark-gray-purple",
    "EX-cyan",
    "EX-yellow-orange",
    "EX-pure-blue",
    "EX-dark-yellow-green",
    "EX-dark-magenta",
    "EX-dark-green",
    "EX-blue-magenta",
    "EX-yellow-green",
    "EX-magenta",
    "EX-green",
    "EX-yellow",
    "EX-purple",
    "EX-dark-purple",
    "EX-dark-red",
    "EX-pale-green",
    "EX-pale-blue",
    "EX-dark-tan",
    "EX-pale-blue-magenta",
    "EX-pale-yellow orange",
    "EX-medium-magenta",
    "EX-pale-red",
    "EX-pale-cyan",
    "EX-pale-yellow-green",
    "EX-pale-purple",
    "EX-pale-magenta",
    "EX-pale-red-orange"
  };
  
  //
  // This is an alternative ordering that maximizes RGB separation:
  //
  
  /*
  private String[] geneCol_ = new String[] {    
    "EX-blue",
    "EX-red",
    "EX-green",
    "EX-magenta",
    "EX-cyan",
    "EX-yellow",
    "EX-pure-blue",
    "EX-yellow-green",
    "EX-blue-magenta",
    "EX-orange",
    "EX-dark-gray-purple",
    "EX-yellow-orange",
    "EX-dark-green",
    "EX-pale-magenta",
    "EX-pale-yellow-green",
    "EX-purple",
    "EX-dark-cyan",
    "EX-dark-orange",
    "EX-pale-cyan",
    "EX-dark-magenta",
    "EX-pale-purple",
    "EX-dark-yellow-green",
    "EX-pale-red-orange",
    "EX-pale-blue",
    "EX-dark-red",
    "EX-pale-yellow-orange",
    "EX-dark-purple",
    "EX-pale-green",
    "EX-pale-blue-magenta",
    "EX-pale-red",
    "EX-medium-magenta",
    "EX-dark-tan"
    };
  */ 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CLASS MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private static final float[] inactiveHSVR_;
  private static final float[] activeHSV_;
  private static final float[] activeRegionHSV_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // CLASS INITIALIZATION
  //
  ////////////////////////////////////////////////////////////////////////////
  
  static {
    inactiveHSVR_ = new float[3];
    Color.RGBtoHSB(0xFF, 0xFF, 0xFF, inactiveHSVR_); 
    activeHSV_ = new float[3];
    Color.RGBtoHSB(0x66, 0xEE, 0x66, activeHSV_);
    activeRegionHSV_ = new float[3];
    Color.RGBtoHSB(0x66, 0x66, 0xEE, activeRegionHSV_);   
  } 
 
  /***************************************************************************
  **
  ** Modulate color by activity
  */

  public Color modulateColor(double level, Color inCol, DisplayOptions dopt) {
    float[] colHSB = new float[3];    
    Color.RGBtoHSB(inCol.getRed(), inCol.getGreen(), inCol.getBlue(), colHSB);
    // Hue stays the same:
    
    float[] inactiveHSV = dopt.getInactiveGrayHSV();
    
    colHSB[1] = inactiveHSV[1] + ((float)level * (colHSB[1] - inactiveHSV[1])); 
    colHSB[2] = inactiveHSV[2] + ((float)level * (colHSB[2] - inactiveHSV[2]));         
    return (Color.getHSBColor(colHSB[0], colHSB[1], colHSB[2]));
  } 
  
  /***************************************************************************
  **
  ** Modulate color by activity
  */

  public Color modulateColorSaturation(double level, Color inCol) {
    float[] asHSV = new float[3];
    Color.RGBtoHSB(inCol.getRed(), inCol.getGreen(), inCol.getBlue(), asHSV);
    asHSV[1] = (float)level * asHSV[1]; 
    return (Color.getHSBColor(asHSV[0], asHSV[1], asHSV[2]));
  }

  /***************************************************************************
  **
  ** Build a table cell for expression data
  **
  */
  
  public String variableBlockColor(double level, boolean forRegion) {
    float[] colHSB  = new float[3];
    // Hue stays the same:
    float[] whichActive = (forRegion) ? activeRegionHSV_ : activeHSV_;
    colHSB[0] = whichActive[0];
    colHSB[1] = inactiveHSVR_[1] + ((float)level * (whichActive[1] - inactiveHSVR_[1])); 
    colHSB[2] = inactiveHSVR_[2] + ((float)level * (whichActive[2] - inactiveHSVR_[2]));         
    int varCol = Color.HSBtoRGB(colHSB[0], colHSB[1], colHSB[2]);
    return (Integer.toHexString(varCol));
  }
  
  /***************************************************************************
  **
  ** Inactive Color
  */

  public Color inactiveColor(DisplayOptions dopt) {
    return (dopt.getInactiveGray());
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor
  */

  public ColorGenerator() {
    colorLabels_ = new UniqueLabeller();
    colorLabels_.addExistingLabel("zz_newColor_0");
    // Can't do this: SpEndomes has custom tags e.g. "forest"
    //colorLabels_.setFixedPrefix("zz_newColor_0")
    colors_ = new HashMap<String, NamedColor>();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Need to be able to repair color keys after tab appending. Has to go someplace...
  **
  */    
  
  public static void repairMergedRefs(DataAccessContext dacx, Map<String, String> ackm) {
    Iterator<Layout> lit = dacx.getLayoutSource().getLayoutIterator();
    while (lit.hasNext()) {
      Layout lo = lit.next();
      lo.mapColorKeyForAppend(ackm);
    }
    return;
  }

  /***************************************************************************
  **
  ** Merge with another Color Generator
  */
  
  public List<String> mergeData(DataAccessContext dacx, ColorGenerator otherCG, Map<String, String> keyMap) {

    ArrayList<String> retval = new ArrayList<String>();
    
    //
    // 1) For each new color, if it matches in name and color, just add the key mapping, unless the mapping 
    // is the identity. Don't add the color in.
    //
    // 2) If it matches in name and not in color, make a new name (col-merge1), add it to the set with a new key, 
    // and add the key mapping unless it is the identity.
    //
    // 3) If if matches in color and not in name, add the key mapping to have the merged models use the existing color,
    // unless it is the identity. An alternative would be to add it in as is.
    //
    // 4) If it matches in neither, add it to the existing set with a new key, and add the mapping unless it is the identity.
    //
    
    ResourceManager rMan = dacx.getRMan();
    HashSet<String> existingNames = new HashSet<String>();
    for (NamedColor thisColor : this.colors_.values()) {
      // SHOULD be doing NormKey(), but existing implementation does not, so we are stuck for the moment with that crap...
      // existingNames.add(DataUtil.normKey(thisColor.getDescription()));
      existingNames.add(thisColor.getDescription());
    }    
    
    for (String otherColorID : otherCG.colors_.keySet()) {
      NamedColor onc = otherCG.colors_.get(otherColorID);
      boolean gottaMatch = false;
      for (String thisColorID : this.colors_.keySet()) {
        NamedColor tnc = this.colors_.get(thisColorID);
        boolean colorsMatch = onc.getColor().equals(tnc.getColor());
       
        boolean namesMatch = DataUtil.stringsEqual(tnc.getDescription(), onc.getDescription());
        if (colorsMatch) {
          if (namesMatch) {
            if (!thisColorID.equals(otherColorID)) {
              keyMap.put(otherColorID, thisColorID);
            }
            gottaMatch = true;
            break;
          } else { // Note at the moment we do the same thing as above...
            String format = rMan.getString("tabMerge.ColorGenNameConflictUseExistingFmt"); 
            String msg = MessageFormat.format(format, new Object[] {onc.getDescription(), tnc.getDescription()});
            retval.add(msg);      
            if (!thisColorID.equals(otherColorID)) {
              keyMap.put(otherColorID, thisColorID);
            }
            gottaMatch = true;
            break;
          }
        } else {
          if (namesMatch) {
            String nextLab = getNextColorLabel();
            int i = 1;
            String newName = onc.getDescription() + "-" + i;
            while (existingNames.contains(newName)) {
              newName = onc.getDescription() + "-" + ++i;
            }   
            String format = rMan.getString("tabMerge.ColorGenNameConflictFmt"); 
            String msg = MessageFormat.format(format, new Object[] {onc.getDescription(), newName});
            retval.add(msg);
            NamedColor mergeColor = new NamedColor(nextLab, onc.getColor(), newName);       
            setColor(nextLab, mergeColor);
            if (!nextLab.equals(otherColorID)) {
              keyMap.put(otherColorID, nextLab);
            }
            gottaMatch = true;
            break;
          }
        }
      }
      if (!gottaMatch) {
        String nextLab = getNextColorLabel();
        NamedColor mergeColor = new NamedColor(nextLab, onc.getColor(), onc.getDescription());       
        setColor(nextLab, mergeColor);
        if (!nextLab.equals(otherColorID)) {
          keyMap.put(otherColorID, nextLab);
        }
      }
    }
    return ((retval.isEmpty()) ? null : retval);
  }

  /***************************************************************************
  ** 
  ** Start a new model
  */

  public void newColorModel() {
    buildDefaultColors();
    return;
  }  

  /***************************************************************************
  ** 
  ** Drop everything
  */

  public void dropColors() {
    colorLabels_ = new UniqueLabeller();
    colorLabels_.addExistingLabel("zz_newColor_0");
    // Can't do this: SpEndomes has custom tags e.g. "forest"
    //colorLabels_.setFixedPrefix("zz_newColor_0");    
    colors_.clear();
    buildDefaultColors();    
    return;
  }

  /***************************************************************************
  ** 
  ** Get the color
  */

  public Color getColor(String colorKey) {
    return (colors_.get(colorKey).color);
  }

  /***************************************************************************
  ** 
  ** Get the named color
  */

  public NamedColor getNamedColor(String colorKey) {
    return (colors_.get(colorKey));
  }  

  /***************************************************************************
  **
  ** Update the color set
  */
  
  public GlobalChange updateColors(Map<String, NamedColor> namedColors) {
    GlobalChange retval = new GlobalChange();
    retval.origColors = deepCopyColorMap(colors_);
    colors_ = deepCopyColorMap(namedColors);
    retval.newColors = deepCopyColorMap(namedColors);   
    return (retval);
  }
    
  /***************************************************************************
  **
  ** Dump the database to the given file using XML
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {    
    writeColors(out, ind);
    return;
  }

  /***************************************************************************
  **
  ** Get the next color label
  */
  
  public String getNextColorLabel() {
    return (colorLabels_.getNextLabel());    
  }  
  
  /***************************************************************************
  **
  ** Set the color for the given name
  */
  
  public void setColor(String itemId, NamedColor color) {
    colorLabels_.addExistingLabel(itemId);    
    colors_.put(itemId, color);
    return;
  }
  
  /***************************************************************************
  **
  ** Return an iterator over all the color keys
  */
  
  public Iterator<String> getColorKeys() {
    return (colors_.keySet().iterator());
  }

  /***************************************************************************
  **
  ** Return gene colors as list
  */
  
  public List<String> getGeneColorsAsList() {    
    return (Arrays.asList(geneCol_));
  }
    
  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void changeUndo(GlobalChange undo) {   
    if ((undo.origColors != null) || (undo.newColors != null)) {
      colorChangeUndo(undo);
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void changeRedo(GlobalChange undo) {
    if ((undo.origColors != null) || (undo.newColors != null)) {
      colorChangeRedo(undo);
    }    
    return;
  }

  /***************************************************************************
  **
  ** Return the colors you cannot delete
  */
  
  public Set<String> cannotDeleteColors() {
    HashSet<String> retval = new HashSet<String>();
    retval.add("white");
    //retval.add("red");  // This is handled now by EX-red
    retval.add("black");
    
    retval.add("yellowGreen");
    retval.add("inactiveYellowGreen");  
    retval.add("lightBlue");
    retval.add("inactiveLightBlue");
    retval.add("lightOrange");
    retval.add("inactiveLightOrange");
    retval.add("lightGreen");
    retval.add("inactiveLightGreen"); 
    retval.add("lightPurple");
    retval.add("inactiveLightPurple");
    retval.add("lightGray");
    retval.add("inactiveLightGray");
 
    int size = geneCol_.length;
    for (int i = 0; i < size; i++) {
      retval.add(geneCol_[i]);
    }
    return (retval);
  }  

  /***************************************************************************
  **
  ** Return from a cycle of active colors
  */
  
  public String activeColorCycle(int i) {
    i = i % 5;
    switch (i) {
      case 0:
        return ("lightGreen");
      case 1:
        return ("yellowGreen");
      case 2:
        return ("lightOrange");
      case 3:
        return ("lightPurple");
      case 4:
        return ("lightGray");
    }
    throw new IllegalStateException();
  }
  
  /***************************************************************************
  **
  ** A distinct active color
  */
  
  public String distinctActiveColor() { 
    return ("lightBlue");
  }
  
  /***************************************************************************
  **
  ** Return a distinct inactive color
  */
  
  public String distinctInactiveColor() {  
    return ("inactiveLightBlue");
  }
   
  /***************************************************************************
  **
  ** Return from a cycle of inactive colors
  */
  
  public String inactiveColorCycle(int i) {
    i = i % 5;
    switch (i) {
      case 0:
        return ("inactiveLightGreen");
      case 1:
        return ("inactiveYellowGreen");
      case 2:
        return ("inactiveLightOrange");
      case 3:
        return ("inactiveLightPurple");
      case 4:
        return ("inactiveLightGray");
    }
    throw new IllegalStateException();
  }
   
  /***************************************************************************
  **
  ** Get Ith available color
  */
  
  public String getGeneColor(int i) {
    return (geneCol_[i]);
  }   
  
  /***************************************************************************
  **
  ** Get number of colors
  */
  
  public int getNumColors() {
    return (geneCol_.length);
  }
  
  /***************************************************************************
  **
  ** Build the default color set.
  */
  
  private void buildDefaultColors() {
    colors_ = new HashMap<String, NamedColor>();
    colors_.put("inactiveLightBlue", new NamedColor("inactiveLightBlue", new Color(235, 235, 250), "Very Light Blue"));
    colorLabels_.addExistingLegacyLabel("inactiveLightBlue");   
    colors_.put("white", new NamedColor("white", new Color(255, 255, 255), "White"));    
    colorLabels_.addExistingLegacyLabel("white");   
    colors_.put("inactiveLightPurple", new NamedColor("inactiveLightPurple", new Color(245, 229, 240), "Very Light Purple"));
    colorLabels_.addExistingLegacyLabel("inactiveLightPurple");   
    colors_.put("lightBlue", new NamedColor("lightBlue", new Color(220, 220, 240), "Light Blue"));
    colorLabels_.addExistingLegacyLabel("lightBlue");   
    colors_.put("black", new NamedColor("black", new Color(0, 0, 0), "Black"));
    colorLabels_.addExistingLegacyLabel("black");   
    colors_.put("inactiveLightOrange", new NamedColor("inactiveLightOrange", new Color(255, 230, 200), "Very Light Orange"));
    colorLabels_.addExistingLegacyLabel("inactiveLightOrange");   
    colors_.put("lightGray", new NamedColor("lightGray", new Color(240, 240, 240), "Light Gray"));
    colorLabels_.addExistingLegacyLabel("lightGray");   
    colors_.put("darkGray", new NamedColor("darkGray", new Color(150, 150, 150), "Dark Gray"));   
    colorLabels_.addExistingLegacyLabel("darkGray");    
    colors_.put("inactiveYellowGreen", new NamedColor("inactiveYellowGreen", new Color(255, 255, 220), "Light Yellow Green"));
    colorLabels_.addExistingLegacyLabel("inactiveYellowGreen");   
    colors_.put("yellowGreen", new NamedColor("yellowGreen", new Color(246, 249, 170), "Yellow Green"));
    colorLabels_.addExistingLegacyLabel("yellowGreen");   
    colors_.put("inactiveLightGreen", new NamedColor("inactiveLightGreen", new Color(230, 255, 220), "Very Light Green"));
    colorLabels_.addExistingLegacyLabel("inactiveLightGreen");   
    colors_.put("lightGreen", new NamedColor("lightGreen", new Color(214, 239, 209), "Light Green"));
    colorLabels_.addExistingLegacyLabel("lightGreen");   
    colors_.put("lightOrange", new NamedColor("lightOrange", new Color(244, 211, 170), "Light Orange"));
    colorLabels_.addExistingLegacyLabel("lightOrange");   
    colors_.put("inactiveLightGray", new NamedColor("inactiveLightGray", new Color(245, 245, 245), "Very Light Gray"));
    colorLabels_.addExistingLegacyLabel("inactiveLightGray");   
    colors_.put("lightPurple", new NamedColor("lightPurple", new Color(235, 219, 229), "Light Purple"));
    colorLabels_.addExistingLegacyLabel("lightPurple");
    
    List<NamedColor> geneColors = buildGeneColors();
    int geneColSize = geneColors.size();
    for (int i = 0; i < geneColSize; i++) {
      NamedColor col = geneColors.get(i);
      colors_.put(col.key, col);
      colorLabels_.addExistingLegacyLabel(col.key);
    }
    return;
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** For XML I/O
  */ 

  public static class ColorGeneratorWorker extends AbstractFactoryClient {
 
    private boolean isForAppend_;
    private DataAccessContext dacx_;
    private MyColorGlue mcg_;
    
    public ColorGeneratorWorker(FactoryWhiteboard whiteboard, boolean isForAppend) {
      super(whiteboard);
      myKeys_.add("colors");
      isForAppend_ = isForAppend;
      mcg_ = new MyColorGlue(isForAppend);
      installWorker(new ColorWorker(whiteboard), mcg_);
    }
    
    public void setContext(DataAccessContext dacx) {
      dacx_ = dacx;
      mcg_.setContext(dacx_);
      return;
    }
   
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("colors")) {
        if (isForAppend_) {
          FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
          board.appendCGen = new ColorGenerator();
          retval = board.appendCGen;
        } else {
          retval = dacx_.getMetabase().getColorGenerator();
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
        ColorGenerator existing = dacx_.getMetabase().getColorGenerator();
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        // Map needs to be applied AFTER ALL THE LAYOUTS ARE LOADED
               
        HashMap<String, String> keyMap = new HashMap<String, String>();
        
        List<String> mm = existing.mergeData(dacx_, board.appendCGen, keyMap);
        if ((mm != null) && !mm.isEmpty()) {
          if (board.mergeIssues == null) {
            board.mergeIssues = new ArrayList<String>();
          } 
          board.mergeIssues.addAll(mm);
        }
        if (!keyMap.isEmpty()) {
          if (board.appendColorKeyMap == null) {
            board.appendColorKeyMap = keyMap;
          } else { 
            board.appendColorKeyMap.putAll(keyMap);
          }
        }        
      }
      return;
    }
  }

  public static class ColorWorker extends AbstractFactoryClient {
 
    public ColorWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("color");
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("color")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.nextColor = buildFromXML(elemName, attrs);
        retval = board.nextColor;
      }
      return (retval);     
    }  
    
    private NamedColor buildFromXML(String elemName, Attributes attrs) throws IOException {
      
      String color = AttributeExtractor.extractAttribute(elemName, attrs, "color", "color", true); 
      String name = AttributeExtractor.extractAttribute(elemName, attrs, "color", "name", false); 
      String r = AttributeExtractor.extractAttribute(elemName, attrs, "color", "r", true); 
      String g = AttributeExtractor.extractAttribute(elemName, attrs, "color", "g", true); 
      String b = AttributeExtractor.extractAttribute(elemName, attrs, "color", "b", true); 
  
      color = color.trim();
      if (color.equals("")) {
        throw new IOException();
      }
      
      if (name == null) {
        name = color;
      }
      
      int red = -1;
      int green = -1;
      int blue = -1;
      try {
        red = Integer.parseInt(r);
        green = Integer.parseInt(g);    
        blue = Integer.parseInt(b); 
      } catch (NumberFormatException nfe) {
        throw new IOException();
      }
      if ((red < 0) || (green < 0) || (blue < 0) ||
          (red > 255) || (green > 255) || (blue > 255)) {
        throw new IOException();
      }
      
      return (new NamedColor(color, new Color(red, green, blue), name));    
    }
  }
  
  public static class MyColorGlue implements GlueStick {
     
    private boolean isForAppend_;
    private DataAccessContext dacx_;
    
    public MyColorGlue(boolean isForAppend) {
      isForAppend_ = isForAppend;
    }
    
    public void setContext(DataAccessContext dacx) {
      dacx_ = dacx;
      return;
    }
    
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      NamedColor nCol = board.nextColor;
      try {
        ColorGenerator mgr = (isForAppend_) ? board.appendCGen : dacx_.getMetabase().getColorGenerator();     
        mgr.setColor(nCol.key, nCol);
      } catch (IllegalArgumentException iaex) {
        throw new IOException();
      }
      return (null);
    }
  } 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
    
  /***************************************************************************
  **
  ** Write the colors to XML
  **
  */
  
  private void writeColors(PrintWriter out, Indenter ind) {
    ind.indent();
    out.println("<colors>");
    ind.up();
    Iterator<String> colors = colors_.keySet().iterator();
    while (colors.hasNext()) {
      String key = colors.next();
      NamedColor nc = colors_.get(key);
      Color c = nc.color;
      ind.indent();
      out.print("<color ");
      out.print("color=\"");
      out.print(key);
      out.print("\" name=\"");
      out.print(nc.name);      
      out.print("\" r=\"");
      out.print(c.getRed());
      out.print("\" g=\"");
      out.print(c.getGreen());
      out.print("\" b=\"");
      out.print(c.getBlue());      
      out.println("\" />");
    }
    ind.down().indent();
    out.println("</colors>");
    return;
  }   

  /***************************************************************************
  ** 
  ** Build gene colors
  */

  private List<NamedColor> buildGeneColors() {

    ArrayList<NamedColor> colors = new ArrayList<NamedColor>();
    colors.add(new NamedColor("EX-red", Color.getHSBColor(0.0F, 1.0F, 1.0F), "Bright Red"));
    colors.add(new NamedColor("EX-pale-red-orange", Color.getHSBColor(0.033F, 0.4F, 0.9F), "Dark Salmon"));
    colors.add(new NamedColor("EX-orange", Color.getHSBColor(0.067F, 1.0F, 1.0F), "Pumpkin Orange"));            
    colors.add(new NamedColor("EX-yellow-orange", Color.getHSBColor(0.1F, 1.0F, 1.0F), "Tangerine"));
    colors.add(new NamedColor("EX-pale-yellow orange", Color.getHSBColor(0.12F, 0.5F, 0.8F), "Dark Wheat"));    
    colors.add(new NamedColor("EX-yellow", Color.getHSBColor(0.133F, 1.0F, 1.0F), "Gold"));
    colors.add(new NamedColor("EX-pale-yellow-green", Color.getHSBColor(0.183F, 0.4F, 0.9F), "Pale Goldenrod"));
    colors.add(new NamedColor("EX-yellow-green", Color.getHSBColor(0.233F, 1.0F, 1.0F), "Lime"));
    colors.add(new NamedColor("EX-pale-green", Color.getHSBColor(0.283F, 0.5F, 0.8F), "Pale Green"));                     
    colors.add(new NamedColor("EX-green", Color.getHSBColor(0.333F, 1.0F, 1.0F), "Bright Green"));
    colors.add(new NamedColor("EX-pale-cyan", Color.getHSBColor(0.413F, 0.4F, 0.9F), "Aquamarine"));                     
    colors.add(new NamedColor("EX-cyan", Color.getHSBColor(0.5F, 1.0F, 1.0F), "Cyan"));
    colors.add(new NamedColor("EX-pale-blue", Color.getHSBColor(0.534F, 0.5F, 0.8F), "Powder Blue"));                    
    colors.add(new NamedColor("EX-blue", Color.getHSBColor(0.567F, 1.0F, 1.0F), "Sky Blue"));
    colors.add(new NamedColor("EX-pale-purple", Color.getHSBColor(0.634F, 0.35F, 0.9F), "Cornflower Blue")); 
    colors.add(new NamedColor("EX-pure-blue", Color.getHSBColor(0.667F, 1.0F, 1.0F), "Blue"));
    colors.add(new NamedColor("EX-purple", Color.getHSBColor(0.708F, 0.8F, 1.0F), "Indigo"));
    colors.add(new NamedColor("EX-pale-blue-magenta", Color.getHSBColor(0.738F, 0.5F, 0.8F), "Lilac"));                    
    colors.add(new NamedColor("EX-blue-magenta", Color.getHSBColor(0.767F, 1.0F, 1.0F), "Bright Purple"));
    colors.add(new NamedColor("EX-pale-magenta", Color.getHSBColor(0.80F, 0.4F, 0.9F), "Light Plum"));                   
    colors.add(new NamedColor("EX-magenta", Color.getHSBColor(0.833F, 1.0F, 1.0F), "Fuchsia"));
    colors.add(new NamedColor("EX-pale-red", Color.getHSBColor(0.917F, 0.5F, 0.8F), "Rose"));                    
    colors.add(new NamedColor("EX-dark-red", Color.getHSBColor(0.0F, 0.6F, 0.55F), "Deep Ochre"));
    colors.add(new NamedColor("EX-dark-tan", Color.getHSBColor(0.1F, 0.5F, 0.65F), "Dark Tan"));
    colors.add(new NamedColor("EX-dark-orange", Color.getHSBColor(0.12F, 1.0F, 0.5F), "Sienna"));                   
    colors.add(new NamedColor("EX-dark-yellow-green", Color.getHSBColor(0.183F, 1.0F, 0.5F), "Olive Green"));
    colors.add(new NamedColor("EX-dark-green", Color.getHSBColor(0.283F, 1.0F, 0.5F), "Dark Green"));                    
    colors.add(new NamedColor("EX-dark-cyan", Color.getHSBColor(0.534F, 1.0F, 0.5F), "Dark Steel Blue"));                      
    colors.add(new NamedColor("EX-dark-gray-purple", Color.getHSBColor(0.634F, 1.0F, 0.5F), "Dark Blue"));        
    colors.add(new NamedColor("EX-dark-purple", Color.getHSBColor(0.708F, 0.6F, 0.55F), "Slate Blue"));
    colors.add(new NamedColor("EX-dark-magenta", Color.getHSBColor(0.80F, 1.0F, 0.5F), "Violet"));                    
    colors.add(new NamedColor("EX-medium-magenta", Color.getHSBColor(0.833F, 0.5F, 0.65F), "Mauve"));
    return (colors);
  }       
  
  /***************************************************************************
  **
  ** Write the note properties to XML
  */
  
  private HashMap<String, NamedColor> deepCopyColorMap(Map<String, NamedColor> otherMap) {
    HashMap<String, NamedColor> retval = new HashMap<String, NamedColor>();
    Set<String> keys = otherMap.keySet();
    Iterator<String> kit = keys.iterator();
    while (kit.hasNext()) {
      String key = kit.next();
      NamedColor col = otherMap.get(key);
      retval.put(new String(key), new NamedColor(col));
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Undo a color change
  */
  
  private void colorChangeUndo(GlobalChange undo) {
    if ((undo.origColors != null) && (undo.newColors != null)) {
      colors_ = undo.origColors;
    } else {
      throw new IllegalArgumentException();
    }
    return;
  }
    
  /***************************************************************************
  **
  ** Redo a color change
  */
  
  private void colorChangeRedo(GlobalChange undo) {
    if ((undo.origColors != null) && (undo.newColors != null)) {
      colors_ = undo.newColors;
    } else {
      throw new IllegalArgumentException();
    }   
    return;
  }
}
