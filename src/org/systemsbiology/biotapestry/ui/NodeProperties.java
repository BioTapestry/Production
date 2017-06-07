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

package org.systemsbiology.biotapestry.ui;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.HashMap;
import java.util.Vector;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.db.ColorResolver;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.DBNode;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.ui.freerender.BareNodeFree;
import org.systemsbiology.biotapestry.ui.freerender.BoxNodeFree;
import org.systemsbiology.biotapestry.ui.freerender.BubbleNodeFree;
import org.systemsbiology.biotapestry.ui.freerender.GeneFree;
import org.systemsbiology.biotapestry.ui.freerender.IntercellFree;
import org.systemsbiology.biotapestry.ui.freerender.SlashFree;
import org.systemsbiology.biotapestry.ui.freerender.DiamondNodeFree;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.LineBreaker;

/****************************************************************************
**
** Contains the information needed to layout a node
*/

public class NodeProperties implements Cloneable {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private int orient_;
  private Point2D location_;
  private String colorTag_;
  private String color2Tag_;  
  private String nodeTag_;
  private String orientTag_;
  private boolean hideName_;
  private int extraPadGrowth_;
  private FontManager.FontOverride localFont_;
  private INodeRenderer myRenderer_;
  private String lineBreakDef_;
  private ColorResolver cRes_;
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////

  /**
  *** Possible orientations:
  **/
  
  public static final int UNDEFINED = -1;
  public static final int NONE  = 0;
  public static final int LEFT  = 1;
  public static final int RIGHT = 2;
  public static final int UP    = 3;
  public static final int DOWN  = 4;
 
  private static final String NONE_TAG_  = "";
  private static final String LEFT_TAG_  = "left";  
  private static final String RIGHT_TAG_ = "right";     
  private static final String UP_TAG_    = "up";     
  private static final String DOWN_TAG_  = "down";
  
  /**
  *** Possible extraPadGrowth:
  **/
  public static final int UNDEFINED_GROWTH  = -1;
  public static final int NO_GROWTH          = 0;
  public static final int HORIZONTAL_GROWTH  = 1;
  public static final int VERTICAL_GROWTH    = 2;
 
  private static final String NO_GROWTH_TAG_          = "none";
  private static final String HORIZONTAL_GROWTH_TAG_  = "horz";  
  private static final String VERTICAL_GROWTH_TAG_    = "vert";     

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Copy Constructor
  */

  public NodeProperties(NodeProperties other) {
    this.cRes_ = other.cRes_;
    this.orient_ = other.orient_;
    this.location_ = (Point2D)other.location_.clone();
    this.colorTag_ = other.colorTag_;
    this.color2Tag_ = other.color2Tag_;
    this.nodeTag_ = other.nodeTag_;
    this.orientTag_ = other.orientTag_;    
    this.myRenderer_ = other.myRenderer_;
    this.hideName_ = other.hideName_;
    this.extraPadGrowth_ = other.extraPadGrowth_;
    this.localFont_ = (other.localFont_ == null) ? null : (FontManager.FontOverride)other.localFont_.clone();
    this.lineBreakDef_ = other.lineBreakDef_;
  }
  
  /***************************************************************************
  **
  ** Used to build a prop for another node id
  */

  public NodeProperties(String newNode, NodeProperties other) {
    this.cRes_ = other.cRes_;
    this.orient_ = other.orient_;
    this.location_ = (Point2D)other.location_.clone();
    this.colorTag_ = other.colorTag_;
    this.color2Tag_ = other.color2Tag_;
    this.nodeTag_ = newNode;
    this.orientTag_ = other.orientTag_;    
    this.myRenderer_ = other.myRenderer_;
    this.hideName_ = other.hideName_;
    this.extraPadGrowth_ = other.extraPadGrowth_;
    this.localFont_ = (other.localFont_ == null) ? null : (FontManager.FontOverride)other.localFont_.clone();
    this.lineBreakDef_ = other.lineBreakDef_;
  }  

  /***************************************************************************
  **
  ** Change the type
  */

