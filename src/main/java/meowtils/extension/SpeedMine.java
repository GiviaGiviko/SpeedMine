package meowtils.extension;

import wtf.tatp.meowtils.config.Config;
import wtf.tatp.meowtils.event.api.EventTarget;
import wtf.tatp.meowtils.event.ClientTickEvent;
import wtf.tatp.meowtils.event.RenderWorldLastEvent;
import wtf.tatp.meowtils.extension.Extension;
import wtf.tatp.meowtils.util.Render;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockObsidian;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.awt.Color;
import java.lang.reflect.Field;

/**
 * This is a module class, it is registered in our Main class. In-game it will show up as a new module
 * in the Extensions category.
 *
 * Ported from Myau's SpeedMine. Fast mine is gated by the first solid block behind the crosshair target.
 *
 * docs.tatp.wtf/extensions/gui/module
 */
public class SpeedMine extends Extension {

    /// You should always keep an "enabled" boolean for each module. This represents the current module state.
    @Config public boolean enabled = false;

    /// You should always keep a "key" int, this represents currently bound keycode to this module.
    @Config public int key = 0;

    /// Forced block damage, as a percent.
    @Config public int speed = 15;

    /// Extra block hit delay allowance.
    @Config public int delay = 0;

    /// Highlights the block behind the target: green = obby/bed, red = other.
    @Config public boolean debug = false;

    private static Field blockHitDelayField;
    private static Field curBlockDamageMPField;
    private static Field isHittingBlockField;

    static {
        try {
            blockHitDelayField = PlayerControllerMP.class.getDeclaredField("blockHitDelay");
            blockHitDelayField.setAccessible(true);

            curBlockDamageMPField = PlayerControllerMP.class.getDeclaredField("curBlockDamageMP");
            curBlockDamageMPField.setAccessible(true);

            isHittingBlockField = PlayerControllerMP.class.getDeclaredField("isHittingBlock");
            isHittingBlockField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public SpeedMine() {

        /**
         * Change the module name & author, the module name should be something relevant to your feature,
         * the author should be you.
         */
        super("SpeedMine", "Givia");
        info("SpeedMine for only OBBY & BED");

        slider("Speed", 0, 100, 1, "%", "speed", int.class);
        slider("Delay", 0, 4, 1, null, "delay", int.class);

        expand("Advance", e -> {
            e.toggle("Debug", "debug");
        });
    }

    /// First solid block behind the target, walking from the hit point along the look direction.
    private BlockPos getNextBlock(MovingObjectPosition mop) {
        if (mop == null || mop.hitVec == null) return null;
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return null;

        Vec3 look = mc.thePlayer.getLook(1.0F);
        Vec3 probe = mop.hitVec;
        BlockPos target = mop.getBlockPos();

        // step forward in small increments until we hit a solid block that isn't the target
        for (int i = 0; i < 20; i++) {
            probe = probe.addVector(look.xCoord * 0.25, look.yCoord * 0.25, look.zCoord * 0.25);

            BlockPos p = new BlockPos(probe.xCoord, probe.yCoord, probe.zCoord);
            if (p.equals(target)) continue;
            if (!mc.theWorld.isAirBlock(p)) return p;
        }

        return null;
    }

    /// True if the block at pos is obsidian or a bed.
    private boolean isValuable(BlockPos pos) {
        if (pos == null || mc == null || mc.theWorld == null) return false;

        Block block = mc.theWorld.getBlockState(pos).getBlock();
        return block instanceof BlockObsidian || block instanceof BlockBed;
    }

    @EventTarget
    public void onClientTick(ClientTickEvent event) {
        if (event.getPhase() != ClientTickEvent.Phase.POST) return;
        if (mc == null || mc.thePlayer == null || mc.theWorld == null || mc.playerController == null) return;
        if (mc.playerController.isInCreativeMode()) return;
        if (blockHitDelayField == null || curBlockDamageMPField == null || isHittingBlockField == null) return;

        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;

        // Gate: only fast mine when the first solid block behind the target is obby/bed.
        if (!isValuable(getNextBlock(mop))) return;

        try {
            PlayerControllerMP pc = mc.playerController;

            int hitDelay = blockHitDelayField.getInt(pc);
            blockHitDelayField.setInt(pc, Math.min(hitDelay, this.delay + 1));

            if (isHittingBlockField.getBoolean(pc)) {
                float current = curBlockDamageMPField.getFloat(pc);
                float damage = 0.3F * (this.speed / 100.0F);
                if (current < damage) {
                    curBlockDamageMPField.setFloat(pc, damage);
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @EventTarget
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!this.debug) return;
        if (mc == null || mc.theWorld == null) return;

        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;

        BlockPos next = getNextBlock(mop);
        if (next == null) return; // nothing behind, skip drawing

        boolean ok = isValuable(next);

        Color fill = ok ? new Color(0, 255, 0, 100) : new Color(255, 0, 0, 100);
        Color outline = ok ? new Color(0, 255, 0, 255) : new Color(255, 0, 0, 255);

        AxisAlignedBB box = new AxisAlignedBB(
                next.getX(), next.getY(), next.getZ(),
                next.getX() + 1, next.getY() + 1, next.getZ() + 1);

        Render.drawBlockBox(box, next, true, fill, true, outline, 0.0, 0.0, 0.0);
    }
}