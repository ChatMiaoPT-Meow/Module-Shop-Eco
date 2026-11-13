package cn.autoforged.module_shop.eco.profession;

import cn.autoforged.module_shop.Config;
import cn.autoforged.module_shop.ModuleShopMod;
import cn.autoforged.module_shop.eco.quality.QualityEvaluator;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.PumpkinBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.brewing.PlayerBrewedPotionEvent;
import net.neoforged.neoforge.event.brewing.PotionBrewEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 八大职业真人行为检测与防刷过滤监听器（100% 软联动，零第三方模组编译期依赖）。
 */
@EventBusSubscriber(modid = ModuleShopMod.MODID)
public final class ProfessionBehaviorListeners {
    private static final Logger LOG = LoggerFactory.getLogger("module_shop_eco/listeners");
    private static final String FRESH_BREW_MARKER = "ms_eco_fresh_brew";

    // ① 战斗：防刷怪塔同区块高频连杀衰减 (Key = uuid + "|" + regionX + "|" + regionZ + "|" + minuteBucket)
    private static final Map<String, Integer> COMBAT_REGION_KILLS = new ConcurrentHashMap<>();
    private static long lastCombatCleanupMinute = -1L;

    // ④ 农牧：挤奶冷却 (Key = entityUUID -> expireGameTime)
    private static final Map<UUID, Long> MILK_COOLDOWN = new ConcurrentHashMap<>();
    // ④/⑤/⑦/⑧ 右键方块前后状态微快照（同一服务端 Tick 末尾比对结算）
    private static final List<PendingRightClickCheck> PENDING_RIGHT_CLICKS = new ArrayList<>();

    // ⑤ 厨师 & ⑧ 魔导师：有界面方块打开期间的容器快照追踪
    private static final Map<UUID, TrackedMenuSession> OPEN_MENUS = new ConcurrentHashMap<>();

    // ⑥ 钓鱼：三通道防重与东篱/捕星者钩子追踪
    private static final Map<UUID, Long> LAST_FISH_AWARD_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> RECENT_BOBBER_RETRIEVE_SOUND_TICK = new ConcurrentHashMap<>();

    // ⑦ 调酒师：森罗酒馆酒龙头 30 tick 延迟接酒追踪
    private static final List<PendingBarrelTapWatch> PENDING_TAP_WATCHES = new ArrayList<>();

    private static volatile boolean optionalHooksRegistered;

    private ProfessionBehaviorListeners() {
    }

