package me.egg82.antivpn.enums;

import co.aikar.locales.MessageKey;
import co.aikar.locales.MessageKeyProvider;

public enum Message implements MessageKeyProvider {
    GENERAL__HEADER,
    GENERAL__ENABLED,
    GENERAL__DISABLED,
    GENERAL__LOAD,
    GENERAL__HOOK_ENABLE,
    GENERAL__HOOK_DISABLE,
    GENERAL__UPDATE,

    ERROR__INTERNAL,

    IMPORT__SAME_STORAGE,
    IMPORT__NO_MASTER,
    IMPORT__NO_SLAVE,
    IMPORT__IPS,
    IMPORT__PLAYERS,
    IMPORT__VPNS,
    IMPORT__MCLEAKS,
    IMPORT__BEGIN,
    IMPORT__END,

    CHECK__BEGIN,
    CHECK__VPN_DETECTED,
    CHECK__NO_VPN_DETECTED,
    CHECK__MCLEAKS_DETECTED,
    CHECK__NO_MCLEAKS_DETECTED,

    SCORE__BEGIN,
    SCORE__TYPE,
    SCORE__SLEEP,
    SCORE__ERROR,
    SCORE__END,

    TEST__BEGIN,
    TEST__ERROR,
    TEST__VPN_DETECTED,
    TEST__NO_VPN_DETECTED,
    TEST__END,

    RELOAD__BEGIN,
    RELOAD__END;

    private final MessageKey key = MessageKey.of(name().toLowerCase().replace("__", "."));
    public MessageKey getMessageKey() { return key; }
}
