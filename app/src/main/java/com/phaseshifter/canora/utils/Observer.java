package com.phaseshifter.canora.utils;

public interface Observer<Y> {
    void update(Observable<Y> observable, Y value);
}
