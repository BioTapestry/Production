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


package org.systemsbiology.biotapestry.cmd.flow;

import org.systemsbiology.biotapestry.app.CmdSource;
import org.systemsbiology.biotapestry.app.PathAndFileSource;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.util.UndoFactory;

/****************************************************************************
**
** Base class to extend for implementing StepState.
*/

public abstract class AbstractStepState implements DialogAndInProcessCmd.CmdState {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  protected StaticDataAccessContext dacx_;
  protected ServerControlFlowHarness cfh_;
  protected TabSource tSrc_;
  protected CmdSource cmdSrc_;
  protected UndoFactory uFac_;
  protected UIComponentSource uics_;
  protected String nextStep_;
  protected HarnessBuilder hBld_;
  protected PathAndFileSource pafs_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor
  */
     
  protected AbstractStepState(ServerControlFlowHarness cfh) {
    cfh_ = cfh;
    uFac_ = cfh.getUndoFactory();
    dacx_ = cfh.getDataAccessContext();
    uics_ = cfh.getUI();
    tSrc_ = cfh.getTabSource();
    cmdSrc_ = cfh.getCmdSource();
    hBld_ = cfh.getHarnessBuilder();
    pafs_ = cfh.getPathAndFileSource();
    
  }

  /***************************************************************************
  **
  ** Constructor. Gotta call stockCfhIfNeeded() before using!
  */
     
  protected AbstractStepState(StaticDataAccessContext dacx) {
    cfh_ = null;
    uFac_ = null;
    dacx_ = dacx;
    uics_ = null;
    tSrc_ = null;
    cmdSrc_ = null;
    hBld_ = null;
    pafs_ = null;
  } 
 
  /***************************************************************************
  **
  ** Get DACX
  */
     
  public StaticDataAccessContext getDACX() {
    return (dacx_);
  } 
  
  /***************************************************************************
  **
  ** Get DACX
  */
     
  public CmdSource getCmdSource() {
    return (cmdSrc_);
  } 

  /***************************************************************************
  **
  ** step thru
  */
  
  public String getNextStep() {
    return (nextStep_);
  } 
 
  /***************************************************************************
  **
  ** Set the next step
  */
  
  public void setNextStep(String step) {
    nextStep_ = step;
    return;
  } 
  
  /***************************************************************************
  **
  ** Add cfh in if StepState was pre-built
  */
     
  public void stockCfhIfNeeded(ServerControlFlowHarness cfh) {
    if (cfh_ != null) {
      return;
    }
    cfh_ = cfh;
    uFac_ = cfh.getUndoFactory();
    uics_ = cfh.getUI();
    tSrc_ = cfh.getTabSource();
    cmdSrc_ = cfh.getCmdSource();
    hBld_ = cfh.getHarnessBuilder();
    pafs_ = cfh.getPathAndFileSource();
    if (dacx_ == null) {
      dacx_ = cfh.getDataAccessContext();
    }
    return;
  }
}
