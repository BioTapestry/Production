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

package org.systemsbiology.biotapestry.ui.dialogs;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTTransmitResultsDialog;
import org.systemsbiology.biotapestry.ui.dialogs.utils.TimeAxisHelper;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;

/****************************************************************************
**
** Factory for dialog boxes for creating new genome instance models
*/

public class GenomeInstanceCreationDialogFactory extends DialogFactory {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public GenomeInstanceCreationDialogFactory(ServerControlFlowHarness cfh) {
    super(cfh);
  }  
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // FACTORY METHOD
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Get the appropriate dialog
  */  
  
  public ServerControlFlowHarness.Dialog getDialog(DialogBuildArgs ba) { 
   
    GenomeInstanceBuildArgs dniba = (GenomeInstanceBuildArgs)ba;
  
    if (platform.getPlatform() == DialogPlatform.Plat.DESKTOP) {
      return (new DesktopDialog(cfh, dniba.defaultName, dniba.timeBounded, 
                                dniba.minTime, dniba.maxTime, dniba.suggested));
    } else if (platform.getPlatform() == DialogPlatform.Plat.WEB) {
      throw new IllegalStateException();
    }
    throw new IllegalArgumentException();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // COMMON HELPERS
  //
  ////////////////////////////////////////////////////////////////////////////      

  ////////////////////////////////////////////////////////////////////////////
  //
  // BUILD ARGUMENT CLASS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  public static class GenomeInstanceBuildArgs extends DialogBuildArgs { 
    
    String defaultName; 
    boolean timeBounded; 
    int minTime; 
    int maxTime; 
    boolean suggested;
          
    public GenomeInstanceBuildArgs(String defaultName, boolean timeBounded, 
                                   int minTime, int maxTime, boolean suggested) {
      super(null);
      this.defaultName = defaultName;
      this.timeBounded = timeBounded;
      this.minTime = minTime;
      this.maxTime = maxTime;
      this.suggested = suggested;
    } 
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // DIALOG CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  public static class DesktopDialog extends BTTransmitResultsDialog { 
 
    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE INSTANCE MEMBERS
    //
    ////////////////////////////////////////////////////////////////////////////  
  
    private JTextField nameField_;
    private JTextField minField_;
    private JTextField maxField_;  
    private JLabel minFieldLabel_;
    private JLabel maxFieldLabel_;    
    private JCheckBox timeBoundsBox_;
    
    private int minTime_;
    private int maxTime_;
    private boolean timeBounded_;
    private boolean suggested_;
    private TimeAxisHelper timeAxisHelper_;
    private DataAccessContext dacx_;
    
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
    
    public DesktopDialog(ServerControlFlowHarness cfh, String defaultName, boolean timeBounded, 
                         int minTime, int maxTime, boolean suggested) { 
      super(cfh, "gicreate.title", new Dimension(500, 200), 4, new GenomeInstanceCreationRequest(), false);
      minTime_ = minTime;
      maxTime_ = maxTime;
      timeBounded_ = timeBounded;
      suggested_ = suggested;
      dacx_ = cfh.getDataAccessContext();
   
      //
      // Build the name panel:
      //
  
      JLabel label = new JLabel(rMan_.getString("gicreate.name"));
      nameField_ = new JTextField(defaultName);
      addLabeledWidget(label, nameField_, false, true);
      
      nameField_.selectAll();  
      addWindowListener(new WindowAdapter() {
        public void windowOpened(WindowEvent e) {
          nameField_.requestFocus();
        }
      });    
          
      //
      // Time bounds box and times.  If we are forced, we cannot change our bounding.
      //
      
      timeBoundsBox_ = new JCheckBox(rMan_.getString("gicreate.timeBounds"));
      timeBoundsBox_.setSelected(timeBounded_);
      if (!suggested_) {
        timeBoundsBox_.setEnabled(false);
      }    
      addWidgetFullRow(timeBoundsBox_, true, true);
      
      timeBoundsBox_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            boolean enabled = timeBoundsBox_.isSelected();
            if (enabled && !timeAxisHelper_.establishTimeAxis()) {          
              timeBoundsBox_.setSelected(false);  // gonna cause a callback!
              return;
            }
            minField_.setEnabled(enabled);
            minFieldLabel_.setEnabled(enabled);
            maxField_.setEnabled(enabled);
            maxFieldLabel_.setEnabled(enabled);
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });
  
