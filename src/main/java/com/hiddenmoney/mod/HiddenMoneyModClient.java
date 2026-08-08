package com.hiddenmoney.mod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

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
    private static boolean wasCtrlPressed = false;
    private static boolean wasJumpPressed = false;
    private static DimensionType lastDimension = null;
    private static double lastX = 0;
    private static double lastZ = 0;
    private static int blocksWalked = 0;

    public enum DimensionType {
        OVERWORLD,
        NETHER,
        OTHER
    }

    @Override
    public void onInitializeClient() {
        // Регистрация событий
        
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
            blocksWalked = 0;
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
            
            // Проверка Ctrl (код 341)
            boolean isCtrlPressed = InputUtil.isKeyPressed(
                MinecraftClient.getInstance().getWindow().getHandle(), 
                GLFW.GLFW_KEY_LEFT_CONTROL
            ) || InputUtil.isKeyPressed(
                MinecraftClient.getInstance().getWindow().getHandle(), 
                GLFW.GLFW_KEY_RIGHT_CONTROL
            );
            
            if (isCtrlPressed && !wasCtrlPressed) {
                addMoney(uuid, -100); // Нажатие ctrl -100 рублей
            }
            wasCtrlPressed = isCtrlPressed;
            
            // Проверка смены слота
            int currentSlot = player.getInventory().selectedSlot;
            if (lastSlot != -1 && lastSlot != currentSlot) {
                addMoney(uuid, -5); // Перемещение текущего выбранного слота -5 рублей
            }
            lastSlot = currentSlot;
            
            // Проверка прыжка
            boolean isJumpPressed = InputUtil.isKeyPressed(
                MinecraftClient.getInstance().getWindow().getHandle(),
                GLFW.GLFW_KEY_SPACE
            );
            if (isJumpPressed && !wasJumpPressed) {
                addMoney(uuid, -20); // Прыжок -20 рублей
            }
            wasJumpPressed = isJumpPressed;
            
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
                    blocksWalked += blocksPassed;
                }
                lastX = player.getX();
                lastZ = player.getZ();
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
        
        // Событие поломки блока
        BlockBreakCallback.EVENT.register((player, world, pos, state, blockEntity) -> {
            addMoney(player.getUuid(), -50); // Сломанный блок -50 рублей
            return ActionResult.PASS;
        });
        
        // Событие установки блока
        BlockPlaceCallback.EVENT.register((player, world, hand, hitResult) -> {
            addMoney(player.getUuid(), 2); // Поставленный блок +2 рубля
            return ActionResult.PASS;
        });
        
        // Событие атаки сущности
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            addMoney(player.getUuid(), -10); // Удар игроком что угодно -10 рублей
            return ActionResult.PASS;
        });
        
        // Событие выброса предмета
        PlayerDropItemCallback.EVENT.register((player, inventory, stack) -> {
            addMoney(player.getUuid(), 1); // Выброшенный предмет +1 рубль
            return true;
        });
        
        // Событие убийства сущности
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            // Будет обработано после проверки смерти сущности
            return ActionResult.PASS;
        });
        
        // Отслеживание открытия инвентаря
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof InventoryScreen && client.player != null) {
                addMoney(client.player.getUuid(), -15); // Заход в инвентарь -15 рублей
            }
        });
        
        // Отслеживание отправки сообщений в чат
        // Fabric не имеет прямого API для этого, нужен mixin
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
    
    public static void onPlayerDamaged(UUID playerUuid, DamageSource source, float amount) {
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
}
