package at.petrak.hexcasting.paucal.common;

import at.petrak.hexcasting.paucal.api.PaucalAPI;
import at.petrak.hexcasting.paucal.api.contrib.Contributor;
import at.petrak.hexcasting.paucal.PaucalConfig;
//import blue.endless.jankson.Jankson;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.audio.OggAudioStream;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import net.minecraft.DefaultUncaughtExceptionHandler;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class ContributorsManifest {
    private static final Gson GSON = new Gson();
//    private static final Jankson JANKSON = Jankson.builder().allowBareRootObject().build();

    private static Map<UUID, Contributor> CONTRIBUTORS = Object2ObjectMaps.emptyMap();
    private static Map<String, ByteBuffer> GITHUB_SOUNDS = Object2ObjectMaps.emptyMap();
    private static boolean startedLoading = false;

    @Nullable
    public static Contributor getContributor(UUID uuid) {
        return CONTRIBUTORS.get(uuid);
    }

    public static void loadContributors() {
        if (startedLoading) {
            PaucalAPI.LOGGER.warn("Tried to reload the contributors in the middle of reloading the contributors");
        } else {
            startedLoading = true;

            if (!PaucalConfig.common().loadContributors()) {
                PaucalAPI.LOGGER.info("Contributors disabled in the config!");
                return;
            }

            var thread = new Thread(ContributorsManifest::fetchAndPopulate);
            thread.setName("PAUCAL Contributors Loading Thread");
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(PaucalAPI.LOGGER));
            thread.start();
        }
    }

    private static void fetchAndPopulate() {
        var pair = fetch();
        CONTRIBUTORS = pair.getFirst();
        GITHUB_SOUNDS = pair.getSecond();
        startedLoading = false;
    }

    public static Pair<Map<UUID, Contributor>, Map<String, ByteBuffer>> fetch() {
        // TODO: Delete this entire contributor system
        return Pair.of(new HashMap<>(), GITHUB_SOUNDS);
    }

    @Nullable
    public static OggAudioStream getSound(String name) {
        var oggBytes = GITHUB_SOUNDS.getOrDefault(name, null);
        if (oggBytes == null) {
            PaucalAPI.LOGGER.warn("Tried to load a github sound {} that wasn't found", name);
            return null;
        }

        // Make a new ogg stream that reads the bytes
        try {
            return new OggAudioStream(new ByteArrayInputStream(oggBytes.array()));
        } catch (IOException e) {
            PaucalAPI.LOGGER.error("The github sound {} is an INVALID OGG FILE. This is Really Bad, what are you " +
                "doing.", name, e);
            return null;
        }
    }
}
