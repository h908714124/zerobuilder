package isobuilder.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.ExecutableElement;
import java.util.HashSet;
import java.util.Set;

import static isobuilder.compiler.ErrorMessages.DUPLICATE;

final class DuplicateValidator {

  private final Set<TypeName> annotatedClasses = new HashSet<>();

  ValidationReport<ExecutableElement> validateClassname(ExecutableElement element) {
    ValidationReport.Builder<ExecutableElement> builder = ValidationReport.about(element);
    if (!annotatedClasses.add(ClassName.get(element.getEnclosingElement().asType()))) {
      builder.addError(DUPLICATE);
    }
    return builder.build();
  }

}
