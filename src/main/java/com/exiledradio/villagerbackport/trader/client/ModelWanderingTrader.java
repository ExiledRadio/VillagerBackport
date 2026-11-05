package com.exiledradio.villagerbackport.trader.client;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelVillager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.14's villager model, which the wandering trader uses: 1.12.2's villager plus the two boxes 1.14
 * added to the head.
 *
 * <p>The trader's hood is drawn on the second, slightly larger head box at texture offset 32,0, and
 * its brim on the flat 16x16 sheet at 30,47 - neither of which the 1.12.2 model has. They are added as
 * children of the head here, so they follow it through every pose without any copying of angles.
 * Box for box the same as this mod's villager hat layer, and 1.14's {@code VillagerModel}.
 */
@SideOnly(Side.CLIENT)
public class ModelWanderingTrader extends ModelVillager {

    public ModelWanderingTrader() {
        super(0.0F, 0.0F, 64, 64);

        ModelRenderer hat = new ModelRenderer(this).setTextureSize(64, 64);
        hat.setTextureOffset(32, 0).addBox(-4.0F, -10.0F, -4.0F, 8, 10, 8, 0.5F);

        ModelRenderer rim = new ModelRenderer(this).setTextureSize(64, 64);
        rim.setTextureOffset(30, 47).addBox(-8.0F, -8.0F, -6.0F, 16, 16, 1, 0.0F);
        rim.rotateAngleX = (float) -Math.PI / 2.0F;
        hat.addChild(rim);

        this.villagerHead.addChild(hat);
    }
}
