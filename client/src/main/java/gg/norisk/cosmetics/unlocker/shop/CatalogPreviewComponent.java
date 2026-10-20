package gg.norisk.cosmetics.unlocker.shop;

import gg.norisk.abstraction.client.MCClient;
import gg.norisk.abstraction.client.NrcPreviewEntityTracker;
import gg.norisk.abstraction.text.TextKt;
import gg.norisk.cosmetics.v2.player.NrcCosmeticPlayer;
import gg.norisk.cosmetics.v2.player.NrcCosmeticPlayerData;
import gg.norisk.cosmetics.v2.render.NrcCosmeticRenderBridge;
import gg.norisk.cosmetics.v2.ui.preview.CosmeticShowcasePlayer;
import gg.norisk.owolib.owo.ui.component.LabelComponent;
import gg.norisk.owolib.owo.ui.component.entity.EntityComponent;
import gg.norisk.owolib.owo.ui.container.FlowLayout;
import gg.norisk.owolib.owo.ui.core.HorizontalAlignment;
import gg.norisk.owolib.owo.ui.core.OwoUIGraphics;
import gg.norisk.owolib.owo.ui.core.ParentUIComponent;
import gg.norisk.owolib.owo.ui.core.Positioning;
import gg.norisk.owolib.owo.ui.core.Sizing;
import gg.norisk.owolib.owo.ui.core.UIComponent;
import gg.norisk.owolib.owo.ui.core.VerticalAlignment;
import gg.norisk.owolib.owo.ui.renderstate.NrcEntityElementRenderState;
import gg.norisk.owoplus.extensions.ICachedElementRenderStateWrapper;
import kotlin.Unit;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 3D showcase for a catalog item, built like NoRisk's own single-cosmetic showcase:
 * an invisible preview player wearing only this item, posed per category.
 * The preview player only exists while the card is on screen; cards that stop being drawn
 * (scrolled away, page rebuilt, wardrobe closed) release it so a long catalog stays cheap.
 */
final class CatalogPreviewComponent extends FlowLayout {

    /** Previews whose player is registered with NoRisk and the catalog; render thread only. */
    private static final Set<CatalogPreviewComponent> LIVE = new LinkedHashSet<>();
    private static final long IDLE_MS = 1500L;
    private static long lastSweep;
    private static boolean sweeping;

    static {
        // Also runs while no card is drawn at all, e.g. after the wardrobe was closed.
        ClientTickEvents.END_CLIENT_TICK.register(client -> sweep(System.currentTimeMillis()));
    }

    private final ShopCatalogBridge.Item item;
    private CosmeticShowcasePlayer entity;
    private EntityComponent<CosmeticShowcasePlayer> view;
    private LabelComponent error;
    private float[] shape;
    private boolean mounted;
    private boolean attached;
    private long lastTick = System.currentTimeMillis();
    private long lastDrawn;

    CatalogPreviewComponent(ShopCatalogBridge.Item item) {
        super(Sizing.fill(), Sizing.fill(), Algorithm.VERTICAL);
        this.item = item;
        alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
        allowOverflow(false);
    }

    private void init() {
        if (entity != null) return;
        entity = new CosmeticShowcasePlayer(MCClient.getGameProfile());
        entity.setInvisible(true);
        entity.setPos(0.0, 0.0, 0.0);
        entity.xo = 0.0;
        entity.yo = 0.0;
        entity.zo = 0.0;
        if ("CAPE".equals(item.type()) || "SHIELD".equals(item.type())) entity.setFrozen(true);

        view = EntityComponent.of(Sizing.fill(), entity).scaleToFit(true).allowMouseRotation(false).showNametag(false);
        view.sizing(Sizing.fill(), Sizing.fill());
        view.poseTransform(pose -> {
            pose(pose);
            return Unit.INSTANCE;
        });
        if (view instanceof ICachedElementRenderStateWrapper wrapper) {
            Object hash = "nrcu-catalog:" + item.id();
            wrapper.setNrc_wrapSpecialElementState((state, matrix, bounds, scissor) -> {
                NrcEntityElementRenderState renderState = new NrcEntityElementRenderState(state, matrix, bounds, scissor);
                renderState.setNrc_hashCode(() -> hash);
                renderState.setNrc_createRenderer(NrcEntityElementRenderState.Renderer::new);
                return renderState;
            });
        }
        child(view);
    }

