package li.cil.oc.neoforge.server.component;

import java.util.Map;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.server.component.DebugCardBase;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

public class DebugCard extends DebugCardBase {
  public DebugCard(EnvironmentHost host) {
    super(host);
  }

  @Override
  protected boolean platformIsBlockBreakCanceled(@NotNull Level world, @NotNull net.minecraft.core.BlockPos pos, @NotNull BlockState state) {
    if (!(world instanceof net.minecraft.server.level.ServerLevel serverLevel)) return false;
    FakePlayer fakePlayer = FakePlayerFactory.get(serverLevel, OCSettings.get().fakePlayerProfile);
    fakePlayer.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    var event = new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(serverLevel, pos, state, fakePlayer);
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
    return event.isCanceled();
  }

  @Override
  protected boolean platformIsModLoaded(@NotNull String name) {
    return ModList.get().isLoaded(name);
  }

  @Override
  protected Object @NotNull [] platformRunCommand(@NotNull Context ignoredContext, @NotNull Arguments args) {
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
    var server = ServerLifecycleHooks.getCurrentServer();
    if (server == null) return ResultWrapper.result(0, null);
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

  @Override
  protected Object @NotNull [] platformGetPlayers(@NotNull Context ignoredContext) {
    var server = ServerLifecycleHooks.getCurrentServer();
    if (server == null) return ResultWrapper.result((Object) new String[0]);
    return ResultWrapper.result((Object) server.getPlayerList().getPlayers().stream()
      .map(ServerPlayer::getScoreboardName).toArray(String[]::new));
  }

  @Override
  protected boolean platformSendToClipboard(@NotNull String playerName, @NotNull String text) {
    var server = ServerLifecycleHooks.getCurrentServer();
    if (server == null) return false;
    ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
    if (player != null) {
      PacketSender.sendClipboard(player, text);
      return true;
    }
    return false;
  }

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

    @Override
    protected ServerPlayer getServerPlayer(@NotNull String name) {
      var server = ServerLifecycleHooks.getCurrentServer();
      if (server == null) return null;
      return server.getPlayerList().getPlayerByName(name);
    }
  }

  public class CommandSender extends FakePlayer implements DebugCardBase.CommandSenderBase {
    public String messages = null;

    public CommandSender() {
      super((net.minecraft.server.level.ServerLevel) host().level(), OCSettings.get().fakePlayerProfile);
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
