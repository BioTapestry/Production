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
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.perturb.PertDisplayOptions;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.EnumChoiceContent;
import org.systemsbiology.biotapestry.util.HandlerAndManagerSource;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.NameValuePair;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;

import org.xml.sax.Attributes;

/****************************************************************************
**
** A class to specify display options
*/

public class DisplayOptions implements Cloneable {
  
  ////////////////////////////////////////////////////////////////////////////    
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  public static final int NO_BUS_BRANCHES          = 0;
  public static final int FILLED_BUS_BRANCHES      = 1;
  public static final int OUTLINED_BUS_BRANCHES    = 2; 
  private static final int NUM_BUS_BRANCH_OPTIONS_ = 3;

  public static final String NO_BUS_BRANCHES_TAG       = "noBranches";
  public static final String FILLED_BUS_BRANCHES_TAG   = "filledBranches";  
  public static final String OUTLINED_BUS_BRANCHES_TAG = "outlinedBranches";   

  public static final int NO_EVIDENCE_DISPLAY       = 0;  
  public static final int EVIDENCE_WITH_DIAMONDS    = 1;
  public static final int EVIDENCE_WITH_THICK_LINES = 2;
  public static final int EVIDENCE_WITH_BOTH        = 3; 
  public static final int EVIDENCE_IS_CUSTOM        = 4;   
  private static final int NUM_EVIDENCE_OPTIONS_    = 5;  

  public static final String NO_EVIDENCE_TAG         = "noEvidence";  
  public static final String DIAMOND_EVIDENCE_TAG    = "diamondEvidence";
  public static final String THICK_LINK_EVIDENCE_TAG = "thickEvidence";  
  public static final String BOTH_TYPES_EVIDENCE_TAG = "diamondThickEvidence"; 
  public static final String CUSTOM_EVIDENCE_TAG     = "customEvidence";  
  
  public enum FirstZoom {
    FIRST_ZOOM_TO_ALL_MODELS("firstZoomToAll"),
    FIRST_ZOOM_TO_CURRENT_MODEL("firstZoomToCurrent"),
    FIRST_ZOOM_TO_WORKSPACE("firstZoomToWorkspace"),
    ;   
    private String tag_;
 
    FirstZoom(String tag) {
      this.tag_ = tag;  
    }  
     
    public String getTag() {
      return (tag_);      
    }
    
    public static FirstZoom mapFirstZoomTypeTag(String firstZoomTypeTag) {
      for (FirstZoom z : FirstZoom.values()) {
        if (z.getTag().equalsIgnoreCase(firstZoomTypeTag)) {
          return (z);
        }
      }
      throw new IllegalArgumentException();
    }
  }

  public enum NavZoom {
    NAV_MAINTAIN_ZOOM("navZoomMaintain"),
    NAV_ZOOM_TO_EACH_MODEL("navZoomToEach"),
    ;   
    private String tag_;
 
    NavZoom(String tag) {
      this.tag_ = tag;  
    }  
     
    public String getTag() {
      return (tag_);      
    }
    
    public static NavZoom mapNavZoomTypeTag(String navZoomTypeTag) {
      for (NavZoom z : NavZoom.values()) {
        if (z.getTag().equalsIgnoreCase(navZoomTypeTag)) {
          return (z);
        }
      }
      throw new IllegalArgumentException();
    }
  } 
  
  public static final int NO_NODE_ACTIVITY_DISPLAY    = 0;  
  public static final int NODE_ACTIVITY_COLOR         = 1;
  public static final int NODE_ACTIVITY_PIE           = 2;
  public static final int NODE_ACTIVITY_BOTH          = 3; 
  private static final int NUM_NODE_ACTIVITY_OPTIONS_ = 4;  
  
  public static final String NO_NODE_ACTIVITY_DISPLAY_TAG = "noNodeActivity";  
  public static final String NODE_ACTIVITY_COLOR_TAG      = "colorNodeActivity";  
  public static final String NODE_ACTIVITY_PIE_TAG        = "pieNodeActivity";  
  public static final String NODE_ACTIVITY_BOTH_TAG       = "colorAndPieNodeActivity";  

  public static final int NO_LINK_ACTIVITY_DISPLAY    = 0;  
  public static final int LINK_ACTIVITY_COLOR         = 1;
  public static final int LINK_ACTIVITY_THICK         = 2;
  public static final int LINK_ACTIVITY_BOTH          = 3; 
  private static final int NUM_LINK_ACTIVITY_OPTIONS_ = 4;
  
  public static final String NO_LINK_ACTIVITY_DISPLAY_TAG = "noLinkActivity";  
  public static final String LINK_ACTIVITY_COLOR_TAG      = "colorLinkActivity";  
  public static final String LINK_ACTIVITY_THICK_TAG      = "thicknessLinkActivity";  
  public static final String LINK_ACTIVITY_BOTH_TAG       = "colorAndThicknessLinkActivity";  
  
  private static final double DEFAULT_WEAK_EXPRESSION_LEVEL_ = 0.50; 
  
  public static final double INACTIVE_BRIGHT_MIN       = 0.5;  
  public static final double INACTIVE_BRIGHT_MAX       = 0.75;
  
