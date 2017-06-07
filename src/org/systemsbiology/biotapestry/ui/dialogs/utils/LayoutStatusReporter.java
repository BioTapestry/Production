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


package org.systemsbiology.biotapestry.ui.dialogs.utils;

import java.text.MessageFormat;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.ui.ColorAssigner;
import org.systemsbiology.biotapestry.ui.LinkRouter;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Collection of commands for tools
*/

public class LayoutStatusReporter {
                                                             
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private DataAccessContext dacx_;
  private UIComponentSource uics_;
  private LinkRouter.RoutingResult layoutResult_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public LayoutStatusReporter(UIComponentSource uics, DataAccessContext dacx, LinkRouter.RoutingResult layoutResult) {
    uics_ = uics;
    dacx_ = dacx;
    layoutResult_ = layoutResult;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** make layout status announcements
  */  
 
  public void doStatusAnnouncements() {    

    //
    // Make status announcements
    //

    boolean isHeadless = uics_.isHeadless();
    
    if ((layoutResult_.linkResult & LinkRouter.COLOR_PROBLEM) != 0x00) {
      ResourceManager rMan = dacx_.getRMan();
      if ((layoutResult_.colorResult & ColorAssigner.COLOR_COLLISION) != 0x00) {
        String src1 = ""; 
        String src2 = "";
        if (layoutResult_.collisionSrc1 != null) {
          src1 = layoutResult_.collisionSrc1;
        }
        if (layoutResult_.collisionSrc2 != null) {
          src2 = layoutResult_.collisionSrc2;
        }
        if (src1.trim().equals("")) {
          src1 = rMan.getString("tip.noname");
        }
        if (src2.trim().equals("")) {
          src2 = rMan.getString("tip.noname");
        }        
        String format = rMan.getString("autoLayout.colorProblemCollide");
        String srcMsg = MessageFormat.format(format, new Object[] {src1, src2});
        if (isHeadless) {
          System.err.println(srcMsg);
        } else {
          srcMsg = UiUtil.convertMessageToHtml(srcMsg);
          JOptionPane.showMessageDialog(uics_.getTopFrame(), srcMsg,
                                        rMan.getString("autoLayout.colorProblemTitle"),
                                        JOptionPane.WARNING_MESSAGE);
        }
      }
      if ((layoutResult_.colorResult & ColorAssigner.COLOR_REASSIGNMENT) != 0x00) {
        if (isHeadless) {
          System.err.println(rMan.getString("autoLayout.colorProblemReassign"));
        } else {
          JOptionPane.showMessageDialog(uics_.getTopFrame(),
                                        rMan.getString("autoLayout.colorProblemReassign"),
                                        rMan.getString("autoLayout.colorProblemTitle"),
                                        JOptionPane.WARNING_MESSAGE);
        }
      }
      if ((layoutResult_.colorResult & ColorAssigner.TOO_FEW_COLORS) != 0x00) {
        if (isHeadless) {
          System.err.println(rMan.getString("autoLayout.colorProblemTooFew"));
        } else {
          JOptionPane.showMessageDialog(uics_.getTopFrame(),
                                        rMan.getString("autoLayout.colorProblemTooFew"),
                                        rMan.getString("autoLayout.colorProblemTitle"),
                                        JOptionPane.WARNING_MESSAGE);
        }
      }
    }
    
    int allLayoutProblems = LinkRouter.LAYOUT_PROBLEM | LinkRouter.LAYOUT_COULD_NOT_PROCEED | LinkRouter.LAYOUT_OVERLAID_SUBSET;
    
    if ((layoutResult_.linkResult & allLayoutProblems) != 0x00) {
      ResourceManager rMan = dacx_.getRMan(); 
      if (isHeadless) {
        System.err.println(rMan.getString("autoLayout.layoutProblem"));  
      } else {
        String msg = "";
        if ((layoutResult_.linkResult & LinkRouter.LAYOUT_PROBLEM) != 0x00) {
          msg = rMan.getString("autoLayout.layoutProblem");
        } else if ((layoutResult_.linkResult & LinkRouter.LAYOUT_COULD_NOT_PROCEED) != 0x00) {
          msg = rMan.getString("autoLayout.layoutProblemHalted");                     
        } else if ((layoutResult_.linkResult & LinkRouter.LAYOUT_OVERLAID_SUBSET) != 0x00) {
          msg = rMan.getString("autoLayout.layoutProblemOverlaid");
        }
        msg = UiUtil.convertMessageToHtml(msg);
        JOptionPane.showMessageDialog(uics_.getTopFrame(), msg,                                    
                                      rMan.getString("autoLayout.layoutProblemTitle"),
                                      JOptionPane.WARNING_MESSAGE);
      }
    }  
    return;
  }
}
