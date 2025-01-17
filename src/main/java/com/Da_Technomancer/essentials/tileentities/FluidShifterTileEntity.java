package com.Da_Technomancer.essentials.tileentities;

import com.Da_Technomancer.essentials.Essentials;
import com.Da_Technomancer.essentials.blocks.BlockUtil;
import com.Da_Technomancer.essentials.gui.container.FluidShifterContainer;
import com.Da_Technomancer.essentials.gui.container.FluidSlotManager;
import com.Da_Technomancer.essentials.gui.container.IFluidSlotTE;
import com.Da_Technomancer.essentials.packets.INBTReceiver;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ObjectHolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@ObjectHolder(Essentials.MODID)
public class FluidShifterTileEntity extends AbstractShifterTileEntity implements IFluidSlotTE, INBTReceiver{

	@ObjectHolder("fluid_shifter")
	private static TileEntityType<FluidShifterTileEntity> TYPE = null;
	private static final int CAPACITY = 4_000;

	private FluidSlotManager fluidManager;
	private FluidStack fluid = FluidStack.EMPTY;

	public FluidSlotManager getFluidManager(){
		if(fluidManager == null){
			fluidManager = new FluidSlotManager(world, pos, fluid, CAPACITY, 0);
			fluidManager.markChanged();
		}
		return fluidManager;
	}

	public FluidShifterTileEntity(){
		super(TYPE);
	}

	@Override
	public void tick(){
		if(world.isRemote || fluid == null){
			return;
		}

		if(endPos == null){
			refreshCache();
		}

		TileEntity outputTE = world.getTileEntity(endPos);
		Direction dir = getFacing();
		LazyOptional<IFluidHandler> outHandlerCon;
		if(outputTE != null && (outHandlerCon = outputTE.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, dir.getOpposite())).isPresent()){
			IFluidHandler outHandler = outHandlerCon.orElseThrow(NullPointerException::new);
			int filled = outHandler.fill(fluid, IFluidHandler.FluidAction.EXECUTE);
			if(filled != 0){
				FluidSlotManager manager = getFluidManager();
				fluid.shrink(filled);
				manager.updateState(fluid);
				markDirty();
			}
		}
	}

	@Override
	public CompoundNBT write(CompoundNBT nbt){
		super.write(nbt);
		nbt.put("fluid", fluid.writeToNBT(new CompoundNBT()));
		return nbt;
	}

	@Override
	public void read(CompoundNBT nbt){
		super.read(nbt);
		fluid = FluidStack.loadFluidStackFromNBT(nbt.getCompound("fluid"));
	}

	@Override
	public CompoundNBT getUpdateTag(){
		return write(super.getUpdateTag());
	}

	@Override
	public void remove(){
		super.remove();
		invOptional.invalidate();
	}

	private LazyOptional<IFluidHandler> invOptional = LazyOptional.of(FluidHandler::new);

	@SuppressWarnings("unchecked")
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction facing){
		if(cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY){
			return (LazyOptional<T>) invOptional;
		}

		return super.getCapability(cap, facing);
	}

	@Override
	public ITextComponent getDisplayName(){
		return new TranslationTextComponent("container.fluid_shifter");
	}

	@Nullable
	@Override
	public Container createMenu(int id, PlayerInventory playerInventory, PlayerEntity player){
		return new FluidShifterContainer(id, playerInventory, pos);
	}

	@Override
	public IFluidHandler getFluidHandler(){
		return invOptional.orElseGet(FluidHandler::new);
	}

	@Override
	public boolean isRemote(){
		return world.isRemote;
	}

	@Override
	public void receiveNBT(CompoundNBT nbt){
		getFluidManager().handlePacket(nbt);
		fluid = getFluidManager().getStack();
	}

	private class FluidHandler implements IFluidHandler{

		@Override
		public int getTanks(){
			return 1;
		}

		@Nonnull
		@Override
		public FluidStack getFluidInTank(int tank){
			return fluid;
		}

		@Override
		public int getTankCapacity(int tank){
			return CAPACITY;
		}

		@Override
		public boolean isFluidValid(int tank, @Nonnull FluidStack stack){
			return true;
		}

		@Override
		public int fill(FluidStack resource, FluidAction action){
			if((fluid.isEmpty() || BlockUtil.sameFluid(fluid, resource)) && !resource.isEmpty()){
				int filled = Math.min(CAPACITY - fluid.getAmount(), resource.getAmount());
				if(action.execute()){
					if(fluid.isEmpty()){
						fluid = resource.copy();
						fluid.setAmount(filled);
					}else{
						fluid.grow(filled);
					}
					markDirty();
					getFluidManager().updateState(fluid);
				}
				return filled;
			}

			return 0;
		}

		@Nonnull
		@Override
		public FluidStack drain(FluidStack resource, FluidAction action){
			if(BlockUtil.sameFluid(fluid, resource)){
				int drained = Math.min(resource.getAmount(), fluid.getAmount());
				FluidStack drainFluid = drained == 0 ? FluidStack.EMPTY : fluid.copy();
				if(!drainFluid.isEmpty()){
					drainFluid.setAmount(drained);
				}

				if(action.execute()){
					fluid.shrink(drained);
					markDirty();
					getFluidManager().updateState(fluid);
				}
				return drainFluid;
			}

			return FluidStack.EMPTY;
		}

		@Nonnull
		@Override
		public FluidStack drain(int maxDrain, FluidAction action){
			int drained = Math.min(maxDrain, fluid.getAmount());
			FluidStack drainFluid = drained == 0 ? FluidStack.EMPTY : fluid.copy();
			if(!drainFluid.isEmpty()){
				drainFluid.setAmount(drained);
			}

			if(action.execute()){
				fluid.shrink(drained);
				getFluidManager().updateState(fluid);
				markDirty();
			}

			return drainFluid;
		}
	}
}
