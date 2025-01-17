package untamedwilds.entity.ai.target;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPredicate;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.TargetGoal;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.util.math.AxisAlignedBB;
import untamedwilds.entity.ComplexMob;
import untamedwilds.entity.ComplexMobTerrestrial;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

public class HuntMobTarget<T extends LivingEntity> extends TargetGoal {
    protected final Class<T> targetClass;
    protected final Sorter sorter;
    protected Predicate<? super T> targetEntitySelector;
    protected T targetEntity;
    private final int threshold;
    private final boolean isCannibal;

    public HuntMobTarget(ComplexMob creature, Class<T> classTarget, boolean checkSight, boolean isCannibal, final Predicate<? super T > targetSelector) {
        this(creature, classTarget, checkSight, 200, isCannibal, targetSelector);
    }

    public HuntMobTarget(ComplexMob creature, Class<T> classTarget, boolean checkSight, int hungerThreshold, boolean isCannibal, final Predicate<? super T> targetSelector) {
        super(creature, checkSight, true);
        this.targetClass = classTarget;
        this.sorter = new Sorter(creature);
        this.setMutexFlags(EnumSet.of(Flag.TARGET));
        this.threshold = hungerThreshold;
        this.isCannibal = isCannibal;
        this.targetEntitySelector = (Predicate<T>) entity -> {
            if (targetSelector != null && !targetSelector.test(entity)) {
                return false;
            }
            return this.isSuitableTarget(entity, EntityPredicate.DEFAULT);
        };
    }

    public boolean shouldExecute() {
        if (this.goalOwner.isChild() || this.goalOwner.getHealth() < this.goalOwner.getMaxHealth() / 3) {
            return false;
        }
        if (this.goalOwner instanceof ComplexMob) {
            if (((ComplexMob)this.goalOwner).peacefulTicks != 0) {
                return false;
            }
            if (this.goalOwner instanceof ComplexMobTerrestrial) {
                ComplexMobTerrestrial temp = (ComplexMobTerrestrial) this.goalOwner;
                if (temp.isTamed() || temp.getHunger() > this.threshold) {
                    return false;
                }
            }
        }
        List<T> list = this.goalOwner.world.getEntitiesWithinAABB(this.targetClass, this.getTargetableArea(this.getTargetDistance()), this.targetEntitySelector);
        list.removeIf((Predicate<LivingEntity>) this::shouldRemoveTarget);

        if (list.isEmpty()) {
            return false;
        }
        else {
            list.sort(this.sorter);
            this.targetEntity = list.get(0);
            if (this.goalOwner instanceof ComplexMob) {
                ((ComplexMob)this.goalOwner).peacefulTicks = 6000;
            }
            return true;
        }
    }

    AxisAlignedBB getTargetableArea(double targetDistance) {
        return this.goalOwner.getBoundingBox().grow(targetDistance, 4.0D, targetDistance);
    }

    public void startExecuting() {
        this.goalOwner.setAttackTarget(this.targetEntity);
        super.startExecuting();
    }

    public boolean shouldRemoveTarget(LivingEntity entity) {
        if (entity instanceof CreeperEntity) {
            return false; // Hardcoded Creepers out because they will absolutely destroy wildlife if targeted
        }
        if (!this.isCannibal) {
            if (entity instanceof ComplexMob) {
                ComplexMob ctarget = (ComplexMob)entity;
                return (goalOwner.getClass() == entity.getClass() && ((ComplexMob)goalOwner).getVariant() == ctarget.getVariant()) || !ctarget.canBeTargeted();
            }
        }
        return false;
    }

    public static class Sorter implements Comparator<Entity> {
        private final Entity entity;

        private Sorter(Entity entityIn)
        {
            this.entity = entityIn;
        }

        public int compare(Entity entity_1, Entity entity_2) {
            double dist_1 = this.entity.getDistanceSq(entity_1);
            double dist_2 = this.entity.getDistanceSq(entity_2);

            if (dist_1 < dist_2) {
                return -1;
            }
            else {
                return dist_1 > dist_2 ? 1 : 0;
            }
        }
    }
}
