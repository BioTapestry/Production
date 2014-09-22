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


package org.systemsbiology.biotapestry.app;

import java.awt.Dimension;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.cmd.flow.BatchJobControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.ControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.cmd.flow.export.ExportPublish;
import org.systemsbiology.biotapestry.cmd.flow.io.ExportWeb;
import org.systemsbiology.biotapestry.cmd.flow.io.ImportCSV;
import org.systemsbiology.biotapestry.cmd.flow.io.LoadSaveOps;
import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.nav.ZoomTarget;
import org.systemsbiology.biotapestry.ui.ImageExporter;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.ViewExporter;
import org.systemsbiology.biotapestry.ui.dialogs.SIFImportChoicesDialogFactory;
import org.systemsbiology.biotapestry.util.ExceptionHandler;
import org.systemsbiology.biotapestry.util.NamedOutputStreamSource;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.NodeRegionModelNameTuple;
import org.systemsbiology.biotapestry.util.ModelNodeIDPair;
import org.systemsbiology.biotapestry.util.WebPublisher;

/****************************************************************************
**
** The top-level application or in-process entry point for headless image generation
*/

public class ImageGeneratorApplication {
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////   
  
  /****************************************************************************
  **
  ** These are the allowed input and output options when operating
  ** in-process with input and output streams:
  */  
   
  public final static int BTP_INPUT                = 0;
  public final static int CSV_INPUT                = 1;
  public final static int BTP_PRE_INPUT_CSV_PRUNED = 2;
  
  public final static int PNG_OUTPUT = 0;
  public final static int BTP_OUTPUT = 1;
  private final static int FULL_HIERARCHY_OUTPUT_ = 2;  // Internal use only...
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////   
  
  private int inputType_;
  private InputStream input_;
  private InputStream input2_;
  private SIFImportChoicesDialogFactory.LayoutModes csvMode_;
  private Integer csvOverlayMode_;
  private int outputType_;
  private OutputStream output_;
  private Boolean csvCompress_;
  private HashMap<String, String> modelIDMap_;
  private HashMap<NodeRegionModelNameTuple, ModelNodeIDPair> nodeIDMap_;
  private HashMap<WebPublisher.ModelScale, ViewExporter.BoundsMaps> intersectionMap_;
  private NamedOutputStreamSource noss_;
  private HashSet<WebPublisher.ModelScale> publishKeys_;
  private Map<String, Object> args_;
  private BTState appState_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  ** 
  ** This Constructor is to run BioTapestry in-process as a headless I/O filter.  
  ** Note that this sets the system property "java.awt.headless" to "true".
  ** inputType = BTP_INPUT or CSV_INPUT;
  ** outputType = PNG_OUTPUT or BTP_OUTPUT;
  ** The csvCompress arg must be null unless the CSV_INPUT type is specified.
  ** Any errors will throw a ImageGeneratorApplication.GeneratorException:
  */ 
  
  public ImageGeneratorApplication(int inputType, InputStream input, 
                                   InputStream input2, int outputType, 
                                   OutputStream output, SIFImportChoicesDialogFactory.LayoutModes csvMode, 
                                   Integer csvOverlayMode, Boolean csvCompress) {
    
    if (!checkArgs(inputType, input, input2, csvMode, csvOverlayMode, csvCompress)) {
      throw new IllegalArgumentException();
    }
  
    inputType_ = inputType;
    input_ = input;
    input2_ = input2;
    
    if ((outputType_ != PNG_OUTPUT) && (outputType_ != BTP_OUTPUT)) {
      throw new IllegalArgumentException();
    }    
    outputType_ = outputType;
    output_ = output;
          
    if ((inputType_ == CSV_INPUT) || (inputType_ == BTP_PRE_INPUT_CSV_PRUNED)) {
      modelIDMap_ = new HashMap<String, String>();
      nodeIDMap_  = new HashMap<NodeRegionModelNameTuple, ModelNodeIDPair>();
      csvCompress_ = csvCompress;
      csvMode_ = csvMode;
      csvOverlayMode_ = csvOverlayMode;
    }
  }  

