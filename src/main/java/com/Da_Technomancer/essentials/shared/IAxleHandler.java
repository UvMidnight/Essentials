package com.Da_Technomancer.essentials.shared;

import javax.annotation.Nonnull;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Gears and other rotary connectables use two capabilities, ICogHandler and IAxleHandler. 
 * The AxleHandler represents the core of the block, that can connect to machines and axles.
 * The CogHandler represents the part of the block able to connect to other blocks.
 *
 * In most cases, the AxleHandler and CogHandler are on the same side, though there are exceptions. Some blocks may only have one of them.
 */
public interface IAxleHandler{

	/**
	 * [0]=w, [1]=E, [2]=P, [3]=lastE.
	 * Must be mutable and allow modification of the original values through it.
	 */
	public double[] getMotionData();

	/**
	 * If lastRadius equals 0, then the AxleHandler should not convert the rotationRationIn, as this is an axial connection.
	 * The caller is normally responsible for adjusting the sign on the rotationRatioIn
	 */
	public void propogate(@Nonnull IAxisHandler masterIn, byte key, double rotationRatioIn, double lastRadius);

	public double getMoInertia();
	
	public double getRotationRatio();

	/**
	 * Resets the angle based on sign of the rotation ratio (small gears go to 0 degrees if positive or 22.5 if negative). Use is optional.
	 */
	public default void resetAngle(){
		
	}

	/**
	 * @return The angle of this axle in degrees. Use is optional. 
	 */
	public default float getAngle(){
		return 0;
	}
	
	/**
	 * Use is optional. 
	 * @param angleIn Sets the angle value. 
	 */
	public default void setAngle(float angleIn){
		
	}
	
	/**
	 * To be used for rendering with partial ticks. Use of this is completely optional. If it is used, then it should return a prediction for the next angle value (usually found by assuming the speed is constant).
	 */
	@SideOnly(Side.CLIENT)
	public default float getNextAngle(){
		return getAngle();
	}

	/**
	 * negative value decreases energy. For non-gears (or axes) affecting the
	 * network
	 * absolute controls whether the change is relative or absolute (to spin direction)
	 */
	public default void addEnergy(double energy, boolean allowInvert, boolean absolute){
		double[] motionData = getMotionData();
		if(allowInvert && absolute){
			motionData[1] += energy;
		}else if(allowInvert){
			motionData[1] += energy * Math.signum(motionData[1]);
		}else if(absolute){
			int sign = (int) Math.signum(motionData[1]);
			motionData[1] += energy;
			if(sign != 0 && Math.signum(motionData[1]) != sign){
				motionData[1] = 0;
			}
		}else{
			int sign = (int) Math.signum(motionData[1]);
			motionData[1] += energy * ((double) sign);
			if(Math.signum(motionData[1]) != sign){
				motionData[1] = 0;
			}
		}
		markChanged();
	}
	
	/**
	 * Should be called whenever a value in the AxleHandler is changed by something outside the AxleHandler. 
	 * Used to markDirty() in tile entities. 
	 */
	public void markChanged();
	
	/**
	 * @return The angular velocity the client is using to calculate angle. Use is optional. 
	 */
	public default float getClientW(){
		return 0;
	}
	
	/**
	 * Synchronizes the angle and angular velocity to the client. Use is optional.
	 */
	public default void syncAngle(){
		
	}
	
	/**
	 * @return Whether the Master Axis should keep the angle and clientW synchronized to client. If true, this should implement syncAngle, getAngle, setAngle, resetAngle, and getClientW. 
	 */
	public boolean shouldManageAngle();
}