package com.amadornes.framez.movement;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;

import com.amadornes.framez.api.movement.IMovingBlock;
import com.amadornes.framez.init.FramezBlocks;
import com.amadornes.framez.tile.TileMoving;
import com.amadornes.framez.util.BlockUtils;

public class MovingBlock implements IMovingBlock {

    private World world;
    private BlockCoord loc;

    private Block block;
    private int meta;
    private TileEntity te;

    private MovingStructure structure;

    private TileMoving placeholder = null;

    private boolean isStored = false;

    public MovingBlock(BlockCoord location, World world, MovingStructure structure) {

        loc = location;
        this.world = world;
        this.structure = structure;
    }

    public MovingBlock(int x, int y, int z, World world, MovingStructure structure) {

        loc = new BlockCoord(x, y, z);
        this.world = world;
        this.structure = structure;
    }

    @Override
    public World getWorld() {

        return world;
    }

    @Override
    public int getX() {

        return getLocation().x;
    }

    @Override
    public int getY() {

        return getLocation().y;
    }

    @Override
    public int getZ() {

        return getLocation().z;
    }

    public BlockCoord getLocation() {

        return loc;
    }

    @Override
    public Block getBlock() {

        return block;
    }

    @Override
    public int getMetadata() {

        return meta;
    }

    @Override
    public TileEntity getTileEntity() {

        return te;
    }

    @Override
    public ForgeDirection getDirection() {

        return structure.getDirection();
    }

    @Override
    public double getMoved() {

        return structure.getMoved();
    }

    @Override
    public double getSpeed() {

        return structure.getSpeed();
    }

    @Override
    public World getWorldWrapper() {

        return structure.getWorldWrapper();
    }

    public MovingStructure getStructure() {

        return structure;
    }

    @Override
    public void setBlock(Block block) {

        this.block = block;
    }

    @Override
    public void setMetadata(int meta) {

        this.meta = meta;
    }

    @Override
    public void setTileEntity(TileEntity te) {

        this.te = te;
    }

    public void storeData() {

        te = world.getTileEntity(loc.x, loc.y, loc.z);
        block = world.getBlock(loc.x, loc.y, loc.z);
        meta = world.getBlockMetadata(loc.x, loc.y, loc.z);
    }

    public void remove() {

        if (!MovementApi.INST.handleRemoval(this))
            remove_do(true, true);

        isStored = true;
    }

    public void place() {

        if (!MovementApi.INST.handlePlacement(this))
            place_do(true, true);

        isStored = false;
    }

    @Override
    public void remove_do(boolean invalidate, boolean validate) {

        int x = getX();
        int y = getY();
        int z = getZ();
        TileEntity te = getTileEntity();

        if (te != null) {
            if (te instanceof TileMultipart) {
                if (!getWorld().isRemote) {
                    for (TMultiPart p : ((TileMultipart) te).jPartList())
                        p.onWorldSeparate();
                }
            }
            if (invalidate)
                te.invalidate();

        }
        BlockUtils.setBlockSneaky(getWorld(), x, y, z, Blocks.air);
        if (te != null) {
            BlockUtils.removeTileEntity(getWorld(), x, y, z);

            te.setWorldObj(getWorldWrapper());

            if (validate && !(te instanceof TileMultipart))
                te.validate();
            if (te instanceof TileMultipart && !getWorld().isRemote)
                for (TMultiPart p : ((TileMultipart) te).jPartList())
                    p.onMoved();
        }
    }

    @Override
    public void place_do(boolean invalidate, boolean validate) {

        int x = getX() + getDirection().offsetX;
        int y = getY() + getDirection().offsetY;
        int z = getZ() + getDirection().offsetZ;
        TileEntity te = getTileEntity();

        BlockUtils.setBlockSneaky(world, x, y, z, getBlock());
        BlockUtils.setBlockMetadataSneaky(world, x, y, z, getMetadata());

        if (te != null) {
            if (te instanceof TileMultipart) {
                if (!getWorld().isRemote) {
                    TileMultipart tmp = TileMultipart.getTile(getWorld(), new BlockCoord(x, y, z));
                    if (tmp != null)
                        tmp.clearParts();
                    for (TMultiPart p : ((TileMultipart) te).jPartList()) {
                        p.onWorldSeparate();
                        TileMultipart.addPart(getWorld(), new BlockCoord(x, y, z), p);
                        p.onMoved();
                    }
                }
            } else {
                if (invalidate && !(te instanceof TileMultipart))
                    te.invalidate();

                te.xCoord = x;
                te.yCoord = y;
                te.zCoord = z;
                te.setWorldObj(getWorldWrapper());

                if (validate)
                    te.validate();

                if (getWorld().getTileEntity(x, y, z) != te)
                    getWorld().setTileEntity(x, y, z, te);
            }
        }

    }

    public void removePlaceholder() {

        if (placeholder == null)
            return;

        placeholder.setBlockA(null);
        MovingBlock b = structure.getBlock(loc.x + getDirection().offsetX, loc.y + getDirection().offsetY, loc.z + getDirection().offsetZ);
        if (b != null) {
            b.placeholder.setBlockB(null);
        }
    }

    public void placePlaceholder() {

        world.setBlock(loc.x, loc.y, loc.z, FramezBlocks.moving, 0, 0);
        TileMoving te = null;
        if (placeholder != null)
            te = placeholder;
        else
            te = placeholder = new TileMoving();
        te.setBlockA(this);
        world.setTileEntity(loc.x, loc.y, loc.z, te);

        TileMoving te2 = null;
        MovingBlock b = structure.getBlock(loc.x + getDirection().offsetX, loc.y + getDirection().offsetY, loc.z + getDirection().offsetZ);
        if (b != null) {
            if (b.placeholder == null)
                te2 = b.placeholder = new TileMoving();
            else
                te2 = b.placeholder;
        } else {
            world.setBlock(loc.x + getDirection().offsetX, loc.y + getDirection().offsetY, loc.z + getDirection().offsetZ,
                    FramezBlocks.moving, 0, 0);
            world.setTileEntity(loc.x + getDirection().offsetX, loc.y + getDirection().offsetY, loc.z + getDirection().offsetZ,
                    te2 = new TileMoving());
        }
        te2.setBlockB(this);
    }

    public TileMoving getPlaceholder() {

        return placeholder;
    }

    public boolean isStored() {

        return isStored;
    }

    private int renderList = -1;

    public int getRenderList() {

        return renderList;
    }

    public void setRenderList(int renderList) {

        this.renderList = renderList;
    }

}
