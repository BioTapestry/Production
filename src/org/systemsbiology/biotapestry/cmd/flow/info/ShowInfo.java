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
import java.util.Set;
import java.util.TreeMap;

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
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.Metabase;
import org.systemsbiology.biotapestry.db.TabNameData;
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
    DATA_SHARING("command.DataSharingInfo", "command.DataSharingInfo", "FIXME24.gif", "command.DataSharingInfoMnem", null),
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
  private BTState appState_;
  
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
    appState_ = appState;
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
      case DATA_SHARING:
        return ((new StaticDataAccessContext(appState_)).getMetabase().amSharingExperimentalData());       
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
        ans = new StepState(action_, cfh);
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

    private InfoType myAction_;
    
    private URL aboutURL_;
    private JEditorPane pane_;
    private JFrame frame_;
    private FixedJButton buttonB_;
    private URL gnuUrl_;
    private URL sunUrl_;
    private URL l4jUrl_;
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(InfoType action, ServerControlFlowHarness cfh) {
      super(cfh);
      myAction_ = action;
      nextStep_ = "stepToProcess";
      aboutURL_ = cmdSrc_.getMainCmds().getClass().getResource("/org/systemsbiology/biotapestry/data/about.html");
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
        case DATA_SHARING:      
          showDataSharing();
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
      Genome genome = dacx_.getCurrentGenome();
      int numGenes = 0;
      int numRest = 0;
      int numLinks = 0;
      if (genome != null) {
        numGenes = genome.getGeneCount();
        int numAllNodes = genome.getFullNodeCount();
        numRest = numAllNodes - numGenes;
        numLinks = genome.getLinkageCount();
      }
      ResourceManager rMan = dacx_.getRMan();        
      String desc = MessageFormat.format(rMan.getString("modelCounts.message"), 
                                         new Object[] {new Integer(numGenes), 
                                                       new Integer(numRest), 
                                                       new Integer(numLinks)});  
      desc = UiUtil.convertMessageToHtml(desc);
      JOptionPane.showMessageDialog(uics_.getTopFrame(), desc,
                                    rMan.getString("modelCounts.modelCountTitle"),
                                    JOptionPane.INFORMATION_MESSAGE);        
      return;
    }
    
    /***************************************************************************
    **
    ** Show the data sharing status
    */
    
    private void showDataSharing() {
       
      String showText = showDataSharingText(dacx_, tSrc_);
      ResourceManager rMan = dacx_.getRMan();
      JOptionPane.showMessageDialog(uics_.getTopFrame(), showText,
                                    rMan.getString("dataSharingInfo.windowTitle"),
                                    JOptionPane.INFORMATION_MESSAGE);        
      return;
    }
 
    /***************************************************************************
    **
    ** Command
    */ 
      
    private void howHelp() {
        ResourceManager rMan = dacx_.getRMan();
        JOptionPane.showMessageDialog(uics_.getTopFrame(), 
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
      // 8/09: COMPLETELY BOGUS, but URLs are breaking everywhere in the latest JVMs, an I don't
      // have time to fix this in a more elegant fashion!
      gnuUrl_ = cmdSrc_.getMainCmds().getClass().getResource("/org/systemsbiology/biotapestry/data/licenses/LICENSE");
      sunUrl_ = cmdSrc_.getMainCmds().getClass().getResource("/org/systemsbiology/biotapestry/data/licenses/LICENSE-SUN");
      l4jUrl_ = cmdSrc_.getMainCmds().getClass().getResource("/org/systemsbiology/biotapestry/data/licenses/launch4j-head-LICENSE.txt");
      ResourceManager rMan = dacx_.getRMan();
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
              } else {
                toUse = gnuUrl_;
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
      URL sugif = cmdSrc_.getMainCmds().getClass().getResource("/org/systemsbiology/biotapestry/images/BioTapestrySplash.gif");
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
            uics_.getExceptionHandler().displayException(ex);
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
            uics_.getExceptionHandler().displayException(ex);
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
      frame_.setLocationRelativeTo(uics_.getTopFrame());
      frame_.setVisible(true);
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Shared method for shared data display text
  */
  
  public static String showDataSharingText(DataAccessContext dacx, TabSource tSrc) {
    
    Metabase mb = dacx.getMetabase();
    Metabase.DataSharingPolicy dsp = mb.getDataSharingPolicy();
    boolean isSharing = dsp.isSpecifyingSharing();     
    
    ResourceManager rMan = dacx.getRMan();
    
    StringBuffer buf = new StringBuffer();
    buf.append("<html>");
    buf.append(rMan.getString("dataSharingInfo.modelsSharingData"));
    buf.append("<ul>");
      
    TreeMap<Integer, String> ordered = new TreeMap<Integer, String>();
    
    if (isSharing) {
      Set<String> sharDB = mb.tabsSharingData();
      for (String dbID : sharDB) {
        int indx = tSrc.getTabIndexFromId(dbID); 
        TabNameData tnd = mb.getDB(dbID).getTabNameData();
        ordered.put(Integer.valueOf(indx), tnd.getTitle());
      }     
      for (String modName : ordered.values()) { 
        buf.append("<li>");
        buf.append(modName);
        buf.append("</li>");
      }
    }
    buf.append("</ul><br/>");

    StringBuffer sbuf = new StringBuffer();
    sbuf.append(rMan.getString("dataSharingInfo.dataShared"));
    sbuf.append("<ul>");
    int sbufStrt = sbuf.length();
    
    StringBuffer nbuf = new StringBuffer();
    nbuf.append(rMan.getString("dataSharingInfo.dataNotShared"));
    nbuf.append("<ul>");
    int nbufStrt = nbuf.length();
    
    StringBuffer useBuf = (dsp.shareTimeUnits) ? sbuf : nbuf;
    useBuf.append("<li>");
    useBuf.append(rMan.getString("dataSharingInfo.sharingTimeUnits"));
    useBuf.append("</li>");
      
    useBuf = (dsp.sharePerts) ? sbuf : nbuf;
    useBuf.append("<li>");
    useBuf.append(rMan.getString("dataSharingInfo.sharingPerturbationData"));
    useBuf.append("</li>");

    useBuf = (dsp.shareTimeCourses) ? sbuf : nbuf;
    useBuf.append("<li>");
    useBuf.append(rMan.getString("dataSharingInfo.sharingTimeCourseData"));
    useBuf.append("</li>");
      
    useBuf = (dsp.sharePerEmbryoCounts) ? sbuf : nbuf;
    useBuf.append("<li>");
    useBuf.append(rMan.getString("dataSharingInfo.sharingPerEmbryoCounts"));
    useBuf.append("</li>");
    
    StringBuffer comboBuf = new StringBuffer(); 
    comboBuf.append(buf.toString());
    
    if (sbuf.length() > sbufStrt) {
      comboBuf.append(sbuf.toString());
      comboBuf.append("</ul><br/>");
    }
    if (nbuf.length() > nbufStrt) {
      comboBuf.append(nbuf.toString());
      comboBuf.append("</ul><br/>");
    }
    comboBuf.append("</html>");  
    return (comboBuf.toString());
  }

}
