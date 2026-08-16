package com.hiddenmoney.mod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HiddenMoneyModClient implements ClientModInitializer {
    
    public static final Map<UUID, Integer> playerMoney = new HashMap<>();
    public static final Map<UUID, Integer> chatMessageCount = new HashMap<>();
    public static final Set<UUID> playersWhoDied = new HashSet<>();
    
    private static int lastSlot = -1;
    private static DimensionType lastDimension = null;
    private static double lastX = 0;
    private static double lastZ = 0;
    private static boolean wasInInventory = false;
    private static int eventTimer = 0;
    private static boolean eventNotificationSent = false;

    public enum DimensionType {
        OVERWORLD,
        NETHER,
        OTHER
    }

    @Override
    public void onInitializeClient() {
        // Событие захода в мир
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            PlayerEntity player = client.player;
            if (player != null && !playerMoney.containsKey(player.getUuid())) {
                playerMoney.put(player.getUuid(), 1000); // Начальные 1000 рублей
                chatMessageCount.put(player.getUuid(), 0);
                lastX = player.getX();
                lastZ = player.getZ();
            }
            // Сброс при новом заходе
            lastDimension = null;
            lastSlot = -1;
            eventTimer = 0;
            eventNotificationSent = false;
        });
        
        // Событие выхода из мира
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (client.player != null) {
                UUID uuid = client.player.getUuid();
                playerMoney.remove(uuid);
                chatMessageCount.remove(uuid);
                playersWhoDied.remove(uuid);
            }
        });
        
        // Тик игрока - отслеживание действий
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PlayerEntity player = client.player;
            if (player == null) return;
            
            UUID uuid = player.getUuid();
            
            // Проверка смены слота
            int currentSlot = player.getInventory().selectedSlot;
            if (lastSlot != -1 && lastSlot != currentSlot) {
                addMoney(uuid, -5); // Перемещение текущего выбранного слота -5 рублей
            }
            lastSlot = currentSlot;
            
            // Проверка измерения
            DimensionType currentDim = getDimensionType(player);
            if (lastDimension != null && lastDimension != currentDim) {
                if (lastDimension == DimensionType.OVERWORLD && currentDim == DimensionType.NETHER) {
                    addMoney(uuid, 10); // Перемещение между обычным миром в ад +10 рублей
                } else if (lastDimension == DimensionType.NETHER && currentDim == DimensionType.OVERWORLD) {
                    addMoney(uuid, 5); // Перемещение между адом и обычным миром +5 рублей
                }
            }
            lastDimension = currentDim;
            
            // Отслеживание пройденных блоков
            double dx = Math.abs(player.getX() - lastX);
            double dz = Math.abs(player.getZ() - lastZ);
            if (dx >= 1.0 || dz >= 1.0) {
                int blocksPassed = (int)(Math.floor(Math.max(dx, dz)));
                if (blocksPassed > 0) {
                    addMoney(uuid, -blocksPassed); // Пройденный блок -1 рубль за каждый
                }
                lastX = player.getX();
                lastZ = player.getZ();
            }
            
            // Отслеживание открытия инвентаря
            boolean isInInventory = client.currentScreen instanceof InventoryScreen;
            if (isInInventory && !wasInInventory) {
                addMoney(uuid, -15); // Заход в инвентарь -15 рублей
            }
            wasInInventory = isInInventory;
            
            // Таймер для событий (каждые 5 минут = 6000 тиков)
            eventTimer++;
            if (eventTimer >= 6000) {
                eventTimer = 0;
                Integer money = playerMoney.get(uuid);
                if (money != null && money < 0) {
                    // Шанс 50%
                    if (client.world.random.nextFloat() < 0.5f) {
                        triggerEvent(client.world, player, money);
                        eventNotificationSent = true;
                    }
                }
            }
        });
        
        // Рендеринг панели с деньгами справа
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;
            
            UUID uuid = client.player.getUuid();
            Integer money = playerMoney.get(uuid);
            if (money == null) return;
            
            // Рисуем панель справа
            int x = client.getWindow().getScaledWidth() - 100;
            int y = 10;
            
            drawContext.fill(x, y, x + 90, y + 20, 0x80000000);
            drawContext.drawText(
                client.textRenderer, 
                "Баланс: " + money + " ₽", 
                x + 5, 
                y + 6, 
                0xFFFFFF, 
                true
            );
        });
        
        // Событие поломки блока (до разрушения)
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            addMoney(player.getUuid(), -50); // Сломанный блок -50 рублей
            return true;
        });
        
        // Событие установки блока
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!stack.isEmpty()) {
                addMoney(player.getUuid(), 2); // Поставленный блок +2 рубля
            }
            return ActionResult.PASS;
        });
        
        // Событие атаки сущности
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            addMoney(player.getUuid(), -10); // Удар игроком что угодно -10 рублей
            return ActionResult.PASS;
        });
        
        // Событие выброса предмета (через атаку блока для дропа)
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            // Это не совсем то, но оставляем как заглушку
            return ActionResult.PASS;
        });
    }
    
    private static DimensionType getDimensionType(PlayerEntity player) {
        if (player.getWorld().getRegistryKey() == net.minecraft.world.World.OVERWORLD) {
            return DimensionType.OVERWORLD;
        } else if (player.getWorld().getRegistryKey() == net.minecraft.world.World.NETHER) {
            return DimensionType.NETHER;
        }
        return DimensionType.OTHER;
    }
    
    public static void addMoney(UUID uuid, int amount) {
        Integer current = playerMoney.get(uuid);
        if (current != null) {
            playerMoney.put(uuid, current + amount);
        }
    }
    
    public static void onEntityKilled(UUID playerUuid, Entity entity) {
        // Убийство любого существа +5 рублей
        addMoney(playerUuid, 5);
    }
    
    public static void onPlayerDeath(UUID playerUuid) {
        if (!playersWhoDied.contains(playerUuid)) {
            addMoney(playerUuid, -500); // Смерть -500 рублей
            playersWhoDied.add(playerUuid);
        }
    }
    
    public static void resetDeathFlag(UUID playerUuid) {
        playersWhoDied.remove(playerUuid);
    }
    
    public static void onPlayerDamaged(UUID playerUuid) {
        addMoney(playerUuid, -150); // Получение урона -150 рублей
    }
    
    public static void onChatMessage(UUID playerUuid) {
        Integer count = chatMessageCount.get(playerUuid);
        if (count == null) count = 0;
        
        if (count < 5) {
            addMoney(playerUuid, 50); // Написание в чате любого сообщения +50 рублей (только 5 раз)
            chatMessageCount.put(playerUuid, count + 1);
        }
    }
    
    public static void onItemDropped(UUID playerUuid) {
        addMoney(playerUuid, 1); // Выброшенный предмет +1 рубль
    }
    
    private static void triggerEvent(World world, PlayerEntity player, int money) {
        BlockPos playerPos = player.getBlockPos();
        
        if (money >= -1 && money <= -100) {
            // События для баланса от -1 до -100
            int eventType = world.random.nextInt(3);
            switch (eventType) {
                case 0:
                    // Динамит под игроком (таймер 5 секунд, без урона)
                    EventSystem.spawnSafeTNT(world, playerPos.down(), 100);
                    break;
                case 1:
                    // Телепортация на 3 блока вверх
                    player.teleport(player.getX(), player.getY() + 3, player.getZ());
                    break;
                case 2:
                    // 5 зомби "Буржуи"
                    EventSystem.spawnZombies(world, playerPos, 5, false);
                    break;
            }
        } else if (money >= -101 && money <= -500) {
            // События для баланса от -101 до -500
            int eventType = world.random.nextInt(2);
            switch (eventType) {
                case 0:
                    // Динамит под игроком (таймер 3 секунды, без урона)
                    EventSystem.spawnSafeTNT(world, playerPos.down(), 60);
                    break;
                case 1:
                    // 10 зомби "Буржуи"
                    EventSystem.spawnZombies(world, playerPos, 10, false);
                    break;
            }
        } else if (money >= -501 && money <= -1000) {
            // События для баланса от -501 до -1000
            int eventType = world.random.nextInt(2);
            switch (eventType) {
                case 0:
                    // 20 маленьких зомби "Буржуи"
                    EventSystem.spawnZombies(world, playerPos, 20, true);
                    break;
                case 1:
                    // Эффекты и удаление предмета из руки
                    EventSystem.applyNegativeEffects(player);
                    EventSystem.removeItemFromHand(player);
                    break;
            }
        } else if (money <= -1001) {
            // События для баланса от -1001 и ниже
            int eventType = world.random.nextInt(2);
            switch (eventType) {
                case 0:
                    // Вардены в радиусе 10 блоков
                    EventSystem.spawnWardens(world, playerPos, 10);
                    break;
                case 1:
                    // Телепортация на 100 блоков вверх, эффекты, удаление предмета и брони
                    player.teleport(player.getX(), player.getY() + 100, player.getZ());
                    EventSystem.applyNegativeEffects(player);
                    EventSystem.removeItemFromHand(player);
                    EventSystem.removeArmor(player);
                    break;
            }
        }
    }
}
