package me.egg82.antivpn.apis;

import me.egg82.antivpn.APIException;

public interface VPNAPI {
    String getName();

    boolean isKeyRequired();

    boolean getResult(String ip) throws APIException;
}
