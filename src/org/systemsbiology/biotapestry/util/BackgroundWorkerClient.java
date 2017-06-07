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

package org.systemsbiology.biotapestry.util;


import java.awt.Container;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.SortedMap;
import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;

/****************************************************************************
**
** Class to support running background threads.  It handles all the
** thread launching, UI disable/enable, progress dialog, cancel, and cleanup operations.
*/

public class BackgroundWorkerClient {
  
  private JDialog progressDialog_;
  private GoodnessChart chart_;
  private JProgressBar progressBar_;
  private BackgroundWorker worker_;
  private BackgroundWorkerOwner owner_;
  private boolean done_;
  private String waitTitle_;
  private String waitMsg_;
  private BackgroundWorkerControlManager suw_;
  private JFrame topWindow_;
  private UndoSupport support_;
  private boolean allowCancels_;
  private boolean cancelRequested_;
  private JLabel cancellingMessage_;
  private FixedJButton cancelButton_;
  private boolean isHeadless_;
  private UIComponentSource uics_;
  private ResourceManager rMan_;

  //
  // For alternate top Frames
  //
  
  public BackgroundWorkerClient(UIComponentSource uics, JFrame topFrame, 
                                DataAccessContext dacx, BackgroundWorkerOwner owner, 
                                BackgroundWorker worker, String waitTitle, 
                                String waitMsg, UndoSupport support, boolean allowCancels) {
     
    
    done_ = false;
    uics_ = uics;
    rMan_ = dacx.getRMan();
    worker_ = worker;
    owner_ = owner;
    waitTitle_ = waitTitle;
    waitMsg_ = waitMsg;
    suw_ = uics_.getCommonView();
    topWindow_ = topFrame;
    support_ = support;
    allowCancels_ = allowCancels;
    cancelRequested_ = false;
    isHeadless_ = false;
    chart_ = null;
  }
  
  //
  // For alternate background control
  //
  
  public BackgroundWorkerClient(UIComponentSource uics, DataAccessContext dacx, 
                                BackgroundWorkerControlManager bwcm, BackgroundWorkerOwner owner, 
                                BackgroundWorker worker, String waitTitle, 
                                String waitMsg, UndoSupport support, boolean allowCancels) {
     
    uics_ = uics;
    rMan_ = dacx.getRMan();
    done_ = false;
    worker_ = worker;
    owner_ = owner;
    waitTitle_ = waitTitle;
    waitMsg_ = waitMsg;
    suw_ = bwcm;
    topWindow_ = uics_.getTopFrame();
    support_ = support;
    allowCancels_ = allowCancels;
    cancelRequested_ = false;
    isHeadless_ = false;
    chart_ = null;
  }
 
  //
  // The usual version
  //
  
  public BackgroundWorkerClient(UIComponentSource uics, DataAccessContext dacx, BackgroundWorkerOwner owner, 
                                BackgroundWorker worker, String waitTitle, 
                                String waitMsg, UndoSupport support, boolean allowCancels) {
    this(uics, uics.getTopFrame(), dacx, owner, worker, waitTitle, waitMsg, support, allowCancels);
  }
  
  public void makeSuperChart() {
    chart_ = new GoodnessChart();
  }

  //
  // For headless operation ON THE CALLING THREAD
  //
  
  public BackgroundWorkerClient(UIComponentSource uics, DataAccessContext dacx, BackgroundWorkerOwner owner, BackgroundWorker worker, UndoSupport support) {
    
    done_ = false;
    uics_ = uics;
    rMan_ = dacx.getRMan();
    worker_ = worker;
    owner_ = owner;
    waitTitle_ = null;
    waitMsg_ = null;
    suw_ = null;
    topWindow_ = null;
    support_ = support;
    allowCancels_ = false;
    cancelRequested_ = false;
    isHeadless_ = true;
    chart_ = null;
  }
 
  public void launchWorker(boolean doDisable) {
    try {
      if (suw_ != null) suw_.disableControls();
      done_ = false;
      progressDialog_ = null;
      progressBar_ = null;       
      worker_.setTotal(100);
      if (!isHeadless_) {
        Thread runThread = new Thread(worker_);
        prepProgressDialog();
        runThread.start();
        progressDialog_.setVisible(true);
      } else {
        worker_.run();
      }
    } catch (Exception ex) {
      uics_.getExceptionHandler().displayException(ex);
    }      
    return;
  }
  
  public void launchWorker() {
    launchWorker(true);
    return;
  }

