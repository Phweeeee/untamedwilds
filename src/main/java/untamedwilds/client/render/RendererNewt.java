package untamedwilds.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.util.ResourceLocation;
import untamedwilds.client.model.ModelNewt;
import untamedwilds.entity.amphibian.EntityNewt;

import javax.annotation.Nonnull;

public class RendererNewt extends MobRenderer<EntityNewt, EntityModel<EntityNewt>> {

    private static final ModelNewt NEWT_MODEL = new ModelNewt();

    public RendererNewt(EntityRendererManager renderManager) {
        super(renderManager, NEWT_MODEL, 0.1F);
    }

    @Override
    protected void preRenderCallback(EntityNewt entity, MatrixStack matrixStackIn, float partialTickTime) {
        float f = entity.getMobSize();
        f *= entity.getRenderScale();
        matrixStackIn.scale(f, f, f);
        this.shadowSize = f * 0.2F;
    }

    public ResourceLocation getEntityTexture(@Nonnull EntityNewt entity) {
        return entity.getTexture();
    }
}
