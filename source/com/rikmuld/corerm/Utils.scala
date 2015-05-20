package com.rikmuld.corerm

import java.util.ArrayList
import java.util.Random
import net.minecraft.block.Block
import net.minecraft.entity.EntityList
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.Slot
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.util.MathHelper
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minecraft.world.World
import net.minecraft.world.storage.MapData
import net.minecraftforge.oredict.OreDictionary
import scala.collection.JavaConversions._
import net.minecraftforge.common.util.Constants
import scala.collection.mutable.WrappedArray
import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag
import net.minecraft.util.EnumFacing
import net.minecraft.util.BlockPos

object CoreUtils {
  var startEntityId = 300

  def getUniqueEntityId = {
    while (EntityList.getStringFromID(startEntityId) != null) startEntityId += 1
    startEntityId
  }
  def nwsk(item:Item) = new ItemStack(item)
  def nwsk(item:Block) = new ItemStack(item)
  def nwsk(item:Item, meta:Int) = new ItemStack(item, 1, meta)
  def nwsk(item:Block, meta:Int) = new ItemStack(item, 1, meta)
  def nwsk(item:Item, size:Int, meta:Int) = new ItemStack(item, size, meta)
  def nwsk(item:Block, size:Int, meta:Int) = new ItemStack(item, size, meta)
  implicit class ItemUtils(obj: AnyRef) {
    def toStack(): ItemStack = {
      var stack: ItemStack = null
      if (obj.isInstanceOf[Block]) stack = new ItemStack(obj.asInstanceOf[Block])
      else if (obj.isInstanceOf[Item]) stack = new ItemStack(obj.asInstanceOf[Item])
      else if (obj.isInstanceOf[ItemStack]) stack = obj.asInstanceOf[ItemStack]
      stack
    }
    def toStack(count: Int): ItemStack = {
      val stack = toStack
      stack.stackSize = count
      stack
    }
    def getMetaCycle(maxMetadata: Int): Array[ItemStack] = {
      val stack = Array.ofDim[ItemStack](maxMetadata)
      for (i <- 0 until maxMetadata) {
        if (obj.isInstanceOf[Block]) stack(i) = new ItemStack(obj.asInstanceOf[Block], 1, i)
        if (obj.isInstanceOf[Item]) stack(i) = new ItemStack(obj.asInstanceOf[Item], 1, i)
      }
      stack
    }
  }
  implicit class ContainerUtils(container: Container) {
    def addSlot(slot: Slot) = {
      for (m <- classOf[net.minecraft.inventory.Container].getDeclaredMethods()) {
        if (m.getParameterTypes().length == 1 && m.getParameterTypes().apply(0) == classOf[Slot] && m.getReturnType() == classOf[Slot]) {
          m.setAccessible(true)
          m.invoke(container, slot)
        }
      }
    }
    def addSlots(inv: IInventory, slotID: Int, rowMax: Int, collomMax: Int, xStart: Int, yStart: Int) {
      for (row <- 0 until rowMax; collom <- 0 until collomMax) container.addSlot(new Slot(inv, collom + (row * collomMax) + slotID, xStart + (collom * 18), yStart + (row * 18)))
    }
  }
  implicit class WorldUtils(world: World) {
    def dropItemInWorld(itemStack: ItemStack, x: Float, y: Float, z: Float, rand: Random) {
      if (!world.isRemote) {
        if ((itemStack != null) && (itemStack.stackSize > 0)) {
          val dX = (rand.nextFloat() * 0.8F) + 0.1F
          val dY = (rand.nextFloat() * 0.8F) + 0.1F
          val dZ = (rand.nextFloat() * 0.8F) + 0.1F
          val entityItem = new EntityItem(world, x + dX, y + dY, z + dZ, new ItemStack(itemStack.getItem,
            itemStack.stackSize, itemStack.getItemDamage))
          if (itemStack.hasTagCompound()) {
            entityItem.getEntityItem.setTagCompound(itemStack.getTagCompound.copy().asInstanceOf[NBTTagCompound])
          }
          val factor = 0.05F
          entityItem.motionX = rand.nextGaussian() * factor
          entityItem.motionY = (rand.nextGaussian() * factor) + 0.2F
          entityItem.motionZ = rand.nextGaussian() * factor
          world.spawnEntityInWorld(entityItem)
          itemStack.stackSize = 0
        }
      }
    }
    def dropItemsInWorld(stacks: ArrayList[ItemStack], x: Float, y: Float, z: Float, rand: Random) {
      if (!world.isRemote) {
        for (i <- 0 until stacks.size) {
          val itemStack = stacks(i)
          dropItemInWorld(itemStack, x, y, z, rand)
        }
      }
    }
    def dropItemsInWorld(stacks: Array[ItemStack], x: Float, y: Float, z: Float, rand: Random) {
      if (!world.isRemote) {
        for (i <- 0 until stacks.size) {
          val itemStack = stacks(i)
          dropItemInWorld(itemStack, x, y, z, rand)
        }
      }
    }
    def dropBlockItems(pos:BlockPos, rand: Random) {
      if (!world.isRemote) {
        val tileEntity = world.getTileEntity(pos)
        if (tileEntity.isInstanceOf[IInventory]) {
          val inventory = tileEntity.asInstanceOf[IInventory]
          for (i <- 0 until inventory.getSizeInventory) {
            val itemStack = inventory.getStackInSlot(i)
            if ((itemStack != null) && (itemStack.stackSize > 0)) {
              val dX = (rand.nextFloat() * 0.8F) + 0.1F
              val dY = (rand.nextFloat() * 0.8F) + 0.1F
              val dZ = (rand.nextFloat() * 0.8F) + 0.1F
              val entityItem = new EntityItem(world, pos.getX + dX, pos.getY + dY, pos.getZ + dZ, new ItemStack(itemStack.getItem,
                itemStack.stackSize, itemStack.getItemDamage))
              if (itemStack.hasTagCompound()) {
                entityItem.getEntityItem.setTagCompound(itemStack.getTagCompound.copy().asInstanceOf[NBTTagCompound])
              }
              val factor = 0.05F
              entityItem.motionX = rand.nextGaussian() * factor
              entityItem.motionY = (rand.nextGaussian() * factor) + 0.2F
              entityItem.motionZ = rand.nextGaussian() * factor
              world.spawnEntityInWorld(entityItem)
              itemStack.stackSize = 0
            }
          }
        }
      }
    }
  }

