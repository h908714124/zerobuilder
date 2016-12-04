package net.zerobuilder.modules.builder;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoContext.GoalContext;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoMethodGoal.InstanceMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoal.SimpleRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;

import java.util.List;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoGoalDetails.regularDetailsCases;
import static net.zerobuilder.compiler.generate.DtoRegularGoal.regularGoalContextCases;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.fieldSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.simpleName;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;
import static net.zerobuilder.modules.builder.RegularBuilder.implType;

final class Generator {

  static BuilderMethod builderMethod(SimpleRegularGoalContext goal) {
    AbstractRegularDetails abstractRegularDetails = goal.description.details;
    List<SimpleParameter> steps = goal.description.parameters;
    MethodSpec.Builder method = methodBuilder(RegularBuilder.methodName(goal))
        .returns(RegularBuilder.contractType(goal).nestedClass(upcase(steps.get(0).name)))
        .addModifiers(abstractRegularDetails.access(STATIC));
    GoalContext context = goal.description.context;
    ParameterSpec varInstance = parameterSpec(context.type,
        downcase(simpleName(context.type)));
    CodeBlock returnBlock = returnBlock(varInstance).apply(goal);
    method.addCode(returnBlock);
    if (isInstance.apply(goal.description.details)) {
      method.addParameter(varInstance);
    }
    return new BuilderMethod(goal.description.details.name, method.build());
  }

  private static final Function<AbstractRegularDetails, Boolean> isInstance =
      regularDetailsCases(
          constructor -> false,
          staticMethod -> false,
          instanceMethod -> true);

  private static Function<SimpleRegularGoalContext, CodeBlock> returnBlock(ParameterSpec varInstance) {
    return regularGoalContextCases(
        Generator::returnRegular,
        method -> returnInstanceMethod(method, varInstance),
        Generator::returnRegular);
  }

  private static CodeBlock returnRegular(SimpleRegularGoalContext goal) {
    ParameterSpec varBuilder = builderInstance(goal);
    if (goal.description.details.lifecycle == REUSE_INSTANCES) {
      FieldSpec cache = goal.description.context.cache(implType(goal));
      return CodeBlock.builder()
          .addStatement("$T $N = $N.get()", varBuilder.type, varBuilder, cache)
          .beginControlFlow("if ($N._currently_in_use)", varBuilder)
          .addStatement("$N.remove()", cache)
          .addStatement("$N = $N.get()", varBuilder, cache)
          .endControlFlow()
          .addStatement("$N._currently_in_use = true", varBuilder)
          .addStatement("return $N", varBuilder)
          .build();
    }
    return statement("return new $T()", varBuilder.type);
  }

  private static CodeBlock returnInstanceMethod(
      InstanceMethodGoalContext goal, ParameterSpec varInstance) {
    ParameterSpec varBuilder = builderInstance(goal);
    if (goal.description.details.lifecycle == REUSE_INSTANCES) {
      FieldSpec cache = goal.description.context.cache(implType(goal));
      return CodeBlock.builder()
          .addStatement("$T $N = $N.get()", varBuilder.type, varBuilder, cache)
          .beginControlFlow("if ($N._currently_in_use)", varBuilder)
          .addStatement("$N.remove()", cache)
          .addStatement("$N = $N.get()", varBuilder, cache)
          .endControlFlow()
          .addStatement("$N._currently_in_use = true", varBuilder)
          .addStatement("$N.$N = $N", varBuilder, instanceField(goal.description), varInstance)
          .addStatement("return $N", varBuilder)
          .build();
    }
    return statement("return new $T($N)", implType(goal), varInstance);
  }

  private static ParameterSpec builderInstance(SimpleRegularGoalContext goal) {
    return parameterSpec(implType(goal), "_builder");
  }

  static FieldSpec instanceField(DtoRegularGoalDescription.SimpleRegularGoalDescription description) {
    TypeName type = description.context.type;
    String name = '_' + downcase(simpleName(type));
    return description.details.lifecycle == REUSE_INSTANCES
        ? fieldSpec(type, name, PRIVATE)
        : fieldSpec(type, name, PRIVATE, FINAL);
  }

  private Generator() {
    throw new UnsupportedOperationException("no instances");
  }
}
