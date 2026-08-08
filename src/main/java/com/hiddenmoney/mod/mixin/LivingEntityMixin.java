package com.hiddenmoney.mod.mixin;

import com.hiddenmoney.mod.HiddenMoneyModClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "damage", at = @At("HEAD"))
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        
        // Отслеживание получения урона игроком
        if (entity instanceof net.minecraft.entity.player.PlayerEntity) {
            HiddenMoneyModClient.onPlayerDamaged(entity.getUuid(), source, amount);
        }
    }
    
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onDeath(DamageSource source, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        
        // Отслеживание смерти существа (для награды игроку)
        // Нужно найти кто убил
        if (source.getAttacker() instanceof net.minecraft.entity.player.PlayerEntity) {
            net.minecraft.entity.player.PlayerEntity player = 
                (net.minecraft.entity.player.PlayerEntity) source.getAttacker();
            HiddenMoneyModClient.onEntityKilled(player.getUuid(), entity);
        }
    }
}
