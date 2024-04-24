package at.petrak.hexcasting.paucal.common.command;

import at.petrak.hexcasting.paucal.common.ContributorsManifest;
import at.petrak.hexcasting.paucal.common.msg.MsgReloadContributorsS2C;
import at.petrak.hexcasting.paucal.xplat.IXplatAbstractions;
import at.petrak.hexcasting.paucal.PaucalConfig;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class CommandReloadContributors {
    public static void add(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(Commands.literal("reload").requires(css -> css.hasPermission(Commands.LEVEL_MODERATORS))
            .executes(ctx -> {
                var enabled = PaucalConfig.common().loadContributors();
                if (!enabled) {
                    ctx.getSource().sendFailure(Component.translatable("command.paucal.reload.disabled"));
                    return 0;
                }

                ContributorsManifest.loadContributors();

                for (var player : ctx.getSource().getLevel().players()) {
                    IXplatAbstractions.INSTANCE.sendPacketToPlayerS2C(player, new MsgReloadContributorsS2C());
                }

                ctx.getSource().sendSuccess(() -> Component.translatable("command.paucal.reload"), true);
                return 1;
            }));
    }
}
