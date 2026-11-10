package li.cil.oc.fabric.server.component;

import java.util.Map;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.server.component.DebugCardBase;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.util.ResultWrapper;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class DebugCard extends DebugCardBase {
  @SuppressWarnings("unused")
  public DebugCard(EnvironmentHost host) {
    super(host);
  }

  @SuppressWarnings("unused")
  @Override
  protected boolean platformIsBlockBreakCanceled(@NotNull Level world, @NotNull net.minecraft.core.BlockPos pos, @NotNull BlockState state) {
    if (!(world instanceof ServerLevel serverLevel)) return false;
    FakePlayer fakePlayer = FakePlayer.get(serverLevel, OCSettings.get().fakePlayerProfile);
    fakePlayer.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    return !net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.invoker()
      .beforeBlockBreak(serverLevel, fakePlayer, pos, state, serverLevel.getBlockEntity(pos));
  }

  @SuppressWarnings("unused")
  @Override
  protected boolean platformIsModLoaded(@NotNull String name) {
    return FabricLoader.getInstance().isModLoaded(name);
  }

  @SuppressWarnings("unused")
  @Override
  protected Object @NotNull [] platformRunCommand(@NotNull Context context, @NotNull Arguments args) {
    CommandSender sender = new CommandSender();
    sender.prepare();
    int value = 0;
    String[] commands;
    if (args.isTable(0)) {
      @SuppressWarnings("rawtypes")
      Map table = args.checkTable(0);
      commands = new String[table.size()];
      int i = 0;
      for (Object cmd : table.values()) {
        commands[i++] = cmd.toString();
      }
    } else {
      commands = new String[]{args.checkString(0)};
    }
    var server = ((ServerLevel) sender.level()).getServer();
    for (String command : commands) {
      var dispatcher = server.getCommands().getDispatcher();
      var parse = dispatcher.parse(command, sender.createCommandSourceStack());
      try {
        value = dispatcher.execute(parse);
      } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
        value = 0;
      }
    }
    return ResultWrapper.result(value, sender.messages != null ? sender.messages : null);
  }

  @SuppressWarnings("unused")
  @Override
  protected Object @NotNull [] platformGetPlayers(@NotNull Context context) {
    if (!(host().level() instanceof ServerLevel serverLevel)) return ResultWrapper.result((Object) new String[0]);
    var server = serverLevel.getServer();
    return ResultWrapper.result((Object) server.getPlayerList().getPlayers().stream()
      .map(ServerPlayer::getScoreboardName).toArray(String[]::new));
  }

  @SuppressWarnings("unused")
  @Override
  protected boolean platformSendToClipboard(@NotNull String playerName, @NotNull String text) {
    if (!(host().level() instanceof ServerLevel serverLevel)) return false;
    var server = serverLevel.getServer();
    ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
    if (player != null) {
      PacketSender.sendClipboard(player, text);
      return true;
    }
    return false;
  }

  @SuppressWarnings("unused")
  @Override
  protected @NotNull PlayerValue createPlayerValue(@NotNull String name) {
    return new PlayerValue(name);
  }

  public class PlayerValue extends DebugCardBase.PlayerValue {
    @SuppressWarnings("unused")
    public PlayerValue() {
      super();
    }

    public PlayerValue(String name) {
      super(name);
    }

    @SuppressWarnings("unused")
    @Override
    protected ServerPlayer getServerPlayer(@NotNull String name) {
      if (!(host().level() instanceof ServerLevel serverLevel)) return null;
      var server = serverLevel.getServer();
      return server.getPlayerList().getPlayerByName(name);
    }
  }

  public class CommandSender extends FakePlayer implements DebugCardBase.CommandSenderBase {
    public String messages = null;

    public CommandSender() {
      super((ServerLevel) host().level(), OCSettings.get().fakePlayerProfile);
    }

    public void prepare() {
      var pos = BlockPosition.apply(host());
      setPos(pos.x(), pos.y(), pos.z());
      messages = null;
    }

    @Override
    public @NotNull String getScoreboardName() {
      return getGameProfile().getName();
    }

    @Override
    public @NotNull Level level() {
      return host().level();
    }

    @Override
    public void sendSystemMessage(net.minecraft.network.chat.@NotNull Component message) {
      if (messages == null) messages = message.getString();
      else messages += "\n" + message.getString();
    }
  }
}