  implicit class PlayerUtils(player: EntityPlayer) {
    def setCurrentItem(stack: ItemStack) = player.inventory.setInventorySlotContents(player.inventory.currentItem, stack)
    def getMOP(): MovingObjectPosition = {
      val f = 1.0F
      val f1 = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * f
      val f2 = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * f
      val d0 = player.prevPosX + (player.posX - player.prevPosX) * f.toDouble
      val d1 = player.prevPosY + (player.posY - player.prevPosY) * f.toDouble + (if (player.worldObj.isRemote) player.getEyeHeight - player.getDefaultEyeHeight else player.getEyeHeight).toDouble
      val d2 = player.prevPosZ + (player.posZ - player.prevPosZ) * f.toDouble
      val vec3 = new Vec3(d0, d1, d2)
      val f3 = MathHelper.cos(-f2 * 0.017453292F - Math.PI.toFloat)
      val f4 = MathHelper.sin(-f2 * 0.017453292F - Math.PI.toFloat)
      val f5 = -MathHelper.cos(-f1 * 0.017453292F)
      val f6 = MathHelper.sin(-f1 * 0.017453292F)
      val f7 = f4 * f5
      val f8 = f3 * f5
      var d3 = 5.0D
      if (player.isInstanceOf[EntityPlayerMP]) {
        d3 = player.asInstanceOf[EntityPlayerMP].theItemInWorldManager.getBlockReachDistance
      }
      val vec31 = vec3.addVector(f7.toDouble * d3, f6.toDouble * d3, f8.toDouble * d3)
      player.worldObj.rayTraceBlocks(vec3, vec31, true, false, false)
    }
  }
  
  implicit class InventoryUtils(stacks: ArrayList[ItemStack]) {
    def getNBT(): NBTTagList = {
      val inventory = new NBTTagList()
      for (slot <- 0 until stacks.size if stacks.get(slot) != null) {
        val Slots = new NBTTagCompound()
        Slots.setByte("Slot", slot.toByte)
        stacks.get(slot).writeToNBT(Slots)
        inventory.appendTag(Slots)
      }
      inventory
    }
    def containsItem(item: Item): Boolean = stacks.find(_.getItem == item).map(_ => true).getOrElse(false)
  }

  implicit class ItemStackUtils(item: ItemStack) {
    def addDamage(player: EntityPlayer, damage: Int) {
      val returnStack = new ItemStack(item.getItem, 1, (item.getItemDamage + damage))
      player.inventory.setInventorySlotContents(player.inventory.currentItem, if ((returnStack.getItemDamage >= item.getMaxDamage)) null else returnStack)
    }
    def getWildValue = new ItemStack(item.getItem(), 1, OreDictionary.WILDCARD_VALUE)
    def addDamage(damage: Int): ItemStack = {
      val returnStack = new ItemStack(item.getItem, 1, (item.getItemDamage + damage))
      val returnStack2 = returnStack.copy()
      returnStack2.stackSize = 0
      if (returnStack.getItemDamage >= item.getMaxDamage) returnStack2 else returnStack
    }
  }

  implicit class IntArrayUtils(numbers: Array[Int]) {
    def inverse(): Array[Int] = {
      val returnNumbers = Array.ofDim[Int](numbers.length)
      for (i <- 0 until numbers.length) returnNumbers(i) = -numbers(i)
      returnNumbers
    }
  }
  
  implicit class WrappedArrayUtils(wrap:WrappedArray[_]) {
    def unwrap:ListBuffer[Any] = {
      val retArr = new ListBuffer[Any]
      for(arr <- wrap){
        if(arr.isInstanceOf[WrappedArray[_]])retArr.append(arr.asInstanceOf[WrappedArray[_]].unwrap)
        else retArr.append(arr)
      }
      retArr
    }
  }

  implicit class IntegerUtils(currNumber: Int) {
    def getScaledNumber(maxNumber: Int, scaledNumber: Int): Float = (currNumber.toFloat / maxNumber.toFloat) * scaledNumber
  }
}
