package com.amadornes.framez.part;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.lib.raytracer.ExtendedMOP;
import codechicken.lib.raytracer.IndexedCuboid6;
import codechicken.lib.render.EntityDigIconFX;
import codechicken.lib.render.RenderUtils;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import codechicken.microblock.HollowMicroblock;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TNormalOcclusion;

import com.amadornes.framez.api.FramezApi;
import com.amadornes.framez.api.IFrame;
import com.amadornes.framez.api.IFrameModifier;
import com.amadornes.framez.api.IFrameModifierProvider;
import com.amadornes.framez.client.render.RenderFrame;
import com.amadornes.framez.ref.References;
import com.amadornes.framez.util.Utils;

public class PartFrame extends TMultiPart implements TNormalOcclusion, IFrame {

    private static final Cuboid6[] subParts = new Cuboid6[] { null, null, null, null, null, null };

    private Object[] connections = new Object[] { null, null, null, null, null, null };

    private List<IFrameModifier> modifiers = new ArrayList<IFrameModifier>();

    private boolean[] render = new boolean[20];

    private boolean isConnected(ForgeDirection dir) {

        return getConnection(dir) != null && getConnection(dir) instanceof IFrame;
    }

    public Object getConnection(ForgeDirection dir) {

        return connections[dir.ordinal()];
    }

    private boolean shouldRenderCorner(ForgeDirection face, ForgeDirection a, ForgeDirection b) {

        Object of = face != ForgeDirection.UNKNOWN ? getConnection(face) : null;
        Object oa = getConnection(a);
        Object ob = getConnection(b);

        if (face == ForgeDirection.UNKNOWN) {
            return !(oa != null || ob != null) || ((oa == null || !(oa instanceof PartFrame)) && (ob == null || !(ob instanceof PartFrame)));
        } else {
            if (oa != null && ob != null && oa instanceof PartFrame && ob instanceof PartFrame && ((PartFrame) oa).isConnected(b))
                return false;

            if (oa != null && of != null && oa instanceof PartFrame && of instanceof PartFrame && ((PartFrame) oa).isConnected(face))
                return false;

            if (ob != null && of != null && ob instanceof PartFrame && of instanceof PartFrame && ((PartFrame) ob).isConnected(face))
                return false;
        }

        return true;
    }

    public PartFrame() {

    }

    @Override
    public String getType() {

        return References.FRAME_NAME;
    }

    @Override
    public Iterable<Cuboid6> getOcclusionBoxes() {

        List<Cuboid6> boxes = new ArrayList<Cuboid6>();

        return boxes;
    }

    @Override
    public boolean occlusionTest(TMultiPart npart) {

        if (npart instanceof PartFrame)
            return false;

        return true;
    }

    @Override
    public Iterable<Cuboid6> getCollisionBoxes() {

        return Arrays.asList(new Cuboid6[] { new Cuboid6(0, 0, 0, 1, 1, 1) });
    }

    @Override
    public Iterable<IndexedCuboid6> getSubParts() {

        List<IndexedCuboid6> boxes = new ArrayList<IndexedCuboid6>();

        double translation = 0.0001;
        double t = 0.001;

        subParts[0] = new Cuboid6(0, 0 + t, 0, 1, translation + t, 1); // DOWN (-Y)
        subParts[1] = new Cuboid6(0, 1 - translation - t, 0, 1, 1 - t, 1); // UP (+Y)
        subParts[2] = new Cuboid6(0, 0, 0 + t, 1, 1, translation + t); // WEST (-Z)
        subParts[3] = new Cuboid6(0, 0, 1 - translation - t, 1, 1, 1 - t); // EAST (+Z)
        subParts[4] = new Cuboid6(0 + t, 0, 0, translation + t, 1, 1); // NORTH (-X)
        subParts[5] = new Cuboid6(1 - translation - t, 0, 0, 1 - t, 1, 1); // SOUTH (+X)

        if (tile() != null) {
            List<TMultiPart> parts = tile().jPartList();
            for (int i = 0; i < 6; i++) {
                IndexedCuboid6 c = new IndexedCuboid6(0, subParts[i]);
                boolean skip = false;
                for (TMultiPart p : parts) {
                    if (p instanceof HollowMicroblock && ((HollowMicroblock) p).getSlot() == i) {
                        skip = true;
                        break;
                    }
                }
                if (skip)
                    continue;
                boxes.add(c);
            }
        }

        return boxes;
    }

