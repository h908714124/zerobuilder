package net.zerobuilder.compiler.analyse;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.analyse.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.analyse.DtoBeanParameter.AccessorPair;
import net.zerobuilder.compiler.analyse.DtoBeanParameter.LoneGetter;
import net.zerobuilder.compiler.analyse.DtoGoal.AbstractGoal;
import net.zerobuilder.compiler.analyse.DtoGoal.ConstructorGoal;
import net.zerobuilder.compiler.analyse.DtoGoal.MethodGoal;
import net.zerobuilder.compiler.analyse.DtoGoal.RegularGoalCases;
import net.zerobuilder.compiler.analyse.DtoParameter.AbstractParameter;
import net.zerobuilder.compiler.analyse.DtoParameter.RegularParameter;
import net.zerobuilder.compiler.analyse.DtoValidGoal.ValidBeanGoal;
import net.zerobuilder.compiler.analyse.DtoValidGoal.ValidGoal;
import net.zerobuilder.compiler.analyse.DtoValidGoal.ValidGoalCases;
import net.zerobuilder.compiler.analyse.DtoValidGoal.ValidRegularGoal;
import net.zerobuilder.compiler.generate.DtoBeanGoalContext.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoBeanStep.AbstractBeanStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.AccessorPairStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.LoneGetterStep;
import net.zerobuilder.compiler.generate.DtoBuilders.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.ConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.MethodGoalContext;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;
import net.zerobuilder.compiler.generate.DtoStep.RegularStep;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;

import static net.zerobuilder.compiler.Utilities.upcase;
import static net.zerobuilder.compiler.analyse.DtoParameter.parameterName;

final class GoalContextFactory {

  static AbstractGoalContext context(final ValidGoal validGoal, final BuildersContext builders,
                                     final boolean toBuilder, final boolean builder) throws ValidationException {
    return validGoal.accept(new ValidGoalCases<AbstractGoalContext>() {
      @Override
      public AbstractGoalContext regularGoal(ValidRegularGoal goal) {
        final ClassName contractName = contractName(goal.goal.goal, builders);
        final ImmutableList<TypeName> thrownTypes = thrownTypes(goal.goal.executableElement);
        final ImmutableList<RegularStep> steps = steps(contractName,
            goal.goal.goal.goalType,
            goal.parameters,
            thrownTypes,
            regularParameterFactory);
        return goal.goal.goal.accept(new RegularGoalCases<AbstractGoalContext>() {
          @Override
          public AbstractGoalContext method(MethodGoal goal) {
            return new MethodGoalContext(
                goal, builders, toBuilder, builder, contractName, steps, thrownTypes);
          }
          @Override
          public AbstractGoalContext constructor(ConstructorGoal goal) {
            return new ConstructorGoalContext(
                goal, builders, toBuilder, builder, contractName, steps, thrownTypes);
          }
        });
      }
      @Override
      public AbstractGoalContext beanGoal(ValidBeanGoal goal) {
        ClassName contractName = contractName(goal.goal.goal, builders);
        ImmutableList<? extends AbstractBeanStep> steps = steps(contractName,
            goal.goal.goal.goalType,
            goal.parameters,
            ImmutableList.<TypeName>of(),
            beansParameterFactory);
        return BeanGoalContext.create(
            goal.goal.goal, builders, toBuilder, builder, contractName, steps);
      }
    });
  }

  private static <P extends AbstractParameter, R extends AbstractStep>
  ImmutableList<R> steps(ClassName builderType,
                         TypeName nextType,
                         ImmutableList<P> parameters,
                         ImmutableList<TypeName> thrownTypes,
                         ParameterFactory<P, R> parameterFactory) {
    ImmutableList.Builder<R> builder = ImmutableList.builder();
    for (P parameter : parameters.reverse()) {
      ClassName thisType = builderType.nestedClass(upcase(parameterName.apply(parameter)));
      builder.add(parameterFactory.create(thisType, nextType, parameter, thrownTypes));
      nextType = thisType;
    }
    return builder.build().reverse();
  }

  private static abstract class ParameterFactory<P extends AbstractParameter, R extends AbstractStep> {
    abstract R create(ClassName typeThisStep, TypeName typeNextStep, P parameter, ImmutableList<TypeName> declaredExceptions);
  }

  private static final ParameterFactory<AbstractBeanParameter, ? extends AbstractBeanStep> beansParameterFactory
      = new ParameterFactory<AbstractBeanParameter, AbstractBeanStep>() {
    @Override
    AbstractBeanStep create(final ClassName thisType, final TypeName nextType, final AbstractBeanParameter validParameter, ImmutableList<TypeName> declaredExceptions) {
      return validParameter.accept(new DtoBeanParameter.BeanParameterCases<AbstractBeanStep>() {
        @Override
        public AbstractBeanStep accessorPair(AccessorPair pair) {
          String setter = "set" + upcase(parameterName.apply(pair));
          return AccessorPairStep.create(thisType, nextType, pair, setter);
        }
        @Override
        public AbstractBeanStep loneGetter(LoneGetter loneGetter) {
          return LoneGetterStep.create(thisType, nextType, loneGetter);
        }
      });
    }
  };

  private static final ParameterFactory<RegularParameter, RegularStep> regularParameterFactory
      = new ParameterFactory<RegularParameter, RegularStep>() {
    @Override
    RegularStep create(ClassName thisType, TypeName nextType, RegularParameter validParameter, ImmutableList<TypeName> declaredExceptions) {
      return RegularStep.create(thisType, nextType, validParameter, declaredExceptions);
    }
  };

  private static ClassName contractName(AbstractGoal goal, BuildersContext config) {
    return config.generatedType.nestedClass(upcase(goal.name + "Builder"));
  }

  private static ImmutableList<TypeName> thrownTypes(ExecutableElement executableElement) {
    return FluentIterable
        .from(executableElement.getThrownTypes())
        .transform(new Function<TypeMirror, TypeName>() {
          @Override
          public TypeName apply(TypeMirror thrownType) {
            return TypeName.get(thrownType);
          }
        })
        .toList();
  }

  private GoalContextFactory() {
    throw new UnsupportedOperationException("no instances");
  }
}
