package untamedwilds.entity.ai;

import net.minecraft.entity.EntityPredicate;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.EntityPredicates;
import net.minecraft.util.math.vector.Vector3d;
import untamedwilds.entity.ComplexMob;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

public class SmartAvoidGoal <T extends LivingEntity> extends AvoidEntityGoal<T> {

    protected ComplexMob taskOwner;
    protected final float avoidDistance;
    private final EntityPredicate builtTargetSelector;

    public SmartAvoidGoal(ComplexMob entityIn, Class<T> classToAvoidIn, float avoidDistanceIn, double farSpeedIn, double nearSpeedIn, final Predicate<LivingEntity> targetSelector) {
        super(entityIn, classToAvoidIn, avoidDistanceIn, farSpeedIn, nearSpeedIn, EntityPredicates.CAN_AI_TARGET::test);
        this.taskOwner = entityIn;
        this.avoidDistance = avoidDistanceIn;
        this.builtTargetSelector = (new EntityPredicate()).setDistance(avoidDistanceIn).setCustomPredicate(targetSelector);
        this.setMutexFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean shouldExecute() {
        if (this.taskOwner.ticksExisted % 40 != 0) {
            return false;
        }
        if (this.taskOwner.getAttackTarget() != null || this.taskOwner.isSleeping() || this.taskOwner.getCommandInt() != 0 || this.taskOwner.isTamed()) {
            return false;
        }

        List<T> list = this.taskOwner.world.getTargettableEntitiesWithinAABB(classToAvoid, this.builtTargetSelector, this.taskOwner, this.taskOwner.getBoundingBox().grow(avoidDistance, 4f, avoidDistance));
        if (list.isEmpty()) {
            return false;
        } else {
            this.avoidTarget = list.get(0);
            Vector3d vec3d = RandomPositionGenerator.findRandomTargetBlockAwayFrom(this.taskOwner, 16, 7, new Vector3d(this.avoidTarget.getPosX(), this.avoidTarget.getPosY(), this.avoidTarget.getPosZ()));
            if (vec3d == null) {
                return false;
            } else if (this.avoidTarget.getDistanceSq(vec3d.x, vec3d.y, vec3d.z) < this.avoidTarget.getDistanceSq(this.entity)) {
                return false;
            } else {
                this.path = this.navigation.getPathToPos(vec3d.x, vec3d.y, vec3d.z, 0);
                return this.path != null;
            }
        }
    }
}