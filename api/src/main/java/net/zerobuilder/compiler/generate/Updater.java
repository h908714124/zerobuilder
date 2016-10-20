package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoModuleOutput.SimpleModuleOutput;
import net.zerobuilder.compiler.generate.DtoRegularGoal.AbstractRegularGoalContext;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.BuilderV.regularInvoke;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoGoalContext.goalCases;
import static net.zerobuilder.compiler.generate.GeneratorBU.goalToUpdaterB;
import static net.zerobuilder.compiler.generate.GeneratorVU.goalToUpdaterV;
import static net.zerobuilder.compiler.generate.UpdaterB.fieldsB;
import static net.zerobuilder.compiler.generate.UpdaterB.updateMethodsB;
import static net.zerobuilder.compiler.generate.UpdaterV.fieldsV;
import static net.zerobuilder.compiler.generate.UpdaterV.updateMethodsV;
import static net.zerobuilder.compiler.generate.Utilities.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.Utilities.statement;

public final class Updater extends DtoModule.SimpleModule {

  private static final Function<AbstractGoalContext, List<FieldSpec>> fields
      = goalCases(fieldsV, fieldsB);

  private static final Function<AbstractGoalContext, List<MethodSpec>> updateMethods
      = goalCases(updateMethodsV, updateMethodsB);

  private static final Function<AbstractGoalContext, DtoGeneratorOutput.BuilderMethod> goalToUpdater
      = goalCases(goalToUpdaterV, goalToUpdaterB);

  private static MethodSpec buildMethod(AbstractGoalContext goal) {
    return methodBuilder("done")
        .addModifiers(PUBLIC)
        .returns(goal.goalType())
        .addCode(invoke.apply(goal))
        .build();
  }

  private static TypeSpec defineUpdater(AbstractGoalContext goal) {
    return classBuilder(goal.implType())
        .addFields(fields.apply(goal))
        .addMethods(updateMethods.apply(goal))
        .addMethod(buildMethod(goal))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(builderConstructor.apply(goal))
        .build();
  }

  private static final Function<AbstractGoalContext, MethodSpec> builderConstructor =
      goalCases(
          AbstractRegularGoalContext::builderConstructor,
          bGoal -> constructorBuilder()
              .addModifiers(PRIVATE)
              .addExceptions(bGoal.context.lifecycle == REUSE_INSTANCES
                  ? Collections.emptyList()
                  : bGoal.goal.thrownTypes)
              .addCode(bGoal.context.lifecycle == REUSE_INSTANCES
                  ? emptyCodeBlock
                  : statement("this.$N = new $T()", bGoal.bean(), bGoal.type()))
              .build());

  private static final Function<BeanGoalContext, CodeBlock> returnBean
      = goal -> statement("return this.$N", goal.bean());

  private static final Function<AbstractGoalContext, CodeBlock> invoke
      = goalCases(regularInvoke, returnBean);

  @Override
  public String name() {
    return "updater";
  }

  @Override
  protected SimpleModuleOutput process(AbstractGoalContext goal) {
    return new SimpleModuleOutput(
        goalToUpdater.apply(goal),
        defineUpdater(goal));
  }
}
