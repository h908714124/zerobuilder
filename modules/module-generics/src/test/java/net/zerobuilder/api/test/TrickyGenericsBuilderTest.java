package net.zerobuilder.api.test;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import net.zerobuilder.compiler.generate.DtoContext;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.RegularSimpleGoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.DtoGoalDetails.StaticMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;
import net.zerobuilder.compiler.generate.Generator;
import net.zerobuilder.modules.generics.GenericsBuilder;
import org.junit.Ignore;
import org.junit.Test;

import javax.lang.model.element.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.groupingBy;
import static net.zerobuilder.compiler.generate.Access.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.NEW_INSTANCE;
import static net.zerobuilder.compiler.generate.DtoContext.createContext;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TrickyGenericsBuilderTest {

  private static final ClassName TYPE = ClassName.get(TrickyGenericsBuilderTest.class)
      .peerClass("MyType");

  // the type we wish to generate; in this case, a nested type
  private static final ClassName GENERATED_TYPE = ClassName.get(TrickyGenericsBuilderTest.class)
      .nestedClass("MyTypeBuilders");

  private static final TypeVariableName K = TypeVariableName.get("K");
  private static final TypeVariableName V = TypeVariableName.get("V");

  private static final ParameterizedTypeName LIST_OF_K =
      ParameterizedTypeName.get(ClassName.get(List.class), K);
  private static final ParameterizedTypeName LIST_OF_V =
      ParameterizedTypeName.get(ClassName.get(List.class), V);
  private static final ParameterizedTypeName MAP_K_LIST_V =
      ParameterizedTypeName.get(ClassName.get(Map.class), K, LIST_OF_V);

  /**
   * <p>We want to generate a generics for {@code MyType#create(String, Integer)}
   * </p>
   * <pre><code>
   *   class MyType {
   *     // our goal method
   *     static <K, V> List<V> getList(Map<K, List<V>> source, K key, V defaultValue) {
   *       return null;
   *     }
   *   }
   * </pre></code>
   */
  @Ignore
  @Test
  public void staticMethodGoal() {

    // create goal context
    DtoContext.GoalContext goalContext = createContext(
        TYPE, // type that contains the goal method; in this case, this is the same as the goal type
        GENERATED_TYPE // the type we wish to generate; it will contain all the generated code
    );

    // create goal details
    String goalName = "multiKey"; // free choice, but should be a valid java identifier
    StaticMethodGoalDetails details = StaticMethodGoalDetails.create(
        LIST_OF_V, // return type of the goal method
        goalName,
        asList("source", "key", "defaultValue"),
        "getList",
        PRIVATE,
        asList(K, V),
        NEW_INSTANCE);

    // use SimpleParameter because the generics module doesn't need projections
    SimpleParameter fooParameter = DtoRegularParameter.create("source", MAP_K_LIST_V);
    SimpleParameter barParameter = DtoRegularParameter.create("key", K);
    SimpleParameter tarParameter = DtoRegularParameter.create("defaultValue", V);
    SimpleRegularGoalDescription description = SimpleRegularGoalDescription.create(
        details,
        Collections.emptyList(), // the goal method declares no exceptions
        // step order; not necessarily the order of the goal parameters
        asList(fooParameter, barParameter, tarParameter),
        goalContext);

    // Invoke the generator
    GeneratorOutput generatorOutput = Generator.generate(
        singletonList(new RegularSimpleGoalInput(new GenericsBuilder(), description)));

    assertThat(generatorOutput.methods().size(), is(1));
    assertThat(generatorOutput.methods().get(0).name(), is(goalName));
    assertThat(generatorOutput.methods().get(0).method().name, is("multiKeyBuilder"));
    assertThat(generatorOutput.methods().get(0).method().parameters.size(), is(0));
    assertThat(generatorOutput.methods().get(0).method().modifiers.contains(Modifier.STATIC), is(true));
    assertThat(generatorOutput.methods().get(0).method().modifiers.contains(Modifier.PRIVATE), is(true));
    Map<String, TypeSpec> nested = unique(generatorOutput.nestedTypes().stream().collect(groupingBy(type -> type.name)));
    TypeSpec contract = nested.get("MultiKeyBuilder");
    Map<String, TypeSpec> steps = unique(contract.typeSpecs.stream().collect(groupingBy(type -> type.name)));
    checkKeysContract(steps.get("Keys"));
    checkValueContract(steps.get("Value"));
  }

  private void checkKeysContract(TypeSpec keys) {
    assertThat(keys.methodSpecs.size(), is(1));
    assertThat(keys.typeVariables.size(), is(0));
    MethodSpec stepMethod = keys.methodSpecs.get(0);
    assertThat(stepMethod.typeVariables, is(singletonList(K)));
    assertThat(stepMethod.returnType, is(LIST_OF_K));
  }

  private void checkValueContract(TypeSpec value) {
    assertThat(value.methodSpecs.size(), is(1));
    assertThat(value.typeVariables, is(singletonList(K)));
    MethodSpec method = value.methodSpecs.get(0);
    assertThat(method.typeVariables, is(singletonList(V)));
    assertThat(method.returnType, is(MAP_K_LIST_V));
  }

  private static <K, V> Map<K, V> unique(Map<K, List<V>> map) {
    HashMap<K, V> m = new HashMap<>();
    for (Map.Entry<K, List<V>> entry : map.entrySet()) {
      assertThat(entry.getValue().size(), is(1));
      m.put(entry.getKey(), entry.getValue().get(0));
    }
    return m;
  }
}