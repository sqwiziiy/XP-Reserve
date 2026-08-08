package com.mentality.xpreserve.xp;

import net.minecraft.world.entity.player.Player;

/** Central authoritative bridge to vanilla player's exact persisted XP total. */
public final class PlayerXp {
    private PlayerXp() {
    }

    /**
     * Reads vanilla's persisted {@link Player#totalExperience} field, which is updated by
     * {@link Player#giveExperiencePoints(int)} together with level and progress.
     */
    public static long currentRawXp(Player player) {
        if (player.totalExperience < 0) {
            throw new IllegalStateException("Vanilla player totalExperience must not be negative");
        }
        return player.totalExperience;
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
        if (player.totalExperience != (int) newRawXp) {
            throw new IllegalStateException("Vanilla player XP mutation did not preserve the planned raw total");
        }
    }
}
