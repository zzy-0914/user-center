package com.zzy.usercenter.common;

public class ResultUtils {
    public static <T> BaseResponse<T>  success(T data){
      return   new BaseResponse<T>(0,data,"ok");
    }
    public static BaseResponse error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }
    public static BaseResponse error(ErrorCode errorCode,String message) {
        return new BaseResponse<>(errorCode);
    }
    public static BaseResponse error(int  errorCode,String message) {
        return new BaseResponse<>(errorCode,message);
    }
}
