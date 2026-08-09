package li.cil.oc.core.impl.server.component;

import java.lang.ref.WeakReference;
import java.util.UUID;
import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jetbrains.annotations.Nullable;

public abstract class TradeInfoBase {
    public EnvironmentHost host;
    public WeakReference<Merchant> merchant;
    public int recipeID;
    public int merchantID;

    public TradeInfoBase(EnvironmentHost host, Merchant merchant, int recipeID, int merchantID) {
        this.host = host;
        this.merchant = new WeakReference<>(merchant);
        this.recipeID = recipeID;
        this.merchantID = merchantID;
    }

    public @Nullable MerchantOffer recipe() {
        Merchant m = merchant.get();
        if (m != null) {
            try {
                return m.getOffers().get(recipeID);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public @Nullable Container inventory() {
        if (host instanceof li.cil.oc.api.internal.Agent) {
            return ((li.cil.oc.api.internal.Agent) host).mainInventory();
        }
        return null;
    }

    public void load(CompoundTag nbt, HolderLookup.Provider ignoredProvider) {
        boolean isEntity = nbt.getBoolean("hostIsEntity");
        host = isEntity ? loadHostEntity(nbt) : loadHostBlockEntity(nbt);
        UUID merchantUUID = new UUID(nbt.getLong("merchantUUIDMost"), nbt.getLong("merchantUUIDLeast"));
        Entity merchantEntity = loadEntity(nbt, merchantUUID);
        merchant = new WeakReference<>(merchantEntity instanceof Merchant ? (Merchant) merchantEntity : null);
        recipeID = nbt.getInt("recipeID");
        merchantID = nbt.contains("merchantID") ? nbt.getInt("merchantID") : -1;
    }

    public void save(CompoundTag nbt, HolderLookup.Provider ignoredProvider) {
        if (host instanceof Entity entity) {
            nbt.putBoolean("hostIsEntity", true);
            nbt.putInt("dimensionID", entity.level().dimension().location().hashCode());
            nbt.putLong("hostUUIDLeast", entity.getUUID().getLeastSignificantBits());
            nbt.putLong("hostUUIDMost", entity.getUUID().getMostSignificantBits());
        } else if (host instanceof net.minecraft.world.level.block.entity.BlockEntity te) {
            nbt.putBoolean("hostIsEntity", false);
            var level = te.getLevel();
            nbt.putInt("dimensionID", level != null ? level.dimension().location().hashCode() : 0);
            nbt.putInt("hostX", te.getBlockPos().getX());
            nbt.putInt("hostY", te.getBlockPos().getY());
            nbt.putInt("hostZ", te.getBlockPos().getZ());
        }
        Merchant m = merchant.get();
        if (m instanceof Entity entity) {
            nbt.putLong("merchantUUIDLeast", entity.getUUID().getLeastSignificantBits());
            nbt.putLong("merchantUUIDMost", entity.getUUID().getMostSignificantBits());
        }
        nbt.putInt("recipeID", recipeID);
        nbt.putInt("merchantID", merchantID);
    }

    protected abstract @Nullable Entity loadEntity(CompoundTag nbt, UUID uuid);

    protected abstract @Nullable EnvironmentHost loadHostEntity(CompoundTag nbt);

    protected abstract @Nullable EnvironmentHost loadHostBlockEntity(CompoundTag nbt);
}
