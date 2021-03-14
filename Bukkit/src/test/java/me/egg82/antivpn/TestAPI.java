package me.egg82.antivpn;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import me.egg82.antivpn.api.VPNAPIProvider;
import me.egg82.antivpn.api.model.ip.IPManager;
import me.egg82.antivpn.api.model.player.PlayerManager;
import me.egg82.antivpn.api.model.source.Source;
import me.egg82.antivpn.api.model.source.models.SourceModel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.List;

class TestAPI {
    private static ServerMock server;
    private static BukkitBootstrap plugin;

    @BeforeAll
    static void setup() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(BukkitBootstrap.class);
    }

    @Test
    void testProvider() {
        Assertions.assertDoesNotThrow((Executable) VPNAPIProvider::getInstance);
    }

    @Test
    void testSources() {
        Assertions.assertDoesNotThrow(() -> VPNAPIProvider.getInstance().getSourceManager().getSources());

        List<Source<? extends SourceModel>> sources = VPNAPIProvider.getInstance().getSourceManager().getSources();
        for (Source<? extends SourceModel> source : sources) {
            Assertions.assertDoesNotThrow(() -> source.getResult("8.8.8.8").get());
            Assertions.assertDoesNotThrow(() -> source.getRawResponse("8.8.8.8").get());
        }
    }

    @Test
    void testPlayers() {
        PlayerManager manager = VPNAPIProvider.getInstance().getPlayerManager();
        Assertions.assertDoesNotThrow(() -> manager.getPlayer("egg82").get());
        Assertions.assertDoesNotThrow(() -> manager.checkMcLeaks(manager.getPlayer("egg82").get(), false));
        Assertions.assertDoesNotThrow(() -> manager.checkMcLeaks(manager.getPlayer("egg82").get(), true));
        Assertions.assertDoesNotThrow(() -> manager.checkMcLeaks(manager.getPlayer("egg82").get(), true));
    }

    @Test
    void testIps() {
        IPManager manager = VPNAPIProvider.getInstance().getIPManager();
        Assertions.assertDoesNotThrow(() -> manager.getIP("8.8.8.8").get());
        Assertions.assertDoesNotThrow(() -> manager.cascade("8.8.8.8", false).get());
        Assertions.assertDoesNotThrow(() -> manager.cascade("8.8.8.8", true).get());
        Assertions.assertDoesNotThrow(() -> manager.cascade("8.8.8.8", true).get());
        Assertions.assertDoesNotThrow(() -> manager.consensus("8.8.8.8", false).get());
        Assertions.assertDoesNotThrow(() -> manager.consensus("8.8.8.8", true).get());
        Assertions.assertDoesNotThrow(() -> manager.consensus("8.8.8.8", true).get());
    }

    @AfterAll
    static void destroy() {
        MockBukkit.unmock();
    }
}
