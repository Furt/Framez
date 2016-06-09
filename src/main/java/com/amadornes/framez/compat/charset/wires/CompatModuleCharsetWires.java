package com.amadornes.framez.compat.charset.wires;

import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.amadornes.framez.api.DynamicReference;
import com.amadornes.framez.api.FramezAPI;
import com.amadornes.framez.api.motor.IMotor;
import com.amadornes.framez.api.motor.IMotorExtension;
import com.amadornes.framez.api.motor.IMotorTrigger;
import com.amadornes.framez.api.motor.IMotorVariable;
import com.amadornes.framez.api.motor.MotorTriggerType;
import com.amadornes.framez.compat.IModule;

import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import pl.asie.charset.api.wires.IBundledEmitter;
import pl.asie.charset.api.wires.IBundledReceiver;

public class CompatModuleCharsetWires implements IModule {

    @CapabilityInject(IBundledReceiver.class)
    public static final Capability<IBundledReceiver> CAPABILITY_BUNDLED_RECEIVER = null;
    @CapabilityInject(IBundledEmitter.class)
    public static final Capability<IBundledEmitter> CAPABILITY_BUNDLED_EMITTER = null;

    @Override
    public void preInit(FMLPreInitializationEvent event) {

        FramezAPI.INSTANCE.getMotorRegistry().registerExtension(new ResourceLocation("CharsetWires", "IBundledReceiver"),
                m -> new BundledReceiverExtension(m));
    }

    @Override
    public void init(FMLInitializationEvent event) {

    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {

    }

    public class BundledReceiverExtension implements IMotorExtension, IBundledReceiver {

        private final DynamicReference<? extends IMotor> motor;
        private final Map<ResourceLocation, BundledTrigger> triggers = new LinkedHashMap<ResourceLocation, BundledTrigger>();
        private BitSet values = new BitSet(16);

        public BundledReceiverExtension(DynamicReference<? extends IMotor> motor) {

            this.motor = motor;
            for (int i = 0; i < 16; i++)
                triggers.put(new ResourceLocation("CharsetWires", "bundled." + EnumDyeColor.byMetadata(i).getName()),
                        new BundledTrigger(this, i));
        }

        @Override
        public Map<IMotorVariable<?>, Object> getProvidedVariables() {

            return Collections.emptyMap();
        }

        @Override
        public Map<ResourceLocation, BundledTrigger> getProvidedTriggers() {

            return triggers;
        }

        @Override
        public boolean hasCapability(Capability<?> capability, EnumFacing facing) {

            return capability == CAPABILITY_BUNDLED_RECEIVER;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getCapability(Capability<T> capability, EnumFacing facing) {

            return (T) (capability == CAPABILITY_BUNDLED_RECEIVER ? this : null);
        }

        @Override
        public <T> T alterValue(T value, IMotorVariable<T> variable) {

            return value;
        }

        @Override
        public int getAlterationPriority(IMotorVariable<?> variable) {

            return 0;
        }

        @Override
        public NBTTagCompound serializeNBT() {

            NBTTagCompound tag = new NBTTagCompound();
            tag.setByteArray("values", values.toByteArray());
            return tag;
        }

        @Override
        public void deserializeNBT(NBTTagCompound tag) {

            values = BitSet.valueOf(tag.getByteArray("values"));
        }

        @Override
        public void onBundledInputChange() {

            values.clear();
            for (EnumFacing f : EnumFacing.VALUES) {
                TileEntity te = motor.get().getMotorWorld().getTileEntity(motor.get().getMotorPos().offset(f));
                if (te != null && te.hasCapability(CAPABILITY_BUNDLED_EMITTER, f.getOpposite())) {
                    byte[] signal = te.getCapability(CAPABILITY_BUNDLED_EMITTER, f.getOpposite()).getBundledSignal();
                    for (int i = 0; i < 16; i++)
                        if (!values.get(i) && signal[i] != 0) values.set(i);
                }
            }
        }

    }

    public class BundledTrigger implements IMotorTrigger {

        private final BundledReceiverExtension extension;
        private final int color;

        public BundledTrigger(BundledReceiverExtension extension, int color) {

            this.extension = extension;
            this.color = color;
        }

        @Override
        public String getUnlocalizedName(boolean inverted) {

            return "trigger.framez:bundled." + EnumDyeColor.byMetadata(color).getName();
        }

        @Override
        public ItemStack getIconStack(boolean inverted) {

            return new ItemStack(Item.getByNameOrId("CharsetWires:wire"), 1, (color + 1) * 2);
        }

        @Override
        public MotorTriggerType getTriggerType() {

            return MotorTriggerType.TYPE_REDSTONE_BUNLDED[color];
        }

        @Override
        public boolean isActive() {

            return extension.values.get(color);
        }

        @Override
        public boolean canBeInverted() {

            return true;
        }

        @Override
        public boolean requiresInvertedOverlay() {

            return true;
        }

    }

}
