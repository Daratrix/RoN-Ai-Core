package com.daratrix.ronapi.ai.player;

import com.daratrix.ronapi.ai.registers.AiPacketHandler;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.player.PlayerAction;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class AiPlayerServerboundPacket {
    PlayerAction action;
    public int x;
    public int y;
    public int z;
    public String aiName;

    public static void startRTSBot(Level level, Faction faction, String aiName, Integer x, Integer y, Integer z) {
        if (level != null) {
            BlockState bs = level.getBlockState(new BlockPos(x, y, z));
            if (!bs.getFluidState().isEmpty()) {
                HudClientEvents.showTemporaryMessage("Invalid starting location");
                return;
            }

            PlayerAction playerAction = switch (faction) {
                case VILLAGERS -> PlayerAction.START_RTS_VILLAGERS;
                case MONSTERS -> PlayerAction.START_RTS_MONSTERS;
                case PIGLINS -> PlayerAction.START_RTS_PIGLINS;
                case NONE -> null;
            };

            AiPacketHandler.INSTANCE.sendToServer(new AiPlayerServerboundPacket(playerAction, x, y, z, aiName));
        }

    }

    // packet-handler functions
    public AiPlayerServerboundPacket(PlayerAction action, Integer x, Integer y, Integer z, String aiName) {
        this.action = action;
        this.x = x;
        this.y = y;
        this.z = z;
        this.aiName = aiName;
    }

    public AiPlayerServerboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(PlayerAction.class);
        this.x = buffer.readInt();
        this.y = buffer.readInt();
        this.z = buffer.readInt();
        this.aiName = buffer.readUtf();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeInt(this.x);
        buffer.writeInt(this.y);
        buffer.writeInt(this.z);
        buffer.writeUtf(this.aiName);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        AtomicBoolean success = new AtomicBoolean(false);
        //this.aiName = this.action.name();
        ((NetworkEvent.Context)ctx.get()).enqueueWork(() -> {
            switch (this.action) {
                case START_RTS_VILLAGERS -> AiPlayerServerEvents.startRTSBot(ctx.get().getSender().server, this.aiName, new Vec3(this.x, this.y, this.z), Faction.VILLAGERS);
                case START_RTS_MONSTERS -> AiPlayerServerEvents.startRTSBot(ctx.get().getSender().server, this.aiName, new Vec3(this.x, this.y, this.z), Faction.MONSTERS);
                case START_RTS_PIGLINS -> AiPlayerServerEvents.startRTSBot(ctx.get().getSender().server, this.aiName, new Vec3(this.x, this.y, this.z), Faction.PIGLINS);
            }

            success.set(true);
        });
        ((NetworkEvent.Context)ctx.get()).setPacketHandled(true);
        return success.get();
    }
}
