package com.Da_Technomancer.essentials.blocks;

import com.Da_Technomancer.essentials.EssentialsConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateContainer;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class ItemChute extends Block{

	private static final VoxelShape[] BB = new VoxelShape[] {makeCuboidShape(0, 2, 2, 16, 14, 14), makeCuboidShape(2, 0, 2, 14, 16, 14), makeCuboidShape(2, 2, 0, 14, 14, 16)};
	
	protected ItemChute(){
		super(Properties.create(Material.IRON).hardnessAndResistance(1.5F).sound(SoundType.METAL));
		String name = "item_chute";
		setRegistryName(name);
		EssentialsBlocks.toRegister.add(this);
		EssentialsBlocks.blockAddQue(this);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context){
		return getDefaultState().with(EssentialsProperties.AXIS, context.getFace().getAxis());
	}

	@Override
	public boolean onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity playerIn, Hand hand, BlockRayTraceResult hit){
		if(EssentialsConfig.isWrench(playerIn.getHeldItem(hand))){
			if(!worldIn.isRemote){
				worldIn.setBlockState(pos, state.cycle(EssentialsProperties.AXIS));
			}
			return true;
		}
		return false;
	}

	@Override
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context){
		return BB[state.get(EssentialsProperties.AXIS).ordinal()];
	}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public void addInformation(ItemStack stack, @Nullable IBlockReader world, List<ITextComponent> tooltip, ITooltipFlag advanced){
		tooltip.add(new StringTextComponent("Extends the output range of Item Shifters along a line of chutes"));
		tooltip.add(new StringTextComponent("Safe for decoration"));
	}

	@Override
	protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder){
		builder.add(EssentialsProperties.AXIS);
	}

	@Override
	public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean flag){
		//Block updates are propogated down lines of Item Chutes, allowing caching of target positions for Item Shifters
		if(fromPos != null){
			Direction.Axis axis = state.get(EssentialsProperties.AXIS);
			Direction dir = Direction.getFacingFromVector(pos.getX() - fromPos.getX(), pos.getY() - fromPos.getY(), pos.getZ() - fromPos.getZ());
			if(dir.getAxis() == axis){
				fromPos = pos;
				pos = pos.offset(dir);
				worldIn.getBlockState(pos).neighborChanged(worldIn, pos, this, fromPos, false);
			}
		}
	}
}
