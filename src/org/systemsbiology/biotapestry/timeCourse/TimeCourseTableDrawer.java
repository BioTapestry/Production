/*
**    Copyright (C) 2003-2016 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.timeCourse;

import java.awt.Color;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Comparator;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.perturb.PertProperties;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** This holds time course data for a target gene
*/

public class TimeCourseTableDrawer {
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  // Key values
  
  public static final int NO_TABLE_KEY        = 0x00;  
  public static final int BASIC_TABLE_KEY     = 0x01;
  public static final int ADD_CONFIDENCE      = 0x02;
  public static final int ADD_SOURCE          = 0x04;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private float[] inactiveHSV_;
  private float[] activeHSV_;
  private float[] activeRegionHSV_;
  private float[] colHSB_;
  private TimeCourseGene client_;
  private PerturbedTimeCourseGene pertClient_;
  private BTState appState_;
  
  private static final String BIG_VERT_COLOR_  = "#F8F8F8";  
  private static final String NO_REGION_COLOR_ = "#DDDDDD";
  private static final String BLACK_COLOR_     = "black";
  private static final String WHITE_COLOR_     = "white";
  private static final String MID_GREY_COLOR   = "#888888";

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public TimeCourseTableDrawer(BTState appState, TimeCourseGene client) {
    this(appState, client, null);
  }  
    
  /***************************************************************************
  **
  ** Constructor
  */

  public TimeCourseTableDrawer(BTState appState, TimeCourseGene client, PerturbedTimeCourseGene pertClient) {
    appState_ = appState;
    client_ = client;
    pertClient_ = pertClient;
    inactiveHSV_ = new float[3];
    Color.RGBtoHSB(0xFF, 0xFF, 0xFF, inactiveHSV_); 
    activeHSV_ = new float[3];
    Color.RGBtoHSB(0x66, 0xEE, 0x66, activeHSV_);
    activeRegionHSV_ = new float[3];
    Color.RGBtoHSB(0x66, 0x66, 0xEE, activeRegionHSV_);
    colHSB_ = new float[3];
  }  
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // CLIENT INTERFACE
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Clients implement to use the class
  */

  public interface Client {
    public boolean isInternalOnly();
    public void getRegions(Set<String> set);
    public void getInterestingTimes(Set<Integer> set);
    public String getName();
    public String getTimeCourseNote();
    public int getExpressionLevelForSource(String region, int time, int exprSource, TimeCourseGene.VariableLevel varLev);
    public int getConfidence(String region, int time);
    public int getExprSource(String region, int time);
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get an HTML expression table suitable for display.
  */
  
  public int getPertExpressionTable(PrintWriter out, TimeCourseData tcd) {
      
    if (pertClient_.isInternalOnly() || client_.isInternalOnly()) {
      return (NO_TABLE_KEY);
    }
    
    HashSet<String> rawregions = new HashSet<String>();
    client_.getRegions(rawregions);  
    List<String> ordered = tcd.getRegionsKeepOrder();    
    List<RegionData> regions = sortRegions(rawregions, ordered);
        
    TreeSet<Integer> times = new TreeSet<Integer>();
    HashSet<Integer> rawtimes = new HashSet<Integer>();
    client_.getInterestingTimes(rawtimes);
    times.addAll(rawtimes);
    
    ResourceManager rMan = appState_.getRMan();
    String format = rMan.getString("timeCourseDrawer.perTablePertTitleFormat");
    out.print("<center><h2>"); 
    out.print(MessageFormat.format(format, new Object[] {client_.getName(), pertClient_.getName()})); 
    out.print("</h2></center>\n");    
    
    // On Java 5 & 6, 3-pix columns not rendering unless we force the table to
    // be big enough to render:
    int num = 130 + 2 + (times.size() * (70 + 3 + 10)); 
    out.print("<table width=\"");
    out.print(num);
    out.println("\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\">");
    out.println("<tr>");
    
    List<RegionData> reducedReg = new ArrayList<RegionData>();
    HashSet<String> usedRegions = new HashSet<String>();
    int numRegions1 = regions.size();
    for (int i = 0; i < numRegions1; i++) {
      RegionData reg = regions.get(i);
      Iterator<Integer> tmit = times.iterator();
      while (tmit.hasNext()) {
        Integer timeObj = tmit.next();
        TimeCourseGene.VariableLevel varLev = new TimeCourseGene.VariableLevel();
        int level = pertClient_.getExpressionLevelForSource(reg.region, timeObj.intValue(), 
                                                            ExpressionEntry.NO_SOURCE_SPECIFIED, varLev);
        if ((level == ExpressionEntry.NO_DATA) || (level == ExpressionEntry.NO_REGION)) {
          continue;
        }
        usedRegions.add(reg.region);
        reducedReg.add(reg);
        break;
      }
    }    
    
    //
    // Region Labels
    //
    
    out.println("<td valign=\"center\">");
    out.print("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\">");
    pertHoriz(out, 3, WHITE_COLOR_);
    out.println("<tr>");
    pertVert(out, WHITE_COLOR_);
    out.print("<td width=\"130\" align=\"center\" valign=\"center\"><b>"); 
    out.print(" ");
    out.println("</b></td>");
    pertVert(out, WHITE_COLOR_);
    out.println("</tr>");
    pertHoriz(out, 3, WHITE_COLOR_);
    out.println("<tr>");
    pertVert(out, WHITE_COLOR_);
    out.print("<td width=\"130\" align=\"center\" valign=\"center\"><b>");
    out.print(" ");
    out.println("</b></td>");
    pertVert(out, WHITE_COLOR_);
    out.println("</tr>");
    pertHoriz(out, 3, MID_GREY_COLOR);
    // Regions
    int numRegions = regions.size();
    for (int i = 0; i < numRegions; i++) {
      RegionData reg = regions.get(i);
      if (!usedRegions.contains(reg.region)) {
        continue;
      }
      out.println("<tr>");
      pertVert(out, BLACK_COLOR_);
      out.print("<td width=\"130\" height=\"40\" align=\"center\" valign=\"center\"><b>");
      out.print(reg.region);
      out.println("</b></td>");
      pertVert(out, BLACK_COLOR_);
      out.println("</tr>");
      pertHoriz(out, 3, MID_GREY_COLOR);
    }
    out.println("</table>");
    out.println("</td>");
    pertBigVert(out, BIG_VERT_COLOR_);

    // Heading
    Iterator<Integer> tmit = times.iterator();
    while (tmit.hasNext()) {
      Integer timeObj = tmit.next();
      out.println("<td valign=\"center\">");
      out.print("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\">");
      pertHoriz(out, 5, MID_GREY_COLOR);
      // Time heading
      out.println("<tr>");
      pertVert(out, BLACK_COLOR_);
      out.print("<td width=\"70\" align=\"center\" valign=\"center\" colspan=\"3\"><b>");
      String timeStr = TimeAxisDefinition.getTimeDisplay(appState_, timeObj, true, false);      
      out.print(timeStr);
      out.println("</b></td>");
      pertVert(out, BLACK_COLOR_);
      out.println("</tr>");
      pertHoriz(out, 5, MID_GREY_COLOR);
      // Pert/WT
      out.println("<tr>");
      pertVert(out, BLACK_COLOR_);
      out.print("<td width=\"35\" align=\"center\" valign=\"center\"><b>");
      out.print(rMan.getString("timeCourseTable.wildType"));
      out.println("</b></td>");
      pertVert(out, BLACK_COLOR_);
      out.print("<td width=\"35\" align=\"center\" valign=\"center\"><b>");
      out.print(rMan.getString("timeCourseTable.pertType"));
      out.println("</b></td>");
      pertVert(out, BLACK_COLOR_);
      out.println("</tr>");
      pertHoriz(out, 5, MID_GREY_COLOR);
      // Regions
      for (int i = 0; i < numRegions; i++) {
        RegionData reg = regions.get(i);
        if (!usedRegions.contains(reg.region)) {
          continue;
        }
        out.println("<tr>");
        buildPertCell(out, reg.region, timeObj.intValue());
        out.println("</tr>");
        pertHoriz(out, 5, MID_GREY_COLOR);
      }
      out.println("</table>");
      out.println("</td>");
      if (tmit.hasNext()) {
        pertBigVert(out, BIG_VERT_COLOR_);
      }
    }
    out.println("</tr>");
    out.println("</table>");
    
    //
    // Notes
    //
    
    String timeCourseNote = pertClient_.getTimeCourseNote();
    if ((timeCourseNote != null) && !timeCourseNote.trim().equals("")) {
      out.println("<p>");
      out.print("<b>");
      out.print(rMan.getString("timeCourseTable.note"));
      out.print(":</b> ");
      out.println(CharacterEntityMapper.mapEntities(timeCourseNote, false));
      out.println("</p>");    
    }
       
    int clientKey = needKey(reducedReg, times, client_);
    int pertKey = needKey(reducedReg, times, pertClient_);
    int retval = Math.max(clientKey, pertKey);
    return (retval);
  }

