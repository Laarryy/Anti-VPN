package me.egg82.antivpn.extended;

import me.egg82.antivpn.core.MCLeaksResult;
import me.egg82.antivpn.core.VPNResult;

public interface PostHandler {
    void handle(VPNResult post);
    void handle(MCLeaksResult post);
}
