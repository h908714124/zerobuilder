package net.zerobuilder.compiler.analyse;

import net.zerobuilder.compiler.analyse.DtoGoalElement.AbstractGoalElement;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static net.zerobuilder.compiler.Messages.ErrorMessages.DUPLICATE_GOAL_NAME;
import static net.zerobuilder.compiler.Messages.ErrorMessages.PRIVATE_METHOD;
import static net.zerobuilder.compiler.analyse.DtoGoalElement.element;
import static net.zerobuilder.compiler.analyse.DtoGoalElement.goalName;

final class MoreValidations {

  static void checkNameConflict(List<? extends AbstractGoalElement> goals) throws ValidationException {
    Map<String, List<AbstractGoalElement>> m = goals.stream()
        .collect(groupingBy(goalName));
    m.forEach((name, group) -> {
      if (group.size() == 2) {
        Element el0 = element.apply(group.get(0));
        Element el1 = element.apply(group.get(1));
        if (!el0.equals(el1)) {
          throw new ValidationException(DUPLICATE_GOAL_NAME, el1);
        }
      }
      if (group.size() > 2) {
        Element el = element.apply(group.get(2));
        throw new ValidationException(DUPLICATE_GOAL_NAME, el);
      }
    });
  }

  static void checkAccessLevel(List<? extends AbstractGoalElement> goals) throws ValidationException {
    goals.stream().map(element)
        .forEach(el -> {
          if (el.getModifiers().contains(Modifier.PRIVATE)) {
            throw new ValidationException(PRIVATE_METHOD, el);
          }
        });
  }

  private MoreValidations() {
    throw new UnsupportedOperationException("no instances");
  }
}