    @Override
    public boolean renderStatic(Vector3 pos, int pass) {

        if (pass != 0)
            return false;

        RenderFrame.render(this, x(), y(), z());

        return true;
    }

    @Override
    public boolean doesTick() {

        return false;
    }

    @Override
    public void addDestroyEffects(MovingObjectPosition hit, EffectRenderer effectRenderer) {

        IIcon icon = Blocks.planks.getIcon(0, 0);
        EntityDigIconFX.addBlockDestroyEffects(world(), Cuboid6.full.copy().add(Vector3.fromTileEntity(tile())), new IIcon[] { icon, icon, icon,
                icon, icon, icon }, effectRenderer);

    }

    @Override
    public void addHitEffects(MovingObjectPosition hit, EffectRenderer effectRenderer) {

        IIcon icon = Blocks.planks.getIcon(0, 0);
        EntityDigIconFX.addBlockHitEffects(world(), Cuboid6.full.copy().add(Vector3.fromTileEntity(tile())), hit.sideHit, icon, effectRenderer);
    }

    @Override
    public void drawBreaking(RenderBlocks renderBlocks) {

        RenderBlocks rb2 = RenderFrame.rb;
        RenderFrame.rb = renderBlocks;
        renderStatic(new Vector3(), 0);
        RenderFrame.rb = rb2;
    }

    @Override
    public boolean drawHighlight(MovingObjectPosition hit, EntityPlayer player, float frame) {

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(0, 0, 0, 0.4F);
        GL11.glLineWidth(2);
        GL11.glDepthMask(true);
        GL11.glPushMatrix();
        RenderUtils.translateToWorldCoords(player, frame);
        GL11.glTranslated(x(), y(), z());

        RenderUtils.drawCuboidOutline(new Cuboid6(0, 0, 0, 1, 1, 1).expand(0.001));

        GL11.glPopMatrix();
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);

