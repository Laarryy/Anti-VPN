package me.egg82.avpn.apis;

import java.util.Optional;

public interface IFetchAPI {
	//functions
	String getName();
	Optional<Boolean> getResult(String ip);
}
