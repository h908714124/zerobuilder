package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.function.Function;

public final class DtoGoalDetails {

  public interface AbstractGoalDetails {

    /**
     * Returns the goal name.
     *
     * @return goal name
     */
    String name();
    Modifier[] access(Modifier... modifiers);
    TypeName type();

    <R> R acceptAbstract(AbstractGoalDetailsCases<R> cases);
  }

  interface AbstractGoalDetailsCases<R> {
    R regular(AbstractRegularDetails details);
    R bean(BeanGoalDetails details);
  }

  interface RegularGoalDetailsCases<R> {
    R method(InstanceMethodGoalDetails details);
    R staticMethod(StaticMethodGoalDetails details);
    R constructor(ConstructorGoalDetails details);
  }

  public static <R> Function<AbstractGoalDetails, R> asFunction(AbstractGoalDetailsCases<R> cases) {
    return details -> details.acceptAbstract(cases);
  }

  public static <R> Function<AbstractRegularDetails, R> asFunction(RegularGoalDetailsCases<R> cases) {
    return details -> details.accept(cases);
  }

  public interface ProjectableDetails extends AbstractGoalDetails {
    <R> R accept(ProjectableDetailsCases<R> cases);
  }

  interface ProjectableDetailsCases<R> {
    R constructor(ConstructorGoalDetails constructor);
    R method(StaticMethodGoalDetails method);
  }

  static <R> Function<ProjectableDetails, R> asFunction(ProjectableDetailsCases<R> cases) {
    return details -> details.accept(cases);
  }

  static <R> Function<ProjectableDetails, R> projectableDetailsCases(
      Function<ConstructorGoalDetails, R> constructorFunction,
      Function<StaticMethodGoalDetails, R> methodFunction) {
    return asFunction(new ProjectableDetailsCases<R>() {
      @Override
      public R constructor(ConstructorGoalDetails constructor) {
        return constructorFunction.apply(constructor);
      }
      @Override
      public R method(StaticMethodGoalDetails method) {
        return methodFunction.apply(method);
      }
    });
  }

  static final Function<ProjectableDetails, List<String>> parameterNames =
      projectableDetailsCases(
          constructor -> constructor.parameterNames,
          method -> method.parameterNames);

  public static abstract class AbstractRegularDetails implements AbstractGoalDetails {

    final String name;
    final Access access;

    /**
     * <p>method goal: return type</p>
     * <p>constructor goal: type of enclosing class</p>
     */
    final TypeName goalType;

    /**
     * parameter names in original order
     */
    final List<String> parameterNames;

    public final String name() {
      return name;
    }

    public final Modifier[] access(Modifier... modifiers) {
      return access.modifiers(modifiers);
    }

    public final TypeName type() {
      return goalType;
    }

    /**
     * @param goalType       goal type
     * @param name           goal name
     * @param parameterNames parameter names in original order
     * @param access         goal options
     */
    AbstractRegularDetails(TypeName goalType, String name, List<String> parameterNames,
                           Access access) {
      this.name = name;
      this.access = access;
      this.goalType = goalType;
      this.parameterNames = parameterNames;
    }

    @Override
    public final <R> R acceptAbstract(AbstractGoalDetailsCases<R> cases) {
      return cases.regular(this);
    }

    abstract <R> R accept(RegularGoalDetailsCases<R> cases);
  }

  public static final class ConstructorGoalDetails extends AbstractRegularDetails
      implements ProjectableDetails, AbstractGoalDetails {

    private ConstructorGoalDetails(TypeName goalType, String name, List<String> parameterNames,
                                   Access access) {
      super(goalType, name, parameterNames, access);
    }

    public static ConstructorGoalDetails create(TypeName goalType, String name, List<String> parameterNames,
                                                Access access) {
      return new ConstructorGoalDetails(goalType, name, parameterNames, access);
    }

    @Override
    <R> R accept(RegularGoalDetailsCases<R> cases) {
      return cases.constructor(this);
    }

    @Override
    public <R> R accept(ProjectableDetailsCases<R> cases) {
      return cases.constructor(this);
    }
  }

  public static final class InstanceMethodGoalDetails extends AbstractRegularDetails {
    final String methodName;

    private InstanceMethodGoalDetails(TypeName goalType, String name, List<String> parameterNames, String methodName,
                                      Access access) {
      super(goalType, name, parameterNames, access);
      this.methodName = methodName;
    }

    public static InstanceMethodGoalDetails create(TypeName goalType,
                                                   String name,
                                                   List<String> parameterNames,
                                                   String methodName,
                                                   Access access) {
      return new InstanceMethodGoalDetails(goalType, name, parameterNames, methodName, access);
    }

    @Override
    <R> R accept(RegularGoalDetailsCases<R> cases) {
      return cases.method(this);
    }
  }

  public static final class StaticMethodGoalDetails extends AbstractRegularDetails
      implements ProjectableDetails, AbstractGoalDetails {
    final String methodName;

    private StaticMethodGoalDetails(TypeName goalType, String name, List<String> parameterNames, String methodName,
                                    Access access) {
      super(goalType, name, parameterNames, access);
      this.methodName = methodName;
    }

    public static StaticMethodGoalDetails create(TypeName goalType,
                                                 String name,
                                                 List<String> parameterNames,
                                                 String methodName,
                                                 Access access) {
      return new StaticMethodGoalDetails(goalType, name, parameterNames, methodName, access);
    }

    @Override
    <R> R accept(RegularGoalDetailsCases<R> cases) {
      return cases.staticMethod(this);
    }

    @Override
    public <R> R accept(ProjectableDetailsCases<R> cases) {
      return cases.method(this);
    }
  }

  public static final class BeanGoalDetails implements AbstractGoalDetails {
    public final ClassName goalType;
    final String name;
    private final Access access;
    public BeanGoalDetails(ClassName goalType, String name, Access access) {
      this.name = name;
      this.access = access;
      this.goalType = goalType;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public Modifier[] access(Modifier... modifiers) {
      return access.modifiers(modifiers);
    }

    @Override
    public TypeName type() {
      return goalType;
    }

    @Override
    public <R> R acceptAbstract(AbstractGoalDetailsCases<R> cases) {
      return cases.bean(this);
    }
  }

  public static final Function<AbstractGoalDetails, TypeName> goalType
      = asFunction(new AbstractGoalDetailsCases<TypeName>() {
    @Override
    public TypeName regular(AbstractRegularDetails goal) {
      return goal.goalType;
    }
    @Override
    public TypeName bean(BeanGoalDetails goal) {
      return goal.goalType;
    }
  });

  private DtoGoalDetails() {
    throw new UnsupportedOperationException("no instances");
  }
}