package net.joefoxe.hexerei.block.custom;

import net.joefoxe.hexerei.item.custom.FlowerOutputItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.RegistryObject;

public class PickableDoubleFlower extends DoublePlantBlock implements BonemealableBlock {
    protected static final float AABB_OFFSET = 3.0F;
    protected static final VoxelShape SHAPE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 10.0D, 11.0D);
    protected static final VoxelShape SHAPE_BOTTOM = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 16.0D, 12.0D);
    private final MobEffect suspiciousStewEffect;
    private final int effectDuration;
    public static final int MAX_AGE = 3;
    public int type;
    public RegistryObject<FlowerOutputItem> firstOutput;
    public int maxFirstOutput;
    public RegistryObject<FlowerOutputItem> secondOutput;
    public int maxSecondOutput;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    public PickableDoubleFlower(MobEffect p_53512_, int p_53513_, Properties p_53514_, RegistryObject<FlowerOutputItem> firstOutput , int maxFirstOutput, RegistryObject<FlowerOutputItem> secondOutput , int maxSecondOutput) {
        super(p_53514_);
        this.suspiciousStewEffect = p_53512_;
        if (p_53512_.isInstantenous()) {
            this.effectDuration = p_53513_;
        } else {
            this.effectDuration = p_53513_ * 20;
        }

        this.firstOutput = firstOutput;
        this.maxFirstOutput = maxFirstOutput;
        this.secondOutput = secondOutput;
        this.maxSecondOutput = maxSecondOutput;

        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0).setValue(HALF, DoubleBlockHalf.LOWER));

    }
    public PickableDoubleFlower(MobEffect p_53512_, int p_53513_, Properties p_53514_, RegistryObject<FlowerOutputItem> firstOutput , int maxFirstOutput) {
        super(p_53514_);
        this.suspiciousStewEffect = p_53512_;
        if (p_53512_.isInstantenous()) {
            this.effectDuration = p_53513_;
        } else {
            this.effectDuration = p_53513_ * 20;
        }

        this.firstOutput = firstOutput;
        this.maxFirstOutput = maxFirstOutput;
        this.secondOutput = null;

        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));

    }

    public VoxelShape getShape(BlockState p_53517_, BlockGetter p_53518_, BlockPos p_53519_, CollisionContext p_53520_) {
        Vec3 vec3 = p_53517_.getOffset(p_53518_, p_53519_);
        if(p_53517_.getValue(HALF) == DoubleBlockHalf.LOWER)
            return SHAPE_BOTTOM.move(vec3.x, vec3.y, vec3.z);
        else
            return SHAPE.move(vec3.x, vec3.y, vec3.z);
    }

    public OffsetType getOffsetType() {
        return OffsetType.XZ;
    }

    public boolean isRandomlyTicking(BlockState p_57284_) {
        return p_57284_.getValue(AGE) < 3;
    }

    public void randomTick(BlockState p_57286_, ServerLevel p_57287_, BlockPos p_57288_, RandomSource p_57289_) {
        int i = p_57286_.getValue(AGE);
        if(p_57286_.hasProperty(HALF) && p_57286_.getValue(HALF) == DoubleBlockHalf.LOWER){
            if (i < 3 && p_57287_.getRawBrightness(p_57288_.above(), 0) >= 9 && net.minecraftforge.common.ForgeHooks.onCropsGrowPre(p_57287_, p_57288_, p_57286_, p_57289_.nextInt(10) == 0)) {

                if(p_57286_.hasProperty(AGE) && p_57287_.getBlockState(p_57288_.above()).hasProperty(AGE)) {
                    p_57287_.setBlock(p_57288_, p_57286_.setValue(AGE, i + 1), 2);
                    if(p_57287_.getBlockState(p_57288_.above()).getBlock() instanceof PickableDoubleFlower)
                        p_57287_.setBlock(p_57288_.above(), p_57287_.getBlockState(p_57288_.above()).setValue(AGE, i + 1), 2);
                    else {
                        this.destroy(p_57287_, p_57288_, p_57286_);
                    }
                    net.minecraftforge.common.ForgeHooks.onCropsGrowPost(p_57287_, p_57288_, p_57286_);
                }
            }
        }
    }

    public InteractionResult use(BlockState blockstate, Level level, BlockPos blockpos, Player player, InteractionHand hand, BlockHitResult hit) {
        int i = blockstate.getValue(AGE);
        boolean flag = i == 3;
        if (!flag && player.getItemInHand(hand).is(Items.BONE_MEAL)) {
            return InteractionResult.PASS;
        } else if (i > 1) {

            ItemStack firstOutput = new ItemStack(this.firstOutput.get(), 4);
            ItemStack secondOutput = ItemStack.EMPTY;
            if(this.secondOutput != null)
                secondOutput = new ItemStack(this.secondOutput.get(), this.maxSecondOutput);
            int j = Math.max(1, level.random.nextInt(firstOutput.getCount()));
            int k = 0;
            if(this.secondOutput != null)
                k = Math.max(1, level.random.nextInt(secondOutput.getCount()));
            popResource(level, blockpos, new ItemStack(firstOutput.getItem(), Math.max(1,(int)Math.floor(j/2f)) + (flag ? (int)Math.ceil(j/2f) : 0)));
            if (level.random.nextInt(2) == 0 && this.secondOutput != null)
                popResource(level, blockpos, new ItemStack(secondOutput.getItem(), Math.max(1,(int)Math.floor(k/2f)) + (flag ? (int)Math.ceil(k/2f) : 0)));
            level.playSound(null, blockpos, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + level.random.nextFloat() * 0.4F);
            if(blockstate.getValue(HALF) == DoubleBlockHalf.LOWER) {
                level.setBlock(blockpos, blockstate.setValue(AGE, 0), 2);

                BlockState blockState = level.getBlockState(blockpos.above());

                if(blockState.getBlock() == level.getBlockState(blockpos).getBlock())
                {
                    level.setBlock(blockpos.above(), blockState.setValue(AGE, 0), 2);
                }
                else if(blockState.isAir()){
                    level.setBlock(blockpos.above(), level.getBlockState(blockpos).setValue(AGE, 0).setValue(HALF, DoubleBlockHalf.UPPER), 2);
                }

            }else {
                level.setBlock(blockpos, blockstate.setValue(AGE, 0), 2);
//                level.setBlock(blockpos.below(), level.getBlockState(blockpos.below()).setValue(AGE, Integer.valueOf(0)), 2);

                BlockState blockState = level.getBlockState(blockpos.below());

                if(blockState.getBlock() == level.getBlockState(blockpos).getBlock())
                {
                    level.setBlock(blockpos.below(), blockState.setValue(AGE, 0), 2);
                }
                else if(blockState.isAir()){
                    level.setBlock(blockpos.below(), level.getBlockState(blockpos).setValue(AGE, 0).setValue(HALF, DoubleBlockHalf.LOWER), 2);
                }
            }


            return InteractionResult.sidedSuccess(level.isClientSide);
        } else {
            return super.use(blockstate, level, blockpos, player, hand, hit);
        }
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_57282_) {
        p_57282_.add(AGE).add(HALF);
    }

    public boolean isValidBonemealTarget(BlockGetter p_57260_, BlockPos p_57261_, BlockState p_57262_, boolean p_57263_) {
        return p_57262_.getValue(AGE) < 3;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader pLevel, BlockPos pPos, BlockState pState, boolean pIsClient) {
        return false;
    }

    public boolean isBonemealSuccess(Level p_57265_, RandomSource p_57266_, BlockPos p_57267_, BlockState p_57268_) {
        return true;
    }

    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos blockPos, BlockState blockState) {


        if(blockState.getValue(HALF) == DoubleBlockHalf.LOWER) {
            int i = Math.min(3, blockState.getValue(AGE) + 1);
            level.setBlock(blockPos, blockState.setValue(AGE, i), 2);
            level.setBlock(blockPos.above(), level.getBlockState(blockPos.above()).setValue(AGE, i), 2);
        }else {
            int i = Math.min(3, level.getBlockState(blockPos.below()).getValue(AGE) + 1);
            level.setBlock(blockPos, blockState.setValue(AGE, i), 2);
            level.setBlock(blockPos.below(), level.getBlockState(blockPos.below()).setValue(AGE, i), 2);
        }
    }
}