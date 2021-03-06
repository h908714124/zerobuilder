package net.zerobuilder.compiler.common;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleElementVisitor6;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import static javax.lang.model.element.ElementKind.PACKAGE;
import static javax.lang.model.util.ElementFilter.fieldsIn;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static net.zerobuilder.compiler.common.LessTypes.asTypeElement;

/**
 * Guava-free versions of some helpers from auto-common.
 */
public final class LessElements {

  private static final ElementVisitor<TypeElement, Void> TYPE_ELEMENT_VISITOR =
      new SimpleElementVisitor6<TypeElement, Void>() {
        @Override
        protected TypeElement defaultAction(Element e, Void p) {
          throw new IllegalArgumentException();
        }

        @Override
        public TypeElement visitType(TypeElement e, Void p) {
          return e;
        }
      };

  private static final ElementVisitor<ExecutableElement, Void> EXECUTABLE_ELEMENT_VISITOR =
      new SimpleElementVisitor6<ExecutableElement, Void>() {
        @Override
        protected ExecutableElement defaultAction(Element e, Void p) {
          throw new IllegalArgumentException();
        }

        @Override
        public ExecutableElement visitExecutable(ExecutableElement e, Void p) {
          return e;
        }
      };

  /**
   * Find all non-static, visible methods that match the predicate, and group by name.
   * In case of name conflict, the first found wins.
   * The iteration order is:
   * <ul>
   * <li>{@code type} first, {@code Object} last</li>
   * <li>concrete types before interfaces</li>
   * </ul>
   * Ideally the {@code predicate} should prevent name conflicts.
   *
   * @param type      type to search
   * @param predicate filter
   * @return methods by name
   */
  public static Map<String, ExecutableElement> getLocalAndInheritedMethods(
      TypeElement type, Predicate<ExecutableElement> predicate) {
    Map<String, ExecutableElement> methods = new LinkedHashMap<>();
    PackageElement pkg = getPackage(type);
    addFromSuperclass(pkg, type, methods, predicate);
    addFromInterfaces(pkg, type, methods, predicate);
    return methods;
  }

  public static Map<String, VariableElement> getLocalAndInheritedFields(
      TypeElement type) {
    Map<String, VariableElement> fields = new LinkedHashMap<>();
    PackageElement pkg = getPackage(type);
    addFieldsFromSuperclass(pkg, type, fields);
    return fields;
  }

  private static void addFieldsFromSuperclass(
      PackageElement pkg, TypeElement type, Map<String, VariableElement> methods) {
    addEnclosedFields(pkg, type, methods);
    TypeMirror superclass = type.getSuperclass();
    if (superclass.getKind() == TypeKind.NONE) {
      return;
    }
    addFieldsFromSuperclass(pkg, asTypeElement(superclass), methods);
  }

  private static void addFromSuperclass(
      PackageElement pkg, TypeElement type, Map<String, ExecutableElement> methods,
      Predicate<ExecutableElement> predicate) {
    addEnclosedMethods(pkg, type, methods, predicate);
    TypeMirror superclass = type.getSuperclass();
    if (superclass.getKind() == TypeKind.NONE) {
      return;
    }
    addFromSuperclass(pkg, asTypeElement(superclass), methods,
        predicate);
  }

  private static void addFromInterfaces(
      PackageElement pkg, TypeElement type, Map<String, ExecutableElement> methods,
      Predicate<ExecutableElement> predicate) {
    addEnclosedMethods(pkg, type, methods, predicate);
    for (TypeMirror superInterface : type.getInterfaces()) {
      addFromInterfaces(pkg, asTypeElement(superInterface), methods,
          predicate);
    }
  }

  private static void addEnclosedMethods(PackageElement pkg, TypeElement type, Map<String, ExecutableElement> methods,
                                         Predicate<ExecutableElement> predicate) {
    methodsIn(type.getEnclosedElements())
        .stream()
        .filter(predicate)
        .forEach(method -> {
          if (method.getKind() == ElementKind.METHOD
              && !method.getModifiers().contains(Modifier.STATIC)
              && methodVisibleFromPackage(method, pkg)) {
            methods.computeIfAbsent(method.getSimpleName().toString(), name -> method);
          }
        });
  }

  private static void addEnclosedFields(PackageElement pkg, TypeElement type, Map<String, VariableElement> fields) {
    fieldsIn(type.getEnclosedElements())
        .stream()
        .forEach(field -> {
          if (field.getKind() == ElementKind.FIELD
              && !field.getModifiers().contains(Modifier.STATIC)
              && methodVisibleFromPackage(field, pkg)) {
            fields.computeIfAbsent(field.getSimpleName().toString(), name -> field);
          }
        });
  }

  private static boolean methodVisibleFromPackage(Element method, PackageElement pkg) {
    Visibility visibility = Visibility.ofElement(method);
    switch (visibility) {
      case PRIVATE:
        return false;
      case DEFAULT:
        return getPackage(method).equals(pkg);
      default:
        return true;
    }
  }

  private static PackageElement getPackage(Element element) {
    while (element.getKind() != PACKAGE) {
      element = element.getEnclosingElement();
    }
    return (PackageElement) element;
  }

  public static ExecutableElement asExecutable(Element element) {
    return element.accept(EXECUTABLE_ELEMENT_VISITOR, null);
  }

  static TypeElement asType(Element element) {
    return element.accept(TYPE_ELEMENT_VISITOR, null);
  }

  private LessElements() {
    throw new UnsupportedOperationException("no instances");
  }
}
