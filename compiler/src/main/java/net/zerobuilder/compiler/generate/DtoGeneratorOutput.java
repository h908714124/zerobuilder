package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.util.List;

import static com.google.common.collect.Iterables.transform;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod.getMethod;
import static net.zerobuilder.compiler.generate.Utilities.constructor;

public final class DtoGeneratorOutput {

  public interface GeneratorOutputCases<R> {
    R success(GeneratorSuccess output);
    R failure(GeneratorFailure failure);
  }

  public interface GeneratorOutput {
    <R> R accept(GeneratorOutputCases<R> cases);
  }

  /**
   * Can be either a {@code builder} or {@code toBuilder} method
   */
  public static final class BuilderMethod {

    private final String name;
    private final MethodSpec method;

    BuilderMethod(String name, MethodSpec method) {
      this.name = name;
      this.method = method;
    }

    /**
     * Returns the name of the goal that generates this method.
     *
     * @return goal name
     */
    public String name() {
      return name;
    }

    public MethodSpec method() {
      return method;
    }

    static final Function<BuilderMethod, MethodSpec> getMethod
        = new Function<BuilderMethod, MethodSpec>() {
      @Override
      public MethodSpec apply(BuilderMethod builderMethod) {
        return builderMethod.method;
      }
    };
  }

  public static final class GeneratorSuccess implements GeneratorOutput {

    private final ImmutableList<BuilderMethod> methods;
    private final ImmutableList<TypeSpec> nestedTypes;
    private final ImmutableList<FieldSpec> fields;
    private final ClassName generatedType;

    /**
     * Definition of the generated type.
     *
     * @return a TypeSpec
     */
    public TypeSpec typeSpec(List<AnnotationSpec> generatedAnnotations) {
      return classBuilder(generatedType)
          .addFields(fields)
          .addMethod(constructor(PRIVATE))
          .addMethods(transform(methods, getMethod))
          .addAnnotations(generatedAnnotations)
          .addModifiers(PUBLIC, FINAL)
          .addTypes(nestedTypes)
          .build();
    }

    GeneratorSuccess(ImmutableList<BuilderMethod> methods,
                     ImmutableList<TypeSpec> nestedTypes,
                     ImmutableList<FieldSpec> fields,
                     ClassName generatedType) {
      this.methods = methods;
      this.nestedTypes = nestedTypes;
      this.fields = fields;
      this.generatedType = generatedType;
    }

    @Override
    public <R> R accept(GeneratorOutputCases<R> cases) {
      return cases.success(this);
    }
  }

  public static final class GeneratorFailure implements GeneratorOutput {
    private final String message;

    /**
     * Returns an error message.
     *
     * @return error message
     */
    public String message() {
      return message;
    }
    GeneratorFailure(String message) {
      this.message = message;
    }

    @Override
    public <R> R accept(GeneratorOutputCases<R> cases) {
      return cases.failure(this);
    }
  }

  private DtoGeneratorOutput() {
    throw new UnsupportedOperationException("no instances");
  }
}
