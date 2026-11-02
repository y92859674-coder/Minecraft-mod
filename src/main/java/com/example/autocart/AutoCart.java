package com.example.autocart;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;

public class AutoCart implements ModInitializer {

    private record Pending(ServerPlayer player, InteractionHand hand, BlockPos pos) {}

    private static final List<Pending> PENDING = new ArrayList<>();

    @Override
    public void onInitialize() {
        UseBlockCallback.EVENT.register((Player player, Level level, InteractionHand hand, BlockHitResult hit) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer sp)) {
                return InteractionResult.PASS;
            }
            ItemStack held = sp.getItemInHand(hand);
            if (!held.is(ItemTags.RAILS) || findCart(sp) < 0) {
                return InteractionResult.PASS;
            }
            BlockPos pos = new BlockPlaceContext(sp, hand, held, hit).getClickedPos();
            PENDING.add(new Pending(sp, hand, pos.immutable()));
            return InteractionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (PENDING.isEmpty()) return;
            List<Pending> todo = new ArrayList<>(PENDING);
            PENDING.clear();

            for (Pending p : todo) {
                ServerPlayer player = p.player();
                Level level = player.level();
                if (!level.getBlockState(p.pos()).is(BlockTags.RAILS)) continue;

                int slot = findCart(player);
                if (slot < 0) continue;

                ItemStack cart = player.getInventory().getItem(slot);
                ItemStack previous = player.getItemInHand(p.hand());
                BlockHitResult hit = new BlockHitResult(
                        Vec3.atCenterOf(p.pos()), Direction.UP, p.pos(), false);

                player.setItemInHand(p.hand(), cart);
                try {
                    cart.useOn(new UseOnContext(player, p.hand(), hit));
                } finally {
                    player.setItemInHand(p.hand(), previous);
                }
            }
        });
    }

    private static int findCart(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(Items.TNT_MINECART)) return i;
        }
        return -1;
    }
}
