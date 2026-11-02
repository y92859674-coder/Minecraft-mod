package com.example.autocart;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class AutoCart implements ClientModInitializer {

    private static BlockPos pendingPos = null;
    private static int ticksLeft = 0;

    @Override
    public void onInitializeClient() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!level.isClientSide()) return InteractionResult.PASS;
            ItemStack held = player.getItemInHand(hand);
            if (!held.is(ItemTags.RAILS) || findCartSlot(player) < 0) {
                return InteractionResult.PASS;
            }
            pendingPos = new BlockPlaceContext(player, hand, held, hit).getClickedPos().immutable();
            ticksLeft = 20;
            return InteractionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (pendingPos == null) return;
            LocalPlayer player = mc.player;
            if (player == null || mc.level == null || mc.gameMode == null) {
                pendingPos = null;
                return;
            }
            if (mc.level.getBlockState(pendingPos).is(BlockTags.RAILS)) {
                BlockPos pos = pendingPos;
                pendingPos = null;
                int slot = findCartSlot(player);
                if (slot < 0) return;

                Inventory inv = player.getInventory();
                int old = inv.getSelectedSlot();
                inv.setSelectedSlot(slot);
                BlockHitResult hit = new BlockHitResult(
                        Vec3.atCenterOf(pos), Direction.UP, pos, false);
                mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
                inv.setSelectedSlot(old);
            } else if (--ticksLeft <= 0) {
                pendingPos = null;
            }
        });
    }

    private static int findCartSlot(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).is(Items.TNT_MINECART)) return i;
        }
        return -1;
    }
}
