package li.cil.oc.core.impl.integration.util;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class DamageSourceWithRandomCause extends DamageSource {
    private final String name;
    private final int numCauses;

    public DamageSourceWithRandomCause(String name, int numCauses) {
        super(Holder.direct(new DamageType(name, DamageScaling.NEVER, 0.1f)));
        this.name = name;
        this.numCauses = numCauses;
    }

    @Override
    public boolean is(@NotNull TagKey<DamageType> tag) {
        if (tag == DamageTypeTags.BYPASSES_ARMOR ||
                tag == DamageTypeTags.BYPASSES_EFFECTS ||
                tag == DamageTypeTags.BYPASSES_RESISTANCE ||
                tag == DamageTypeTags.BYPASSES_ENCHANTMENTS) {
            return true;
        }
        return super.is(tag);
    }

    @Override
    public @NotNull Component getLocalizedDeathMessage(LivingEntity damagee) {
        LivingEntity damager = damagee.getKillCredit();
        String format = "death.attack." + name + "." + (damagee.level().random.nextInt(numCauses) + 1);
        String withCauseFormat = format + ".player";
        if (damager != null && net.minecraft.client.resources.language.I18n.exists(withCauseFormat)) {
            return Component.translatable(withCauseFormat, damagee.getDisplayName(), damager.getDisplayName());
        } else {
            return Component.translatable(format, damagee.getDisplayName());
        }
    }
}
