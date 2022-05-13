package com.phaseshifter.canora.ui.redux.middlewares;

import com.phaseshifter.canora.ui.redux.core.Action;
import com.phaseshifter.canora.ui.redux.core.Dispatcher;
import com.phaseshifter.canora.ui.redux.core.Middleware;
import com.phaseshifter.canora.ui.redux.core.Store;

public class Thunk<T> implements Middleware<T> {
    public abstract static class ThunkAction implements Action {
        public abstract Action run();

        @Override
        public String getType() {
            return "THUNK";
        }

        @Override
        public Object getPayload() {
            return null;
        }

        @Override
        public boolean isError() {
            return false;
        }
    }

    @Override
    public void apply(Store<T> store, Action action, Dispatcher next) {
        Action mutableAction = action;
        while (mutableAction instanceof ThunkAction) {
            mutableAction = ((ThunkAction) mutableAction).run();
        }
        next.dispatch(mutableAction);
    }
}