  /***************************************************************************
  **
  ** Get an HTML expression table suitable for display.
  */
  
  public int getExpressionTable(PrintWriter out, TimeCourseData tcd, boolean showTree) {
     
    if (client_.isInternalOnly()) {
      return (NO_TABLE_KEY);
    }
   
    List<RegionData> regions;
    TreeSet<Integer> times;
    
    ResourceManager rMan = appState_.getRMan();
    String format = rMan.getString("timeCourseDrawer.perTableTitleFormat");
    out.print("<center><h2>"); 
    out.print(MessageFormat.format(format, new Object[] {client_.getName()})); 
    out.print("</h2></center>\n");
        
    if (showTree) {
      TreeModel rawHierTree = tcd.getRegionHierarchyTree();
      regions = new ArrayList<RegionData>();
      times = new TreeSet<Integer>();
      drawTableWithLineage(out, rawHierTree, regions, times);
    } else {
      HashSet<String> rawregions = new HashSet<String>();
      client_.getRegions(rawregions);  
      List<String> ordered = (tcd.hierarchyIsSet()) ? tcd.getRegionHierarchyList() : tcd.getRegionsKeepOrder();    
      regions = sortRegions(rawregions, ordered);

      HashSet<Integer> rawtimes = new HashSet<Integer>();
      client_.getInterestingTimes(rawtimes);
      times = new TreeSet<Integer>();
      times.addAll(rawtimes);
      
      int num = 130 + (times.size() * 70); 
      
      out.print("<table width=\"");
      out.print(num);
      out.println("\" border=\"1\" cellpadding=\"0\" cellspacing=\"0\">");
      
      // Heading
      out.println("<tr>"); 
      out.println("<td width=\"130\" align=\"center\" valign=\"center\">");
      out.println(client_.getName());
      out.println("</td>");
      
      Iterator<Integer> tmit = times.iterator();
      while (tmit.hasNext()) {
        Integer timeObj = tmit.next();
        out.print("<td width=\"70\" align=\"center\" valign=\"center\"><b>");
        String timeStr = TimeAxisDefinition.getTimeDisplay(appState_, timeObj, true, false);      
        out.print(timeStr);
        out.println("</b></td>");
      }
      out.println("</tr>");
      
      // data
      Iterator<RegionData> regIt = regions.iterator();
      while (regIt.hasNext()) {
        RegionData reg = regIt.next();
        out.println("<tr>");
        out.println("<td width=\"130\" align=\"center\" valign=\"center\"><b>");
        out.println(reg.region);
        out.println("</b></td>");
        tmit = times.iterator();
        while (tmit.hasNext()) {
          Integer hour = tmit.next();
          buildCell(out, reg.region, hour.intValue(), client_);
        }
        out.println("</tr>");
      }
      out.println("</table>");
    }
    
    //
    // Notes
    //
    
    String timeCourseNote = client_.getTimeCourseNote();
    if ((timeCourseNote != null) && !timeCourseNote.trim().equals("")) {
      out.println("<p>");
      out.print("<b>");
      out.print(rMan.getString("timeCourseTable.note"));
      out.print(":</b> ");
      out.println(CharacterEntityMapper.mapEntities(timeCourseNote, false));
      out.println("</p>");    
    }
    
    out.println("<p></p>");     
    return (needKey(regions, times, client_));
  }
  
