package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoGoal.RegularGoalContext;
import net.zerobuilder.compiler.generate.StepContext.RegularStep;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.Utilities.parameterSpec;
import static net.zerobuilder.compiler.analyse.GoalContextFactory.GoalKind.INSTANCE_METHOD;
import static net.zerobuilder.compiler.generate.StepContext.maybeNullCheck;
import static net.zerobuilder.compiler.generate.UpdaterContext.typeName;

final class UpdaterContextV {

  static final Function<RegularGoalContext, ImmutableList<FieldSpec>> fields
      = new Function<RegularGoalContext, ImmutableList<FieldSpec>>() {
    @Override
    public ImmutableList<FieldSpec> apply(RegularGoalContext goal) {
      ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
      if (goal.goal.kind == INSTANCE_METHOD) {
        builder.add(goal.builders.field);
      }
      for (RegularStep parameter : goal.steps) {
        String name = parameter.validParameter.name;
        TypeName type = parameter.validParameter.type;
        builder.add(FieldSpec.builder(type, name, PRIVATE).build());
      }
      return builder.build();
    }
  };

  static final Function<RegularGoalContext, ImmutableList<MethodSpec>> updateMethods
      = new Function<RegularGoalContext, ImmutableList<MethodSpec>>() {
    @Override
    public ImmutableList<MethodSpec> apply(RegularGoalContext goal) {
      ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
      for (RegularStep parameter : goal.steps) {
        String name = parameter.validParameter.name;
        TypeName type = parameter.validParameter.type;
        builder.add(methodBuilder(name)
            .returns(goal.accept(typeName))
            .addParameter(parameterSpec(type, name))
            .addCode(parameter.accept(maybeNullCheck))
            .addStatement("this.$N = $N", name, name)
            .addStatement("return this")
            .addModifiers(PUBLIC)
            .build());
      }
      return builder.build();
    }
  };

  private UpdaterContextV() {
    throw new UnsupportedOperationException("no instances");
  }
}