  private static final double DEFAULT_INACTIVE_BRIGHT_ = INACTIVE_BRIGHT_MAX; 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private int renderBusBranches_;  
  private int renderEvidence_;  
  private int repressionFootExtraSize_; 
  private int renderNodeActivity_; 
  private int renderLinkActivity_;   
  private FirstZoom firstZoom_; 
  private NavZoom navZoom_;
  private double weakExpressionLevel_;
  private boolean displayExpressionTableTree_;
  private TreeMap<Integer, CustomEvidenceDrawStyle> customEvidence_;
  private float[] inactiveHSV_;
  private double inactiveBright_;
  private PertDisplayOptions legacyPDO_;
  private Color inactiveCol_;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public DisplayOptions() {
    renderBusBranches_ = NO_BUS_BRANCHES;
    renderEvidence_ = EVIDENCE_WITH_DIAMONDS;
    repressionFootExtraSize_ = 0;    
    firstZoom_ = FirstZoom.FIRST_ZOOM_TO_ALL_MODELS;    
    navZoom_ = NavZoom.NAV_MAINTAIN_ZOOM;
    renderNodeActivity_ = NODE_ACTIVITY_COLOR; 
    renderLinkActivity_ = LINK_ACTIVITY_COLOR;
    weakExpressionLevel_ = DEFAULT_WEAK_EXPRESSION_LEVEL_;
    displayExpressionTableTree_ = true;
    customEvidence_ = new TreeMap<Integer, CustomEvidenceDrawStyle>();
    legacyPDO_ = null;
    inactiveBright_ = DEFAULT_INACTIVE_BRIGHT_;
  }

  /***************************************************************************
  **
  ** Constructor for XML
  */

