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

package org.systemsbiology.biotapestry.cmd.flow.edit;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractOptArgs;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.RemoteRequest;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PertDataChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.TemporalInputChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.TimeCourseChangeCmd;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.perturb.PertDataChange;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.plugin.ModelBuilderPlugIn;
import org.systemsbiology.biotapestry.plugin.PlugInManager;
import org.systemsbiology.biotapestry.timeCourse.GroupUsage;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputChange;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseChange;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseDataMaps;
import org.systemsbiology.biotapestry.ui.GroupProperties;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.dialogs.RegionPropertiesDialogFactory;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;


/****************************************************************************
 **
 ** Handle editing group properties
 */

public class EditGroupProperties extends AbstractControlFlow {

	////////////////////////////////////////////////////////////////////////////
	//
	// PRIVATE VARIABLES
	//
	////////////////////////////////////////////////////////////////////////////    

	private String groupID_;

	////////////////////////////////////////////////////////////////////////////
	//
	// PUBLIC CONSTRUCTORS
	//
	////////////////////////////////////////////////////////////////////////////    

	/***************************************************************************
	 **
	 ** Constructor 
	 */ 

	public EditGroupProperties(GroupArg arg) { 
		name = "groupPopup.GroupProperties";
		desc = "groupPopup.GroupProperties";
		mnem =  "groupPopup.GroupPropertiesMnem";
		groupID_ = (arg == null ? null : (arg.getForDeferred()) ? null : arg.getGroupID());

	}

	////////////////////////////////////////////////////////////////////////////
	//
	// PUBLIC METHODS
	//
	////////////////////////////////////////////////////////////////////////////  

	/***************************************************************************
	 **
	 ** For programmatic preload
	 ** 
	 */ 

	@Override
	public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {  
		return (new StepState(groupID_, dacx));
	}

	/***************************************************************************
	 **
	 ** Handle the flow
	 ** 
	 */ 

	public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {

		DialogAndInProcessCmd next;
		while (true) {
			if (last == null) {
				throw new IllegalStateException();
			} else {
				StepState ans = (StepState)last.currStateX;
				ans.stockCfhIfNeeded(cfh);
				if (ans.getNextStep().equals("stepGetDialog")) {
					next = ans.stepGetDialog();
				} else if(ans.getNextStep().equals("stepExtractAndInstallProps")){
					next = ans.stepExtractAndInstallProps(last);
				} else {
					throw new IllegalStateException();
				}
			}
			if (!next.state.keepLooping()) {
				return (next);
			}
			last = next;
		}
	}


	/***************************************************************************
	 *
	 * Handle out-of-band question/response
	 * 
	 */
	@Override     
	public RemoteRequest.Result processRemoteRequest(RemoteRequest qbom, DialogAndInProcessCmd.CmdState cms) {
		StepState ans = (StepState)cms;
		if (qbom.getLabel().equals("queBombCheckName")) {
			return (ans.queBombCheckName(qbom));
		} else if(qbom.getLabel().equals("queBombCheckDataDisconnect")) {
			return (ans.queBombCheckDataDisconnect(qbom));
		} else if(qbom.getLabel().equals("queBombCheckOtherRegionsAttachedByDefault")) {
      return (ans.queBombCheckOtherRegionsAttachedByDefault(qbom));
    } else if(qbom.getLabel().equals("queBombDataNameCollision")) {
      return (ans.queBombDataNameCollision(qbom));
		} else {
			throw new IllegalArgumentException();
		}
	}

	/***************************************************************************
	 *
	 * Running State
	 * 
	 */
	public static class StepState extends AbstractStepState implements DialogAndInProcessCmd.PopupCmdState {

		private String myGroupID_;
		private String interID_;

		/***************************************************************************
	  *
		* Construct
		*
		*/
		
		public StepState(String groupID, StaticDataAccessContext dacx) {
			super(dacx);
			nextStep_ = "stepGetDialog";
			myGroupID_ = groupID;
		}
		
		/***************************************************************************
		 **
		 ** for preload
		 */ 

		public void setIntersection(Intersection inter) {
			if (myGroupID_ == null) {
				interID_ = inter.getObjectID();
			}
			return;
		}  

