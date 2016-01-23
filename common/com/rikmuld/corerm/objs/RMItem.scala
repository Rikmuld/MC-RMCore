package com.rikmuld.corerm.objs

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.EnumDyeColor
import net.minecraft.creativetab.CreativeTabs
import java.util.List
import net.minecraft.item.ItemBlock
import net.minecraft.block.Block
import net.minecraftforge.fml.relauncher.SideOnly
import net.minecraftforge.fml.relauncher.Side
import net.minecraft.item.ItemFood
import net.minecraft.item.ItemArmor.ArmorMaterial
import net.minecraft.item.ItemArmor
import com.rikmuld.corerm.objs.PropType._
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World
import com.rikmuld.corerm.RMMod

class RMItem(val modId:String, info:ObjInfo) extends RMCoreItem {
  info.register(this, modId)
  
  def getItemInfo:ObjInfo = info
}

class RMItemBlock(val modId:String, info:ObjInfo, block:Block) extends ItemBlock(block) with RMCoreItem {
  def getItemInfo:ObjInfo = info
}

class RMItemFood(val modId:String, info:ObjInfo) extends ItemFood(info.getValue(HEAL), info.getValue(SATURATION), info.getValue(WOLFMEAT)) with RMCoreItem {
  info.register(this, modId) 
  
  def getItemInfo:ObjInfo = info
}

class RMItemArmor(val modId:String, info:ObjInfo) extends ItemArmor(info.getValue(ARMORMAT), 0, info.getValue(ARMORTYPE))  with RMCoreItem {
  info.register(this, modId)  
  maxStackSize = 1

  def getItemInfo:ObjInfo = info

  override def getArmorTexture(stack: ItemStack, entity: Entity, slot: Int, layer: String): String = getItemInfo.getValue(ARMORTEX)
}

abstract trait RMCoreItem extends Item {  
  getItemInfo.apply(this)
  
  val hasMeta = getItemInfo.hasProp(METADATA)
  val meta = if(!hasMeta) null else getItemInfo.getValue[Array[String]](METADATA)
  if(getItemInfo.hasProp(FORCESUBTYPE))setHasSubtypes(getItemInfo.getValue(FORCESUBTYPE))
  else if(getItemInfo.hasProp(METADATA))setHasSubtypes(true)
    
  def getItemInfo:ObjInfo
  
  override def getMetadata(damageValue: Int): Int = if(hasMeta) damageValue else 0
  override def getUnlocalizedName(stack:ItemStack):String = if(!getItemInfo.hasProp(METADATA)) getUnlocalizedName else "item." + getItemInfo.getValue[String](NAME) + "_" + meta(stack.getMetadata)
  @SideOnly(Side.CLIENT)
  override def getSubItems(itemIn:Item, tab:CreativeTabs, subItems:List[_]) {
    if(hasMeta){
      for(i <- 0 until meta.length) subItems.asInstanceOf[List[ItemStack]].add(new ItemStack(itemIn, 1, i)) 
    } else subItems.asInstanceOf[List[ItemStack]].add(new ItemStack(itemIn, 1, 0))
  }
  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer): ItemStack = {
    if (!world.isRemote&&getItemInfo.hasProp(GUITRIGGER)) {
      val gui = getItemInfo.getProp[Properties.GuiTrigger](GUITRIGGER)
      player.openGui(RMMod, gui.value.asInstanceOf[Int], world, 0, 0, 0)
    } 
    super.onItemRightClick(stack, world, player)
  }
}