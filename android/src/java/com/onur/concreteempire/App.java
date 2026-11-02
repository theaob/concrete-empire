package com.onur.concreteempire;

import android.app.Application;
import com.google.android.gms.games.PlayGamesSdk;

/** Starts Play Games Services v2 once per process, but only when a project id is configured (res/values/games-ids.xml). */
public class App extends Application {
    static boolean gamesOn;

    @Override
    public void onCreate() {
        super.onCreate();
        gamesOn = getString(R.string.game_services_project_id).trim().length() > 0;
        if (gamesOn) PlayGamesSdk.initialize(this);
    }
}
