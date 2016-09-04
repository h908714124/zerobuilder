package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;

import javax.annotation.Generated;
import javax.lang.model.util.Elements;

final class Messages {

  static final class ErrorMessages {

    static final String PRIVATE_METHOD =
        "The goal may not be private.";

    static final String PRIVATE_TYPE =
        "The @Build annotated type may not be private.";

    static final String NOT_ENOUGH_PARAMETERS =
        "The goal must have at least one parameter.";

    static final String NESTING_KIND =
        "The @Build annotation can only be used on top level and non-private static inner classes.";

    static final String COULD_NOT_GUESS_GOAL =
        "Could not guess the goal. Please add a @Goal annotation.";

    static final String MULTIPLE_TOBUILDER =
        "Only one goal can have the toBuilder flag set.";

    static final String GOAL_NOT_IN_BUILD =
        "The @Goal annotation may not appear outside a class that carries the @Build annotation.";

    private ErrorMessages() {
    }

  }

  static final class JavadocMessages {

    static final String GENERATED_COMMENTS = "https://github.com/h908714124/zerobuilder";

    static ImmutableList<AnnotationSpec> generatedAnnotations(Elements elements) {
      if (elements.getTypeElement("javax.annotation.Generated") != null) {
        return ImmutableList.of(AnnotationSpec.builder(Generated.class)
            .addMember("value", "$S", ZeroProcessor.class.getName())
            .addMember("comments", "$S", GENERATED_COMMENTS)
            .build());
      }
      return ImmutableList.of();

    }

    private JavadocMessages() {
    }

  }

  private Messages() {
  }

}
