package net.zerobuilder.compiler.analyse;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.Goal;
import net.zerobuilder.Ignore;
import net.zerobuilder.Step;
import net.zerobuilder.compiler.analyse.DtoGoalElement.BeanGoalElement;
import net.zerobuilder.compiler.analyse.ProjectionValidator.TmpAccessorPair;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoBeanParameter.LoneGetter;
import net.zerobuilder.compiler.generate.DtoGoalDescription.BeanGoalDescription;
import net.zerobuilder.compiler.generate.DtoGoalDescription.GoalDescription;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.auto.common.MoreElements.getLocalAndInheritedMethods;
import static com.google.auto.common.MoreTypes.asTypeElement;
import static com.google.common.base.Ascii.isUpperCase;
import static com.squareup.javapoet.ClassName.OBJECT;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.type.TypeKind.DECLARED;
import static javax.lang.model.util.ElementFilter.constructorsIn;
import static net.zerobuilder.compiler.Messages.ErrorMessages.BEAN_COULD_NOT_FIND_SETTER;
import static net.zerobuilder.compiler.Messages.ErrorMessages.BEAN_GETTER_EXCEPTION;
import static net.zerobuilder.compiler.Messages.ErrorMessages.BEAN_GETTER_SETTER_TYPE_MISMATCH;
import static net.zerobuilder.compiler.Messages.ErrorMessages.BEAN_IGNORE_AND_STEP;
import static net.zerobuilder.compiler.Messages.ErrorMessages.BEAN_NO_ACCESSOR_PAIRS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.BEAN_NO_DEFAULT_CONSTRUCTOR;
import static net.zerobuilder.compiler.Messages.ErrorMessages.BEAN_PRIVATE_CLASS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.BEAN_SETTER_EXCEPTION;
import static net.zerobuilder.compiler.Messages.ErrorMessages.IGNORE_ON_SETTER;
import static net.zerobuilder.compiler.Messages.ErrorMessages.STEP_ON_SETTER;
import static net.zerobuilder.compiler.analyse.ProjectionValidator.TmpAccessorPair.toValidParameter;
import static net.zerobuilder.compiler.analyse.ProjectionValidator.TmpValidParameter.nonNull;
import static net.zerobuilder.compiler.analyse.ProjectionValidator.shuffledParameters;
import static net.zerobuilder.compiler.analyse.Utilities.ClassNames.COLLECTION;
import static net.zerobuilder.compiler.analyse.Utilities.transform;
import static net.zerobuilder.compiler.generate.DtoBeanParameter.beanParameterName;

final class ProjectionValidatorB {

  private static final Ordering<TmpAccessorPair> ACCESSOR_PAIR_ORDERING
      = Ordering.from((pair0, pair1) -> {
    String name0 = pair0.validBeanParameter.accept(beanParameterName);
    String name1 = pair1.validBeanParameter.accept(beanParameterName);
    return name0.compareTo(name1);
  });

  static final Function<BeanGoalElement, GoalDescription> validateBean
      = goal -> {
    Map<String, List<ExecutableElement>> settersByName = setters(goal);
    ImmutableList.Builder<TmpAccessorPair> builder = ImmutableList.builder();
    for (ExecutableElement getter : getters(goal)) {
      List<ExecutableElement> setters = settersByName.get(setterName(getter));
      if (setters != null && setters.size() != 1) {
        throw new IllegalStateException("setter name should be unique");
      }
      builder.add(setters == null
          ? loneGetter(getter, goal.goalAnnotation)
          : regularAccessorPair(getter, setters.get(0), goal.goalAnnotation));
    }
    List<TmpAccessorPair> tmpAccessorPairs = builder.build();
    if (tmpAccessorPairs.isEmpty()) {
      throw new ValidationException(BEAN_NO_ACCESSOR_PAIRS, goal.beanType);
    }
    return createResult(goal, tmpAccessorPairs);
  };

  private static TmpAccessorPair loneGetter(ExecutableElement getter, Goal goalAnnotation) {
    TypeMirror type = getter.getReturnType();
    String name = getter.getSimpleName().toString();
    if (!isImplementationOf(type, COLLECTION)) {
      throw new ValidationException(BEAN_COULD_NOT_FIND_SETTER, getter);
    }
    // no setter but we have a getter that returns something like List<E>
    // in this case we need to find what E is ("collectionType")
    TypeName typeName = TypeName.get(type);
    boolean nonNull = nonNull(type, getter.getAnnotation(Step.class), goalAnnotation);
    LoneGetter loneGetter = LoneGetter.create(typeName, name, nonNull);
    return TmpAccessorPair.createLoneGetter(getter, loneGetter);
  }