  /***************************************************************************
  **
  ** Do key building stuff
  */
  
  public static String buildKey(BTState appState, int needKey, boolean showTree, boolean showWTPert) {
    StringWriter sw = new StringWriter();
    PrintWriter out = new PrintWriter(sw);
    if ((needKey & TimeCourseTableDrawer.BASIC_TABLE_KEY) != 0x00) {
      if (showTree) {
        out.print("<p><center>");
        out.print(appState.getRMan().getString("timeCourseTable.cellEdgeNote"));
        out.println("</center></p><p></p>");
      }
      if (showWTPert) {
        TimeCourseTableDrawer.buildWTPertNote(appState, out);
      }
      buildKey(out);
    }
    boolean didC = false;
    if ((needKey & TimeCourseTableDrawer.ADD_CONFIDENCE) != 0x00) {
      buildConfidenceKey(out);
      didC = true;
    }
    if ((needKey & TimeCourseTableDrawer.ADD_SOURCE) != 0x00) {
      if (didC) {
        out.println("<br>");      
      }
      buildSourceKey(out);
    }
    return (sw.getBuffer().toString());
  }
   
  /***************************************************************************
  **
  ** Answer if we need to show the confidence key
  */
  
  private int needKey(List<RegionData> regions, SortedSet<Integer> times, Client client) {
    int retval = BASIC_TABLE_KEY;
    int numRegions = regions.size();
    TimeCourseGene.VariableLevel varLev = new TimeCourseGene.VariableLevel();
    for (int i = 0; i < numRegions; i++) {
      RegionData reg = regions.get(i);
      Iterator<Integer> tmit = times.iterator();
      while (tmit.hasNext()) {
        Integer hourObj = tmit.next();
        int hour = hourObj.intValue();
        int level = client.getExpressionLevelForSource(reg.region, hour, ExpressionEntry.NO_SOURCE_SPECIFIED, varLev);
        if ((level == ExpressionEntry.NO_REGION) || (level == ExpressionEntry.NO_DATA)) {
          continue;
        }
        int source = client.getExprSource(reg.region, hour);
        if (source != ExpressionEntry.NO_SOURCE_SPECIFIED) {
          retval |= ADD_SOURCE;
        }       
        int confidence = client.getConfidence(reg.region, hour);
        if (confidence != TimeCourseGene.NORMAL_CONFIDENCE) {
          retval |= ADD_CONFIDENCE;
        }
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get an HTML expression table suitable for display.
  */
  
  private void drawTableWithLineage(PrintWriter out, TreeModel rawHierTree, List<RegionData> regions, TreeSet<Integer> times) {
  
    DefaultMutableTreeNode rawRootNode = (DefaultMutableTreeNode)rawHierTree.getRoot();
    DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
    DefaultTreeModel hierTree = new DefaultTreeModel(rootNode); 
    stockRegionTree(rawHierTree, rawRootNode, hierTree, rootNode, regions);
     
    HashSet<Integer> rawtimes = new HashSet<Integer>();
    client_.getInterestingTimes(rawtimes);
    times.addAll(rawtimes);
     
    // On Java 5 & 6, 3-pix columns not rendering unless we force the table to
    // be big enough to render:
    int num = 130 + (times.size() * (70 + 3)) + 6; 
    out.print("<table width=\"");
    out.print(num);
    out.println("\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\">");  
    // Heading
    int numTimes = times.size();
    
    horiz(out, numTimes + 1, 0, null);
    out.println("<tr>");
    vert(out, null);
    out.println("<td width=\"130\" align=\"center\" valign=\"center\">");
    out.println(client_.getName());
    out.println("</td>");
    vert(out, null);
    Iterator<Integer> tmit = times.iterator();
    while (tmit.hasNext()) {
      Integer timeObj = tmit.next();
      out.println("<td width=\"70\" align=\"center\" valign=\"center\"><b>");
      String timeStr = TimeAxisDefinition.getTimeDisplay(appState_, timeObj, true, false);      
      out.print(timeStr);
      out.println("</b></td>");
      vert(out, null);
    }
    out.println("</tr>");
    
    // data
    int numRegions = regions.size();
    TreeInTable trit = new TreeInTable(appState_, hierTree, times, numRegions);
   
    for (int i = 0; i < numRegions; i++) {
      RegionData reg = regions.get(i);
      RowColorSpec rcs = trit.getTopColor(i);
      horiz(out, numTimes + 1, (rcs == null) ? 0 : rcs.startCol, (rcs == null) ? null : rcs.color);
      out.println("<tr>");
      vert(out, null);
      out.println("<td width=\"130\" align=\"center\" valign=\"center\"><b>");
      out.println(reg.region);
      out.println("</b></td>");
      int currTime = 0;
      Map<Integer, String> colCols = trit.getColumnColors(i);
      String col = colCols.get(new Integer(-1));  // may be null
      vert(out, col);
      tmit = times.iterator();
      while (tmit.hasNext()) {
        Integer hour = tmit.next();
        buildCell(out, reg.region, hour.intValue(), client_);
        col = colCols.get(new Integer(currTime++));  // may be null
        vert(out, col);
      }
      out.println("</tr>");
    }
    horiz(out, numTimes + 1, 0, null);
    out.println("</table>"); 
    
    return;
  }
  
  /***************************************************************************
  **
  ** Write a table row boundary
  **
  */
  
  private void horiz(PrintWriter out, int columns, int colorStartCol, String color) {
    int rawColumns = columns + ((columns + 1) * 3);
    int rawColorStart = 3 + (colorStartCol * 4) + 1;
    rawHoriz(out, rawColumns, rawColorStart, color);
    return;
  }  
 
  /***************************************************************************
  **
  ** Write a table row boundary
  **
  */
  
  private void rawHoriz(PrintWriter out, int colspan, int colorStart, String color) {
    String spacer = "<spacer type=\"block\" height=\"1\" width=\"1\">";
    int colorSpan = (color == null) ? 0 : colspan - colorStart;
    int frontSpan = (color == null) ? colspan : colspan - colorSpan;
    
    out.print("<tr>");
    if (frontSpan != 0) {
      out.print("<td colspan=\"");
      out.print(frontSpan);
      out.print("\" bgcolor=\"#888888\" ");
      out.print("height=\"1\">");
      out.print(spacer);
      out.print("</td>");
    }
    if (color != null) {
      out.print("<td colspan=\"");
      out.print(colorSpan);
      out.print("\" bgcolor=\"");
      out.print(color);
      out.print("\" height=\"1\">");
      out.print(spacer);
      out.print("</td>");
    }
    out.println("</tr>");
    
    out.print("<tr>");
    if (frontSpan != 0) {
      out.print("<td colspan=\"");
      out.print(frontSpan);
      out.print("\" bgcolor=\"white\" ");
      out.print("height=\"1\">");
      out.print(spacer);
      out.print("</td>");
    }
    if (color != null) {
      out.print("<td colspan=\"");
      out.print(colorSpan);
      out.print("\" bgcolor=\"");
      out.print(color);
      out.print("\" height=\"1\">");
      out.print(spacer);
      out.print("</td>");
    }
    out.println("</tr>");
    
    out.print("<tr>");
    if (frontSpan != 0) {
      out.print("<td colspan=\"");
      out.print(frontSpan);
      out.print("\" bgcolor=\"black\" ");
      out.print("height=\"1\">");
      out.print(spacer);
      out.print("</td>");
    }
    if (color != null) {
      out.print("<td colspan=\"");
      out.print(colorSpan);
      out.print("\" bgcolor=\"");
      out.print(color);
      out.print("\" height=\"1\">");
      out.print(spacer);
      out.print("</td>");
    }
    out.println("</tr>");
    
    return;
  }
  
  /***************************************************************************
  **
  ** Write a table column boundary
  **
  */
  
  private void vert(PrintWriter out, String color) {
    
    if (color == null) {
      out.println("<td bgcolor=\"black\" width=\"1\"></td>");
      out.println("<td bgcolor=\"white\" width=\"1\"></td>");
      out.println("<td bgcolor=\"black\" width=\"1\"></td>");
    } else {
      out.print("<td bgcolor=\"");
      out.print(color);
      out.println("\" width=\"1\"></td>");
      
      out.print("<td bgcolor=\"");
      out.print(color);
      out.println("\" width=\"1\"></td>");
      
      out.print("<td bgcolor=\"");
      out.print(color);
      out.println("\" width=\"1\"></td>");
    }
  
    return;
  }
   
  /***************************************************************************
  **
  ** Write a table column boundary
  **
  */
  
  private void pertVert(PrintWriter out, String color) {
    out.print("<td bgcolor=\"");
    out.print(color);
    out.println("\" width=\"1\"></td>");
    return;
  }
  
  /***************************************************************************
  **
  ** Write a table column boundary
  **
  */
  
  private void pertBigVert(PrintWriter out, String color) {
    out.print("<td bgcolor=\"");
    out.print(color);
    out.println("\" width=\"10\"></td>");
    return;
  }
 
  /***************************************************************************
  **
  ** Write a table row boundary
  **
  */
  
  private void pertHoriz(PrintWriter out, int span, String color) {
    String spacer = "<spacer type=\"block\" height=\"1\" width=\"1\">";
    out.print("<tr>");
    out.print("<td colspan=\"");
    out.print(span);
    out.print("\" bgcolor=\"");
    out.print(color);
    out.print("\" height=\"1\">");
    out.print(spacer);
    out.print("</td>");
    out.println("</tr>");
    return;
  }

  /***************************************************************************
  **
  ** Build a table cell for expression data
  **
  */
  
  private void buildCell(PrintWriter out, String reg, int hour, Client confClient) {
    TimeCourseGene.VariableLevel varLev = new TimeCourseGene.VariableLevel();
    int level = client_.getExpressionLevelForSource(reg, hour, ExpressionEntry.NO_SOURCE_SPECIFIED, varLev);
    if ((level == ExpressionEntry.NO_DATA) || (level == ExpressionEntry.VARIABLE)) {
      out.print("<td width=\"70\" align=\"center\" valign=\"center\" ");
    } else {
      out.print("<td width=\"70\" align=\"right\" valign=\"bottom\" ");
    }
    buildCellGuts(out, reg, hour, level, varLev, confClient);
    out.println("</td>");        
    return;
  }
  
  /***************************************************************************
  **
  ** Build table cell guts for expression data
  **
  */
  
  private void buildCellGuts(PrintWriter out, String reg, int hour, int level, 
                             TimeCourseGene.VariableLevel varLev, Client confClient) {
    switch (level) {
      case ExpressionEntry.NO_REGION:
        out.println("bgcolor=\"#DDDDDD\">");        
        out.println("<br>");
        break;
      case ExpressionEntry.NO_DATA:
        out.println("bgcolor=\"#FFFFFF\">");
        out.println("-");
        break;        
      case ExpressionEntry.NOT_EXPRESSED:
        out.println("bgcolor=\"#FFFFFF\">");
        buildConfidenceAndSource(out, reg, hour, null, confClient);
        break;
      case ExpressionEntry.WEAK_EXPRESSION:
        out.println("bgcolor=\"#CCFFCC\">");
        buildConfidenceAndSource(out, reg, hour, null, confClient);
        break;
      case ExpressionEntry.EXPRESSED:
        out.println("bgcolor=\"#66EE66\">");
        buildConfidenceAndSource(out, reg, hour, null, confClient);
        break;
      case ExpressionEntry.VARIABLE:
        String col = variableBlockColor(varLev.level, false);
        out.print("bgcolor=\"#");
        out.print(col.substring(2));
        out.println("\">");
        buildConfidenceAndSource(out, reg, hour, varLev, confClient);
        break;   
      default:
        throw new IllegalArgumentException();
    } 
    return;
  }

  /***************************************************************************
  **
  ** Build a table cell for perturbed expression data
  **
  */
  
  private void buildPertCell(PrintWriter out, String reg, int hour) {
    pertVert(out, BLACK_COLOR_);
    TimeCourseGene.VariableLevel varLev = new TimeCourseGene.VariableLevel();
    
    int level;
    boolean controlFromPert = pertClient_.usingDistinctControlExpr();
    if (controlFromPert) {
      level = pertClient_.getControlExpressionLevelForSource(reg, hour, ExpressionEntry.NO_SOURCE_SPECIFIED, varLev);
    } else {
      level = client_.getExpressionLevelForSource(reg, hour, ExpressionEntry.NO_SOURCE_SPECIFIED, varLev);
    }
    if ((level == ExpressionEntry.NO_DATA) || (level == ExpressionEntry.VARIABLE)) {
      out.print("<td width=\"35\" height=\"40\" align=\"center\" valign=\"center\" ");
    } else {
      out.print("<td width=\"35\" height=\"40\" align=\"right\" valign=\"bottom\" ");
    }
    buildCellGuts(out, reg, hour, level, varLev, (controlFromPert) ? (Client)pertClient_ : (Client)client_);
    out.println("</td>");
    
    pertVert(out, (level == ExpressionEntry.NO_REGION) ? NO_REGION_COLOR_ : BLACK_COLOR_);
    TimeCourseGene.VariableLevel pertVarLev = new TimeCourseGene.VariableLevel();
    int pertLevel = pertClient_.getExpressionLevelForSource(reg, hour, ExpressionEntry.NO_SOURCE_SPECIFIED, pertVarLev);
    if ((pertLevel == ExpressionEntry.NO_DATA) || (pertLevel == ExpressionEntry.VARIABLE)) {
      out.print("<td width=\"35\" height=\"40\" align=\"center\" valign=\"center\" ");
    } else {
      out.print("<td width=\"35\" height=\"40\" align=\"right\" valign=\"bottom\" ");
    }
    buildCellGuts(out, reg, hour, pertLevel, pertVarLev, pertClient_);
    out.println("</td>");
    pertVert(out, BLACK_COLOR_);
    return;
  }

 /***************************************************************************
  **
  ** Build a table cell for expression data
  **
  */
  
  private String variableBlockColor(double level, boolean forRegion) {  
    // Hue stays the same:
    float[] whichActive = (forRegion) ? activeRegionHSV_ : activeHSV_;
    colHSB_[0] = whichActive[0];
    colHSB_[1] = inactiveHSV_[1] + ((float)level * (whichActive[1] - inactiveHSV_[1])); 
    colHSB_[2] = inactiveHSV_[2] + ((float)level * (whichActive[2] - inactiveHSV_[2]));         
    int varCol = Color.HSBtoRGB(colHSB_[0], colHSB_[1], colHSB_[2]);
    return (Integer.toHexString(varCol));
  }
 
  /***************************************************************************
  **
  ** Build a table cell for expression data
  **
  */
  
  @SuppressWarnings("unused")
  private void buildCellWithQPCR(PrintWriter out, String reg, Set<PerturbationData.SourceInfo> sources, int hour) {
    TimeCourseGene.VariableLevel varLev = new TimeCourseGene.VariableLevel();
    int level = client_.getExpressionLevelForSource(reg, hour, ExpressionEntry.NO_SOURCE_SPECIFIED, varLev);
    out.println("<td WIDTH=\"70\" "); 
    switch (level) {
      case ExpressionEntry.NO_REGION:
        out.println("BGCOLOR=\"#DDDDDD\">");        
        buildQPCRList(out, sources);
        break;
      case ExpressionEntry.NO_DATA:
        out.println("BGCOLOR=\"#FFFFFF\">");
        out.println("---");
        buildQPCRList(out, sources);        
        break;        
      case ExpressionEntry.NOT_EXPRESSED:
        out.println("BGCOLOR=\"#FFFFFF\">");
        buildQPCRList(out, sources);
        break;
      case ExpressionEntry.WEAK_EXPRESSION:
        out.println("BGCOLOR=\"#CCCCFF\">");
        buildQPCRList(out, sources);
        break;
      case ExpressionEntry.EXPRESSED:
        out.println("BGCOLOR=\"#6666EE\">");
        buildQPCRList(out, sources);
        break;
      case ExpressionEntry.VARIABLE:
        String col = variableBlockColor(varLev.level, true);
        out.print("bgcolor=\"#");
        out.print(col.substring(2));
        out.println("\">");
        buildQPCRList(out, sources);
        break;          
      default:
        throw new IllegalArgumentException();
    }
    out.println("</td>");        
    return;
  }  

  /***************************************************************************
  **
  ** Output confidence and source (and value for variable levels!)
  **
  */

  private void buildConfidenceAndSource(PrintWriter out, String reg, int hour, 
                                        TimeCourseGene.VariableLevel varLev, Client csClient) {
    if (varLev == null) {
      buildFixedConfidenceAndSource(out, reg, hour, csClient);
      return;
    }
    out.println(varLev.level);
    int confidence = csClient.getConfidence(reg, hour);
    int source = csClient.getExprSource(reg, hour);
    String cStr = confTag(confidence);
    String sStr = sourceTag(source);
    if (sStr == null) {
      if (cStr == null) {
        return;
      } else if (cStr.equals("<br>")) {
        out.println("<br>");
        return;
      }
    }
    if ((cStr != null) && cStr.equals("<br>")) {
      cStr = null;
    }

    out.print("<sub>");
    if (cStr != null) {
      out.print(cStr);
      if (sStr != null) {
        out.print("; ");
      }
    }
    if (sStr != null) {
      out.print(sStr);
    }
    out.println("</sub>");
    return;
  }  

  /***************************************************************************
  **
  ** Output confidence and source tag for fixed vals
  **
  */

  private void buildFixedConfidenceAndSource(PrintWriter out, String reg, int hour, Client csClient) {
    int confidence = csClient.getConfidence(reg, hour);
    int source = csClient.getExprSource(reg, hour);
    String cStr = confTag(confidence);
    String sStr = sourceTag(source);
    if (sStr == null) {
      if (cStr == null) {
        return;
      } else if (cStr.equals("<br>")) {
        out.println("<br>");
        return;
      }
    }
    if ((cStr != null) && cStr.equals("<br>")) {
      cStr = null;
    }

    out.print("<font size=\"2\">");
    if (cStr != null) {
      out.print(cStr);
      if (sStr != null) {
        out.print("; ");
      }
    }
    if (sStr != null) {
      out.print(sStr);
    }
    out.println("</font>");
    return;
  }
  
  /***************************************************************************
  **
  ** Source tag
  **
  */

  private String sourceTag(int source) {
     switch (source) {
      case ExpressionEntry.NO_SOURCE_SPECIFIED:
        return (null);
      case ExpressionEntry.MATERNAL_SOURCE:
        return ("M");    
      case ExpressionEntry.ZYGOTIC_SOURCE:
        return ("Z");    
      case ExpressionEntry.MATERNAL_AND_ZYGOTIC:
        return ("M+Z");
      default:
        throw new IllegalArgumentException();
    } 
  }
 
  /***************************************************************************
  **
  ** Confidence tag
  **
  */

  private String confTag(int confidence) {
    switch (confidence) {
      case TimeCourseGene.NORMAL_CONFIDENCE:
        return (null);
      case TimeCourseGene.ASSUMPTION:
        return ("A"); 
      case TimeCourseGene.INTERPOLATED:
        return ("I");    
      case TimeCourseGene.INFERRED:
        return ("R");
      case TimeCourseGene.QUESTIONABLE:
        // FIX ME!!!  HAS NO_DATA VALUE TOO
        return ("Q");        
      default:
        return ("<br>");
        //System.err.println("USE BASE CONFIDENCE");
        //throw new IllegalArgumentException();
    }
  }
 
  /***************************************************************************
  **
  ** Build QPCR contents for table cell
  **
  */

  private void buildQPCRList(PrintWriter out, Set<PerturbationData.SourceInfo> sources) {
    int size = sources.size();
    if (size == 0) {
      return;
    }
    boolean first = true;
    Iterator<PerturbationData.SourceInfo> sit = sources.iterator();
    while (sit.hasNext()) {
      PerturbationData.SourceInfo source = sit.next();
      if (source.sign == PertProperties.NO_LINK) {
        continue;
      }
      if (!first) {
        out.print(", ");
      } else {
        first = false;
      }
      out.print("<FONT SIZE=2 COLOR=\"");
      if (source.sign == PertProperties.PROMOTE_LINK) {
        out.print("#66CC66\">");
      } else if (source.sign == PertProperties.REPRESS_LINK) { 
        out.print("#CC6666\">"); 
      } else {
        out.print("#000000\">");
      }
      out.print(source.source);
      out.println("</FONT>");      
    }
    
    return;
  }  
  
  /***************************************************************************
  **
  ** Build the color key
  **
  */
  
  public static void buildKey(PrintWriter out) {
    
    out.println("<table WIDTH=810 BORDER=1 CELLPADDING=2 CELLSPACING=0>");  

    //
    // Text row
    //
    
    out.println("<tr align=\"Center\" valign=\"Center\" >"); 
    out.println("<td width=\"100\" >");
    out.println("<B>EXPRESSION:</B>");
    out.println("</td>");
    
    out.println("<td width=\"100\" >");
    out.print("Region not Present");        
    out.println("</td>");
    
    out.println("<td width=\"100\" >");
    out.print("No Data");        
    out.println("</td>");
    
    out.println("<td width=\"100\" >");
    out.print("Not Expressed");        
    out.println("</td>");    
    
    out.println("<td width=\"100\" >");
    out.print("Weak Expression");        
    out.println("</td>");
    
    out.println("<td width=\"100\" >");
    out.print("Expressed");        
    out.println("</td>");
    
    out.println("</tr>");
    
    //
    // Color Row
    //
    
    out.println("<tr align=\"Center\" valign=\"Center\" >"); 
    out.println("<td width=\"100\">");
    out.println("<B>COLOR:</B>");
    out.println("</td>");
    out.print("<td width=\"100\"");
    out.print("bgcolor=\"#DDDDDD\">");        
    out.print("<br>");
    out.println("</td>");
    out.print("<td width=\"100\"");
    out.print("bgcolor=\"#FFFFFF\">");
    out.print("-");
    out.println("</td>");
    out.print("<td width=\"100\"");        
    out.print("bgcolor=\"#FFFFFF\">");
    out.print("<br>");
    out.println("</td>");
    out.print("<td width=\"100\"");
    out.print("bgcolor=\"#CCFFCC\">");
    out.print("<br>");
    out.println("</td>");
    out.print("<td width=\"100\"");
    out.print("bgcolor=\"#66EE66\">");
    out.print("<br>");
    out.println("</td>");
    out.println("</tr>");    
    out.println("</table>");    
    return;
  }  

  /***************************************************************************
  **
  ** Build the wt/pert key
  **
  */
  
  public static void buildWTPertNote(BTState appState, PrintWriter out) {  
    out.print("<p><center>");
    out.print(appState.getRMan().getString("timeCourseTable.wildVersusPertNote"));
    out.println("</center></p>");      
    out.println("<p></p>"); 
    return;
  }
  
  /***************************************************************************
  **
  ** Build the confidence key
  **
  */
  
  public static void buildConfidenceKey(PrintWriter out) {

    //
    // Text row
    //

    out.print("<b>CONFIDENCE:</b>");
    out.print("&nbsp;&nbsp;&nbsp;[No Symbol] = Normal Confidence");        
    out.print("&nbsp;&nbsp;&nbsp;I = Interpolated");
    out.print("&nbsp;&nbsp;&nbsp;R = Inferred"); 
    out.print("&nbsp;&nbsp;&nbsp;Q = Questionable");
    out.print("&nbsp;&nbsp;&nbsp;A = Assumption"); 
    return;
  }  
  
  /***************************************************************************
  **
  ** Build the source key
  **
  */
  
  public static void buildSourceKey(PrintWriter out) {

    //
    // Text row
    //

    out.print("<b>SOURCE:</b>");  
    out.print("&nbsp;&nbsp;&nbsp;M = Maternal");
    out.print("&nbsp;&nbsp;&nbsp;Z = Zygotic"); 
    out.print("&nbsp;&nbsp;&nbsp;[No Symbol] = Not Specified");        
    return;
  }  
 
  /***************************************************************************
  **
  ** Get region data
  **
  */
  
  private RegionData buildRegionData(String region) { 
    TimeCourseGene.VariableLevel varLev = new TimeCourseGene.VariableLevel();
    // Crank through regions.
    RegionData retval = new RegionData();
    retval.minHour = 10000;
    retval.maxHour = -1;
    retval.region = region;
    HashSet<Integer> times = new HashSet<Integer>();
    client_.getInterestingTimes(times);
    Iterator<Integer> tmit = times.iterator();
    while (tmit.hasNext()) {
      Integer hour = tmit.next();
      int hr = hour.intValue();
      int level = client_.getExpressionLevelForSource(region, hr, ExpressionEntry.NO_SOURCE_SPECIFIED, varLev);
      if (level != ExpressionEntry.NO_REGION) {
        if (hr < retval.minHour) {
          retval.minHour = hr;
        }
        if (hr > retval.maxHour) {
          retval.maxHour = hr;
        }
      }
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Sort regions.  Originally, ones that show up first went first, but if 
  ** a tie, the one that lasts longest went second.  These days (4.1+), we
  ** make order explicit by providing a list, so the original method is
  ** unused...
  */
  
  private List<RegionData> sortRegions(HashSet<String> regions, List<String> regionOrder) {
    // First option is when we have no region order:
    if (regionOrder == null) {
      TreeSet<RegionData> sorted = new TreeSet<RegionData>(new RegionComparator());
      Iterator<String> rit = regions.iterator();
      while (rit.hasNext()) {
        String region = rit.next();
        RegionData rd = buildRegionData(region);
        sorted.add(rd);
      }
      return (new ArrayList<RegionData>(sorted));
    } else {
      ArrayList<RegionData> retval = new ArrayList<RegionData>();
      int numRO = regionOrder.size();
      for (int i = 0; i < numRO; i++) {
        String region = regionOrder.get(i);
        RegionData rd = buildRegionData(region);
        retval.add(rd);
      }
      return (retval);
    }
  }
  
  /***************************************************************************
  **
  ** Take a basic region hierarchy tree and fill one that has
  ** RegionData kids, with children sorted by first expression 
  ** appearing last
  */
  
  private void stockRegionTree(TreeModel hierTree, DefaultMutableTreeNode currNode, 
                               DefaultTreeModel newTree, DefaultMutableTreeNode newNode, List<RegionData> depthFirstList) {  
    
    //
    // Get original order:
    //
    ArrayList<String> origOrder = new ArrayList<String>();
    HashMap<String, DefaultMutableTreeNode> regToKid = new HashMap<String, DefaultMutableTreeNode>();
    int kidCount = hierTree.getChildCount(currNode);
    for (int i = 0; i < kidCount; i++) {
      DefaultMutableTreeNode kidNode = (DefaultMutableTreeNode)hierTree.getChild(currNode, i);
      String regID = (String)kidNode.getUserObject();
      origOrder.add(regID);
      regToKid.put(regID, kidNode);
    }
    TreeSet<RegionData> sorted = new TreeSet<RegionData>(new PreservingRegionComparator(origOrder));
    
    //
    // Sorted List:
    //
    
    for (int i = 0; i < kidCount; i++) {
      DefaultMutableTreeNode kidNode = (DefaultMutableTreeNode)hierTree.getChild(currNode, i);
      RegionData rd = buildRegionData((String)kidNode.getUserObject());
      sorted.add(rd);
    }
    
    Iterator<RegionData> sit = sorted.iterator();
    while (sit.hasNext()) {
      RegionData rd = sit.next();
      depthFirstList.add(rd);
      DefaultMutableTreeNode mtn = new DefaultMutableTreeNode(rd);
      newTree.insertNodeInto(mtn, newNode, newNode.getChildCount());
      DefaultMutableTreeNode kidNode = regToKid.get(rd.region);
      stockRegionTree(hierTree, kidNode, newTree, mtn, depthFirstList);
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
  ** Sorts regions for output
  **
  */
  
  private class RegionComparator implements Comparator<RegionData> {
    public int compare(RegionData rd1, RegionData rd2) {
      if (rd1.minHour > rd2.minHour) {
        return (1);
      }
      if (rd2.minHour > rd1.minHour) {
        return (-1);
      }
      // mins are equal
      if (rd1.maxHour > rd2.maxHour) {
        return (1);
      }
      if (rd2.maxHour > rd1.maxHour) {
        return (-1);
      }
      return (rd1.region.compareTo(rd2.region));
    }
  }
  
  /***************************************************************************
  **
  ** Sorts regions for output.  Sorts in order of lowest min hour last, while
  ** otherwise preserving the original list order 
  **
  */
  
  private class PreservingRegionComparator implements Comparator<RegionData> {
    
    private List<String> origOrder_;
    
    PreservingRegionComparator(List<String> origOrder) {
      origOrder_ = origOrder;  
    }
    
    public int compare(RegionData rd1, RegionData rd2) {
      if (rd1.minHour > rd2.minHour) {
        return (-1);
      }
      if (rd2.minHour > rd1.minHour) {
        return (1);
      }
      int reg1Index = origOrder_.indexOf(rd1.region);
      int reg2Index = origOrder_.indexOf(rd2.region);
      
      return (reg1Index - reg2Index);
    }
  }
  
 /***************************************************************************
  **
  ** Region Data
  **
  */
  
  private static class RegionData {
    String region;
    int minHour;
    int maxHour;
  }
  
  /***************************************************************************
  **
  ** Used to show hierarchy in an html table.
  **
  */
  
  private static class TreeInTable {
    private ArrayList<RowData> list_;
    private int maxCols_;
    private int colCount_;
    private BTState appState_;
    
    TreeInTable(BTState appState, DefaultTreeModel hierTree, SortedSet<Integer> times, int numRows) {
      appState_ = appState;
      colCount_ = 0;
      maxCols_ = appState_.getDB().getNumColors();
      list_ = new ArrayList<RowData>();
      for (int i = 0; i < numRows; i++) {
        list_.add(new RowData());
      }
      ArrayList<Integer> timeList = new ArrayList<Integer>(times);
      DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode)hierTree.getRoot(); 
      stockTheList(hierTree, rootNode, timeList, -1);
    }
      
    void addBlock(TreeInTableBlock block, int startRow) {
      Set<Integer> times = block.branches.keySet();
      Iterator<Integer> timit = times.iterator();
      int srFloor = (startRow < 0) ? 0 : startRow;
      while (timit.hasNext()) {
        Integer time = timit.next();
        TreeBranch tb = block.branches.get(time);
        Integer hiRow = tb.rows.last();
        int lastRow = hiRow.intValue();
        for (int i = srFloor; i <= lastRow; i++) {
          RowData rd = list_.get(i);
          Integer tm1 = Integer.valueOf(time.intValue() - 1);
          rd.vertColors.put(tm1, block.color);
          Integer checkRow = new Integer(i);
          if (tb.rows.contains(checkRow)) {
            rd.rcs = new RowColorSpec(time.intValue(), block.color);
          }
        }
      }
      return;
    }  
    
    Map<Integer, String> getColumnColors(int row) {
      RowData rd = list_.get(row);
      return (rd.vertColors);
    }
  
    RowColorSpec getTopColor(int row) {
      RowData rd = list_.get(row);
      return (rd.rcs);
    }
    
    Color getNextColor() {    
      Database db = appState_.getDB();
      String colTag = null;
      // Don't use green!
      while (colTag == null) {
        colTag = db.getGeneColor(colCount_++);
        if (colTag.equals("EX-green")) {
          colTag = null;
        }
        if (colCount_ >= maxCols_) {
          colCount_ = 0;
        }
      }
      return (db.getColor(colTag));
    }
    
    void stockTheList(DefaultTreeModel hierTree, DefaultMutableTreeNode currNode, List<Integer> timeList, int startRow) { 
      int kidCount = hierTree.getChildCount(currNode);
      
      TreeMap<Integer, TreeBranch> kidBranches = new TreeMap<Integer, TreeBranch>();
      int sibStart = startRow;
      int lastRow = startRow + 1;
      int kidStart = startRow + 1;
      RegionData cnuo = (RegionData)currNode.getUserObject();     
      //
      // If the top dummy root has mutiple kids, we show the heirarchy for it.
      // If only one kid, we do not.
      //
      boolean multiKids = false;
      if (cnuo == null) {
        multiKids = (kidCount > 1);
      }     
      Color myColor = ((cnuo != null) || multiKids) ?  getNextColor() : null;
      
      for (int i = 0; i < kidCount; i++) {
        DefaultMutableTreeNode kidNode = (DefaultMutableTreeNode)hierTree.getChild(currNode, i);
        int decCount = getDecendantCount(hierTree, kidNode);
        lastRow += (decCount + 1); // add kid too
        RegionData regKid = (RegionData)kidNode.getUserObject();
        int minTimeIndex = timeList.indexOf(Integer.valueOf(regKid.minHour));
        Integer minTimeIndexObj = Integer.valueOf(minTimeIndex);
        TreeBranch bra = kidBranches.get(minTimeIndexObj);
        if (bra == null) {
          bra = new TreeBranch(minTimeIndexObj);
          kidBranches.put(minTimeIndexObj, bra);
        }
        bra.rows.add(new Integer(kidStart));
        if (decCount != 0) {
          stockTheList(hierTree, kidNode, timeList, kidStart);
          kidStart += decCount;
        }
        kidStart++;
      }
     
      if ((cnuo != null) || multiKids) {
        addBlock(new TreeInTableBlock(kidBranches, myColor), sibStart);
      }
      sibStart = lastRow + 1;
      return;
    }
    
    int getDecendantCount(TreeModel tree, TreeNode node) {   
      int kidCount = tree.getChildCount(node);
      int retval = kidCount;
      for (int i = 0; i < kidCount; i++) {
        TreeNode kidNode = (TreeNode)tree.getChild(node, i);
        retval += getDecendantCount(tree, kidNode);
      }
      return (retval);
    }
  }
  
  /***************************************************************************
  **
  ** Used to show hierarchy in an html table.
  **
  */
  
  private static class TreeInTableBlock {
    TreeMap<Integer, TreeBranch> branches;
    String color;
    
    TreeInTableBlock(TreeMap<Integer, TreeBranch> branches, Color color) {
      this.branches = branches;
      this.color = "#" + (Integer.toHexString(color.getRGB())).substring(2);
    }
  }
  
  /***************************************************************************
  **
  ** Used to show hierarchy in an html table.
  **
  */
  
  private static class TreeBranch {
    //Integer time;
    TreeSet<Integer> rows;
    
    @SuppressWarnings("unused")    
    TreeBranch(Integer time) {
      //this.time = time;
      this.rows = new TreeSet<Integer>();
    } 
  }
  
  /***************************************************************************
  **
  ** Used to show hierarchy in an html table.
  **
  */
  
  private static class RowColorSpec {
    int startCol;
    String color;
     
    RowColorSpec(int startCol, String color) {
      this.startCol = startCol;
      this.color = color;
    } 
  }  
  
  /***************************************************************************
  **
  ** Used to show hierarchy in an html table.
  **
  */
  
  private static class RowData {
    RowColorSpec rcs;
    HashMap<Integer, String> vertColors;
     
    RowData() {
      this.rcs = null;
      vertColors = new HashMap<Integer, String>();
    } 
  }  
}
