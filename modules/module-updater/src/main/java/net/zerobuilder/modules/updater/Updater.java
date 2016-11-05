package net.zerobuilder.modules.updater;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput;
import net.zerobuilder.compiler.generate.DtoModule.ProjectedModule;
import net.zerobuilder.compiler.generate.DtoModuleOutput.AbstractModuleOutput;
import net.zerobuilder.compiler.generate.DtoProjectedGoal;
import net.zerobuilder.compiler.generate.DtoProjectedGoal.ProjectedGoal;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularStep.AbstractRegularStep;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static java.util.Collections.singletonList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import static net.zerobuilder.compiler.generate.DtoGoalContext.context;
import static net.zerobuilder.compiler.generate.DtoProjectedGoal.goalType;
import static net.zerobuilder.compiler.generate.DtoProjectedGoal.projectedGoalCases;
import static net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.projectedRegularGoalContextCases;
import static net.zerobuilder.compiler.generate.ZeroUtil.constructor;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.ZeroUtil.joinCodeBlocks;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

public final class Updater extends ProjectedModule {

  private static final String moduleName = "updater";

  private static final Function<ProjectedGoal, List<FieldSpec>> fields =
      projectedGoalCases(UpdaterV.fieldsV, UpdaterB.fieldsB);

  private static final Function<ProjectedGoal, List<MethodSpec>> updateMethods =
      projectedGoalCases(UpdaterV.updateMethodsV, UpdaterB.updateMethodsB);

  private static final Function<ProjectedGoal, DtoGeneratorOutput.BuilderMethod> goalToUpdater =
      projectedGoalCases(GeneratorV::updaterMethodV, GeneratorB::updaterMethodB);

  private MethodSpec buildMethod(ProjectedGoal goal) {
    return methodBuilder("done")
        .addModifiers(PUBLIC)
        .returns(goalType.apply(goal))
        .addCode(invoke.apply(goal))
        .build();
  }

  private TypeSpec defineUpdater(ProjectedGoal projectedGoal) {
    return classBuilder(implType(projectedGoal))
        .addFields(fields.apply(projectedGoal))
        .addMethods(updateMethods.apply(projectedGoal))
        .addMethod(buildMethod(projectedGoal))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(updaterConstructor.apply(projectedGoal))
        .build();
  }

  static ClassName implType(ProjectedGoal projectedGoal) {
    AbstractGoalContext goal = goalContext(projectedGoal);
    String implName = upcase(goal.name()) + upcase(moduleName);
    return context.apply(goal)
        .generatedType.nestedClass(implName);
  }

  private static final Function<ProjectedRegularGoalContext, MethodSpec> regularConstructor =
      projectedRegularGoalContextCases(
          method -> constructor(PRIVATE),
          constructor -> constructor(PRIVATE));

  private static final Function<ProjectedGoal, MethodSpec> updaterConstructor =
      projectedGoalCases(
          Updater.regularConstructor,
          bean -> constructorBuilder()
              .addModifiers(PRIVATE)
              .addExceptions(bean.context.lifecycle == REUSE_INSTANCES
                  ? Collections.emptyList()
                  : bean.thrownTypes)
              .addCode(bean.context.lifecycle == REUSE_INSTANCES
                  ? emptyCodeBlock
                  : statement("this.$N = new $T()", bean.bean(), bean.type()))
              .build());

  private final Function<ProjectedRegularGoalContext, CodeBlock> regularInvoke =
      projectedRegularGoalContextCases(
          this::staticCall,
          this::constructorCall);

  private CodeBlock staticCall(ProjectedMethodGoalContext goal) {
    String method = goal.details.methodName;
    TypeName type = goal.details.goalType;
    ParameterSpec varGoal = parameterSpec(type,
        '_' + downcase(((ClassName) type.box()).simpleName()));
    CodeBlock.Builder builder = CodeBlock.builder();
    if (goal.context.lifecycle == REUSE_INSTANCES) {
      builder.addStatement("this._currently_in_use = false");
    }
    return builder
        .addStatement("$T $N = $T.$N($L)", varGoal.type, varGoal, goal.context.type,
            method, goal.invocationParameters())
        .add(free(goal.steps))
        .addStatement("return $N", varGoal)
        .build();
  }

  private CodeBlock constructorCall(ProjectedConstructorGoalContext goal) {
    ClassName type = goal.details.goalType;
    ParameterSpec varGoal = parameterSpec(type,
        '_' + downcase(type.simpleName()));
    CodeBlock.Builder builder = CodeBlock.builder();
    if (goal.context.lifecycle == REUSE_INSTANCES) {
      builder.addStatement("this._currently_in_use = false");
    }
    return builder
        .addStatement("$T $N = new $T($L)", varGoal.type, varGoal, type, goal.invocationParameters())
        .add(free(goal.steps))
        .addStatement("return $N", varGoal)
        .build();
  }

  private CodeBlock free(List<? extends AbstractRegularStep> steps) {
    return steps.stream()
        .map(step -> step.regularParameter())
        .filter(parameter -> !parameter.type.isPrimitive())
        .map(parameter -> statement("this.$N = null", parameter.name))
        .collect(joinCodeBlocks);
  }

  private CodeBlock returnBean(BeanGoalContext goal) {
    ClassName type = goal.details.goalType;
    ParameterSpec varGoal = parameterSpec(type,
        '_' + downcase(type.simpleName()));
    CodeBlock.Builder builder = CodeBlock.builder();
    if (goal.context.lifecycle == REUSE_INSTANCES) {
      builder.addStatement("this._currently_in_use = false");
    }
    builder.addStatement("$T $N = this.$N", varGoal.type, varGoal, goal.bean());
    if (goal.context.lifecycle == REUSE_INSTANCES) {
      builder.addStatement("this.$N = null", goal.bean());
    }
    return builder.addStatement("return $N", varGoal).build();
  }

  private final Function<ProjectedGoal, CodeBlock> invoke
      = projectedGoalCases(regularInvoke, this::returnBean);

  static AbstractGoalContext goalContext(ProjectedGoal goal) {
    return DtoProjectedGoal.goalContext.apply(goal);
  }

  static String methodName(AbstractGoalContext goal) {
    return goal.name() + upcase(moduleName);
  }

  static FieldSpec cacheField(ProjectedGoal projectedGoal) {
    ClassName type = implType(projectedGoal);
    return FieldSpec.builder(type, downcase(type.simpleName()), PRIVATE)
        .initializer("new $T()", type)
        .build();
  }

  @Override
  protected AbstractModuleOutput process(ProjectedGoal goal) {
    return new AbstractModuleOutput(
        goalToUpdater.apply(goal),
        singletonList(defineUpdater(goal)),
        singletonList(cacheField(goal)));
  }
}
