package net.joefoxe.hexerei.events;

import net.joefoxe.hexerei.Hexerei;
import net.joefoxe.hexerei.block.custom.SageBurningPlate;
import net.joefoxe.hexerei.config.HexConfig;
import net.joefoxe.hexerei.util.HexereiUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber
public class SageBurningPlateEvent {

    /*@SubscribeEvent
    public void onEntityJoin(LivingSpawnEvent.CheckSpawn e) {
        Level world = e.getLevel().isClientSide() ? null : e.getLevel() instanceof Level ? (Level)e.getLevel() : null;

        if (world == null) {
            return;
        }

        if(e.getSpawnReason() != MobSpawnType.NATURAL)
            return;

        if(HexConfig.SAGE_BURNING_PLATE_RANGE.get()==0)
            return;

        Entity entity = e.getEntity();

        if (entity.getTags().contains(Hexerei.MOD_ID + ".checked" )) {

            return;
        }
        entity.addTag(Hexerei.MOD_ID + ".checked");

        if (!HexereiUtil.entityIsHostile(entity)) {
            return;
        }

        List<BlockPos> nonSagePlatesInList = new ArrayList<>();

        if (Hexerei.sageBurningPlateTileList.size() == 0) {
            return;
        }

        BlockPos burning_plate = null;
        for (BlockPos nearbySageBurningPlate : Hexerei.sageBurningPlateTileList) {
            if (entity.distanceToSqr(nearbySageBurningPlate.getX() + 0.5f, nearbySageBurningPlate.getY(), nearbySageBurningPlate.getZ() + 0.5f) < HexConfig.SAGE_BURNING_PLATE_RANGE.get() * HexConfig.SAGE_BURNING_PLATE_RANGE.get() + 1) {
                BlockState burning_platestate = world.getBlockState(nearbySageBurningPlate);
                Block block = burning_platestate.getBlock();

                if (!(block instanceof SageBurningPlate)) {
                    nonSagePlatesInList.add(nearbySageBurningPlate);
                    continue;
                }

                if (!burning_platestate.getValue(SageBurningPlate.LIT)) {
                    continue;
                }

                burning_plate = nearbySageBurningPlate.immutable();
                break;
            }
        }
        for(BlockPos nonSageBurninPlate : nonSagePlatesInList){
//            System.out.println(Hexerei.sageBurningPlateTileList.indexOf(nonSageBurninPlate));
            Hexerei.sageBurningPlateTileList.remove(nonSageBurninPlate);
        }

        if (burning_plate == null) {
            return;
        }

        List<Entity> passengers = entity.getPassengers();
        if (passengers.size() > 0) {
            for (Entity passenger : passengers) {
                passenger.remove(RemovalReason.DISCARDED);
            }
        }

        e.setResult(Result.DENY);
    } */

}