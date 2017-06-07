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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.perturb.MeasureDictionary;
import org.systemsbiology.biotapestry.util.EnumChoiceContent;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.NameValuePair;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.Indenter;

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
  
  private DataAccessContext dacx_;
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
  private ArrayList<MinMax> timeSpanCols_;
  private MinMax nullPertDefaultSpan_;
  private HashMap<String, String> pertMeasureColors_;
  private String currentScaleTag_;
  private boolean breakOutInvestigators_;
  private float[] inactiveHSV_;
  private double inactiveBright_;
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

  public DisplayOptions(DataAccessContext dacx) {
    dacx_ = dacx;
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
    timeSpanCols_ = new ArrayList<MinMax>();
    breakOutInvestigators_ = false;
    nullPertDefaultSpan_ = null;
    MeasureDictionary md = dacx_.getExpDataSrc().getPertData().getMeasureDictionary();
    currentScaleTag_ = md.getStandardScaleKeys()[MeasureDictionary.DEFAULT_INDEX];
    pertMeasureColors_ = new HashMap<String, String>();
    inactiveBright_ = DEFAULT_INACTIVE_BRIGHT_;
  }

  /***************************************************************************
  **
  ** Constructor for XML
  */

  public DisplayOptions(DataAccessContext dacx, String branches, String evidence, String footSize, 
                        String firstZoom, String navZoom, 
                        String nodeActivity, String linkActivity, 
                        String weakLevel, String dispTreeStr,
                        String pertDefMin, String pertDefMax, 
                        String scaleTag, String breakOutStr, String inactiveBright) throws IOException {
    
    dacx_ = dacx;
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
    timeSpanCols_ = new ArrayList<MinMax>();
    pertMeasureColors_ = new HashMap<String, String>();
    
    nullPertDefaultSpan_ = null;
    if ((pertDefMin != null) && (pertDefMax != null)) {
      try {
        int minTime = Integer.parseInt(pertDefMin);
        int maxTime = Integer.parseInt(pertDefMax);
        nullPertDefaultSpan_ = new MinMax(minTime, maxTime);
      } catch (NumberFormatException ex) {
        throw new IOException();
      }
    }
    
    breakOutInvestigators_ = (breakOutStr != null) ? Boolean.valueOf(breakOutStr).booleanValue() : false;
    currentScaleTag_ = scaleTag; 
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** Break out investigators in perturbation display
  */
  
  public boolean breakOutInvestigators() {    
    return (breakOutInvestigators_);
  }
  
  /***************************************************************************
  **
  ** Set whether to break out investigators in perturbation display
  */
  
  public void setBreakOutInvestigatorMode(boolean investMode) {
    breakOutInvestigators_ = investMode;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the measurement display colors
  */
  
  public Map<String, String> getMeasurementDisplayColors() {
    if (pertMeasureColors_.isEmpty()) {
      HashMap<String, String> defMap = new HashMap<String, String>();
      UiUtil.fixMePrintout("NO! If pert data is per tab, so must this be per database!");
    UiUtil.fixMePrintout("Seeing null ptr here trying to look at experimental data on non-shared tab");
       UiUtil.fixMePrintout("appState_.getDB()  fixed the problem, but we are not doing that anymore!");
      MeasureDictionary md = dacx_.getExpDataSrc().getPertData().getMeasureDictionary();
      Iterator<String> mkit = md.getKeys();
      while (mkit.hasNext()) {
        String mkey = mkit.next();
        defMap.put(mkey, "black"); 
      }
      return (defMap);
    } else {
      return (pertMeasureColors_);
    }
  }
  
  /***************************************************************************
  **
  ** Answer if we are stale following a perturbation data change
  */
  
  public Map<String, String> haveInconsistentMeasurementDisplayColors() {
    if (pertMeasureColors_.isEmpty()) {
      return (null);
    }
    UiUtil.fixMePrintout("NO! If pert data is per tab, so must this be per database!");
    MeasureDictionary md = dacx_.getMetabase().getSharedPertData().getMeasureDictionary();
    HashSet<String> currKeys = new HashSet<String>();
    Iterator<String> mkit = md.getKeys();
    while (mkit.hasNext()) {
      currKeys.add(mkit.next()); 
    }
    if (currKeys.equals(pertMeasureColors_.keySet())) {
      return (null);
    }
    //
    // We are out of whack!  Return the fix!
    //
    
    HashMap<String, String> retval = new HashMap<String, String>();
    mkit = md.getKeys();
    while (mkit.hasNext()) {
      String key = mkit.next();
      String color = pertMeasureColors_.get(key);
      if (color == null) {
        color = "black";
      }
      retval.put(key, color);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Answer if we are stale following a perturbation data change
  */
  
  public String haveInconsistentScaleTag() {
    if (currentScaleTag_ == null) {
      return (null);
    }
    UiUtil.fixMePrintout("NO! If pert data is per tab, so must this be per database!");
    MeasureDictionary md = dacx_.getMetabase().getSharedPertData().getMeasureDictionary();
    Iterator<String> mkit = md.getScaleKeys();
    while (mkit.hasNext()) {
      String key = mkit.next();
      if (key.equals(currentScaleTag_)) {
        return (null);
      }
    }
    return (md.getStandardScaleKeys()[MeasureDictionary.DEFAULT_INDEX]);
  }
  
  /***************************************************************************
  **
  ** Set the measurement display colors
  */
  
  public void setMeasurementDisplayColors(Map<String, String> colors) {
    pertMeasureColors_.clear();
    pertMeasureColors_.putAll(colors);
    return;
  }
  
  /***************************************************************************
  **
  ** Add a measurement display color (for IO)
  */
  
  public void addMeasurementDisplayColor(NameValuePair colorPair) {
    pertMeasureColors_.put(colorPair.getName(), colorPair.getValue());
    return;
  }
  
  /***************************************************************************
  **
  ** Get the perturbation data measurement display scale key
  */
  
  public String getPerturbDataDisplayScaleKey() {
    if (currentScaleTag_ == null) {
      UiUtil.fixMePrintout("NO! If pert data is per tab, so must this be per database!");
      UiUtil.fixMePrintout("Seeing null ptr here trying to look at experimental data on non-shared tab");
       UiUtil.fixMePrintout("appState_.getDB() fixed the problem, but we are now using dacx");
      MeasureDictionary md = dacx_.getExpDataSrc().getPertData().getMeasureDictionary();
      return (md.getStandardScaleKeys()[MeasureDictionary.DEFAULT_INDEX]);
    } else {
      return (currentScaleTag_);
    }
  }
  
  /***************************************************************************
  **
  ** Set the perturbation data measurement display scale key
  */
  
  public void setPerturbDataDisplayScaleKey(String scaleKey) {
    currentScaleTag_ = scaleKey;
    return;
  }
 
  /***************************************************************************
  **
  ** Get the null perturbation default span.
  */
  
  public MinMax getNullPertDefaultSpan() {
    if (nullPertDefaultSpan_ == null) {
      UiUtil.fixMePrintout("NO! If pert data is per tab, so must this be per database!");
      TimeAxisDefinition tad = dacx_.getExpDataSrc().getTimeAxisDefinition();
      return ((tad.isInitialized()) ? tad.getDefaultTimeSpan() : null);
    } else {
      return (nullPertDefaultSpan_);
    }
  }
 
  /***************************************************************************
  **
  ** Set the null perturbation default span.
  */
  
  public void setNullDefaultSpan(MinMax defaultSpan) {
    nullPertDefaultSpan_ = (defaultSpan == null) ? null : (MinMax)defaultSpan.clone();
    return;
  }
 
  /***************************************************************************
  **
  ** Drop column definitions and other data-specific options
  **
  */
  
  public void dropDataBasedOptions() {
    pertMeasureColors_.clear();
    timeSpanCols_.clear();
    nullPertDefaultSpan_ = null;
    currentScaleTag_ = null;
    return;
  }
 
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
  ** Set the column headings
  */
  
  public void setColumns(ArrayList<MinMax> columns) {
    timeSpanCols_.clear();
    timeSpanCols_.addAll(columns);
    return;
  }

  /***************************************************************************
  **
  ** Set a column heading
  */
  
  public void addColumn(MinMax col) {
    timeSpanCols_.add(col);
    return;
  }
  
  /***************************************************************************
  **
  ** Get the column headings iterator
  */
  
  public Iterator<MinMax> getColumnIterator() {
    return (timeSpanCols_.iterator());
  }  
  
  /***************************************************************************
  **
  ** Answer if we have column headings
  */
  
  public boolean hasColumns() {
    return (timeSpanCols_.size() > 0);
  } 
  
  /***************************************************************************
  **
  ** Answer if we have a default time span
  */
  
  public boolean hasDefaultTimeSpan() {
    return (nullPertDefaultSpan_ != null);
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
      
      retval.timeSpanCols_ = new ArrayList<MinMax>();
      Iterator<MinMax> cit = this.timeSpanCols_.iterator();
      while (cit.hasNext()) {
        MinMax mm = cit.next();
        retval.timeSpanCols_.add(mm.clone());
      } 
      
      retval.nullPertDefaultSpan_ = (this.nullPertDefaultSpan_ == null) ? null : (MinMax)this.nullPertDefaultSpan_.clone();  
      retval.pertMeasureColors_ = new HashMap<String, String>(this.pertMeasureColors_);
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
       
    if (breakOutInvestigators_) {
      out.print("breakInvest=\"true\" ");
    }
    
    if (currentScaleTag_ != null) {
      out.print("scale=\"");
      out.print(currentScaleTag_);
      out.print("\" ");
    }
    
    if (nullPertDefaultSpan_ != null) {
      out.print("pdMin=\"");
      out.print(nullPertDefaultSpan_.min);
      out.print("\" pdMax=\"");
      out.print(nullPertDefaultSpan_.max);
      out.print("\" ");
    }
    
    if ((renderEvidence_ == EVIDENCE_IS_CUSTOM) || hasColumns() || !pertMeasureColors_.isEmpty()) {
      out.println(">");
      ind.up();    
      if (hasColumns()) {
        ind.indent();
        out.println("<colRanges>");
        Iterator<MinMax> cit = timeSpanCols_.iterator();
        ind.up();    
        while (cit.hasNext()) {
          MinMax tc = cit.next();
          ind.indent();
          out.print("<colRange min=\"");
          out.print(tc.min);
          if (tc.min != tc.max) {
            out.print("\" max=\"");
            out.print(tc.max);
          }
          out.println("\" />");
        }
        ind.down().indent(); 
        out.println("</colRanges>");
      }
      if (renderEvidence_ == EVIDENCE_IS_CUSTOM) {
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
      }
      if (!pertMeasureColors_.isEmpty()) {
        ind.indent();
        out.println("<pertColors>");
        TreeSet<String> sorted = new TreeSet<String>(pertMeasureColors_.keySet());
        Iterator<String> pcit = sorted.iterator();
        ind.up();    
        while (pcit.hasNext()) {
          String key = pcit.next();
          ind.indent();
          out.print("<pertColor key=\"");
          out.print(key);
          out.print("\" color=\"");
          out.print(pertMeasureColors_.get(key));
          out.println("\" />");
        }
        ind.down().indent(); 
        out.println("</pertColors>");
      }
      ind.down().indent();
      out.println("</displayOptions>");
    } else {
      out.println("/>");
    }
   
    return;
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

  public static Vector<ChoiceContent> branchOptions(DataAccessContext dacx) {
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    for (int i = 0; i < NUM_BUS_BRANCH_OPTIONS_; i++) {
      retval.add(mapBranchOptions(dacx, i));
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Useful for JComboBoxes
  */

  public static ChoiceContent mapBranchOptions(DataAccessContext dacx, int val) {
    return (new ChoiceContent(dacx.getRMan().getString("displayOptions." + mapBranches(val)), val));
  }
  
  /***************************************************************************
  **
  ** Useful for JComboBoxes
  */

  public static Vector<ChoiceContent> evidenceOptions(DataAccessContext dacx) {
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    for (int i = 0; i < NUM_EVIDENCE_OPTIONS_; i++) {
      retval.add(mapEvidenceOptions(dacx, i));
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Useful for JComboBoxes
  */

  public static ChoiceContent mapEvidenceOptions(DataAccessContext dacx, int val) {
    return (new ChoiceContent(dacx.getRMan().getString("displayOptions." + mapEvidence(val)), val));
  }
  
  /***************************************************************************
  **
  ** Return possible first zoom values
  */
  
  public static Vector<EnumChoiceContent<FirstZoom>> getFirstZoomChoices(DataAccessContext dacx) {
    Vector<EnumChoiceContent<FirstZoom>> retval = new Vector<EnumChoiceContent<FirstZoom>>();
    for (FirstZoom z : FirstZoom.values()) {
      retval.add(firstZoomForCombo(dacx, z));    
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get a combo box element
  */
  
  public static EnumChoiceContent<FirstZoom> firstZoomForCombo(DataAccessContext dacx, FirstZoom firstZoomType) {
    return (new EnumChoiceContent<FirstZoom>(dacx.getRMan().getString("displayOptions." + firstZoomType.getTag()), firstZoomType));
  }   
  
  /***************************************************************************
  **
  ** Return possible Nav zoom values
  */
  
  public static Vector<EnumChoiceContent<NavZoom>> getNavZoomChoices(DataAccessContext dacx) {
    Vector<EnumChoiceContent<NavZoom>> retval = new Vector<EnumChoiceContent<NavZoom>>();
    for (NavZoom z : NavZoom.values()) {
      retval.add(navZoomForCombo(dacx, z));    
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get a combo box element
  */
  
  public static EnumChoiceContent<NavZoom> navZoomForCombo(DataAccessContext dacx, NavZoom navZoomType) {
    return (new EnumChoiceContent<NavZoom>(dacx.getRMan().getString("displayOptions." + navZoomType.getTag()), navZoomType));
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
  
  public static Vector<ChoiceContent> getNodeActivityChoices(DataAccessContext dacx) {
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    for (int i = 0; i < NUM_NODE_ACTIVITY_OPTIONS_; i++) {
      retval.add(nodeActivityForCombo(dacx, i));    
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get a combo box element
  */
  
  public static ChoiceContent nodeActivityForCombo(DataAccessContext dacx, int nodeActivity) {
    return (new ChoiceContent(dacx.getRMan().getString("displayOptions." + mapNodeActivity(nodeActivity)), nodeActivity));
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
  
  public static Vector<ChoiceContent> getLinkActivityChoices(DataAccessContext dacx) {
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    for (int i = 0; i < NUM_LINK_ACTIVITY_OPTIONS_; i++) {
      retval.add(linkActivityForCombo(dacx, i));    
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get a combo box element
  */
  
  public static ChoiceContent linkActivityForCombo(DataAccessContext dacx, int linkActivity) {
    return (new ChoiceContent(dacx.getRMan().getString("displayOptions." + mapLinkActivity(linkActivity)), linkActivity));
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
      UiUtil.fixMePrintout("Display options need append/merge abilities!");
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
      UiUtil.fixMePrintout("Ignoring appended display options!");
      if (isForAppend_) {
        return (null);
      }
      
      Object retval = null;
      if (elemName.equals("displayOptions")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.displayOptions = buildFromXML(elemName, attrs);
        dacx_.getDisplayOptsSource().setDisplayOptionsForIO(board.displayOptions);
        retval = board.displayOptions;
      }
      return (retval);     
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
 
      return (new DisplayOptions(dacx_, branchStr, evidenceStr, footStr, firstZoom, 
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
