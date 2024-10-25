package com.solegendary.reignofnether.time;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.guiscreen.TopdownGui;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.research.researchItems.ResearchLingeringPotions;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialStage;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class TimeClientEvents {

    private static int xPos = 0;
    private static int yPos = 0;

    private static final Minecraft MC = Minecraft.getInstance();

    // setting this value causes the time of day to smoothly move towards it regardless of the server time
    public static long targetClientTime = 0;
    // actual time on the server
    public static long serverTime = 0;

    public static boolean showNightRadius = true;

    private static final long DAWN = 500;
    private static final long DUSK = 12500;

    private static final Button CLOCK_BUTTON = new Button(
        "Clock",
        14,
        null,
        null,
        null,
        () -> false,
        () -> false,
        () -> true,
        () -> showNightRadius = !showNightRadius,
        null,
        null
    );

    // use instead of level.isDay() as its much stricter for undead burning checks
    public static boolean isDay() {
        return serverTime > DAWN && serverTime <= DUSK;
    }

    // more consistent version of Mob.isSunburnTick()
    public static boolean isSunBurnTick(Mob mob) {
        if (isDay() && !mob.level.isClientSide) {
            BlockPos blockpos = new BlockPos(mob.getX(), mob.getEyeY(), mob.getZ());
            boolean flag = mob.isInWaterRainOrBubble() || mob.isInPowderSnow || mob.wasInPowderSnow;
            return !mob.isOnFire() && !flag && mob.level.canSeeSky(blockpos) && !NightUtils.isInRangeOfNightSource(mob.getEyePosition(), mob.level.isClientSide);
        }
        return false;
    }

    // ensures a time value is between 0 and 24000
    public static long normaliseTime(long time) {
        long timeNorm = time;
        while (timeNorm < 0)
            timeNorm += 24000;
        while (timeNorm >= 24000)
            timeNorm -= 24000;
        return timeNorm;
    }

    private static String get12HourTimeStr(long time) {
        long hours = time / 1000 + 6;
        long minutes = (time % 1000) * 60 / 1000;
        String ampm = "am";
        while (hours >= 12) {
            hours -= 12;
            ampm = "pm";
        }
        if (hours == 0) hours = 12;
        String mm = "0" + minutes;
        mm = mm.substring(mm.length() - 2);
        return hours + ":" + mm + ampm;
    }

    // get a string representing real time in min/sec until the given time
    private static String getTimeUntilStr(long currentTime, long targetTime) {
        if (currentTime > targetTime)
            currentTime -= 24000;
        long timeDiff = targetTime - currentTime;

        // there's 1200 real time seconds per MC day (24000 units)
        int sec = (int) Math.round(timeDiff / 20d);
        int min = sec / 60;
        sec -= (min * 60);

        if (min == 0)
            return sec + "s";
        return min + "m" + sec + "s";
    }

    // get a string representing real time in min/sec until the given time
    private static String getTimeStrFromTicks(long ticks) {
        int sec = (int) Math.round(ticks / 20d);
        int min = sec / 60;
        sec -= (min * 60);

        if (min == 0)
            return sec + "s";
        return min + "m" + sec + "s";
    }

    // render directly above the minimap
    @SubscribeEvent
    public static void renderOverlay(RenderGuiOverlayEvent.Post evt) {
        if (!OrthoviewClientEvents.isEnabled() || MC.isPaused() ||
            !TutorialClientEvents.isAtOrPastStage(TutorialStage.MINIMAP_CLICK))
            return;

        xPos = MC.getWindow().getGuiScaledWidth() - MinimapClientEvents.getMapGuiRadius() - (MinimapClientEvents.CORNER_OFFSET * 2) + 2;
        yPos = MC.getWindow().getGuiScaledHeight() - (MinimapClientEvents.getMapGuiRadius() * 2) - (MinimapClientEvents.CORNER_OFFSET * 2) - 6;

        ItemRenderer itemrenderer = MC.getItemRenderer();

        itemrenderer.renderAndDecorateItem(new ItemStack(Items.CLOCK), xPos, yPos);
    }

    @SubscribeEvent
    public static void onDrawScreen(ScreenEvent.Render.Post evt) {
        if (!OrthoviewClientEvents.isEnabled() || MC.isPaused() ||
            !TutorialClientEvents.isAtOrPastStage(TutorialStage.MINIMAP_CLICK))
            return;

        xPos = MC.getWindow().getGuiScaledWidth() - MinimapClientEvents.getMapGuiRadius() - (MinimapClientEvents.CORNER_OFFSET * 2) + 2;
        yPos = MC.getWindow().getGuiScaledHeight() - (MinimapClientEvents.getMapGuiRadius() * 2) - (MinimapClientEvents.CORNER_OFFSET * 2) - 6;

        CLOCK_BUTTON.render(evt.getPoseStack(), xPos-3, yPos-3, evt.getMouseX(), evt.getMouseY());
    }

    @SubscribeEvent
    public static void onMousePress(ScreenEvent.MouseButtonPressed.Post evt) {
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1)
            CLOCK_BUTTON.checkClicked((int) evt.getMouseX(), (int) evt.getMouseY(), true);
        //else if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2)
        //    CLOCK_BUTTON.checkClicked((int) evt.getMouseX(), (int) evt.getMouseY(), false);
    }



    @SubscribeEvent
    public static void onDrawScreen(ScreenEvent.Render evt) {
        if (!TutorialClientEvents.isAtOrPastStage(TutorialStage.MINIMAP_CLICK))
            return;

        final int GUI_LENGTH = 16;

        if (evt.getMouseX() > xPos && evt.getMouseX() <= xPos + GUI_LENGTH &&
            evt.getMouseY() > yPos && evt.getMouseY() <= yPos + GUI_LENGTH) {

            // 'day' is when undead start burning, ~500
            // 'night' is when undead stop burning, ~12500
            boolean isDay = isDay();
            String dayStr = isDay ? " (day)" : " (night)";
            String timeStr = get12HourTimeStr(serverTime) + dayStr;

            FormattedCharSequence timeUntilStr = FormattedCharSequence.forward(
                    getTimeUntilStr(serverTime, isDay ? DUSK : DAWN) + " until " + (isDay ? "night" : "day"), Style.EMPTY);

            FormattedCharSequence gameLengthStr = FormattedCharSequence.forward("", Style.EMPTY);

            if (PlayerClientEvents.isRTSPlayer)
                gameLengthStr = FormattedCharSequence.forward("Game time: " + getTimeStrFromTicks(PlayerClientEvents.rtsGameTicks), Style.EMPTY);

            List<FormattedCharSequence> tooltip = List.of(
                    FormattedCharSequence.forward("Time: " + timeStr, Style.EMPTY),
                    timeUntilStr,
                    FormattedCharSequence.forward("" + timeStr, Style.EMPTY),
                    gameLengthStr,
                    FormattedCharSequence.forward("Night circles: " + (showNightRadius ? "ON" : "OFF"), Style.EMPTY)
            );
            if (targetClientTime != serverTime)
                tooltip = List.of(
                        FormattedCharSequence.forward("Time is distorted to midnight", Style.EMPTY.withBold(true)),
                        FormattedCharSequence.forward("Real time: " + timeStr, Style.EMPTY),
                        timeUntilStr,
                        gameLengthStr,
                        FormattedCharSequence.forward("Night circles: " + (showNightRadius ? "ON" : "OFF"), Style.EMPTY)
                );

            MyRenderer.renderTooltip(
                    evt.getPoseStack(),
                    tooltip,
                    evt.getMouseX(),
                    evt.getMouseY()
            );
        }
    }

    // show corners of all frozenChunks
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evt) {
        if (evt.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
            return;
        if (!OrthoviewClientEvents.isEnabled() || !showNightRadius || MC.level == null)
            return;

        // draw night-ranges for monsters
        for (Building building : BuildingClientEvents.getBuildings())
            if (building instanceof NightSource ns)
                for (BlockPos bp : ns.getNightBorderBps()) {
                    MyRenderer.drawBlockFace(evt.getPoseStack(), Direction.UP, bp, 0f, 0f, 0f, 0.6f);
                    /* causes a lot of flickering
                    if (MC.level.getBlockState(bp.north()).isAir())
                        MyRenderer.drawBlockFace(evt.getPoseStack(), Direction.NORTH, bp, 0f, 0f, 0f, 0.5f);
                    if (MC.level.getBlockState(bp.south()).isAir())
                        MyRenderer.drawBlockFace(evt.getPoseStack(), Direction.SOUTH, bp, 0f, 0f, 0f, 0.5f);
                    if (MC.level.getBlockState(bp.east()).isAir())
                        MyRenderer.drawBlockFace(evt.getPoseStack(), Direction.EAST, bp, 0f, 0f, 0f, 0.5f);
                    if (MC.level.getBlockState(bp.west()).isAir())
                        MyRenderer.drawBlockFace(evt.getPoseStack(), Direction.WEST, bp, 0f, 0f, 0f, 0.5f);
                    */
                }
    }
}
