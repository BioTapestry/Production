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

package org.systemsbiology.biotapestry.genome;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.io.IOException;
import java.awt.geom.Point2D;
import java.awt.Dimension;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.instruct.BuildInstruction;
import org.systemsbiology.biotapestry.cmd.instruct.BuildInstructionProcessor;
import org.systemsbiology.biotapestry.cmd.instruct.GeneralBuildInstruction;
import org.systemsbiology.biotapestry.cmd.instruct.LoneNodeBuildInstruction;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.ui.LinkRouter;
import org.systemsbiology.biotapestry.ui.LayoutOptions;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayout;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngineParams;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BTProgressMonitor;

/****************************************************************************
**
** This handles reading a DBGenome from a SIF file
*/

public class DBGenomeSIFFormatFactory {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private static final String NEGATIVE_STR_ = "neg";
  private static final String NONE_STR_     = "neu";
  private static final String POSITIVE_STR_ = "pos";  
  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
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

  public DBGenomeSIFFormatFactory() {
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Get the SIF file
  */

  public AugmentedResult buildFromSIF(BTState appState, DataAccessContext dacx, File infile,                                                                                             
                                      boolean doReplacement,
                                      SpecialtyLayout specLayout,
                                      SpecialtyLayoutEngineParams params,
                                      UndoSupport support, boolean doOpts, int overlayOption,
                                      BTProgressMonitor monitor, double startFrac, double endFrac) 
                                      throws IOException, AsynchExitRequestException {
    //
    // Read in the lines.
    //
    
    Point2D center = appState.getCanvasCenter();
    Dimension size = appState.getCanvasSize();
    ArrayList<BuildInstruction> commands = new ArrayList<BuildInstruction>();    
    BufferedReader in = new BufferedReader(new FileReader(infile));
    String line = null;
    boolean seenSign = false;
    while ((line = in.readLine()) != null) {
      String[] tokens = line.split("\\t");
      if (tokens.length == 0) {
        continue;
      } else if ((tokens.length == 1) || (tokens.length == 2)) {
        if (tokens[0].trim().equals("")) {
          throw new IOException();
        }
        commands.add(processRelationToInstruction(appState, dacx, tokens[0].trim(), null, null, Linkage.NONE));
      } else {
        for (int i = 2; i < tokens.length; i++) {
          if (tokens[0].trim().equals("") || tokens[i].trim().equals("")) {
            throw new IOException();
          }

          int sign = Linkage.POSITIVE;
          if (tokens[1] != null) {
            String link = tokens[1].trim();   
            if (link.equalsIgnoreCase(POSITIVE_STR_) || link.equalsIgnoreCase(SifSupport.POSITIVE_STR_EXP)) {
              sign = Linkage.POSITIVE;
              seenSign = true;
            } else if (link.equalsIgnoreCase(NEGATIVE_STR_) || link.equalsIgnoreCase(SifSupport.NEGATIVE_STR_EXP)) {
              sign = Linkage.NEGATIVE;
              seenSign = true;
            } else if (link.equalsIgnoreCase(NONE_STR_) || link.equalsIgnoreCase(SifSupport.NONE_STR_EXP)) {
              sign = Linkage.NONE;
              seenSign = true;
            }
          }
          commands.add(processRelationToInstruction(appState, dacx, tokens[0].trim(), tokens[1], tokens[i].trim(), sign));
        }
      }
    }  
    in.close();

    LayoutOptions options = new LayoutOptions(appState.getLayoutOptMgr().getLayoutOptions());
    options.optimizationPasses = (doOpts) ? 1 : 0;
    options.overlayOption = overlayOption;
    
    BuildInstructionProcessor bip = new BuildInstructionProcessor(appState);
    BuildInstructionProcessor.PISIFData psd = new BuildInstructionProcessor.PISIFData(appState, dacx, commands, center, size, !doReplacement, false,
                                                                                      options, support, monitor, startFrac, endFrac, 
                                                                                      specLayout, params);
    bip.installPISIFData(psd);
    LinkRouter.RoutingResult result = bip.processInstructionsForSIF(dacx);
    return (new AugmentedResult(result, seenSign));
  } 
  
  /***************************************************************************
  ** 
  ** Build for gaggle
  */

  public LinkRouter.RoutingResult buildForGaggle(BTState appState, DataAccessContext dacx,
                                                 List<BuildInstruction> commands,
                                                 boolean doReplacement,
                                                 SpecialtyLayout specLayout,
                                                 SpecialtyLayoutEngineParams params,                                                
                                                 UndoSupport support, boolean doOpts, 
                                                 BTProgressMonitor monitor, double startFrac, double endFrac) 
                                                 throws IOException, AsynchExitRequestException {
    
    LayoutOptions options = appState.getLayoutOptMgr().getLayoutOptions();
    options.optimizationPasses = (doOpts) ? 1 : 0; 
    Point2D center = appState.getCanvasCenter(); 
    Dimension size = appState.getCanvasSize();
          
    //
    // Gaggle instructions have no id yet:
    //
    
    Iterator<BuildInstruction> cit = commands.iterator();
    while (cit.hasNext()) {
      BuildInstruction bi = cit.next();
      String id = dacx.getInstructSrc().getNextInstructionLabel(); 
      bi.setID(id);
    }
    
    BuildInstructionProcessor bip = new BuildInstructionProcessor(appState);
    BuildInstructionProcessor.PISIFData psd = new BuildInstructionProcessor.PISIFData(appState, dacx, commands, center, size, !doReplacement, false,
                                                                                      options, support, monitor, startFrac, endFrac, 
                                                                                      specLayout, params);
    bip.installPISIFData(psd);
    LinkRouter.RoutingResult result = bip.processInstructionsForSIF(dacx);
    return (result);
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
   
  public static Object[] getSignTags() {
    Object[] retval = new Object[3];
    retval[0] = POSITIVE_STR_;
    retval[1] = NEGATIVE_STR_;
    retval[2] = NONE_STR_;
    return (retval);
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
    
  /***************************************************************************
  **
  ** Result wrapper
  */ 
    
  public static final class AugmentedResult {
       
    public LinkRouter.RoutingResult coreResult;
    public boolean usedSignTag;
   
    public AugmentedResult(LinkRouter.RoutingResult coreResult, boolean usedSignTag) {   
      this.coreResult = coreResult;
      this.usedSignTag = usedSignTag;
    }
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Process the relation into a Build Instruction
  */
  
  private BuildInstruction processRelationToInstruction(BTState appState, DataAccessContext dacx, String node1ID, String link, String node2ID, int sign) {
    String id = dacx.getInstructSrc().getNextInstructionLabel();    
    if ((link != null) && (node2ID != null)) {
      return (new GeneralBuildInstruction(id, Node.GENE, node1ID, 
                                          GeneralBuildInstruction.mapFromLinkageSign(sign), 
                                          Node.GENE, node2ID));
    } else {
      return (new LoneNodeBuildInstruction(id, Node.GENE, node1ID));
    }
  }
}
