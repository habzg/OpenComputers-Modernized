package li.cil.oc.core.impl.common.block;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class FakeEndstone extends SimpleBlock {
    public FakeEndstone() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3f, 15f).sound(SoundType.STONE));
    }
}
