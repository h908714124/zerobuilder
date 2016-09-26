package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import net.zerobuilder.compiler.analyse.DtoShared.ValidBeanParameter;
import net.zerobuilder.compiler.generate.DtoGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoStep.BeanStep;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.Utilities.emptyCodeBlock;
import static net.zerobuilder.compiler.Utilities.parameterSpec;
import static net.zerobuilder.compiler.Utilities.statement;
import static net.zerobuilder.compiler.generate.DtoGoal.builderImplName;
import static net.zerobuilder.compiler.generate.Generator.TL;
import static net.zerobuilder.compiler.generate.Generator.stepsField;
import static net.zerobuilder.compiler.generate.Generator.updaterField;
import static net.zerobuilder.compiler.generate.StepContext.iterationVarNullCheck;

final class GeneratorB {

  static final Function<BeanGoalContext, MethodSpec> goalToToBuilder
      = new Function<BeanGoalContext, MethodSpec>() {
    @Override
    public MethodSpec apply(BeanGoalContext goal) {
      ParameterSpec parameter = parameterSpec(goal.goal.goalType, goal.field.name);
      MethodSpec.Builder method = methodBuilder(downcase(goal.goal.name + "ToBuilder"))
          .addParameter(parameter);
      ParameterSpec updater = updaterInstance(goal);
      method.addCode(initializeUpdater(goal, updater));
      for (BeanStep step : goal.steps) {
        if (step.validParameter.collectionType.isPresent()) {
          method.addCode(copyCollection(goal, step));
        } else {
          method.addCode(copyRegular(goal, step));
        }
      }
      method.addStatement("return $N", updater);
      return method
          .returns(goal.accept(UpdaterContext.typeName))
          .addModifiers(PUBLIC, STATIC).build();
    }
  };

  private static CodeBlock copyCollection(BeanGoalContext goal, BeanStep step) {
    ParameterSpec parameter = parameterSpec(goal.goal.goalType, goal.field.name);
    ParameterSpec iterationVar = step.validParameter.collectionType.get(parameter);
    return CodeBlock.builder().add(nullCheck(parameter, step.validParameter, true))
        .beginControlFlow("for ($T $N : $N.$N())",
            iterationVar.type, iterationVar, parameter,
            step.validParameter.getter)
        .add(iterationVarNullCheck(step, parameter))
        .addStatement("$N.$N.$N().add($N)", updaterInstance(goal),
            downcase(goal.goal.goalType.simpleName()),
            step.validParameter.getter,
            iterationVar)
        .endControlFlow()
        .build();
  }

  private static CodeBlock copyRegular(BeanGoalContext goal, BeanStep step) {
    ParameterSpec parameter = parameterSpec(goal.goal.goalType, goal.field.name);
    ParameterSpec updater = updaterInstance(goal);
    return CodeBlock.builder()
        .add(nullCheck(parameter, step.validParameter))
        .addStatement("$N.$N.$L($N.$N())", updater,
            goal.field,
            step.setter,
            parameter,
            step.validParameter.getter)
        .build();
  }


  private static CodeBlock nullCheck(ParameterSpec parameter, ValidBeanParameter validParameter) {
    return nullCheck(parameter, validParameter, validParameter.nonNull);
  }

  private static CodeBlock nullCheck(ParameterSpec parameter, ValidBeanParameter validParameter, boolean nonNull) {
    if (!nonNull) {
      return emptyCodeBlock;
    }
    return CodeBlock.builder()
        .beginControlFlow("if ($N.$N() == null)", parameter,
            validParameter.getter)
        .addStatement("throw new $T($S)",
            NullPointerException.class, validParameter.name)
        .endControlFlow().build();
  }

  private static CodeBlock initializeUpdater(BeanGoalContext goal, ParameterSpec updater) {
    CodeBlock.Builder builder = CodeBlock.builder();
    if (goal.builders.recycle) {
      builder.addStatement("$T $N = $L.get().$N", updater.type, updater,
          TL, updaterField(goal));
    } else {
      builder.addStatement("$T $N = new $T()", updater.type, updater, updater.type);
    }
    builder.addStatement("$N.$N = new $T()", updater, goal.field, goal.goal.goalType);
    return builder.build();
  }

  private static ParameterSpec updaterInstance(BeanGoalContext goal) {
    ClassName updaterType = goal.accept(UpdaterContext.typeName);
    return parameterSpec(updaterType, "updater");
  }

  static final Function<BeanGoalContext, MethodSpec> goalToBuilder
      = new Function<BeanGoalContext, MethodSpec>() {
    @Override
    public MethodSpec apply(BeanGoalContext goal) {
      ClassName stepsType = goal.accept(builderImplName);
      MethodSpec.Builder method = methodBuilder(goal.goal.name + "Builder")
          .returns(goal.steps.get(0).thisType)
          .addModifiers(PUBLIC, STATIC);
      String steps = downcase(stepsType.simpleName());
      method.addCode(goal.builders.recycle
          ? statement("$T $N = $N.get().$N", stepsType, steps, TL, stepsField(goal))
          : statement("$T $N = new $T()", stepsType, steps, stepsType));
      return method.addStatement("$N.$N = new $T()", steps,
          downcase(goal.goal.goalType.simpleName()), goal.goal.goalType)
          .addStatement("return $N", steps)
          .build();
    }
  };

  private GeneratorB() {
    throw new UnsupportedOperationException("no instances");
  }
}