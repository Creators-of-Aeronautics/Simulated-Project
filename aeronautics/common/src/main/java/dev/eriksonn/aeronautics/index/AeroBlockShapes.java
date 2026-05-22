package dev.eriksonn.aeronautics.index;

import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.BiFunction;

import static net.minecraft.core.Direction.NORTH;

public class AeroBlockShapes {

	public static final VoxelShaper
			STEAM_VENT = shape(1, 0, 1, 15, 3, 15)
					.add(3, 2, 3, 13, 16, 13)
					.forAxis(),

			PROPELLER_BEARING = shape(0, 0, 0, 16, 16, 16)
					.erase(0, 0, 0, 5, 12, 5)
					.erase(11, 0, 0, 16, 12, 5)
					.erase(0, 0, 11, 5, 12, 16)
					.erase(11, 0, 11, 16, 12, 16)
					.forDirectional(),

			PROPELLER = shape(0, 0, 0, 16, 6, 16)
					.add(4, 5, 4, 12, 12, 12)
					.forDirectional(),

			SMART_PROPELLER = shape(0, 0, 0, 16, 4, 16)
					.add(2, 4, 2, 14, 10, 14)
					.forHorizontal(NORTH),

			SMART_PROPELLER_CEILING = shape(0, 16 - 4, 0, 16, 16, 16)
					.add(2, 16 - 10, 2, 14, 16 - 4, 14)
					.forHorizontal(NORTH);

	public static final VoxelShape
			HOT_AIR_BURNER = shape(0, 0, 0, 16, 8, 16)
					.add(3, 8, 3, 13, 16, 13)
					.build(),

			HOT_AIR_BURNER_PLAYER_COLLISION = shape(0, 0, 0, 16, 8, 16)
					.add(3, 8, 3, 13, 15.99, 13)
					.build(),

			HOT_AIR_BURNER_SMOKE_CLIP = shape(0, 0, 0, 16, 7, 16)
					.build(),

			MOUNTED_POTATO_CANNON = shape(0, 0, 0, 16, 8, 16)
					.add(1, 8, 1, 15, 12, 15)
					.add(4, 12, 4, 12, 28, 12)
					.erase(5, 18, 5, 11, 28, 11)
					.build(),

			MOUNTED_POTATO_CANNON_BLOCKED = shape(0, 0, 0, 16, 8, 16)
					.add(1, 8, 1, 15, 12, 15)
					.build();




	private static Builder shape(final VoxelShape shape) {
		return new Builder(shape);
	}

	private static Builder shape(final double x1, final double y1, final double z1, final double x2, final double y2, final double z2) {
		return shape(cuboid(x1, y1, z1, x2, y2, z2));
	}

	private static VoxelShape cuboid(final double x1, final double y1, final double z1, final double x2, final double y2, final double z2) {
		return Block.box(x1, y1, z1, x2, y2, z2);
	}

	public static class Builder {
		private VoxelShape shape;

		public Builder(final VoxelShape shape) {
			this.shape = shape;
		}

		public Builder add(final VoxelShape shape) {
			this.shape = Shapes.or(this.shape, shape);
			return this;
		}

		public Builder add(final double x1, final double y1, final double z1, final double x2, final double y2, final double z2) {
			return this.add(cuboid(x1, y1, z1, x2, y2, z2));
		}

		public Builder erase(final double x1, final double y1, final double z1, final double x2, final double y2, final double z2) {
			this.shape = Shapes.join(this.shape, cuboid(x1, y1, z1, x2, y2, z2), BooleanOp.ONLY_FIRST);
			return this;
		}

		public VoxelShape build() {
			return this.shape;
		}

		public VoxelShaper build(final BiFunction<VoxelShape, Direction, VoxelShaper> factory, final Direction direction) {
			return factory.apply(this.shape, direction);
		}

		public VoxelShaper build(final BiFunction<VoxelShape, Direction.Axis, VoxelShaper> factory, final Direction.Axis axis) {
			return factory.apply(this.shape, axis);
		}

		public VoxelShaper forAxis() {
			return this.build(VoxelShaper::forAxis, Direction.Axis.Y);
		}

		public VoxelShaper forHorizontalAxis() {
			return this.build(VoxelShaper::forHorizontalAxis, Direction.Axis.Z);
		}

		public VoxelShaper forHorizontal(final Direction direction) {
			return this.build(VoxelShaper::forHorizontal, direction);
		}

		public VoxelShaper forDirectional(final Direction direction) {
			return this.build(VoxelShaper::forDirectional, direction);
		}

		public VoxelShaper forDirectional() {
			return this.forDirectional(Direction.UP);
		}
	}
}
