package com.phaseshifter.canora.ui.utils.motionlayout;

import androidx.constraintlayout.motion.widget.MotionLayout;
import com.phaseshifter.canora.R;
import com.phaseshifter.canora.ui.data.constants.SettingsPage;

public class SettingsMotionLayoutController {
    private final MotionLayout layout;

    private SettingsPage page;

    public SettingsMotionLayoutController(MotionLayout layout) {
        this.layout = layout;
    }

    public void setPage(SettingsPage page) {
        if (page == null)
            layout.transitionToState(R.id.state_settings_home);
        else
            layout.transitionToState(R.id.state_settings_view);
        this.page = page;
    }

    public SettingsPage getPage() {
        return page;
    }
}