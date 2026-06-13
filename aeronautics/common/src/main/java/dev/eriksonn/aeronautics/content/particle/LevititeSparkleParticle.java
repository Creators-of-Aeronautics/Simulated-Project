package dev.eriksonn.aeronautics.content.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import org.jetbrains.annotations.Nullable;

public class LevititeSparkleParticle extends SimpleAnimatedParticle {
    protected LevititeSparkleParticle(final ClientLevel level,
                                      final double x, final double y, final double z,
                                      final double dx, final double dy, final double dz,
                                      final SpriteSet sprite, final int color) {
        super(level, x, y, z, sprite, level.random.nextFloat() * 0.5f);
        this.hasPhysics = false;
        this.lifetime = 16;
        this.quadSize *= 0.75f;
        this.selectSprite(level.random.nextInt(2));
        this.age++;
        this.setColor(color);
    }

    private void selectSprite(final int index) {
        this.setSprite(this.sprites.get(7 - index, 8));
    }

    @Override
    public void tick() {
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        final int previousIndex = ((this.age - 1) * 4) / (this.lifetime + 1);
        final int index = ((this.age) * 4) / (this.lifetime + 1);
        if (previousIndex != index) {
            this.selectSprite(index * 2 + this.level.random.nextInt(2));
        }

        this.move(this.xd, this.yd, this.zd);
    }

    public static class Factory implements ParticleProvider<LevititeSparkleParticleData> {
        private final SpriteSet spriteSet;

        public Factory(final SpriteSet animatedSprite) {
            this.spriteSet = animatedSprite;
        }

        @Override
        public @Nullable Particle createParticle(final LevititeSparkleParticleData levititeSparkleParticleData, final ClientLevel level,
                                                 final double x, final double y, final double z, final double dx, final double dy, final double dz) {
            return new LevititeSparkleParticle(
                    level,
                    x, y, z, dx, dy, dz,
                    this.spriteSet, levititeSparkleParticleData.color
            );
        }
    }
}
