package dev.eriksonn.aeronautics.content.particle;

import com.simibubi.create.AllTags;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class PropellerAirParticle extends SimpleAnimatedParticle {

    public static final double frictionScale = 0.2;
    public static final int lifeTime = 20;
    private boolean stoppedByCollision;
    boolean isVirtual;

    protected PropellerAirParticle(final ClientLevel world, final double x, final double y, final double z, final double dx, final double dy,
                                   final double dz, final SpriteSet sprite, final boolean enableCollision, final boolean isVirtual) {
        super(world, x, y, z, sprite, world.random.nextFloat() * .5f);
        this.quadSize *= 0.75F;
        this.lifetime = lifeTime;
        this.bbWidth = this.bbHeight = 0.01f;
        this.hasPhysics = enableCollision;
        this.isVirtual=isVirtual;
        this.selectSprite(0);


        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        this.setPos(x+dx, y+dy, z+dz);
        this.setAlpha(.25f);
    }

    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    private void dissipate() {
        this.remove();
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.selectSprite((int) Mth.clamp((this.age / (float) this.lifetime) * 8, 0, 7));

            double friction = frictionScale * new Vec3(this.xd, this.yd, this.zd).length();
            friction = Math.min(friction, 0.5f);

            this.move(this.xd, this.yd, this.zd);
            this.xd *= 1.0 - friction;
            this.yd *= 1.0 - friction;
            this.zd *= 1.0 - friction;

        }

    }

    public void move(double pX, double pY, double pZ) {
        if (!this.stoppedByCollision) {
            double d0 = pX;
            final double d1 = pY;
            final double d2 = pZ;
            if(this.isVirtual && this.hasPhysics && !this.level.getBlockState(new BlockPos((int) (Math.floor(this.x + pX)), (int) (Math.floor(this.y + pY)), (int) (Math.floor(this.z + pZ)))).isAir())
            {
                this.stoppedByCollision = true;
            }
            if (this.hasPhysics && (pX != 0.0D || pY != 0.0D || pZ != 0.0D)) {
                if (!this.level.getBlockState(new BlockPos((int) (Math.floor(this.x + pX)), (int) (Math.floor(this.y + pY)), (int) (Math.floor(this.z + pZ)))).is(AllTags.AllBlockTags.FAN_TRANSPARENT.tag)) {
                    final Vec3 vec3 = Entity.collideBoundingBox(null, new Vec3(pX, pY, pZ), this.getBoundingBox(), this.level, List.of());
                    //Vec3 Vec3 = Entity.collideBoundingBox((Entity) null, new Vec3(pX, pY, pZ), this.getBoundingBox(), this.level, ISelectionContext.empty(), new ReuseableStream<>(Stream.empty()));
                    pX = vec3.x;
                    pY = vec3.y;
                    pZ = vec3.z;
                } else {
                    d0 = pX;
                }
            }

            if (pX != 0.0D || pY != 0.0D || pZ != 0.0D) {
                this.setBoundingBox(this.getBoundingBox().move(pX, pY, pZ));
                this.setLocationFromBoundingbox();
            }



            if (Math.abs(d1) >= (double) 1.0E-5F && Math.abs(pY) < (double) 1.0E-5F) {
                this.stoppedByCollision = true;
            }

            this.onGround = d1 != pY && d1 < 0.0D;
            if (d0 != pX) {
                this.xd = 0.0D;
            }

            if (d2 != pZ) {
                this.zd = 0.0D;
            }

        }
    }

    public int getLightColor(final float partialTick) {
        final BlockPos blockpos = new BlockPos((int) this.x, (int) this.y, (int) this.z);
        return this.level.isLoaded(blockpos) ? LevelRenderer.getLightColor(this.level, blockpos) : 0;
    }

    private void selectSprite(final int index) {
        this.setSprite(this.sprites.get(index, 8));
    }

    public static class Factory implements ParticleProvider<PropellerAirParticleData> {
        private final SpriteSet spriteSet;

        public Factory(final SpriteSet animatedSprite) {
            this.spriteSet = animatedSprite;
        }

        public Particle createParticle(final PropellerAirParticleData data, final ClientLevel worldIn, final double x, final double y, final double z,
                                       final double xSpeed, final double ySpeed, final double zSpeed) {
            return new PropellerAirParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet, data.enableCollision,data.isVirtual);
        }
    }
}
