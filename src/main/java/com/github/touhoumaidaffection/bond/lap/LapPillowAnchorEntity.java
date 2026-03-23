package com.github.touhoumaidaffection.bond.lap;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.ModEntityTypes;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class LapPillowAnchorEntity extends Entity {
    public static final String ANCHOR_TAG = "touhou_maid_affection.lap_pillow_anchor";
    private static final int ORPHAN_GRACE_TICKS = 20;
    private static final double PLAYER_LYING_LIFT = 0.35D;
    private static final EntityDataAccessor<Integer> DATA_MODE = SynchedEntityData.defineId(LapPillowAnchorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_MAID_OFFSET_X = SynchedEntityData.defineId(LapPillowAnchorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_MAID_OFFSET_Y = SynchedEntityData.defineId(LapPillowAnchorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_MAID_OFFSET_Z = SynchedEntityData.defineId(LapPillowAnchorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PLAYER_OFFSET_X = SynchedEntityData.defineId(LapPillowAnchorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PLAYER_OFFSET_Y = SynchedEntityData.defineId(LapPillowAnchorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PLAYER_OFFSET_Z = SynchedEntityData.defineId(LapPillowAnchorEntity.class, EntityDataSerializers.FLOAT);

    private UUID ownerPlayerUuid;
    private UUID maidUuid;
    private int orphanTicks;
    private LapPillowPoseSnapshot poseSnapshot = LapPillowPoseSnapshot.maidSitPlayerLieDefault();

    public LapPillowAnchorEntity(EntityType<? extends LapPillowAnchorEntity> type, Level level) {
        super(type, level);
        configureAnchor();
    }

    public LapPillowAnchorEntity(Level level) {
        this(ModEntityTypes.LAP_PILLOW_ANCHOR.get(), level);
    }

    public LapPillowAnchorEntity(Level level, UUID ownerPlayerUuid, UUID maidUuid) {
        this(level);
        this.ownerPlayerUuid = ownerPlayerUuid;
        this.maidUuid = maidUuid;
    }

    public UUID getOwnerPlayerUuid() {
        return ownerPlayerUuid;
    }

    public UUID getMaidUuid() {
        return maidUuid;
    }

    public LapPillowPoseSnapshot getPoseSnapshot() {
        return poseSnapshot;
    }

    public void setPoseSnapshot(LapPillowPoseSnapshot poseSnapshot) {
        this.poseSnapshot = poseSnapshot == null ? LapPillowPoseSnapshot.maidSitPlayerLieDefault() : poseSnapshot.clamp();
        entityData.set(DATA_MODE, this.poseSnapshot.mode().ordinal());
        entityData.set(DATA_MAID_OFFSET_X, (float) this.poseSnapshot.maidOffsetX());
        entityData.set(DATA_MAID_OFFSET_Y, (float) this.poseSnapshot.maidOffsetY());
        entityData.set(DATA_MAID_OFFSET_Z, (float) this.poseSnapshot.maidOffsetZ());
        entityData.set(DATA_PLAYER_OFFSET_X, (float) this.poseSnapshot.playerOffsetX());
        entityData.set(DATA_PLAYER_OFFSET_Y, (float) this.poseSnapshot.playerOffsetY());
        entityData.set(DATA_PLAYER_OFFSET_Z, (float) this.poseSnapshot.playerOffsetZ());
    }

    public Vec3 resolveWorldOffset(double localX, double localY, double localZ) {
        float yawRad = getYRot() * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yawRad), 0.0D, Mth.cos(yawRad));
        Vec3 right = new Vec3(forward.z, 0.0D, -forward.x);
        return right.scale(localX).add(0.0D, localY, 0.0D).add(forward.scale(localZ));
    }

    public Vec3 getMaidWorldPosition() {
        return position().add(resolveWorldOffset(poseSnapshot.maidOffsetX(), poseSnapshot.maidOffsetY(), poseSnapshot.maidOffsetZ()));
    }

    public Vec3 getPlayerWorldPosition() {
        return position().add(resolveWorldOffset(poseSnapshot.playerOffsetX(), poseSnapshot.playerOffsetY(), poseSnapshot.playerOffsetZ()));
    }

    public net.minecraft.core.BlockPos getMaidFakeBedPos() {
        return net.minecraft.core.BlockPos.containing(getMaidWorldPosition());
    }

    public net.minecraft.core.BlockPos getPlayerFakeBedPos() {
        return net.minecraft.core.BlockPos.containing(getPlayerWorldPosition());
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        LapPillowPoseSnapshot defaults = LapPillowPoseSnapshot.maidSitPlayerLieDefault();
        builder.define(DATA_MODE, defaults.mode().ordinal());
        builder.define(DATA_MAID_OFFSET_X, (float) defaults.maidOffsetX());
        builder.define(DATA_MAID_OFFSET_Y, (float) defaults.maidOffsetY());
        builder.define(DATA_MAID_OFFSET_Z, (float) defaults.maidOffsetZ());
        builder.define(DATA_PLAYER_OFFSET_X, (float) defaults.playerOffsetX());
        builder.define(DATA_PLAYER_OFFSET_Y, (float) defaults.playerOffsetY());
        builder.define(DATA_PLAYER_OFFSET_Z, (float) defaults.playerOffsetZ());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerPlayerUuid = readUuid(tag, "OwnerPlayer");
        maidUuid = readUuid(tag, "Maid");
        orphanTicks = Math.max(0, tag.getInt("OrphanTicks"));
        poseSnapshot = new LapPillowPoseSnapshot(
                LapPillowMode.fromName(tag.getString("PoseMode")),
                tag.getDouble("MaidOffsetX"),
                tag.getDouble("MaidOffsetY"),
                tag.getDouble("MaidOffsetZ"),
                tag.getDouble("PlayerOffsetX"),
                tag.getDouble("PlayerOffsetY"),
                tag.getDouble("PlayerOffsetZ"),
                tag.getString("MaidAction"),
                tag.getString("PlayerAction")
        ).clamp();
        configureAnchor();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        writeUuid(tag, "OwnerPlayer", ownerPlayerUuid);
        writeUuid(tag, "Maid", maidUuid);
        tag.putInt("OrphanTicks", orphanTicks);
        tag.putString("PoseMode", poseSnapshot.mode().serializedName());
        tag.putDouble("MaidOffsetX", poseSnapshot.maidOffsetX());
        tag.putDouble("MaidOffsetY", poseSnapshot.maidOffsetY());
        tag.putDouble("MaidOffsetZ", poseSnapshot.maidOffsetZ());
        tag.putDouble("PlayerOffsetX", poseSnapshot.playerOffsetX());
        tag.putDouble("PlayerOffsetY", poseSnapshot.playerOffsetY());
        tag.putDouble("PlayerOffsetZ", poseSnapshot.playerOffsetZ());
        tag.putString("MaidAction", poseSnapshot.maidActionId());
        tag.putString("PlayerAction", poseSnapshot.playerActionId());
    }

    @Override
    public void tick() {
        super.tick();
        configureAnchor();
        if (level().isClientSide) {
            syncPoseFromEntityData();
        }
        setDeltaMovement(Vec3.ZERO);
        move(MoverType.SELF, Vec3.ZERO);
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ServerPlayer owner = ownerPlayerUuid == null ? null : serverLevel.getServer().getPlayerList().getPlayer(ownerPlayerUuid);
        Entity maidEntity = maidUuid == null ? null : serverLevel.getEntity(maidUuid);
        if (!(maidEntity instanceof EntityMaid maid) || !maid.isAlive()) {
            discardWithLog("maid_missing");
            return;
        }
        if (owner == null || !owner.isAlive()) {
            discardWithLog("owner_missing");
            return;
        }
        if (!LapPillowState.isActive(owner) || !getUUID().equals(LapPillowState.getAnchorUuid(owner))) {
            discardWithLog("state_desynced");
            return;
        }
        if (!maid.getUUID().equals(LapPillowState.getMaidUuid(owner))) {
            discardWithLog("maid_desynced");
            return;
        }

        if (poseSnapshot.playerLying() || (getFirstPassenger() == owner && owner.getVehicle() == this)) {
            orphanTicks = 0;
            return;
        }

        orphanTicks++;
        if (orphanTicks > ORPHAN_GRACE_TICKS) {
            discardWithLog("orphan_timeout");
        }
    }

    @Override
    public boolean canAddPassenger(Entity passenger) {
        return passenger != null
                && passenger.getUUID().equals(ownerPlayerUuid)
                && getPassengers().isEmpty();
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return false;
    }

    @Override
    public boolean shouldRiderSit() {
        return !poseSnapshot.playerLying();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        return new Vec3(0.0D, 0.0D, 0.0D);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        Vec3 riderPos = getPlayerWorldPosition();
        if (poseSnapshot.playerLying()) {
            riderPos = riderPos.add(0.0D, PLAYER_LYING_LIFT, 0.0D);
        }
        moveFunction.accept(passenger, riderPos.x, riderPos.y, riderPos.z);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return position().add(0.0D, 0.1D, 0.0D);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return false;
    }

    private void configureAnchor() {
        noPhysics = true;
        setNoGravity(true);
        setInvisible(true);
        setSilent(true);
        addTag(ANCHOR_TAG);
    }

    private void discardWithLog(String reason) {
        if (!isRemoved()) {
            TouhouMaidAffection.LOGGER.info(
                    "[LapPillow] Anchor discarded: anchor={} owner={} maid={} reason={}",
                    getUUID(),
                    ownerPlayerUuid,
                    maidUuid,
                    reason
            );
        }
        discard();
    }

    private void syncPoseFromEntityData() {
        int modeOrdinal = entityData.get(DATA_MODE);
        LapPillowMode[] values = LapPillowMode.values();
        LapPillowMode mode = modeOrdinal >= 0 && modeOrdinal < values.length
                ? values[modeOrdinal]
                : LapPillowMode.MAID_SIT_PLAYER_LIE;
        poseSnapshot = new LapPillowPoseSnapshot(
                mode,
                entityData.get(DATA_MAID_OFFSET_X),
                entityData.get(DATA_MAID_OFFSET_Y),
                entityData.get(DATA_MAID_OFFSET_Z),
                entityData.get(DATA_PLAYER_OFFSET_X),
                entityData.get(DATA_PLAYER_OFFSET_Y),
                entityData.get(DATA_PLAYER_OFFSET_Z),
                poseSnapshot.maidActionId(),
                poseSnapshot.playerActionId()
        ).clamp();
    }

    private static UUID readUuid(CompoundTag tag, String key) {
        if (!tag.contains(key)) {
            return null;
        }
        try {
            return tag.getUUID(key);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void writeUuid(CompoundTag tag, String key, UUID uuid) {
        if (uuid != null) {
            tag.putUUID(key, uuid);
        }
    }
}
