package untamedwilds.entity.mammal;

import com.github.alexthe666.citadel.animation.Animation;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.HurtByTargetGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.BlockParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ToolType;
import untamedwilds.entity.ComplexMob;
import untamedwilds.entity.ComplexMobTerrestrial;
import untamedwilds.entity.INewSkins;
import untamedwilds.entity.ISpecies;
import untamedwilds.entity.ai.*;
import untamedwilds.init.ModEntity;
import untamedwilds.init.ModLootTables;
import untamedwilds.init.ModSounds;
import untamedwilds.util.EntityUtils;

import javax.annotation.Nullable;
import java.util.List;

public class EntityBoar extends ComplexMobTerrestrial implements ISpecies, INewSkins {

    private BlockPos lastDugPos = null;

    public static Animation WORK_DIG;
    public static Animation ATTACK;
    public static Animation TALK;

    public EntityBoar(EntityType<? extends ComplexMob> type, World worldIn) {
        super(type, worldIn);
        this.turn_speed = 0.6F;
        WORK_DIG = Animation.create(48);
        ATTACK = Animation.create(18);
        TALK = Animation.create(20);
    }

    public void registerGoals() {
        this.goalSelector.addGoal(1, new SmartSwimGoal(this));
        this.goalSelector.addGoal(2, new FindItemsGoal(this, 12, 100, false, true));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.6D, false));
        this.goalSelector.addGoal(2, new SmartMateGoal(this, 1D));
        this.goalSelector.addGoal(2, new SmartAvoidGoal<>(this, LivingEntity.class, 16, 1.2D, 1.6D, input -> getEcoLevel(input) > 7));
        this.goalSelector.addGoal(3, new GotoSleepGoal(this, 1));
        this.goalSelector.addGoal(5, new SmartWanderGoal(this, 1D, 120, 20, true));
        this.goalSelector.addGoal(6, new SmartLookAtGoal(this, LivingEntity.class, 10.0F));
        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this)));
    }

    public static AttributeModifierMap.MutableAttribute registerAttributes() {
        return MobEntity.func_233666_p_()
                .createMutableAttribute(Attributes.ATTACK_DAMAGE, 3.0D)
                .createMutableAttribute(Attributes.MOVEMENT_SPEED, 0.2D)
                .createMutableAttribute(Attributes.FOLLOW_RANGE, 24.0D)
                .createMutableAttribute(Attributes.MAX_HEALTH, 16.0D)
                .createMutableAttribute(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .createMutableAttribute(Attributes.ARMOR, 2D);
    }

    public boolean wantsToBreed() {
        if (super.wantsToBreed()) {
            return !this.isSleeping() && this.getGrowingAge() == 0 && EntityUtils.hasFullHealth(this) && this.getHunger() >= 80;
        }
        return false;
    }

    @Override
    public void livingTick() {
        if (!this.world.isRemote) {
            this.setAngry(this.getAttackTarget() != null);
            if (this.ticksExisted % 600 == 0) {
                if (this.wantsToBreed()) {
                    this.setInLove(null);
                }
            }
            if (this.world.getGameTime() % 1000 == 0) {
                this.addHunger(-10);
                if (!this.isStarving()) {
                    this.heal(1.0F);
                }
            }
            // Random idle animations
            if (this.getAnimation() == NO_ANIMATION && this.getAttackTarget() == null && !this.isSleeping() && this.getCommandInt() == 0) {
                int i = this.rand.nextInt(3000);
                if (i == 13 && !this.isInWater() && this.isNotMoving() && this.canMove()) {
                    this.setSitting(true);
                }
                if (i == 14 && this.isSitting()) {
                    this.setSitting(false);
                }
                if (i > 2960 && i < 2979 && !this.isInWater() && !this.isChild()) {
                    this.setAnimation(TALK);
                }
                if (i > 2980 && !this.isInWater() && this.getHunger() < 60 && this.canMove() && this.getAnimation() == NO_ANIMATION) {
                    if ((this.lastDugPos == null || this.getDistanceSq(this.lastDugPos.getX(), this.getPosY(), this.lastDugPos.getZ()) > 50) && this.world.getBlockState(this.getPosition().down()).getHarvestTool() == ToolType.SHOVEL) {
                        this.setAnimation(WORK_DIG);
                        this.addHunger(20);
                        this.lastDugPos = this.getPosition();
                    }
                }
            }
            if (this.getAnimation() == WORK_DIG && this.getAnimationTick() % 8 == 0) {
                ((ServerWorld)this.world).spawnParticle(new BlockParticleData(ParticleTypes.BLOCK, this.world.getBlockState(this.getPosition().down())), this.getPosX(), this.getPosY(), this.getPosZ(), 20, 0.0D, 0.0D, 0.0D, 0.15F);
                this.playSound(SoundEvents.ITEM_SHOVEL_FLATTEN, 0.8F, 0.6F);
                if (this.getAnimationTick() == 64 && this.rand.nextInt(5) == 0) {
                    List<ItemStack> result = EntityUtils.getItemFromLootTable(ModLootTables.LOOT_DIGGING, this.world);
                    for (ItemStack itemstack : result)
                        this.entityDropItem(itemstack);
                }
            }
            if (this.getAnimation() == TALK && this.getAnimationTick() == 1) {
                this.playSound(this.getAmbientSound(), 1.5F, 1);
            }
            if (this.getAttackTarget() != null && this.ticksExisted % 120 == 0) {
                this.playSound(this.getThreatSound(), 1.5F, 1);
            }
            if (this.getAnimation() != NO_ANIMATION) {
                if (this.getAnimation() == ATTACK && this.getAnimationTick() == 8 && this.rand.nextInt(3) == 0) {
                    this.playSound(ModSounds.ENTITY_BOAR_SQUEAL, 1.5F, 1F);
                }
            }
        }
        super.livingTick();
    }

    public ActionResultType func_230254_b_(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getHeldItem(Hand.MAIN_HAND);
        if (hand == Hand.MAIN_HAND && !this.world.isRemote()) {
            if (!this.isTamed() && this.isChild() && EntityUtils.hasFullHealth(this) && this.isFavouriteFood(itemstack)) {
                this.playSound(SoundEvents.ENTITY_HORSE_EAT, 1.5F, 0.8F);
                if (this.getRNG().nextInt(3) == 0) {
                    this.setTamedBy(player);
                    EntityUtils.spawnParticlesOnEntity(this.world, this, ParticleTypes.HEART, 3, 6);
                } else {
                    EntityUtils.spawnParticlesOnEntity(this.world, this, ParticleTypes.SMOKE, 3, 3);
                }
            }
        }

        return super.func_230254_b_(player, hand);
    }

    @Override
    public boolean attackEntityAsMob(Entity entityIn) {
        boolean flag = super.attackEntityAsMob(entityIn);
        if (flag && this.getAnimation() == NO_ANIMATION && !this.isChild()) {
            this.setAnimation(ATTACK);
            this.setAnimationTick(0);
        }
        return flag;
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{NO_ANIMATION, WORK_DIG, ATTACK, TALK};
    }

    public Animation getAnimationEat() { return WORK_DIG; }

    @Nullable
    public EntityBoar func_241840_a(ServerWorld serverWorld, AgeableEntity ageable) {
        return create_offspring(new EntityBoar(ModEntity.BOAR, this.world));
    }

    public void writeAdditional(CompoundNBT compound){
        super.writeAdditional(compound);
        if (this.lastDugPos != null) {
            compound.putInt("DugPosX", this.lastDugPos.getX());
            compound.putInt("DugPosZ", this.lastDugPos.getZ());
        }
    }

    public void readAdditional(CompoundNBT compound) {
        super.readAdditional(compound);
        if (compound.contains("LastDugPos")) {
            this.lastDugPos = new BlockPos(compound.getInt("DugPosX"), 0, compound.getInt("DugPosZ"));
        }
    }
}
