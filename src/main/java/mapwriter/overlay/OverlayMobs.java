package mapwriter.overlay;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import mapwriter.map.MapView;
import mapwriter.map.mapmode.MapMode;
import mapwriter.util.Render;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;

public class OverlayMobs
{
    private static final int UPDATE_INTERVAL = 5;
    private static final int MAX_RANGE = 64;
    private static final int DOT_RADIUS = 1;
    private static final int PLAYER_DOT_RADIUS = 2;
    private static final float BASE_ALPHA = 0.85f;
    private static final int MAX_Y_DIFF = 32;

    private static class MobData
    {
        final double x;
        final double z;
        final double y;
        final int color;
        MobData(double x, double y, double z, int color)
        {
            this.x = x;
            this.y = y;
            this.z = z;
            this.color = color;
        }
    }

    private final List<MobData> cache = new ArrayList<>();
    private int tickCounter = 0;
    private boolean enabled = false;
    private double playerY = 0;

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
        if (!enabled) cache.clear();
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void onTick()
    {
        if (!enabled) return;
        if (++tickCounter % UPDATE_INTERVAL != 0) return;
        updateCache();
    }

    private void updateCache()
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null) return;

        cache.clear();
        this.playerY = mc.player.posY;

        AxisAlignedBB aabb = new AxisAlignedBB(
                mc.player.posX - MAX_RANGE, 0, mc.player.posZ - MAX_RANGE,
                mc.player.posX + MAX_RANGE, 256, mc.player.posZ + MAX_RANGE
        );

        List<Entity> entities = mc.world.getEntitiesWithinAABB(Entity.class, aabb);
        for (Entity e : entities)
        {
            if (e == mc.player) continue;
            if (!(e instanceof EntityLiving)) continue;

            int color = getEntityColor(e);
            if (color != 0)
            {
                cache.add(new MobData(e.posX, e.posY, e.posZ, color));
            }
        }
    }

    private int getEntityColor(Entity e)
    {
        if (e instanceof EntityPlayer)
        {
            return 0xFFFFDD44;
        }

        if (e instanceof EntityTameable && ((EntityTameable) e).isTamed())
        {
            return 0xFF66DD66;
        }
        if (e instanceof AbstractHorse && ((AbstractHorse) e).isTame())
        {
            return 0xFF66DD66;
        }

        if (e instanceof IMob)
        {
            return 0xFFDD5544;
        }

        return 0xFFDDDDDD;
    }

    private float getAlpha(double entityY)
    {
        float dy = (float)Math.abs(entityY - this.playerY);
        if (dy >= MAX_Y_DIFF) return 0.0f;
        return BASE_ALPHA * (1.0f - dy / MAX_Y_DIFF);
    }

    public void draw(MapMode mapMode, MapView mapView)
    {
        if (!enabled || cache.isEmpty()) return;

        int tx = mapMode.getXTranslation();
        int ty = mapMode.getYTranslation();

        for (MobData data : cache)
        {
            Point.Double rel = mapMode.getClampedScreenXY(mapView, data.x, data.z);
            int screenX = tx + (int)rel.x;
            int screenY = ty + (int)rel.y;

            int radius = (data.color == 0xFFFFDD44) ? PLAYER_DOT_RADIUS : DOT_RADIUS;
            float alpha = getAlpha(data.y);
            if (alpha <= 0.01f) continue;

            int r = (data.color >> 16) & 0xFF;
            int g = (data.color >> 8) & 0xFF;
            int b = data.color & 0xFF;
            GlStateManager.color(r / 255f, g / 255f, b / 255f, alpha);
            Render.drawCircle(screenX, screenY, radius);
        }
        GlStateManager.color(1f, 1f, 1f, 1f);
    }
}