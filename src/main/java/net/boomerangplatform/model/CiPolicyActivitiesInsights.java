package net.boomerangplatform.model;

import java.io.Serializable;
import java.util.Date;

public class CiPolicyActivitiesInsights implements Serializable {

	private static final long serialVersionUID = 1L;

	private String ciPolicyActivityId;
	private Date ciPolicyActivityCreatedDate;
	private Integer violations;
	
	public CiPolicyActivitiesInsights() {
		
	}

	public String getCiPolicyActivityId() {
		return ciPolicyActivityId;
	}

	public void setCiPolicyActivityId(String ciPolicyActivityId) {
		this.ciPolicyActivityId = ciPolicyActivityId;
	}

	public Date getCiPolicyActivityCreatedDate() {
		return ciPolicyActivityCreatedDate;
	}

	public void setCiPolicyActivityCreatedDate(Date ciPolicyActivityCreatedDate) {
		this.ciPolicyActivityCreatedDate = ciPolicyActivityCreatedDate;
	}

	public Integer getViolations() {
		return violations;
	}

	public void setViolations(Integer violations) {
		this.violations = violations;
	}
}