package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.ConstructorGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.StaticMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoProjectedGoal.ProjectedGoal;
import net.zerobuilder.compiler.generate.DtoProjectedGoal.ProjectedGoalCases;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.RegularGoalContext;

import java.util.List;
import java.util.function.Function;

import static com.squareup.javapoet.TypeName.VOID;
import static net.zerobuilder.compiler.generate.DtoRegularStep.ProjectedRegularStep;
import static net.zerobuilder.compiler.generate.ZeroUtil.emptyCodeBlock;

public final class DtoProjectedRegularGoalContext {

  interface ProjectedRegularGoalContextCases<R> {
    R method(ProjectedMethodGoalContext method);
    R constructor(ProjectedConstructorGoalContext constructor);
  }

  public static abstract class ProjectedRegularGoalContext extends RegularGoalContext
      implements ProjectedGoal {

    ProjectedRegularGoalContext(List<TypeName> thrownTypes) {
      super(thrownTypes);
    }

    abstract <R> R acceptRegularProjected(ProjectedRegularGoalContextCases<R> cases);

    @Override
    public final <R> R acceptProjected(ProjectedGoalCases<R> cases) {
      return cases.regular(this);
    }

    @Override
    <R> R acceptRegular(DtoRegularGoalContext.RegularGoalContextCases<R> cases) {
      return cases.projected(this);
    }

    public final CodeBlock invocationParameters() {
      return CodeBlock.of(String.join(", ", goalDetails.apply(this).parameterNames));
    }
  }

  static <R> Function<ProjectedRegularGoalContext, R> asFunction(ProjectedRegularGoalContextCases<R> cases) {
    return goal -> goal.acceptRegularProjected(cases);
  }

  public static <R> Function<ProjectedRegularGoalContext, R> projectedRegularGoalContextCases(
      Function<? super ProjectedMethodGoalContext, ? extends R> methodFunction,
      Function<? super ProjectedConstructorGoalContext, ? extends R> constructorFunction) {
    return asFunction(new ProjectedRegularGoalContextCases<R>() {
      @Override
      public R method(ProjectedMethodGoalContext method) {
        return methodFunction.apply(method);
      }
      @Override
      public R constructor(ProjectedConstructorGoalContext constructor) {
        return constructorFunction.apply(constructor);
      }
    });
  }

  public static final class ProjectedMethodGoalContext extends ProjectedRegularGoalContext {
    final List<ProjectedRegularStep> steps;
    final BuildersContext context;
    final StaticMethodGoalDetails details;

    public CodeBlock methodGoalInvocation() {
      TypeName type = details.goalType;
      String method = details.methodName;
      return CodeBlock.builder()
          .add(VOID.equals(type) ? emptyCodeBlock : CodeBlock.of("return "))
          .addStatement("$T.$N($L)", context.type, method, invocationParameters())
          .build();
    }

    ProjectedMethodGoalContext(
        BuildersContext context,
        StaticMethodGoalDetails details,
        List<ProjectedRegularStep> steps,
        List<TypeName> thrownTypes) {
      super(thrownTypes);
      this.context = context;
      this.details = details;
      this.steps = steps;
    }

    @Override
    public <R> R acceptRegularProjected(ProjectedRegularGoalContextCases<R> cases) {
      return cases.method(this);
    }
  }

  public static final class ProjectedConstructorGoalContext
      extends ProjectedRegularGoalContext {

    final BuildersContext context;
    final ConstructorGoalDetails details;
    final List<ProjectedRegularStep> steps;

    ProjectedConstructorGoalContext(BuildersContext context,
                                    ConstructorGoalDetails details,
                                    List<ProjectedRegularStep> steps,
                                    List<TypeName> thrownTypes) {
      super(thrownTypes);
      this.context = context;
      this.details = details;
      this.steps = steps;
    }

    @Override
    public <R> R acceptRegularProjected(ProjectedRegularGoalContextCases<R> cases) {
      return cases.constructor(this);
    }
  }

  public static final Function<ProjectedRegularGoalContext, AbstractRegularDetails> goalDetails =
      projectedRegularGoalContextCases(
          method -> method.details,
          constructor -> constructor.details);

  public static final Function<ProjectedRegularGoalContext, List<ProjectedRegularStep>> steps = DtoProjectedRegularGoalContext.projectedRegularGoalContextCases(
      method -> method.steps,
      constructor -> constructor.steps);

  private DtoProjectedRegularGoalContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
