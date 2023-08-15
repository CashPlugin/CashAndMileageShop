package com.uomaep.kotlintestplugin.utils

import org.apache.commons.lang.ObjectUtils.Null
import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOError
import java.io.IOException
import java.lang.NullPointerException

class ItemUtil {
    companion object {
        fun serialize(itemStack: ItemStack): String {
            try {
               val outputStream = ByteArrayOutputStream()
               val dataOutput = BukkitObjectOutputStream(outputStream)

               dataOutput.writeObject(itemStack)
               return Base64Coder.encodeLines(outputStream.toByteArray())
            } catch (e: Exception) {
               throw IllegalStateException("Unable to save item stack.", e)
            }
        }

        fun deserialize(serializedItemStack: String): ItemStack {
            try {
                val inputStream = ByteArrayInputStream(Base64Coder.decodeLines(serializedItemStack))
                val dataInput = BukkitObjectInputStream(inputStream)

                return dataInput.readObject() as ItemStack
            } catch (e: ClassNotFoundException) {
                throw NullPointerException()
            } catch (e: IOException) {
                throw NullPointerException()
            }
        }
    }
}