    /** Share of the card the cosmetic fills when framed by its shape. */
    private static final float FILL = 0.82f;

    /**
     * Frames the item like NoRisk's own single-cosmetic showcase: the category's camera angle, then zoomed and
     * centred on where the item really is once its shape is known. Until then NoRisk's typical framing applies.
     */
    private void pose(EntityComponent.Pose pose) {
        String type = item.type() == null ? "" : item.type();
        float[] angle = angle(type);
        float[] shape = shape();
        if (shape == null || view == null || view.height() <= 0) {
            fallback(pose, type);
        } else {
            // EntityComponent views the entity at rotX 35, rotY -45 after the pose; project with the same chain.
            var rotation = new Matrix3f().rotateY(rad(angle[0])).rotateX(rad(angle[1])).rotateX(rad(35)).rotateY(rad(-45));
            var point = new Vector3f();
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
            for (int i = 0; i + 2 < shape.length; i += 3) {
                rotation.transform(shape[i], shape[i + 1], shape[i + 2], point);
                minX = Math.min(minX, point.x); maxX = Math.max(maxX, point.x);
                minY = Math.min(minY, point.y); maxY = Math.max(maxY, point.y);
                minZ = Math.min(minZ, point.z); maxZ = Math.max(maxZ, point.z);
            }
            // Visible half extent in entity units, from EntityComponent's render scale.
            float halfHeight = 32f / (75f * view.entityScale());
            float halfWidth = (float) (halfHeight * view.width() / view.height());
            float k = Math.min(halfWidth * FILL / Math.max(0.02f, (maxX - minX) / 2), halfHeight * FILL / Math.max(0.02f, (maxY - minY) / 2));
            k = Math.max(0.25f, Math.min(10f, k));
            pose.scale(k);
            pose.translate(-(minX + maxX) / 2, entity.getBbHeight() / 2 / k - (minY + maxY) / 2, -(minZ + maxZ) / 2);
        }
        if (angle[0] != 0) pose.rotateY(angle[0]);
        if (angle[1] != 0) pose.rotateX(angle[1]);
    }

    private float[] shape() {
        if (shape != null) return shape;
        ShopCatalogBridge bridge = ShopCatalog.get();
        if (bridge == null || entity == null) return null;
        boolean slim = entity.getSkin().model().toString().equalsIgnoreCase("SLIM");
        return shape = bridge.previewShape(item.id(), slim);
    }

    /** NoRisk's camera angle per category (rotateY, rotateX), as in its single-cosmetic showcase. */
    private static float[] angle(String type) {
        return switch (type) {
            case "WINGS", "BACK" -> new float[]{-140f, -35f};
            case "CAPE" -> new float[]{-150f, -35f};
            case "SHIELD" -> new float[]{-65f, -34f};
            case "FACE" -> new float[]{25f, -35f};
            default -> new float[]{0f, 0f};
        };
    }

    /** NoRisk's most common preview scale and offset per category, for items without a known shape. */
    private static void fallback(EntityComponent.Pose pose, String type) {
        switch (type) {
            case "WINGS" -> { pose.scale(1.58f); pose.translate(-0.01f, -0.69f, 0f); }
            case "CAPE" -> { pose.scale(2.9f); pose.translate(-0.01f, -0.58f, 0f); pose.translate(-0.075f, -0.05f, 0f); }
            case "HAT" -> { pose.scale(2.8f); pose.translate(0f, -0.25f, 0f); pose.translate(0f, -0.9f, 0f); }
            case "PET" -> { pose.scale(2.23f); pose.translate(0.56f, -2.71f, 0f); pose.scale(2.0f); }
            case "BACK" -> { pose.scale(3.5f); pose.translate(-0.15f, -0.65f, 0f); pose.translate(0.15f, 0.375f, 0f); pose.scale(0.6f); }
            case "FACE" -> { pose.scale(2.81f); pose.translate(-0.04f, -0.62f, 0f); pose.translate(-0.4f, -2.0f, 0f); pose.scale(1.5f); }
            case "SUIT" -> { pose.scale(3.73f); pose.translate(-0.01f, -0.68f, 0f); pose.scale(0.375f); pose.translate(0f, 1.5f, 0f); }
            case "SHOES" -> { pose.scale(3.73f); pose.translate(0f, 0.2f, 0f); }
            case "AURA" -> { pose.scale(1.58f); pose.translate(-0.01f, -0.4f, 0f); }
            case "SHIELD" -> { pose.scale(2.7f); pose.scale(0.85f); pose.translate(0.2f, -0.55f, 0f); }
            case "ACCESSORIES" -> { pose.scale(3.3f); pose.translate(0.28f, -1f, 0f); pose.translate(-0.275f, 0.225f, 0f); }
            default -> {}
        }
    }

