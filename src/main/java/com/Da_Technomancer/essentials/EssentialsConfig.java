package com.Da_Technomancer.essentials;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;

import java.util.Arrays;
import java.util.List;

public class EssentialsConfig{

	public static ForgeConfigSpec.BooleanValue addWrench;

	private static ForgeConfigSpec.ConfigValue<List<? extends String>> wrenchTypes;
	public static ForgeConfigSpec.IntValue brazierRange;
	public static ForgeConfigSpec.IntValue itemChuteRange;
	public static ForgeConfigSpec.DoubleValue fertileSoilRate;
	public static ForgeConfigSpec.IntValue maxRedstoneRange;
	public static ForgeConfigSpec.EnumValue numberDisplay;

	private static ForgeConfigSpec clientSpec;
	private static ForgeConfigSpec serverSpec;

	protected static void init(){
		//Client config
		ForgeConfigSpec.Builder clientBuilder = new ForgeConfigSpec.Builder();
		addWrench = clientBuilder.worldRestart().comment("Should the Wrench show up in the creative menu?").define("creative_wrench", true);
		numberDisplay = clientBuilder.comment("How should very large and small numbers be displayed?", "Options are: NORMAL, SCIENTIFIC, ENGINEERING, and RAW").defineEnum("num_display", NumberTypes.SCIENTIFIC);

		clientSpec = clientBuilder.build();
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, clientSpec);

		//Server config
		ForgeConfigSpec.Builder serverBuilder = new ForgeConfigSpec.Builder();
		wrenchTypes = serverBuilder.comment("Item ids for wrench items. Should be in format 'modid:itemregistryname', ex. minecraft:apple or essentials:wrench").defineList("wrench_types", (List<String>) Arrays.asList(Essentials.MODID + ":wrench", "crossroads:liech_wrench", "actuallyadditions:itemlaserwrench", "appliedenergistics2:certus_quartz_wrench", "appliedenergistics2:nether_quartz_wrench", "base:wrench", "enderio:itemyetawrench", "extrautils2:wrench", "bigreactors:wrench", "forestry:wrench", "progressiveautomation:wrench", "thermalfoundation:wrench", "redstonearsenal:tool.wrench_flux", "rftools:smartwrench", "immersiveengineering:tool"), (Object s) -> s instanceof String && ((String) s).contains(":"));
		brazierRange = serverBuilder.comment("Range of the Brazier anti-witch effect", "Set to 0 to disable").defineInRange("brazier_range", 64, 0, 512);
		itemChuteRange = serverBuilder.comment("Maximum Transport Chutes in a line").defineInRange("chute_limit", 16, 0, 128);
		fertileSoilRate = serverBuilder.comment("Percent of normal speed Fertile Soil should work at", "Set to 0 to disable").defineInRange("fertile_rate", 100D, 0, 100);
		maxRedstoneRange = serverBuilder.comment("Range of signals through Circuit Wire").defineInRange("redstone_range", 16, 1, 128);

		serverSpec = serverBuilder.build();
		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, serverSpec);
	}

	protected static void load(){
		CommentedFileConfig clientConfig = CommentedFileConfig.builder(FMLPaths.CONFIGDIR.get().resolve(Essentials.MODID + "-client.toml")).sync().autosave().writingMode(WritingMode.REPLACE).build();
		clientConfig.load();
		clientSpec.setConfig(clientConfig);

		CommentedFileConfig serverConfig = CommentedFileConfig.builder(FMLPaths.CONFIGDIR.get().resolve(Essentials.MODID + "-server.toml")).sync().autosave().writingMode(WritingMode.REPLACE).build();
		serverConfig.load();
		serverSpec.setConfig(serverConfig);
	}

	/**
	 * @param stack The stack to test
	 * @return Whether this item is considered a wrench
	 */
	public static boolean isWrench(ItemStack stack){
		if(stack.isEmpty()){
			return false;
		}
		ResourceLocation loc = stack.getItem().getRegistryName();
		if(loc == null){
			return false;
		}
		String name = loc.toString();
		for(String s : wrenchTypes.get()){
			if(name.equals(s)){
				return true;
			}
		}
		return false;
	}

	/**
	 * Formats floating point values for display
	 * @param value The value to format
	 * @param format The format to conform the value to. Uses the value in the config if null.
	 * @return The formatted string version, for display
	 */
	public static String formatFloat(float value, NumberTypes format){
		if(format == null){
			format = (NumberTypes) numberDisplay.get();
		}
		switch(format){
			case SCIENTIFIC:
				float absValue = Math.abs(value);
				if(absValue == 0){
					return "0";
				}
				if(absValue < 1000 && absValue >= 0.0005F){
					return trimTrail(Math.round(value * 1000F) / 1000F);
				}

				int expon = (int) Math.floor(Math.log10(absValue));
				return trimTrail(Math.round(value * 1000F * Math.pow(10, -expon)) / 1000F) + "\u00D710^" + expon;
			case ENGINEERING:
				float absoValue = Math.abs(value);
				if(absoValue == 0){
					return "0";
				}
				if(absoValue < 1000 && absoValue >= 0.0005){
					return trimTrail(Math.round(value * 1000F) / 1000F);
				}

				int exponent = (int) Math.floor(Math.log10(absoValue));
				if(exponent > 0){
					exponent -= exponent % 3;
				}else if(exponent % 3 != 0){
					exponent -= 3 + exponent % 3;
				}
				return trimTrail(Math.round(value * 1000F * Math.pow(10, -exponent)) / 1000F) + "\u00D710^" + exponent;
			case RAW:
				//This option exists mainly for debugging. It shows the entire value in normal decimal notation as it is actually saved.
				if(value == 0 || value == -0){
					return "0";
				}
				StringBuilder output = new StringBuilder(".");
				int buildVal = (int) Math.abs(value);

				//Add portion before the decimal point, one digit at a time
				while(buildVal != 0){
					output.insert(0, buildVal % 10);
					buildVal /= 10;
				}

				if((int) value == value){
					output.deleteCharAt(output.length() - 1);//Remove the decimal point
				}else{
					//Add portion after the decimal point, one digit at a time
					float decValue = Math.abs(value) % 1;
					decValue *= 10F;
					while(decValue > 0){
						int digit = (int) decValue;
						output.append(digit);
						decValue *= 10F;
						decValue %= 10;
					}
				}

				if(value < 0){
					output.insert(0, "-");
				}

				return output.toString();
			case NORMAL:
			default:
				return Float.toString(value);
		}
	}

	private static String trimTrail(float valFloat){
		String val = "" + valFloat;
		while(val.contains(".") && (val.endsWith("0") || val.endsWith("."))){
			val = val.substring(0, val.length() - 2);
		}
		return val;
	}

	public enum NumberTypes{

		NORMAL(),//Java default
		SCIENTIFIC(),//Scientific notation when magnitude outside of 0.001-1000
		ENGINEERING(),//Engineering notation when magnitude outside of 0.001-1000
		RAW();//Every single decimal point. You want this? WHAT IS WRONG WITH YOU?
	}
}
