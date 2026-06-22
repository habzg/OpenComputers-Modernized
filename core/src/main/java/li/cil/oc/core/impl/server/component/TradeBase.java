package li.cil.oc.core.impl.server.component;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.prefab.AbstractValue;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.util.InventoryUtils;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;

public abstract class TradeBase extends AbstractValue {
    public final TradeInfoBase info;

    public TradeBase(TradeInfoBase info) {
        this.info = info;
    }

    public double maxRange() {
        return Settings.get().tradingRange;
    }

    public boolean isInRange() {
        Merchant m = info.merchant.get();
        if (m instanceof Entity merchant && info.host != null) {
            return merchant.distanceToSqr(info.host.xPosition(), info.host.yPosition(), info.host.zPosition()) < maxRange() * maxRange();
        }
        return false;
    }

    protected abstract void deferredLoad(CompoundTag nbt, HolderLookup.Provider provider, TradeInfoBase target);

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        deferredLoad(nbt, provider, info);
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        info.save(nbt, provider);
    }

    @Callback(doc = "function():number -- Returns the merchant ID.")
    public Object[] getMerchantId(Context context, Arguments arguments) {
        return ResultWrapper.result((double) info.merchantID);
    }

    @Callback(doc = "function():table, table -- Returns the items the merchant wants.")
    public Object[] getInput(Context context, Arguments arguments) {
        MerchantOffer recipe = info.recipe();
        if (recipe != null) {
            ItemStack first = recipe.getBaseCostA().copy();
            ItemStack second = recipe.getCostB().isEmpty() ? null : recipe.getCostB().copy();
            return ResultWrapper.result(first, second);
        }
        return ResultWrapper.result(null, null);
    }

    @Callback(doc = "function():table -- Returns the item the merchant offers.")
    public Object[] getOutput(Context context, Arguments arguments) {
        MerchantOffer recipe = info.recipe();
        if (recipe != null) return ResultWrapper.result(recipe.getResult().copy());
        return ResultWrapper.result((Object) null);
    }

    @Callback(doc = "function():boolean -- Returns whether the merchant currently wants to trade this.")
    public Object[] isEnabled(Context context, Arguments arguments) {
        Merchant m = info.merchant.get();
        MerchantOffer recipe = info.recipe();
        if (m != null && recipe != null) return ResultWrapper.result(!recipe.isOutOfStock());
        return ResultWrapper.result(false);
    }

    @Callback(doc = "function():boolean, string -- Returns true when trade succeeds.")
    public Object[] trade(Context context, Arguments arguments) {
        Container inventory = info.inventory();
        if (inventory == null)
            return ResultWrapper.result(false, "trading requires an inventory upgrade to be installed");
        Merchant m = info.merchant.get();
        if (m == null)
            return ResultWrapper.result(false, "trade has become invalid");
        if (!(m instanceof Entity) || !((Entity) m).isAlive())
            return ResultWrapper.result(false, "trader died");
        if (!isInRange())
            return ResultWrapper.result(false, "out of range");
        MerchantOffer recipe = info.recipe();
        if (recipe == null)
            return ResultWrapper.result(false, "trade has become invalid");
        if (recipe.isOutOfStock())
            return ResultWrapper.result(false, "trade is disabled");
        if (!hasRoomForRecipe(inventory, recipe))
            return ResultWrapper.result(false, "not enough inventory space to trade");
        if (completeTrade(inventory, recipe, true) || completeTrade(inventory, recipe, false))
            return ResultWrapper.result(true);
        return ResultWrapper.result(false, "not enough items to trade");
    }

    private boolean hasRoomForRecipe(Container inventory, MerchantOffer recipe) {
        ItemStack remainder = recipe.getResult().copy();
        InventoryUtils.insertIntoInventory(remainder, inventory, null, remainder.getCount(), true);
        return remainder.getCount() == 0;
    }

    private boolean completeTrade(Container inventory, MerchantOffer recipe, boolean exact) {
        Merchant merchant = info.merchant.get();
        if (merchant == null) return false;
        ItemStack firstInput = recipe.getBaseCostA();
        ItemStack secondInput = recipe.getCostB().isEmpty() ? null : recipe.getCostB();

        ItemStack firstExtracted = InventoryUtils.extractFromInventory(firstInput, inventory, null, true, exact);
        if (firstExtracted.getCount() != 0) return false;
        if (secondInput != null) {
            ItemStack secondExtracted = InventoryUtils.extractFromInventory(secondInput, inventory, null, true, exact);
            if (secondExtracted.getCount() != 0) return false;
        }

        ItemStack outputStack = recipe.getResult().copy();
        InventoryUtils.extractFromInventory(firstInput, inventory, null, false, exact);
        if (secondInput != null)
            InventoryUtils.extractFromInventory(secondInput, inventory, null, false, exact);
        InventoryUtils.insertIntoInventory(outputStack, inventory, null, outputStack.getCount());
        merchant.notifyTrade(recipe);
        return true;
    }
}
