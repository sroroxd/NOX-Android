package de.art.nox;

import android.content.Context;
import android.content.SharedPreferences;

final class NoxConfig {
    static final String PREFS = "nox_options";
    static final String ACTIVE = "active";
    static final String SLEEP = "sleep";
    static final String TURBO = "turbo";
    static final String PASS_THROUGH = "pass_through";
    static final String SIZE = "size";
    static final String X = "x";
    static final String Y = "y";

    static SharedPreferences get(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private NoxConfig() {}
}
