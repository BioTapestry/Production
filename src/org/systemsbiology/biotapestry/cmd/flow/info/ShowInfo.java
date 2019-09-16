/*
**    Copyright (C) 2003-2013 Institute for Systems Biology 
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


package org.systemsbiology.biotapestry.cmd.flow.info;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Handle some info providers.
*/

public class ShowInfo extends AbstractControlFlow {

  /***************************************************************************
  **
  ** Possible actions we can perform
  */ 
   
  public enum InfoType {
    HELP("command.HelpBrowser", "command.HelpBrowser", "FIXME24.gif", "command.HelpBrowserMnem", null),
    ABOUT("command.About", "command.About", "About24.gif", "command.AboutMnem", null),
    COUNTS("command.ModelCounts", "command.ModelCounts", "FIXME24.gif", "command.ModelCountsMnem", null),
    ;
    
    private String name_;
    private String desc_;
    private String icon_;
    private String mnem_;
    private String accel_;
     
    InfoType(String name, String desc, String icon, String mnem, String accel) {
      this.name_ = name;  
      this.desc_ = desc;
      this.icon_ = icon;
      this.mnem_ = mnem;
      this.accel_ = accel;
    }  
     
    public String getName() {
      return (name_);      
    }
     
    public String getDesc() {
      return (desc_);      
    }
     
    public String getIcon() {
      return (icon_);      
    }
     
    public String getMnem() {
      return (mnem_);      
    }
     
    public String getAccel() {
      return (accel_);      
    }     
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  private InfoType action_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public ShowInfo(BTState appState, InfoType action) {
    super(appState);
    name =  action.getName();
    desc =  action.getDesc();
    icon =  action.getIcon();
    mnem =  action.getMnem();
    accel =  action.getAccel();
    action_ = action;
  }
  
  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
  
