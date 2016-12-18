package net.zerobuilder.modules.generics;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeName.VOID;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoGoalDetails.regularDetailsCases;
import static net.zerobuilder.compiler.generate.NullPolicy.REJECT;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.ZeroUtil.joinCodeBlocks;
import static net.zerobuilder.compiler.generate.ZeroUtil.nullCheck;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;
import static net.zerobuilder.compiler.generate.ZeroUtil.rawClassName;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.compiler.generate.ZeroUtil.transform;

final class GenericsImpl {

  private final ClassName impl;
  private final ClassName contract;
  private final SimpleRegularGoalDescription description;

  List<TypeSpec> stepImpls(List<TypeSpec> stepSpecs,
                           List<List<TypeVariableName>> methodParams,
                           List<List<TypeVariableName>> typeParams) {
    List<TypeSpec> builder = new ArrayList<>(stepSpecs.size());
    ImplFields implFields = new ImplFields(impl, description, stepSpecs, typeParams);
    for (int i = 0; i < stepSpecs.size(); i++) {
      builder.add(createStep(implFields, stepSpecs, methodParams, typeParams, i));
    }
    return builder;
  }

  TypeSpec createStep(ImplFields implFields,
                      List<TypeSpec> stepSpecs,
                      List<List<TypeVariableName>> methodParams,
                      List<List<TypeVariableName>> typeParams, int i) {
    TypeSpec stepSpec = stepSpecs.get(i);
    MethodSpec method = stepSpec.methodSpecs.get(0);
    ParameterSpec parameter = method.parameters.get(0);
    List<FieldSpec> fields = implFields.fields.apply(description.details, i);
    TypeName superinterface = parameterizedTypeName(contract.nestedClass(stepSpec.name),
        stepSpec.typeVariables);
    TypeSpec.Builder builder = classBuilder(stepSpec.name + "Impl");
    if (!fields.isEmpty()) {
      builder.addMethod(createConstructor(fields));
    }
    return builder.addFields(fields)
        .addSuperinterface(superinterface)
        .addTypeVariables(typeParams.get(i))
        .addMethod(methodBuilder(method.name)
            .addAnnotation(Override.class)
            .addParameter(parameter)
            .addTypeVariables(methodParams.get(i))
            .addModifiers(PUBLIC)
            .returns(method.returnType)
            .addCode(getCodeBlock(stepSpecs, i, parameter))
            .addExceptions(i == description.parameters.size() - 1 ?
                description.thrownTypes :
                emptyList())
            .build())
        .addModifiers(PRIVATE, STATIC, FINAL)
        .build();
  }

  private CodeBlock getCodeBlock(List<TypeSpec> stepSpecs, int i, ParameterSpec parameter) {
    CodeBlock.Builder builder = CodeBlock.builder();
    if (description.parameters.get(i).nullPolicy == REJECT
        && !description.parameters.get(i).type.isPrimitive()) {
      builder.add(nullCheck(parameter.name, parameter.name));
    }
    if (i == stepSpecs.size() - 1) {
      return builder.add(fullInvoke(stepSpecs)).build();
    }
    ClassName next = impl.nestedClass(stepSpecs.get(i + 1).name + "Impl");
    return builder.addStatement("return new $T(this, $N)", next, parameter).build();
  }

  private CodeBlock fullInvoke(List<TypeSpec> stepSpecs) {
    List<CodeBlock> blocks = basicInvoke(stepSpecs);
    CodeBlock invoke = description.unshuffle(blocks)
        .stream()
        .collect(joinCodeBlocks(", "));
    return regularDetailsCases(
        constructor -> statement("return new $T($L)",
            rawClassName(description.context.type), invoke),
        staticMethod -> CodeBlock.builder()
            .add(staticMethod.goalType == VOID ? emptyCodeBlock : CodeBlock.of("return "))
            .addStatement("$T.$L($L)",
                rawClassName(description.context.type),
                staticMethod.methodName, invoke).build(),
        instanceMethod -> CodeBlock.builder()
            .add(instanceMethod.goalType == VOID ? emptyCodeBlock : CodeBlock.of("return "))
            .addStatement("$L.$L($L)",
                instance(stepSpecs),
                instanceMethod.methodName, invoke).build())
        .apply(description.details);
  }

  private static CodeBlock instance(List<TypeSpec> stepSpecs) {
    CodeBlock.Builder builder = CodeBlock.builder();
    for (int i = stepSpecs.size() - 2; i >= 0; i--) {
      TypeSpec type = stepSpecs.get(i);
      builder.add("$L.", downcase(type.name) + "Impl");
    }
    return builder.add("instance").build();
  }

  private static IntFunction<CodeBlock> invokeFn(List<TypeSpec> stepSpecs) {
    return i -> {
      CodeBlock.Builder block = CodeBlock.builder();
      for (int j = stepSpecs.size() - 3; j >= i; j--) {
        TypeSpec type = stepSpecs.get(j + 1);
        MethodSpec method = type.methodSpecs.get(0);
        ParameterSpec parameter = method.parameters.get(0);
        block.add("$N", parameter).add("Impl.");
      }
      TypeSpec type = stepSpecs.get(i);
      MethodSpec method = type.methodSpecs.get(0);
      ParameterSpec parameter = method.parameters.get(0);
      block.add("$N", parameter);
      return block.build();
    };
  }

  static List<CodeBlock> basicInvoke(List<TypeSpec> stepSpecs) {
    IntFunction<CodeBlock> invokeFn = invokeFn(stepSpecs);
    return IntStream.range(0, stepSpecs.size())
        .mapToObj(invokeFn)
        .collect(toList());
  }

  private MethodSpec createConstructor(List<FieldSpec> fields) {
    List<ParameterSpec> parameters = transform(fields, field -> parameterSpec(field.type, field.name));
    return MethodSpec.constructorBuilder()
        .addParameters(parameters)
        .addCode(parameters.stream().map(parameter -> statement("this.$N = $N", parameter, parameter))
            .collect(joinCodeBlocks))
        .build();
  }

  GenericsImpl(ClassName impl, ClassName contract, SimpleRegularGoalDescription description) {
    this.impl = impl;
    this.contract = contract;
    this.description = description;
  }
}
