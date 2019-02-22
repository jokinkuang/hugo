package hugo.weaving.internal;

import android.os.Looper;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import hugo.weaving.io.NLog;

public class Statistics {
    public static class MethodProfile {
        public String name;
        public long cost;

        public MethodProfile(String name, long cost) {
            this.name = name;
            this.cost = cost;
        }
    }

    private static final int MAX_SIZE = 100;
    private static List<MethodProfile> mMethodProfiles = new LinkedList<>();

    public static void add(MethodProfile methodProfile) {
        // main process
        if (Looper.myLooper() != Looper.getMainLooper()) {
            return;
        }

        // empty
        if (mMethodProfiles.size() == 0) {
            mMethodProfiles.add(methodProfile);
            return;
        }

        // sort
        for (int i = 0; i < mMethodProfiles.size(); ++i) {
            MethodProfile target = mMethodProfiles.get(i);
            if (target.cost < methodProfile.cost) {
                mMethodProfiles.add(i, methodProfile);
                break;
            }
        }

        // trim
        if (mMethodProfiles.size() > MAX_SIZE) {
            mMethodProfiles.remove(mMethodProfiles.size() - 1);
        }
    }

    public static void dump() {
        NLog.e(Constants.TAG, "=======================HugoTrace===========================");
        for (MethodProfile methodProfile : mMethodProfiles) {
            NLog.e(Constants.TAG, methodProfile.name + ":" + methodProfile.cost + "ms");
        }
    }
}
