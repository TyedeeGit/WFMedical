package com.warfactory.medical.network;

import com.warfactory.medical.WFMedical;
import com.warfactory.medical.config.MedicalConfig;
import com.warfactory.medical.core.MedicalProfile;
import com.warfactory.medical.core.damage.HitAuthority;
import com.warfactory.medical.core.limb.LimbType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Map;
import java.util.WeakHashMap;

public final class MedicalNetworking {

    private static final String PROTOCOL = "3";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(WFMedical.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);
    private static final Map<ServerPlayer, MedicalSyncPacket> LAST_SENT = new WeakHashMap<>();
    // Last game tick each player was re-baselined with a full authoritative sync (safety net; see syncTo).
    private static final Map<ServerPlayer, Long> LAST_FULL_TICK = new WeakHashMap<>();
    private static boolean registered;

    private MedicalNetworking() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        CHANNEL.messageBuilder(MedicalSyncPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MedicalSyncPacket::encode)
                .decoder(MedicalSyncPacket::decode)
                .consumerMainThread((packet, ctx) -> {
                    packet.handleClient();
                    ctx.get().setPacketHandled(true);
                })
                .add();


        CHANNEL.messageBuilder(MedicalActionPacket.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder(MedicalActionPacket::encode)
                .decoder(MedicalActionPacket::decode)
                .consumerMainThread((packet, ctx) -> {
                    packet.handleServer(ctx.get().getSender());
                    ctx.get().setPacketHandled(true);
                })
                .add();


        CHANNEL.messageBuilder(ActiveTreatmentPacket.class, 4, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ActiveTreatmentPacket::encode)
                .decoder(ActiveTreatmentPacket::decode)
                .consumerMainThread((packet, ctx) -> {
                    packet.handleClient();
                    ctx.get().setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(DownedStatePacket.class, 5, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DownedStatePacket::encode)
                .decoder(DownedStatePacket::decode)
                .consumerMainThread((packet, ctx) -> {
                    packet.handleClient();
                    ctx.get().setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(RemoveTourniquetPacket.class, 6, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RemoveTourniquetPacket::encode)
                .decoder(RemoveTourniquetPacket::decode)
                .consumerMainThread((packet, ctx) -> {
                    packet.handleServer(ctx.get().getSender());
                    ctx.get().setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(MedicalDeltaPacket.class, 7, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MedicalDeltaPacket::encode)
                .decoder(MedicalDeltaPacket::decode)
                .consumerMainThread((packet, ctx) -> {
                    packet.handleClient();
                    ctx.get().setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(PoseStreamPacket.class, 8, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PoseStreamPacket::encode)
                .decoder(PoseStreamPacket::decode)
                .consumerMainThread((packet, ctx) -> {
                    packet.handleServer(ctx.get().getSender());
                    ctx.get().setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(HitAuthorityPacket.class, 9, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(HitAuthorityPacket::encode)
                .decoder(HitAuthorityPacket::decode)
                .consumerMainThread((packet, ctx) -> {
                    packet.handleClient();
                    ctx.get().setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(TourniquetStatePacket.class, 10, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TourniquetStatePacket::encode)
                .decoder(TourniquetStatePacket::decode)
                .consumerMainThread((packet, ctx) -> {
                    packet.handleClient();
                    ctx.get().setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(TreatmentTargetRequestPacket.class, 11, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TreatmentTargetRequestPacket::encode)
                .decoder(TreatmentTargetRequestPacket::decode)
                .consumerMainThread((packet, ctx) -> {
                    packet.handleServer(ctx.get().getSender());
                    ctx.get().setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(TreatmentTargetInfoPacket.class, 12, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TreatmentTargetInfoPacket::encode)
                .decoder(TreatmentTargetInfoPacket::decode)
                .consumerMainThread((packet, ctx) -> {
                    packet.handleClient();
                    ctx.get().setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(CancelTreatmentPacket.class, 13, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CancelTreatmentPacket::encode)
                .decoder(CancelTreatmentPacket::decode)
                .consumerMainThread((packet, ctx) -> {
                    packet.handleServer(ctx.get().getSender());
                    ctx.get().setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(TargetSheetRequestPacket.class, 14, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TargetSheetRequestPacket::encode)
                .decoder(TargetSheetRequestPacket::decode)
                .consumerMainThread((packet, ctx) -> {
                    packet.handleServer(ctx.get().getSender());
                    ctx.get().setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(TargetSheetInfoPacket.class, 15, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TargetSheetInfoPacket::encode)
                .decoder(TargetSheetInfoPacket::decode)
                .consumerMainThread((packet, ctx) -> {
                    packet.handleClient();
                    ctx.get().setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(GiveUpPacket.class, 16, NetworkDirection.PLAY_TO_SERVER)
                .encoder(GiveUpPacket::encode)
                .decoder(GiveUpPacket::decode)
                .consumerMainThread((packet, ctx) -> {
                    packet.handleServer(ctx.get().getSender());
                    ctx.get().setPacketHandled(true);
                })
                .add();
    }

    public static int tourniquetMask(MedicalProfile profile) {
        int mask = 0;
        for (LimbType lt : LimbType.VALUES) {
            if (profile.limb(lt).hasTourniquet()) {
                mask |= (1 << lt.ordinal());
            }
        }
        return mask;
    }

    public static void broadcastTourniquets(LivingEntity entity, MedicalProfile profile) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                new TourniquetStatePacket(entity.getId(), tourniquetMask(profile)));
    }

    public static void sendTourniquetsTo(ServerPlayer viewer, int entityId, int mask) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer), new TourniquetStatePacket(entityId, mask));
    }

    public static void sendHitAuthority(ServerPlayer player) {
        boolean stream = MedicalConfig.useClientPose();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new HitAuthorityPacket(stream));
    }

    public static void sendFull(ServerPlayer player, MedicalProfile profile) {
        MedicalSyncPacket full = MedicalSyncPacket.fromProfile(profile);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), full);
        LAST_SENT.put(player, full);
        LAST_FULL_TICK.put(player, player.level().getGameTime());
        if (MedicalConfig.logMedicalSync()) {
            WFMedical.LOGGER.info("[wfmed-sync] FULL  -> {} {}", player.getGameProfile().getName(), summarize(full));
        }
    }

    public static void syncTo(ServerPlayer player, MedicalProfile profile) {
        MedicalSyncPacket prev = LAST_SENT.get(player);
        if (prev == null) {
            sendFull(player, profile);
            return;
        }

        // Safety net: periodically re-baseline with a full authoritative sync so any client-side delta drift
        // (a dropped / late / mis-based delta that left the cache stuck on a stale or falsely-healthy limb)
        // self-corrects within the interval. Invisible while in sync -- the full carries the same data the
        // deltas already produced -- so this only ever visibly does anything when a desync actually happened.
        int fullInterval = MedicalConfig.syncFullResyncIntervalTicks();
        if (fullInterval > 0) {
            long now = player.level().getGameTime();
            Long lastFull = LAST_FULL_TICK.get(player);
            if (lastFull == null || now - lastFull >= fullInterval) {
                sendFull(player, profile);
                return;
            }
        }

        MedicalSyncPacket full = MedicalSyncPacket.fromProfile(profile);
        MedicalDeltaPacket delta = MedicalDeltaPacket.diff(prev, full);
        if (delta.isEmpty()) {
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), delta);
        LAST_SENT.put(player, full);
        if (MedicalConfig.logMedicalSync()) {
            WFMedical.LOGGER.info("[wfmed-sync] DELTA -> {} mask={} {}", player.getGameProfile().getName(),
                    Integer.toBinaryString(delta.mask()), summarize(full));
        }
    }

    /** Compact per-limb health%/bleed/pain/wound-count line for sync tracing (see logMedicalSync). */
    private static String summarize(MedicalSyncPacket packet) {
        StringBuilder sb = new StringBuilder(96);
        sb.append("state=").append(packet.state()).append(" blood=").append(Math.round(packet.bloodMl()))
                .append(" limbs[");
        MedicalSyncPacket.LimbSummary[] limbs = packet.limbs();
        for (int i = 0; i < limbs.length; i++) {
            MedicalSyncPacket.LimbSummary s = limbs[i];
            if (s == null) {
                continue;
            }
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(s.limb()).append('=').append(Math.round(s.healthPercent() * 100.0F)).append('%');
            if (s.bleeding() > 0.0F) {
                sb.append(",bleed").append(String.format(java.util.Locale.ROOT, "%.2f", s.bleeding()));
            }
            if (s.pain() > 0.0F) {
                sb.append(",pain").append(String.format(java.util.Locale.ROOT, "%.2f", s.pain()));
            }
            if (s.fracture()) {
                sb.append(",fx");
            }
            int w = s.wounds() == null ? 0 : s.wounds().size();
            if (w > 0) {
                sb.append(",w").append(w);
            }
        }
        return sb.append(']').toString();
    }

    public static void sendActiveTreatment(ServerPlayer player, ActiveTreatmentPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendTargetInfo(ServerPlayer medic, TreatmentTargetInfoPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> medic), packet);
    }

    public static void sendTargetSheet(ServerPlayer medic, TargetSheetInfoPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> medic), packet);
    }

    public static void broadcastDowned(ServerPlayer player, boolean downed) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new DownedStatePacket(player.getId(), downed));
    }

    public static void sendDownedTo(ServerPlayer viewer, int entityId, boolean downed) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer), new DownedStatePacket(entityId, downed));
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
