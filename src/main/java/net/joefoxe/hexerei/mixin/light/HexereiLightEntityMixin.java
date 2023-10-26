package net.joefoxe.hexerei.mixin.light;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.joefoxe.hexerei.event.EventQueue;
import net.joefoxe.hexerei.event.FadeLightTimedEventHexerei;
import net.joefoxe.hexerei.light.DynamicLightUtil;
import net.joefoxe.hexerei.light.LambHexereiDynamicLight;
import net.joefoxe.hexerei.light.LightManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class HexereiLightEntityMixin implements LambHexereiDynamicLight {
    @Shadow
    public Level level;

    @Shadow
    public abstract double getX();

    @Shadow
    public abstract double getEyeY();

    @Shadow
    public abstract double getZ();

    @Shadow
    public abstract double getY();


    @Shadow
    public abstract EntityType<?> getType();

    @Shadow
    public abstract BlockPos blockPosition();

    @Shadow
    public abstract boolean isRemoved();

    @Shadow
    private ChunkPos chunkPosition;

    @Shadow
    public abstract Level getLevel();

    @Shadow
    public abstract BlockPos getOnPos();

    @Shadow
    public abstract double getZ(double pScale);

    @Shadow
    public abstract Vec3 position();

    @Shadow public abstract Component getName();

    @Unique
    protected int lambdynlights$luminance = 0;
    @Unique
    private int lambdynlights$lastLuminance = 0;
    @Unique
    private long lambdynlights$lastUpdate = 0;
    @Unique
    private double lambdynlights$prevX;
    @Unique
    private double lambdynlights$prevY;
    @Unique
    private double lambdynlights$prevZ;
    @Unique
    private LongOpenHashSet lambdynlights$trackedLitChunkPos = new LongOpenHashSet();

    @Inject(method = "tick", at = @At("HEAD"))
    public void onTick(CallbackInfo ci) {
        // We do not want to update the entity on the server.
        if (level.isClientSide && !LightManager.shouldUpdateDynamicLight()) {
            lambdynlights$luminance = 0;
        }
        if (this.level.isClientSide() && LightManager.shouldUpdateDynamicLight()) {
            if (this.isRemoved()) {
                this.setHexereiDynamicLightEnabled(false);
            } else {
                this.dynamicLightTickH();
                LightManager.updateLightTracking(this);
            }
        }
    }

    @Inject(method = "remove", at = @At("HEAD"))
    public void onRemove(CallbackInfo ci) {
        if (this.level.isClientSide()) {
            this.setHexereiDynamicLightEnabled(false);
        }
    }

    @Inject(method = "onClientRemoval", at = @At("HEAD"))
    public void removed(CallbackInfo ci) {
        if (this.level.isClientSide()) {
            this.setHexereiDynamicLightEnabled(false);
            if (lambdynlights$luminance > 0)
                EventQueue.getClientQueue().addEvent(new FadeLightTimedEventHexerei(this.getLevel(), this.position(), 8, lambdynlights$luminance));
        }
    }

    @Override
    public double getDynamicLightXH() {
        return this.getX();
    }

    @Override
    public double getDynamicLightYH() {
        return this.getEyeY();
    }

    @Override
    public double getDynamicLightZH() {
        return this.getZ();
    }

    @Override
    public Level getDynamicLightWorldH() {
        return this.level;
    }

    @Override
    public void resetDynamicLightH() {
        this.lambdynlights$lastLuminance = 0;
    }

    @Override
    public boolean shouldUpdateDynamicLightH() {
        return LightManager.shouldUpdateDynamicLight() && DynamicLightUtil.couldGiveLight((Entity) (Object) this);
    }

    @Override
    public void dynamicLightTickH() {
        lambdynlights$luminance = 0;
        int luminance = DynamicLightUtil.lightForEntity((Entity) (Object) this);
        if (luminance > this.lambdynlights$luminance)
            this.lambdynlights$luminance = luminance;
    }

    @Override
    public int getLuminanceH() {
        return this.lambdynlights$luminance;
    }

    @Override
    public boolean lambdynlights$updateDynamicLightH(LevelRenderer renderer) {
        if (!this.shouldUpdateDynamicLightH())
            return false;
        double deltaX = this.getX() - this.lambdynlights$prevX;
        double deltaY = this.getY() - this.lambdynlights$prevY;
        double deltaZ = this.getZ() - this.lambdynlights$prevZ;

        int luminance = this.getLuminanceH();

        if (Math.abs(deltaX) > 0.1D || Math.abs(deltaY) > 0.1D || Math.abs(deltaZ) > 0.1D || luminance != this.lambdynlights$lastLuminance) {
            this.lambdynlights$prevX = this.getX();
            this.lambdynlights$prevY = this.getY();
            this.lambdynlights$prevZ = this.getZ();
            this.lambdynlights$lastLuminance = luminance;

            var newPos = new LongOpenHashSet();

            if (luminance > 0) {
                var entityChunkPos = this.chunkPosition;
                var chunkPos = new BlockPos.MutableBlockPos(entityChunkPos.x, DynamicLightUtil.getSectionCoord(this.getEyeY()), entityChunkPos.z);

                LightManager.scheduleChunkRebuild(renderer, chunkPos);
                LightManager.updateTrackedChunks(chunkPos, this.lambdynlights$trackedLitChunkPos, newPos);

                var directionX = (this.blockPosition().getX() & 15) >= 8 ? Direction.EAST : Direction.WEST;
                // TODO: Make fastfloor
                var directionY = (Mth.floor(this.getEyeY()) & 15) >= 8 ? Direction.UP : Direction.DOWN;
                var directionZ = (this.blockPosition().getZ() & 15) >= 8 ? Direction.SOUTH : Direction.NORTH;

                for (int i = 0; i < 7; i++) {
                    if (i % 4 == 0) {
                        chunkPos.move(directionX); // X
                    } else if (i % 4 == 1) {
                        chunkPos.move(directionZ); // XZ
                    } else if (i % 4 == 2) {
                        chunkPos.move(directionX.getOpposite()); // Z
                    } else {
                        chunkPos.move(directionZ.getOpposite()); // origin
                        chunkPos.move(directionY); // Y
                    }
                    LightManager.scheduleChunkRebuild(renderer, chunkPos);
                    LightManager.updateTrackedChunks(chunkPos, this.lambdynlights$trackedLitChunkPos, newPos);
                }
            }
            // Schedules the rebuild of removed chunks.
            this.lambdynlights$scheduleTrackedChunksRebuildH(renderer);
            // Update tracked lit chunks.
            this.lambdynlights$trackedLitChunkPos = newPos;
            return true;
        }
        return false;
    }

    @Override
    public void lambdynlights$scheduleTrackedChunksRebuildH(LevelRenderer renderer) {
        if (Minecraft.getInstance().level == this.level)
            for (long pos : this.lambdynlights$trackedLitChunkPos) {
                LightManager.scheduleChunkRebuild(renderer, pos);
            }
    }
}
