package li.cil.oc.fabric.common.block;

import net.minecraft.world.level.block.state.BlockBehaviour;

public abstract class AbstractBlock extends li.cil.oc.core.impl.common.block.AbstractBlock {
    public AbstractBlock() {
        super();
    }

    public AbstractBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
}
