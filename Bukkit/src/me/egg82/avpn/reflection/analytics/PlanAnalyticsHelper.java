package me.egg82.avpn.reflection.analytics;

import com.djrapitops.plan.api.PlanAPI;

public class PlanAnalyticsHelper {
    // vars

    // constructor
    public PlanAnalyticsHelper() {
        PlanAPI.getInstance().addPluginDataSource(new PlanData());
    }

    // public

    // private

}
