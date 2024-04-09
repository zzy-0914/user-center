package com.zzy.usercenter.constant;

public enum TeamStatusEnum {
    PUBLIC(0,"公开"),
    PRIVATE(1,"私密"),
    SECRET(2,"加密");
    private int value;
    private String text;

    TeamStatusEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public int getValue() {
        return value;
    }

    public String getText() {
        return text;
    }

    public static  TeamStatusEnum getEnumByValue(Integer value){
        if (value==null){
            return null;
        }
        for (TeamStatusEnum teamStatusEnum:values()){
            if (teamStatusEnum.value==value){
                return teamStatusEnum;
            }
        }
        return null;
    }
}
