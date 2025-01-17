package untamedwilds.entity.mollusk;

import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShovelItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.server.ServerWorld;
import untamedwilds.config.ConfigGamerules;
import untamedwilds.entity.ComplexMob;
import untamedwilds.entity.INewSkins;
import untamedwilds.entity.ISpecies;
import untamedwilds.util.EntityUtils;
import untamedwilds.util.SpeciesDataHolder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EntityGiantClam extends ComplexMob implements ISpecies, INewSkins {

    private static final DataParameter<Boolean> CLAM_OPEN = EntityDataManager.createKey(EntityGiantClam.class, DataSerializers.BOOLEAN);
    public int closeProgress;

    public EntityGiantClam(EntityType<? extends ComplexMob> type, World worldIn) {
        super(type, worldIn);
        this.setPathPriority(PathNodeType.WATER, 0.0F);
    }

    public static AttributeModifierMap.MutableAttribute registerAttributes() {
        return MobEntity.func_233666_p_()
                .createMutableAttribute(Attributes.ATTACK_DAMAGE, 0D)
                .createMutableAttribute(Attributes.MOVEMENT_SPEED, 0D)
                .createMutableAttribute(Attributes.FOLLOW_RANGE, 1D)
                .createMutableAttribute(Attributes.MAX_HEALTH, 20.0D)
                .createMutableAttribute(Attributes.KNOCKBACK_RESISTANCE, 1D)
                .createMutableAttribute(Attributes.ARMOR, 12D);
    }

    @Override
    protected void registerData() {
        super.registerData();
        //this.dataManager.register(CLAM_OPEN, true);
        this.entityCollisionReduction = 1F;
    }

    public void applyEntityCollision(Entity entityIn) {
    }

    public float getCollisionBorderSize() {
        return 0.0F;
    }

    @Override
    public void baseTick() {
        int i = this.getAir();
        super.baseTick();
        if (this.isAlive() && !this.isInWaterOrBubbleColumn()) {
            --i;
            this.setAir(i);

            if (this.getAir() == -20) {
                this.setAir(0);
                this.attackEntityFrom(DamageSource.DROWN, 2.0F);
            }
        }
        else {
            this.setAir(300);
        }
    }

    public void livingTick() {
        super.livingTick();
        // The following locks the X and Z position to zero, preventing the entity from being pushed
        this.setMotion(0, this.getMotion().getY(), 0);
        if (!this.world.isRemote) {
            if (this.ticksExisted % 1000 == 0) {
                if (this.wantsToBreed()) {
                    this.breed();
                }
            }
            this.setOpen(this.world.isDaytime());
        }
        if (this.world.isRemote) {
            if (!this.isOpen() && this.closeProgress < 200) {
                this.closeProgress++;
            } else if (this.isOpen() && this.closeProgress > 0) {
                this.closeProgress--;
            }
        }
    }

    /* Breeding conditions for the Giant Clam are:
     * A nearby Giant Clam of the same species, being hermaphrodites, they do not take Gender into account */
    public boolean wantsToBreed() {
        if (ConfigGamerules.naturalBreeding.get() && this.getGrowingAge() == 0 && EntityUtils.hasFullHealth(this)) {
            List<EntityGiantClam> list = this.world.getEntitiesWithinAABB(EntityGiantClam.class, this.getBoundingBox().grow(12.0D, 6.0D, 12.0D));
            list.removeIf(input -> input == this || input.getGrowingAge() != 0 || input.getVariant() != this.getVariant());
            if (list.size() >= 1) {
                this.setGrowingAge(this.getGrowingAge());
                list.get(0).setGrowingAge(this.getGrowingAge());
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public AgeableEntity func_241840_a(ServerWorld serverWorld, AgeableEntity ageableEntity) {
        EntityUtils.dropEggs(this, "egg_giant_clam", this.getOffspring());
        return null;
    }

    @Override
    public ActionResultType func_230254_b_(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getHeldItem(Hand.MAIN_HAND);

        if (itemstack.getItem() instanceof ShovelItem && this.isAlive() && hand == Hand.MAIN_HAND) {
            if (this.rand.nextInt(4) == 0) {
                world.playSound(null, this.getPosition(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.BLOCKS, 1.0F, 0.8F);
                EntityUtils.turnEntityIntoItem(this, "spawn_giant_clam");
                return ActionResultType.func_233537_a_(this.world.isRemote);
            }
            else {
                world.playSound(null, this.getPosition(), SoundEvents.ENTITY_SHULKER_HURT_CLOSED, SoundCategory.BLOCKS, 1.0F, 0.8F);
                EntityUtils.spawnParticlesOnEntity(world, this, ParticleTypes.SMOKE, 3, 1);
            }
        }
        return super.func_230254_b_(player, hand);
    }

    // TODO: Again, move to ComplexMob
    @Override
    public int setSpeciesByBiome(RegistryKey<Biome> biomekey, Biome biome, SpawnReason reason) {
        if (biomekey.equals(Biomes.WARM_OCEAN) || biomekey.equals(Biomes.LUKEWARM_OCEAN) || biomekey.equals(Biomes.DEEP_WARM_OCEAN) || biome.getRegistryName().equals(new ResourceLocation("terraforged:warm_beach"))) {
            if (ConfigGamerules.randomSpecies.get() || isArtificialSpawnReason(reason)) {
                return this.getRNG().nextInt(getEntityData(this.getType()).getSpeciesData().size());
            }
            List<Integer> validTypes = new ArrayList<>();
            if (ComplexMob.ENTITY_DATA_HASH.containsKey(this.getType())) {
                for (SpeciesDataHolder speciesDatum : getEntityData(this.getType()).getSpeciesData()) {
                    for(Biome.Category biomeTypes : speciesDatum.getBiomeCategories()) {
                        if(biome.getCategory() == biomeTypes){
                            for (int i=0; i < speciesDatum.getRarity(); i++) {
                                validTypes.add(speciesDatum.getVariant());
                            }
                        }
                    }
                }
                if (validTypes.isEmpty()) {
                    return 99;
                } else {
                    return validTypes.get(new Random().nextInt(validTypes.size()));
                }
            }
        }
        if (isArtificialSpawnReason(reason)) {
            return this.rand.nextInt(getEntityData(this.getType()).getSpeciesData().size());
        }
        return 99;
    }

    public boolean canBeTargeted() { return false; }
    private boolean isOpen(){ return (this.dataManager.get(CLAM_OPEN)); }
    private void setOpen(boolean open){ this.dataManager.set(CLAM_OPEN, open); }

    public void writeAdditional(CompoundNBT compound){ // Write NBT Tags
        super.writeAdditional(compound);
        compound.putBoolean("isOpen", this.isOpen());
    }

    public void readAdditional(CompoundNBT compound){ // Read NBT Tags
        super.readAdditional(compound);
        this.setOpen(compound.getBoolean("isOpen"));
    }
}