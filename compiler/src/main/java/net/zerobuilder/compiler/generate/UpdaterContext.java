package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoBeanGoalContext.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.Utilities.constructor;
import static net.zerobuilder.compiler.generate.Utilities.statement;
import static net.zerobuilder.compiler.generate.Utilities.upcase;
import static net.zerobuilder.compiler.generate.BuilderContextV.regularInvoke;
import static net.zerobuilder.compiler.generate.DtoGoalContext.buildersContext;
import static net.zerobuilder.compiler.generate.DtoGoalContext.goalCases;
import static net.zerobuilder.compiler.generate.DtoGoalContext.goalName;
import static net.zerobuilder.compiler.generate.DtoGoalContext.goalType;
import static net.zerobuilder.compiler.generate.DtoGoalContext.thrownTypes;

final class UpdaterContext {

  static ClassName updaterType(AbstractGoalContext goal) {
    return buildersContext.apply(goal).generatedType.nestedClass(
        upcase(goalName.apply(goal) + "Updater"));
  }

  private static final Function<AbstractGoalContext, ImmutableList<FieldSpec>> fields
      = goalCases(UpdaterContextV.fields, UpdaterContextB.fields);

  private static final Function<AbstractGoalContext, ImmutableList<MethodSpec>> updateMethods
      = goalCases(UpdaterContextV.updateMethods, UpdaterContextB.updateMethods);

  private static MethodSpec buildMethod(AbstractGoalContext goal) {
    return methodBuilder("build")
        .addModifiers(PUBLIC)
        .returns(goalType.apply(goal))
        .addCode(invoke.apply(goal))
        .addExceptions(thrownTypes.apply(goal))
        .build();
  }

  static TypeSpec defineUpdater(AbstractGoalContext goal) {
    return classBuilder(updaterType(goal))
        .addFields(fields.apply(goal))
        .addMethods(updateMethods.apply(goal))
        .addMethod(buildMethod(goal))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(constructor(PRIVATE))
        .build();
  }

  private static final Function<BeanGoalContext, CodeBlock> returnBean
      = new Function<BeanGoalContext, CodeBlock>() {
    @Override
    public CodeBlock apply(BeanGoalContext goal) {
      return statement("return this.$N", goal.goal.field);
    }
  };

  private static final Function<AbstractGoalContext, CodeBlock> invoke
      = goalCases(regularInvoke, returnBean);

  private UpdaterContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
