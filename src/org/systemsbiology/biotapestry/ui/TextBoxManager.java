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

package org.systemsbiology.biotapestry.ui;

import java.awt.Graphics;

import javax.swing.JTextPane;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.genome.Note;

/***************************************************************************
** 
** This has been broken out from SUPanel and is now stand alone
*/

public class TextBoxManager {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  public static final int MODEL_MESSAGE         = 0;    
  public static final int OVERLAY_MESSAGE       = 1;
  public static final int NETMOD_MESSAGE        = 2;
  public static final int SELECTED_ITEM_MESSAGE = 3;
  private static final int NUM_TYPES_    = 4;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private BTState appState_;
  private boolean isHeadless_;
  private String[] currText_;
  private String[] currSourceKeys_;
  private String currMouseOver_;
  private LazyJTextPane textPane_;
  private String displayedText_;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////  

  public TextBoxManager(BTState appState) {
    appState_ = appState.setTextBoxManager(this);
    isHeadless_ = appState_.isHeadless();
    if (!isHeadless_) {   
      textPane_ = new LazyJTextPane(this);
    }
    displayedText_ = "";
    currText_ = new String[NUM_TYPES_];
    currSourceKeys_ = new String[NUM_TYPES_];
    currMouseOver_ = null;
  }
    
  public JTextPane getTextPane() {
    return (textPane_);
  }
  
  public void refresh() {
    if (!isHeadless_) {
      textPane_.repaint();
    }
    return;
  }
  
  public String getFreshText() {
    for (int i = 0; i < NUM_TYPES_; i++) {
      updateMessage(i);
    }
    return (chooseText());
  }

  public void checkForChanges(ModelChangeEvent mcev) {
    for (int i = 0; i < NUM_TYPES_; i++) {
      updateMessage(i);
    }
    displayedText_ = chooseText();
    if (!isHeadless_) {
      textPane_.setText(displayedText_);
    }
    return;
  } 
    
  public void clearMessageSource(int which) {
    currText_[which] = null;
    currSourceKeys_[which] = null;
    displayedText_ = chooseText();
    if (!isHeadless_) {
      textPane_.setText(displayedText_);
    }
    return;
  }
    
  public void setMessageSource(String messageSourceKey, int which, boolean skipSync) {
    currSourceKeys_[which] = messageSourceKey;
    currText_[which] = null;
    if (!skipSync) {
      updateMessage(which);
      displayedText_ = chooseText();
      if (!isHeadless_) {
        textPane_.setText(displayedText_);
      }
    } else {
      if (!isHeadless_) {
        textPane_.makeStale();
      }
    }
    return;
  }  
  
  void setCurrentMouseOver(String noteText) {
    currMouseOver_ = noteText;
    displayedText_ = chooseText();
    if (!isHeadless_) {
      textPane_.setText(displayedText_);
    }
    return;
  }     
  
  public void clearCurrentMouseOver() {
    currMouseOver_ = null;
    displayedText_ = chooseText();
    if (!isHeadless_) {
      textPane_.setText(displayedText_);
    }
    return;
  }
  
  private String chooseText() {
    if (currMouseOver_ != null) {
      return (currMouseOver_);
    } else {
      for (int i = NUM_TYPES_ - 1; i >= 0; i--) {
        if (currText_[i] != null) {
          return (currText_[i]);
        }
      }
      return ("");
    }
  }
  
  private void updateMessage(int which) {
    switch (which) {
      case MODEL_MESSAGE:
        if (currSourceKeys_[MODEL_MESSAGE] == null) {
          currText_[MODEL_MESSAGE] = null;
        } else {
          Genome genome = appState_.getDB().getGenome(currSourceKeys_[MODEL_MESSAGE]);
          currText_[MODEL_MESSAGE] = genome.getDescription();
        }
        break;
      case OVERLAY_MESSAGE:
        if ((currSourceKeys_[MODEL_MESSAGE] == null) ||
            (currSourceKeys_[OVERLAY_MESSAGE] == null)) {
          currText_[OVERLAY_MESSAGE] = null;
        } else {
          NetOverlayOwner owner = appState_.getDB().getOverlayOwnerFromGenomeKey(currSourceKeys_[MODEL_MESSAGE]);
          NetworkOverlay nol = owner.getNetworkOverlay(currSourceKeys_[OVERLAY_MESSAGE]);
          currText_[OVERLAY_MESSAGE] = nol.getDescription();
        }
        break;
      case NETMOD_MESSAGE:
        if ((currSourceKeys_[MODEL_MESSAGE] == null) ||
            (currSourceKeys_[OVERLAY_MESSAGE] == null) ||
            (currSourceKeys_[NETMOD_MESSAGE] == null)) {
          currText_[NETMOD_MESSAGE] = null;
        } else {
          NetOverlayOwner owner = appState_.getDB().getOverlayOwnerFromGenomeKey(currSourceKeys_[MODEL_MESSAGE]);
          NetworkOverlay nol = owner.getNetworkOverlay(currSourceKeys_[OVERLAY_MESSAGE]);
          NetModule nmod = nol.getModule(currSourceKeys_[NETMOD_MESSAGE]);
          currText_[NETMOD_MESSAGE] = nmod.getDescription();
        }
        break;
      case SELECTED_ITEM_MESSAGE:
        if ((currSourceKeys_[MODEL_MESSAGE] == null) ||
            (currSourceKeys_[SELECTED_ITEM_MESSAGE] == null)) {
          currText_[SELECTED_ITEM_MESSAGE] = null;
        } else {
          currText_[SELECTED_ITEM_MESSAGE] = null;
          Genome genome = appState_.getDB().getGenome(currSourceKeys_[MODEL_MESSAGE]);
          Note note = genome.getNote(currSourceKeys_[SELECTED_ITEM_MESSAGE]);
          if (note != null) {
            currText_[SELECTED_ITEM_MESSAGE] = note.getTextWithBreaksReplaced();
          }
        }
        break;
      default:
        throw new IllegalArgumentException();
      
    }
    
    if ((currText_[which] != null) && (currText_[which].trim().equals(""))) {
      currText_[which] = null;
    }
    
    return;
  }
  
  /***************************************************************************
  **
  ** Handles text box display.  Gotta be lazy since undo/redo has 
  ** asynchronous updates of genomeID and overlay state, so we need
  ** to delay text updates until the last possible moment.
  */  
      
  private static class LazyJTextPane extends JTextPane {
    
    private boolean stale_;
    private TextBoxManager textMgr_;
    private static final long serialVersionUID = 1L;
    
    LazyJTextPane(TextBoxManager textMgr) {
      super();
      stale_ = false;
      textMgr_ = textMgr;
    }
        
    public void paint(Graphics gr) {
      if (stale_) {
        setText(textMgr_.getFreshText());
        stale_ = false;
      }
      super.paint(gr);
      return;
    }

    public void makeStale() {
      stale_ = true;
      return;
    } 
  }  
}