  public NodeProperties(NodeProperties other, int oldType, int newType) {
    //
    // FIX ME!!  This kinda sucks.  Some fields are only appropriate
    // for certain types of nodes.  Changing the type means we must
    // trash fields not appropriate for type.  At the moment, that
    // is only imposed by the UI; it should be defined here.
    //
    
    this.cRes_ = other.cRes_;
    this.location_ = (Point2D)other.location_.clone();
    this.colorTag_ = other.colorTag_;
    this.nodeTag_ = other.nodeTag_;
    this.hideName_ = other.hideName_;
    this.myRenderer_ = buildRenderer(newType);
  
    //
    // If new guy has fewer valid orientations, map them:
    // 
            
    this.orient_ = orientationMap(oldType, newType, other.orient_);
    this.orientTag_ = mapOrientTypes(this.orient_);
    this.extraPadGrowth_ = extraGrowthMap(oldType, newType, other.extraPadGrowth_);    
    
    if (newType == Node.GENE) {
      this.hideName_ = false;
    }

    //
    // Only intercell supports two colors
    //
        
    this.color2Tag_ = (newType == Node.INTERCELL) ? other.color2Tag_ : null;
    
    this.localFont_ = (other.localFont_ == null) ? null : (FontManager.FontOverride)other.localFont_.clone();
    this.lineBreakDef_ = other.lineBreakDef_;
  }  

  /***************************************************************************
  **
  ** Used to propagate a new node property to other layouts.
  */

  public NodeProperties(NodeProperties other, int nodeType) {
    this(other);
    this.myRenderer_ = buildRenderer(nodeType);
  }  
 
  /***************************************************************************
  **
  ** Basic Constructor for UI creation
  */

  public NodeProperties(ColorResolver cRes, int nodeType, String ref, 
                        double xPos, double yPos, boolean hideName) {
    cRes_ = cRes;
    colorTag_ = defaultColor(nodeType);                          
    color2Tag_ = null;      
    myRenderer_ = buildRenderer(nodeType);
    orient_ = RIGHT;    
    orientTag_ = RIGHT_TAG_;
    location_ = new Point2D.Float((float)xPos, (float)yPos);
    nodeTag_ = ref;
    hideName_ = hideName;
    extraPadGrowth_ = (usesGrowth(nodeType)) ? HORIZONTAL_GROWTH : NO_GROWTH;
    localFont_ = null;
    lineBreakDef_ = null;
  }
  
  /***************************************************************************
  **
  ** Constructor
  */