    private static float rad(float degrees) {
        return (float) Math.toRadians(degrees);
    }

    private void track() {
        if (attached || entity == null) return;
        attached = true;
        LIVE.add(this);
        // Empty preview data: the player's own NoRisk outfit must not show up on the card.
        NrcCosmeticPlayerData data = new NrcCosmeticPlayerData();
        data.setPreview(true);
        ((NrcCosmeticPlayer) (Object) entity).setNrcDataOverride(data);
        long id = entity.getNrcEntityId();
        NrcCosmeticRenderBridge.registerPreviewEntity(id, (NrcCosmeticPlayer) (Object) entity);
        NrcPreviewEntityTracker.addPreviewEntity(id);
        ShopCatalogBridge bridge = ShopCatalog.get();
        if (bridge != null) {
            boolean slim = entity.getSkin().model().toString().equalsIgnoreCase("SLIM");
            bridge.attachPreview(item.id(), id, slim);
        }
    }

    private void untrack() {
        LIVE.remove(this);
        if (!attached) return;
        attached = false;
        long id = entity.getNrcEntityId();
        ShopCatalogBridge bridge = ShopCatalog.get();
        if (bridge != null) bridge.detachPreview(id);
        NrcCosmeticRenderBridge.unregisterPreviewEntity(id);
        NrcPreviewEntityTracker.removePreviewEntity(id);
        ((NrcCosmeticPlayer) (Object) entity).setNrcDataOverride(null);
    }

    /** Drops the preview player; the next draw builds a fresh one. */
    private void release() {
        untrack();
        if (view != null) removeChild(view);
        view = null;
        entity = null;
        shape = null;
    }

    private static void sweep(long now) {
        if (sweeping || now - lastSweep < 250L || LIVE.isEmpty()) return;
        lastSweep = now;
        sweeping = true;
        try {
            for (CatalogPreviewComponent preview : new ArrayList<>(LIVE)) {
                if (now - preview.lastDrawn > IDLE_MS) preview.release();
            }
        } finally {
            sweeping = false;
        }
    }

    @Override
    public void draw(OwoUIGraphics context, int mouseX, int mouseY, float partialTicks, float delta) {
        long now = System.currentTimeMillis();
        lastDrawn = now;
        init();
        if (mounted) track();
        if (!entity.getFrozen() && now - lastTick >= 50L) {
            entity.tickCount++;
            lastTick = now;
        }
        updateError();
        sweep(now);
        super.draw(context, mouseX, mouseY, partialTicks, delta);
    }

    private void updateError() {
        ShopCatalogBridge bridge = ShopCatalog.get();
        String text = bridge == null ? "" : bridge.previewError(item.id());
        if (text == null || text.isEmpty()) {
            if (error != null) {
                removeChild(error);
                error = null;
            }
            return;
        }
        if (error != null) return;
        error = new LabelComponent(TextKt.toSmallCapsText(text), 0.55f);
        error.setBrightnessMultiplier(0.6f);
        error.positioning(Positioning.relative(50, 40));
        child(error);
    }

    @Override
    public void mount(ParentUIComponent parent, double x, double y) {
        super.mount(parent, x, y);
        mounted = true;
    }

    @Override
    public void dismount(UIComponent.DismountReason reason) {
        super.dismount(reason);
        mounted = false;
        if (reason == UIComponent.DismountReason.REMOVED) untrack();
    }
}
