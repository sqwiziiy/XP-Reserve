package com.mentality.xpreserve.xp;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;

/** Releases stored raw XP through vanilla's own Mending-and-pickup collision path. */
public final class MendingAwareXpRelease {
    private MendingAwareXpRelease() { }

    public record Result(long releasedXp, long playerGainedXp, long repairConsumedXp) { }

    /**
     * Uses one unspawned orb, so vanilla performs its data-driven recursive Mending selection
     * without creating persistent world-orb entities. Returns null when pickup cannot safely run.
     */
    public static Result release(ServerPlayer player, long releasedXp) {
        if (releasedXp <= 0 || releasedXp > Integer.MAX_VALUE) return null;
        long before = PlayerXp.currentRawXp(player);
        if (player.takeXpDelay != 0 || !PlayerXp.canRepresentRawXp(Math.addExact(before, releasedXp))) return null;

        // A command may have updated level/progress without totalExperience. Establish a single
        // exact vanilla state before invoking vanilla's Mending/pickup implementation.
        PlayerXp.normalizeExactRawXp(player, before);

        ExperienceOrb transientOrb = new ExperienceOrb((ServerLevel) player.level(), player.getX(), player.getY(), player.getZ(),
                Math.toIntExact(releasedXp));
        transientOrb.playerTouch(player);
        // This is not a physical world pickup: preserve the player's existing (zero) cooldown so
        // a sequence of deliberate flask interactions cannot silently no-op.
        player.takeXpDelay = 0;

        // playerTouch updates totalExperience from the normalized state. Normalize again because
        // vanilla float progress can otherwise remain infinitesimally below a level boundary.
        long after = player.totalExperience;
        PlayerXp.normalizeExactRawXp(player, after);
        long gained = Math.subtractExact(after, before);
        long repaired = Math.subtractExact(releasedXp, gained);
        if (gained < 0 || repaired < 0) {
            throw new IllegalStateException("Vanilla XP release produced invalid accounting");
        }
        return new Result(releasedXp, gained, repaired);
    }
}
