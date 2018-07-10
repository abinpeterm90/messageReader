package com.hackathon.androidserver.model;

public class Message {
    public String getSearchCriteria() {
        return SearchCriteria;
    }

    public void setSearchCriteria(String searchCriteria) {
        SearchCriteria = searchCriteria;
    }

    public String getMobileNumber() {
        return MobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        MobileNumber = mobileNumber;
    }

    private String SearchCriteria;
    private String MobileNumber;

    public String getRepriceKey() {
        return RepriceKey;
    }

    public void setRepriceKey(String repriceKey) {
        RepriceKey = repriceKey;
    }

    private String RepriceKey;
}
