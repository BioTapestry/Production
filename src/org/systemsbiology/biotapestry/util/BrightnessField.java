/*
**    Copyright (C) 2003-2016 Institute for Systems Biology 
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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import org.systemsbiology.biotapestry.app.BTState;

/****************************************************************************
**
** Widget for allowing selection of gray
*/

public class BrightnessField extends JPanel {
  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  private JTextField brightness_;
  private JPanel canvas_;
  private BTState appState_;
  private double brightVal_;
  private boolean haveResult_;
  private double min_;
  private double max_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public BrightnessField(BTState appState, String title, double initVal, double min, double max) { 
    
    appState_ = appState;
    ResourceManager rMan = appState_.getRMan();
    brightVal_ = initVal;
    haveResult_ = false;
    min_ = min;
    max_ = max;
    
    JLabel wLab = new JLabel(rMan.getString(title));   
    brightness_ = new JTextField();
    canvas_ = new JPanel();
    
    //
    // Do it now to avoid events:
    //
    
    brightness_.setText(Double.toString(initVal));
    
    FieldListener pl1 = new FieldListener();
    brightness_.addActionListener(pl1);
    brightness_.addCaretListener(pl1);
    brightness_.addFocusListener(pl1);
     
    setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    add(wLab, gbc);
    
    UiUtil.gbcSet(gbc, 1, 0, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.5, 1.0);
    add(brightness_, gbc);

    UiUtil.gbcSet(gbc, 2, 0, 1, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.5, 1.0);    
    add(canvas_, gbc);
    
    processVal();
  }

  /***************************************************************************
  **
  ** Reset the field value
  */
  
  public void resetValue(double val) {
    brightness_.setText(Double.toString(val));
    processVal();
    return;
  }
 
  /***************************************************************************
  **
  ** Get if we have valid results
  */
  
  public boolean haveResults() {
    return (haveResult_);
  }
  
  /***************************************************************************
  **
  ** Get the  results.
  */
  
  public double getResults() {
    if (!haveResult_) {
      throw new IllegalStateException();
    }
    return (brightVal_);
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED/PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Process a new brightness value
  */
  
  private void processVal() {
    boolean showRed = true;
    String brightText = brightness_.getText().trim();
    if (!brightText.equals("")) {
      try {
        double parsedBright = Double.parseDouble(brightText);
        if ((parsedBright <= max_) && (parsedBright >= min_)) {
          brightVal_ = parsedBright;
          showRed = false;
        }
      } catch (NumberFormatException nfe) {
      }
    }
    if (showRed) {
      canvas_.setBackground(new Color(255, 0, 0));
      haveResult_ = false;
    } else {
      canvas_.setBackground(new Color((int)(255.0 * brightVal_), (int)(255.0 * brightVal_), (int)(255.0 * brightVal_)));
      haveResult_ = true;
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
       
    public void actionPerformed(ActionEvent evt) {
      try {
        processVal();
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      } catch (OutOfMemoryError oom) {
        appState_.getExceptionHandler().displayOutOfMemory(oom);
      }
    }
    public void caretUpdate(CaretEvent evt) {
      try {
        processVal();
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      } catch (OutOfMemoryError oom) {
        appState_.getExceptionHandler().displayOutOfMemory(oom);
      }
    }
    public void focusGained(FocusEvent evt) {
    }    
    public void focusLost(FocusEvent evt) {
      try {
        processVal();
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      } catch (OutOfMemoryError oom) {
        appState_.getExceptionHandler().displayOutOfMemory(oom);
      }
    }        
  } 
}
