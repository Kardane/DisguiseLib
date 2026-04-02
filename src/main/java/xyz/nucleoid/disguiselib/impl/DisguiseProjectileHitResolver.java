package xyz.nucleoid.disguiselib.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.disguiselib.api.EntityDisguise;
import xyz.nucleoid.disguiselib.impl.mixin.accessor.EntityAccessor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public final class DisguiseProjectileHitResolver {
	private DisguiseProjectileHitResolver() {
	}

	@Nullable
	public static EntityHitResult resolve(Entity projectile, Vec3d start, Vec3d end, Box searchBox,
			Predicate<Entity> predicate, @Nullable EntityHitResult vanillaHit) {
		if (DisguiseTracker.getDisguisedEntityIds().isEmpty()) {
			return vanillaHit;
		}

		@Nullable
		CandidateHit<Entity> vanillaCandidate = vanillaHit == null ? null
				: new CandidateHit<>(vanillaHit.getEntity(), vanillaHit.getPos(),
						start.squaredDistanceTo(vanillaHit.getPos()));
		List<CandidateBounds<Entity>> candidateBounds = new ArrayList<>();
		Set<Entity> disguisedTargets = new HashSet<>();
		var world = ((EntityAccessor) projectile).getWorld();

		for (int entityId : DisguiseTracker.getDisguisedEntityIds()) {
			Entity entity = world.getEntityById(entityId);
			if (entity == null || entity == projectile || !predicate.test(entity)) {
				continue;
			}

			Box disguiseBounds = getDisguiseBounds(entity);
			if (disguiseBounds == null) {
				continue;
			}

			disguisedTargets.add(entity);
			if (disguiseBounds.intersects(searchBox)) {
				candidateBounds.add(new CandidateBounds<>(entity, disguiseBounds));
			}
		}

		CandidateHit<Entity> resolved = resolveHit(start, end, vanillaCandidate, disguisedTargets, candidateBounds);
		if (resolved == null) {
			return null;
		}

		if (vanillaHit != null && vanillaHit.getEntity() == resolved.target()
				&& vanillaHit.getPos().equals(resolved.hitPos())) {
			return vanillaHit;
		}

		return new EntityHitResult(resolved.target(), resolved.hitPos());
	}

	@Nullable
	static <T> CandidateHit<T> resolveHit(Vec3d start, Vec3d end, @Nullable CandidateHit<T> vanillaHit,
			Set<T> disguisedTargets, List<CandidateBounds<T>> disguisedCandidates) {
		CandidateHit<T> sanitizedVanillaHit = sanitizeVanillaHit(vanillaHit, disguisedTargets, disguisedCandidates);
		CandidateHit<T> closestDisguiseHit = findClosestDisguiseHit(start, end, disguisedCandidates);

		if (sanitizedVanillaHit == null) {
			return closestDisguiseHit;
		}

		if (closestDisguiseHit == null || closestDisguiseHit.squaredDistance() >= sanitizedVanillaHit.squaredDistance()) {
			return sanitizedVanillaHit;
		}

		return closestDisguiseHit;
	}

	@Nullable
	private static <T> CandidateHit<T> sanitizeVanillaHit(@Nullable CandidateHit<T> vanillaHit, Set<T> disguisedTargets,
			List<CandidateBounds<T>> disguisedCandidates) {
		if (vanillaHit == null || !disguisedTargets.contains(vanillaHit.target())) {
			return vanillaHit;
		}

		for (CandidateBounds<T> candidate : disguisedCandidates) {
			if (candidate.target().equals(vanillaHit.target()) && candidate.bounds().contains(vanillaHit.hitPos())) {
				return vanillaHit;
			}
		}

		return null;
	}

	@Nullable
	private static <T> CandidateHit<T> findClosestDisguiseHit(Vec3d start, Vec3d end,
			List<CandidateBounds<T>> disguisedCandidates) {
		CandidateHit<T> closestHit = null;

		for (CandidateBounds<T> candidate : disguisedCandidates) {
			Optional<Vec3d> hitPos = candidate.bounds().raycast(start, end);
			if (hitPos.isEmpty()) {
				continue;
			}

			CandidateHit<T> hit = new CandidateHit<>(candidate.target(), hitPos.get(),
					start.squaredDistanceTo(hitPos.get()));
			if (closestHit == null || hit.squaredDistance() < closestHit.squaredDistance()) {
				closestHit = hit;
			}
		}

		return closestHit;
	}

	@Nullable
	private static Box getDisguiseBounds(Entity entity) {
		Entity disguiseEntity = ((EntityDisguise) entity).getDisguiseEntity();
		if (disguiseEntity == null) {
			return null;
		}

		EntityDimensions dimensions = disguiseEntity.getDimensions(disguiseEntity.getPose());
		if (dimensions.width() <= 0.0F || dimensions.height() <= 0.0F) {
			return null;
		}

		Box bounds = dimensions.getBoxAt(entity.getPos());
		return bounds.isNaN() ? null : bounds;
	}

	record CandidateBounds<T>(T target, Box bounds) {
	}

	record CandidateHit<T>(T target, Vec3d hitPos, double squaredDistance) {
	}
}
