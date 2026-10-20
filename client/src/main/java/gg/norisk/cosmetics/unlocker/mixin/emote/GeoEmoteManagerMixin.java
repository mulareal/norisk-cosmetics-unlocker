package gg.norisk.cosmetics.unlocker.mixin.emote;

import gg.norisk.animations.emote.NrcEmoteHolder;
import gg.norisk.cosmetics.unlocker.api.ClientHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(targets = "gg.norisk.animations.emote.GeoEmoteManager", remap = false)
public abstract class GeoEmoteManagerMixin {

    private static boolean isLocalPlayerHolder(NrcEmoteHolder holder) {
        if (holder == null) return false;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return false;
            if (holder instanceof Player p) {
                return p.getUUID().equals(mc.player.getUUID());
            }
            return holder.getCurrentEntityId() == mc.player.getId();
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Inject(method = "playEmote(Ljava/util/UUID;Ljava/lang/String;D)Z", at = @At("HEAD"), remap = false)
    private void onPlayEmoteUuid(UUID playerUuid, String emoteId, double transitionTime, CallbackInfoReturnable<Boolean> cir) {
        if (playerUuid != null && emoteId != null && !emoteId.isEmpty()) {
            try {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && playerUuid.equals(mc.player.getUUID())) {
                    ClientHooks.localEmote(emoteId, true);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    @Inject(method = "playEmote(Lgg/norisk/animations/emote/NrcEmoteHolder;Ljava/lang/String;D)Z", at = @At("HEAD"), remap = false)
    private void onPlayEmoteHolder(NrcEmoteHolder holder, String emoteId, double transitionTime, CallbackInfoReturnable<Boolean> cir) {
        if (holder != null && emoteId != null && !emoteId.isEmpty()) {
            if (isLocalPlayerHolder(holder)) {
                ClientHooks.localEmote(emoteId, true);
            }
        }
    }

    @Inject(method = "stopEmote(Ljava/util/UUID;Ljava/lang/String;D)V", at = @At("HEAD"), remap = false)
    private void onStopEmoteUuid(UUID playerUuid, String emoteId, double transitionTime, CallbackInfo ci) {
        if (playerUuid != null && emoteId != null && !emoteId.isEmpty()) {
            try {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && playerUuid.equals(mc.player.getUUID())) {
                    ClientHooks.localEmote(emoteId, false);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    @Inject(method = "stopEmote(Lgg/norisk/animations/emote/NrcEmoteHolder;Ljava/lang/String;D)V", at = @At("HEAD"), remap = false)
    private void onStopEmoteHolder(NrcEmoteHolder holder, String emoteId, double transitionTime, CallbackInfo ci) {
        if (holder != null && emoteId != null && !emoteId.isEmpty()) {
            if (isLocalPlayerHolder(holder)) {
                ClientHooks.localEmote(emoteId, false);
            }
        }
    }

    @Inject(method = "stopAllEmotes(Ljava/util/UUID;D)V", at = @At("HEAD"), remap = false)
    private void onStopAllEmotesUuid(UUID playerUuid, double transitionTime, CallbackInfo ci) {
        if (playerUuid != null) {
            try {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && playerUuid.equals(mc.player.getUUID())) {
                    ClientHooks.localEmote("all", false);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    @Inject(method = "stopAllEmotes(Lgg/norisk/animations/emote/NrcEmoteHolder;D)V", at = @At("HEAD"), remap = false)
    private void onStopAllEmotesHolder(NrcEmoteHolder holder, double transitionTime, CallbackInfo ci) {
        if (holder != null) {
            if (isLocalPlayerHolder(holder)) {
                ClientHooks.localEmote("all", false);
            }
        }
    }
}
