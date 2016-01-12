package ninja.shadowfox.shadowfox_botany.common.core.handler

import cpw.mods.fml.client.event.ConfigChangedEvent
import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import net.minecraft.potion.Potion
import net.minecraftforge.common.config.Configuration
import java.io.File
import kotlin.properties.Delegates


public class ConfigHandler {

    class ChangeListener {
        constructor()

        @SubscribeEvent
        fun onConfigChanged(eventArgs: ConfigChangedEvent.OnConfigChangedEvent) {
            if (eventArgs.modID == "shadowfox_botany") {
                load()
            }
        }
    }

    public companion object {
        var config: Configuration by Delegates.notNull()
        var realLightning = false
        var uberCreepers = false
        var passiveLightning = true
        var blackLotusDropRate = 0.05
        var addTincturemAspect = true
        var potionIDManaVoid = 110
        var schemaArray: IntArray = IntArray(17, { i: Int -> -1 + i })


        private var potionArrayLimit = 0
        private var verifiedPotionArray = false

        fun loadConfig(configFile: File) {
            config = Configuration(configFile)
            config.load()
            load()
            FMLCommonHandler.instance().bus().register(ChangeListener())
        }

        fun load() {
            var desc = "Lightning rod creates real lightning."
            realLightning = loadPropBool("realLightning.enabled", desc, realLightning)

            desc = "Uber Creepers can be spawned by OP or Creative players"
            uberCreepers = loadPropBool("uberCreepers.enabled", desc, uberCreepers)

            desc = "Lightning rod can hit passive mobs"
            passiveLightning = loadPropBool("passiveLightning.enabled", desc, passiveLightning)

            desc = "Add a Color aspect to thaumcraft"
            addTincturemAspect = loadPropBool("tincturem.enabled", desc, addTincturemAspect)

            desc = "Rate of black loti dropping from Manaseal Creepers"
            blackLotusDropRate = loadPropDouble("voidCreepers.dropRate", desc, blackLotusDropRate)

            desc = "Which schemas are allowed to be generated"
            schemaArray = loadPropIntArray("schemas.enabled", desc, schemaArray)

            potionIDManaVoid = loadPropPotionId("manaVoid", potionIDManaVoid)

            if (config.hasChanged()) {
                config.save()
            }

        }

        fun loadPostInit() {
            if (config.hasChanged()) {
                config.save()
            }
        }

        fun loadPropInt(propName: String, desc: String, default_: Int): Int {
            val prop = config.get("general", propName, default_)
            prop.comment = desc
            return prop.getInt(default_)
        }

        fun loadPropPotionId(propName: String, default_: Int): Int {
            if (!verifiedPotionArray) {
                verifyPotionArray()
            }

            val prop = config.get("potions", propName, default_)
            var `val` = prop.getInt(default_)
            if (`val` > potionArrayLimit) {
                `val` = default_
                prop.set(default_)
            }

            return `val`
        }

        private fun verifyPotionArray() {
            if (Loader.isModLoaded("DragonAPI")) {
                potionArrayLimit = Potion.potionTypes.size
            } else {
                potionArrayLimit = 127
            }

            verifiedPotionArray = true
        }

        fun loadPropDouble(propName: String, desc: String, default_: Double): Double {
            val prop = config.get("general", propName, default_)
            prop.comment = desc
            return prop.getDouble(default_)
        }

        fun loadPropIntArray(propName: String, desc: String, default_: IntArray): IntArray {
            val prop = config.get("general", propName, default_)
            prop.comment = desc
            return prop.intList
        }

        fun loadPropBool(propName: String, desc: String, default_: Boolean): Boolean {
            val prop = config.get("general", propName, default_)
            prop.comment = desc
            return prop.getBoolean(default_)
        }
    }
}
