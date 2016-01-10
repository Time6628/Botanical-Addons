package ninja.shadowfox.shadowfox_botany.common.item.baubles

import baubles.api.BaubleType
import baubles.common.lib.PlayerHandler
import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ItemRenderer
import net.minecraft.client.renderer.RenderBlocks
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.MathHelper
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import ninja.shadowfox.shadowfox_botany.common.core.ShadowFoxCreativeTab
import org.lwjgl.opengl.GL11
import vazkii.botania.client.core.handler.ClientTickHandler
import vazkii.botania.client.lib.LibResources
import vazkii.botania.common.core.helper.ItemNBTHelper
import vazkii.botania.common.item.equipment.bauble.ItemBauble
import java.awt.Color
import kotlin.text.replace
import kotlin.text.toRegex

class ItemToolbelt() : ItemBauble("toolbelt") {
    companion object {
        val glowTexture = ResourceLocation(LibResources.MISC_GLOW_GREEN)

        val SEGMENTS = 12

        val TAG_ITEM_PREFIX = "item"
        val TAG_EQUIPPED = "equipped"
        val TAG_ROTATION_BASE = "rotationBase"

        fun isEquipped(stack: ItemStack): Boolean = ItemNBTHelper.getBoolean(stack, TAG_EQUIPPED, false)
        fun setEquipped(stack: ItemStack, equipped: Boolean) = ItemNBTHelper.setBoolean(stack, TAG_EQUIPPED, equipped)
        fun getRotationBase(stack: ItemStack): Float = ItemNBTHelper.getFloat(stack, TAG_ROTATION_BASE, 0F)
        fun setRotationBase(stack: ItemStack, rotation: Float) = ItemNBTHelper.setFloat(stack, TAG_ROTATION_BASE, rotation)

        fun getSegmentLookedAt(stack: ItemStack, player: EntityLivingBase): Int {
            val yaw = getCheckingAngle(player, getRotationBase(stack))

            val angles = 360
            val segAngles = angles / SEGMENTS
            for (seg in 0..SEGMENTS - 1) {
                val calcAngle = seg.toFloat() * segAngles
                if (yaw >= calcAngle && yaw < calcAngle + segAngles)
                    return seg
            }
            return -1
        }

        fun getCheckingAngle(player: EntityLivingBase): Float = getCheckingAngle(player, 0F)

        // Agreed, V, minecraft's rotation is shit. And no roll? Seriously?
        fun getCheckingAngle(player: EntityLivingBase, base: Float): Float {
            var yaw = MathHelper.wrapAngleTo180_float(player.rotationYaw) + 90F
            val angles = 360
            val segAngles = angles / SEGMENTS
            val shift = segAngles / 2

            if (yaw < 0)
                yaw = 180F + (180F + yaw)
            yaw -= 360F - base
            var angle = 360F - yaw + shift

            if (angle < 0)
                angle += 360F

            return angle
        }

        fun getItemForSlot(stack: ItemStack, slot: Int): ItemStack? {
            if (slot >= SEGMENTS) return null
            else {
                val cmp = getStoredCompound(stack, slot) ?: return null
                return ItemStack.loadItemStackFromNBT(cmp)
            }
        }

        fun getStoredCompound(stack: ItemStack, slot: Int): NBTTagCompound? = ItemNBTHelper.getCompound(stack, TAG_ITEM_PREFIX + slot, true)
        fun setItem(beltStack: ItemStack, stack: ItemStack?, pos: Int) {
            if (stack == null) ItemNBTHelper.setCompound(beltStack, TAG_ITEM_PREFIX + pos, NBTTagCompound())
            else {
                var tag = NBTTagCompound()
                stack.writeToNBT(tag)
                ItemNBTHelper.setCompound(beltStack, TAG_ITEM_PREFIX + pos, tag)
            }
        }
    }

    init {
        MinecraftForge.EVENT_BUS.register(this)
        FMLCommonHandler.instance().bus().register(this)
        setHasSubtypes(true)
        setCreativeTab(ShadowFoxCreativeTab)
    }

