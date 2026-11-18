package com.mentality.xpreserve.xp;

import net.minecraft.world.entity.player.Player;

/** Central authoritative bridge to vanilla player's exact persisted XP total. */
public final class PlayerXp {
    private PlayerXp() {
    }

    /** Reads authoritative visible vanilla XP from level plus in-level progress. */
    public static long currentRawXp(Player player) {
        return XpMath.totalXpFromLevelProgress(player.experienceLevel, player.experienceProgress);
    }

    public static boolean canRepresentRawXp(long rawXp) {
        return rawXp >= 0 && rawXp <= Integer.MAX_VALUE;
    }

    /**
     * Applies an already validated raw-XP change through vanilla's point mutation method.
     *
     * @throws IllegalStateException if the player's state changed after transaction planning
     */
    public static void applyExactRawXp(Player player, long expectedOldRawXp, long newRawXp) {
        long currentRawXp = currentRawXp(player);
        if (currentRawXp != expectedOldRawXp) {
            throw new IllegalStateException("Player raw XP changed before transaction application");
        }
        if (!canRepresentRawXp(newRawXp)) {
            throw new IllegalArgumentException("Raw XP is outside vanilla's integer range: " + newRawXp);
        }

        long delta = Math.subtractExact(newRawXp, currentRawXp);
        player.giveExperiencePoints(Math.toIntExact(delta));
        normalizeExactRawXp(player, newRawXp);
    }

    /** Establishes the three vanilla XP fields from an already validated exact raw total. */
    public static void normalizeExactRawXp(Player player, long rawXp) {
        if (!canRepresentRawXp(rawXp)) {
            throw new IllegalArgumentException("Raw XP is outside vanilla's integer range: " + rawXp);
        }
        // Vanilla death reward reads experienceLevel, not totalExperience. Normalize all three
        // vanilla fields at the single authoritative mutation boundary so a death immediately
        // following a withdrawal/capture cannot observe a stale level or progress value.
        XpState state = XpMath.stateFromTotalXp(rawXp);
        player.totalExperience = Math.toIntExact(rawXp);
        player.experienceLevel = state.level();
        player.experienceProgress = (float) state.xpIntoLevel() / state.xpNeededForNextLevel();
        if (player.totalExperience != (int) rawXp || player.experienceLevel != state.level()
                || Math.abs(player.experienceProgress - (float) state.xpIntoLevel() / state.xpNeededForNextLevel()) > 0.00001F) {
            throw new IllegalStateException("Vanilla player XP mutation did not preserve the planned raw total");
        }
    }
}
