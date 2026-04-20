package dev.simulated_team.simulated.content.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

public class AugerIndicatorParticle extends SimpleAnimatedParticle {
    protected float radius;
    protected float radius1;
    protected float radius2;
    protected float angleOffset;
    protected float speed;
    protected Direction direction;
    protected Vec3 origin;
    protected Vec3 offset;
    protected boolean isVisible;

    protected AugerIndicatorParticle(final ClientLevel level,
                                     final double x, final double y, final double z,
                                     final int color,
                                     final float radius1, final float radius2, final float angle,
                                     final float speed, final Direction direction, final int lifeSpan,
                                     final boolean isVisible, final SpriteSet sprite) {
        super(level, x, y, z, sprite, 0);
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        final Vec3i normal = direction.getNormal();
        this.origin = new Vec3(
                x - normal.getX() * 0.5,
                y - normal.getY() * 0.5,
                z - normal.getZ() * 0.5
        );
        this.quadSize *= 0.75F;
        this.lifetime = lifeSpan + this.random.nextInt(32);
        this.setFadeColor(color);
        this.setColor(Color.mixColors(color, 0xFFFFFF, .5f));
        this.setSpriteFromAge(sprite);
        this.radius1 = radius1;
        this.radius = radius1;
        this.radius2 = radius2;
        this.angleOffset = angle;
        this.speed = speed;
        this.direction = direction;
        this.isVisible = isVisible;
        this.offset = direction.getAxis().isHorizontal() ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        this.move(0, 0, 0);
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
    }

    @Override
    public void tick() {
        super.tick();
        this.radius += (this.radius2 - this.radius) * .1f;
    }

    @Override
    public void render(final VertexConsumer buffer, final Camera renderInfo, final float partialTicks) {
        if (!this.isVisible)
            return;
        super.render(buffer, renderInfo, partialTicks);
    }

    public void move(final double x, final double y, final double z) {
        final float time = AnimationTickHolder.getTicks(this.level);
        float angle = ((time * this.speed) % 360) - (this.speed / 2 * this.age * (((float) this.age) / this.lifetime));
        if (this.speed < 0 && this.direction.getAxis().isVertical())
            angle += 180;
        angle += this.angleOffset * 360f;
        final Vec3 position = VecHelper.rotate(this.offset.scale(this.radius), angle, this.direction.getAxis())
                .add(this.origin)
                .add(Vec3.atLowerCornerOf(this.direction.getNormal()).scale((double) this.age / this.lifetime));
        this.x = position.x;
        this.y = position.y;
        this.z = position.z;
    }

    public static class Factory implements ParticleProvider<AugerIndicatorParticleData> {
        private final SpriteSet spriteSet;

        public Factory(final SpriteSet animatedSprite) {
            this.spriteSet = animatedSprite;
        }

        public Particle createParticle(final AugerIndicatorParticleData data, final ClientLevel worldIn, final double x, final double y, final double z,
                                       final double xSpeed, final double ySpeed, final double zSpeed) {
            final Minecraft mc = Minecraft.getInstance();
            final LocalPlayer player = mc.player;
            final boolean visible = worldIn != mc.level || player != null && GogglesItem.isWearingGoggles(player);
            return new AugerIndicatorParticle(worldIn, x, y, z, data.color, data.radius1, data.radius2, data.angleOffset, data.speed,
                    data.direction, data.lifeSpan, visible, this.spriteSet);
        }
    }
}
