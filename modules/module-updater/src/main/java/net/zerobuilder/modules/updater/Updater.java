package net.zerobuilder.modules.updater;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoParameter;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularStep.AbstractRegularStep;
import net.zerobuilder.compiler.generate.DtoRegularStep.ProjectedRegularStep;
import net.zerobuilder.compiler.generate.DtoStep;
import net.zerobuilder.compiler.generate.ZeroUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeName.BOOLEAN;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoParameter.parameterName;
import static net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.steps;
import static net.zerobuilder.compiler.generate.DtoStep.always;
import static net.zerobuilder.compiler.generate.ZeroUtil.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.ZeroUtil.fieldSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.modules.updater.RegularUpdater.implType;

final class Updater {

  static final Function<ProjectedRegularGoalContext, List<FieldSpec>> fieldsV =
      goal -> {
        List<FieldSpec> builder = new ArrayList<>();
        if (goal.mayReuse()) {
          builder.add(fieldSpec(BOOLEAN, "_currently_in_use", PRIVATE));
        }
        for (ProjectedRegularStep step : steps.apply(goal)) {
          String name = step.regularParameter().name;
          TypeName type = step.regularParameter().type;
          builder.add(fieldSpec(type, name, PRIVATE));
        }
        return builder;
      };

  static final Function<ProjectedRegularGoalContext, List<MethodSpec>> stepMethodsV =
      goal -> steps.apply(goal).stream()
          .map(updateMethods(goal))
          .collect(toList());

  private static Function<AbstractRegularStep, MethodSpec> updateMethods(ProjectedRegularGoalContext goal) {
    return step -> normalUpdate(goal, step);
  }

  private static MethodSpec normalUpdate(ProjectedRegularGoalContext goal, AbstractRegularStep step) {
    String name = step.regularParameter().name;
    TypeName type = step.regularParameter().type;
    ParameterSpec parameter = parameterSpec(type, name);
    return methodBuilder(name)
        .returns(implType(goal))
        .addParameter(parameter)
        .addCode(nullCheck.apply(step))
        .addStatement("this.$N = $N", step.field(), parameter)
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build();
  }

  private static final Function<DtoStep.AbstractStep, CodeBlock> nullCheck =
      always(step -> {
        DtoParameter.AbstractParameter parameter = step.abstractParameter();
        if (!parameter.nullPolicy.check() || parameter.type.isPrimitive()) {
          return emptyCodeBlock;
        }
        String name = parameterName.apply(parameter);
        return ZeroUtil.nullCheck(name, name);
      });

  private Updater() {
    throw new UnsupportedOperationException("no instances");
  }
}
