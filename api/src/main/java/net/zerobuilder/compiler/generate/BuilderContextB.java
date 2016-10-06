package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanGoalContext.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoBeanStep.AbstractBeanStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.AccessorPairStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.BeanStepCases;
import net.zerobuilder.compiler.generate.DtoBeanStep.LoneGetterStep;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.presentInstances;
import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Iterables.getLast;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.WildcardTypeName.subtypeOf;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.Utilities.ClassNames.ITERABLE;
import static net.zerobuilder.compiler.generate.Utilities.nullCheck;
import static net.zerobuilder.compiler.generate.Utilities.parameterSpec;
import static net.zerobuilder.compiler.generate.Utilities.statement;
import static net.zerobuilder.compiler.generate.DtoBeanParameter.beanParameterName;
import static net.zerobuilder.compiler.generate.DtoBeanStep.asFunction;
import static net.zerobuilder.compiler.generate.StepContext.nullCheck;

final class BuilderContextB {

  static final Function<BeanGoalContext, ImmutableList<FieldSpec>> fields
      = new Function<BeanGoalContext, ImmutableList<FieldSpec>>() {
    @Override
    public ImmutableList<FieldSpec> apply(BeanGoalContext goal) {
      return ImmutableList.of(goal.goal.field);
    }
  };

  static final Function<BeanGoalContext, ImmutableList<MethodSpec>> steps
      = new Function<BeanGoalContext, ImmutableList<MethodSpec>>() {
    @Override
    public ImmutableList<MethodSpec> apply(BeanGoalContext goal) {
      ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
      Function<AbstractBeanStep, ImmutableList<MethodSpec>> stepToMethods = stepToMethods(goal, false);
      for (AbstractBeanStep step : goal.goal.steps.subList(0, goal.goal.steps.size() - 1)) {
        builder.addAll(stepToMethods.apply(step));
      }
      builder.addAll(stepToMethods(goal, true).apply(getLast(goal.goal.steps)));
      return builder.build();
    }
  };

  private static Function<AbstractBeanStep, ImmutableList<MethodSpec>>
  stepToMethods(final BeanGoalContext goal, final boolean isLast) {
    return asFunction(new BeanStepCases<ImmutableList<MethodSpec>>() {
      @Override
      public ImmutableList<MethodSpec> accessorPair(AccessorPairStep step) {
        return regularMethods(step, goal, isLast);
      }
      @Override
      public ImmutableList<MethodSpec> loneGetter(LoneGetterStep step) {
        return collectionMethods(step, goal, isLast);
      }
    });
  }

  private static ImmutableList<MethodSpec> regularMethods(AccessorPairStep step, BeanGoalContext goal, boolean isLast) {
    ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
    builder.add(regularStep(step, goal, isLast));
    builder.addAll(presentInstances(of(regularEmptyCollection(step, goal, isLast))));
    return builder.build();
  }

  private static Optional<MethodSpec> regularEmptyCollection(AccessorPairStep step, BeanGoalContext goal, boolean isLast) {
    Optional<DtoStep.EmptyOption> maybeEmptyOption = step.emptyOption();
    if (!maybeEmptyOption.isPresent()) {
      return absent();
    }
    DtoStep.EmptyOption emptyOption = maybeEmptyOption.get();
    TypeName type = step.accessorPair.type;
    String name = step.accessorPair.accept(beanParameterName);
    ParameterSpec emptyColl = parameterSpec(type, name);
    return Optional.of(methodBuilder(emptyOption.name)
        .addAnnotation(Override.class)
        .returns(step.nextType)
        .addStatement("$T $N = $L", emptyColl.type, emptyColl, emptyOption.initializer)
        .addStatement("this.$N.$L($N)", goal.goal.field, step.setter, emptyColl)
        .addCode(regularFinalBlock(goal, isLast))
        .addModifiers(PUBLIC)
        .build());
  }

  private static ImmutableList<MethodSpec> collectionMethods(LoneGetterStep step, BeanGoalContext goal, boolean isLast) {
    return ImmutableList.of(
        iterateCollection(step, goal, isLast),
        loneGetterEmptyCollection(step, goal, isLast));
  }

  private static MethodSpec loneGetterEmptyCollection(LoneGetterStep step, BeanGoalContext goal, boolean isLast) {
    return methodBuilder(step.emptyMethod)
        .addAnnotation(Override.class)
        .returns(step.nextType)
        .addCode(regularFinalBlock(goal, isLast))
        .addModifiers(PUBLIC)
        .build();
  }

  private static CodeBlock regularFinalBlock(BeanGoalContext goal, boolean isLast) {
    return isLast
        ? statement("return this.$N", goal.goal.field)
        : statement("return this");
  }

  private static MethodSpec iterateCollection(LoneGetterStep step,
                                              BeanGoalContext goal,
                                              boolean isLast) {
    String name = step.loneGetter.accept(beanParameterName);
    ParameterizedTypeName iterable = ParameterizedTypeName.get(ITERABLE,
        subtypeOf(step.loneGetter.iterationType()));
    ParameterSpec parameter = parameterSpec(iterable, name);
    ParameterSpec iterationVar = step.loneGetter.iterationVar(parameter);
    return methodBuilder(name)
        .addAnnotation(Override.class)
        .returns(step.nextType)
        .addParameter(parameter)
        .addCode(nullCheck(parameter))
        .beginControlFlow("for ($T $N : $N)",
            iterationVar.type, iterationVar, parameter)
        .addStatement("this.$N.$L().add($N)", goal.goal.field,
            step.loneGetter.getter, iterationVar)
        .endControlFlow()
        .addCode(regularFinalBlock(goal, isLast))
        .addModifiers(PUBLIC)
        .build();
  }

  private static MethodSpec regularStep(AccessorPairStep step, BeanGoalContext goal, boolean isLast) {
    ParameterSpec parameter = step.parameter();
    return methodBuilder(step.accessorPair.accept(beanParameterName))
        .addAnnotation(Override.class)
        .addParameter(parameter)
        .addModifiers(PUBLIC)
        .returns(step.nextType)
        .addCode(nullCheck.apply(step))
        .addStatement("this.$N.$L($N)", goal.goal.field, step.setter, parameter)
        .addCode(regularFinalBlock(goal, isLast)).build();
  }

  private BuilderContextB() {
    throw new UnsupportedOperationException("no instances");
  }
}