package com.nebulon.slayer;

import com.nebulon.client.NebulonClient;
import com.nebulon.config.InfernoDemonlordSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "Inferno Demonlord" (Combat), Hypixel SkyBlock's Blaze slayer boss. Everything is read from the
 * name tags (armor stands) Hypixel puts over the boss:
 * <ul>
 *     <li>Hide Fire Particles and Fireballs: while an Inferno Demonlord name tag is near, flame / lava
 *     particles are not created and fireballs are not drawn.</li>
 *     <li>Attunement Highlight: a name tag with ASHEN / SPIRIT / AURIC / CRYSTAL (the hit attunement of
 *     the boss or one of its demons) makes the mob under it glow in that attunement's colour.</li>
 *     <li>Fire Pillar Alert: a fire pillar's name tag ("7s 8 hits") shows a title once per pillar.</li>
 * </ul>
 */
public final class InfernoDemonlord {
	private static final Pattern ATTUNEMENT = Pattern.compile("\\b(ASHEN|SPIRIT|AURIC|CRYSTAL)\\b");
	/** Fire pillar name tag, e.g. "7s 8 hits". */
	private static final Pattern PILLAR = Pattern.compile("\\b(\\d+)s\\s+(\\d+)\\s+hits?\\b");
	private static final Set<ParticleType<?>> FIRE_PARTICLES = Set.of(
			ParticleTypes.FLAME, ParticleTypes.SMALL_FLAME, ParticleTypes.SOUL_FIRE_FLAME, ParticleTypes.LAVA,
			ParticleTypes.DRIPPING_LAVA, ParticleTypes.FALLING_LAVA, ParticleTypes.LANDING_LAVA);
	private static final Set<ParticleType<?>> POTION_PARTICLES = Set.of(
			ParticleTypes.ENTITY_EFFECT, ParticleTypes.EFFECT, ParticleTypes.INSTANT_EFFECT, ParticleTypes.WITCH);
	private static final double RANGE = 40.0;
	/** The boss counts as spawned this long after its name tag was last seen. */
	private static final long BOSS_MEMORY_MS = 3000L;

	private static long bossSeenAt;
	private static Map<Integer, Integer> attunements = Map.of();
	private static final Set<Integer> alertedPillars = new HashSet<>();
	private static int alertCount;

	private InfernoDemonlord() {
	}

	private static InfernoDemonlordSettings settings() {
		return NebulonClient.get().configManager().get().infernoDemonlord;
	}

	private static boolean enabled() {
		NebulonClient client = NebulonClient.get();
		return client != null && settings().enabled;
	}

	public static int colorOf(String attunement) {
		return switch (attunement) {
			case "ASHEN" -> 0x555555;
			case "SPIRIT" -> 0xFFFFFF;
			case "AURIC" -> 0xFFFF55;
			default -> 0x55FFFF; // CRYSTAL
		};
	}

	/** Client tick: reads the boss, attunement and fire pillar name tags around the player. */
	public static void tick(Minecraft mc) {
		if (!enabled() || mc.level == null || mc.player == null) {
			attunements = Map.of();
			alertedPillars.clear();
			return;
		}
		InfernoDemonlordSettings s = settings();
		Map<Integer, Integer> found = new HashMap<>();
		Set<Integer> pillarsNow = new HashSet<>();
		long now = Util.getMillis();
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (!(entity instanceof ArmorStand stand) || !stand.hasCustomName() || stand.distanceTo(mc.player) > RANGE) {
				continue;
			}
			String name = ChatFormatting.stripFormatting(stand.getCustomName().getString());
			if (name == null) {
				continue;
			}
			if (name.contains("Inferno Demonlord")) {
				bossSeenAt = now;
			}
			Matcher attunement = ATTUNEMENT.matcher(name);
			if (s.attunementHighlight && attunement.find()) {
				LivingEntity mob = mobUnder(mc, stand);
				if (mob != null) {
					found.put(mob.getId(), colorOf(attunement.group(1)));
				}
			}
			Matcher pillar = PILLAR.matcher(name);
			if (pillar.find()) {
				pillarsNow.add(stand.getId());
				if (s.firePillarAlert && alertedPillars.add(stand.getId())) {
					alert(mc, pillar.group(1), pillar.group(2));
				}
			}
		}
		attunements = found;
		alertedPillars.retainAll(pillarsNow);
	}

	/** The boss or demon a name tag belongs to: the nearest mob just below it. */
	private static LivingEntity mobUnder(Minecraft mc, ArmorStand stand) {
		LivingEntity best = null;
		double bestDistance = Double.MAX_VALUE;
		List<Entity> near = mc.level.getEntities(stand, stand.getBoundingBox().inflate(1.5, 4.0, 1.5));
		for (Entity entity : near) {
			// Only the boss (blaze) and its demons (Quazii: wither skeleton, Typhoeus: zombified piglin);
			// Hypixel also has guardians and other helper mobs there, often invisible.
			if (!(entity instanceof LivingEntity living) || entity.isInvisible() || !isBossOrDemon(entity)) {
				continue;
			}
			double dy = stand.getY() - entity.getY();
			if (dy < -0.5 || dy > 4.0) {
				continue;
			}
			double distance = entity.distanceToSqr(stand);
			if (distance < bestDistance) {
				bestDistance = distance;
				best = living;
			}
		}
		return best;
	}

	private static boolean isBossOrDemon(Entity entity) {
		return entity instanceof net.minecraft.world.entity.monster.Blaze
				|| entity instanceof net.minecraft.world.entity.monster.skeleton.WitherSkeleton
				|| entity instanceof net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
	}

	private static void alert(Minecraft mc, String seconds, String hits) {
		alertCount++;
		var hud = mc.gui.hud;
		hud.setTimes(2, 30, 8);
		hud.setSubtitle(Component.literal(seconds + "s - " + hits + " hits").withStyle(ChatFormatting.GOLD));
		hud.setTitle(Component.literal("FIRE PILLAR!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
	}

	// ---- Hooks ----------------------------------------------------------------------------------------

	/** Outline colour for a boss / demon with a known attunement, or 0. */
	public static int outlineColor(Entity entity) {
		Integer rgb = attunements.get(entity.getId());
		return rgb == null ? 0 : 0xFF000000 | rgb;
	}

	private static boolean bossUp() {
		return enabled() && Util.getMillis() - bossSeenAt < BOSS_MEMORY_MS;
	}

	private static boolean hideFireNow() {
		return bossUp() && settings().hideFire;
	}

	public static boolean hidesParticle(ParticleType<?> type) {
		if (FIRE_PARTICLES.contains(type)) {
			return hideFireNow();
		}
		return POTION_PARTICLES.contains(type) && bossUp() && settings().hidePotionParticles;
	}

	public static boolean hidesEntity(Entity entity) {
		return entity instanceof Fireball && hideFireNow();
	}

	// ---- Test hooks -----------------------------------------------------------------------------------

	public static int alertCountForTest() {
		return alertCount;
	}

	public static boolean bossActiveForTest() {
		return hideFireNow();
	}

	public static String attunementOfForTest(int entityId) {
		Integer rgb = attunements.get(entityId);
		return rgb == null ? "none" : String.format(Locale.ROOT, "%06X", rgb);
	}
}
