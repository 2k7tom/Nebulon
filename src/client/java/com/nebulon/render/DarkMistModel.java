package com.nebulon.render;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * Darkmode Mist through the Fabric model API: the baked models of white wool, white stained glass,
 * snow blocks, snow layers and ice are wrapped once. While {@link DarkmodeMist#active()} is false
 * they draw exactly as before; while it is true, wool and glass draw the gray model, and snow and
 * ice get the mod's dark textures. Chunk builders that use the Fabric renderer API (Fabric's Indigo and Sodium) call
 * {@link #emitQuads}, so this works with and without Sodium. The world itself is never changed.
 */
final class DarkMistModel extends WrapperBlockStateModel {
	private static final net.minecraft.resources.Identifier SNOW = net.minecraft.resources.Identifier.withDefaultNamespace("block/snow");
	private static final net.minecraft.resources.Identifier ICE = net.minecraft.resources.Identifier.withDefaultNamespace("block/ice");
	private static final net.minecraft.resources.Identifier CLAY = net.minecraft.resources.Identifier.withDefaultNamespace("block/clay");
	/** The mod's dark textures ("Midnight" snow, "Graphite" ice) in the block atlas. */
	private static final net.minecraft.resources.Identifier MIST_SNOW =
			net.minecraft.resources.Identifier.fromNamespaceAndPath(com.nebulon.Nebulon.MOD_ID, "block/mist_snow");
	private static final net.minecraft.resources.Identifier MIST_ICE =
			net.minecraft.resources.Identifier.fromNamespaceAndPath(com.nebulon.Nebulon.MOD_ID, "block/mist_ice");

	private final BlockState replacement;
	/** Texture swap for snow and ice: the vanilla texture and the mod's one (null for wool / glass). */
	private final net.minecraft.resources.@Nullable Identifier fromTexture;
	private final net.minecraft.resources.@Nullable Identifier toTexture;
	private @Nullable BlockStateModel replacementModel;

	private DarkMistModel(BlockStateModel wrapped, @Nullable BlockState replacement,
			net.minecraft.resources.@Nullable Identifier fromTexture, net.minecraft.resources.@Nullable Identifier toTexture) {
		super(wrapped);
		this.replacement = replacement;
		this.fromTexture = fromTexture;
		this.toTexture = toTexture;
	}

	static void register() {
		ModelLoadingPlugin.register(context -> context.modifyBlockModelAfterBake().register(ModelModifier.WRAP_PHASE, (model, ctx) -> {
			BlockState state = ctx.state();
			BlockState replacement = DarkmodeMist.swap(state);
			if (replacement != state) {
				return new DarkMistModel(model, replacement, null, null);
			}
			if (DarkmodeMist.isSnow(state)) {
				return new DarkMistModel(model, null, SNOW, MIST_SNOW);
			}
			if (DarkmodeMist.isClay(state)) {
				return new DarkMistModel(model, null, CLAY, MIST_SNOW);
			}
			return DarkmodeMist.isIce(state) ? new DarkMistModel(model, null, ICE, MIST_ICE) : model;
		}));
	}

	private static net.minecraft.client.renderer.texture.TextureAtlasSprite sprite(net.minecraft.resources.Identifier id) {
		return Minecraft.getInstance().getAtlasManager()
				.getAtlasOrThrow(net.minecraft.data.AtlasIds.BLOCKS).getSprite(id);
	}

	/** Moves a quad's texture coordinates from the vanilla sprite to the same place on the mod's sprite. */
	private static boolean retexture(net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView quad,
			net.minecraft.client.renderer.texture.TextureAtlasSprite from, net.minecraft.client.renderer.texture.TextureAtlasSprite to) {
		float fromWidth = from.getU1() - from.getU0();
		float fromHeight = from.getV1() - from.getV0();
		for (int i = 0; i < 4; i++) {
			float nu = (quad.u(i) - from.getU0()) / fromWidth;
			float nv = (quad.v(i) - from.getV0()) / fromHeight;
			quad.uv(i, to.getU0() + nu * (to.getU1() - to.getU0()), to.getV0() + nv * (to.getV1() - to.getV0()));
		}
		return true;
	}

	/** Gray model, looked up lazily because all models are baked before the first frame. */
	private BlockStateModel replacementModel() {
		BlockStateModel model = replacementModel;
		if (model == null) {
			model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(replacement);
			replacementModel = model;
		}
		return model;
	}

	@Override
	public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
			Predicate<@Nullable Direction> cullTest) {
		if (!DarkmodeMist.active()) {
			super.emitQuads(emitter, level, pos, state, random, cullTest);
		} else if (replacement != null && DarkmodeMist.isMistGlass(state)) {
			emitConnectedGlass(emitter, level, pos, random, cullTest);
		} else if (replacement != null) {
			replacementModel().emitQuads(emitter, level, pos, replacement, random, cullTest);
		} else if (fromTexture != null && toTexture != null) {
			var from = sprite(fromTexture);
			var to = sprite(toTexture);
			emitter.pushTransform(quad -> retexture(quad, from, to));
			try {
				super.emitQuads(emitter, level, pos, state, random, cullTest);
			} finally {
				emitter.popTransform();
			}
		}
	}

	/** Width of the glass frame in block units (one texture pixel). */
	private static final float FRAME = 1F / 16F;
	/** Texture areas (in pixels) that are plain: the glass pane and the frame. */
	private static final float[] PANE_UV = {6, 6, 10, 10};
	private static final float[] FRAME_UV = {6, 0, 10, 1};

	/**
	 * Connected glass: every face is drawn as plain gray glass, and the darker frame only along the
	 * edges where no other mist glass continues the surface, so walls of glass look like one pane
	 * instead of a grid. Drawn from the gray glass model's quads (same sprite, layer and lighting) in
	 * five passes: the pane, then the frame strips on top, bottom, left and right.
	 */
	private void emitConnectedGlass(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, RandomSource random,
			Predicate<@Nullable Direction> cullTest) {
		for (int pass = 0; pass < 5; pass++) {
			int p = pass;
			emitter.pushTransform(quad -> shapeGlassQuad(quad, p, level, pos));
			try {
				replacementModel().emitQuads(emitter, level, pos, replacement, random, cullTest);
			} finally {
				emitter.popTransform();
			}
		}
	}

	/**
	 * Reshapes one face quad of the gray glass model for a pass (0 pane, 1 top, 2 bottom, 3 left,
	 * 4 right frame strip, in the face's own orientation); false drops it.
	 */
	private static boolean shapeGlassQuad(net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView quad, int pass,
			BlockAndTintGetter level, BlockPos pos) {
		Direction face = quad.nominalFace();
		if (face == null) {
			return pass == 0;
		}
		float minU = Float.MAX_VALUE;
		float maxU = -Float.MAX_VALUE;
		float minV = Float.MAX_VALUE;
		float maxV = -Float.MAX_VALUE;
		for (int i = 0; i < 4; i++) {
			minU = Math.min(minU, quad.u(i));
			maxU = Math.max(maxU, quad.u(i));
			minV = Math.min(minV, quad.v(i));
			maxV = Math.max(maxV, quad.v(i));
		}
		// Which world direction each edge of the face points to: from a full square (vertices
		// 0 = left/top, 1 = left/bottom, 2 = right/bottom, 3 = right/top).
		quad.square(face, 0, 0, 1, 1, 0);
		Direction top = edge(quad, 0, 3);
		Direction bottom = edge(quad, 1, 2);
		Direction left = edge(quad, 0, 1);
		Direction right = edge(quad, 2, 3);
		boolean frameTop = !continues(level, pos, top);
		boolean frameBottom = !continues(level, pos, bottom);
		boolean frameLeft = !continues(level, pos, left);
		boolean frameRight = !continues(level, pos, right);
		float b = frameBottom ? FRAME : 0;
		float t = frameTop ? 1 - FRAME : 1;
		float[] rect;
		float[] uvArea = FRAME_UV;
		switch (pass) {
			case 0 -> {
				rect = new float[]{frameLeft ? FRAME : 0, b, frameRight ? 1 - FRAME : 1, t};
				uvArea = PANE_UV;
			}
			case 1 -> rect = frameTop ? new float[]{0, 1 - FRAME, 1, 1} : null;
			case 2 -> rect = frameBottom ? new float[]{0, 0, 1, FRAME} : null;
			case 3 -> rect = frameLeft ? new float[]{0, b, FRAME, t} : null;
			default -> rect = frameRight ? new float[]{1 - FRAME, b, 1, t} : null;
		}
		if (rect == null) {
			return false;
		}
		quad.square(face, rect[0], rect[1], rect[2], rect[3], 0);
		float u0 = minU + (maxU - minU) * uvArea[0] / 16F;
		float u1 = minU + (maxU - minU) * uvArea[2] / 16F;
		float v0 = minV + (maxV - minV) * uvArea[1] / 16F;
		float v1 = minV + (maxV - minV) * uvArea[3] / 16F;
		quad.uv(0, u0, v0);
		quad.uv(1, u0, v1);
		quad.uv(2, u1, v1);
		quad.uv(3, u1, v0);
		return true;
	}

	/** World direction of the edge through vertices a and b of a full-face square (from the face centre). */
	private static Direction edge(net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView quad, int a, int b) {
		float cx = 0;
		float cy = 0;
		float cz = 0;
		for (int i = 0; i < 4; i++) {
			cx += quad.x(i) / 4F;
			cy += quad.y(i) / 4F;
			cz += quad.z(i) / 4F;
		}
		float dx = (quad.x(a) + quad.x(b)) / 2F - cx;
		float dy = (quad.y(a) + quad.y(b)) / 2F - cy;
		float dz = (quad.z(a) + quad.z(b)) / 2F - cz;
		return Direction.getApproximateNearest(dx, dy, dz);
	}

	/** Whether the glass surface continues into the neighbour in that direction. */
	private static boolean continues(BlockAndTintGetter level, BlockPos pos, Direction direction) {
		return DarkmodeMist.isMistGlass(level.getBlockState(pos.relative(direction)));
	}

	@Override
	public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
		if (DarkmodeMist.active() && replacement != null) {
			replacementModel().collectParts(random, parts);
		} else {
			super.collectParts(random, parts);
		}
	}

	@Override
	public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
		// Geometry changes with the effect, so never let a renderer reuse a cached mesh while it is on.
		return DarkmodeMist.active() ? null : super.createGeometryKey(level, pos, state, random);
	}
}