  private static TmpAccessorPair regularAccessorPair(ExecutableElement getter, ExecutableElement setter, Goal goalAnnotation) {
    TypeName setterType = TypeName.get(setter.getParameters().get(0).asType());
    TypeName getterType = TypeName.get(getter.getReturnType());
    if (!setterType.equals(getterType)) {
      throw new ValidationException(BEAN_GETTER_SETTER_TYPE_MISMATCH, setter);
    }
    if (!getter.getThrownTypes().isEmpty()) {
      throw new ValidationException(BEAN_GETTER_EXCEPTION, getter);
    }
    return TmpAccessorPair.createAccessorPair(getter, goalAnnotation);
  }

  private static List<ExecutableElement> getters(BeanGoalElement goal) {
    return getLocalAndInheritedMethods(goal.beanType, goal.elements).stream()
        .filter(method -> {
          String name = method.getSimpleName().toString();
          return method.getParameters().isEmpty()
              && !method.getModifiers().contains(PRIVATE)
              && !method.getModifiers().contains(STATIC)
              && !method.getReturnType().getKind().equals(TypeKind.VOID)
              && !method.getReturnType().getKind().equals(TypeKind.NONE)
              && (name.startsWith("get") || name.startsWith("is"))
              && !"getClass".equals(name);
        })
        .filter(getter -> {
          Ignore ignoreAnnotation = getter.getAnnotation(Ignore.class);
          if (ignoreAnnotation != null && getter.getAnnotation(Step.class) != null) {
            throw new ValidationException(BEAN_IGNORE_AND_STEP, getter);
          }
          return ignoreAnnotation == null;
        })
        .collect(Collectors.toList());
  }

  private static Map<String, List<ExecutableElement>> setters(BeanGoalElement goal) throws ValidationException {
    TypeElement beanType = goal.beanType;
    if (!hasParameterlessConstructor(beanType)) {
      throw new ValidationException(BEAN_NO_DEFAULT_CONSTRUCTOR, beanType);
    }
    if (beanType.getModifiers().contains(PRIVATE)) {
      throw new ValidationException(BEAN_PRIVATE_CLASS, beanType);
    }
    return getLocalAndInheritedMethods(beanType, goal.elements).stream()
        .filter(method -> method.getKind() == ElementKind.METHOD
            && !method.getModifiers().contains(PRIVATE)
            && method.getSimpleName().length() >= 4
            && isUpperCase(method.getSimpleName().charAt(3))
            && method.getSimpleName().toString().startsWith("set")
            && method.getParameters().size() == 1
            && method.getReturnType().getKind() == TypeKind.VOID)
        .filter(setter -> {
          if (setter.getThrownTypes().isEmpty()) {
            return true;
          } else {
            throw new ValidationException(BEAN_SETTER_EXCEPTION, setter);
          }
        })
        .filter(setter -> {
          if (setter.getAnnotation(Step.class) != null) {
            throw new ValidationException(STEP_ON_SETTER, setter);
          }
          if (setter.getAnnotation(Ignore.class) != null) {
            throw new ValidationException(IGNORE_ON_SETTER, setter);
          }
          return true;
        })
        .collect(Collectors.groupingBy(setter1 -> setter1.getSimpleName().toString().substring(3)));
  }

  private static String setterName(ExecutableElement getter) {
    String name = getter.getSimpleName().toString();
    return name.substring(name.startsWith("get") ? 3 : 2);
  }

  private static boolean hasParameterlessConstructor(TypeElement type) {
    for (ExecutableElement constructor : constructorsIn(type.getEnclosedElements())) {
      if (constructor.getParameters().isEmpty()
          && !constructor.getModifiers().contains(PRIVATE)) {
        return true;
      }
    }
    return false;
  }

  private static GoalDescription createResult(BeanGoalElement goal, List<TmpAccessorPair> tmpAccessorPairs) {
    List<AbstractBeanParameter> validBeanParameters
        = transform(shuffledParameters(ACCESSOR_PAIR_ORDERING.sortedCopy(tmpAccessorPairs)), toValidParameter);
    return BeanGoalDescription.create(goal.details, validBeanParameters);
  }

  private static boolean isImplementationOf(TypeMirror typeMirror, ClassName test) {
    if (typeMirror.getKind() != DECLARED) {
      return false;
    }
    TypeElement element = asTypeElement(typeMirror);
    return implementationOf(element, test);
  }

  private static boolean implementationOf(TypeElement element, ClassName test) {
    ClassName className = ClassName.get(element);
    if (className.equals(test)) {
      return true;
    }
    if (className.equals(OBJECT)) {
      return false;
    }
    for (TypeMirror anInterface : element.getInterfaces()) {
      if (implementationOf(asTypeElement(anInterface), test)) {
        return true;
      }
    }
    return false;
  }

  private ProjectionValidatorB() {
    throw new UnsupportedOperationException("no instances");
  }
}
