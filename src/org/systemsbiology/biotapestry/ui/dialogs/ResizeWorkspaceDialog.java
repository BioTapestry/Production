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


package org.systemsbiology.biotapestry.ui.dialogs;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.geom.Point2D;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.Workspace;
import org.systemsbiology.biotapestry.nav.ZoomTarget;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTStashResultsDialog;
import org.systemsbiology.biotapestry.util.FixedJButton;

/****************************************************************************
**
** Dialog box for sizing the workspace
*/

public class ResizeWorkspaceDialog extends BTStashResultsDialog {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private enum ParamVals {NONE_, WIDTH_, HEIGHT_};

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  private JTextField workWidth_;
  private JTextField workHeight_;
  private boolean processing_;
  private Workspace.FixedAspectDim currDims_;
  private Workspace.FixedAspectDim modelDims_;
  private Point2D currentCenter_;
  private Point2D allModelCenter_;
  
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
  
  public ResizeWorkspaceDialog(UIComponentSource uics, Workspace wsp, ZoomTarget ztrg, 
                               boolean emptyNoOverlay, int padding) { 
    super(uics, "resizeWork.title", new Dimension(500, 250), 2);
   
    Dimension currDims = wsp.getCanvasSize();
    double aspectRatio = wsp.getCanvasAspectRatio();
    Rectangle modelSize = ztrg.getAllModelBounds();
    Point2D currentCenter = ztrg.getRawCenterPoint();
    
    currDims_ = new Workspace.FixedAspectDim(currDims, aspectRatio);
    modelDims_ = Workspace.calcBoundedFit(modelSize, padding, aspectRatio);
    currentCenter_ = (Point2D)currentCenter.clone();
    allModelCenter_ = Workspace.getAllModelCenter(modelSize);
   
    JLabel wLab = new JLabel(rMan_.getString("resizeWork.width"));   
    workWidth_ = new JTextField();
    
    JLabel hLab = new JLabel(rMan_.getString("resizeWork.height"));
    workHeight_ = new JTextField();

    //
    // Do it now to avoid events:
    //
    
    workWidth_.setText(Integer.toString(currDims_.getWidth()));
    workHeight_.setText(Integer.toString(currDims_.getHeight()));
    
    FieldListener pl1 = new FieldListener(ParamVals.WIDTH_);
    workWidth_.addActionListener(pl1);
    workWidth_.addCaretListener(pl1);
    workWidth_.addFocusListener(pl1);
    
    FieldListener pl2 = new FieldListener(ParamVals.HEIGHT_);
    workHeight_.addActionListener(pl2);
    workHeight_.addCaretListener(pl2);
    workHeight_.addFocusListener(pl2);
    
    addLabeledWidget(wLab, workWidth_, false, false); 
    addLabeledWidget(hLab, workHeight_, false, false);

    FixedJButton buttonM = new FixedJButton(rMan_.getString("resizeWork.fitToModel"));
    buttonM.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          fitToModel();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        } catch (OutOfMemoryError oom) {
          uics_.getExceptionHandler().displayOutOfMemory(oom);
        }
      }
    }); 
    if (emptyNoOverlay) {
      buttonM.setEnabled(false);
    }
    
    finishConstructionWithExtraLeftButton(buttonM);
    
  }

  /***************************************************************************
  **
  ** Get the dialog results.  Bogus if results not advertised.
  */
  
  public Rectangle getResults() {
    if (currDims_ == null) {
      return (null);
    }
    
    int x = (int)Math.round(currentCenter_.getX() - (currDims_.getWidth() / 2.0));
    int y = (int)Math.round(currentCenter_.getY() - (currDims_.getHeight() / 2.0));    
    
    return (new Rectangle(x, y, currDims_.getWidth(), currDims_.getHeight()));
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED/PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Process a new size value
  */
  
  private void processVal(boolean force, ParamVals whichVal) {

    //
    // While typing, blank fields are OK.  If that happens, we don't update anything else.
    // Bad or negative results give a beep, and we don't update anything either.  During
    // a force, bad results mean we replace the bad field with the current valid value,
    // as well as a beep.
    //
    
    if (processing_) {
      return;
    }
    processing_ = true;    
    try {

      boolean fixVal = false;

      int newHeight = currDims_.getHeight();
      if (whichVal == ParamVals.HEIGHT_) {
        String heightText = workHeight_.getText().trim();
        boolean haveHeight = false;
        if (!heightText.equals("")) {
          try {
            int parsedHeight = Integer.parseInt(heightText);
            if ((parsedHeight >= Workspace.MIN_DIMENSION) && (parsedHeight <= Workspace.MAX_DIMENSION)) {
              newHeight = parsedHeight;
              haveHeight = true;
            } else if (parsedHeight == 0) {
              if (force) {
                Toolkit.getDefaultToolkit().beep();
              }
            } else {
              Toolkit.getDefaultToolkit().beep();
            }
            if (haveHeight && tooBigOrSmall(newHeight, ParamVals.HEIGHT_)) {
              newHeight = currDims_.getHeight();
              haveHeight = false;
              Toolkit.getDefaultToolkit().beep();
            }       
          } catch (NumberFormatException nfe) {
            Toolkit.getDefaultToolkit().beep();
          }
        } else if (force) {
          Toolkit.getDefaultToolkit().beep();  
        }
        if (force && !haveHeight) {
          fixVal = true;
        }
      }    

      int newWidth = currDims_.getWidth();
      if (whichVal == ParamVals.WIDTH_) {
        String widthText = workWidth_.getText().trim();
        boolean haveWidth = false;
        if (!widthText.equals("")) {
          try {
            int parsedWidth = Integer.parseInt(widthText);
            if ((parsedWidth >= Workspace.MIN_DIMENSION) && (parsedWidth <= Workspace.MAX_DIMENSION)) {
              newWidth = parsedWidth;
              haveWidth = true;
            } else if (parsedWidth == 0) {
              if (force) {
                Toolkit.getDefaultToolkit().beep();
              }
            } else {
              Toolkit.getDefaultToolkit().beep();
            }
            if (haveWidth && tooBigOrSmall(newWidth, ParamVals.WIDTH_)) {
              newWidth = currDims_.getWidth();
              haveWidth = false;
              Toolkit.getDefaultToolkit().beep();
            }
          } catch (NumberFormatException nfe) {
            Toolkit.getDefaultToolkit().beep();
          }
        } else if (force) {
          Toolkit.getDefaultToolkit().beep();  
        }
        if (force && !haveWidth) {
          fixVal = true;
        }
      }
      
      //
      // Now go and set fields:
      //
      
      setFields(whichVal, newHeight, newWidth, fixVal); 
    } finally {
      processing_ = false;
    }
    return;
  } 
  
  /***************************************************************************
  **
  ** Set fields
  */
  
  private void setFields(ParamVals whichVal, int newHeight, int newWidth, boolean fixVal) {        

    //
    // Now go and set fields:
    //
                           
    Workspace.FixedAspectDim dims = new Workspace.FixedAspectDim(currDims_);
    
    switch (whichVal) { 
      case HEIGHT_:
        dims.changeHeight(newHeight, false);
        if (dims.heightChanged(currDims_)) {
          currDims_.mergeHeight(dims);
        }
        if (dims.widthChanged(currDims_)) {
          currDims_.mergeWidth(dims);
          workWidth_.setText(Integer.toString(currDims_.getWidth())); 
        }
        if (fixVal) {
          workHeight_.setText(Integer.toString(currDims_.getHeight()));
        }
        break;        
      case WIDTH_:
        dims.changeWidth(newWidth, false);
        if (dims.widthChanged(currDims_)) {
          currDims_.mergeWidth(dims);
        }
        currDims_.mergeWidth(dims);
        if (dims.heightChanged(currDims_)) {
          currDims_.mergeHeight(dims);
          workHeight_.setText(Integer.toString(currDims_.getHeight())); 
        }
        if (fixVal) {
          workWidth_.setText(Integer.toString(currDims_.getWidth()));
        }
        break;     
      default:
        throw new IllegalStateException();
    }
    return;
  } 
 
  /***************************************************************************
  **
  ** fix values
  */
  
  private void fixVals() {    
    if (processing_) {
      return;
    }
    // Fix for BT-05-14-07:2. Hard to reproduce, but
    // perhaps a focus change after a stashResults?
    if (currDims_ == null) {
      return;
    }
    processing_ = true;
    try {
      workWidth_.setText(Integer.toString(currDims_.getWidth()));
      workHeight_.setText(Integer.toString(currDims_.getHeight()));
    } finally {
      processing_ = false;
    }
    return;
  }  
 
  /***************************************************************************
  **
  ** Report if a value is too big or small
  */
  
  private boolean tooBigOrSmall(int newVal, ParamVals whichVal) {
    return (tooBig(newVal, whichVal) || tooSmall(newVal, whichVal));
  } 
  
  /***************************************************************************
  **
  ** Report if a value is too small
  */
  
  private boolean tooSmall(int newVal, ParamVals whichVal) {
    //
    // If a dimension setting forces the other below min, it is too small:
    //
          
    Workspace.FixedAspectDim dims = new Workspace.FixedAspectDim(currDims_);
    
    switch (whichVal) {
      case HEIGHT_:
        dims.changeHeight(newVal, false);
        return (dims.getWidth() < Workspace.MIN_DIMENSION);
      case WIDTH_:
        dims.changeWidth(newVal, false);
        return (dims.getHeight() < Workspace.MIN_DIMENSION); 
      default:
        throw new IllegalStateException();
    }
  } 
  
  /***************************************************************************
  **
  ** Report if a value is too big
  */
  
  private boolean tooBig(int newVal, ParamVals whichVal) {
    //
    // If a dimension setting forces the other above, it is too big:
    //
          
    Workspace.FixedAspectDim dims = new Workspace.FixedAspectDim(currDims_);
    
    switch (whichVal) {
      case HEIGHT_:
        dims.changeHeight(newVal, false);
        return (dims.getWidth() > Workspace.MAX_DIMENSION);
      case WIDTH_:
        dims.changeWidth(newVal, false);
        return (dims.getHeight() > Workspace.MAX_DIMENSION); 
      default:
        throw new IllegalStateException();
    }
  }   
 
  /***************************************************************************
  **
  ** Get ready for export.  Since we are always getting updated, not much to do:
  ** 
  */

  protected boolean stashForOK() { 
    return (true);
  }

  /***************************************************************************
  **
  ** Fit the workspace to the model
  */
  
  private void fitToModel() {    
    if (processing_) {
      return;
    }
    processing_ = true;
    try {
      currDims_.mergeAll(modelDims_);
      workWidth_.setText(Integer.toString(currDims_.getWidth()));
      workHeight_.setText(Integer.toString(currDims_.getHeight()));
      currentCenter_ = (Point2D)allModelCenter_.clone();
    } finally {
      processing_ = false;
    }
    return;
  } 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  /***************************************************************************
  **
  ** For handling textField changes
  ** 
  */
  
  private class FieldListener implements ActionListener, CaretListener, FocusListener {
    
    private ParamVals whichVal_;
    
    FieldListener(ParamVals whichVal) {
      whichVal_ = whichVal;
    }
    
    public void actionPerformed(ActionEvent evt) {
      try {
        processVal(true, whichVal_);
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      } catch (OutOfMemoryError oom) {
        uics_.getExceptionHandler().displayOutOfMemory(oom);
      }
    }
    public void caretUpdate(CaretEvent evt) {
      try {
        processVal(false, whichVal_);
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      } catch (OutOfMemoryError oom) {
        uics_.getExceptionHandler().displayOutOfMemory(oom);
      }
    }
    public void focusGained(FocusEvent evt) {
    }    
    public void focusLost(FocusEvent evt) {
      try {
        fixVals();
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      } catch (OutOfMemoryError oom) {
        uics_.getExceptionHandler().displayOutOfMemory(oom);
      }
    }        
  } 
}
