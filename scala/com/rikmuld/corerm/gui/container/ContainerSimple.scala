package com.rikmuld.corerm.gui.container

import com.rikmuld.corerm.Library.AdvancementInfo
import com.rikmuld.corerm.advancements.TriggerHelper
import net.minecraft.entity.player.{EntityPlayer, EntityPlayerMP}
import net.minecraft.inventory.{InventoryCraftResult, InventoryCrafting, _}
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.CraftingManager
import net.minecraft.network.play.server.SPacketSetSlot
import net.minecraft.world.World

abstract class ContainerSimple[A <: IInventory](player: EntityPlayer) extends Container {
  val inv: A = initIInventory

  addInventorySlots()
  addPlayerSlots()

  getIInventory.openInventory(player)

  if(!player.world.isRemote)
    TriggerHelper.trigger(AdvancementInfo.GUI_OPEN, player, getID)

  def playerInvX: Int =
    8

  def playerInvY: Int =
    84

  def getID: String

  override def canInteractWith(player: EntityPlayer): Boolean =
    !player.isDead && getIInventory.isUsableByPlayer(player)

  override def onContainerClosed(player: EntityPlayer) {
    super.onContainerClosed(player)

    getIInventory.closeInventory(player)
  }

  def getIInventory: A =
    inv

  def initIInventory: A

  def addInventorySlots(): Unit

  def addPlayerSlots(): Unit = {
    addSlots(player.inventory, 0, 1, 9, playerInvX, playerInvY + 58)
    addSlots(player.inventory, 9, 3, 9, playerInvX, playerInvY)
  }

  override def transferStackInSlot(playerIn: EntityPlayer, index: Int): ItemStack = {
    var stack: ItemStack = ItemStack.EMPTY
    val slot: Slot = this.inventorySlots.get(index)

    if (slot != null && slot.getHasStack) {
      val moveStack: ItemStack = slot.getStack
      stack = moveStack.copy

      if (index < (inventorySlots.size - 36)) {
        if (!mergeFromInventory(moveStack, stack, index)) return ItemStack.EMPTY
      } else {
        if (!mergeToInventory(moveStack, stack, index)) {
          if(index < (inventorySlots.size - 27)){
            if(!mergeItemStack(moveStack, inventorySlots.size - 27, inventorySlots.size, false)) return ItemStack.EMPTY
          } else {
            if(!mergeItemStack(moveStack, inventorySlots.size - 36, inventorySlots.size - 27, false)) return ItemStack.EMPTY
          }
        }
      }

      if (moveStack.isEmpty) slot.putStack(ItemStack.EMPTY)
      else slot.onSlotChanged()

      val result = postSlotTransfer(moveStack, stack, index)
      if(result.isDefined) return result.get
    }

    stack
  }

  def postSlotTransfer(moveStack: ItemStack, original: ItemStack, index: Int): Option[ItemStack] =
    None

  def mergeFromInventory(stack: ItemStack, original: ItemStack, index: Int): Boolean = {
    mergeItemStack(stack, inventorySlots.size - 36, inventorySlots.size, false)
  }

  def mergeToInventory(stack: ItemStack, original: ItemStack, index: Int): Boolean = {
    mergeItemStack(stack, 0, inventorySlots.size - 36, false)
  }

  def addSlots(inv: IInventory, slotID: Int, rowMax: Int, columnMax: Int, xStart: Int, yStart: Int) {
    for (row <- 0 until rowMax; column <- 0 until columnMax)
      addSlotToContainer(new Slot(
        inv, column + (row * columnMax) + slotID, xStart + (column * 18), yStart + (row * 18)
      ))
  }

  //default implementation always (for the client side) puts the stack in slot with ID 0
  protected def slotChangedCraftingGrid(world: World, player: EntityPlayer, crafting: InventoryCrafting, result: InventoryCraftResult, slotId: Int): Unit = {
    if (!world.isRemote) {
      val playerMP = player.asInstanceOf[EntityPlayerMP]

      val stack = Option(CraftingManager.findMatchingRecipe(crafting, world)).fold(ItemStack.EMPTY)(
        recipe =>
          if(recipe.isDynamic || !world.getGameRules.getBoolean("doLimitedCrafting") || playerMP.getRecipeBook.isUnlocked(recipe)){
            result.setRecipeUsed(recipe)
            recipe.getCraftingResult(crafting)
          } else ItemStack.EMPTY
      )

      result.setInventorySlotContents(0, stack)
      playerMP.connection.sendPacket(new SPacketSetSlot(this.windowId, slotId, stack))
    }
  }
}

