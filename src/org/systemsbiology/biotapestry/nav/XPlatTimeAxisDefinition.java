package org.systemsbiology.biotapestry.nav;

import java.util.List;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition.NamedStage;

public class XPlatTimeAxisDefinition {
	
	private String units_;
	private String unitAbbr_;
	private boolean unitIsSuffix_;
	private List<NamedStage> namedStages_;
	
	public XPlatTimeAxisDefinition(TimeAxisDefinition tad, DataAccessContext dacx) {
		boolean isCustom = tad.haveCustomUnits();
		units_ = isCustom ? tad.getUserUnitName() : TimeAxisDefinition.displayStringForUnit(dacx, tad.getUnits());
		unitAbbr_ = isCustom ? tad.getUserUnitAbbrev() : TimeAxisDefinition.abbrevStringForUnit(dacx, tad.getUnits());
		unitIsSuffix_ = tad.unitsAreASuffix();
		if(tad.haveNamedStages()) {
			namedStages_ = tad.getNamedStages();
		}
	}
	

	public String getUnits() {
		return units_;
	}

	public void setUnits(String units) {
		this.units_ = units;
	}


	public String getUnitAbbr() {
		return unitAbbr_;
	}

	public void setUnitAbbr(String unitAbbr) {
		this.unitAbbr_ = unitAbbr;
	}


	public boolean getUnitIsSuffix() {
		return unitIsSuffix_;
	}

	public void setUnitIsSuffix(boolean unitIsSuffix) {
		this.unitIsSuffix_ = unitIsSuffix;
	}


	public List<NamedStage> getNamedStages() {
		return namedStages_;
	}

	public void setNamedStages(List<NamedStage> namedStages) {
		this.namedStages_ = namedStages;
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
