package net.zerobuilder.modules.generics;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleStaticMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularStep.SimpleRegularStep;
import net.zerobuilder.compiler.generate.DtoStep;

import java.util.ArrayList;
import java.util.List;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoSimpleGoal.context;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

final class GenericsContract {

  static final TypeVariableName[] NO_TYPEVARNAME = new TypeVariableName[0];

  private static ClassName nextType(DtoStep.AbstractStep step) {
    return step.context.generatedType
        .nestedClass(upcase(step.goalDetails.name() + "Builder"))
        .nestedClass(step.nextStep.get().thisType);
  }

  static List<TypeSpec> stepInterfaces(SimpleStaticMethodGoalContext goal,
                                       List<List<TypeVariableName>> typeParams,
                                       List<List<TypeVariableName>> methodParams) {
    ArrayList<TypeSpec> builder = new ArrayList<>();
    for (int i = 0; i < goal.steps.size(); i++) {
      SimpleRegularStep step = goal.steps.get(i);
      builder.add(TypeSpec.interfaceBuilder(step.thisType)
          .addTypeVariables(typeParams.get(i))
          .addMethod(nextStep(goal,
              typeParams,
              methodParams, i))
          .addModifiers(PUBLIC)
          .build());
    }
    return builder;
  }

  private static MethodSpec nextStep(SimpleStaticMethodGoalContext goal, List<List<TypeVariableName>> typeParams, List<List<TypeVariableName>> methodParams, int i) {
    SimpleRegularStep step = goal.steps.get(i);
    return MethodSpec.methodBuilder(downcase(step.thisType))
        .addTypeVariables(methodParams.get(i))
        .addModifiers(PUBLIC, ABSTRACT)
        .returns(nextStepType(typeParams, step, i))
        .addParameter(parameterSpec(step.parameter.type, step.parameter.name))
        .build();
  }

  private static TypeName nextStepType(List<List<TypeVariableName>> typeParams, SimpleRegularStep step, int i) {
    if (!step.nextStep.isPresent()) {
      return step.goalDetails.type();
    }
    ClassName rawNext = nextType(step);
    return typeParams.get(i + 1).isEmpty() ?
        rawNext :
        ParameterizedTypeName.get(rawNext, typeParams.get(i + 1).toArray(NO_TYPEVARNAME));
  }

  static ClassName contractType(SimpleStaticMethodGoalContext goal) {
    String contractName = upcase(goal.details.name) + "Builder";
    return context.apply(goal)
        .generatedType.nestedClass(contractName);
  }

  static ClassName implType(SimpleStaticMethodGoalContext goal) {
    String contractName = upcase(goal.details.name) + "BuilderImpl";
    return context.apply(goal)
        .generatedType.nestedClass(contractName);
  }

  static List<TypeName> stepTypes(SimpleStaticMethodGoalContext goal) {
    List<TypeName> builder = new ArrayList<>(goal.steps.size() + 1);
    goal.steps.stream().map(step -> step.parameter.type).forEach(builder::add);
    builder.add(goal.details.goalType);
    return builder;
  }
}