		///////////////////
		// stepGetDialog
		///////////////////
		//
		//
		private DialogAndInProcessCmd stepGetDialog() {  
			GroupProperties gp = dacx_.getCurrentLayout().getGroupProperties(Group.getBaseID(myGroupID_ == null ? interID_ : myGroupID_));

			if (gp != null) {
				Layout layout = dacx_.getLayoutSource().getLayout(dacx_.getCurrentLayoutID());
				String genomeKey = layout.getTarget();
				GenomeInstance genomeInstance = (GenomeInstance)dacx_.getGenomeSource().getGenome(genomeKey);
				Group group = genomeInstance.getGroup(gp.getReference());
			    String oldName = group.getName();
				RegionPropertiesDialogFactory.RegionPropBuildArgs ba = new RegionPropertiesDialogFactory.RegionPropBuildArgs(gp, (myGroupID_ != null),oldName);
				RegionPropertiesDialogFactory rpdf = new RegionPropertiesDialogFactory(cfh_);
				ServerControlFlowHarness.Dialog cfhd = rpdf.getDialog(ba);
				nextStep_ = "stepExtractAndInstallProps";
				return(new DialogAndInProcessCmd(cfhd, this));
			}
			return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
		}


		/////////////////////////////////
		// stepExtractAndInstallProps
		////////////////////////////////
		//
		//
		private DialogAndInProcessCmd stepExtractAndInstallProps(DialogAndInProcessCmd daipc) {  

			RegionPropertiesDialogFactory.RegionRequest rrq = (RegionPropertiesDialogFactory.RegionRequest)daipc.cfhui;
			UndoSupport support = uFac_.provideUndoSupport("undo.groupprop", dacx_);
			boolean submit = false;
			
			String groupID = Group.getBaseID(myGroupID_ == null ? interID_ : myGroupID_);
			
			GroupProperties gp = dacx_.getCurrentLayout().getGroupProperties(groupID);
			
			Layout layout = dacx_.getLayoutSource().getLayout(dacx_.getCurrentLayoutID());
			String genomeKey = layout.getTarget();
			GenomeInstance genomeInstance = (GenomeInstance)dacx_.getGenomeSource().getGenome(genomeKey);
			Group group = genomeInstance.getGroup(gp.getReference());
	    String oldName = group.getName();
	    
	    if (rrq.nameMapping) {
        if (!handleNameRemapping(rrq, oldName, rrq.nameResult.trim(), groupID, support, genomeInstance)) {
          return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));      
        }
      }

	    if(!oldName.equals(rrq.nameResult)) {
	        if (rrq.nameResult.trim().equals("")) {
	        	rrq.nameResult = null;
	        }
		        
	        GenomeChange chng = new GenomeChange();
	        chng.grOrig = new Group(group);
	        group.setName(rrq.nameResult);
	        chng.grNew = new Group(group);
	        chng.genomeKey = genomeKey;    
	        support.addEdit(new GenomeChangeCmd(chng));
	        // Actually, all child model groups change too. FIX ME?
	        support.addEvent(new ModelChangeEvent(dacx_.getGenomeSource().getID(), genomeKey, ModelChangeEvent.UNSPECIFIED_CHANGE));
	        submit = true;
	    }
	      
	    //
	    // Though we hold layout's property directly, make change by changing
	    // a clone and submitting it:
	    //

	    GroupProperties changedProps = new GroupProperties(gp); 
	    changedProps.setColor(true, rrq.activeColorResult);    
	    changedProps.setColor(false, rrq.inactiveColorResult);

	    if (rrq.layerResult > 0) {
	    	changedProps.setLayer(rrq.layerResult);
	    }

	    changedProps.setPadding(GroupProperties.TOP, rrq.tpadResult);
	    changedProps.setPadding(GroupProperties.BOTTOM, rrq.bpadResult);
	    changedProps.setPadding(GroupProperties.LEFT, rrq.lpadResult);
	    changedProps.setPadding(GroupProperties.RIGHT, rrq.rpadResult);

	    changedProps.setHideLabel(rrq.hideLabelResult);

