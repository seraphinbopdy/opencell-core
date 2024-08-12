package org.meveo.model.billing;

public enum VatDateCodeEnum {
	INV_DOC_ISSUE_DATETIME(3),
	DELIVERY_DATETIME(35),
	PAID_TO_DATE(432);
	
	private int paidToDays;
	
	VatDateCodeEnum(int paidToDays) {
		this.paidToDays = paidToDays;
	}
	public int getPaidToDays() {
		return paidToDays;
	}
}
