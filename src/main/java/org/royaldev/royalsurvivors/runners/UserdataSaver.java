package org.royaldev.royalsurvivors.runners;

import org.royaldev.royalsurvivors.ConfManager;
import org.royaldev.royalsurvivors.PConfManager;
import org.royaldev.royalsurvivors.RoyalSurvivors;

public class UserdataSaver implements Runnable {

    private final RoyalSurvivors plugin;

    public UserdataSaver(RoyalSurvivors instance) {
        plugin = instance;
    }

    @Override
    public void run() {
        synchronized (plugin.pconfs) {
            for (PConfManager pcm : plugin.pconfs.values()) pcm.forceSave();
        }
        synchronized (plugin.confs) {
            for (ConfManager cm : plugin.confs.values()) cm.forceSave();
        }
    }

}
