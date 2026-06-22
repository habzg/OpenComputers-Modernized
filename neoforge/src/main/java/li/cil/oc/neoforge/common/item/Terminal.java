package li.cil.oc.neoforge.common.item;

import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.client.renderer.gui.BufferRenderer;
import li.cil.oc.neoforge.common.component.TerminalServer;
import li.cil.oc.neoforge.common.item.traits.DelegateItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Terminal extends DelegateItem {
    @SuppressWarnings("unused")
    public Terminal(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    public boolean hasServer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        return cd != null && !cd.isEmpty() && cd.copyTag().contains(Settings.namespace + "server");
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (hasServer(stack)) {
            CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
            if (cd == null) return;
            String server = cd.copyTag().getString(Settings.namespace + "server");
            String shown = server.length() > 13 ? server.substring(0, 13) + "..." : server;
            tooltip.add(Component.literal("§8" + shown + "§7"));
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (stack.isEmpty()) return super.use(level, player, hand);
        if (player.isShiftKeyDown()) return super.use(level, player, hand);
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null || cd.isEmpty()) return super.use(level, player, hand);
        CompoundTag tag = cd.copyTag();
        String key = tag.getString(Settings.namespace + "key");
        String address = tag.getString(Settings.namespace + "server");
        if (key.isEmpty() || address.isEmpty()) {
            return super.use(level, player, hand);
        }
        if (level.isClientSide) {
            openTerminalScreen(player, stack, key, address);
        }
        player.swing(hand);
        return super.use(level, player, hand);
    }

    private static void openTerminalScreen(Player player, ItemStack stack, String key, String address) {
        var terminal = TerminalServer.TerminalServerCache.loaded.find(address);
        if (terminal == null) {
            player.sendSystemMessage(Component.translatable("gui.opencomputers.terminal.outofrange"));
            return;
        }
        var rack = terminal.rack;
        if (rack == null) {
            player.sendSystemMessage(Component.translatable("gui.opencomputers.terminal.outofrange"));
            return;
        }
        var range = terminal.range;
        var dx = player.getX() - rack.xPosition();
        var dy = player.getY() - rack.yPosition();
        var dz = player.getZ() - rack.zPosition();
        if (!player.isAlive() || dx * dx + dy * dy + dz * dz > range * range) {
            player.sendSystemMessage(Component.translatable("gui.opencomputers.terminal.outofrange"));
            return;
        }
        if (!terminal.sidedKeys().contains(key)) {
            player.sendSystemMessage(Component.translatable("gui.opencomputers.terminal.invalidkey"));
            return;
        }
        var initialBuffer = terminal.buffer();
        var initialNode = initialBuffer != null ? initialBuffer.node() : null;
        var bufferAddress = initialNode != null ? initialNode.address() : null;
        if (bufferAddress == null || bufferAddress.isEmpty()) {
            bufferAddress = terminal.address();
        }

        final String lockedBufferAddress = bufferAddress;
        final TerminalServer lockedTerminal = terminal;
        final ItemStack lockedStack = stack;
        final String lockedKey = key;

        var inner = new li.cil.oc.neoforge.client.gui.Screen(initialBuffer, true, () -> true, () -> {
            var cd = lockedStack.get(DataComponents.CUSTOM_DATA);
            var currentKey = (cd != null && !cd.isEmpty()) ? cd.copyTag().getString(Settings.namespace + "key") : "";
            if (!lockedKey.equals(currentKey)) {
                Minecraft.getInstance().setScreen(null);
                return false;
            }
            var r = lockedTerminal.rack;
            if (!player.isAlive()) {
                Minecraft.getInstance().setScreen(null);
                return false;
            }
            var dx2 = player.getX() - r.xPosition();
            var dy2 = player.getY() - r.yPosition();
            var dz2 = player.getZ() - r.zPosition();
            var range2 = lockedTerminal.range;
            if (dx2 * dx2 + dy2 * dy2 + dz2 * dz2 > range2 * range2) {
                Minecraft.getInstance().setScreen(null);
                return false;
            }
            return true;
        }) {
            @Override
            public li.cil.oc.api.internal.TextBuffer buffer() {
                var mc = Minecraft.getInstance();
                var lvl = mc.level;
                if (lvl != null && lockedBufferAddress != null && !lockedBufferAddress.isEmpty()) {
                    var found = li.cil.oc.core.impl.client.ClientComponentTracker.INSTANCE.get(lvl, lockedBufferAddress);
                    if (found instanceof TextBuffer tb) return tb;
                }
                return super.buffer();
            }
        };
        Minecraft.getInstance().setScreen(new net.minecraft.client.gui.screens.Screen(Component.literal("terminal")) {
            @Override
            public boolean isPauseScreen() {
                return false;
            }

            @Override
            protected void init() {
                super.init();
                BufferRenderer.init(Minecraft.getInstance().getTextureManager());
                inner.setGuiSize(this.width, this.height);
            }

            @Override
            public void render(net.minecraft.client.gui.@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float dt) {
                renderBackground(guiGraphics, mouseX, mouseY, dt);
                inner.render(guiGraphics, mouseX, mouseY, dt);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return inner.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public boolean mouseReleased(double mouseX, double mouseY, int button) {
                return inner.mouseReleased(mouseX, mouseY, button) || super.mouseReleased(mouseX, mouseY, button);
            }

            @Override
            public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
                return inner.mouseDragged(mouseX, mouseY, button, dragX, dragY) || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            }

            @Override
            public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
                return inner.mouseScrolled(mouseX, mouseY, scrollDeltaY) || super.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
            }

            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                return inner.handleKeyPress(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
            }

            @Override
            public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
                return inner.handleKeyRelease(keyCode, scanCode, modifiers) || super.keyReleased(keyCode, scanCode, modifiers);
            }

            @Override
            public boolean charTyped(char codePoint, int modifiers) {
                return inner.handleCharTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers);
            }
        });
    }
}
