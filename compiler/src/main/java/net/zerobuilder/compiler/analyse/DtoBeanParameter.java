package net.zerobuilder.compiler.analyse;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.analyse.DtoValidParameter.ValidParameter;

import javax.lang.model.type.TypeMirror;

import static com.google.auto.common.MoreTypes.asTypeElement;
import static net.zerobuilder.compiler.Utilities.distinctFrom;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.Utilities.parameterSpec;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorB.OBJECT;

public final class DtoBeanParameter {

  public static abstract class ValidBeanParameter extends ValidParameter {

    /**
     * Name of the getter method (could start with {@code "is"})
     */
    public final String getter;

    ValidBeanParameter(TypeName type, String getter, boolean nonNull) {
      super(name(getter), type, nonNull);
      this.getter = getter;
    }

    private static String name(String projectionMethodName) {
      return downcase(projectionMethodName.substring(projectionMethodName.startsWith("is") ? 2 : 3));
    }

    abstract <R> R accept(BeanParameterCases<R> cases);

  }

  interface BeanParameterCases<R> {
    R accessorPair(AccessorPair pair);
    R loneGetter(LoneGetter getter);
  }

  public static final class AccessorPair extends ValidBeanParameter {
    AccessorPair(TypeName type, String getter, boolean nonNull) {
      super(type, getter, nonNull);
    }
    @Override
    <R> R accept(BeanParameterCases<R> cases) {
      return cases.accessorPair(this);
    }
  }

  public static final class LoneGetter extends ValidBeanParameter {

    /**
     * Example: If getter returns {@code List<String>}, then this would be a variable of type
     * {@ccode String}
     */
    private final ParameterSpec iterationVar;
    public final boolean allowShortcut;
    public TypeName iterationType() {
      return iterationVar.type;
    }

    /**
     * avoid clashing variable names
     *
     * @param avoid a variable name that is already taken
     * @return a variable that's different from {@code avoid}
     */
    public ParameterSpec iterationVar(ParameterSpec avoid) {
      if (!iterationVar.name.equals(avoid.name)) {
        return iterationVar;
      }
      return parameterSpec(iterationVar.type,
          distinctFrom(iterationVar.name, avoid.name));
    }

    private LoneGetter(TypeName type, String getter, boolean nonNull, ParameterSpec iterationVar, boolean allowShortcut) {
      super(type, getter, nonNull);
      this.iterationVar = iterationVar;
      this.allowShortcut = allowShortcut;
    }
    @Override
    <R> R accept(BeanParameterCases<R> cases) {
      return cases.loneGetter(this);
    }
  }

  public static final class LoneGetterBuilder {
    private final ParameterSpec iterationVar;
    private final boolean allowShortcut;
    LoneGetterBuilder(ParameterSpec iterationVar, boolean allowShortcut) {
      this.iterationVar = iterationVar;
      this.allowShortcut = allowShortcut;
    }
    public LoneGetter build(TypeName type, String getter, boolean nonNull) {
      return new LoneGetter(type, getter, nonNull, iterationVar, allowShortcut);
    }
  }

  static LoneGetterBuilder builder(TypeMirror type, boolean allowShortcut) {
    TypeName typeName = TypeName.get(type);
    String name = downcase(ClassName.get(asTypeElement(type)).simpleName().toString());
    ParameterSpec iterationVar = ParameterSpec.builder(typeName, name).build();
    return new LoneGetterBuilder(iterationVar, allowShortcut);
  }

  static LoneGetterBuilder builder() {
    ParameterSpec iterationVar = parameterSpec(OBJECT, "object");
    return new LoneGetterBuilder(iterationVar, false);
  }

  private DtoBeanParameter() {
    throw new UnsupportedOperationException("no instances");
  }
}
