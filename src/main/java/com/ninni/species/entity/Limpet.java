package com.ninni.species.entity;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Dynamic;
import com.ninni.species.criterion.SpeciesCriterion;
import com.ninni.species.entity.ai.LimpetAi;
import com.ninni.species.entity.enums.LimpetType;
import com.ninni.species.entity.pose.SpeciesPose;
import com.ninni.species.registry.SpeciesSoundEvents;
import com.ninni.species.registry.SpeciesTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;

public class Limpet extends PathfinderMob {
    protected static final ImmutableList<SensorType<? extends Sensor<? super Limpet>>> SENSOR_TYPES = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.HURT_BY);
    protected static final ImmutableList<MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(MemoryModuleType.LOOK_TARGET, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryModuleType.WALK_TARGET, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleType.PATH, MemoryModuleType.IS_PANICKING, MemoryModuleType.AVOID_TARGET);
    private static final EntityDataAccessor<Integer> SCARED_TICKS = SynchedEntityData.defineId(Limpet.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(Limpet.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CRACKED_STAGE = SynchedEntityData.defineId(Limpet.class, EntityDataSerializers.INT);
    private static final UniformInt RETREAT_DURATION = TimeUtil.rangeOfSeconds(5, 20);
    private static final EntityDimensions SCARED_DIMENSIONS = EntityDimensions.scalable(0.75F, 0.75F);

    public Limpet(EntityType<? extends PathfinderMob> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    public float maxUpStep() {
        return 1.0F;
    }

    @Override
    protected Brain.Provider<Limpet> brainProvider() {
        return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    @Override
    public Brain<Limpet> getBrain() {
        return (Brain<Limpet>) super.getBrain();
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return LimpetAi.makeBrain(this.brainProvider().makeBrain(dynamic));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor serverLevelAccessor, DifficultyInstance difficultyInstance, MobSpawnType mobSpawnType, @Nullable SpawnGroupData spawnGroupData, @Nullable CompoundTag compoundTag) {

        int i = this.chooseLimpetType(serverLevelAccessor);
        if (spawnGroupData instanceof LimpetGroupData) i = ((LimpetGroupData)spawnGroupData).limpetType;
        else spawnGroupData = new LimpetGroupData(i);
        this.setLimpetType(i);

        return super.finalizeSpawn(serverLevelAccessor, difficultyInstance, mobSpawnType, spawnGroupData, compoundTag);
    }

    public int chooseLimpetType(LevelAccessor world) {
        int yLevel = this.blockPosition().getY();
        float random = this.random.nextFloat();
        boolean isMountain = world.getBiome(this.blockPosition()).is(Tags.Biomes.IS_MOUNTAIN);

        if (yLevel <= 16) {
            if (random <= 0.35) return LimpetType.DIAMOND.getId();
            else if (random <= 0.55 && random > 0.35) return LimpetType.LAPIS.getId();
            else return LimpetType.COAL.getId();
        }
        else if (yLevel <= 30) {
            if (!isMountain) {
                if (random <= 0.05) return LimpetType.EMERALD.getId();
                else if (random <= 0.15 && random > 0.05) return LimpetType.AMETHYST.getId();
                else if (random <= 0.35 && random > 0.15) return LimpetType.LAPIS.getId();
                else return LimpetType.COAL.getId();
            } else {
                if (random <= 0.10) return LimpetType.AMETHYST.getId();
                else if (random <= 0.20 && random > 0.10) return LimpetType.LAPIS.getId();
                else if (random <= 0.40 && random > 0.20) return LimpetType.EMERALD.getId();
                else return LimpetType.COAL.getId();
            }
        }
        else {
            if (!isMountain) {
                if (random <= 0.20) return LimpetType.LAPIS.getId();
                else return LimpetType.COAL.getId();
            } else {
                if (yLevel <= 64) {
                    if (random <= 0.20) return LimpetType.LAPIS.getId();
                    else if (random <= 0.50 && random > 0.20) return LimpetType.EMERALD.getId();
                    else return LimpetType.COAL.getId();
                } else {
                    if (random <= 0.20) return LimpetType.COAL.getId();
                    else return LimpetType.EMERALD.getId();
                }
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        this.level().getProfiler().push("limpetBrain");
        this.getBrain().tick((ServerLevel)this.level(), this);
        this.level().getProfiler().pop();
        this.level().getProfiler().push("limpetActivityUpdate");
        LimpetAi.updateActivity(this);
        this.level().getProfiler().pop();
        super.customServerAiStep();
    }

    public static AttributeSupplier.Builder createLimpetAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SCARED_TICKS, 0);
        this.entityData.define(TYPE, 0);
        this.entityData.define(CRACKED_STAGE, 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("ScaredTicks", this.getScaredTicks());
        nbt.putInt("LimpetType", this.getLimpetType().getId());
        nbt.putInt("CrackedStage", this.getCrackedStage());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setScaredTicks(nbt.getInt("ScaredTicks"));
        this.setLimpetType(nbt.getInt("LimpetType"));
        this.setCrackedStage(nbt.getInt("CrackedStage"));
    }

    public int getCrackedStage() {
        return this.entityData.get(CRACKED_STAGE);
    }
    public void setCrackedStage(int crackedStage) {
        this.entityData.set(CRACKED_STAGE, crackedStage);
    }
    public LimpetType getLimpetType() { return LimpetType.TYPES[this.entityData.get(TYPE)]; }
    public void setLimpetType(int id) { this.entityData.set(TYPE, id); }
    public int getScaredTicks() {
        return this.entityData.get(SCARED_TICKS);
    }
    public void setScaredTicks(int scaredTicks) {
        this.entityData.set(SCARED_TICKS, scaredTicks);
    }
    public boolean isScared() {
        return this.getScaredTicks() > 0;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.isScared()) this.setPose(SpeciesPose.SCARED.get());
        else this.setPose(Pose.STANDING);

        if (!this.level().isClientSide) {
            if (!this.getBrain().hasMemoryValue(MemoryModuleType.AVOID_TARGET)) {
                this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(4D), this::isValidEntity).forEach(player -> this.setScaredTicks(100));
            }
            if (this.isScared()) {
                int scaredTicks = this.getBrain().hasMemoryValue(MemoryModuleType.AVOID_TARGET) ? 0 : this.getScaredTicks() - 1;
                this.getNavigation().stop();
                this.setScaredTicks(scaredTicks);
            }
        }

    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return pose == SpeciesPose.SCARED.get() ? SCARED_DIMENSIONS.scale(this.getScale()) : super.getDimensions(pose);
    }
    @Override
    public boolean canBeCollidedWith() {
        return this.isScared();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand interactionHand) {
        LimpetType type = this.getLimpetType();
        ItemStack stack = player.getItemInHand(interactionHand);
        if (this.getCrackedStage() > 0 && type.getId() > 0 && stack.getItem() == type.getItem() && !this.getBrain().hasMemoryValue(MemoryModuleType.AVOID_TARGET)) {
            this.setCrackedStage(this.getCrackedStage() - 1);
            this.playSound(type.getPlacingSound(), 1, 1);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            this.setPersistenceRequired();
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, interactionHand);
    }

    @Override
    public boolean isPushable() {
        return !this.isScared();
    }

    public boolean isValidEntity(Player player) {
        Optional<ItemStack> stack = this.getStackInHand(player);
        return this.getLimpetType().getId() > 0
                && !player.isSpectator()
                && player.isAlive()
                && !player.getAbilities().instabuild
                && !player.isShiftKeyDown() || (this.getLimpetType().getId() > 0
                && stack.isPresent()
                && stack.get().getItem() instanceof PickaxeItem);
    }

    public boolean isValidEntityHoldingPickaxe(Player player) {
        return this.getLimpetType().getId() > 0 && this.getStackInHand(player).isPresent() && this.getStackInHand(player).get().getItem() instanceof PickaxeItem;
    }

    public Optional<ItemStack> getStackInHand(Player player) {
        return Arrays.stream(InteractionHand.values()).filter(hand -> player.getItemInHand(hand).getItem() instanceof PickaxeItem).map(player::getItemInHand).findFirst();
    }


    @Override
    public boolean hurt(DamageSource source, float amount) {
        LimpetType type = this.getLimpetType();
        if (source.getEntity() instanceof Player player
                && type.getId() > 0
                && !player.getMainHandItem().isEmpty()
                && player.getMainHandItem().getItem() instanceof PickaxeItem pickaxe
                && pickaxe.getTier().getLevel() >= type.getPickaxeLevel()
                && !player.getCooldowns().isOnCooldown(pickaxe)) {

            if (type.getId() > 1) spawnBreakingParticles();

            ItemStack stack = player.getMainHandItem();
            if (this.getCrackedStage() < 3) {
                this.getBrain().setMemoryWithExpiry(MemoryModuleType.AVOID_TARGET, player, RETREAT_DURATION.sample(this.level().random));
                this.setCrackedStage(this.getCrackedStage() + 1);
                this.playSound(type.getAdditionalBreakSound(), 1, (float) this.getCrackedStage() * 0.3f + 0.5f);
                this.playSound(SpeciesSoundEvents.LIMPET_BREAK.get(), 0.6f, this.getCrackedStage() + 1);
                this.setScaredTicks(0);
                for (ItemStack itemStack : player.getInventory().items) {
                    if (itemStack.getItem() instanceof PickaxeItem) {
                        player.getCooldowns().addCooldown(itemStack.getItem(), player.getAbilities().instabuild ? 0 : 80);
                    }
                }
                return false;
            } else {
                int count = (int) ((type.getMaxCount()/2 + random.nextInt(type.getMaxCount()/2)) * ( 1 + (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, stack) * 0.15f)));
                if (type.getId() > 1) {
                    for (int i = 0; i < count; i++) {
                        this.spawnAtLocation(type.getItem(), 1);
                    }
                }

                this.playSound(type.getAdditionalBreakSound(), 1, (float) this.getCrackedStage() * 0.3f + 1f);
                this.playSound(SpeciesSoundEvents.LIMPET_BREAK.get(), 0.6f, this.getCrackedStage() + 1.5f);
                this.setCrackedStage(0);
                if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, stack) != 0) {
                    if (type.getId() > 1) this.setLimpetType(1);
                    else this.setLimpetType(0);
                    if (player instanceof ServerPlayer serverPlayer) SpeciesCriterion.SILK_TOUCH_BREAK_LIMPET.trigger(serverPlayer);
                    return false;
                } else {
                    this.setLimpetType(0);
                    this.setScaredTicks(0);
                }
            }

        } else if (source.getEntity() instanceof LivingEntity && amount < 12 && !this.level().isClientSide && type.getId() > 0) {
            this.playSound(SpeciesSoundEvents.LIMPET_DEFLECT.get(), 1, 1);
            if (!this.getBrain().hasMemoryValue(MemoryModuleType.AVOID_TARGET)) this.setScaredTicks(300);
            return false;
        }
        return super.hurt(source, amount);
    }

    private void spawnBreakingParticles() {
        for (int i = 0; i < 40; ++i) {
            this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, this.getLimpetType().getItem().getDefaultInstance()),
                    this.getX(), this.getY() + this.getBbHeight(), this.getZ(),
                    ((double)this.random.nextFloat() - 0.5) * 0.5, ((double)this.random.nextFloat() - 0.5) * 0.8, ((double)this.random.nextFloat() - 0.5) * 0.5);
        }
    }

    @Override
    public void travel(Vec3 vec3) {
        if (!this.getBrain().hasMemoryValue(MemoryModuleType.AVOID_TARGET) && this.isScared()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0, 1, 0));
            vec3 = vec3.multiply(0, 1, 0);
        }
        super.travel(vec3);
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SpeciesSoundEvents.LIMPET_IDLE.get();
    }
    @Override
    protected SoundEvent getDeathSound() {
        return SpeciesSoundEvents.LIMPET_DEATH.get();
    }
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SpeciesSoundEvents.LIMPET_HURT.get();
    }
    @Override
    protected void playStepSound(BlockPos blockPos, BlockState blockState) {
        this.playSound(SpeciesSoundEvents.LIMPET_STEP.get(), 0.15F, 1.0F);
    }

    public float getWalkTargetValue(BlockPos p_33013_, LevelReader p_33014_) {
        return -p_33014_.getPathfindingCostFromLightLevels(p_33013_);
    }

    @SuppressWarnings("unused")
    public static boolean canSpawn(EntityType<? extends PathfinderMob> entityType, ServerLevelAccessor levelAccessor, MobSpawnType spawnType, BlockPos blockPos, RandomSource randomSource) {
        return levelAccessor.getBrightness(LightLayer.BLOCK, blockPos) == 0 && levelAccessor.getBrightness(LightLayer.SKY, blockPos) == 0 && levelAccessor.getBlockState(blockPos.below()).is(SpeciesTags.LIMPET_SPAWNABLE_ON) && levelAccessor.getBlockState(blockPos.below()).isValidSpawn(levelAccessor, blockPos, entityType);
    }

    public static class LimpetGroupData implements SpawnGroupData {
        public final int limpetType;

        public LimpetGroupData(int i) {
            this.limpetType = i;
        }
    }
}
