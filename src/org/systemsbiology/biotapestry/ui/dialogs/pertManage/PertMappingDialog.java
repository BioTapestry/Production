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
package org.systemsbiology.biotapestry.ui.dialogs.pertManage;


import java.awt.Dimension;
import java.util.List;
import java.text.MessageFormat;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTStashResultsDialog;

/****************************************************************************
 **
 ** Dialog box for setting up perturbation mapping.  Unlike previous QPCR
 ** mapping dialog, we don't need to be able to add entries, since we don't
 ** need to be able to create a target gene entry before editing it!
 */

public class PertMappingDialog extends BTStashResultsDialog {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private PertMappingPanel pmp_;
  
  private static final long serialVersionUID = 1L;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
   **
   ** Constructor 
   */
  
  public PertMappingDialog(UIComponentSource uics, PerturbationData pd, String nodeName, List currEntries, List currSources) {
    super(uics, "", new Dimension(700, 500), 1);
    String format = rMan_.getString("pertMapping.title");
    String desc = MessageFormat.format(format, new Object[]{nodeName});
    setTitle(desc);   
    pmp_ = new PertMappingPanel(uics, pd, nodeName, currEntries, currSources, false, false);
    addTable(pmp_, 6);
    finishConstruction();   
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Get the target list
  */
  
  public List getEntryList() {
    return (pmp_.getEntryList());
  }

  /***************************************************************************
  **
  ** Get the source list
  */
  
  public List getSourceList() {
    return (pmp_.getSourceList());
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED/PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Does the processing of the result
  ** 
  */
  
  protected boolean stashForOK() {
    return (pmp_.stashForOKSupport());
  }
}
