package org.loxf.jyapi.exception;

public class NotLoginException extends RuntimeException {
    private int code = -1;
    private String msg  ;
    public NotLoginException(){
        super("未登录");
        msg = "未登录";
    }
}