    private static final TagKey<Item> TAG_C_SEEDS = TagKey.create(
            Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "seeds"));
    private static final TagKey<Item> TAG_FID_SEEDS = TagKey.create(
            Registries.ITEM, ResourceLocation.fromNamespaceAndPath("fid", "seeds"));
    private static final TagKey<Item> TAG_C_CROPS = TagKey.create(
            Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "crops"));

    private record PendingRightClickCheck(
            UUID playerId,
            ServerLevel level,
            BlockPos pos,
            String blockId,
            int beforeMaturityScore,
            int beforeHoneyLevel,
            boolean isCookingBlock,
            boolean isBartendingBlock,
            boolean isAlchemistCauldron,
            boolean isHangingFruit,
            boolean heldShakerHadResult,
            Map<String, Integer> bagBefore,
            List<UUID> nearbyDropsBefore,
            Map<BlockPos, String> areaHarvestTargets,
            List<BlockPos> stickFruitTargets
    ) {}

    private static final class TrackedMenuSession {
        final int containerId;
        final String mode; // "cooking" or "arcane_anvil"
        final String blockId;
        Map<String, Integer> playerOutputsSnapshot;
        ItemStack anvilIn0 = ItemStack.EMPTY;
        ItemStack anvilIn1 = ItemStack.EMPTY;

        TrackedMenuSession(int containerId, String mode, String blockId, Map<String, Integer> snapshot) {
            this.containerId = containerId;
            this.mode = mode;
            this.blockId = blockId;
            this.playerOutputsSnapshot = snapshot;
        }
    }

    private static final class PendingBarrelTapWatch {
        final UUID playerId;
        final ServerLevel level;
        final BlockPos barrelPos;
        int lastOutputCount;
        int brewLevel;
        int ticksRemaining;

        PendingBarrelTapWatch(UUID playerId, ServerLevel level, BlockPos barrelPos, int lastOutputCount, int brewLevel) {
            this.playerId = playerId;
            this.level = level;
            this.barrelPos = barrelPos;
            this.lastOutputCount = lastOutputCount;
            this.brewLevel = brewLevel;
            this.ticksRemaining = 50;
        }
    }

    /** 启动时动态挂载可选模组专属事件（如铁魔法 InscribeSpellEvent），未安装时静默跳过。 */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static synchronized void registerOptionalModHooks() {
        if (optionalHooksRegistered) {
            return;
        }
        optionalHooksRegistered = true;
        try {
            if (ModList.get().isLoaded("irons_spellbooks")) {
                Class<? extends Event> inscribeCls = (Class<? extends Event>) Class.forName(
                        "io.redspace.ironsspellbooks.api.events.InscribeSpellEvent");
                Method getEntity = inscribeCls.getMethod("getEntity");
                Method getSpellData = inscribeCls.getMethod("getSpellData");
                NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, (Class) inscribeCls, event -> {
                    try {
                        if (event instanceof net.neoforged.bus.api.ICancellableEvent c && c.isCanceled()) {
                            return;
                        }
                        Object playerObj = getEntity.invoke(event);
                        ServerPlayer sp = RealPlayerGuard.asRealSurvivalPlayer(playerObj instanceof Entity e ? e : null);
                        if (sp == null) {
                            return;
                        }
                        Object spellData = getSpellData.invoke(event);
                        long xp = computeIronsSpellDataXp(spellData);
                        ProfessionManager.grantXp(sp, ProfessionConfig.ARCHMAGE, xp, "inscribe");
                    } catch (Throwable ignored) {
                    }
                });
                LOG.info("[ModuleShop-Eco] 已软联动挂载 Iron's Spellbooks 卷轴抄写事件（魔导师）");
            }
        } catch (Throwable ex) {
            LOG.warn("[ModuleShop-Eco] 软联动挂载可选魔法事件跳过：{}", ex.toString());
        }
    }

    private static long computeIronsSpellDataXp(Object spellData) {
        if (spellData == null) {
            return 18L;
        }
        try {
            Method getRarity = spellData.getClass().getMethod("getRarity");
            Method getLevel = spellData.getClass().getMethod("getLevel");
            Object rarity = getRarity.invoke(spellData);
            int lv = Math.max(1, (Integer) getLevel.invoke(spellData));
            long base = 15L;
            if (rarity instanceof Enum<?> e) {
                base = switch (e.name()) {
                    case "UNCOMMON" -> 24L;
                    case "RARE" -> 38L;
                    case "EPIC" -> 60L;
                    case "LEGENDARY" -> 95L;
                    default -> 15L;
                };
            }
            return base + (lv - 1) * 4L;
        } catch (Throwable ignored) {
            return 18L;
        }
    }

    // =========================================================================
    // 1. 【战斗】与【农牧宰杀】—— 实体死亡归属检测 + 刷怪塔衰减
    // =========================================================================

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !ProfessionManager.isEnabled()) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (victim == null || victim.level().isClientSide()) {
            return;
        }
        ServerPlayer killer = resolveRealPlayerAttacker(event.getSource().getEntity(), event.getSource().getDirectEntity());
        if (killer == null) {
            return;
        }

        // A. 敌对怪物或 Boss -> 【战斗】职业
        boolean isBoss = victim.getMaxHealth() >= 150.0F
                || victim.getType().is(EntityTypeTags.RAIDERS) && victim.getMaxHealth() >= 80.0F
                || BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).getPath().contains("boss");
        if (victim instanceof Enemy || isBoss) {
            float maxHp = Math.max(1.0F, victim.getMaxHealth());
            long baseXp;
            if (maxHp >= 200.0F) {
                baseXp = 120L; // 灾变 / 暮色 / 铁魔法大 Boss
            } else if (maxHp >= 80.0F) {
                baseXp = 45L;  // 精英怪 / 小 Boss
            } else if (maxHp >= 35.0F) {
                baseXp = 14L;  // 末影人 / 猪灵蛮兵等中大型怪
            } else {
                baseXp = 6L;   // 普通僵尸骷髅
            }

            if (!isBoss) {
                double decay = computeGrinderDecay(killer, victim.blockPosition());
                baseXp = Math.round(baseXp * decay);
            }
            if (baseXp > 0L) {
                ProfessionManager.grantXp(killer, ProfessionConfig.COMBAT, baseXp, "kill_mob");
            }
            return;
        }

        // B. 成年家畜/东篱禽畜宰杀 -> 【农牧】职业
        if (isAdultLivestock(victim)) {
            ProfessionManager.grantXp(killer, ProfessionConfig.FARMING, 6L, "butcher");
        }
    }

    private static ServerPlayer resolveRealPlayerAttacker(Entity sourceEntity, Entity directEntity) {
        ServerPlayer sp = RealPlayerGuard.asRealSurvivalPlayer(sourceEntity);
        if (sp != null) {
            return sp;
        }
        if (directEntity instanceof Projectile proj && proj.getOwner() != null) {
            return RealPlayerGuard.asRealSurvivalPlayer(proj.getOwner());
        }
        return RealPlayerGuard.asRealSurvivalPlayer(directEntity);
    }

    private static double computeGrinderDecay(ServerPlayer player, BlockPos pos) {
        long nowMinute = System.currentTimeMillis() / 60_000L;
        if (nowMinute != lastCombatCleanupMinute) {
            lastCombatCleanupMinute = nowMinute;
            String suffix = "|" + nowMinute;
            COMBAT_REGION_KILLS.keySet().removeIf(k -> !k.endsWith(suffix));
        }
        int rx = (pos.getX() >> 4) / 3;
        int rz = (pos.getZ() >> 4) / 3;
        String key = player.getUUID() + "|" + rx + "|" + rz + "|" + nowMinute;
        int count = COMBAT_REGION_KILLS.merge(key, 1, Integer::sum);
        int threshold = Math.max(5, Config.INSTANCE.combatGrinderKillThreshold.get());
        if (count <= threshold) {
            return 1.0D;
        }
        if (count >= threshold * 2) {
            return 0.0D;
        }
        return (double) (threshold * 2 - count) / (double) threshold;
    }

    private static boolean isAdultLivestock(LivingEntity entity) {
        if (entity instanceof Animal animal) {
            return !animal.isBaby();
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id != null && "flavor_immersed_daily_argo".equals(id.getNamespace())) {
            String p = id.getPath();
            return (p.contains("duck") || p.contains("goose") || p.contains("turkey")
                    || p.contains("pigeon") || p.contains("quail") || p.contains("bullfrog")
                    || p.contains("bamboo_rat") || p.contains("bamboorat")) && !entity.isBaby();
        }
        return false;
    }

    // =========================================================================
    // 2. 【附魔】—— 附魔台消耗人物经验等级完成附魔（兼容青金石与森罗末地虚空海螺）
    // =========================================================================

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerLevelChange(PlayerXpEvent.LevelChange event) {
        if (event.isCanceled() || event.getLevels() >= 0 || !ProfessionManager.isEnabled()) {
            return;
        }
        ServerPlayer sp = RealPlayerGuard.asRealSurvivalPlayer(event.getEntity());
        if (sp == null) {
            return;
        }
        if (sp.containerMenu instanceof EnchantmentMenu) {
            int tier = Math.min(3, Math.max(1, -event.getLevels()));
            long xp = switch (tier) {
                case 1 -> 12L;
                case 2 -> 28L;
                default -> 55L;
            };
            ProfessionManager.grantXp(sp, ProfessionConfig.ENCHANTING, xp, "enchant_table");
        }
    }

    // =========================================================================
    // 3. 【药水】—— 酿造台读完条贴新鲜出锅标记，真人端走加经验并当场撕掉标记防刷
    // =========================================================================

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPotionBrewPost(PotionBrewEvent.Post event) {
        if (!ProfessionManager.isEnabled()) {
            return;
        }
        for (int i = 0; i < Math.min(3, event.getLength()); i++) {
            ItemStack stack = event.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof PotionItem && !isPlainWaterBottle(stack)) {
                markFreshBrewedPotion(stack);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerBrewedPotion(PlayerBrewedPotionEvent event) {
        if (!ProfessionManager.isEnabled()) {
            return;
        }
        ServerPlayer sp = RealPlayerGuard.asRealSurvivalPlayer(event.getEntity());
        ItemStack stack = event.getStack();
        if (sp == null || stack == null || stack.isEmpty()) {
            return;
        }
        if (consumeFreshBrewedMarker(stack) && !isPlainWaterBottle(stack)) {
            ProfessionManager.grantXp(sp, ProfessionConfig.BREWING, 18L * Math.max(1, stack.getCount()), "brewing_stand");
        }
    }

    private static boolean isPlainWaterBottle(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return true;
        }
        return contents.is(Potions.WATER) || contents.is(Potions.MUNDANE) || contents.is(Potions.THICK);
    }

    private static void markFreshBrewedPotion(ItemStack stack) {
        CustomData cd = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = cd.copyTag();
        tag.putBoolean(FRESH_BREW_MARKER, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static boolean consumeFreshBrewedMarker(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null || cd.isEmpty()) {
            return false;
        }
        CompoundTag tag = cd.copyTag();
        if (!tag.getBoolean(FRESH_BREW_MARKER)) {
            return false;
        }
        tag.remove(FRESH_BREW_MARKER);
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return true;
    }

    // =========================================================================
    // 4. 【农牧】—— 人工放置防刷 + 左键满熟破坏 + 右键摘果 + 繁殖/剪毛/挤奶/采蜜
    // =========================================================================

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }
        Block block = event.getPlacedBlock().getBlock();
        if (isPlaceTrackablePlant(block) || isHangingFruitBlock(block) || isForageableGrassBlock(block)) {
            PlacedBlockTracker.markPlaced(level, event.getPos());
        }
    }

    private static boolean isPlaceTrackablePlant(Block block) {
        return block == Blocks.MELON || block == Blocks.PUMPKIN || block instanceof PumpkinBlock
                || block instanceof SugarCaneBlock || block instanceof BambooStalkBlock
                || block instanceof CactusBlock || block instanceof CocoaBlock;
    }

    /** 识别悬挂果实类方块（如南山 HangingFruitBlock / FallingFruitBlock），避免误判无属性空气或普通叶子 */
    private static boolean isHangingFruitBlock(Block block) {
        String cls = block.getClass().getSimpleName();
        return "HangingFruitBlock".equals(cls) || "FallingFruitBlock".equals(cls);
    }

    /** 识别可通过工具/捕虫网采集野生种子的草本方块 */
    private static boolean isForageableGrassBlock(Block block) {
        return block == Blocks.SHORT_GRASS || block == Blocks.TALL_GRASS
                || block == Blocks.FERN || block == Blocks.LARGE_FERN;
    }

    private static boolean isForageableSeedOrCropDrop(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.is(TAG_C_SEEDS) || stack.is(TAG_FID_SEEDS) || stack.is(TAG_C_CROPS)) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) {
            return false;
        }
        String ns = id.getNamespace();
        String path = id.getPath();
        return ("flavor_immersed_daily".equals(ns) || "flavor_immersed_daily_argo".equals(ns))
                && (path.endsWith("_seeds") || path.endsWith("_seed") || path.contains("shepherds_purse"));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !ProfessionManager.isEnabled() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer sp = RealPlayerGuard.asRealSurvivalPlayer(event.getPlayer());
        if (sp == null) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        Block block = state.getBlock();

        if (isPlaceTrackablePlant(block)) {
            if (PlacedBlockTracker.consumeIfPlayerPlaced(level, pos)) {
                return; // 玩家自己摆在地上再挖掉，0 经验
            }
            if (block instanceof CocoaBlock) {
                if (state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE) {
                    ProfessionManager.grantXp(sp, ProfessionConfig.FARMING, 4L, "harvest_cocoa");
                }
                return;
            }
            ProfessionManager.grantXp(sp, ProfessionConfig.FARMING, 3L, "harvest_natural");
            return;
        }

        if (isHangingFruitBlock(block)) {
            if (!PlacedBlockTracker.consumeIfPlayerPlaced(level, pos)) {
                ProfessionManager.grantXp(sp, ProfessionConfig.FARMING, 4L, "harvest_fruit");
            }
            return;
        }

        if (block instanceof CropBlock crop && crop.isMaxAge(state)) {
            ProfessionManager.grantXp(sp, ProfessionConfig.FARMING, 4L, "harvest_crop");
        } else if (block instanceof NetherWartBlock && state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE) {
            ProfessionManager.grantXp(sp, ProfessionConfig.FARMING, 4L, "harvest_wart");
        } else if (block instanceof SweetBerryBushBlock && state.getValue(SweetBerryBushBlock.AGE) >= 2) {
            ProfessionManager.grantXp(sp, ProfessionConfig.FARMING, 3L, "harvest_berry");
        } else if (isCropLikeBlock(block, state) && extractMaturityScore(state) >= 100) {
            // 兼容任意模组自定义满熟作物方块（age == maxAge）
            ProfessionManager.grantXp(sp, ProfessionConfig.FARMING, 4L, "harvest_mod_crop");
        }
    }

    /** 兼容南山捕虫网打草掉野生种子等 BlockDropsEvent 场景（排除玩家自放草方块刷经验） */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockDrops(BlockDropsEvent event) {
        if (event.isCanceled() || !ProfessionManager.isEnabled() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockState state = event.getState();
        if (!isForageableGrassBlock(state.getBlock())) {
            return;
        }
        if (PlacedBlockTracker.consumeIfPlayerPlaced(level, event.getPos())) {
            return;
        }
        ServerPlayer sp = RealPlayerGuard.asRealSurvivalPlayer(event.getBreaker());
        if (sp == null) {
            return;
        }
        boolean hasSeedOrCrop = event.getDrops().stream()
                .map(ItemEntity::getItem)
                .anyMatch(ProfessionBehaviorListeners::isForageableSeedOrCropDrop);
        if (hasSeedOrCrop) {
            ProfessionManager.grantXp(sp, ProfessionConfig.FARMING, 2L, "forage_seeds");
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBabySpawn(BabyEntitySpawnEvent event) {
        if (event.isCanceled() || !ProfessionManager.isEnabled()) {
            return;
        }
        ServerPlayer sp = RealPlayerGuard.asRealSurvivalPlayer(event.getCausedByPlayer());
        if (sp != null) {
            ProfessionManager.grantXp(sp, ProfessionConfig.FARMING, 12L, "breed_animal");
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.isCanceled() || event.getLevel().isClientSide() || !ProfessionManager.isEnabled()) {
            return;
        }
        ServerPlayer sp = RealPlayerGuard.asRealSurvivalPlayer(event.getEntity());
        if (sp == null) {
            return;
        }
        ItemStack held = event.getItemStack();
        Entity target = event.getTarget();

        // 剪羊毛 / 蘑菇牛剪菇
        if (held.getItem() instanceof ShearsItem) {
            if ((target instanceof Sheep sheep && sheep.readyForShearing())
                    || (target instanceof MushroomCow mooshroom && mooshroom.readyForShearing())
                    || (target instanceof SnowGolem golem && golem.readyForShearing())) {
                ProfessionManager.grantXp(sp, ProfessionConfig.FARMING, 6L, "shear");
            }
            return;
        }

        // 挤牛奶 / 羊奶（每头牛 5 分钟冷却防无限连点刷级）
        if (held.is(Items.BUCKET) && (target instanceof Cow || target instanceof Goat) && !((Animal) target).isBaby()) {
            long nowTick = event.getLevel().getGameTime();
            Long until = MILK_COOLDOWN.get(target.getUUID());
            if (until == null || nowTick >= until) {
                MILK_COOLDOWN.put(target.getUUID(), nowTick + 6000L);
                ProfessionManager.grantXp(sp, ProfessionConfig.FARMING, 5L, "milk");
            }
        }
    }

    // =========================================================================
    // 5. 【厨师】【调酒师】【农牧右键摘果】【魔导师炼金锅】—— 右键方块前后精准比对
    // =========================================================================

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlockPre(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level) || !ProfessionManager.isEnabled()) {
            return;
        }
        ServerPlayer sp = RealPlayerGuard.asRealSurvivalPlayer(event.getEntity());
        if (sp == null) {
            return;
        }
        BlockPos pos = event.getPos().immutable();
        BlockState state = level.getBlockState(pos);
        ResourceLocation blockKey = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String blockId = blockKey == null ? "" : blockKey.toString();

        // 如果玩家手里拿药水往酿造台塞，先清掉可能残留的新鲜标记防刷
        if ("minecraft:brewing_stand".equals(blockId)) {
            consumeFreshBrewedMarker(event.getItemStack());
        }

        ProfessionConfig cfg = ProfessionConfig.get();
        boolean isCooking = cfg.isCookingBlockBound(blockId);
        boolean isBartending = cfg.isBartendingBlockBound(blockId);
        boolean isAlchCauldron = "irons_spellbooks:alchemist_cauldron".equals(blockId);

        // 森罗酒馆酒龙头特殊异步机制：右键龙头 30 刻后才从背后酒桶出酒
        if ("kaleidoscope_tavern:tap".equals(blockId)) {
            registerTapWatchIfAttachedToBarrel(sp, level, pos, state);
        }

        int maturityBefore = extractMaturityScore(state);
        int honeyBefore = state.hasProperty(BeehiveBlock.HONEY_LEVEL) ? state.getValue(BeehiveBlock.HONEY_LEVEL) : -1;
        boolean shakerHadResult = hasTavernShakerResult(event.getItemStack());
        boolean isHangingFruit = isHangingFruitBlock(state.getBlock());

        ItemStack heldStack = event.getItemStack();
        Map<BlockPos, String> areaHarvestTargets = collectAreaHarvestTargets(level, pos, heldStack);
        List<BlockPos> stickFruitTargets = collectStickFruitTargets(level, pos, state, heldStack);

        if (!isCooking && !isBartending && !isAlchCauldron && !isHangingFruit
                && maturityBefore < 100 && honeyBefore < 5
                && areaHarvestTargets.isEmpty() && stickFruitTargets.isEmpty()) {
            return;
        }

        Map<String, Integer> bagSnap = (isCooking || isBartending || isAlchCauldron)
                ? snapshotPlayerItems(sp) : Map.of();
        List<UUID> dropsBefore = (isCooking || isBartending)
                ? snapshotNearbyItemEntities(level, pos) : List.of();

        synchronized (PENDING_RIGHT_CLICKS) {
            PENDING_RIGHT_CLICKS.add(new PendingRightClickCheck(
                    sp.getUUID(), level, pos, blockId,
                    maturityBefore, honeyBefore,
                    isCooking, isBartending, isAlchCauldron,
                    isHangingFruit, shakerHadResult, bagSnap, dropsBefore,
                    areaHarvestTargets, stickFruitTargets
            ));
        }
    }

    private static boolean isCropLikeBlock(Block block, BlockState state) {
        return block instanceof CropBlock
                || block instanceof BushBlock
                || block instanceof StemBlock
                || block instanceof BonemealableBlock
                || block instanceof LeavesBlock
                || isHangingFruitBlock(block);
    }

    private static boolean isAreaHarvestTool(ItemStack held) {
        if (held == null || held.isEmpty()) {
            return false;
        }
        if (held.getItem() instanceof HoeItem) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(held.getItem());
        if (id == null) {
            return false;
        }
        String path = id.getPath();
        return path.contains("sickle") || path.contains("scythe");
    }

    private static Map<BlockPos, String> collectAreaHarvestTargets(ServerLevel level, BlockPos center, ItemStack held) {
        if (!isAreaHarvestTool(held)) {
            return Map.of();
        }
        Map<BlockPos, String> targets = new HashMap<>();
        for (BlockPos scanPos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            BlockState s = level.getBlockState(scanPos);
            if (isCropLikeBlock(s.getBlock(), s) && extractMaturityScore(s) >= 100) {
                ResourceLocation key = BuiltInRegistries.BLOCK.getKey(s.getBlock());
                targets.put(scanPos.immutable(), key == null ? "" : key.toString());
            }
        }
        return targets.isEmpty() ? Map.of() : Map.copyOf(targets);
    }

    private static List<BlockPos> collectStickFruitTargets(
            ServerLevel level, BlockPos logPos, BlockState clickedState, ItemStack held) {
        if (held == null || !held.is(Items.STICK) || !clickedState.is(BlockTags.LOGS)) {
            return List.of();
        }
        List<BlockPos> targets = new ArrayList<>();
        for (BlockPos scanPos : BlockPos.betweenClosed(logPos.offset(-6, -2, -6), logPos.offset(6, 10, 6))) {
            BlockState s = level.getBlockState(scanPos);
            Block b = s.getBlock();
            if (isHangingFruitBlock(b) || (b instanceof LeavesBlock && extractMaturityScore(s) >= 100)) {
                targets.add(scanPos.immutable());
            }
        }
        return targets.isEmpty() ? List.of() : List.copyOf(targets);
    }

    private static boolean hasTavernShakerResult(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null || !"kaleidoscope_tavern:shaker".equals(id.toString())) {
            return false;
        }
        ResourceLocation compId = ResourceLocation.fromNamespaceAndPath("kaleidoscope_tavern", "shaker_result");
        var opt = BuiltInRegistries.DATA_COMPONENT_TYPE.getOptional(compId);
        return opt.isPresent() && stack.has(opt.get());
    }

    private static void registerTapWatchIfAttachedToBarrel(ServerPlayer sp, ServerLevel level, BlockPos tapPos, BlockState tapState) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos adj = tapPos.relative(dir);
            BlockEntity be = level.getBlockEntity(adj);
            if (be == null) continue;
            ResourceLocation beBlockId = BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock());
            if (beBlockId != null && "kaleidoscope_tavern:barrel".equals(beBlockId.toString())) {
                CompoundTag nbt = be.saveWithoutMetadata(level.registryAccess());
                if ("kaleidoscope_tavern:empty".equals(nbt.getString("recipe_id"))) {
                    return; // 错配方出的是醋，不给调酒师经验
                }
                int outCount = countBarrelOutputFromNbt(nbt);
                int brewLv = Math.max(1, nbt.getInt("brew_level"));
                if (outCount > 0) {
                    synchronized (PENDING_TAP_WATCHES) {
                        PENDING_TAP_WATCHES.removeIf(w -> w.barrelPos.equals(adj));
                        PENDING_TAP_WATCHES.add(new PendingBarrelTapWatch(sp.getUUID(), level, adj.immutable(), outCount, brewLv));
                    }
                }
                return;
            }
        }
    }

    private static int countBarrelOutputFromNbt(CompoundTag barrelTag) {
        if (!barrelTag.contains("output", 10)) {
            return 0;
        }
        CompoundTag out = barrelTag.getCompound("output");
        if (out.contains("Items", 9)) {
            var list = out.getList("Items", 10);
            if (!list.isEmpty()) {
                return Math.max(1, list.getCompound(0).getInt("count"));
            }
        }
        return 0;
    }

    /**
     * 提取方块是否处于“满熟/可采摘”状态：返回 100 表示满熟可采，返回 0~99 表示未熟，-1 表示非生长方块。
     * 通吃原版 + harvest_with_ease + 森罗架子作物 + 丰饶果树/茶树 + 东篱果树菌木 + 农夫乐事！
     */
    private static int extractMaturityScore(BlockState state) {
        // 先调用 CropBlock.isMaxAge，兼容如南山作物复用原版 AGE(0..7) 但覆写 getMaxAge()=3/4 的方块
        if (state.getBlock() instanceof CropBlock crop) {
            int max = Math.max(1, crop.getMaxAge());
            return crop.isMaxAge(state) ? 100 : (crop.getAge(state) * 90 / max);
        }
        for (Property<?> prop : state.getProperties()) {
            String name = prop.getName().toLowerCase(Locale.ROOT);
            if (prop instanceof IntegerProperty intProp
                    && (name.equals("age") || name.equals("stage") || name.equals("fruit") || name.equals("maturity"))) {
                int cur = state.getValue(intProp);
                int max = intProp.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(cur);
                if (max > 0) {
                    return cur >= max ? 100 : (cur * 90 / max);
                }
            } else if (prop instanceof BooleanProperty boolProp
                    && (name.equals("berries") || name.equals("has_fruit") || name.equals("ripe"))) {
                return Boolean.TRUE.equals(state.getValue(boolProp)) ? 100 : 0;
            }
        }
        return -1;
    }

    // =========================================================================
    // 5B. 有界面厨具（烟熏炉/烟火凡人心锅/烘焙坊/火锅）与奥术铁砧监控
    // =========================================================================

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (!ProfessionManager.isEnabled()) {
            return;
        }
        ServerPlayer sp = RealPlayerGuard.asRealSurvivalPlayer(event.getEntity());
        ItemStack smelted = event.getSmelting();
        if (sp == null || smelted == null || smelted.isEmpty() || QualityEvaluator.isRejectedWaste(smelted)) {
            return;
        }
        if (ProfessionConfig.COOKING.equals(ProfessionManager.matchProfessionForOutput(smelted))
                && isEligibleCookedDishForXp(smelted)) {
            ProfessionManager.grantXp(sp, ProfessionConfig.COOKING, 6L * Math.max(1, smelted.getCount()), "smelt_cook");
        }
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!ProfessionManager.isEnabled()) {
            return;
        }
        ServerPlayer sp = RealPlayerGuard.asRealSurvivalPlayer(event.getEntity());
        if (sp == null) {
            return;
        }
        AbstractContainerMenu menu = event.getContainer();
        if (menu instanceof BrewingStandMenu) {
            // 打开酿造台时绝不清除酿造台内部标记，但清理玩家自己背包里可能带旧标记的药水防倒灌刷经验
            for (ItemStack s : sp.getInventory().items) {
                consumeFreshBrewedMarker(s);
            }
            return;
        }
        String menuCls = menu.getClass().getName();
        if ("io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu".equals(menuCls)) {
            TrackedMenuSession sess = new TrackedMenuSession(menu.containerId, "arcane_anvil", "irons_spellbooks:arcane_anvil", Map.of());
            if (menu.slots.size() >= 2) {
                sess.anvilIn0 = menu.slots.get(0).getItem().copy();
                sess.anvilIn1 = menu.slots.get(1).getItem().copy();
            }
            OPEN_MENUS.put(sp.getUUID(), sess);
            return;
        }
        // 若是刚通过右键绑定做饭方块打开的非原版熔炉自定义厨具菜单（如 fidworkblock CookingMenu、bakeries、hotpot）
        if (isLikelyModCookingMenu(menuCls)) {
            OPEN_MENUS.put(sp.getUUID(), new TrackedMenuSession(
                    menu.containerId, "cooking", menuCls, snapshotPlayerItems(sp)));
        }
    }

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            TrackedMenuSession sess = OPEN_MENUS.remove(sp.getUUID());
            if (sess != null && "cooking".equals(sess.mode)) {
                settleMenuCookingDelta(sp, sess);
            }
        }
    }

    private static boolean isLikelyModCookingMenu(String menuClassName) {
        String lower = menuClassName.toLowerCase(Locale.ROOT);
        return lower.startsWith("com.fidtest.")
                || lower.contains("bakeries")
                || lower.contains("hotpot")
                || lower.contains("bountifulfares")
                || lower.contains("baishi")
                || lower.contains("kaleidoscope")
                || lower.contains("flavor_immersed")
                || lower.contains("farmersdelight")
                || lower.contains("cooking")
                || lower.contains("brewinandchewin");
    }

    private static void settleMenuCookingDelta(ServerPlayer sp, TrackedMenuSession sess) {
        Map<String, Integer> now = snapshotPlayerItems(sp);
        int gainedDishes = ProfessionOutputFilter.advanceHighWaterCookDelta(sess.playerOutputsSnapshot, now);
        if (gainedDishes > 0) {
            ProfessionManager.grantXp(sp, ProfessionConfig.COOKING, 10L * Math.min(64, gainedDishes), "menu_cook");
        }
    }

    // =========================================================================
    // 6. 【钓鱼】三通道全打通（原版 + 捕星者小游戏 + 东篱南山居遛鱼搏斗）
    // =========================================================================

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onVanillaItemFished(ItemFishedEvent event) {
        if (event.isCanceled() || !ProfessionManager.isEnabled()) {
            return;
        }
        ServerPlayer sp = RealPlayerGuard.asRealSurvivalPlayer(event.getEntity());
        if (sp == null) {
            return;
        }
        NonNullList<ItemStack> drops = event.getDrops();
        if (drops == null || drops.isEmpty()) {
            return;
        }
        awardFishingOncePerTick(sp, 18L);
    }

    @SubscribeEvent
    public static void onPlaySoundAtPos(PlayLevelSoundEvent.AtPosition event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getSound() == null || !ProfessionManager.isEnabled()) {
            return;
        }
        // 东篱南山居 ModFishingHook.giveCatch 胜利结算时必在玩家坐标播放 FISHING_BOBBER_RETRIEVE
        if (event.getSound().value() == SoundEvents.FISHING_BOBBER_RETRIEVE) {
            var pos = event.getPosition();
            for (ServerPlayer sp : level.players()) {
                if (RealPlayerGuard.asRealSurvivalPlayer(sp) != null && sp.distanceToSqr(pos) <= 4.0D) {
                    RECENT_BOBBER_RETRIEVE_SOUND_TICK.put(sp.getUUID(), level.getGameTime());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level) || !ProfessionManager.isEnabled()) {
            return;
        }
        Entity entity = event.getEntity();
        String cls = entity.getClass().getName();

        // 通道 ③：东篱南山居 ModFishingHook 遛鱼成功收竿
        if ("com.flavor_immersed_daily_argo.fishing.ModFishingHook".equals(cls) && entity instanceof Projectile proj) {
            ServerPlayer owner = RealPlayerGuard.asRealSurvivalPlayer(proj.getOwner());
            if (owner != null) {
                Long soundTick = RECENT_BOBBER_RETRIEVE_SOUND_TICK.get(owner.getUUID());
                if (soundTick != null && Math.abs(level.getGameTime() - soundTick) <= 2L) {
                    awardFishingOncePerTick(owner, 22L);
                }
            }
            return;
        }

        // 通道 ②：捕星者 FishingBobEntity 小游戏完结发鱼
        if ("com.wdiscute.starcatcher.bobentity.FishingBobEntity".equals(cls)) {
            // 在同一 tick 末尾检查附近持捕星者鱼竿的真人玩家是否刚拿到了带 caught_fish_info 的新鱼
            for (ServerPlayer sp : level.players()) {
                if (RealPlayerGuard.asRealSurvivalPlayer(sp) != null && sp.distanceToSqr(entity) <= 1024.0D) {
                    synchronized (PENDING_RIGHT_CLICKS) {
                        PENDING_RIGHT_CLICKS.add(new PendingRightClickCheck(
                                sp.getUUID(), level, sp.blockPosition(), "starcatcher:fishing_bob",
                                -1, -1, false, false, false, false, false,
                                snapshotPlayerItems(sp), snapshotNearbyItemEntities(level, sp.blockPosition()),
                                Map.of(), List.of()
                        ));
                    }
                }
            }
        }
    }

    private static void awardFishingOncePerTick(ServerPlayer sp, long xp) {
        long tick = sp.level().getGameTime();
        Long prev = LAST_FISH_AWARD_TICK.put(sp.getUUID(), tick);
        if (prev != null && Math.abs(tick - prev) <= 3L) {
            return;
        }
        ProfessionManager.grantXp(sp, ProfessionConfig.FISHING, xp, "fishing");
    }

    // =========================================================================
    // Tick 末尾统一结算：右键出锅/摘果/调酒/炼金锅/捕星者鱼获/酒桶龙头/打开中的厨具菜单
    // =========================================================================

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        if (!ProfessionManager.isEnabled()) {
            return;
        }
        processPendingRightClicks(event);
        processPendingTapWatches(event);
        if (event.getServer().getTickCount() % 10 == 0) {
            processOpenMenus(event);
        }
    }

    private static void processPendingRightClicks(ServerTickEvent.Post event) {
        List<PendingRightClickCheck> batch;
        synchronized (PENDING_RIGHT_CLICKS) {
            if (PENDING_RIGHT_CLICKS.isEmpty()) {
                return;
            }
            batch = new ArrayList<>(PENDING_RIGHT_CLICKS);
            PENDING_RIGHT_CLICKS.clear();
        }

        for (PendingRightClickCheck check : batch) {
            ServerPlayer sp = event.getServer().getPlayerList().getPlayer(check.playerId());
            if (sp == null) {
                continue;
            }

            // 捕星者小游戏刚结束时的渔获增量检测
            if ("starcatcher:fishing_bob".equals(check.blockId())) {
                if (hasNewFishInWorldOrBag(sp, check.level(), check.pos(), check.bagBefore(), check.nearbyDropsBefore())) {
                    awardFishingOncePerTick(sp, 22L);
                }
                continue;
            }

            BlockState afterState = check.level().getBlockState(check.pos());
            ResourceLocation afterKey = BuiltInRegistries.BLOCK.getKey(afterState.getBlock());
            String afterBlockId = afterKey == null ? "" : afterKey.toString();

            // A. 右键采摘成熟农作物 / 果树 / 茶树 / 葡萄架（同方块生长阶段从满熟回落）
            if (!check.areaHarvestTargets().containsKey(check.pos())
                    && check.beforeMaturityScore() >= 100
                    && afterBlockId.equals(check.blockId())) {
                int afterMaturity = extractMaturityScore(afterState);
                if (afterMaturity >= 0 && afterMaturity < check.beforeMaturityScore()) {
                    ProfessionManager.grantXp(sp, ProfessionConfig.FARMING, 4L, "right_click_harvest");
                }
            }

            // A2. 镰刀 / 锄头等 3x3 范围右键收割成熟作物（如南山镰刀将作物直接设回空气或低成熟度）
            if (!check.areaHarvestTargets().isEmpty()) {
                int areaHarvested = 0;
                for (Map.Entry<BlockPos, String> entry : check.areaHarvestTargets().entrySet()) {
                    BlockState cur = check.level().getBlockState(entry.getKey());
                    if (cur.isAir()) {
                        areaHarvested++;
                        continue;
                    }
                    ResourceLocation curKey = BuiltInRegistries.BLOCK.getKey(cur.getBlock());
                    String curId = curKey == null ? "" : curKey.toString();
                    if (!curId.equals(entry.getValue())
                            || (isCropLikeBlock(cur.getBlock(), cur) && extractMaturityScore(cur) < 100)) {
                        areaHarvested++;
                    }
                }
                if (areaHarvested > 0) {
                    ProfessionManager.grantXp(sp, ProfessionConfig.FARMING, 4L * Math.min(16, areaHarvested), "right_click_harvest");
                }
            }

            // A3. 无成熟度属性的悬挂果实方块右键直接采摘（排除玩家自放方块）
            if (check.isHangingFruit()
                    && (afterState.isAir() || !afterBlockId.equals(check.blockId()))
                    && !PlacedBlockTracker.consumeIfPlayerPlaced(check.level(), check.pos())) {
                ProfessionManager.grantXp(sp, ProfessionConfig.FARMING, 4L, "right_click_harvest_fruit");
            }

            // A4. 木棍敲击树干摇落周围树冠果实（如南山木棍敲树）
            if (!check.stickFruitTargets().isEmpty()) {
                int stickHarvested = 0;
                for (BlockPos targetPos : check.stickFruitTargets()) {
                    BlockState cur = check.level().getBlockState(targetPos);
                    boolean picked = cur.isAir()
                            || (!isHangingFruitBlock(cur.getBlock()) && extractMaturityScore(cur) < 100);
                    if (picked && !PlacedBlockTracker.consumeIfPlayerPlaced(check.level(), targetPos)) {
                        stickHarvested++;
                    }
                }
                if (stickHarvested > 0) {
                    ProfessionManager.grantXp(sp, ProfessionConfig.FARMING, 3L * Math.min(16, stickHarvested), "stick_harvest_fruit");
                }
            }

            // B. 右键剪满格蜂蜜
            if (check.beforeHoneyLevel() >= 5 && afterState.hasProperty(BeehiveBlock.HONEY_LEVEL)
                    && afterState.getValue(BeehiveBlock.HONEY_LEVEL) == 0) {
                ProfessionManager.grantXp(sp, ProfessionConfig.FARMING, 10L, "harvest_honey");
            }

            // C. 无界面做饭方块盛菜出锅 / 调酒方块出酒 / 铁魔法炼金锅舀墨水药剂
            if (check.isCookingBlock() || check.isBartendingBlock() || check.isAlchemistCauldron()) {
                Map<String, Integer> bagAfter = snapshotPlayerItems(sp);
                List<ItemStack> newDrops = collectNewNearbyDrops(check.level(), check.pos(), check.nearbyDropsBefore());

                if (check.isCookingBlock()) {
                    int dishes = countPositiveDeltaByPrefix(check.bagBefore(), bagAfter, "COOK|")
                            + countMatchingStacksForProf(newDrops, ProfessionConfig.COOKING);
                    if (dishes > 0) {
                        ProfessionManager.grantXp(sp, ProfessionConfig.COOKING, 10L * Math.min(32, dishes), "appliance_cook");
                    }
                }

                if (check.isBartendingBlock()) {
                    // 调酒壶倒入空酒杯成杯：空酒杯方块会变成鸡尾酒方块，同时手中调酒壶失去 shaker_result
                    if ("kaleidoscope_tavern:empty_glassware".equals(check.blockId())) {
                        if (check.heldShakerHadResult()
                                && !afterBlockId.equals(check.blockId())
                                && !"kaleidoscope_tavern:mystery_cocktail".equals(afterBlockId)) {
                            ProfessionManager.grantXp(sp, ProfessionConfig.BARTENDING, 18L, "pour_cocktail");
                        }
                    } else {
                        int drinks = countPositiveDeltaByPrefix(check.bagBefore(), bagAfter, "BARTEND|")
                                + countMatchingStacksForProf(newDrops, ProfessionConfig.BARTENDING);
                        if (drinks > 0) {
                            long perItem = "kaleidoscope_tavern:pressing_tub".equals(check.blockId()) ? 4L : 14L;
                            ProfessionManager.grantXp(sp, ProfessionConfig.BARTENDING, perItem * Math.min(16, drinks), "brew_extract");
                        }
                    }
                }

                if (check.isAlchemistCauldron()) {
                    int arcaneItems = countPositiveDeltaByPrefix(check.bagBefore(), bagAfter, "ARCHMAGE|");
                    if (arcaneItems > 0) {
                        ProfessionManager.grantXp(sp, ProfessionConfig.ARCHMAGE, 14L * Math.min(16, arcaneItems), "alchemist_cauldron");
                    }
                }
            }
        }
    }

    private static void processPendingTapWatches(ServerTickEvent.Post event) {
        synchronized (PENDING_TAP_WATCHES) {
            if (PENDING_TAP_WATCHES.isEmpty()) {
                return;
            }
            Iterator<PendingBarrelTapWatch> it = PENDING_TAP_WATCHES.iterator();
            while (it.hasNext()) {
                PendingBarrelTapWatch watch = it.next();
                watch.ticksRemaining--;
                BlockEntity be = watch.level.getBlockEntity(watch.barrelPos);
                if (be == null) {
                    it.remove();
                    continue;
                }
                CompoundTag nbt = be.saveWithoutMetadata(watch.level.registryAccess());
                int curOut = countBarrelOutputFromNbt(nbt);
                if (curOut < watch.lastOutputCount) {
                    ServerPlayer sp = event.getServer().getPlayerList().getPlayer(watch.playerId);
                    if (sp != null) {
                        long xp = 10L + (long) Math.max(1, watch.brewLevel) * 5L;
                        ProfessionManager.grantXp(sp, ProfessionConfig.BARTENDING, xp, "barrel_tap");
                    }
                    it.remove();
                    continue;
                }
                if (watch.ticksRemaining <= 0) {
                    it.remove();
                }
            }
        }
    }

    private static void processOpenMenus(ServerTickEvent.Post event) {
        if (OPEN_MENUS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, TrackedMenuSession>> it = OPEN_MENUS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, TrackedMenuSession> entry = it.next();
            ServerPlayer sp = event.getServer().getPlayerList().getPlayer(entry.getKey());
            TrackedMenuSession sess = entry.getValue();
            if (sp == null || sp.containerMenu == null || sp.containerMenu.containerId != sess.containerId) {
                it.remove();
                continue;
            }
            if ("cooking".equals(sess.mode)) {
                settleMenuCookingDelta(sp, sess);
            } else if ("arcane_anvil".equals(sess.mode) && sp.containerMenu.slots.size() >= 2) {
                ItemStack cur0 = sp.containerMenu.slots.get(0).getItem();
                ItemStack cur1 = sp.containerMenu.slots.get(1).getItem();
                // 当两个输入槽同时消耗减少（例如卷轴+墨水升阶、装备+升级法球、法术书+槽位升级），且右槽材料不是退魔石时发放魔导师经验
                if (!sess.anvilIn0.isEmpty() && !sess.anvilIn1.isEmpty()
                        && cur0.getCount() < sess.anvilIn0.getCount()
                        && cur1.getCount() < sess.anvilIn1.getCount()
                        && !isShrivingStone(sess.anvilIn1)) {
                    ProfessionManager.grantXp(sp, ProfessionConfig.ARCHMAGE, 25L, "arcane_anvil_upgrade");
                }
                sess.anvilIn0 = cur0.copy();
                sess.anvilIn1 = cur1.copy();
            }
        }
    }

    private static boolean isShrivingStone(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && id.getPath().contains("shriving_stone");
    }

    private static Map<String, Integer> snapshotPlayerItems(ServerPlayer sp) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            accumulateForSnapshot(map, sp.getInventory().getItem(i));
        }
        accumulateForSnapshot(map, sp.containerMenu != null ? sp.containerMenu.getCarried() : ItemStack.EMPTY);
        return map;
    }

    private static void accumulateForSnapshot(Map<String, Integer> map, ItemStack stack) {
        if (stack == null || stack.isEmpty() || QualityEvaluator.isRejectedWaste(stack)) {
            return;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) {
            return;
        }
        String itemStr = id.toString();
        if ("kaleidoscope_tavern:vinegar".equals(itemStr) || "minecraft:glass_bottle".equals(itemStr)
                || "minecraft:bowl".equals(itemStr)) {
            return;
        }
        String prof = ProfessionManager.matchProfessionForOutput(stack);
        if (ProfessionConfig.COOKING.equals(prof) && isEligibleCookedDishForXp(stack)) {
            map.merge("COOK|" + itemStr, stack.getCount(), Integer::sum);
        } else if (ProfessionConfig.BARTENDING.equals(prof) && isEligibleBartendingOutputForXp(stack)) {
            map.merge("BARTEND|" + itemStr, stack.getCount(), Integer::sum);
        } else if (ProfessionConfig.ARCHMAGE.equals(prof)) {
            map.merge("ARCHMAGE|" + itemStr, stack.getCount(), Integer::sum);
        } else if (ProfessionConfig.FISHING.equals(prof)) {
            map.merge("FISH|" + itemStr, stack.getCount(), Integer::sum);
        }
    }

    private static boolean isEligibleCookedDishForXp(ItemStack stack) {
        if (stack == null || stack.isEmpty() || QualityEvaluator.isRejectedWaste(stack)) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && ProfessionOutputFilter.isEligibleCookedDishForXp(
                id.getNamespace(), id.getPath(), stack.has(DataComponents.FOOD));
    }

    private static boolean isEligibleBartendingOutputForXp(ItemStack stack) {
        if (stack == null || stack.isEmpty() || QualityEvaluator.isRejectedWaste(stack)) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && ProfessionOutputFilter.isEligibleBartendingOutputForXp(
                id.getNamespace(), id.getPath());
    }

    private static int countPositiveDeltaByPrefix(Map<String, Integer> before, Map<String, Integer> after, String prefix) {
        int sum = 0;
        for (Map.Entry<String, Integer> e : after.entrySet()) {
            if (e.getKey().startsWith(prefix)) {
                int diff = e.getValue() - before.getOrDefault(e.getKey(), 0);
                if (diff > 0) {
                    sum += diff;
                }
            }
        }
        return sum;
    }

    private static List<UUID> snapshotNearbyItemEntities(ServerLevel level, BlockPos pos) {
        List<ItemEntity> list = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3.0D));
        List<UUID> ids = new ArrayList<>(list.size());
        for (ItemEntity ie : list) {
            ids.add(ie.getUUID());
        }
        return ids;
    }

    private static List<ItemStack> collectNewNearbyDrops(ServerLevel level, BlockPos pos, List<UUID> beforeIds) {
        List<ItemEntity> list = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3.0D));
        List<ItemStack> out = new ArrayList<>();
        for (ItemEntity ie : list) {
            if (!beforeIds.contains(ie.getUUID()) && !ie.getItem().isEmpty()
                    && !QualityEvaluator.isRejectedWaste(ie.getItem())) {
                out.add(ie.getItem());
            }
        }
        return out;
    }

    private static int countMatchingStacksForProf(List<ItemStack> stacks, String wantProf) {
        int sum = 0;
        for (ItemStack s : stacks) {
            if (!wantProf.equals(ProfessionManager.matchProfessionForOutput(s))) {
                continue;
            }
            if (ProfessionConfig.COOKING.equals(wantProf) && !isEligibleCookedDishForXp(s)) {
                continue;
            }
            if (ProfessionConfig.BARTENDING.equals(wantProf) && !isEligibleBartendingOutputForXp(s)) {
                continue;
            }
            sum += s.getCount();
        }
        return sum;
    }

    private static boolean hasNewFishInWorldOrBag(ServerPlayer sp, ServerLevel level, BlockPos pos,
                                                  Map<String, Integer> bagBefore, List<UUID> dropsBefore) {
        Map<String, Integer> bagAfter = snapshotPlayerItems(sp);
        if (countPositiveDeltaByPrefix(bagBefore, bagAfter, "FISH|") > 0) {
            return true;
        }
        List<ItemStack> drops = collectNewNearbyDrops(level, pos, dropsBefore);
        return countMatchingStacksForProf(drops, ProfessionConfig.FISHING) > 0;
    }
}
