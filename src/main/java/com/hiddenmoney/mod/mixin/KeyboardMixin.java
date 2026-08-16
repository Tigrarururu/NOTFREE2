package com.hiddenmoney.mod.mixin;

import com.hiddenmoney.mod.HiddenMoneyModClient;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.Keyboard.class)
public class KeyboardMixin {
    
    private static boolean wasCtrlPressed = false;
    
    @Inject(method = "onKey", at = @At("HEAD"))
    private void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (action == GLFW.GLFW_PRESS) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                // Проверка Ctrl (левый или правый)
                boolean isCtrlPressed = key == GLFW.GLFW_KEY_LEFT_CONTROL || key == GLFW.GLFW_KEY_RIGHT_CONTROL;
                
                if (isCtrlPressed && !wasCtrlPressed) {
                    HiddenMoneyModClient.addMoney(client.player.getUuid(), -100); // Нажатие ctrl -100 рублей
                }
                wasCtrlPressed = isCtrlPressed;
            }
        }
        
        if (action == GLFW.GLFW_RELEASE) {
            if (wasCtrlPressed) {
                wasCtrlPressed = false;
            }
        }
    }
}