    @SubscribeEvent
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.entityPlayer
        val inv = PlayerHandler.getPlayerBaubles(player)
        var beltStack: ItemStack? = null
        for (i in 0..inv.sizeInventory) {
            var stack = inv.getStackInSlot(i)
            if (stack != null && stack.item == this) {
                beltStack = stack
            }
        }
        var heldItem = player.currentEquippedItem
        if (beltStack != null && isEquipped(beltStack)) {
            if (event.action === PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK || event.action === PlayerInteractEvent.Action.RIGHT_CLICK_AIR) {
                val segment = getSegmentLookedAt(beltStack, player)
                val toolStack = getItemForSlot(beltStack, segment)
                if (toolStack == null && heldItem != null) {
                    setItem(beltStack, heldItem.copy(), segment)
                    player.setCurrentItemOrArmor(0, null)
                    println("added " + heldItem.toString())
                } else if (toolStack != null) {
                    if (!player.inventory.addItemStackToInventory(toolStack.copy())) {
                        player.dropPlayerItemWithRandomChoice(toolStack.copy(), false)
                    }
                    println("removed " + toolStack.toString())
                    setItem(beltStack, null, segment)
                }
                event.isCanceled = true
            }
        }
    }

    override fun getBaubleType(stack: ItemStack): BaubleType {
        return BaubleType.BELT
    }

    override fun onWornTick(stack: ItemStack, player: EntityLivingBase) {
        if (player is EntityPlayer) {
            val eqLastTick = isEquipped(stack)
            val sneak = player.isSneaking
            if (eqLastTick != sneak)
                setEquipped(stack, sneak)

            if (!sneak) {
                val angles = 360
                val segAngles = angles / SEGMENTS
                val shift = segAngles / 2
                setRotationBase(stack, getCheckingAngle(player) - shift)
            }
        }
    }

    override fun getUnlocalizedNameInefficiently(par1ItemStack: ItemStack): String {
        return super.getUnlocalizedNameInefficiently(par1ItemStack).replace("item\\.botania:".toRegex(), "item.shadowfox_botany:")
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    fun onRenderWorldLast(event: RenderWorldLastEvent) {
        val player = Minecraft.getMinecraft().thePlayer
        val inv = PlayerHandler.getPlayerBaubles(player)
        var beltStack: ItemStack? = null
        for (i in 0..inv.sizeInventory) {
            var stack = inv.getStackInSlot(i)
            if (stack != null && stack.item == this) {
                beltStack = stack
            }
        }
        if (beltStack != null && isEquipped(beltStack))
            render(beltStack, player, event.partialTicks)
    }

    @SideOnly(Side.CLIENT)
    fun render(stack: ItemStack, player: EntityPlayer, partialTicks: Float) {
        val mc = Minecraft.getMinecraft()
        val tess = Tessellator.instance
        Tessellator.renderingWorldRenderer = false

        GL11.glPushMatrix()
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        val alpha = (Math.sin((ClientTickHandler.ticksInGame + partialTicks).toDouble() * 0.2) * 0.5F + 0.5F) * 0.4F + 0.3F

        val posX = player.prevPosX + (player.posX - player.prevPosX) * partialTicks
        val posY = player.prevPosY + (player.posY - player.prevPosY) * partialTicks
        val posZ = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks

        GL11.glTranslated(posX - RenderManager.renderPosX, posY - RenderManager.renderPosY, posZ - RenderManager.renderPosZ)


        val base = getRotationBase(stack)
        val angles = 360
        val segAngles = angles / SEGMENTS
        val shift = base - segAngles / 2

        val u = 1F
        val v = 0.25F

        val s = 3F
        val m = 0.8F
        val y = v * s * 2
        var y0 = 0.0

        val segmentLookedAt = getSegmentLookedAt(stack, player)

        for (seg in 0..SEGMENTS - 1) {
            var inside = false
            var rotationAngle = (seg + 0.5F) * segAngles + shift
            GL11.glPushMatrix()
            GL11.glRotatef(rotationAngle, 0F, 1F, 0F)
            GL11.glTranslatef(s * m, -0.75F, 0F)

            if (segmentLookedAt == seg)
                inside = true

            val slotStack = getItemForSlot(stack, seg)
            if (slotStack != null) {
                mc.renderEngine.bindTexture(if (slotStack.item is ItemBlock) TextureMap.locationBlocksTexture else TextureMap.locationItemsTexture)

                if (slotStack.item is ItemBlock && RenderBlocks.renderItemIn3d(Block.getBlockFromItem(slotStack.item).renderType)) {
                    GL11.glScalef(0.6F, 0.6F, 0.6F)
                    GL11.glRotatef(180F, 0F, 1F, 0F)
                    GL11.glTranslatef(if (seg == 0) 0.5F else 0F, if (seg == 0) -0.1F else 0.6F, 0F)

                    RenderBlocks.getInstance().renderBlockAsItem(Block.getBlockFromItem(slotStack.getItem()), slotStack.getItemDamage(), 1F)
                } else {
                    GL11.glScalef(0.75F, 0.75F, 0.75F)
                    GL11.glTranslatef(0F, 0F, 0.5F)
                    GL11.glRotatef(90F, 0F, 1F, 0F)
                    var renderPass = 0
                    while (renderPass < slotStack.item.getRenderPasses(slotStack.itemDamage)) {
                        val icon = slotStack.item.getIcon(slotStack, renderPass)
                        if (icon != null) {
                            val color = Color(slotStack.item.getColorFromItemStack(slotStack, renderPass))
                            GL11.glColor3ub(color.red.toByte(), color.green.toByte(), color.blue.toByte())
                            val f = icon.minU
                            val f1 = icon.maxU
                            val f2 = icon.minV
                            val f3 = icon.maxV
                            ItemRenderer.renderItemIn2D(Tessellator.instance, f1, f2, f, f3, icon.iconWidth, icon.iconHeight, 1F / 16F)
                            GL11.glColor3f(1F, 1F, 1F)
                        }
                        renderPass++
                    }
                }
            }
            GL11.glPopMatrix()

            GL11.glPushMatrix()
            GL11.glRotatef(180F, 1F, 0F, 0F)
            var a = alpha.toFloat()
            if (inside) {
                a += 0.3F
                y0 = -y.toDouble()
            }

            if (seg % 2 == 0)
                GL11.glColor4f(0.6F, 0.6F, 0.6F, a)
            else GL11.glColor4f(1F, 1F, 1F, a)

            GL11.glDisable(GL11.GL_CULL_FACE)
            val item = stack.item as ItemToolbelt
            mc.renderEngine.bindTexture(item.getGlowResource())
            tess.startDrawingQuads()
            for (i in 0..segAngles - 1) {
                val ang = i + seg * segAngles + shift
                var xp = Math.cos(ang * Math.PI / 180F) * s
                var zp = Math.sin(ang * Math.PI / 180F) * s

                tess.addVertexWithUV(xp * m, y.toDouble(), zp * m, u.toDouble(), v.toDouble())
                tess.addVertexWithUV(xp, y0, zp, u.toDouble(), 0.0)

                xp = Math.cos((ang + 1) * Math.PI / 180F) * s
                zp = Math.sin((ang + 1) * Math.PI / 180F) * s

                tess.addVertexWithUV(xp, y0, zp, 0.0, 0.0)
                tess.addVertexWithUV(xp * m, y.toDouble(), zp * m, 0.0, v.toDouble())
            }
            y0 = 0.0
            tess.draw()
            GL11.glEnable(GL11.GL_CULL_FACE)
            GL11.glPopMatrix()
        }
        GL11.glPopMatrix()
    }

    @SideOnly(Side.CLIENT)
    fun getGlowResource(): ResourceLocation = glowTexture
}