package dev.mja00.swarmsmps2.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

// This is a packet that is sent to the server with a list of mods currently installed on the server
// We'll use this to check if the player is using a modded client
public class ModListPacket {
    // We'll just use a list of mod ids
    private final String[] modIds;
    private static final String goodMods = """
            dynamiclightsreforged,ftbessentials,advancedperipherals,additionalentityattributes,apoli,calio,origins,deleteitem,playeranimator,
            additionalbanners,incontrol,connectivity,serverredirect,rubidium,hourglass,darkness,ctm,cookingforblockheads,controlling,placebo,
            wildbackport,extendedslabs,extrapotions,bookshelf,consistency_plus,mcwdoors,balm,dynview,jeresources,cloth_config,shetiphiancore,
            flytre_lib,lod,taterzens,config2brigadier,ambientsounds,mcwtrpdoors,mcwfences,swarmsmps2,bendylib,reeses_sodium_options,
            northerncompass,patchouli,oculus,collective,betterbiomeblend,worldedit,pluto,mcwroofs,architectury,
            computercraft,aiimprovements,ageingspawners,observable,fastleafdecay,geckolib3,waterdripsound,fastload,
            ftblibrary,shieldmechanics,spiderstpo,platforms,jei,disguiselib,pehkui,caelus,mcwpaintings,fastsuite,
            clumps,extendedclouds,dual_riders,alternate_current,configured,decorative_blocks,myserveriscompatible,
            betteranimalsplus,additional_lights,farsight_view,toastcontrol,jeitweaker,blueprint,crafttweaker,
            gamestages,rubidium_extras,forge,mcwpaths,emotecraft,selene,supplementaries,minecraft,voicechat,
            sound_physics_remastered,terrablender,swingthroughgrass,mousetweaks,itemstages,firstpersonmod,
            another_furniture,creativecore,weaponmaster,smoothboot,astikorcarts,betterfpsdist,kotlinforforge,
            notenoughanimations,stonecutter_recipe_tags,cyclepaintings,fastbench,
            polymorph,autoreglib,quark,immersive_paintings,entityculling,canary,worldeditcuife3,fastfurnace,appleskin,ferritecore,damagetilt,swarmsmp
            """;
    public static final Logger LOGGER = LogManager.getLogger("SSMPS2/ModListPacket");

    public ModListPacket(String[] modIds) {
        this.modIds = modIds;
    }

    public String[] getModIds() {
        return this.modIds;
    }

    public static void encode(ModListPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.modIds.length);
        for (String modId : packet.modIds) {
            buffer.writeUtf(modId);
        }
    }

    public static ModListPacket decode(FriendlyByteBuf buffer) {
        int length = buffer.readVarInt();
        String[] modIds = new String[length];
        for (int i = 0; i < length; i++) {
            modIds[i] = buffer.readUtf();
        }
        return new ModListPacket(modIds);
    }

    public static void handle(ModListPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Make a neat string of the mod list
            ServerPlayer player = ctx.get().getSender();
            String name = player != null ? player.getName().getString() : "Unknown";
            // Our known good mods needs to be a set of strings
            List<String> knownGoodMods = List.of(goodMods.split(","));
            // Check if the player has any mods that aren't in our known good mods
            List<String> badMods = Stream.of(packet.modIds).filter(modId -> !knownGoodMods.contains(modId)).toList();
            if (!badMods.isEmpty()) {
                LOGGER.info(name + " has " + badMods.size() + " bad mods: " + String.join(", ", badMods));
            }

        });
        ctx.get().setPacketHandled(true);
    }
}