	    Layout.PropChange[] lpc = new Layout.PropChange[1];    
	    lpc[0] = layout.replaceGroupProperties(gp, changedProps); 

	    if (lpc[0] != null) {
	    	PropChangeCmd mov = new PropChangeCmd(dacx_, lpc);
	    	support.addEdit(mov);
	    	gp = changedProps;
	    	support.addEvent(new LayoutChangeEvent(dacx_.getCurrentLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));
	    	submit = true;
	    }

	    if (submit) {
	    	support.finish();
	    }
			
			return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
		}

		//////////////////////
		// queBombCheckName
		/////////////////////
		//
		//
		private RemoteRequest.Result queBombCheckName(RemoteRequest qbom) {

			RemoteRequest.Result result = new RemoteRequest.Result(qbom);
			result.setBooleanAnswer("haveResult", false);
			String nameResult = qbom.getStringArg("nameResult");
			String ref = qbom.getStringArg("propsRef");
			Layout layout = dacx_.getLayoutSource().getLayout(dacx_.getCurrentLayoutID());
			String genomeKey = layout.getTarget();
			GenomeInstance genomeInstance = (GenomeInstance)dacx_.getGenomeSource().getGenome(genomeKey);
	    if (genomeInstance.groupNameInUse(nameResult, ref)) {
	    	ResourceManager rMan = dacx_.getRMan();
	    	String desc = MessageFormat.format(
	    		rMan.getString("groupprops.NameInUse"), new Object[] {nameResult}
	    	);
	    	SimpleUserFeedback suf = new SimpleUserFeedback(
	    		SimpleUserFeedback.JOP.ERROR, desc, rMan.getString("groupprops.nameError")
	    	);
	    	result.setSimpleUserFeedback(suf);
	    	result.setDirection(RemoteRequest.Progress.STOP);
	    }
			
			result.setBooleanAnswer("haveResult", true);
			
			return result;
		}

		/////////////////////////////////
		// queBombCheckDataDisconnect
		////////////////////////////////
		//
		//
		private RemoteRequest.Result queBombCheckDataDisconnect(RemoteRequest qbom) {
			
			RemoteRequest.Result result = new RemoteRequest.Result(qbom);			
			GroupProperties gp = dacx_.getCurrentLayout().getGroupProperties(Group.getBaseID(myGroupID_ == null ? interID_ : myGroupID_));
			String ref = gp.getReference();
			Layout layout = dacx_.getLayoutSource().getLayout(dacx_.getCurrentLayoutID());
			String genomeKey = layout.getTarget();
			GenomeInstance genomeInstance = (GenomeInstance)dacx_.getGenomeSource().getGenome(genomeKey);
			Group group = genomeInstance.getGroup(ref);
		  String oldName = group.getName();    
		  boolean found = (oldName != null) && dacx_.getExpDataSrc().hasRegionAttachedByDefault(group);
      result.setBooleanAnswer("resolveDisconnect", found);
			return result;
		}
		
    /***************************************************************************
    **
    ** Check other regions for default attachment
    */ 

    public RemoteRequest.Result queBombCheckOtherRegionsAttachedByDefault(RemoteRequest qbom) {    
      RemoteRequest.Result result = new RemoteRequest.Result(qbom);     
      GroupProperties gp = dacx_.getCurrentLayout().getGroupProperties(Group.getBaseID(myGroupID_ == null ? interID_ : myGroupID_));
      String ref = gp.getReference();
      Layout layout = dacx_.getLayoutSource().getLayout(dacx_.getCurrentLayoutID());
      String genomeKey = layout.getTarget();
      GenomeInstance genomeInstance = (GenomeInstance)dacx_.getGenomeSource().getGenome(genomeKey);
      Group group = genomeInstance.getGroup(ref);
      boolean hasOR = dacx_.getExpDataSrc().hasOtherRegionsAttachedByDefault(genomeInstance, group); 
      result.setBooleanAnswer("hasOtherRegionsAttached", hasOR);
      return (result);
    }
    
    /***************************************************************************
    **
    ** Check if other region in the underlying data has the same name
    */ 

    public RemoteRequest.Result queBombDataNameCollision(RemoteRequest qbom) {    
      RemoteRequest.Result result = new RemoteRequest.Result(qbom);            
      String nameResult = qbom.getStringArg("nameResult");  
      boolean doesMatch = queBombDataNameCollisionGuts(nameResult);    
      result.setBooleanAnswer("hasDataNameCollision", doesMatch);
      return (result);  
    }
    
    /***************************************************************************
    **
    ** Check if other region in the underlying data has the same name. Usable internally.
    */ 

    private boolean queBombDataNameCollisionGuts(String nameResult) {        
      TemporalInputRangeData tird = dacx_.getTemporalRangeSrc().getTemporalInputRangeData(); 
      TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();    
      boolean doesMatch = (DataUtil.containsKey(tcd.getRegions(), nameResult) || DataUtil.containsKey(tird.getRegions(), nameResult));    
      return (doesMatch);  
    }

  	/***************************************************************************
    **
    ** Do the name remapping
    ** 
    */

    private boolean handleNameRemapping(RegionPropertiesDialogFactory.RegionRequest rrq, String oldName, 
                                        String newName, String groupID, UndoSupport support, GenomeInstance instance) {  
      if (rrq.handleDataNameChanges) {
        if (!handleDataNameChanges(rrq.networksToo, oldName, newName, support, instance)) {
          return (false);
        }
      } else if (rrq.createCustomMap) {
        if (!handleCustomMapCreation(oldName, groupID, support)) {
          return (false);
        }
      } else if (rrq.breakAssociation) {
        // Nothing needs to be done!
      } else {
        throw new IllegalStateException();
      }
      return (true);
    }
  		
  	/***************************************************************************
    **
    ** Handle changing data entries
    ** 
    */
    
    private boolean handleDataNameChanges(boolean doNetworks, String oldName, String newName,  UndoSupport support, GenomeInstance instance) {
      
      if (!newName.equals(oldName)) {
        if (queBombDataNameCollisionGuts(newName)) {
          return (false);
        }
      }
    
      TemporalInputRangeData tird = dacx_.getTemporalRangeSrc().getTemporalInputRangeData(); 
      TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();
      PerturbationData pd = dacx_.getExpDataSrc().getPertData();
      
      //
      // Gotta handle network before changing data so we accurately assess
      // default mappings:
      //
      
      if (doNetworks) {
        handleNetwork(oldName, newName, support, instance);
      }
  
      TemporalInputChange[] tic = tird.changeRegionName(oldName, newName);    
      for (int i = 0; i < tic.length; i++) {
        support.addEdit(new TemporalInputChangeCmd(dacx_, tic[i])); 
      }    
   
      ArrayList<TimeCourseDataMaps> tcdml = new ArrayList<TimeCourseDataMaps>();
      UiUtil.fixMePrintout("NOT ENOUGH NEED ALL MAPS FROM ALL TABS!");
      tcdml.add(dacx_.getDataMapSrc().getTimeCourseDataMaps());
      
      TimeCourseChange[] tcc = tcd.changeRegionName(oldName, newName, tcdml);
      for (int i = 0; i < tcc.length; i++) {
        support.addEdit(new TimeCourseChangeCmd(tcc[i])); 
      } 
    
      PertDataChange[] pdc = pd.changeRegionNameForRegionRestrictions(oldName, newName);
      if (pdc.length > 0) {
        support.addEdits(PertDataChangeCmd.wrapChanges(pdc));
        support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.PERTURB_DATA_CHANGE));
      }
      
      //
      // Model builder plugin wants to know about this:
      //
 
      PlugInManager pluginManager = uics_.getPlugInMgr(); 
      Iterator<ModelBuilderPlugIn> mbIterator = pluginManager.getBuilderIterator();
      if (mbIterator.hasNext()) {
        mbIterator.next().regionNameChange(dacx_, oldName, newName, support);
      }
      return (true);
    }	
  		
    /***************************************************************************
    **
    ** Handle custom map creation
    ** 
    */
    
    private boolean handleCustomMapCreation(String oldName,  String groupID, UndoSupport support) {
      //
      // If there are any regions with the old name, add a map to them
      //
      
      TemporalInputRangeData tird = dacx_.getTemporalRangeSrc().getTemporalInputRangeData();
      TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();
      TimeCourseDataMaps tcdm = dacx_.getDataMapSrc().getTimeCourseDataMaps();
      
      //
      // Temporal Input
      //
      
      if (DataUtil.containsKey(tird.getRegions(), oldName)) {
        List<GroupUsage> groupMap = tird.getCustomTemporalRangeGroupKeys(groupID);
        if (groupMap == null) {
          groupMap = new ArrayList<GroupUsage>();
        }
        groupMap.add(new GroupUsage(oldName, null));
        TemporalInputChange tic = tird.setTemporalRangeGroupMap(groupID, groupMap);
        if (tic != null) {
          support.addEdit(new TemporalInputChangeCmd(dacx_, tic));      
        }
      }
  
      //
      // Time Course
      //
      
      if (DataUtil.containsKey(tcd.getRegions(), oldName)) {
        List<GroupUsage> groupMap = tcdm.getCustomTimeCourseGroupKeys(groupID);
        if (groupMap == null) {
          groupMap = new ArrayList<GroupUsage>();
        }
        groupMap.add(new GroupUsage(oldName, null));
        TimeCourseChange tcc = tcdm.setTimeCourseGroupMap(groupID, groupMap, true);
        if (tcc != null) {
          support.addEdit(new TimeCourseChangeCmd(tcc));      
        }
      }
      return (true);
    } 	
  		
  	/***************************************************************************
    **
    ** Handle changing data entries
    ** 
    */
    
    private void handleNetwork(String oldName, String newName, UndoSupport support, GenomeInstance instance) {
  
      GenomeSource gs = dacx_.getGenomeSource();
      
      Iterator<GenomeInstance> iit = gs.getInstanceIterator();
      while (iit.hasNext()) {
        GenomeInstance tgi = iit.next();
        // root instances only
        if (tgi.getVfgParent() != null) {
          continue;
        }
        if (tgi == instance) {
          continue;
        }
        Iterator<Group> git = tgi.getGroupIterator();
        while (git.hasNext()) {
          Group testGroup = git.next();
          String tgName = testGroup.getName();
          if ((tgName != null) && DataUtil.keysEqual(tgName, oldName)) {
            if (dacx_.getExpDataSrc().hasRegionAttachedByDefault(testGroup)) {
              GenomeChange chng = new GenomeChange();
              chng.grOrig = new Group(testGroup);
              testGroup.setName(newName);
              chng.grNew = new Group(testGroup);
              chng.genomeKey = tgi.getID();   
              support.addEdit(new GenomeChangeCmd(chng));
              // Actually, all child model groups change too. FIX ME?
              support.addEvent(new ModelChangeEvent(dacx_.getGenomeSource().getID(), chng.genomeKey, ModelChangeEvent.UNSPECIFIED_CHANGE));
            }
          }
        }
      }
      return;
    }  	

	} // StepState


	/***************************************************************************
	 **
	 ** Control Flow Argument
	 */  

	public static class GroupArg extends AbstractOptArgs {

		private static String[] keys_;
		private static Class<?>[] classes_;

		static {
			keys_ = new String[] {"groupID", "forDeferred"};
			classes_ = new Class<?>[] {String.class, Boolean.class};  
		}

		public String getGroupID() {
			return (getValue(0));
		}

		public boolean getForDeferred() {
			return (Boolean.parseBoolean(getValue(1)));
		}

		public GroupArg(Map<String, String> argMap) throws IOException {
			super(argMap);
		}

		protected String[] getKeys() {
			return (keys_);
		}

		@Override
		protected Class<?>[] getClasses() {
			return (classes_);
		}

		public GroupArg(String groupID, boolean forDeferred) {
			super();
			if (forDeferred) {
				if (groupID != null) {
					throw new IllegalArgumentException();
				}
				groupID = "DONT_CARE";
			}
			setValue(0, groupID);
			setValue(1, Boolean.toString(forDeferred));
			bundle();
		}
	} 
}
