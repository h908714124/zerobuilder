package net.zerobuilder.compiler.analyse;

import net.zerobuilder.AccessLevel;
import net.zerobuilder.GoalName;
import net.zerobuilder.Level;
import net.zerobuilder.Recycle;
import net.zerobuilder.compiler.generate.Access;
import net.zerobuilder.compiler.generate.DtoContext;

import javax.lang.model.element.ExecutableElement;

import static net.zerobuilder.compiler.analyse.DtoGoalElement.goalType;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.simpleName;

final class GoalModifiers {

  final Access access;
  final DtoContext.ContextLifecycle lifecycle;
  final String goalName;

  private GoalModifiers(Access access, DtoContext.ContextLifecycle lifecycle, String goalName) {
    this.access = access;
    this.lifecycle = lifecycle;
    this.goalName = goalName;
  }

  private static Access getAccess(ExecutableElement element) {
    AccessLevel accessLevel = element.getAnnotation(AccessLevel.class);
    if (accessLevel != null &&
        accessLevel.value() == Level.PACKAGE) {
      return Access.PACKAGE;
    }
    return Access.PUBLIC;
  }

  static GoalModifiers create(ExecutableElement element) {
    Access access = getAccess(element);
    DtoContext.ContextLifecycle lifecycle = element.getAnnotation(Recycle.class) == null ?
        DtoContext.ContextLifecycle.NEW_INSTANCE :
        DtoContext.ContextLifecycle.REUSE_INSTANCES;
    String goalName = element.getAnnotation(GoalName.class) == null ?
        downcase(simpleName(goalType(element))) :
        element.getAnnotation(GoalName.class).value();
    return new GoalModifiers(access, lifecycle, goalName);
  }
}
