package com.phaseshifter.canora.plugin.soundcloud.api.exceptions;

/**
 * General SoundCloud exception.
 */
public class SCException extends Exception {
    static final long serialVersionUID = -3387516993124229948L;

    public SCException() {
        super();
    }

    public SCException(String message) {
        super(message);
    }

    public SCException(String message, Throwable cause) {
        super(message, cause);
    }

    public SCException(Throwable cause) {
        super(cause);
    }

    protected SCException(String message, Throwable cause,
                          boolean enableSuppression,
                          boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
