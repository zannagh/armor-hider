package de.zannagh.armorhider.configuration;

//? if >= 1.20.5 {
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? }

/**
 * Marker interface for configuration classes that should have their
 * ConfigurationItemBase fields automatically initialized when missing from JSON.
 * In < 1.20.5 the CustomPacketPayload does not yet exist, so the imports and extends are not needed.
 *
 * @since 0.5.0
 */
public interface ConfigurationSource<T>
    //? if >= 1.20.5
    extends CustomPacketPayload
{

    //? if >= 1.20.5
    StreamCodec<ByteBuf, T> getCodec();

    /**
     * Checks whether the configuration source has been modified from its
     * serialized state.
     *
     * @return true if the configuration has been changed since it was last
     *         serialized; false otherwise.
     */
    boolean hasChangedFromSerializedContent();


    /**
     * Marks the configuration source as having been modified from its serialized state.
     */
    void setHasChangedFromSerializedContent();

    /**
     * Config schema version. Absent (0) in configs from before versioning was introduced.
     * Incremented when the structure changes in a way that requires migration.
     */
    int getSchemaVersion();

    /**
     * Retrieves the current schema version used by the configuration system.
     * This version reflects the latest version of the configuration structure,
     * allowing compatibility and migration strategies when updates are introduced.
     *
     * @return the integer value representing the current schema version.
     */
    int getCurrentSchemaVersion();

    /**
     * Determines if a migration is necessary based on the schema version of the configuration.
     * Migration is required if the schema version associated with the configuration is
     * older than the current schema version defined in the system.
     *
     * @return true if the schema version of the configuration is outdated and needs migration;
     *         false otherwise.
     */
    default boolean shouldMigrate() {
        return getSchemaVersion() < getCurrentSchemaVersion();
    }

    T migrateFrom(T old);

    default T ensureSchemaFrom(T old) {
        if (!shouldMigrate()) {
            return old;
        }
        var migrated = migrateFrom(old);
        setHasChangedFromSerializedContent();
        return migrated;
    }
}
