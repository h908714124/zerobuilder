package net.zerobuilder.compiler.analyse;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.BuilderType;
import net.zerobuilder.compiler.generate.GoalContext;
import net.zerobuilder.compiler.generate.ParameterContext;
import net.zerobuilder.compiler.analyse.ProjectionValidator.ValidParameter;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;

import static net.zerobuilder.compiler.Utilities.upcase;

public final class GoalContextFactory {

  static GoalContext context(final ProjectionValidator.ValidationResult validationResult, final BuilderType config,
                             final boolean toBuilder, final boolean builder,
                             final CodeBlock goalCall) throws ValidationException {
    return validationResult.accept(new ProjectionValidator.ValidationResult.ValidationResultCases<GoalContext>() {
      @Override
      GoalContext executableGoal(Analyser.ExecutableGoal goal, ImmutableList<ValidParameter.RegularParameter> validParameters) {
        ClassName contractName = config.generatedType.nestedClass(upcase(goal.name + "Builder"));
        ImmutableList<TypeName> thrownTypes = thrownTypes(goal.executableElement);
        ImmutableList<ParameterContext.ExecutableParameterContext> parameters = parameters(contractName, goal.goalType, validParameters, thrownTypes);
        return new GoalContext.ExecutableGoalContext(
            goal.goalType,
            config,
            toBuilder,
            builder,
            contractName,
            goal.kind,
            goal.name,
            thrownTypes,
            parameters,
            goalCall);
      }
      @Override
      GoalContext beanGoal(Analyser.BeanGoal beanGoal, ImmutableList<ValidParameter.AccessorPair> accessorPairs) {
        ClassName contractName = config.generatedType.nestedClass(upcase(beanGoal.name + "Builder"));
        ImmutableList<ParameterContext.BeansParameterContext> parameters = beanParameters(contractName, beanGoal.goalType, accessorPairs);
        return new GoalContext.BeanGoalContext(
            beanGoal.goalType,
            config,
            toBuilder,
            builder,
            contractName,
            beanGoal.name,
            parameters,
            goalCall);
      }
    });
  }

  private static ImmutableList<ParameterContext.ExecutableParameterContext> parameters(ClassName builderType, TypeName returnType,
                                                                                       ImmutableList<ValidParameter.RegularParameter> parameters, ImmutableList<TypeName> thrownTypes) {
    ImmutableList.Builder<ParameterContext.ExecutableParameterContext> builder = ImmutableList.builder();
    for (int i = parameters.size() - 1; i >= 0; i--) {
      ValidParameter.RegularParameter parameter = parameters.get(i);
      ClassName stepContract = builderType.nestedClass(
          upcase(parameter.name));
      builder.add(new ParameterContext.ExecutableParameterContext(stepContract, returnType, parameter, thrownTypes));
      returnType = stepContract;
    }
    return builder.build().reverse();
  }

  private static ImmutableList<ParameterContext.BeansParameterContext> beanParameters(ClassName builderType, TypeName returnType,
                                                                                      ImmutableList<ValidParameter.AccessorPair> parameters) {
    ImmutableList.Builder<ParameterContext.BeansParameterContext> builder = ImmutableList.builder();
    for (int i = parameters.size() - 1; i >= 0; i--) {
      ValidParameter.AccessorPair parameter = parameters.get(i);
      ClassName stepContract = builderType.nestedClass(
          upcase(parameter.name));
      builder.add(new ParameterContext.BeansParameterContext(stepContract, returnType, parameter));
      returnType = stepContract;
    }
    return builder.build().reverse();
  }

  // beanGoal goals don't have a kind
  public enum GoalKind {
    CONSTRUCTOR, STATIC_METHOD, INSTANCE_METHOD
  }

  enum Visibility {
    PUBLIC, PACKAGE
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