package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoContext.GoalContext;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractGoalDetails;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.RegularGoalContext;

import java.util.function.Function;

import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.projectedRegularGoalContextCases;
import static net.zerobuilder.compiler.generate.DtoRegularGoalContext.regularGoalContextCases;

public final class DtoGoalContext {

  public static abstract class AbstractGoalContext {

    abstract <R> R accept(GoalCases<R> cases);

    public final String name() {
      return goalName.apply(this);
    }

    public final Boolean mayReuse() {
      return mayReuse.apply(this);
    }

    public final GoalContext context() {
      return context.apply(this);
    }
  }

  interface GoalCases<R> {
    R regularGoal(RegularGoalContext goal);
    R beanGoal(BeanGoalContext goal);
  }

  static <R> Function<AbstractGoalContext, R> asFunction(final GoalCases<R> cases) {
    return goal -> goal.accept(cases);
  }

  static <R> Function<AbstractGoalContext, R> goalCases(
      Function<? super RegularGoalContext, ? extends R> regularFunction,
      Function<? super BeanGoalContext, ? extends R> beanFunction) {
    return asFunction(new GoalCases<R>() {
      @Override
      public R regularGoal(RegularGoalContext goal) {
        return regularFunction.apply(goal);
      }
      @Override
      public R beanGoal(BeanGoalContext goal) {
        return beanFunction.apply(goal);
      }
    });
  }

  static final Function<RegularGoalContext, GoalContext> regularContext =
      regularGoalContextCases(
          DtoRegularGoal.regularGoalContextCases(
              constructor -> constructor.context,
              method -> method.context,
              staticMethod -> staticMethod.context),
          projectedRegularGoalContextCases(
              method -> method.context,
              constructor -> constructor.context));

  public static final Function<AbstractGoalContext, GoalContext> context =
      goalCases(
          regularContext,
          bean -> bean.context);

  private static final Function<ProjectedRegularGoalContext, AbstractGoalDetails> PROJECTED_REGULAR_GOAL_CONTEXT_R_FUNCTION = projectedRegularGoalContextCases(
      method -> method.details,
      constructor -> constructor.details);

  private static final Function<RegularGoalContext, AbstractGoalDetails> REGULAR_GOAL_CONTEXT_R_FUNCTION =
      regularGoalContextCases(
          DtoRegularGoal.goalDetails,
          PROJECTED_REGULAR_GOAL_CONTEXT_R_FUNCTION);

  private static final Function<AbstractGoalContext, AbstractGoalDetails> abstractGoalDetails =
      goalCases(
          REGULAR_GOAL_CONTEXT_R_FUNCTION,
          bean -> bean.details);

  private static final Function<AbstractGoalContext, TypeName> goalType =
      goalCases(
          regularGoalContextCases(
              DtoRegularGoal.regularGoalContextCases(
                  constructor -> constructor.details.goalType,
                  method -> method.details.goalType,
                  staticMethod -> staticMethod.details.goalType),
              projectedRegularGoalContextCases(
                  method -> method.details.goalType,
                  constructor -> constructor.details.goalType)),
          bean -> bean.details.goalType);

  private static final Function<AbstractGoalContext, String> goalName =
      goalCases(
          regularGoalContextCases(
              DtoRegularGoal.regularGoalContextCases(
                  constructor -> constructor.details.name,
                  method -> method.details.name,
                  staticMethod -> staticMethod.details.name),
              projectedRegularGoalContextCases(
                  method -> method.details.name,
                  constructor -> constructor.details.name)),
          bean -> bean.details.name);

  private static final Function<AbstractGoalContext, Boolean> mayReuse =
      goalCases(
          DtoRegularGoalContext.mayReuse,
          bean -> bean.context.lifecycle == REUSE_INSTANCES);

  private DtoGoalContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
