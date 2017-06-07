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


package org.systemsbiology.biotapestry.cmd.flow.io;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.ui.GroupPanel;
import org.systemsbiology.biotapestry.ui.GroupPanel.ClickMapColorMinMaxBounder;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalShapeJSONizer;

/****************************************************************************
**
** Json export
*/

public class ExportGroupNode extends AbstractControlFlow {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////   
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
   
  public ExportGroupNode() {
    name = "command.ExportGroupNodeJSON";
    desc = "command.ExportGroupNodeJSON";
    mnem = "command.ExportGroupNodeJSONMnem";
  }

  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
  
  @Override  
  public boolean isEnabled(CheckGutsCache cache) {
    return (true);  
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
 
  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
    
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {
    StepState retval = new StepState(dacx);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Handle the flow
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
    
    DialogAndInProcessCmd next;
  
    while (true) {
      StepState ans;
      if (last == null) {
        throw new IllegalArgumentException();
      } else {
        ans = (StepState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
      }
      if (ans.getNextStep().equals("stepToProcess")) {
        next = ans.stepToProcess();
      } else {
        throw new IllegalStateException();
      }
      if (!next.state.keepLooping()) {
        return (next);
      }
      last = next;
    }
  }
  
  /***************************************************************************
  **
  ** Running State
  */
        
  public static class StepState extends AbstractStepState {

    private String genomeID_;
    private String nodeID_;
    private double zoom_;
    private String holdKey_;
    private boolean swapOut_;
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(StaticDataAccessContext dacx) {
      super(dacx);
      nextStep_ = "stepToProcess";
    }
 
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(ServerControlFlowHarness cfh) {
      super(cfh);
      nextStep_ = "stepToProcess";
    }

    /***************************************************************************
    **
    ** Set the  params
    */ 
        
    public void setParams(String nodeID) {
      nodeID_ = nodeID;
      genomeID_ = null;
      swapOut_ = true;
      return;
    }

    private List<HashMap<String, Object>> exportClickMap(Map<Color, NavTree.GroupNodeMapEntry> map) {
      List<HashMap<String, Object>> result = new ArrayList<HashMap<String, Object>>();

      for (Color color : map.keySet()) {
        NavTree.GroupNodeMapEntry mapEntry = map.get(color);
        HashMap<String, Object> entry = new HashMap<String, Object>();
        entry.put("r", color.getRed());
        entry.put("g", color.getGreen());
        entry.put("b", color.getBlue());
        entry.put("modelID", mapEntry.modelID);
        entry.put("proxyID", mapEntry.proxyID);
        entry.put("proxyTime", mapEntry.proxyTime);
        entry.put("regionID", mapEntry.regionID);

        result.add(entry);
      }

      return result;
    }

    /***************************************************************************
     **
     ** Passthrough-class for exporting click map color bounds through flexjson.
     */

    public class colorBoundsEntry {
      public ArrayList<Integer> color_;
      public HashMap<String, Object> bounds_;

      public colorBoundsEntry(ArrayList<Integer> color, HashMap<String, Object> bounds) {
        color_ = color;
        bounds_ = bounds;
      }
    }

    /***************************************************************************
     **
     ** Do the step
     */

    private DialogAndInProcessCmd stepToProcess() {

      Map<String,Object> modelMap = null;
        
    	if(nodeID_ != null) {
	        GroupPanel groupPanel = uics_.getGroupPanel();
	
	        modelMap = new HashMap<String,Object>();
	        modelMap.put("nodeID", nodeID_);
	
	        BufferedImage img = groupPanel.getImg();
	        Map<String,Object> workspace = new HashMap<String,Object>();
	        workspace.put("x",0);
	        workspace.put("y",0);
	        workspace.put("h",img.getHeight());
	        workspace.put("w",img.getWidth());
	        modelMap.put("workspace",workspace);
	
	        Map<Color, NavTree.GroupNodeMapEntry> clickMap  = groupPanel.getMap();
	        List<HashMap<String, Object>> exportClickMap = exportClickMap(clickMap);
	        modelMap.put("clickMap", exportClickMap);
	
	        modelMap.put("displayText",new HashMap<String,String>());


        BufferedImage mapImage = groupPanel.getMapImg();
        GroupPanel.ClickMapColorMinMaxBounder bounder = new GroupPanel.ClickMapColorMinMaxBounder(mapImage);

        ArrayList<colorBoundsEntry> colorBoundsList = new ArrayList<colorBoundsEntry>();

        Map<Color, NavTree.GroupNodeMapEntry> colorMap = groupPanel.getMap();

        for (Color color : colorMap.keySet()) {
          ClickMapColorMinMaxBounder.BoundSearchResult bsr = bounder.getBoundingRectangleForColor(color);

          if (bsr.found_) {
            HashMap<String, Object> exportRect = new HashMap<String, Object>();
            exportRect.put("x", bsr.rectangle_.x);
            exportRect.put("y", bsr.rectangle_.y);
            exportRect.put("w", bsr.rectangle_.width);
            exportRect.put("h", bsr.rectangle_.height);

            colorBoundsList.add(new colorBoundsEntry(ModalShapeJSONizer.pluckRGBA(color), exportRect));
          }
        }

        modelMap.put("color_bounds_map", colorBoundsList);
    	}

      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this, modelMap));
    }
  }
}
