package com.hiddenmoney.mod.mixin;

import com.hiddenmoney.mod.HiddenMoneyModClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.player == null || client.world == null) return;
        
        // Отслеживание смерти игрока
        if (player.isDead()) {
            HiddenMoneyModClient.onPlayerDeath(player.getUuid());
        }
        
        // Сброс флага смерти при возрождении
        if (player.isAlive() && HiddenMoneyModClient.playersWhoDied.contains(player.getUuid())) {
            HiddenMoneyModClient.resetDeathFlag(player.getUuid());
        }
    }
    
    @Inject(method = "jump", at = @At("HEAD"))
    private void onJump(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        HiddenMoneyModClient.addMoney(player.getUuid(), -20); // Прыжок -20 рублей
    }
}
