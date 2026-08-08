package com.hiddenmoney.mod.mixin;

import com.hiddenmoney.mod.HiddenMoneyModClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.gui.hud.ChatHud.class)
public class ChatInputSuggestorMixin {

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"))
    private void onAddMessage(net.minecraft.text.Text text, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            String content = text.getString();
            // Проверяем, что сообщение не пустое и не команда
            if (!content.isEmpty() && !content.startsWith("/")) {
                HiddenMoneyModClient.onChatMessage(client.player.getUuid());
            }
        }
    }
    
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;I)V", at = @At("HEAD"))
    private void onAddMessageWithTimestamp(net.minecraft.text.Text text, int ticks, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            String content = text.getString();
            // Проверяем, что сообщение не пустое и не команда
            if (!content.isEmpty() && !content.startsWith("/")) {
                HiddenMoneyModClient.onChatMessage(client.player.getUuid());
            }
        }
    }
}
