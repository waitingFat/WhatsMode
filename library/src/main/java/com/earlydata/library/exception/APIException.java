package com.earlydata.library.exception;

/**
 * Created by zchu on 16-12-29.
 */

public class APIException extends RuntimeException {

    public APIException(int code, String message) {
        this.code = code;
        this.message = message;
    }

    private int code;

    private String message;

    @Override
    public String getMessage() {
        return message;
    }
}
