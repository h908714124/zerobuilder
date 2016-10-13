package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static net.zerobuilder.compiler.GeneratedLines.GENERATED_ANNOTATION;

public class RecyclingBeanTest {

  @Test
  public void recycling() {
    JavaFileObject businessAnalyst = forSourceLines("beans.BusinessAnalyst",
        "package beans;",
        "import net.zerobuilder.*;",
        "import java.util.List;",
        "import java.util.ArrayList;",
        "",
        "@Builders(recycle = true)",
        "@Goal(updater = true, builderAccess = AccessLevel.PACKAGE)",
        "public class BusinessAnalyst {",
        "  private String name;",
        "  private List<String> notes;",
        "  public String getName() { return name; }",
        "  public void setName(String name) { this.name = name; }",
        "  @Step(nullPolicy = NullPolicy.REJECT) public List<String> getNotes() {",
        "    if (notes == null) notes = new ArrayList<>();",
        "    return notes;",
        "  }",
        "}");
    JavaFileObject expected =
        forSourceLines("beans.BusinessAnalystBuilders",
            "package beans;",
            "import javax.annotation.Generated;",
            "",
            GENERATED_ANNOTATION,
            "public final class BusinessAnalystBuilders {",
            "  private static final ThreadLocal<BusinessAnalystBuilders> INSTANCE = new ThreadLocal<BusinessAnalystBuilders>() {",
            "    @Override",
            "    protected BusinessAnalystBuilders initialValue() {",
            "      return new BusinessAnalystBuilders();",
            "    }",
            "  }",
            "",
            "  private final BusinessAnalystUpdater businessAnalystUpdater = new BusinessAnalystUpdater();",
            "  private final BusinessAnalystBuilderImpl businessAnalystBuilderImpl = new BusinessAnalystBuilderImpl();",
            "  private BusinessAnalystBuilders() {}",
            "",
            "  static BusinessAnalystBuilder.Name businessAnalystBuilder() {",
            "    BusinessAnalystBuilderImpl businessAnalystBuilderImpl = INSTANCE.get().businessAnalystBuilderImpl;",
            "    businessAnalystBuilderImpl.businessAnalyst = new BusinessAnalyst();",
            "    return businessAnalystBuilderImpl;",
            "  }",
            "",
            "  public static BusinessAnalystUpdater businessAnalystUpdater(BusinessAnalyst businessAnalyst) {",
            "    if (businessAnalyst.getNotes() == null) {",
            "      throw new NullPointerException(\"notes\");",
            "    }",
            "    BusinessAnalystUpdater updater = INSTANCE.get().businessAnalystUpdater;",
            "    updater.businessAnalyst = new BusinessAnalyst();",
            "    updater.businessAnalyst.setName(businessAnalyst.getName());",
            "    for (String string : businessAnalyst.getNotes()) {",
            "      updater.businessAnalyst.getNotes().add(string);",
            "    }",
            "    return updater;",
            "  }",
            "",
            "  public static final class BusinessAnalystUpdater {",
            "    private BusinessAnalyst businessAnalyst;",
            "    private BusinessAnalystUpdater() {}",
            "",
            "    public BusinessAnalystUpdater name(String name) {",
            "      this.businessAnalyst.setName(name);",
            "      return this;",
            "    }",
            "",
            "    public BusinessAnalystUpdater notes(Iterable<? extends String> notes) {",
            "      if (notes == null) {",
            "        throw new NullPointerException(\"notes\");",
            "      }",
            "      this.businessAnalyst.getNotes().clear();",
            "      for (String string : notes) {",
            "        this.businessAnalyst.getNotes().add(string);",
            "      }",
            "      return this;",
            "    }",
            "",
            "    public BusinessAnalystUpdater emptyNotes() {",
            "      this.businessAnalyst.getNotes().clear();",
            "      return this;",
            "    }",
            "",
            "    public BusinessAnalyst done() { return this.businessAnalyst; }",
            "  }",
            "",
            "  static final class BusinessAnalystBuilderImpl",
            "        implements BusinessAnalystBuilder.Name, BusinessAnalystBuilder.Notes {",
            "    private BusinessAnalyst businessAnalyst;",
            "    private BusinessAnalystBuilderImpl() {}",
            "",
            "    @Override public BusinessAnalystBuilder.Notes name(String name) {",
            "      this.businessAnalyst.setName(name);",
            "      return this;",
            "    }",
            "",
            "    @Override public BusinessAnalyst notes(Iterable<? extends String> notes) {",
            "      if (notes == null) {",
            "        throw new NullPointerException(\"notes\");",
            "      }",
            "      for (String string : notes) {",
            "        this.businessAnalyst.getNotes().add(string);",
            "      }",
            "      return this.businessAnalyst;",
            "    }",
            "",
            "    @Override public BusinessAnalyst emptyNotes() {",
            "      return this.businessAnalyst;",
            "    }",
            "  }",
            "",
            "  public static final class BusinessAnalystBuilder {",
            "    private BusinessAnalystBuilder() {",
            "      throw new UnsupportedOperationException(\"no instances\");",
            "    }",
            "    public interface Name { Notes name(String name); }",
            "    public interface Notes {",
            "      BusinessAnalyst notes(Iterable<? extends String> notes);",
            "      BusinessAnalyst emptyNotes();",
            "    }",
            "  }",
            "}");
    assertAbout(javaSources()).that(ImmutableList.of(businessAnalyst))
        .processedWith(new ZeroProcessor())
        .compilesWithoutError()
        .and().generatesSources(expected);
  }
}
