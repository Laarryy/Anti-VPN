package me.egg82.avpn.debug;

import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.velocity.BasePlugin;

public class VelocityDebugPrinter implements IDebugPrinter {
    // vars

    // constructor
    public VelocityDebugPrinter() {

    }

    // public
    public void printInfo(String message) {
        ServiceLocator.getService(BasePlugin.class).printInfo(message);
    }

    public void printWarning(String message) {
        ServiceLocator.getService(BasePlugin.class).printWarning(message);
    }

    public void printError(String message) {
        ServiceLocator.getService(BasePlugin.class).printError(message);
    }

    // private

}
