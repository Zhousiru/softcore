package moe.syrup.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import moe.syrup.Softcore;
import moe.syrup.ban.BanManager;
import moe.syrup.ban.BanState;
import moe.syrup.config.CoolDownRule;
import moe.syrup.data.PlayerData;
import moe.syrup.data.SoftcoreData;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

public class SoftcoreCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(Commands.literal("softcore")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("status")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(SoftcoreCommand::statusCommand)))
            .then(Commands.literal("unban")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(SoftcoreCommand::unbanCommand)))
            .then(Commands.literal("ban")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("time", StringArgumentType.string())
                        .executes(SoftcoreCommand::banCommand))))
            .then(Commands.literal("clear")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(SoftcoreCommand::clearCommand)))
            .then(Commands.literal("list")
                .executes(SoftcoreCommand::listCommand))
            .then(Commands.literal("reload")
                .executes(SoftcoreCommand::reloadCommand))
        );
    }

    private static int statusCommand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        PlayerData data = SoftcoreData.getInstance().getPlayerData(player.getUUID());
        BanState state = data.getBanState();

        ctx.getSource().sendSuccess(() -> Component.literal("=== " + player.getName().getString() + " ==="), false);
        ctx.getSource().sendSuccess(() -> Component.literal("死亡次数: " + data.getDeathHistory().size()), false);

        if (state != null && state.isBanned() && !state.isExpired()) {
            ctx.getSource().sendSuccess(() -> Component.literal("状态: 封禁中"), false);
            ctx.getSource().sendSuccess(() -> Component.literal("剩余时间: " + state.formatRemainingTime()), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.literal("状态: 正常"), false);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int unbanCommand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        if (!BanManager.isBanned(player)) {
            ctx.getSource().sendFailure(Component.literal("该玩家未被封禁"));
            return 0;
        }
        BanManager.unban(player);
        ctx.getSource().sendSuccess(() -> Component.literal("已解封 " + player.getName().getString()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int banCommand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String timeStr = StringArgumentType.getString(ctx, "time");

        try {
            Duration duration = CoolDownRule.parseTime(timeStr);
            BanManager.manualBan(player, duration);
            ctx.getSource().sendSuccess(() -> Component.literal("已封禁 " + player.getName().getString() + " " + timeStr), true);
            return Command.SINGLE_SUCCESS;
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.literal("无效的时间格式: " + timeStr));
            return 0;
        }
    }

    private static int clearCommand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        BanManager.clearDeathHistory(player.getUUID());
        ctx.getSource().sendSuccess(() -> Component.literal("已清除 " + player.getName().getString() + " 的死亡历史"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int listCommand(CommandContext<CommandSourceStack> ctx) {
        Map<UUID, PlayerData> players = SoftcoreData.getInstance().getAllPlayers();
        int bannedCount = 0;

        ctx.getSource().sendSuccess(() -> Component.literal("=== 封禁列表 ==="), false);

        for (Map.Entry<UUID, PlayerData> entry : players.entrySet()) {
            BanState state = entry.getValue().getBanState();
            if (state != null && state.isBanned() && !state.isExpired()) {
                bannedCount++;
                UUID uuid = entry.getKey();
                String remaining = state.formatRemainingTime();
                ServerPlayer onlinePlayer = SoftcoreData.getServer().getPlayerList().getPlayer(uuid);
                String name = onlinePlayer != null ? onlinePlayer.getName().getString() : uuid.toString();
                ctx.getSource().sendSuccess(() -> Component.literal("- " + name + ": " + remaining), false);
            }
        }

        if (bannedCount == 0) {
            ctx.getSource().sendSuccess(() -> Component.literal("无封禁玩家"), false);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int reloadCommand(CommandContext<CommandSourceStack> ctx) {
        Softcore.reloadConfig();
        ctx.getSource().sendSuccess(() -> Component.literal("配置已重载"), true);
        return Command.SINGLE_SUCCESS;
    }
}
