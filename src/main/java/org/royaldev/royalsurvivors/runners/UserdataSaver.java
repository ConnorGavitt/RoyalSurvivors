package org.royaldev.royalsurvivors.runners;

import org.royaldev.royalsurvivors.configuration.ConfManager;
import org.royaldev.royalsurvivors.configuration.PConfManager;

public class UserdataSaver implements Runnable {

    @Override
    public void run() {
        PConfManager.saveAllManagers();
        ConfManager.saveAllManagers();
    }

}
