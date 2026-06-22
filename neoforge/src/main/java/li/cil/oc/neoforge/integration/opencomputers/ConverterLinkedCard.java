package li.cil.oc.neoforge.integration.opencomputers;

import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.core.Constants;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

@SuppressWarnings("unused")
public final class ConverterLinkedCard implements Converter {
    private final ItemInfo linkedCard = li.cil.oc.api.Items.get(Constants.ItemName.LinkedCard);

    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (value instanceof ItemStack stack && linkedCard.equals(li.cil.oc.api.Items.get(stack))) {
            li.cil.oc.core.impl.server.component.LinkedCard card = new li.cil.oc.core.impl.server.component.LinkedCard();
            output.put("linkChannel", card.tunnel);
        }
    }
}