  @Override  
  public boolean isEnabled(CheckGutsCache cache) {
    switch (action_) {
      case HELP:
      case ABOUT:
        return (true);
      case COUNTS:
        return (cache.genomeNotNull());
      default:
        throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
    
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
        ans = new StepState(appState_, action_);
      } else {
        ans = (StepState)last.currStateX;
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
        
  public static class StepState implements DialogAndInProcessCmd.CmdState {

    private String nextStep_;
    private InfoType myAction_;
    private BTState appState_;
    
    private URL aboutURL_;
    private JEditorPane pane_;
    private JFrame frame_;
    private FixedJButton buttonB_;
    private URL gnuUrl_;
    private URL sunUrl_;
    private URL l4jUrl_;
    private URL nsisUrl_;
    private URL apbUrl_ ;
    private URL jdkUrl_;
    private URL aliUrl_;
    private URL aexUrl_; 

    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, InfoType action) {
      myAction_ = action;
      appState_ = appState;
      nextStep_ = "stepToProcess";
      aboutURL_ = appState_.getMainCmds().getClass().getResource("/org/systemsbiology/biotapestry/data/about.html");
    }
     
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToProcess() {       
      switch (myAction_) {
        case COUNTS:      
          showModelCounts();
          break;  
        case HELP:      
          howHelp();
          break; 
        case ABOUT:      
          howAbout();
          break; 
        default:
          throw new IllegalStateException();
      }
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
    
    /***************************************************************************
    **
    ** Do counts
    */
    
    private void showModelCounts() {
      String key = appState_.getGenome();
      Genome genome = appState_.getDB().getGenome(key);
      int numGenes = 0;
      int numRest = 0;
      int numLinks = 0;
      if (genome != null) {
        numGenes = genome.getGeneCount();
        int numAllNodes = genome.getFullNodeCount();
        numRest = numAllNodes - numGenes;
        numLinks = genome.getLinkageCount();
      }
      ResourceManager rMan = appState_.getRMan();        
      String desc = MessageFormat.format(rMan.getString("modelCounts.message"), 
                                         new Object[] {new Integer(numGenes), 
                                                       new Integer(numRest), 
                                                       new Integer(numLinks)});  
      desc = UiUtil.convertMessageToHtml(desc);
      JOptionPane.showMessageDialog(appState_.getTopFrame(), desc,
                                    rMan.getString("modelCounts.modelCountTitle"),
                                    JOptionPane.INFORMATION_MESSAGE);        
      return;
    }
 
    /***************************************************************************
    **
    ** Command
    */ 
      
    private void howHelp() {
        ResourceManager rMan = appState_.getRMan();
        JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                      UiUtil.convertMessageToHtml(rMan.getString("helpMsg.howToFindHelp")),
                                      rMan.getString("helpMsg.howToFindHelpTitle"),
                                      JOptionPane.WARNING_MESSAGE);
      return;
    }
  
    /***************************************************************************
    **
    ** Command
    */

    //
    // Having an image link in the html turns out to be problematic
    // starting Fall 2008 with URL security holes being plugged.  So
    // change the window.  Note we use a back button now too!
    
    private void howAbout() {
      if (frame_ != null) {      
        frame_.setExtendedState(JFrame.NORMAL);
        frame_.toFront();
        return;
      }
      try {
        pane_ = new JEditorPane(aboutURL_);
      } catch (IOException ioex) {
        return;
      }
      // 8/09: COMPLETELY BOGUS, but URLs are breaking everywhere in the latest JVMs, and I don't
      // have time to fix this in a more elegant fashion! 9/19: Same thing!
      gnuUrl_ = appState_.getMainCmds().getClass().getResource("/org/systemsbiology/biotapestry/data/licenses/LICENSE.txt");
      sunUrl_ = appState_.getMainCmds().getClass().getResource("/org/systemsbiology/biotapestry/data/licenses/LICENSE-SUN.txt");
      l4jUrl_ = appState_.getMainCmds().getClass().getResource("/org/systemsbiology/biotapestry/data/licenses/launch4j-head-LICENSE.txt");
      nsisUrl_ = appState_.getMainCmds().getClass().getResource("/org/systemsbiology/biotapestry/data/licenses/NSIS-COPYING.txt");
      apbUrl_ = appState_.getMainCmds().getClass().getResource("/org/systemsbiology/biotapestry/data/licenses/LICENSE-APPBUNDLER.txt");
      jdkUrl_ = appState_.getMainCmds().getClass().getResource("/org/systemsbiology/biotapestry/data/licenses/LICENSE-JDK.txt");
      aliUrl_ = appState_.getMainCmds().getClass().getResource("/org/systemsbiology/biotapestry/data/licenses/ADDITIONAL_LICENSE_INFO.txt");
      aexUrl_ = appState_.getMainCmds().getClass().getResource("/org/systemsbiology/biotapestry/data/licenses/ASSEMBLY_EXCEPTION.txt");  
      
      ResourceManager rMan = appState_.getRMan();
      pane_.setEditable(false);
      frame_ = new JFrame(rMan.getString("window.aboutTitle"));
      pane_.addHyperlinkListener(new HyperlinkListener() {
        public void hyperlinkUpdate(HyperlinkEvent ev) {
          try {
            if (ev.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
              URL toUse;
              if (ev.getDescription().indexOf("-SUN") != -1) {
                toUse = sunUrl_;
              } else if (ev.getDescription().indexOf("launch4j-") != -1) {
                toUse = l4jUrl_;
              } else if (ev.getDescription().indexOf("NSIS") != -1) {
                toUse = nsisUrl_;
              } else if (ev.getDescription().indexOf("APPBUNDLER") != -1) {
                toUse = apbUrl_;
              } else if (ev.getDescription().indexOf("JDK") != -1) {
                toUse = jdkUrl_;
              } else if (ev.getDescription().indexOf("ADDITIONAL") != -1) {
                toUse = aliUrl_;
              } else if (ev.getDescription().indexOf("ASSEMBLY") != -1) {
                toUse = aexUrl_;
              } else if (ev.getDescription().indexOf("LICENSE.txt") != -1) {
                toUse = gnuUrl_;  
              } else {
                throw new IllegalArgumentException();
              }
           
              pane_.setPage(toUse);
              buttonB_.setEnabled(true);
            }
          } catch (IOException ex) {
          }
        }
      });
      frame_.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          frame_ = null;
          e.getWindow().dispose();
        }
      });
             
      JPanel cp = (JPanel)frame_.getContentPane();
      cp.setBackground(Color.white);   
      cp.setBorder(new EmptyBorder(20, 20, 20, 20));
      cp.setLayout(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();    
      URL sugif = appState_.getMainCmds().getClass().getResource("/org/systemsbiology/biotapestry/images/BioTapestrySplash.gif");
      JLabel label = new JLabel(new ImageIcon(sugif));        
      
      UiUtil.gbcSet(gbc, 0, 0, 1, 3, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
      cp.add(label, gbc);
      
      JScrollPane jsp = new JScrollPane(pane_);
      UiUtil.gbcSet(gbc, 0, 3, 1, 2, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
      cp.add(jsp, gbc);
      
      
      buttonB_ = new FixedJButton(rMan.getString("dialogs.back"));
      buttonB_.setEnabled(false);
      buttonB_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            pane_.setPage(aboutURL_);
            buttonB_.setEnabled(false);
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });     
      FixedJButton buttonC = new FixedJButton(rMan.getString("dialogs.close"));
      buttonC.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            frame_.setVisible(false);
            frame_.dispose();
            frame_ = null;
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });
      Box buttonPanel = Box.createHorizontalBox();
      buttonPanel.add(Box.createHorizontalGlue()); 
      buttonPanel.add(buttonB_);
      buttonPanel.add(Box.createHorizontalStrut(10));    
      buttonPanel.add(buttonC);
      UiUtil.gbcSet(gbc, 0, 5, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
      cp.add(buttonPanel, gbc);        
      frame_.setSize(700, 700);
      frame_.setLocationRelativeTo(appState_.getTopFrame());
      frame_.setVisible(true);
      return;
    }
  }
}