        return true;
    }

    @Override
    public ExtendedMOP collisionRayTrace(Vec3 start, Vec3 end) {

        ExtendedMOP mop = super.collisionRayTrace(start, end);

        if (mop != null) {
            Vec3 click = mop.hitVec.addVector(-mop.blockX, -mop.blockY, -mop.blockZ);
            ForgeDirection face = ForgeDirection.getOrientation(mop.sideHit);
            if (face == ForgeDirection.EAST || face == ForgeDirection.WEST) {
                click.yCoord = 0.5;
                click.zCoord = 0.5;
                click.xCoord = (click.xCoord * 0.5) + 0.25;
            }
            if (face == ForgeDirection.UP || face == ForgeDirection.DOWN) {
                click.xCoord = 0.5;
                click.zCoord = 0.5;
                click.yCoord = (click.yCoord * 0.5) + 0.25;
            }
            if (face == ForgeDirection.NORTH || face == ForgeDirection.SOUTH) {
                click.xCoord = 0.5;
                click.yCoord = 0.5;
                click.zCoord = (click.zCoord * 0.5) + 0.25;
            }

            mop.hitVec = click.addVector(mop.blockX, mop.blockY, mop.blockZ);
        }

        return mop;
    }

    private ItemStack getItem() {

        List<String> modifiers = new ArrayList<String>();

        for (IFrameModifier m : this.modifiers)
            modifiers.add(m.getIdentifier());

        return FramezApi.inst().getModifierRegistry().getFrameStack(modifiers.toArray(new String[0]));
    }

    @Override
    public ItemStack pickItem(MovingObjectPosition hit) {

        return getItem();
    }

    @Override
    public Iterable<ItemStack> getDrops() {

        onUpdate(1);

        return Arrays.asList(new ItemStack[] { getItem() });
    }

    @Override
    public void save(NBTTagCompound tag) {

        super.save(tag);

        writeModifiersToNBT(tag);
    }

    @Override
    public void load(NBTTagCompound tag) {

        super.load(tag);

        try {
            readModifiersFromNBT(tag);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void writeDesc(MCDataOutput packet) {

        super.writeDesc(packet);

        NBTTagCompound tag = new NBTTagCompound();
        writeModifiersToNBT(tag);
        packet.writeNBTTagCompound(tag);
    }

    @Override
    public void readDesc(MCDataInput packet) {

        super.readDesc(packet);

        NBTTagCompound tag = packet.readNBTTagCompound();
        try {
            readModifiersFromNBT(tag);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            onUpdate(2);
        } catch (Exception ex) {
            // Not initialized yet
        }
    }

    @Override
    public float getStrength(MovingObjectPosition hit, EntityPlayer player) {

        ItemStack item = player.getCurrentEquippedItem();
        if (item != null)
            return item.getItem().getDigSpeed(item, Blocks.planks, 0);
        return 1;
    }

    @Override
    public void onAdded() {

        super.onAdded();

        onUpdate(0);
    }

    @Override
    public void onWorldJoin() {

        super.onWorldJoin();

        onUpdate(0);
    }

    @Override
    public void onRemoved() {

        super.onRemoved();

        onUpdate(1);
    }

    @Override
    public void onNeighborChanged() {

        super.onNeighborChanged();

        onUpdate(2);

        sendDescUpdate();
    }

    @Override
    public void onPartChanged(TMultiPart part) {

        super.onPartChanged(part);

        onUpdate(3);
    }

    private void onUpdate(int mode) {

        for (int i = 0; i < 6; i++) {
            ForgeDirection face = ForgeDirection.getOrientation(i);
            if (mode != 1)
                connections[i] = getObjectOnSide(face);

            if (mode != 2)
                if (connections[i] != null && connections[i] instanceof PartFrame) {
                    ((PartFrame) connections[i]).onUpdate(2);
                    for (Object o : ((PartFrame) connections[i]).connections) {
                        if (o != null && o instanceof PartFrame) {
                            ((PartFrame) o).onUpdate(2);
                        }
                    }
                }
        }

        updateRenderer();
    }

    private void updateRenderer() {

        render[0] = !hasModifier("connected")
                || (hasModifier("connected") && shouldRenderCorner(ForgeDirection.DOWN, ForgeDirection.NORTH, ForgeDirection.WEST));
        render[1] = !hasModifier("connected")
                || (hasModifier("connected") && !isConnected(ForgeDirection.NORTH) && !isConnected(ForgeDirection.DOWN));
        render[2] = !hasModifier("connected")
                || (hasModifier("connected") && shouldRenderCorner(ForgeDirection.DOWN, ForgeDirection.NORTH, ForgeDirection.EAST));
        render[3] = !hasModifier("connected") || (hasModifier("connected") && !isConnected(ForgeDirection.EAST) && !isConnected(ForgeDirection.DOWN));
        render[4] = !hasModifier("connected")
                || (hasModifier("connected") && shouldRenderCorner(ForgeDirection.DOWN, ForgeDirection.SOUTH, ForgeDirection.EAST));
        render[5] = !hasModifier("connected")
                || (hasModifier("connected") && !isConnected(ForgeDirection.SOUTH) && !isConnected(ForgeDirection.DOWN));
        render[6] = !hasModifier("connected")
                || (hasModifier("connected") && shouldRenderCorner(ForgeDirection.DOWN, ForgeDirection.SOUTH, ForgeDirection.WEST));
        render[7] = !hasModifier("connected") || (hasModifier("connected") && !isConnected(ForgeDirection.WEST) && !isConnected(ForgeDirection.DOWN));
        render[8] = !hasModifier("connected")
                || (hasModifier("connected") && shouldRenderCorner(ForgeDirection.UP, ForgeDirection.NORTH, ForgeDirection.WEST));
        render[9] = !hasModifier("connected") || (hasModifier("connected") && !isConnected(ForgeDirection.NORTH) && !isConnected(ForgeDirection.UP));
        render[10] = !hasModifier("connected")
                || (hasModifier("connected") && shouldRenderCorner(ForgeDirection.UP, ForgeDirection.NORTH, ForgeDirection.EAST));
        render[11] = !hasModifier("connected") || (hasModifier("connected") && !isConnected(ForgeDirection.EAST) && !isConnected(ForgeDirection.UP));
        render[12] = !hasModifier("connected")
                || (hasModifier("connected") && shouldRenderCorner(ForgeDirection.UP, ForgeDirection.SOUTH, ForgeDirection.EAST));
        render[13] = !hasModifier("connected") || (hasModifier("connected") && !isConnected(ForgeDirection.SOUTH) && !isConnected(ForgeDirection.UP));
        render[14] = !hasModifier("connected")
                || (hasModifier("connected") && shouldRenderCorner(ForgeDirection.UP, ForgeDirection.SOUTH, ForgeDirection.WEST));
        render[15] = !hasModifier("connected") || (hasModifier("connected") && !isConnected(ForgeDirection.WEST) && !isConnected(ForgeDirection.UP));
        render[16] = !hasModifier("connected")
                || (hasModifier("connected") && shouldRenderCorner(ForgeDirection.UNKNOWN, ForgeDirection.NORTH, ForgeDirection.WEST));
        render[17] = !hasModifier("connected")
                || (hasModifier("connected") && shouldRenderCorner(ForgeDirection.UNKNOWN, ForgeDirection.NORTH, ForgeDirection.EAST));
        render[18] = !hasModifier("connected")
                || (hasModifier("connected") && shouldRenderCorner(ForgeDirection.UNKNOWN, ForgeDirection.SOUTH, ForgeDirection.EAST));
        render[19] = !hasModifier("connected")
                || (hasModifier("connected") && shouldRenderCorner(ForgeDirection.UNKNOWN, ForgeDirection.SOUTH, ForgeDirection.WEST));
    }

    private Object getObjectOnSide(ForgeDirection face) {

        int x = x() + face.offsetX;
        int y = y() + face.offsetY;
        int z = z() + face.offsetZ;

        int mbThis = Utils.getMicroblockSize(tile(), face);
        if (mbThis > 0)
            return mbThis;

        int mbOther = Utils.getMicroblockSize(Utils.getMultipartTile(world(), x, y, z), face.getOpposite());
        if (mbOther > 0)
            return mbOther;

        IFrame f = Utils.getFrame(world(), x, y, z);
        if (f != null)
            return f;

        if (world().getBlock(x, y, z).isSideSolid(world(), x, y, z, face.getOpposite()))
            return true;

        return null;
    }

    @Override
    public IFrameModifier[] getModifiers() {

        return modifiers.toArray(new IFrameModifier[0]);
    }

    @Override
    public void addModifier(IFrameModifier modifier) {

        if (modifier == null)
            return;
        for (IFrameModifier m : getModifiers())
            if (m.getIdentifier().equals(modifier.getIdentifier()))
                return;

        modifiers.add(modifier);
    }

    @Override
    public void addModifiers(IFrameModifier... modifiers) {

        for (IFrameModifier m : modifiers)
            addModifier(m);
    }

    @Override
    public void removeModifier(String modifier) {

        IFrameModifier mod = null;

        for (IFrameModifier m : getModifiers())
            if (m.getIdentifier().equals(modifier)) {
                mod = m;
                break;
            }

        if (mod != null)
            modifiers.remove(mod);
    }

    @Override
    public boolean hasModifier(String modifier) {

        for (IFrameModifier m : getModifiers())
            if (m.getIdentifier().equals(modifier))
                return true;
        return false;
    }

    @Override
    public void readModifiersFromNBT(NBTTagCompound tag) {

        List<IFrameModifier> unedited = new ArrayList<IFrameModifier>();
        unedited.addAll(Arrays.asList(getModifiers()));

        NBTTagList list = tag.getTagList("modifiers", 10);// List of tag compounds

        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound t = list.getCompoundTagAt(i);
            String type = t.getString("__type");
            IFrameModifier modifier = null;
            for (IFrameModifier m : unedited) {
                if (m == null)
                    continue;
                if (m.getIdentifier().equals(type)) {
                    modifier = m;
                    break;
                }
            }

            if (modifier == null) {
                for (IFrameModifierProvider p : FramezApi.inst().getModifierRegistry().getProviders()) {
                    if (p.getIdentifier().equals(type)) {
                        modifier = p.instantiate(this);
                        break;
                    }
                }
                addModifier(modifier);
            } else {
                unedited.remove(modifier);
            }

            modifier.readFromNBT(t);
        }

        modifiers.removeAll(unedited);
        unedited.clear();
    }

    @Override
    public void writeModifiersToNBT(NBTTagCompound tag) {

        NBTTagList list = new NBTTagList();

        for (IFrameModifier m : getModifiers()) {
            NBTTagCompound t = new NBTTagCompound();
            m.writeToNBT(t);
            t.setString("__type", m.getIdentifier());
            list.appendTag(t);
        }

        tag.setTag("modifiers", list);
    }

    @Override
    public Object[] getConnections() {

        return connections;
    }

    public boolean[] getRender() {

        return render;
    }
}
