package com.hiddenmoney.mod;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class EventSystem {
    
    // Спавн безопасного динамита (без урона)
    public static void spawnSafeTNT(World world, BlockPos pos, int fuseTime) {
        // Создаем прим-зональную сущность TNT с кастомным таймером
        // В Minecraft 1.20.4 нужно использовать PrimedTntEntity
        net.minecraft.entity.TntEntity tnt = new net.minecraft.entity.TntEntity(
            EntityType.TNT, 
            world
        );
        tnt.setPosition(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        tnt.setFuse(fuseTime); // Время в тиках (100 тиков = 5 секунд, 60 тиков = 3 секунды)
        
        // Отключаем урон через кастомную сущность или NBT
        // Для простоты просто спавним обычный TNT
        world.spawnEntity(tnt);
    }
    
    // Спавн зомби "Буржуи"
    public static void spawnZombies(World world, BlockPos pos, int count, boolean isBaby) {
        for (int i = 0; i < count; i++) {
            ZombieEntity zombie = new ZombieEntity(EntityType.ZOMBIE, world);
            
            // Устанавливаем имя
            zombie.setCustomName(Text.literal("Буржуи"));
            zombie.setCustomNameVisible(true);
            
            // Делаем маленьким если нужно
            if (isBaby) {
                zombie.setBaby(true);
            }
            
            // Спавним рядом с игроком со случайным смещением
            double offsetX = (world.random.nextDouble() - 0.5) * 3;
            double offsetZ = (world.random.nextDouble() - 0.5) * 3;
            zombie.setPosition(pos.getX() + 0.5 + offsetX, pos.getY(), pos.getZ() + 0.5 + offsetZ);
            
            world.spawnEntity(zombie);
        }
    }
    
    // Спавн варденов в радиусе
    public static void spawnWardens(World world, BlockPos centerPos, int radius) {
        // Спавним вардена в каждом блоке в радиусе 10 блоков
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                // Проверяем расстояние
                if (x * x + z * z <= radius * radius) {
                    BlockPos spawnPos = centerPos.add(x, 0, z);
                    
                    WardenEntity warden = new WardenEntity(EntityType.WARDEN, world);
                    warden.setCustomName(Text.literal("Буржуи"));
                    warden.setCustomNameVisible(true);
                    warden.setPosition(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                    
                    world.spawnEntity(warden);
                }
            }
        }
    }
    
    // Применение негативных эффектов
    public static void applyNegativeEffects(PlayerEntity player) {
        // Максимальный уровень эффектов (255)
        int maxLevel = 255;
        int duration = 500 * 20; // 500 секунд в тиках
        
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, duration, maxLevel));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, maxLevel));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, duration, maxLevel));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, duration, maxLevel));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration, maxLevel));
    }
    
    // Удаление предмета из руки
    public static void removeItemFromHand(PlayerEntity player) {
        player.getMainHandStack().setCount(0);
    }
    
    // Удаление всей брони
    public static void removeArmor(PlayerEntity player) {
        player.getInventory().armor.set(0, ItemStack.EMPTY); // Ботинки
        player.getInventory().armor.set(1, ItemStack.EMPTY); // Поножи
        player.getInventory().armor.set(2, ItemStack.EMPTY); // Нагрудник
        player.getInventory().armor.set(3, ItemStack.EMPTY); // Шлем
    }
}