  private void prepProgressDialog() {
    if (done_) {
      return;
    }
    progressDialog_ = new JDialog(topWindow_, rMan_.getString(waitTitle_), true);
    if (chart_ == null) {
      progressDialog_.setSize(350, 200);
    } else {
      progressDialog_.setSize(600, 500);
    }
    JLabel label = new JLabel(rMan_.getString(waitMsg_), JLabel.CENTER);
    Container cp = progressDialog_.getContentPane();
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    int rowNum = 0;
    UiUtil.gbcSet(gbc, 0, rowNum, 5, 4, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    rowNum += 4;
    cp.add(label, gbc);
    
    if (allowCancels_) {
      progressDialog_.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
      progressDialog_.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          try {
            boolean cancelPending = cancelRequested_;
            if (!cancelPending) {
              int ok = JOptionPane.showConfirmDialog(progressDialog_,
                                                     rMan_.getString("dialogs.cancelWarning"), 
                                                     rMan_.getString("dialogs.cancelWarningTitle"),
                                                     JOptionPane.YES_NO_OPTION);
              if (ok != JOptionPane.YES_OPTION) {
                return;
              }
              // Supposedly keeps dialog from closing, but on Linux it seems to close anyway!
              dialogCancelAndDisplay();
            } else {
              progressDialog_.setVisible(false);
              progressDialog_.dispose();
            }
          } catch (Exception ex) {
            uics_.getExceptionHandler().displayException(ex);
          }
        }
      });
      
      cancelButton_ = new FixedJButton(rMan_.getString("dialogs.cancel"));
      cancelButton_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            dialogCancelAndDisplay();
          } catch (Exception ex) {
            uics_.getExceptionHandler().displayException(ex);
          }
        }
      });
      UiUtil.gbcSet(gbc, 2, rowNum++, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
      cp.add(cancelButton_, gbc);
      cancellingMessage_ = new JLabel("", JLabel.CENTER);
      UiUtil.gbcSet(gbc, 0, rowNum++, 5, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
      cp.add(cancellingMessage_, gbc);     
    }

    progressDialog_.setLocationRelativeTo(topWindow_);
    progressBar_ = new JProgressBar(0, 100);
    progressBar_.setValue(0);
    progressBar_.setStringPainted(true);
    progressBar_.setIndeterminate(true);
    UiUtil.gbcSet(gbc, 0, rowNum++, 5, 1, UiUtil.BO, 0, 0, 20, 20, 20, 20, UiUtil.CEN, 1.0, 0.0);    
    cp.add(progressBar_, gbc);
    
    if (chart_ != null) {
      UiUtil.gbcSet(gbc, 0, rowNum, 5, 3, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
      cp.add(chart_, gbc);
    }
    
    return;
  }

  public boolean updateRankings(SortedMap<Integer, Double> vals) {
    if (chart_ != null) {
      chart_.setProgress(vals);
      chart_.repaint();
    }
    return (!cancelRequested_);
  }
 
  public boolean updateProgress(int percent) {
    if (progressBar_ != null) {
      progressBar_.setValue(percent);
      progressBar_.setIndeterminate(false);
    }
    return (!cancelRequested_);
  }
  
  public boolean keepGoing() {
    return (!cancelRequested_);
  }  
  
  public void requestCancel() {
    cancelRequested_ = true;
    return;
  } 
  
  /****************************************************************************
  **
  ** This routine is called ON THE UI THREAD following the completion of the
  ** background thread.  It is NOT called if the background thread is cancelled.
  ** The owner has callbacks cleanUpPreEnable() and cleanUpPostRepaint() to
  ** get things done on this thread when things wrap up.  With foreground operation,
  ** called after we are done, on the same thread
  */  
  
  public void finishedWork(Object result, Exception remoteEx, OutOfMemoryError memErr) {
    done_ = true;
    if (memErr != null) {
      uics_.getExceptionHandler().displayOutOfMemory(memErr);
    }
    if (remoteEx != null) {
      if (!owner_.handleRemoteException(remoteEx)) {
        uics_.getExceptionHandler().displayException(remoteEx);
      }
    }      
    try {
      if (support_ != null) {
        support_.finish();
      }
      if (progressDialog_ != null) {
        progressDialog_.setVisible(false);
        progressDialog_.dispose();
      }
      owner_.cleanUpPreEnable(result);      
      if (suw_ != null) {
        suw_.reenableControls();
        suw_.redraw();
      }
      owner_.cleanUpPostRepaint(result);
    } catch (Exception ex) {
      uics_.getExceptionHandler().displayException(ex);
    }
    return;
  }
  
  public void workCancelled() {
    done_ = true;
    try {
      if (!allowCancels_) {
        throw new IllegalStateException();
      }
      // rollback the changes on the UI thread!
      if (support_ != null) {
        support_.rollback();
      }
      if (progressDialog_ != null) {
        progressDialog_.setVisible(false);
        progressDialog_.dispose();
      }
      owner_.handleCancellation();
      if (suw_ != null) {
        suw_.reenableControls();
        suw_.redraw();
      }
    } catch (Exception ex) {
      uics_.getExceptionHandler().displayException(ex);
    }
   return;
  }
  
  public void dialogCancelAndDisplay() {  
    cancelButton_.setEnabled(false);
    cancellingMessage_.setText(rMan_.getString("dialogs.waitForCancel"));
    cancellingMessage_.invalidate();
    progressDialog_.validate();
    cancelRequested_ = true;
  }
} 
