package com.exiledradio.villagerbackport.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A villager workstation: a plain full cube that does nothing but offer a job.
 *
 * <h2>One class, twelve blocks</h2>
 * 1.14's workstations are twelve separate classes because most of them do something - a barrel
 * opens, a composter fills, a bell rings. None of that is here yet: for now these exist to be
 * placed, to look right, and to give a villager a job, which is the same behaviour twelve times
 * over. Writing it once means the differences stay in the data, where they are easy to change.
 *
 * <h2>Why the variants are subclasses and not constructor flags</h2>
 * They were flags, and it crashed on startup. {@link Block}'s constructor calls
 * {@code createBlockState()} and {@code isOpaqueCube()} on the way up - before the subclass
 * constructor body has run, so before any field it sets has a value. A {@code directional} flag read
 * during that window is always false, so the state container came back empty and setting the facing
 * property on it threw.
 *
 * <p>The same trap had already produced a quieter bug: {@code isOpaqueCube} reading a shape field
 * that was still null meant every block was treated as a solid cube for lighting, which would have
 * had lecterns casting a full block's shadow.
 *
 * <p>Answering both questions by which class it is removes the timing question entirely - an
 * overridden method is correct from the first call, whereas a field is only correct after the
 * constructor that sets it has run.
 */
public class BlockWorkstation extends Block {

    public static final PropertyDirection FACING =
            PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);

    private final boolean cutout;

    public BlockWorkstation(Material material, SoundType sound, float hardness) {
        this(material, sound, hardness, false);
    }

    /**
     * @param cutout true if the block's textures have transparent areas that must be discarded
     *               rather than blended
     */
    public BlockWorkstation(Material material, SoundType sound, float hardness, boolean cutout) {
        super(material);

        this.cutout = cutout;

        setHardness(hardness);
        setResistance(hardness * 5.0F);
        setSoundType(sound);
        setCreativeTab(CreativeTabs.DECORATIONS);
    }

    /**
     * Which pass this block is drawn in.
     *
     * <p>The default, {@code SOLID}, ignores alpha entirely - every texel is drawn opaque. That is
     * why a stonecutter's saw came out as a solid white slab: the blade is a flat plane whose
     * texture is mostly transparent, and drawn solid it becomes a rectangle. The same applies to the
     * grindstone's wheel, the composter's interior and the bell.
     *
     * <p>{@code CUTOUT} discards transparent texels outright, which is the right choice for textures
     * that are either fully opaque or fully clear. Translucent blending would also work but costs a
     * sorted pass and is for things like stained glass, which none of these are.
     *
     * <p>Deliberately not {@code CUTOUT_MIPPED}. Mipmapping averages neighbouring texels as it builds
     * each smaller level, so an opaque texel next to a transparent one becomes semi-transparent - and
     * a thin seam appears along every edge where the two meet. That showed up as faint outlines all
     * over the inside of a composter. Unmipped, the alpha stays binary and the seams cannot form; the
     * cost is slightly noisier texture sampling at distance, which is the better trade here.
     *
     * <p>This is safe to read here, unlike the constructor-time questions on the subclasses: nothing
     * asks for a render layer until the block is actually being drawn.
     */
    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return this.cutout ? BlockRenderLayer.CUTOUT : BlockRenderLayer.SOLID;
    }
}
