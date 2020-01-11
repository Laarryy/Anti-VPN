package me.egg82.antivpn.apis;

import java.util.UUID;
import me.egg82.antivpn.APIException;

public interface MCLeaksAPI {
    String getName();

    boolean isKeyRequired();

    boolean getResult(UUID playerID) throws APIException;
}