  /***************************************************************************
  ** 
  ** This Constructor is to run BioTapestry in-process as a headless I/O filter,
  ** where the output is a selection of images for the full model heirarchy.
  ** 
  ** Note that this sets the system property "java.awt.headless" to "true".
  ** inputType = BTP_INPUT or CSV_INPUT;
  ** The csvCompress arg must be null unless the CSV_INPUT type is specified.
  ** publishKeys are a set of WebPublisher.ModelScale objects, e.g.:
  **   WebPublisher.ModelScale(internalModelID, WebPublisher.ModelScale.SMALL);
  ** where scales are SMALL, MEDIUM, LARGE, or JUMBO.
  ** Any errors will throw a ImageGeneratorApplication.GeneratorException:
  */  
 
  public ImageGeneratorApplication(int inputType, InputStream input, 
                                   InputStream input2,
                                   NamedOutputStreamSource noss, 
                                   Set<WebPublisher.ModelScale> publishKeys, 
                                   SIFImportChoicesDialogFactory.LayoutModes csvMode, 
                                   Integer csvOverlayMode, Boolean csvCompress) {
    
    
    if (!checkArgs(inputType, input, input2, csvMode, csvOverlayMode, csvCompress)) {
      throw new IllegalArgumentException();
    }
    
    inputType_ = inputType;
    input_ = input;
    input2_ = input2;
    
    outputType_ = FULL_HIERARCHY_OUTPUT_;
    noss_ = noss;
    
    publishKeys_ = new HashSet<WebPublisher.ModelScale>(publishKeys);
    
    if ((inputType_ == CSV_INPUT) || (inputType_ == BTP_PRE_INPUT_CSV_PRUNED)) {
      modelIDMap_ = new HashMap<String, String>();
      nodeIDMap_  = new HashMap<NodeRegionModelNameTuple, ModelNodeIDPair>();
      csvCompress_ = csvCompress;
      csvMode_ = csvMode;
      csvOverlayMode_ = csvOverlayMode;
    }
    
    intersectionMap_ = new HashMap<WebPublisher.ModelScale, ViewExporter.BoundsMaps>();
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Following a CSV load, this map is filled in with:
  **   (String)(CSV model name)->(String)(Internal model ID)
  ** Not thread-safe; assuming you are calling on the same thread as
  ** the process() call.
  */

  public Map<String, String> getModelIDMap() {
    return (modelIDMap_);
  }
  
  /***************************************************************************
  ** 
  ** Following a CSV load, this map is filled in with:
  **   (NodeRegionModelNameTuple)->(ModelNodeIDPair)
  ** for each node in the model hierarchy
  ** Not thread-safe; assuming you are calling on the same thread as
  ** the process() call.
  */

  public Map<NodeRegionModelNameTuple, ModelNodeIDPair> getNodeIDMap() {
    return (nodeIDMap_);
  }
  
  /***************************************************************************
  ** 
  ** Following a FULL_HIERARCHY_OUTPUT_ output, this map is filled in.
  **  (WebPublisher.ModelScale)->(SUPanel.BoundsMaps)
  ** Not thread-safe; assuming you are calling on the same thread as
  ** the process() call.
  */

  public Map<WebPublisher.ModelScale, ViewExporter.BoundsMaps> getIntersectionMap() {
    return (intersectionMap_);
  }
  
  /***************************************************************************
  ** 
  ** In-process processing entry point.  This method is synchronized internally
  ** on a global basis to insure one process call at a time.
  */

  public void process() throws GeneratorException {    
  
    //
    // Processing of all requests by all IGAs MUST be globally 
    // serialized in a multi-threaded environment.  At this time, 
    // BioTapestry core code must run on one thread when in batch mode.
    //
    
    synchronized (MainCommands.class) {
         
      boolean needInit = (appState_ == null);

      if (needInit) {
        System.setProperty("java.awt.headless", "true"); 
      }

      try {  
        ResourceManager rMan = appState_.getRMan();
        HashMap<String, Object> args = new HashMap<String, Object>();   
        if (needInit) {
          appState_ = new BTState("WJRL", args, true, false);
          appState_.setExceptionHandler(new ExceptionHandler(appState_, appState_.getRMan(), true));
          appState_.getDB().newModelViaDACX(); // Bogus, but no DACX yet
          appState_.setIsEditor(true);
          CommonView cview = new CommonView(appState_);
          cview.buildTheView();       
        }
        FlowMeister flom = appState_.getFloM();
        boolean haveInput = false;
        DataAccessContext dacx = new DataAccessContext(appState_, appState_.getGenome());

        switch (inputType_) {
          case BTP_INPUT:
            Object[] osArgs = new Object[2];
            osArgs[0] = Boolean.valueOf(false);
            osArgs[1] = input_;
            BatchJobControlFlowHarness dcf0 = new BatchJobControlFlowHarness(appState_, null); 
            ControlFlow myFlow0 = flom.getControlFlow(FlowMeister.MainFlow.LOAD, null);
            LoadSaveOps.StepState pre0 = (LoadSaveOps.StepState)myFlow0.getEmptyStateForPreload(dacx);
            pre0.setParams(osArgs);     
            dcf0.initFlow(myFlow0, dacx);
            DialogAndInProcessCmd daipc0 = dcf0.stepTheFlow(pre0);          
            if (daipc0.state != DialogAndInProcessCmd.Progress.DONE) {
              throw new GeneratorException(rMan.getString("headless.btpInputFailure"));
            }
            haveInput = true;
            break;
          case CSV_INPUT:
            osArgs = new Object[7];
            osArgs[0] = csvCompress_;
            osArgs[1] = csvMode_;
            osArgs[2] = csvOverlayMode_;
            osArgs[3] = Boolean.valueOf(false);
            osArgs[4] = input_;
            osArgs[5] = nodeIDMap_;
            osArgs[6] = modelIDMap_;
            BatchJobControlFlowHarness dcf1 = new BatchJobControlFlowHarness(appState_, null); 
            ControlFlow myFlow1 = flom.getControlFlow(FlowMeister.MainFlow.LOAD, null);
            ImportCSV.StepState pre1 = (ImportCSV.StepState)myFlow1.getEmptyStateForPreload(dacx);
            pre1.setParams(osArgs);     
            dcf1.initFlow(myFlow1, dacx);
            DialogAndInProcessCmd daipc1 = dcf1.stepTheFlow(pre1);          
            if (daipc1.state != DialogAndInProcessCmd.Progress.DONE) {
              throw new GeneratorException(rMan.getString("headless.csvInputFailure"));
            }
            haveInput = true;
            break;
          case BTP_PRE_INPUT_CSV_PRUNED:
            osArgs = new Object[2];
            osArgs[0] = Boolean.valueOf(false);
            osArgs[1] = input_;
            BatchJobControlFlowHarness dcf2 = new BatchJobControlFlowHarness(appState_, null); 
            ControlFlow myFlow2 = flom.getControlFlow(FlowMeister.MainFlow.LOAD, null);
            LoadSaveOps.StepState pre2 = (LoadSaveOps.StepState)myFlow2.getEmptyStateForPreload(dacx);
            pre2.setParams(osArgs);     
            dcf2.initFlow(myFlow2, dacx);
            DialogAndInProcessCmd daipc2 = dcf2.stepTheFlow(pre2);          
            if (daipc2.state != DialogAndInProcessCmd.Progress.DONE) {
              throw new GeneratorException(rMan.getString("headless.btpInputFailure"));
            }
            osArgs = new Object[7];
            osArgs[0] = csvCompress_;
            osArgs[1] = csvMode_;
            osArgs[2] = csvOverlayMode_;
            osArgs[3] = Boolean.valueOf(false);
            osArgs[4] = input2_;
            osArgs[5] = nodeIDMap_;
            osArgs[6] = modelIDMap_;
            BatchJobControlFlowHarness dcf3 = new BatchJobControlFlowHarness(appState_, null); 
            ControlFlow myFlow3 = flom.getControlFlow(FlowMeister.MainFlow.IMPORT_FULL_HIERARCHY_FROM_CSV, null);
            ImportCSV.StepState pre3 = (ImportCSV.StepState)myFlow3.getEmptyStateForPreload(dacx);
            pre3.setParams(osArgs);     
            dcf3.initFlow(myFlow3, dacx);
            DialogAndInProcessCmd daipc3 = dcf3.stepTheFlow(pre3);          
            if (daipc3.state != DialogAndInProcessCmd.Progress.DONE) {
              throw new GeneratorException(rMan.getString("headless.csvInputFailure"));
            }
            haveInput = true;
            break;
          default:
            throw new IllegalArgumentException();
        }   
        if (!haveInput) {
          throw new GeneratorException(rMan.getString("headless.noInputFailure"));
        }   

        boolean haveOutput = false;
        switch (outputType_) {
          case PNG_OUTPUT:
            Object[] osArgs = imageExportPrepForStream(args, output_);       
            if (osArgs != null) {
              BatchJobControlFlowHarness dcf = new BatchJobControlFlowHarness(appState_, null); 
              ControlFlow myFlow = flom.getControlFlow(FlowMeister.MainFlow.EXPORT, null);
              LoadSaveOps.StepState pre = (LoadSaveOps.StepState)myFlow.getEmptyStateForPreload(dacx);
              pre.setParams(osArgs);     
              dcf.initFlow(myFlow, dacx);
              DialogAndInProcessCmd daipc = dcf.stepTheFlow(pre);          
              if (daipc.state != DialogAndInProcessCmd.Progress.DONE) {
                throw new GeneratorException(rMan.getString("headless.imageExportFailure"));
              } else {
                haveOutput = true;
              }
            } else {
              throw new GeneratorException(rMan.getString("headless.imageExportFailure"));
            }
            break;
          case BTP_OUTPUT:
            osArgs = new Object[2];
            osArgs[0] = Boolean.valueOf(false);
            osArgs[1] = output_;
            BatchJobControlFlowHarness dcf = new BatchJobControlFlowHarness(appState_, null); 
            ControlFlow myFlow = flom.getControlFlow(FlowMeister.MainFlow.SAVE_AS, null);
            LoadSaveOps.StepState pre = (LoadSaveOps.StepState)myFlow.getEmptyStateForPreload(dacx);
            pre.setParams(osArgs);     
            dcf.initFlow(myFlow, dacx);
            DialogAndInProcessCmd daipc = dcf.stepTheFlow(pre);          
            if (daipc.state != DialogAndInProcessCmd.Progress.DONE) {
              throw new GeneratorException(rMan.getString("headless.btpExportFailure"));
            } else {
              haveOutput = true;
            }
            break;
          case FULL_HIERARCHY_OUTPUT_:
            osArgs = new Object[4];
            osArgs[0] = Boolean.valueOf(false);
            osArgs[1] = noss_;
            osArgs[2] = intersectionMap_;
            osArgs[3] = publishKeys_;
            BatchJobControlFlowHarness dcf2 = new BatchJobControlFlowHarness(appState_, null); 
            ControlFlow myFlow2 = flom.getControlFlow(FlowMeister.MainFlow.WEB, null);
            ExportWeb.StepState pre2 = (ExportWeb.StepState)myFlow2.getEmptyStateForPreload(dacx);
            pre2.setParams(osArgs);     
            dcf2.initFlow(myFlow2, dacx);
            DialogAndInProcessCmd daipc2 = dcf2.stepTheFlow(pre2);          
            if (daipc2.state != DialogAndInProcessCmd.Progress.DONE) {
              throw new GeneratorException(rMan.getString("headless.webExportFailure"));
            } else {
              haveOutput = true;
            }
            break;
          default:
            throw new IllegalArgumentException();
        }   

        if (!haveOutput) {
          throw new GeneratorException(rMan.getString("headless.totalExportFailure"));
        }
      } catch (ExceptionHandler.HeadlessException hex) {
        throw new GeneratorException(appState_.getRMan().getString("headless.wrappedExceptionFailure"), hex);
      }
    }
 
    return;
  }    

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Main entry point for running as a command-line argument
  */

  public static void main(String argv[]) {
    ArgParser ap = new ArgParser(); 
    Map<String, Object> argMap = ap.parse(ArgParser.AppType.PIPELINE, argv);
    if (argMap == null) {
      System.err.print(ap.getUsage(new BTState(), ArgParser.AppType.PIPELINE));
      System.exit(1);
    }
    ImageGeneratorApplication iga = new ImageGeneratorApplication(argMap);
    String errMsg = iga.generate();
    if (errMsg != null) {
      System.err.println(errMsg);
      System.exit(1);
    }
    return;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  ** 
  ** Used for command-line version
  */   
  
  private ImageGeneratorApplication(Map<String, Object> args) {
    args_ = args;
  }  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  ** 
  ** Check args:
  */  
  
  private boolean checkArgs(int inputType, InputStream input, 
                            InputStream input2, SIFImportChoicesDialogFactory.LayoutModes csvMode, 
                            Integer csvOverlayMode, Boolean csvCompress) {
    
    if (inputType == CSV_INPUT) {
      if (input2 != null) {
        return (false);
      }
      if ((csvCompress == null) || (csvMode == null) || (csvOverlayMode == null)) {
        return (false);
      }
    } else if (inputType == BTP_INPUT) {
      if (input2 != null) {
        return (false);
      } 
      if ((csvCompress != null) || (csvMode != null) || (csvOverlayMode != null)) {
        return (false);
      }
    } else if (inputType == BTP_PRE_INPUT_CSV_PRUNED) {
      if (input2 == null) {
        return (false);
      } 
      if ((csvCompress == null) || (csvMode == null) || (csvOverlayMode == null)) {
        return (false);
      }
    } else {
      return (false);
    }
    
    if (csvMode != null) {
   
      if ((csvMode != SIFImportChoicesDialogFactory.LayoutModes.INCREMENTAL) && 
          (csvMode != SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT)) {
        return (false);
      }
      int overVal = csvOverlayMode.intValue();
      if ((overVal != NetOverlayProperties.RELAYOUT_TO_MEMBER_ONLY) && 
          (overVal != NetOverlayProperties.RELAYOUT_SHIFT_AND_RESIZE_SHAPES) &&
          (overVal != NetOverlayProperties.RELAYOUT_NO_CHANGE)) {
        return (false);
      }
    }
    return (true);
  }
    
  /***************************************************************************
  ** 
  ** Handle command-line operation:
  */  
  
  private String generate() {
    
    //
    // Processing of all requests by all IGAs MUST be globally 
    // serialized in a multi-threaded environment.  At this time, 
    // BioTapestry core code must run on one thread when in batch mode.
    //

    synchronized(MainCommands.class) {
      
      System.setProperty("java.awt.headless", "true");   
      appState_ = new BTState("WJRL", args_, true, false);
      appState_.setExceptionHandler(new ExceptionHandler(appState_, appState_.getRMan(), false));
      appState_.getDB().newModelViaDACX(); // Bogus, but no DACX yet
      appState_.setIsEditor(true);
      CommonView cview = new CommonView(appState_);
      cview.buildTheView();
      ResourceManager rMan = appState_.getRMan();
      FlowMeister flom = appState_.getFloM();
      DataAccessContext dacx = new DataAccessContext(appState_, appState_.getGenome());
  
      //
      // Input operations.  Gotta have one and only one
      //

      boolean fileLoaded = false;

      String inFileName = (String)args_.get(ArgParser.FILE);
      if (inFileName != null) {
        Object[] osArgs = new Object[2];
        osArgs[0] = new Boolean(true);  // we are loading from file...
        osArgs[1] = inFileName;
        BatchJobControlFlowHarness dcf = new BatchJobControlFlowHarness(appState_, null); 
        ControlFlow myFlow = flom.getControlFlow(FlowMeister.MainFlow.LOAD, null);
        LoadSaveOps.StepState pre = (LoadSaveOps.StepState)myFlow.getEmptyStateForPreload(dacx);
        pre.setParams(osArgs);     
        dcf.initFlow(myFlow, dacx);
        DialogAndInProcessCmd daipc = dcf.stepTheFlow(pre);          
        if (daipc.state != DialogAndInProcessCmd.Progress.DONE) {
          System.err.println(rMan.getString("headless.btpInputFailure"));
          return (rMan.getString("headless.earlyExit"));
        }
        fileLoaded = true;
      }

      String csvFileName = (String)args_.get(ArgParser.CSV_BATCH_INPUT);
      if (csvFileName != null) {
        if (fileLoaded) {
          System.err.println(rMan.getString("headless.doubleInputFailure"));
          return (rMan.getString("headless.earlyExit"));
        }
        Boolean doCompress = (Boolean)args_.get(ArgParser.CSV_BATCH_COMPRESS);
        Object[] osArgs = new Object[7];
        osArgs[0] = doCompress;
        osArgs[1] = SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT;
        osArgs[2] = new Integer(NetOverlayProperties.RELAYOUT_NO_CHANGE);
        osArgs[3] = new Boolean(true);
        osArgs[4] = csvFileName; 
        osArgs[5] = null;
        osArgs[6] = null;
        BatchJobControlFlowHarness dcf = new BatchJobControlFlowHarness(appState_, null); 
        ControlFlow myFlow = flom.getControlFlow(FlowMeister.MainFlow.IMPORT_FULL_HIERARCHY_FROM_CSV, null);
        ImportCSV.StepState pre = (ImportCSV.StepState)myFlow.getEmptyStateForPreload(dacx);
        pre.setParams(osArgs);     
        dcf.initFlow(myFlow, dacx);
        DialogAndInProcessCmd daipc = dcf.stepTheFlow(pre);          
        if (daipc.state != DialogAndInProcessCmd.Progress.DONE) {
          System.err.println(rMan.getString("headless.csvInputFailure"));
          return (rMan.getString("headless.earlyExit"));
        }
        fileLoaded = true;
      }   

      if (!fileLoaded) {
        System.err.println(rMan.getString("headless.noInputFailure"));
        return (rMan.getString("headless.earlyExit"));
      }

      boolean aSuccess = false;

      String webDirectory = (String)args_.get(ArgParser.WEB_BATCH_OUTPUT);
      if (webDirectory != null) {
        Object[] osArgs = new Object[2];
        osArgs[0] = new Boolean(true);  // we are loading from directory...
        osArgs[1] = webDirectory;
        BatchJobControlFlowHarness dcf = new BatchJobControlFlowHarness(appState_, null); 
        ControlFlow myFlow = flom.getControlFlow(FlowMeister.MainFlow.WEB, null);
        ExportWeb.StepState pre = (ExportWeb.StepState)myFlow.getEmptyStateForPreload(dacx);
        pre.setParams(osArgs);     
        dcf.initFlow(myFlow, dacx);
        DialogAndInProcessCmd daipc = dcf.stepTheFlow(pre);          
        if (daipc.state != DialogAndInProcessCmd.Progress.DONE) {
          System.err.println(rMan.getString("headless.webExportFailure"));
        } else {
          aSuccess = true;
        }
      }    

      String imageFileName = (String)args_.get(ArgParser.IMAGE_BATCH_OUTPUT);
      if (imageFileName != null) {
        Object[] osArgs = imageExportPrepForFile(args_, imageFileName);
        if (osArgs != null) {
          BatchJobControlFlowHarness dcf = new BatchJobControlFlowHarness(appState_, null); 
          ControlFlow myFlow = flom.getControlFlow(FlowMeister.MainFlow.EXPORT, null);
          LoadSaveOps.StepState pre = (LoadSaveOps.StepState)myFlow.getEmptyStateForPreload(dacx);
          pre.setParams(osArgs);     
          dcf.initFlow(myFlow, dacx);
          DialogAndInProcessCmd daipc = dcf.stepTheFlow(pre);          
          if (daipc.state != DialogAndInProcessCmd.Progress.DONE) {
            System.err.println(rMan.getString("headless.webExportFailure"));
          } else {
            aSuccess = true;
          }
        } else {
          System.err.println(rMan.getString("headless.imageExportFailure"));
        }
      }       

      String btpFileName = (String)args_.get(ArgParser.BTP_BATCH_OUTPUT);
      if (btpFileName != null) {
        Object[] osArgs = new Object[2];
        osArgs[0] = new Boolean(true);
        osArgs[1] = btpFileName;
        BatchJobControlFlowHarness dcf = new BatchJobControlFlowHarness(appState_, null); 
        ControlFlow myFlow = flom.getControlFlow(FlowMeister.MainFlow.SAVE_AS, null);
        LoadSaveOps.StepState pre = (LoadSaveOps.StepState)myFlow.getEmptyStateForPreload(dacx);
        pre.setParams(osArgs);     
        dcf.initFlow(myFlow, dacx);
        DialogAndInProcessCmd daipc = dcf.stepTheFlow(pre);          
        if (daipc.state != DialogAndInProcessCmd.Progress.DONE) {
          System.err.println(rMan.getString("headless.btpExportFailure"));
        } else {
          aSuccess = true;
        }
      }       

      if (!aSuccess) {
        System.err.println(rMan.getString("headless.totalExportFailure"));
        return (rMan.getString("headless.earlyExit"));
      } 
    }
    return (null);
  }


 /***************************************************************************
  ** 
  ** Image export argument prep for stream output
  */  
  
  
  private Object[] imageExportPrepForStream(Map<String, Object> args, OutputStream stream) {           
    Object[] osArgs = new Object[3];
    osArgs[1] = new Boolean(false); // This is a stream
    osArgs[2] = stream;
    ExportPublish.ExportSettings settings = imageExportPrepGuts(args);
    osArgs[0] = settings;    
    return (osArgs);
  }  
 
  /***************************************************************************
  ** 
  ** Image export argument prep for file output
  */  
  
  
  private Object[] imageExportPrepForFile(Map<String, Object> args, String imageFileName) {
    Object[] osArgs = new Object[3];
    osArgs[1] = new Boolean(true); // This is a file
    osArgs[2] = imageFileName;         
    ExportPublish.ExportSettings settings = imageExportPrepGuts(args);
    osArgs[0] = settings;    
    return (osArgs);
  }  
  
  /***************************************************************************
  ** 
  ** Image export argument prep
  */  
   
  private ExportPublish.ExportSettings imageExportPrepGuts(Map<String, Object> args) {

    ResourceManager rMan = appState_.getRMan();
                       
    List suppRes = ImageExporter.getSupportedResolutions(false);
    List<String> suppForms = ImageExporter.getSupportedExports();    
    
    ExportPublish.ExportSettings settings = new ExportPublish.ExportSettings();
    settings.zoomVal = 1.0;
    
    // Make this an argument!
    if (suppForms.contains("PNG")) {
      settings.formatType = "PNG";
    } else {
      System.err.println(rMan.getString("headless.imageExportPrepFailure"));
      return (null);
    }
    
    if (ImageExporter.formatRequiresResolution(settings.formatType)) {
      List resList = ImageExporter.getSupportedResolutions(false);    
      if (resList.size() == 0) {
        throw new IllegalStateException();
      }
      Object[] resVal = (Object[])resList.get(0);
      ImageExporter.RationalNumber ratNum = (ImageExporter.RationalNumber)resVal[ImageExporter.CM];
      settings.res = new ImageExporter.ResolutionSettings();
      settings.res.dotsPerUnit = ratNum;
      settings.res.units = ImageExporter.CM;
    } else {
      settings.res = null;
    }
    
    appState_.getZoomTarget().setZoomFactor(settings.zoomVal);    
    Dimension dim = appState_.getZoomTarget().getBasicSize(true, true, ZoomTarget.VISIBLE_MODULES);   
    double currentZoomHeight = Math.round(((double)dim.height) * settings.zoomVal);
    double currentZoomWidth = Math.round(((double)dim.width) * settings.zoomVal);    
    settings.size = new Dimension((int)currentZoomWidth, (int)currentZoomHeight);    
    return (settings);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Thrown exceptions include a message as well as an optional wrapped
  ** exception from deep down in the program...
  */

  public static class GeneratorException extends Exception {
    
    private Exception wrapped_;
    private static final long serialVersionUID = 1L;
    
    GeneratorException(String message, Exception wrapped) {
      super(message);
      wrapped_ = wrapped;
    }
    
    GeneratorException(String message) {
      super(message);
      wrapped_ = null;
    }
   
    public Exception getWrappedException() {
      return (wrapped_);
    }
  }  
    
}