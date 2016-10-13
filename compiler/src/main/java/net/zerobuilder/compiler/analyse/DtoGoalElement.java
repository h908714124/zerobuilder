package net.zerobuilder.compiler.analyse;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.AccessLevel;
import net.zerobuilder.Goal;
import net.zerobuilder.compiler.generate.Access;
import net.zerobuilder.compiler.generate.DtoGoal.BeanGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoal.ConstructorGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoal.GoalMethodType;
import net.zerobuilder.compiler.generate.DtoGoal.GoalOptions;
import net.zerobuilder.compiler.generate.DtoGoal.MethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoal.RegularGoalDetails;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.AccessLevel.UNSPECIFIED;
import static net.zerobuilder.compiler.analyse.Utilities.downcase;

final class DtoGoalElement {

  interface GoalElementCases<R> {
    R regularGoal(RegularGoalElement goal);
    R beanGoal(BeanGoalElement goal);
  }

  private static <R> Function<AbstractGoalElement, R> asFunction(final GoalElementCases<R> cases) {
    return goal -> goal.accept(cases);
  }

  static abstract class AbstractGoalElement {
    final Goal goalAnnotation;
    AbstractGoalElement(Goal goalAnnotation) {
      this.goalAnnotation = goalAnnotation;
    }
    abstract <R> R accept(GoalElementCases<R> goalElementCases);
  }

  static <R> GoalElementCases<R> goalElementCases(
      final Function<RegularGoalElement, R> regularGoalFunction,
      final Function<BeanGoalElement, R> beanGoalFunction) {
    return new GoalElementCases<R>() {
      @Override
      public R regularGoal(RegularGoalElement executableGoal) {
        return regularGoalFunction.apply(executableGoal);
      }
      @Override
      public R beanGoal(BeanGoalElement beanGoal) {
        return beanGoalFunction.apply(beanGoal);
      }
    };
  }

  static final Function<AbstractGoalElement, String> goalName = asFunction(new GoalElementCases<String>() {
    @Override
    public String regularGoal(RegularGoalElement goal) {
      return goal.details.name();
    }
    @Override
    public String beanGoal(BeanGoalElement goal) {
      return goal.details.name();
    }
  });

  static final class RegularGoalElement extends AbstractGoalElement {
    final RegularGoalDetails details;
    final ExecutableElement executableElement;

    private RegularGoalElement(ExecutableElement element, RegularGoalDetails details) {
      super(element.getAnnotation(Goal.class));
      this.details = details;
      this.executableElement = element;
    }

    static RegularGoalElement create(ExecutableElement element, AccessLevel defaultAccess) {
      TypeName goalType = goalType(element);
      Goal goalAnnotation = element.getAnnotation(Goal.class);
      String name = goalName(goalAnnotation, goalType);
      GoalOptions goalOptions = goalOptions(goalAnnotation, defaultAccess);
      String methodName = element.getSimpleName().toString();
      GoalMethodType goalMethodType = element.getModifiers().contains(STATIC)
          ? GoalMethodType.STATIC_METHOD
          : GoalMethodType.INSTANCE_METHOD;
      List<String> parameterNames = parameterNames(element);
      RegularGoalDetails goal = element.getKind() == CONSTRUCTOR
          ? ConstructorGoalDetails.create(goalType, name, parameterNames, goalOptions)
          : MethodGoalDetails.create(goalType, name, parameterNames, methodName, goalMethodType, goalOptions);
      return new RegularGoalElement(element, goal);
    }

    <R> R accept(GoalElementCases<R> goalElementCases) {
      return goalElementCases.regularGoal(this);
    }
  }

  private static List<String> parameterNames(ExecutableElement element) {
    List<String> builder = new ArrayList<>();
    for (VariableElement parameter : element.getParameters()) {
      builder.add(parameter.getSimpleName().toString());
    }
    return builder;
  }

  static final class BeanGoalElement extends AbstractGoalElement {
    final BeanGoalDetails details;
    final TypeElement beanType;
    private BeanGoalElement(ClassName goalType, String name, TypeElement beanType,
                            Goal goalAnnotation, GoalOptions goalOptions) {
      super(goalAnnotation);
      this.details = new BeanGoalDetails(goalType, name, goalOptions);
      this.beanType = beanType;
    }
    static BeanGoalElement create(TypeElement beanType, AccessLevel defaultAccess) {
      ClassName goalType = ClassName.get(beanType);
      Goal goalAnnotation = beanType.getAnnotation(Goal.class);
      String name = goalName(goalAnnotation, goalType);
      GoalOptions goalOptions = goalOptions(goalAnnotation, defaultAccess);
      return new BeanGoalElement(goalType, name, beanType, goalAnnotation, goalOptions);
    }
    <R> R accept(GoalElementCases<R> goalElementCases) {
      return goalElementCases.beanGoal(this);
    }
  }

  static final GoalElementCases<Element> getElement = new GoalElementCases<Element>() {
    @Override
    public Element regularGoal(RegularGoalElement executableGoal) {
      return executableGoal.executableElement;
    }
    @Override
    public Element beanGoal(BeanGoalElement beanGoal) {
      return beanGoal.beanType;
    }
  };

  private static String goalName(Goal goalAnnotation, TypeName goalType) {
    return goalAnnotation.name().isEmpty()
        ? downcase(((ClassName) goalType.box()).simpleName())
        : goalAnnotation.name();
  }

  private static Access accessLevelOverride(AccessLevel override, AccessLevel defaultAccess) {
    defaultAccess = defaultAccess == UNSPECIFIED
        ? AccessLevel.PUBLIC
        : defaultAccess;
    return override == UNSPECIFIED
        ? defaultAccess.access()
        : override.access();
  }

  private static GoalOptions goalOptions(Goal goalAnnotation, AccessLevel defaultAccess) {
    boolean toBuilder = goalAnnotation.updater();
    boolean builder = goalAnnotation.builder();
    return GoalOptions.builder()
        .builderAccess(accessLevelOverride(goalAnnotation.builderAccess(), defaultAccess))
        .toBuilderAccess(accessLevelOverride(goalAnnotation.updaterAccess(), defaultAccess))
        .toBuilder(toBuilder)
        .builder(builder)
        .build();
  }

  private static TypeName goalType(ExecutableElement goal) {
    switch (goal.getKind()) {
      case CONSTRUCTOR:
        return ClassName.get(goal.getEnclosingElement().asType());
      default:
        return TypeName.get(goal.getReturnType());
    }
  }

  private DtoGoalElement() {
    throw new UnsupportedOperationException("no instances");
  }
}
