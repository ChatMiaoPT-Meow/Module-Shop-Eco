package cn.autoforged.module_shop.eco.profession;

import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * 真人玩家行为校验守卫：严格排除全自动机器假人、车万女仆、东篱无人机/收割机与旁观模式刷经验。
 */
public final class RealPlayerGuard {
    private RealPlayerGuard() {
    }

    public static ServerPlayer asRealSurvivalPlayer(Entity entity) {
        if (!(entity instanceof ServerPlayer sp)) {
            return null;
        }
        if (sp instanceof FakePlayer) {
            return null;
        }
        if (sp.connection == null || sp.isSpectator()) {
            return null;
        }
        String cls = sp.getClass().getName().toLowerCase(Locale.ROOT);
        if (cls.contains("fake") || cls.contains("deployer") || cls.contains("maid") || cls.contains("uav")) {
            return null;
        }
        return sp;
    }

    public static boolean isRealPlayer(Player player) {
        return asRealSurvivalPlayer(player) != null;
    }
}
