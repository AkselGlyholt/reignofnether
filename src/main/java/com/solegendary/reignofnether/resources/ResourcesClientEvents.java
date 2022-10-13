package com.solegendary.reignofnether.resources;

import net.minecraft.client.Minecraft;

import java.util.ArrayList;

public class ResourcesClientEvents {

    // tracks all players' resources
    public static ArrayList<Resources> resourcesList = new ArrayList<>();

    public static void syncResources(Resources serverResources) {
        resourcesList.removeIf(resources -> resources.ownerName.equals(serverResources.ownerName));
        resourcesList.add(serverResources);
    }

    public static void addSubtractResources(Resources serverResources) {
        for (Resources resources : resourcesList) {
            if (resources.ownerName.equals(serverResources.ownerName)) {
                resources.foodToAdd += serverResources.food;
                resources.woodToAdd += serverResources.wood;
                resources.oreToAdd += serverResources.ore;
            }
        }
    }

    public static Resources getOwnResources() {
        Minecraft MC = Minecraft.getInstance();
        if (MC.player != null)
            return getResources(MC.player.getName().getString());
        return null;
    }

    public static Resources getResources(String playerName) {
        for (Resources resources : resourcesList)
            if (resources.ownerName.equals(playerName))
                return resources;
        return null;
    }

}