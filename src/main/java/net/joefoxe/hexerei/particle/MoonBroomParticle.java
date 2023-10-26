package net.joefoxe.hexerei.particle;

import net.joefoxe.hexerei.client.renderer.entity.custom.BroomEntity;
import net.joefoxe.hexerei.item.custom.BroomBrushItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class MoonBroomParticle extends TextureSheetParticle {
    protected float scale;
    protected float rotationDir;
    protected float fallingSpeed;

    public MoonBroomParticle(ClientLevel world, double x, double y, double z, double motionX, double motionY, double motionZ) {
        super(world, x, y, z);
        this.xd = motionX;
        this.yd = motionY;
        this.zd = motionZ;
        this.roll = new Random().nextFloat() * (float)Math.PI;
        this.oRoll = this.roll;
        this.rotationDir = new Random().nextFloat() - 0.5f;
        this.fallingSpeed = new Random().nextFloat();

        setScale(0.2F);
    }

    public void setScale(float scale) {
        this.scale = scale;
        this.setSize(scale * 0.5f, scale * 0.5f);
    }

    @Override
    public void tick() {

        this.oRoll = this.roll;
        if(Math.abs(this.yd) > 0 && this.y != this.yo)
            this.roll += 0.3f * rotationDir;
        this.yd -= 0.005f * fallingSpeed;

        super.tick();
    }

    @Override
    public int getLightColor(float pPartialTick) {
        float time = level.getTimeOfDay(0);
        if(time > 0.25f && time < 0.75f && level.getMoonPhase() == 0 && !level.dimensionType().hasFixedTime())
            return LightTexture.FULL_BRIGHT;
        return super.getLightColor(pPartialTick);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Factory(SpriteSet sprite) {
            this.spriteSet = sprite;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            Random rand = new Random();
            float colorOffset = (rand.nextFloat() * 0.4f);
            MoonBroomParticle broomParticle = new MoonBroomParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed);
            broomParticle.pickSprite(this.spriteSet);
            broomParticle.setColor(0.6f + colorOffset, 0.6f + colorOffset, 0.6f + colorOffset);
            if (this.spriteSet.get(0, 1).atlasLocation().getPath().matches("particle/moon_brush_2") ||
                this.spriteSet.get(0, 1).atlasLocation().getPath().matches("particle/moon_brush_3") ||
                this.spriteSet.get(0, 1).atlasLocation().getPath().matches("particle/moon_brush_4")) {
                broomParticle.lifetime += broomParticle.lifetime * 3 + 30;
            }

            return broomParticle;
        }
    }
}
