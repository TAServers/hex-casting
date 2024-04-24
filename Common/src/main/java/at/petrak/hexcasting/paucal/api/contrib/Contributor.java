package at.petrak.hexcasting.paucal.api.contrib;

import at.petrak.hexcasting.paucal.xplat.IXplatAbstractions;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Contributor {
    private final UUID uuid;
    private final int level;
    private final boolean isDev;

    private final float pitchCenter, pitchVariance;
    private final JsonObject otherVals;

    @ApiStatus.Internal
    public Contributor(UUID uuid, JsonObject cfg) {
        this.uuid = uuid;
        this.otherVals = cfg;

        this.level = this.getInt("paucal:contributor_level", 0);
        this.isDev = this.getBool("paucal:is_dev", false);
        this.pitchCenter = this.getFloat("paucal:pat_pitch", 1f);
        this.pitchVariance = this.getFloat("paucal:pat_variance", 0.5f);
    }

    public int getLevel() {
        return level;
    }

    public boolean isDev() {
        return isDev;
    }

    public UUID getUuid() {
        return uuid;
    }


    // =====

    @Nullable
    public String getString(String key) {
        return this.getString(key, null);
    }

    public String getString(String key, String fallback) {
        return GsonHelper.getAsString(otherVals, key, fallback);
    }

    @Nullable
    public Integer getInt(String key) {
        if (otherVals.has(key)) {
            return GsonHelper.getAsInt(otherVals, key);
        } else {
            return null;
        }
    }

    public int getInt(String key, int fallback) {
        return GsonHelper.getAsInt(otherVals, key, fallback);
    }

    @Nullable
    public Float getFloat(String key) {
        if (otherVals.has(key)) {
            return GsonHelper.getAsFloat(otherVals, key);
        } else {
            return null;
        }
    }

    public float getFloat(String key, float fallback) {
        return GsonHelper.getAsFloat(otherVals, key, fallback);
    }

    @Nullable
    public Boolean getBool(String key) {
        if (otherVals.has(key)) {
            return GsonHelper.getAsBoolean(otherVals, key);
        } else {
            return null;
        }
    }

    public boolean getBool(String key, boolean fallback) {
        return GsonHelper.getAsBoolean(this.otherVals, key, fallback);
    }


    public Set<String> allKeys() {
        return this.otherVals.keySet();
    }

    public JsonObject otherVals() {
        return this.otherVals;
    }
}