      //
      // Build the minimum and maxiumum  time:
      //
  
      minFieldLabel_ = new JLabel("");
      maxFieldLabel_ = new JLabel(""); 
      
      timeAxisHelper_ = new TimeAxisHelper(appState_, dacx_, this, minFieldLabel_, maxFieldLabel_);    
      timeAxisHelper_.fixMinMaxLabels(false);
      
      minField_ = (timeBounded_) ? new JTextField(timeAxisHelper_.timeValToDisplay(minTime_)) : new JTextField();
      minFieldLabel_.setEnabled(timeBounded_);
      minField_.setEnabled(timeBounded_); 
   
      maxField_ = (timeBounded_) ? new JTextField(timeAxisHelper_.timeValToDisplay(maxTime_)) : new JTextField();    
      maxFieldLabel_.setEnabled(timeBounded_);
      maxField_.setEnabled(timeBounded_);
      
      addTwoTrueLabeledWidgets(minFieldLabel_, minField_, maxFieldLabel_, maxField_, true);
       
      finishConstruction();
    }
  
    ////////////////////////////////////////////////////////////////////////////
    //
    // PROTECTED METHODS
    //
    ////////////////////////////////////////////////////////////////////////////
  
    /***************************************************************************
    **
    ** Do the bundle 
    ** 
    */
      
    @Override   
    protected boolean bundleForExit(boolean forApply) {
       GenomeInstanceCreationRequest nocrq = (GenomeInstanceCreationRequest)request_;           
       nocrq.newName = nameField_.getText().trim();
       nocrq.timeBounded = timeBoundsBox_.isSelected();
       // These two checks are obsolete, now that we do not enable the
       // checkbox if it is not suggested.  Keep around to be safe:
       if (!nocrq.timeBounded && timeBounded_ && !suggested_) {
          String message = rMan_.getString("gicreate.mustBeBounded"); 
         String title = rMan_.getString("gicreate.mustBeBoundedTitle");
         SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, message, title);
         cfh_.showSimpleUserFeedback(suf);
         return (false);
       } else if (nocrq.timeBounded && !timeBounded_ && !suggested_) {
         String message = rMan_.getString("gicreate.mustNotBeBounded"); 
         String title = rMan_.getString("gicreate.mustNotBeBoundedTitle");
         SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, message, title);
         cfh_.showSimpleUserFeedback(suf);
         return (false);      
       }
       if (nocrq.timeBounded) {
         nocrq.minTime = timeAxisHelper_.timeDisplayToIndex(minField_.getText());
         if (nocrq.minTime == TimeAxisDefinition.INVALID_STAGE_NAME) {
           return (false);
         }
         nocrq.maxTime = timeAxisHelper_.timeDisplayToIndex(maxField_.getText());
         if (nocrq.maxTime == TimeAxisDefinition.INVALID_STAGE_NAME) {
           return (false);
         }
         if ((!suggested_ && (minTime_ != -1) && (nocrq.minTime < minTime_)) ||
             (!suggested_ && (minTime_ != -1) && (nocrq.maxTime > maxTime_)) || 
             (nocrq.minTime > nocrq.maxTime) || (nocrq.minTime < 0)) {
           String message = rMan_.getString("gicreate.badBounds"); 
           String title = rMan_.getString("gicreate.badBoundsTitle");
           SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, message, title);
           cfh_.showSimpleUserFeedback(suf);   
           return (false);      
         }
       } else {
         nocrq.minTime = TimeAxisDefinition.INVALID_STAGE_NAME;      
         nocrq.maxTime = TimeAxisDefinition.INVALID_STAGE_NAME;         
       }
       nocrq.haveResult = true;
       return (true);
    }
  }
 
  /***************************************************************************
  **
  ** Return Results
  ** 
  */

  public static class GenomeInstanceCreationRequest implements ServerControlFlowHarness.UserInputs {
   
    public String newName;
    public boolean timeBounded; 
    public int minTime;
    public int maxTime;
    private boolean haveResult;

    public void clearHaveResults() {
      haveResult = false;
      return;
    }   
    public boolean haveResults() {
      return (haveResult);
    }  
    public boolean isForApply() {
      return (false);
    }
  }
}
