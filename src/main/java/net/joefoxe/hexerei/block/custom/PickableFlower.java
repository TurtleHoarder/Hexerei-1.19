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
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.RegistryObject;

public class PickableFlower extends BushBlock implements BonemealableBlock {
    protected static final float AABB_OFFSET = 3.0F;
    protected static final VoxelShape SHAPE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 10.0D, 11.0D);
    private final MobEffect suspiciousStewEffect;
    private final int effectDuration;
    public static final int MAX_AGE = 3;
    public int type;
    public RegistryObject<FlowerOutputItem> firstOutput;
    public int maxFirstOutput;
    public RegistryObject<FlowerOutputItem> secondOutput;
    public int maxSecondOutput;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;

    public PickableFlower(MobEffect p_53512_, int p_53513_, BlockBehaviour.Properties p_53514_, RegistryObject<FlowerOutputItem> firstOutput , int maxFirstOutput, RegistryObject<FlowerOutputItem> secondOutput , int maxSecondOutput) {
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

        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));

    }
    public PickableFlower(MobEffect p_53512_, int p_53513_, BlockBehaviour.Properties p_53514_, RegistryObject<FlowerOutputItem> firstOutput , int maxFirstOutput) {
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
        return SHAPE.move(vec3.x, vec3.y, vec3.z);
    }

    public BlockBehaviour.OffsetType getOffsetType() {
        return BlockBehaviour.OffsetType.XZ;
    }

    public MobEffect getSuspiciousStewEffect() {
        return this.suspiciousStewEffect;
    }

    public int getEffectDuration() {
        return this.effectDuration;
    }

//    public VoxelShape getShape(BlockState p_57291_, BlockGetter p_57292_, BlockPos p_57293_, CollisionContext p_57294_) {
//        if (p_57291_.getValue(AGE) == 0) {
//            return SAPLING_SHAPE;
//        } else {
//            return p_57291_.getValue(AGE) < 3 ? MID_GROWTH_SHAPE : super.getShape(p_57291_, p_57292_, p_57293_, p_57294_);
//        }
//    }

    public boolean isRandomlyTicking(BlockState p_57284_) {
        return p_57284_.getValue(AGE) < 3;
    }

    public void randomTick(BlockState p_57286_, ServerLevel p_57287_, BlockPos p_57288_, RandomSource p_57289_) {
        int i = p_57286_.getValue(AGE);
        if (i < 3 && p_57287_.getRawBrightness(p_57288_.above(), 0) >= 9 && net.minecraftforge.common.ForgeHooks.onCropsGrowPre(p_57287_, p_57288_, p_57286_,p_57289_.nextInt(5) == 0)) {
            p_57287_.setBlock(p_57288_, p_57286_.setValue(AGE, i + 1), 2);
            net.minecraftforge.common.ForgeHooks.onCropsGrowPost(p_57287_, p_57288_, p_57286_);
        }

    }

    public InteractionResult use(BlockState p_57275_, Level p_57276_, BlockPos p_57277_, Player p_57278_, InteractionHand p_57279_, BlockHitResult p_57280_) {
        int i = p_57275_.getValue(AGE);
        boolean flag = i == 3;
        if (!flag && p_57278_.getItemInHand(p_57279_).is(Items.BONE_MEAL)) {
            return InteractionResult.PASS;
        } else if (i > 1) {

            ItemStack firstOutput = new ItemStack(this.firstOutput.get(), this.maxFirstOutput);
            ItemStack secondOutput = ItemStack.EMPTY;
            if (this.secondOutput != null)
                secondOutput = new ItemStack(this.secondOutput.get(), this.maxSecondOutput);
            int j = Math.max(1, p_57276_.random.nextInt(firstOutput.getCount()));
            int k = 0;
            if (this.secondOutput != null)
                k = Math.max(1, p_57276_.random.nextInt(secondOutput.getCount()));
            popResource(p_57276_, p_57277_, new ItemStack(firstOutput.getItem(), (int) Math.floor(j / 2f) + (flag ? (int) Math.ceil(j / 2f) : 0)));
            if (p_57276_.random.nextInt(2) == 0 && this.secondOutput != null)
                popResource(p_57276_, p_57277_, new ItemStack(secondOutput.getItem(), (int) Math.floor(k / 2f) + (flag ? (int) Math.ceil(k / 2f) : 0)));
            p_57276_.playSound(null, p_57277_, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + p_57276_.random.nextFloat() * 0.4F);
            p_57276_.setBlock(p_57277_, p_57275_.setValue(AGE, 0), 2);


            return InteractionResult.sidedSuccess(p_57276_.isClientSide);
        } else {
            return super.use(p_57275_, p_57276_, p_57277_, p_57278_, p_57279_, p_57280_);
        }
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_57282_) {
        p_57282_.add(AGE);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader pLevel, BlockPos pPos, BlockState pState, boolean pIsClient) {
        return pState.getValue(AGE) < 3;}

    public boolean isBonemealSuccess(Level p_57265_, RandomSource p_57266_, BlockPos p_57267_, BlockState p_57268_) {
        return true;
    }

    public void performBonemeal(ServerLevel p_57251_, RandomSource p_57252_, BlockPos p_57253_, BlockState p_57254_) {
        int i = Math.min(3, p_57254_.getValue(AGE) + 1);
        p_57251_.setBlock(p_57253_, p_57254_.setValue(AGE, i), 2);
    }
}