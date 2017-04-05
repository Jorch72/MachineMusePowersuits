package net.machinemuse.powersuits.powermodule.tool;

import net.machinemuse.api.IModularItem;
import net.machinemuse.api.ModuleManager;
import net.machinemuse.api.moduletrigger.IRightClickModule;
import net.machinemuse.powersuits.powermodule.PowerModuleBase;
import net.machinemuse.utils.ElectricItemUtils;
import net.machinemuse.utils.MuseCommonStrings;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by User: Andrew2448
 * 7:45 PM 4/23/13
 *
 * Updated by leon on 6/14/16.
 */

public class TreetapModule extends PowerModuleBase implements IRightClickModule {
    public static final String MODULE_TREETAP = "Treetap";
    public static final String TREETAP_ENERGY_CONSUMPTION = "Energy Consumption";
    public static final ItemStack resin = new ItemStack( Item.REGISTRY.getObject(new ResourceLocation("ic2", "misc_resource")), 1, 4);
    public static final Block rubber_wood =  Block.REGISTRY.getObject(new ResourceLocation("ic2", "rubber_wood"));
    public static final ItemStack emulatedTool = new ItemStack( Item.REGISTRY.getObject(new ResourceLocation("ic2", "electric_treetap")), 1);
    public static final ItemStack treetap  = new ItemStack( Item.REGISTRY.getObject(new ResourceLocation("ic2", "treetap")), 1);

    public TreetapModule(List<IModularItem> validItems) {
        super(validItems);
        addBaseProperty(TREETAP_ENERGY_CONSUMPTION, 100);
        addInstallCost(emulatedTool);
    }

    @Override
    public ActionResult onItemRightClick(ItemStack itemStackIn, World worldIn, EntityPlayer playerIn, EnumHand hand) {
        return ActionResult.newResult(EnumActionResult.PASS, itemStackIn);
    }

    @Override
    public EnumActionResult onItemUse(ItemStack itemStack, EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        try {
            // This is kinda ugly. I really hate using reflection since changes can throw errors.

            Method attemptExtract = treetap.getItem().getClass().
                    getDeclaredMethod("attemptExtract",EntityPlayer.class , World.class,
                                        BlockPos.class, EnumFacing.class, IBlockState.class, List.class);

            if (block == rubber_wood && ModuleManager.computeModularProperty(itemStack, TREETAP_ENERGY_CONSUMPTION) < ElectricItemUtils.getPlayerEnergy(player)) {
                if (attemptExtract.invoke( "attemptExtract", player, world, pos, facing, state, null).equals(true)) {
                    ElectricItemUtils.drainPlayerEnergy(player, ModuleManager.computeModularProperty(itemStack, TREETAP_ENERGY_CONSUMPTION));
                    return EnumActionResult.SUCCESS;
                }
            }
            return EnumActionResult.PASS;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return EnumActionResult.FAIL;
    }

    @Override
    public EnumActionResult onItemUseFirst(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
        return EnumActionResult.PASS;
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World worldIn, EntityLivingBase entityLiving, int timeLeft) {

    }

    @Override
    public String getCategory() {
        return MuseCommonStrings.CATEGORY_TOOL;
    }

    @Override
    public String getDataName() {
        return MODULE_TREETAP;
    }

    @Override
    public String getUnlocalizedName() {
        return "treetap";
    }

    public static void eject(World world, BlockPos pos, EnumFacing side, int quantity) {
        double ejectX = (double)pos.getX() + 0.5D + (double)side.getFrontOffsetX() * 0.3D;
        double ejectY = (double)pos.getY() + 0.5D + (double)side.getFrontOffsetY() * 0.3D;
        double ejectZ = (double)pos.getZ() + 0.5D + (double)side.getFrontOffsetZ() * 0.3D;

        for(int i = 0; i < quantity; ++i) {
            EntityItem item = new EntityItem(world, ejectX, ejectY, ejectZ, resin.copy());
            item.setDefaultPickupDelay();
            world.spawnEntityInWorld(item);
        }
    }

    @Override
    public TextureAtlasSprite getIcon(ItemStack item) {
        return Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getItemModel(emulatedTool).getParticleTexture();
    }
}