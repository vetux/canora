package com.phaseshifter.canora.service.state;

public enum PlaybackState {
    STATE_IDLE,
    STATE_BUFFERING,
    STATE_READY,
    STATE_ENDED;

    public static PlaybackState fromInt(int code) {
        switch (code) {
            case 1:
                return STATE_IDLE;
            case 2:
                return STATE_BUFFERING;
            case 3:
                return STATE_READY;
            case 4:
                return STATE_ENDED;
            default:
                throw new RuntimeException("Invalid Code: " + code);
        }
    }
}