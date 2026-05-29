package xyz.nucleoid.disguiselib.api;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;

/**
 * Util for disguising entities.
 */
public interface EntityDisguise {

	/**
	 * Tells you the disguised status.
	 *
	 * @return true if entity is disguised, otherwise false.
	 */
	boolean isDisguised();

	/**
	 * Sets entity's disguise from {@link EntityType}
	 *
	 * @param entityType the type to disguise this entity into
	 */
	void disguiseAs(EntityType<?> entityType);

	/**
	 * Sets entity's disguise from {@link EntityType}
	 *
	 * @param entity the entity to disguise into
	 */
	void disguiseAs(Entity entity);

	/**
	 * Clears the disguise - sets the disguiseType back to original.
	 */
	void removeDisguise();

	/**
	 * Gets the disguise entity type
	 *
	 * @return disguise entity type or real type if there's no disguise
	 */
	EntityType<?> getDisguiseType();

	/**
	 * Gets the disguise entity.
	 *
	 * @return disguise entity or null if there's no disguise
	 */
	Entity getDisguiseEntity();

	/**
	 * Sets the scale attribute on the disguise entity only.
	 *
	 * @param scale the disguise entity scale
	 * @return true if the disguise entity supports scale and the value was applied
	 */
	boolean setDisguiseScale(double scale);

	/**
	 * Gets the scale attribute from the disguise entity.
	 *
	 * @return the disguise entity scale, or {@link Double#NaN} if unavailable
	 */
	double getDisguiseScale();

	/**
	 * Whether this entity can bypass the
	 * "disguises" and see entities normally
	 * Intended more for admins (to not get trolled themselves).
	 *
	 * @return if entity can be "fooled" by disguise
	 */
	boolean hasTrueSight();

	/**
	 * Toggles true sight - whether entity
	 * can see disguises or not.
	 * Intended more for admins (to not get trolled themselves).
	 *
	 * @param trueSight if entity should not see disguises
	 */
	void setTrueSight(boolean trueSight);
}
