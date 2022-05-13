package com.phaseshifter.canora.soundcloud.api.exceptions;

public class SCConnectionException extends SCException {
    static final long serialVersionUID = -3387516993124229948L;

    public SCConnectionException() {
        super();
    }

    public SCConnectionException(String message) {
        super(message);
    }

    public SCConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public SCConnectionException(Throwable cause) {
        super(cause);
    }

    protected SCConnectionException(String message, Throwable cause,
                                    boolean enableSuppression,
                                    boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