  public DisplayOptions(String branches, String evidence, String footSize, 
                        String firstZoom, String navZoom, 
                        String nodeActivity, String linkActivity, 
                        String weakLevel, String dispTreeStr,
                        String pertDefMin, String pertDefMax, 
                        String scaleTag, String breakOutStr, String inactiveBright) throws IOException {
    
    try {
      renderBusBranches_ = (branches == null) ? NO_BUS_BRANCHES : mapBranchTag(branches); 
      renderEvidence_ = (evidence == null) ? EVIDENCE_WITH_DIAMONDS : mapEvidenceTag(evidence);
      firstZoom_ = (firstZoom == null) ? FirstZoom.FIRST_ZOOM_TO_ALL_MODELS : FirstZoom.mapFirstZoomTypeTag(firstZoom); 
      navZoom_ = (navZoom == null) ? NavZoom.NAV_MAINTAIN_ZOOM : NavZoom.mapNavZoomTypeTag(navZoom);
      renderNodeActivity_ = (nodeActivity == null) ? NODE_ACTIVITY_COLOR : mapNodeActivityTag(nodeActivity);
      renderLinkActivity_ = (linkActivity == null) ? LINK_ACTIVITY_COLOR : mapLinkActivityTag(linkActivity);
    } catch (IllegalArgumentException ex) {
      throw new IOException();
    }
    
    displayExpressionTableTree_ = (dispTreeStr != null) ? Boolean.valueOf(dispTreeStr).booleanValue() : true;
  
    if (footSize != null) {
      try {
        repressionFootExtraSize_ = Integer.parseInt(footSize);
      } catch (NumberFormatException ex) {
        throw new IOException();
      }
    } else {
      repressionFootExtraSize_ = 0;
    }
    
    if (weakLevel != null) {
      try {
        weakExpressionLevel_ = Double.parseDouble(weakLevel);
      } catch (NumberFormatException ex) {
        throw new IOException();
      }
      if ((weakExpressionLevel_ < 0.0) || (weakExpressionLevel_ > 1.0)) {
        throw new IOException();
      }
    } else {
      weakExpressionLevel_ = DEFAULT_WEAK_EXPRESSION_LEVEL_;
    }
    
    if (inactiveBright != null) {
      try {
        inactiveBright_ = Double.parseDouble(inactiveBright);
      } catch (NumberFormatException ex) {
        throw new IOException();
      }
      if ((inactiveBright_ < INACTIVE_BRIGHT_MIN) || (inactiveBright_ > INACTIVE_BRIGHT_MAX)) {
        throw new IOException();
      }
    } else {
      inactiveBright_ = DEFAULT_INACTIVE_BRIGHT_;
    }

    customEvidence_ = new TreeMap<Integer, CustomEvidenceDrawStyle>();
    
    //
    // Legacy Perturbation-specific options:
    //
    
    if ((pertDefMin != null) || (pertDefMax != null) || (scaleTag != null) || (breakOutStr != null)) {  
      legacyPDO_ = new PertDisplayOptions(null, pertDefMin, pertDefMax, scaleTag, breakOutStr);
    }
    
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
 
  /***************************************************************************
  **
  ** Set custom evidence
  **
  */
  
  public void setCustomEvidence(int level, CustomEvidenceDrawStyle ceds) {
    customEvidence_.put(new Integer(level), ceds);
    return;
  }
 
  /***************************************************************************
  **
  ** Get custom evidence.  May be null if no style.
  **
  */
  
  public CustomEvidenceDrawStyle getCustomEvidence(int level) {
    return (customEvidence_.get(new Integer(level)));
  }
  
  /***************************************************************************
  **
  ** Fill map with custom evidence.
  **
  */
  
  public void fillCustomEvidenceMap(SortedMap<Integer, CustomEvidenceDrawStyle> map) {
    Iterator<Integer> ceit = customEvidence_.keySet().iterator();
    while (ceit.hasNext()) {
      Integer level = ceit.next();
      CustomEvidenceDrawStyle ceds = customEvidence_.get(level);
      map.put(level, ceds.clone()); 
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Get inactive color
  **
  */
  
  public Color getInactiveGray() {
    if (inactiveCol_ == null) {
      inactiveCol_ = new Color((int)(255.0 * inactiveBright_), (int)(255.0 * inactiveBright_), (int)(255.0 * inactiveBright_));
    }
    return (inactiveCol_);
  }
  
  /***************************************************************************
  **
  ** Get inactive color
  **
  */
  
  public float[] getInactiveGrayHSV() {
    if (inactiveHSV_ == null) {
      inactiveHSV_ = new float[3];
      Color inactive = getInactiveGray();
      Color.RGBtoHSB(inactive.getRed(), inactive.getGreen(), inactive.getBlue(), inactiveHSV_);
    } 
    return (inactiveHSV_);
  }

  /***************************************************************************
  **
  ** Get how link drawing style changes with link activity.
  */

  public int getLinkActivity() {
    return (renderLinkActivity_);
  }
  
  /***************************************************************************
  **
  ** Answer if we show an expression table tree
  */

  public boolean showExpressionTableTree() {
    return (displayExpressionTableTree_);
  }
  
  /***************************************************************************
  **
  ** Get how drawing style changes with evidence setting.  May be null.
  */

  public PerLinkDrawStyle getEvidenceDrawChange(int evidence) {
 
    PerLinkDrawStyle retval = null;
    
    if (evidence == Linkage.LEVEL_NONE) {
      return (retval);
    }
    
    if ((renderEvidence_ == EVIDENCE_WITH_THICK_LINES) ||
        (renderEvidence_ == EVIDENCE_WITH_BOTH)) {   
      SuggestedDrawStyle style = 
        new SuggestedDrawStyle(SuggestedDrawStyle.NO_STYLE, null, 
                               SuggestedDrawStyle.THICK_THICKNESS);
      retval = new PerLinkDrawStyle(style, PerLinkDrawStyle.TO_ROOT);
    } else if (renderEvidence_ == EVIDENCE_IS_CUSTOM) {
      CustomEvidenceDrawStyle ceds = customEvidence_.get(new Integer(evidence));
      if (ceds == null) {
        return (retval);
      }
      retval = ceds.getDrawStyle();  // May be null...
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Answer if we draw evidence glyphs
  */

  public boolean drawEvidenceGlyphs(int evidence) {
    if (evidence == Linkage.LEVEL_NONE) {
      return (false);
    }
    if ((renderEvidence_ == EVIDENCE_WITH_DIAMONDS) ||
        (renderEvidence_ == EVIDENCE_WITH_BOTH)) {
      return (true);
    }
    if (renderEvidence_ == EVIDENCE_IS_CUSTOM) {
      CustomEvidenceDrawStyle ceds = customEvidence_.get(new Integer(evidence));
      if (ceds == null) {
        return (true);  // Default is to show it...
      }
      return (ceds.showDiamonds());
    }
    return (false);
  }  

  /***************************************************************************
  **
  ** Get node activity setting
  */

  public int getNodeActivity() {
    return (renderNodeActivity_);
  } 
  
  /***************************************************************************
  **
  ** Answer if node activity modulates color
  */

  public boolean changeNodeColorForActivity() {
    return ((renderNodeActivity_ == DisplayOptions.NODE_ACTIVITY_COLOR) ||
            (renderNodeActivity_ == DisplayOptions.NODE_ACTIVITY_BOTH));
  } 
 
  /***************************************************************************
  **
  ** Answer if node activity modulates shows a pie
  */

  public boolean showNodePieForActivity() {
    return ((renderNodeActivity_ == DisplayOptions.NODE_ACTIVITY_PIE) ||
            (renderNodeActivity_ == DisplayOptions.NODE_ACTIVITY_BOTH));
  } 
  
  /***************************************************************************
  **
  ** Get evidence setting
  */

  public int getEvidence() {
    return (renderEvidence_);
  }

  /***************************************************************************
  **
  ** Get branch mode
  */

  public int getBranchMode() {
    return (renderBusBranches_);
  }
  
  /***************************************************************************
  **
  ** Get extra foot size
  */

  public int getExtraFootSize() {
    return (repressionFootExtraSize_);
  } 
  
  /***************************************************************************
  **
  ** Get weak expression level
  */

  public double getWeakExpressionLevel() {
    return (weakExpressionLevel_);
  }   
  
  /***************************************************************************
  **
  ** Get first zoom mode
  */

  public FirstZoom getFirstZoomMode() {
    return (firstZoom_);
  } 
  
  /***************************************************************************
  **
  ** Get nav zoom mode
  */

  public NavZoom getNavZoomMode() {
    return (navZoom_);
  }   
  
  /***************************************************************************
  **
  ** Set evidence setting
  */

  public void setEvidence(int evidence) {
    renderEvidence_ = evidence;
    return;
  }

  /***************************************************************************
  **
  ** Set branch mode
  */

  public void setBranchMode(int branchMode) {
    renderBusBranches_ = branchMode;
    return;
  }
  
  /***************************************************************************
  **
  ** Set extra foot size
  */

  public void setExtraFootSize(int extraSize) {
    if (extraSize < 0) {
      throw new IllegalArgumentException();
    }
    repressionFootExtraSize_ = extraSize;
    return;
  } 
 
  /***************************************************************************
  **
  ** Set weak expression level
  */

  public void setWeakExpressionLevel(double weakLevel) {
    if ((weakLevel < 0.0) || (weakLevel > 1.0)) {
      throw new IllegalArgumentException();
    }
    weakExpressionLevel_ = weakLevel;
    return;
  }   
  
  /***************************************************************************
  **
  ** Set first zoom mode
  */

  public void setFirstZoomMode(FirstZoom firstZoom) {
    firstZoom_ = firstZoom;
    return;
  } 
  
  /***************************************************************************
  **
  ** Set nav zoom mode
  */

  public void setNavZoomMode(NavZoom navZoom) {
    navZoom_ = navZoom;
    return;
  }     
  
  /***************************************************************************
  **
  ** Set node activity mode
  */

  public void setNodeActivity(int nodeActivity) {
    renderNodeActivity_ = nodeActivity;
    return;
  }     
  
  /***************************************************************************
  **
  ** Set link activity mode
  */

  public void setLinkActivity(int linkActivity) {
    renderLinkActivity_ = linkActivity;
    return;
  }   
  
  /***************************************************************************
  **
  ** Get inactive gray brightness
  */

  public double getInactiveBright() {
    return (inactiveBright_);
  }
  
  /***************************************************************************
  **
  ** Set inactive gray brightness
  */

  public void setInactiveBright(double ibright) {
    inactiveBright_ = ibright;
    return;
  }

  /***************************************************************************
  **
  ** Set if we show an expression table tree
  */

  public void setShowExpressionTableTree(boolean newVal) {
    displayExpressionTableTree_ = newVal;
    return;
  }
  
  /***************************************************************************
  **
  ** For legacy IO repair
  */
  
  public PertDisplayOptions getLegacyPDO() {
    return (legacyPDO_);
  }

  /***************************************************************************
  **
  ** Set the column headings
  */
  
  public void setColumns(ArrayList<MinMax> columns) {
    legacyPDO_.setColumns(columns);
    return;
  }

  /***************************************************************************
  **
  ** Set a column heading
  */
  
  public void addColumn(MinMax col) {
    legacyPDO_.addColumn(col);
    return;
  }
  
  /***************************************************************************
  **
  ** Add a measurement display color (for IO)
  */
  
  public void addMeasurementDisplayColor(NameValuePair colorPair) {
    legacyPDO_.addMeasurementDisplayColor(colorPair);
    return;
  }
  
  
  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public DisplayOptions clone() {
    try {
      DisplayOptions retval = (DisplayOptions)super.clone();
      retval.customEvidence_ = new TreeMap<Integer, CustomEvidenceDrawStyle>();
      Iterator<Integer> kit = this.customEvidence_.keySet().iterator();
      while (kit.hasNext()) {
        Integer level = kit.next();
        CustomEvidenceDrawStyle ceds = this.customEvidence_.get(level);
        retval.customEvidence_.put(level, ceds);
      }
      
      // We do not clone the legacy PDO data. That is just for legacy IO.
      
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();
    out.print("<displayOptions ");
    
    if (renderBusBranches_ != NO_BUS_BRANCHES) {
      out.print("branches=\"");
      out.print(mapBranches(renderBusBranches_));    
      out.print("\" ");
    }
    if (renderEvidence_ != EVIDENCE_WITH_DIAMONDS) {
      out.print("evidence=\"");
      out.print(mapEvidence(renderEvidence_));    
      out.print("\" ");
    }
    if (repressionFootExtraSize_ != 0) {
      out.print("bigFoot=\"");
      out.print(repressionFootExtraSize_);    
      out.print("\" ");
    }
    if (firstZoom_ != FirstZoom.FIRST_ZOOM_TO_ALL_MODELS) {
      out.print("firstZoom=\"");
      out.print(firstZoom_.getTag());    
      out.print("\" ");
    }
    if (navZoom_ != NavZoom.NAV_MAINTAIN_ZOOM) {
      out.print("navZoom=\"");
      out.print(navZoom_.getTag());    
      out.print("\" ");
    }    
    if (renderNodeActivity_ != NODE_ACTIVITY_COLOR) {
      out.print("nodeActivity=\"");
      out.print(mapNodeActivity(renderNodeActivity_));    
      out.print("\" ");
    }    
    if (renderLinkActivity_ != LINK_ACTIVITY_COLOR) {
      out.print("linkActivity=\"");
      out.print(mapLinkActivity(renderLinkActivity_));    
      out.print("\" ");
    }
    if (weakExpressionLevel_ != DEFAULT_WEAK_EXPRESSION_LEVEL_) {
      out.print("weakLevel=\"");
      out.print(weakExpressionLevel_);    
      out.print("\" ");
    }
    if (inactiveBright_ != DEFAULT_INACTIVE_BRIGHT_) {
      out.print("inactiveBright=\"");
      out.print(inactiveBright_);    
      out.print("\" ");
    }

    if (!displayExpressionTableTree_) {
      out.print("showTree=\"false\" ");
    }
       
    if (renderEvidence_ == EVIDENCE_IS_CUSTOM) {
      out.println(">");
      ind.up();    
      ind.indent();
      out.println("<customEvidence>");
      ind.up();    
      Iterator<CustomEvidenceDrawStyle> ceit = customEvidence_.values().iterator();
      while (ceit.hasNext()) {
        CustomEvidenceDrawStyle ceds = ceit.next();
        ceds.writeXML(out, ind);
      }
      ind.down().indent(); 
      out.println("</customEvidence>");
      ind.down().indent();
      out.println("</displayOptions>");
    } else {
      out.println("/>");
    }
   
    return;
  }
  
  /***************************************************************************
  ** 
  ** Return merge mismatches
  */

  public List<String> getMergeMismatches(DisplayOptions otherDO, ResourceManager rMan) {
    
    List<String> retval = new ArrayList<String>();
    
    if (this.renderBusBranches_ != otherDO.renderBusBranches_) {
      String currBB = rMan.getString("displayOptions." + mapBranches(this.renderBusBranches_));
      String mergeBB = rMan.getString("displayOptions." + mapNodeActivity(otherDO.renderBusBranches_));
      String fmt = rMan.getString("displayOptions.mismatchBusBranches");
      String report = MessageFormat.format(fmt, new Object[] {currBB, mergeBB, currBB});   
      retval.add(report);
    }
    if (this.renderEvidence_ != otherDO.renderEvidence_) {
      // We want to retain any custom evidence entries we are using.
      String currRE = rMan.getString("displayOptions." + mapEvidence(this.renderEvidence_));
      String mergeRE = rMan.getString("displayOptions." + mapEvidence(otherDO.renderEvidence_));
      String resolvedRE = currRE;
      if (otherDO.renderEvidence_ == EVIDENCE_IS_CUSTOM) {
        this.renderEvidence_ = otherDO.renderEvidence_;
        resolvedRE = mergeRE;
      }    
      String fmt = rMan.getString("displayOptions.mismatchRenderEvidence");
      String report = MessageFormat.format(fmt, new Object[] {currRE, mergeRE, resolvedRE});   
      retval.add(report);
    }
    if (this.repressionFootExtraSize_ != otherDO.repressionFootExtraSize_) {
      String currRFE = Integer.toString(this.repressionFootExtraSize_);
      String mergeRFE = Integer.toString(otherDO.repressionFootExtraSize_);
      String fmt = rMan.getString("displayOptions.mismatchRepressionFoot");
      String report = MessageFormat.format(fmt, new Object[] {currRFE, mergeRFE, currRFE});   
      retval.add(report);
    }
    if (this.renderNodeActivity_ != otherDO.renderNodeActivity_) {
      String currNA = rMan.getString("displayOptions." + mapNodeActivity(this.renderNodeActivity_ ));
      String mergeNA = rMan.getString("displayOptions." + mapNodeActivity(otherDO.renderNodeActivity_ ));
      String fmt = rMan.getString("displayOptions.mismatchNodeActivity");
      String report = MessageFormat.format(fmt, new Object[] {currNA, mergeNA, currNA});   
      retval.add(report);
    }
    if (this.renderLinkActivity_ != otherDO.renderLinkActivity_) {
      String currLA = rMan.getString("displayOptions." + mapLinkActivity(this.renderLinkActivity_ ));
      String mergeLA = rMan.getString("displayOptions." + mapLinkActivity(otherDO.renderLinkActivity_ ));
      String fmt = rMan.getString("displayOptions.mismatchLinkActivity");
      String report = MessageFormat.format(fmt, new Object[] {currLA, mergeLA, currLA});   
      retval.add(report);
    }
    if (this.firstZoom_ != otherDO.firstZoom_) {
      String currFZ = rMan.getString("displayOptions." + this.firstZoom_.getTag());
      String mergeFZ = rMan.getString("displayOptions." + otherDO.firstZoom_.getTag());
      String fmt = rMan.getString("displayOptions.mismatchFirstZoom");
      String report = MessageFormat.format(fmt, new Object[] {currFZ, mergeFZ, currFZ});   
      retval.add(report);
    }
    if (this.navZoom_ != otherDO.navZoom_) {
      String currNZ = rMan.getString("displayOptions." + this.navZoom_.getTag());
      String mergeNZ = rMan.getString("displayOptions." + otherDO.navZoom_.getTag());
      String fmt = rMan.getString("displayOptions.mismatchSelectionZoom");
      String report = MessageFormat.format(fmt, new Object[] {currNZ, mergeNZ, currNZ});   
      retval.add(report);
    }
    if (this.weakExpressionLevel_ != otherDO.weakExpressionLevel_) {
      String currWEL = Double.toString(this.weakExpressionLevel_);
      String mergeWEL = Double.toString(otherDO.weakExpressionLevel_);
      String fmt = rMan.getString("displayOptions.mismatchWeakExpression");
      String report = MessageFormat.format(fmt, new Object[] {currWEL, mergeWEL, currWEL});   
      retval.add(report);
    }
    if (this.displayExpressionTableTree_ != otherDO.displayExpressionTableTree_) {
      String currDET = Boolean.toString(this.displayExpressionTableTree_);
      String mergeDET = Boolean.toString(otherDO.displayExpressionTableTree_);
      String fmt = rMan.getString("displayOptions.mismatchDisplayTree");
      String report = MessageFormat.format(fmt, new Object[] {currDET, mergeDET, currDET});   
      retval.add(report);
    }
    if (this.inactiveBright_ != otherDO.inactiveBright_) {
      String currIB = Double.toString(this.inactiveBright_);
      String mergeIB = Double.toString(otherDO.inactiveBright_);
      String fmt = rMan.getString("displayOptions.mismatchInactiveBright");
      String report = MessageFormat.format(fmt, new Object[] {currIB, mergeIB, currIB});   
      retval.add(report);
    }
    
    //
    // PertDisplay options belong to tab, so not merged.
    //
    
    //
    // If an evidence level is defined in the append data but not in the
    // original, we merge that in. We do not overwrite if a collision:
    //
    
    if (this.renderEvidence_ == EVIDENCE_IS_CUSTOM) {
      HashSet<Integer> mergedLevels = new HashSet<Integer>(this.customEvidence_.keySet()); 
      mergedLevels.addAll(otherDO.customEvidence_.keySet());     
      TreeMap<Integer, CustomEvidenceDrawStyle> mergedEvidence = new TreeMap<Integer, CustomEvidenceDrawStyle>();
      for (Integer level : mergedLevels) {
        CustomEvidenceDrawStyle tds = this.customEvidence_.get(level);
        CustomEvidenceDrawStyle ods = otherDO.customEvidence_.get(level);
        if (tds != null) {
          if (ods != null) {
            if (tds.equals(ods)) {
              String fmt = rMan.getString("displayOptions.customEvidenceMatched");
              String report = MessageFormat.format(fmt, new Object[] {level});   
              retval.add(report);
            } else {
              String fmt = rMan.getString("displayOptions.customEvidenceDropped");
              String report = MessageFormat.format(fmt, new Object[] {level});   
              retval.add(report);
            }
          }
          mergedEvidence.put(level, tds);
        } else {
          if (ods != null) {
            String fmt = rMan.getString("displayOptions.customEvidenceMerged");
            String report = MessageFormat.format(fmt, new Object[] {level});   
            retval.add(report);
            mergedEvidence.put(level, ods);
          }
        }
      }
      this.customEvidence_ = mergedEvidence;
    }
    return (retval);
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** map the branch option
  */
  
  public static String mapBranches(int busBranch) {
    switch (busBranch) {
      case NO_BUS_BRANCHES:
        return (NO_BUS_BRANCHES_TAG);      
      case FILLED_BUS_BRANCHES:
        return (FILLED_BUS_BRANCHES_TAG);
      case OUTLINED_BUS_BRANCHES:
        return (OUTLINED_BUS_BRANCHES_TAG);
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** map the branch option
  */
  
  public static int mapBranchTag(String busBranchTag) { 
    if (busBranchTag.equalsIgnoreCase(NO_BUS_BRANCHES_TAG)) {
      return (NO_BUS_BRANCHES);
    } else if (busBranchTag.equalsIgnoreCase(FILLED_BUS_BRANCHES_TAG)) {
      return (FILLED_BUS_BRANCHES);
    } else if (busBranchTag.equalsIgnoreCase(OUTLINED_BUS_BRANCHES_TAG)) {
      return (OUTLINED_BUS_BRANCHES);
    } else {
      throw new IllegalArgumentException();
    }  
  }
  
  /***************************************************************************
  **
  ** map the evidence
  */
  
  public static String mapEvidence(int evidence) {
    switch (evidence) {
      case NO_EVIDENCE_DISPLAY:
        return (NO_EVIDENCE_TAG);      
      case EVIDENCE_WITH_DIAMONDS:
        return (DIAMOND_EVIDENCE_TAG);
      case EVIDENCE_WITH_THICK_LINES:
        return (THICK_LINK_EVIDENCE_TAG);
      case EVIDENCE_WITH_BOTH:
        return (BOTH_TYPES_EVIDENCE_TAG);  
      case EVIDENCE_IS_CUSTOM:
        return (CUSTOM_EVIDENCE_TAG);          
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** map the evidence tag
  */
  
  public static int mapEvidenceTag(String evidenceTag) { 
    if (evidenceTag.equalsIgnoreCase(NO_EVIDENCE_TAG)) {
      return (NO_EVIDENCE_DISPLAY);
    } else if (evidenceTag.equalsIgnoreCase(DIAMOND_EVIDENCE_TAG)) {
      return (EVIDENCE_WITH_DIAMONDS);
    } else if (evidenceTag.equalsIgnoreCase(THICK_LINK_EVIDENCE_TAG)) {
      return (EVIDENCE_WITH_THICK_LINES);
    } else if (evidenceTag.equalsIgnoreCase(BOTH_TYPES_EVIDENCE_TAG)) {
      return (EVIDENCE_WITH_BOTH);      
    } else if (evidenceTag.equalsIgnoreCase(CUSTOM_EVIDENCE_TAG)) {
      return (EVIDENCE_IS_CUSTOM);            
    } else {
      throw new IllegalArgumentException();
    }  
  }  
  
  /***************************************************************************
  **
  ** Useful for JComboBoxes
  */

  public static Vector<ChoiceContent> branchOptions(HandlerAndManagerSource hams) {
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    for (int i = 0; i < NUM_BUS_BRANCH_OPTIONS_; i++) {
      retval.add(mapBranchOptions(hams, i));
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Useful for JComboBoxes
  */

  public static ChoiceContent mapBranchOptions(HandlerAndManagerSource hams, int val) {
    return (new ChoiceContent(hams.getRMan().getString("displayOptions." + mapBranches(val)), val));
  }
  
  /***************************************************************************
  **
  ** Useful for JComboBoxes
  */

  public static Vector<ChoiceContent> evidenceOptions(HandlerAndManagerSource hams) {
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    for (int i = 0; i < NUM_EVIDENCE_OPTIONS_; i++) {
      retval.add(mapEvidenceOptions(hams, i));
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Useful for JComboBoxes
  */

  public static ChoiceContent mapEvidenceOptions(HandlerAndManagerSource hams, int val) {
    return (new ChoiceContent(hams.getRMan().getString("displayOptions." + mapEvidence(val)), val));
  }
  
  /***************************************************************************
  **
  ** Return possible first zoom values
  */
  
  public static Vector<EnumChoiceContent<FirstZoom>> getFirstZoomChoices(HandlerAndManagerSource hams) {
    Vector<EnumChoiceContent<FirstZoom>> retval = new Vector<EnumChoiceContent<FirstZoom>>();
    for (FirstZoom z : FirstZoom.values()) {
      retval.add(firstZoomForCombo(hams, z));    
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get a combo box element
  */
  
  public static EnumChoiceContent<FirstZoom> firstZoomForCombo(HandlerAndManagerSource hams, FirstZoom firstZoomType) {
    return (new EnumChoiceContent<FirstZoom>(hams.getRMan().getString("displayOptions." + firstZoomType.getTag()), firstZoomType));
  }   
  
  /***************************************************************************
  **
  ** Return possible Nav zoom values
  */
  
  public static Vector<EnumChoiceContent<NavZoom>> getNavZoomChoices(HandlerAndManagerSource hams) {
    Vector<EnumChoiceContent<NavZoom>> retval = new Vector<EnumChoiceContent<NavZoom>>();
    for (NavZoom z : NavZoom.values()) {
      retval.add(navZoomForCombo(hams, z));    
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get a combo box element
  */
  
  public static EnumChoiceContent<NavZoom> navZoomForCombo(HandlerAndManagerSource hams, NavZoom navZoomType) {
    return (new EnumChoiceContent<NavZoom>(hams.getRMan().getString("displayOptions." + navZoomType.getTag()), navZoomType));
  }  
 
  /***************************************************************************
  **
  ** map the node activity
  */
  
  public static String mapNodeActivity(int nodeActivity) {
    switch (nodeActivity) {
      case NO_NODE_ACTIVITY_DISPLAY:
        return (NO_NODE_ACTIVITY_DISPLAY_TAG);      
      case NODE_ACTIVITY_COLOR:
        return (NODE_ACTIVITY_COLOR_TAG);
      case NODE_ACTIVITY_PIE:
        return (NODE_ACTIVITY_PIE_TAG);
      case NODE_ACTIVITY_BOTH:
        return (NODE_ACTIVITY_BOTH_TAG);        
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** map the node activity tag
  */
  
  public static int mapNodeActivityTag(String nodeActivityTag) { 
    if (nodeActivityTag.equalsIgnoreCase(NO_NODE_ACTIVITY_DISPLAY_TAG)) {
      return (NO_NODE_ACTIVITY_DISPLAY);
    } else if (nodeActivityTag.equalsIgnoreCase(NODE_ACTIVITY_COLOR_TAG)) {
      return (NODE_ACTIVITY_COLOR);
    } else if (nodeActivityTag.equalsIgnoreCase(NODE_ACTIVITY_PIE_TAG)) {
      return (NODE_ACTIVITY_PIE);
    } else if (nodeActivityTag.equalsIgnoreCase(NODE_ACTIVITY_BOTH_TAG)) {
      return (NODE_ACTIVITY_BOTH);      
    } else {
      throw new IllegalArgumentException();
    }  
  } 
  
  /***************************************************************************
  **
  ** Return possible node activity
  */
  
  public static Vector<ChoiceContent> getNodeActivityChoices(HandlerAndManagerSource hams) {
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    for (int i = 0; i < NUM_NODE_ACTIVITY_OPTIONS_; i++) {
      retval.add(nodeActivityForCombo(hams, i));    
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get a combo box element
  */
  
  public static ChoiceContent nodeActivityForCombo(HandlerAndManagerSource hams, int nodeActivity) {
    return (new ChoiceContent(hams.getRMan().getString("displayOptions." + mapNodeActivity(nodeActivity)), nodeActivity));
  }
  
  /***************************************************************************
  **
  ** map the link activity
  */
  
  public static String mapLinkActivity(int linkActivity) {
    switch (linkActivity) {
      case NO_LINK_ACTIVITY_DISPLAY:
        return (NO_LINK_ACTIVITY_DISPLAY_TAG);      
      case LINK_ACTIVITY_COLOR:
        return (LINK_ACTIVITY_COLOR_TAG);
      case LINK_ACTIVITY_THICK:
        return (LINK_ACTIVITY_THICK_TAG);
      case LINK_ACTIVITY_BOTH:
        return (LINK_ACTIVITY_BOTH_TAG);        
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** map the link activity tag
  */
  
  public static int mapLinkActivityTag(String linkActivityTag) { 
    if (linkActivityTag.equalsIgnoreCase(NO_LINK_ACTIVITY_DISPLAY_TAG)) {
      return (NO_NODE_ACTIVITY_DISPLAY);
    } else if (linkActivityTag.equalsIgnoreCase(LINK_ACTIVITY_COLOR_TAG)) {
      return (LINK_ACTIVITY_COLOR);
    } else if (linkActivityTag.equalsIgnoreCase(LINK_ACTIVITY_THICK_TAG)) {
      return (LINK_ACTIVITY_THICK);
    } else if (linkActivityTag.equalsIgnoreCase(LINK_ACTIVITY_BOTH_TAG)) {
      return (LINK_ACTIVITY_BOTH);      
    } else {
      throw new IllegalArgumentException();
    }  
  } 
  
  /***************************************************************************
  **
  ** Return possible link activity
  */
  
  public static Vector<ChoiceContent> getLinkActivityChoices(HandlerAndManagerSource hams) {
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    for (int i = 0; i < NUM_LINK_ACTIVITY_OPTIONS_; i++) {
      retval.add(linkActivityForCombo(hams, i));    
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get a combo box element
  */
  
  public static ChoiceContent linkActivityForCombo(HandlerAndManagerSource hams, int linkActivity) {
    return (new ChoiceContent(hams.getRMan().getString("displayOptions." + mapLinkActivity(linkActivity)), linkActivity));
  }        
  
/***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class DisplayOptionsWorker extends AbstractFactoryClient {
 
    private boolean isForAppend_;
    private DataAccessContext dacx_;
    
    public DisplayOptionsWorker(FactoryWhiteboard whiteboard, boolean isForAppend) {
      super(whiteboard);
      myKeys_.add("displayOptions");
      isForAppend_ = isForAppend;
      installWorker(new CustomEvidenceDrawStyle.CustomEvidenceDrawStyleWorker(whiteboard), new MyStyleGlue());
      installWorker(new ColumnWorker(whiteboard), new MyColumnGlue());
      installWorker(new ColorWorker(whiteboard), new MyColorGlue());
    }
  
    public void setContext(DataAccessContext dacx) {
      dacx_ = dacx;
      return; 
    }
       
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("displayOptions")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        if (isForAppend_) {   
          board.appendDisplayOptions = buildFromXML(elemName, attrs);
          retval = board.appendDisplayOptions;
        } else {
          board.displayOptions = buildFromXML(elemName, attrs);
          dacx_.getDisplayOptsSource().setDisplayOptionsForIO(board.displayOptions);
          retval = board.displayOptions;
        }     
      }
      return (retval);         
    }
    
    @Override
    public void localFinishElement(String elemName) throws IOException {
      if (isForAppend_ && elemName.equals("displayOptions")) {
        DisplayOptions existing = dacx_.getDisplayOptsSource().getDisplayOptions();
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        List<String> mm = existing.getMergeMismatches(board.appendDisplayOptions, dacx_.getRMan());
        if (!mm.isEmpty()) {
          if (board.mergeIssues == null) {
            board.mergeIssues = new ArrayList<String>();
          }  
          board.mergeIssues.addAll(mm);
        }
      }
      return;
    }
    
    private DisplayOptions buildFromXML(String elemName, Attributes attrs) throws IOException {  
      String branchStr = AttributeExtractor.extractAttribute(elemName, attrs, "displayOptions", "branches", false);
      String evidenceStr = AttributeExtractor.extractAttribute(elemName, attrs, "displayOptions", "evidence", false);
      String footStr = AttributeExtractor.extractAttribute(elemName, attrs, "displayOptions", "bigFoot", false);
      String firstZoom = AttributeExtractor.extractAttribute(elemName, attrs, "displayOptions", "firstZoom", false);
      String navZoom = AttributeExtractor.extractAttribute(elemName, attrs, "displayOptions", "navZoom", false);
      String nodeActivity = AttributeExtractor.extractAttribute(elemName, attrs, "displayOptions", "nodeActivity", false);
      String linkActivity = AttributeExtractor.extractAttribute(elemName, attrs, "displayOptions", "linkActivity", false);
      String weakLevel = AttributeExtractor.extractAttribute(elemName, attrs, "displayOptions", "weakLevel", false);
      String inactiveBright = AttributeExtractor.extractAttribute(elemName, attrs, "displayOptions", "inactiveBright", false);
      String showTreeStr = AttributeExtractor.extractAttribute(elemName, attrs, "displayOptions", "showTree", false);     
      String pertDefMin = AttributeExtractor.extractAttribute(elemName, attrs, "displayOptions", "pdMin", false);
      String pertDefMax = AttributeExtractor.extractAttribute(elemName, attrs, "displayOptions", "pdMax", false);
      String scaleTag = AttributeExtractor.extractAttribute(elemName, attrs, "displayOptions", "scale", false);
      String breakOutStr = AttributeExtractor.extractAttribute(elemName, attrs, "displayOptions", "breakInvest", false);
 
      return (new DisplayOptions(branchStr, evidenceStr, footStr, firstZoom, 
                                 navZoom, nodeActivity, linkActivity, weakLevel, 
                                 showTreeStr, pertDefMin, pertDefMax, scaleTag, breakOutStr, inactiveBright));
    }
  }
  
  public static class MyStyleGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;  
        if (board.displayOptions == null) {
          return (null);
        }
      
      DisplayOptions dopt = board.displayOptions;
      CustomEvidenceDrawStyle plds = board.evidenceDrawSty;
      dopt.setCustomEvidence(plds.getEvidenceLevel(), plds);
      return (null);
    }
  }
  
  public static class MyColumnGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      if (board.displayOptions == null) {
        return (null);
      }
      board.displayOptions.addColumn(board.column);
      return (null);
    }
  }
  
  public static class MyColorGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      if (board.displayOptions == null) {
        return (null);
      }
      board.displayOptions.addMeasurementDisplayColor(board.pertColorPair);
      return (null);
    }
  }
    
  public static class ColumnWorker extends AbstractFactoryClient {
 
    public ColumnWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("colRange");
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("colRange")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        if (board.displayOptions == null) {
          return (null);
        }
        
        
        board.column = buildFromXML(elemName, attrs);
        retval = board.column;
      }
      return (retval);     
    }  
    
    private MinMax buildFromXML(String elemName, Attributes attrs) throws IOException {
      String minStr = AttributeExtractor.extractAttribute(elemName, attrs, "colRange", "min", true);     
      String maxStr = AttributeExtractor.extractAttribute(elemName, attrs, "colRange", "max", false);
      try {
        int minVal = Integer.parseInt(minStr);
        int maxVal = minVal;
        if (maxStr != null) {
          maxVal = Integer.parseInt(maxStr);
        }
        return (new MinMax(minVal, maxVal));
      } catch (NumberFormatException ex) {
        throw new IOException();
      } 
    }
  }  
  
   public static class ColorWorker extends AbstractFactoryClient {
 
    public ColorWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("pertColor");
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("pertColor")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        
        if (board.displayOptions == null) {
          return (null);
        }
        
        board.pertColorPair = buildFromXML(elemName, attrs);
        retval = board.pertColorPair;
      }
      return (retval);     
    }  
    
    private NameValuePair buildFromXML(String elemName, Attributes attrs) throws IOException {
      String keyStr = AttributeExtractor.extractAttribute(elemName, attrs, "pertColor", "key", true);     
      String colorStr = AttributeExtractor.extractAttribute(elemName, attrs, "pertColor", "color", true);
      return (new NameValuePair(keyStr, colorStr));
    }
  }
}