  public NodeProperties(ColorResolver cRes, String ref, String color, String color2, 
                        String orient, double xPos, double yPos, 
                        boolean hideName, String growth, String lineBreaks, int nodeType) throws IOException {
    
    cRes_ = cRes;
    myRenderer_ = buildRenderer(nodeType);
    if ((orient == null) || orient.trim().equals("")) {
      orient_ = RIGHT;  // Fix up legacy cases
      orientTag_ = RIGHT_TAG_;
    } else {
      orient = orient.trim();
      try {
        orient_ = mapOrientTypeTag(orient);
      } catch (IllegalArgumentException ex) {
        throw new IOException();
      }
      orientTag_ = orient;
    }
    
    if (growth == null) {
      extraPadGrowth_ = NO_GROWTH;
    } else {
      growth = growth.trim();
      try {
        extraPadGrowth_ = mapGrowthTypeTag(growth);
      } catch (IllegalArgumentException ex) {
        throw new IOException();
      }
    }

    location_ = new Point2D.Float((float)xPos, (float)yPos);
    
    colorTag_ = color;
    color2Tag_ = color2;  
    nodeTag_ = ref;
    hideName_ = hideName;
    localFont_ = null;
    lineBreakDef_ = lineBreaks;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Remap the color tags
  */
  
  public void mapColorTags(Map<String, String> ctm) {
    String nk = ctm.get(colorTag_);
    if (nk != null) {
      colorTag_ = nk;
    }
    if (color2Tag_ != null) {
      nk = ctm.get(color2Tag_);
      if (nk != null) {
        color2Tag_ = nk;
      }
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public NodeProperties clone() {
    try {
      NodeProperties retval = (NodeProperties)super.clone();
      retval.location_ = (Point2D)this.location_.clone();
      retval.localFont_ = (this.localFont_ == null) ? null : (FontManager.FontOverride)this.localFont_.clone();
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }  
   
  /***************************************************************************
  **
  ** compression support
  */
  
  public void compressRow(double rowDelta) {
    double newX = location_.getX();
    double newY = location_.getY() - rowDelta;
    location_.setLocation(newX, newY);
    return;
  }
  
  /***************************************************************************
  **
  ** compression support
  */
  
  public void compressColumn(double colDelta) {
    double newX = location_.getX() - colDelta;
    double newY = location_.getY();
    location_.setLocation(newX, newY);
    return;
  }
  
  /***************************************************************************
  **
  ** Expansion support
  */
  
  public void expandRow(double rowDelta) {
    double newX = location_.getX();
    double newY = location_.getY() + rowDelta;
    location_.setLocation(newX, newY);
    return;
  }
  
  /***************************************************************************
  **
  ** Expansion support
  */
  
  public void expandColumn(double colDelta) {
    double newX = location_.getX() + colDelta;
    double newY = location_.getY();
    location_.setLocation(newX, newY);
    return;
  }  
  
  /***************************************************************************
  **
  ** Set the color
  */
  
  public void setColor(String colorTag) {
    colorTag_ = colorTag;
    return;
  }  

  /***************************************************************************
  **
  ** Set the second color
  */
  
  public void setSecondColor(String colorTag) {
    color2Tag_ = colorTag;
    return;
  }   
   
  /***************************************************************************
  **
  ** Get the color
  */
  
  public Color getColor() {
    return (cRes_.getColor(colorTag_));
  }
  
  /***************************************************************************
  **
  ** Get the color name
  */
  
  public String getColorName() {
    return (colorTag_);
  }
  
  /***************************************************************************
  **
  ** Get the reference
  */
  
  public String getReference() {
    return (nodeTag_);
  }  
  
  /***************************************************************************
  **
  ** Get the second color (may be null)
  */
  
  public Color getSecondColor() {
    if (color2Tag_ == null) {
      return (null);
    }
    return (cRes_.getColor(color2Tag_));
  }
  
  /***************************************************************************
  **
  ** Get the second color name(may be null)
  */
  
  public String getSecondColorName() {
    return (color2Tag_);
  }  

  /***************************************************************************
  **
  ** Replace color with given value
  */
  
  public boolean replaceColor(String oldID, String newID) {
    boolean retval = false;
    if (colorTag_.equals(oldID)) {
      colorTag_ = newID;
      retval = true;
    }
    if ((color2Tag_ != null) && (color2Tag_.equals(oldID))) {
      color2Tag_ = newID;
      retval = true;
    }    
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Get the orientation (LEFT, RIGHT, UP, DOWN)
  */
  
  public int getOrientation() {
    return (orient_);
  }

  /***************************************************************************
  **
  ** Set the orientation
  */
  
  public void setOrientation(int orient) {
    orient_ = orient;
    orientTag_ = mapOrientTypes(orient);
    return;
  }
  
  /***************************************************************************
  **
  ** Get the local font (may be null)
  */
  
  public FontManager.FontOverride getFontOverride() {
    return (localFont_);
  }
  
  /***************************************************************************
  **
  ** Set the local font
  */
  
  public void setFontOverride(FontManager.FontOverride fontover) {
    localFont_ = fontover;
    return;
  }    
  
  /***************************************************************************
  **
  ** Get the direction to grow for adding extra pads
  */
  
  public int getExtraGrowthDirection() {
    return (extraPadGrowth_);
  }

  /***************************************************************************
  **
  ** Set the direction to grow
  */
  
  public void setExtraGrowthDirection(int growthDir) {
    extraPadGrowth_ = growthDir;
    return;
  }   

  /***************************************************************************
  **
  ** Get the location
  */
  
  public Point2D getLocation() {
    return (location_);
  }
  /***************************************************************************
  **
  ** Get the renderer
  */
  
  public INodeRenderer getRenderer() {
    return (myRenderer_);
  }
  
  /***************************************************************************
  **
  ** Set the location
  */
  
  public void setLocation(Point2D newLocation) {
    location_ = newLocation;
    return;
  }
  
  /***************************************************************************
  **
  ** Get whether to hide the name
  */
  
  public boolean getHideName() {
    return (hideName_);
  }
  
  /***************************************************************************
  **
  ** Set whether to hide the name
  */
  
  public void setHideName(boolean hideName) {
    hideName_ = hideName;
    return;
  }   
  
  /***************************************************************************
  **
  ** Get the line break definition
  */
  
  public String getLineBreakDef() {
    return (lineBreakDef_);
  }
  
 /***************************************************************************
  **
  ** Set the line break definition
  */
  
  public void setLineBreakDef(String lineBreakDef) {
    lineBreakDef_ = lineBreakDef;
    return;
  }
 
  /***************************************************************************
  **
  ** Trim and set the line break definition
  */
  
  public void trimAndSetLineBreakDef(String lineBreakDef, String untrimmed) {
    String trimmedBreakDef = LineBreaker.fixBreaksForTrim(lineBreakDef, untrimmed);
    lineBreakDef_ = LineBreaker.massageLineBreaks(trimmedBreakDef, untrimmed.trim());
    return;
  }

  /***************************************************************************
  **
  ** Modify the line break definition.  Used for global propagation from a cumulative
  ** editing session.
  */
  
  public void applyLineBreakMod(LineBreaker.LineBreakChangeSteps steps, String untrimmed) {
    String resolvedBreakDef = LineBreaker.resolveNameChange(lineBreakDef_, steps);
    trimAndSetLineBreakDef(resolvedBreakDef, untrimmed);
    return;
  }  
 
  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();
    out.print("<nprop");
    out.print(" id=\"");
    out.print(nodeTag_);    
    out.print("\" color=\"");
    out.print(colorTag_);
    if (color2Tag_ != null) {    
      out.print("\" color2=\"");
      out.print(color2Tag_);
    }
    out.print("\" x=\"");
    out.print(location_.getX());
    out.print("\" y=\"");
    out.print(location_.getY());
    out.print("\" orient=\"");
    out.print(orientTag_);
    if (hideName_) {
      out.print("\" hideName=\"true");
    }
    if (extraPadGrowth_ != NO_GROWTH) {
      out.print("\" extraGrowth=\"");
      out.print(mapGrowthTypes(extraPadGrowth_));
    } 
    
    if (localFont_ != null) {
      out.print("\" fsize=\"");
      out.print(localFont_.size);
      out.print("\" fbold=\"");
      out.print(localFont_.makeBold);    
      out.print("\" fital=\"");
      out.print(localFont_.makeItalic);    
      out.print("\" fsans=\"");
      out.print(localFont_.makeSansSerif);
    }
    
    if (lineBreakDef_ != null) {
      out.print("\" breakDef=\"");
      out.print(lineBreakDef_);
    }        

    out.println("\" />");
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Return the element keyword that we are interested in
  **
  */
  
  public static String keywordOfInterest() {
    return ("nprop");
  }
  
  /***************************************************************************
  **
  ** Return a pad limit map.  Note this just provides classic fixed limits.
  ** Actual limits may overflow if noted.  Currently used to handle change 
  ** in node type...
  **
  */
  
  public static Map<Integer, PadLimits> getFixedPadLimits() {
    HashMap<Integer, PadLimits> retval = new HashMap<Integer, PadLimits>();
    for (int i = Node.MIN_NODE_TYPE; i <= Node.MAX_NODE_TYPE; i++) {
      INodeRenderer rend = buildRenderer(i);
      Integer key = new Integer(i);
      PadLimits limits = new PadLimits();
      limits.landingPadsCanOverflow = rend.landingPadsCanOverflow();
      limits.sharedNamespace = rend.sharedPadNamespaces();
      limits.launchPadMax = rend.getFixedLaunchPadMax();
      limits.landingPadMax = rend.getFixedLandingPadMax();
      limits.defaultPadCount = DBNode.getDefaultPadCount(i);
      retval.put(key, limits);
    }
    
    return (retval);
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class NodePropertiesWorker extends AbstractFactoryClient {
 
    private ColorResolver cRes_;
    
    public NodePropertiesWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("nprop");
    }
  
    public void installContext(DataAccessContext dacx) {
      cRes_ = dacx.getColorResolver();
      return;
    }
 
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("nprop")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.nodeProps = buildFromXML(elemName, attrs);
        retval = board.nodeProps;
      }
      return (retval);     
    }
    
    private NodeProperties buildFromXML(String elemName, Attributes attrs) throws IOException {      
      String id = AttributeExtractor.extractAttribute(elemName, attrs, "nprop", "id", true);
      String color = AttributeExtractor.extractAttribute(elemName, attrs, "nprop", "color", true);
      String color2 = AttributeExtractor.extractAttribute(elemName, attrs, "nprop", "color2", false);
      String x = AttributeExtractor.extractAttribute(elemName, attrs, "nprop", "x", true);
      String y = AttributeExtractor.extractAttribute(elemName, attrs, "nprop", "y", true);
      String orient = AttributeExtractor.extractAttribute(elemName, attrs, "nprop", "orient", false);
      if (orient == null) {        
        orient = RIGHT_TAG_;  // fix legacy cases
      }
      String growth = AttributeExtractor.extractAttribute(elemName, attrs, "nprop", "extraGrowth", false);
      String doHideVal = AttributeExtractor.extractAttribute(elemName, attrs, "nprop", "hideName", false);
      if (doHideVal == null) {       
        doHideVal = "false";
      }
      String fsize = AttributeExtractor.extractAttribute(elemName, attrs, "nprop", "fsize", false);
      String fbold = AttributeExtractor.extractAttribute(elemName, attrs, "nprop", "fbold", false);
      String fital = AttributeExtractor.extractAttribute(elemName, attrs, "nprop", "fital", false);
      String fsans = AttributeExtractor.extractAttribute(elemName, attrs, "nprop", "fsans", false);
      String breakDef = AttributeExtractor.extractAttribute(elemName, attrs, "nprop", "breakDef", false);
   
      double xPos = -1.0;
      double yPos = -1.0;
      try {
        xPos = Double.parseDouble(x);
        yPos = Double.parseDouble(y);     
      } catch (NumberFormatException nfe) {
        throw new IOException();
      } 
    
      FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
      
      boolean doHide = Boolean.valueOf(doHideVal).booleanValue();
      Node theNode = board.genome.getNode(id);
      int nodeType = theNode.getNodeType();
      // Legacy cases:
      if (usesGrowth(nodeType) && (growth == null)) {
        growth = HORIZONTAL_GROWTH_TAG_;
      }

      NodeProperties nprop = new NodeProperties(cRes_, id, color, color2, orient, xPos, yPos, doHide, growth, breakDef, nodeType);
      if (fsize != null) {      
        boolean makeBold = Boolean.valueOf(fbold).booleanValue();
        boolean makeItalic = Boolean.valueOf(fital).booleanValue();
        boolean makeSansSerif = Boolean.valueOf(fsans).booleanValue();

        int size;
        try {
          size = Integer.parseInt(fsize); 
        } catch (NumberFormatException nfe) {
          throw new IOException();
        }    
        if ((size < FontManager.MIN_SIZE) || (size > FontManager.MAX_SIZE)) {
          throw new IOException();
        }
        FontManager.FontOverride fo = new FontManager.FontOverride(size, makeBold, makeItalic, makeSansSerif);
        nprop.setFontOverride(fo);
      }
      
      return (nprop);
    }
  }  
  
  /***************************************************************************
  **
  ** build renderer
  */

  public static INodeRenderer buildRenderer(int nodeType) {
    
    switch (nodeType) {
      case Node.BARE:
        return (new BareNodeFree());
      case Node.BOX:
        return (new BoxNodeFree());
      case Node.BUBBLE:
        return (new BubbleNodeFree());
      case Node.INTERCELL:
        return (new IntercellFree());
      case Node.SLASH:
        return (new SlashFree());
      case Node.DIAMOND:
        return (new DiamondNodeFree());        
      case Node.GENE:
        return (new GeneFree());        
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Get the default color
  */

  public static String defaultColor(int nodeType) {
    switch (nodeType) {
      case Node.BARE:
      case Node.GENE:
      case Node.INTERCELL:
      case Node.SLASH:
        return ("black");
      case Node.BUBBLE:        
      case Node.DIAMOND:
        return ("white"); 
      case Node.BOX:
        return ("darkGray");
      default:
        System.err.println("Bad node type is " + nodeType);
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Do we set the link along with the node?
  */

  public static boolean setWithLinkColor(int nodeType) {
    switch (nodeType) {
      case Node.GENE:
      case Node.INTERCELL:
      case Node.SLASH:        
        return (true);  
      case Node.BARE:
      case Node.BUBBLE:        
      case Node.DIAMOND:
      case Node.BOX:
        return (false);
      default:
        throw new IllegalArgumentException();
    }
  }

  /***************************************************************************
  **
  ** Return map between orientations when needed
  */
  
  public static int orientationMap(int oldType, int newType, int oldOrient) {
    int oldCount = getOrientTypeCount(oldType);
    int newCount = getOrientTypeCount(newType);    
    
    if (newCount >= oldCount) {
      return (oldOrient);
    }
    
    if (newCount == 1) {
      return (RIGHT);
    }
    
    if ((oldOrient == UP) || (oldOrient == DOWN)) {
      return (RIGHT);
    } else {
      return (oldOrient);
    }  
  }
  
  /***************************************************************************
  **
  ** Return map between growth directions when needed
  */
  
  public static int extraGrowthMap(int oldType, int newType, int oldExtra) {
    boolean oldUses = usesGrowth(oldType);
    boolean newUses = usesGrowth(newType);    
    
    if ((oldUses && newUses) || (!oldUses && !newUses))  {
      return (oldExtra);
    }
    
    if (!newUses) {
      return (NO_GROWTH);
    } else {
      return (HORIZONTAL_GROWTH);
    }
  }  

  /***************************************************************************
  **
  ** Return if growth orientation is needed
  */
  
  public static boolean usesGrowth(int nodeType) {
    switch (nodeType) {
      case Node.GENE:
      case Node.INTERCELL:
      case Node.SLASH:
      case Node.BARE:
      case Node.BOX:  
        return (false);
      case Node.DIAMOND:        
      case Node.BUBBLE:        
        return (true);
      default:
        throw new IllegalArgumentException();
    }    
  }    
  
  /***************************************************************************
  **
  ** Return if name can be hidden
  */
  
  public static boolean canHideName(int nodeType) {
    switch (nodeType) {
      case Node.GENE:
      case Node.BARE:
      case Node.BOX:  
        return (false);
      case Node.DIAMOND:        
      case Node.BUBBLE:
      case Node.INTERCELL:
      case Node.SLASH:
        return (true);
      default:
        throw new IllegalArgumentException();
    }    
  }    

  /***************************************************************************
  **
  ** Return the count of possible orientation values
  */
  
  public static int getOrientTypeCount(int nodeType) {
    switch (nodeType) {
      case Node.GENE:
        return (2);
      case Node.INTERCELL:
      case Node.SLASH:        
        return (4);
      case Node.BARE:
      case Node.BUBBLE:        
      case Node.DIAMOND:
      case Node.BOX:
        return (1);
      default:
        throw new IllegalArgumentException();
    }    
  }  

  /***************************************************************************
  **
  ** Return possible orientation values
  */
  
  public static Vector<ChoiceContent> getOrientTypeChoices(DataAccessContext dacx, int nodeType) {
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    switch (nodeType) {
      case Node.GENE:
        retval.add(orientTypeForCombo(dacx, LEFT));
        retval.add(orientTypeForCombo(dacx, RIGHT));
        return (retval);
      case Node.INTERCELL:
      case Node.SLASH:        
        retval.add(orientTypeForCombo(dacx, LEFT));
        retval.add(orientTypeForCombo(dacx, RIGHT));
        retval.add(orientTypeForCombo(dacx, UP));
        retval.add(orientTypeForCombo(dacx, DOWN)); 
        return (retval);
      case Node.BARE:
      case Node.BUBBLE:        
      case Node.DIAMOND:
      case Node.BOX:
        retval.add(orientTypeForCombo(dacx, RIGHT));
        return (retval);
      default:
        throw new IllegalArgumentException();
    }    
  }
  
  /***************************************************************************
  **
  ** Get a combo box element
  */
  
  public static ChoiceContent orientTypeForCombo(DataAccessContext dacx, int orientType) {
    return (new ChoiceContent(dacx.getRMan().getString("nprop." + mapOrientTypes(orientType)), orientType));
  }
  
  /***************************************************************************
  **
  ** Map the orient types
  */

  public static String mapOrientTypes(int orientType) {
    switch (orientType) {
      case NONE:
        return (NONE_TAG_);
      case LEFT:
        return (LEFT_TAG_);  
      case RIGHT:
        return (RIGHT_TAG_);  
      case UP:
        return (UP_TAG_);  
      case DOWN:
        return (DOWN_TAG_);                        
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Invert the orient types
  */

  public static int reverseOrient(int orientType) {
    switch (orientType) {
      case NONE: // Synonymous with RIGHT
        return (LEFT);
      case LEFT:
        return (RIGHT);  
      case RIGHT:
        return (LEFT);  
      case UP:
        return (DOWN);  
      case DOWN:
        return (UP);                        
      default:
        throw new IllegalArgumentException();
    }
  }
  
  
  
  
  /***************************************************************************
  **
  ** map the source type tags
  */

  public static int mapOrientTypeTag(String orientTag) {  
    if (orientTag.equalsIgnoreCase(NONE_TAG_)) {
      return (NONE);
    } else if (orientTag.equalsIgnoreCase(LEFT_TAG_)) {
      return (LEFT);       
    } else if (orientTag.equalsIgnoreCase(RIGHT_TAG_)) {
      return (RIGHT);   
    } else if (orientTag.equalsIgnoreCase(UP_TAG_)) {
      return (UP);   
    } else if (orientTag.equalsIgnoreCase(DOWN_TAG_)) {
      return (DOWN);         
    } else {
      throw new IllegalArgumentException();
    }  
  } 
  
  /***************************************************************************
  **
  ** Return possible orientation values
  */
  
  public static Vector<ChoiceContent> getExtraGrowthChoices(DataAccessContext dacx, int nodeType) {
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    switch (nodeType) {
      case Node.GENE:
      case Node.INTERCELL:
      case Node.SLASH:        
      case Node.BARE:    
      case Node.BOX:
        return (retval);
      case Node.DIAMOND:        
      case Node.BUBBLE:
        retval.add(growthTypeForCombo(dacx, HORIZONTAL_GROWTH));
        retval.add(growthTypeForCombo(dacx, VERTICAL_GROWTH));        
        return (retval);        
      default:
        throw new IllegalArgumentException();
    }    
  }
  
  /***************************************************************************
  **
  ** Get a combo box element
  */
  
  public static ChoiceContent growthTypeForCombo(DataAccessContext dacx, int growthType) {
    return (new ChoiceContent(dacx.getRMan().getString("nprop." + mapGrowthTypes(growthType)), growthType));
  }
  
  /***************************************************************************
  **
  ** Map the growth types
  */

  public static String mapGrowthTypes(int growthType) {
    switch (growthType) {
      case NO_GROWTH:
        return (NO_GROWTH_TAG_);
      case HORIZONTAL_GROWTH:
        return (HORIZONTAL_GROWTH_TAG_);  
      case VERTICAL_GROWTH:
        return (VERTICAL_GROWTH_TAG_);                     
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** map the growth type tags
  */

  public static int mapGrowthTypeTag(String growthTag) {  
    if (growthTag.equalsIgnoreCase(NO_GROWTH_TAG_)) {
      return (NO_GROWTH);
    } else if (growthTag.equalsIgnoreCase(HORIZONTAL_GROWTH_TAG_)) {
      return (HORIZONTAL_GROWTH);       
    } else if (growthTag.equalsIgnoreCase(VERTICAL_GROWTH_TAG_)) {
      return (VERTICAL_GROWTH);
    } else {
      throw new IllegalArgumentException();
    }  
  }   
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Used to transmit pad limits for nodes
  **
  */
  
  public static class PadLimits {
    public boolean landingPadsCanOverflow;
    public boolean sharedNamespace;
    public int launchPadMax;
    public int landingPadMax;
    public int defaultPadCount;
  }

}  

