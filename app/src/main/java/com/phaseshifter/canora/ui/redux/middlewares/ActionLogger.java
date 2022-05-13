package com.phaseshifter.canora.ui.redux.middlewares;

import android.util.Log;
import com.phaseshifter.canora.ui.redux.core.Action;
import com.phaseshifter.canora.ui.redux.core.Dispatcher;
import com.phaseshifter.canora.ui.redux.core.Middleware;
import com.phaseshifter.canora.ui.redux.core.Store;

public class ActionLogger<T> implements Middleware<T> {
    private final String LOG_TAG = "ActionLogger";

    @Override
    public void apply(Store<T> store, Action action, Dispatcher next) {
        Log.v(LOG_TAG, "Action: " + (action == null ? null : action.getType()));
        next.dispatch(action);
    }
}