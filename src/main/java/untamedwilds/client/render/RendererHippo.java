package untamedwilds.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.util.ResourceLocation;
import untamedwilds.client.model.ModelHippo;
import untamedwilds.client.model.ModelHippoCalf;
import untamedwilds.entity.mammal.EntityHippo;

import javax.annotation.Nonnull;

public class RendererHippo extends MobRenderer<EntityHippo, EntityModel<EntityHippo>> {

    private static final ModelHippo HIPPO_MODEL = new ModelHippo();
    private static final ModelHippoCalf HIPPO_MODEL_CALF = new ModelHippoCalf();

    public RendererHippo(EntityRendererManager renderManager) {
        super(renderManager, HIPPO_MODEL, 1F);
    }

    @Override
    public void render(EntityHippo entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn) {
        if (entityIn.isChild()) {
            entityModel = HIPPO_MODEL_CALF;
        } else {
            entityModel = HIPPO_MODEL;
        }
        super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
    }

    protected void preRenderCallback(EntityHippo entity, MatrixStack matrixStackIn, float partialTickTime) {
        float f = entity.getMobSize();
        //f *= entity.getRenderScale();
        matrixStackIn.scale(f, f, f);
        this.shadowSize = f;
    }

    public ResourceLocation getEntityTexture(@Nonnull EntityHippo entity) {
        return entity.getTexture();
    }
}
