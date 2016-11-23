package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static net.zerobuilder.compiler.test_util.GeneratedLines.GENERATED_ANNOTATION;

public class RecyclingConstructorTest {

  @Test
  public void simpleCube() {
    JavaFileObject cube = forSourceLines("cube.Cube",
        "package cube;",
        "import net.zerobuilder.*;",
        "import java.util.List;",
        "",
        "@Builders(recycle = true)",
        "final class Cube {",
        "  final String width;",
        "  final List<String> length;",
        "  @Goal(updater = true)",
        "  Cube(String width, List<String> length) {",
        "    this.width = width;",
        "    this.length = length;",
        "  }",
        "}");
    JavaFileObject expected =
        forSourceLines("cube.CubeBuilders",
            "package cube;",
            "import java.util.List;",
            "import javax.annotation.Generated;",
            "",
            GENERATED_ANNOTATION,
            "public final class CubeBuilders {",
            "  private static final ThreadLocal<CubeBuilders> INSTANCE = new ThreadLocal<CubeBuilders>() {",
            "    @Override",
            "    protected CubeBuilders initialValue() {",
            "      return new CubeBuilders();",
            "    }",
            "  }",
            "",
            "  private CubeUpdater cubeUpdater = new CubeUpdater();",
            "  private CubeBuilderImpl cubeBuilderImpl = new CubeBuilderImpl();",
            "  private CubeBuilders() {}",
            "",
            "  public static CubeUpdater cubeUpdater(Cube cube) {",
            "    CubeBuilders context = INSTANCE.get();",
            "    if (context.cubeUpdater._currently_in_use) {",
            "      context.cubeUpdater = new CubeUpdater();",
            "    }",
            "    CubeUpdater updater = context.cubeUpdater;",
            "    updater._currently_in_use = true;",
            "    updater.width = cube.width;",
            "    updater.length = cube.length;",
            "    return updater;",
            "  }",
            "",
            "  public static CubeBuilder.Width cubeBuilder() {",
            "    CubeBuilders context = INSTANCE.get();",
            "    if (context.cubeBuilderImpl._currently_in_use) {",
            "      context.cubeBuilderImpl = new CubeBuilderImpl();",
            "    }",
            "    context.cubeBuilderImpl._currently_in_use = true;",
            "    return context.cubeBuilderImpl;",
            "  }",
            "",
            "  public static final class CubeUpdater {",
            "    private boolean _currently_in_use;",
            "    private String width;",
            "    private List<String> length;",
            "    private CubeUpdater() {}",
            "",
            "    public CubeUpdater width(String width) {",
            "      this.width = width;",
            "      return this;",
            "    }",
            "",
            "    public CubeUpdater length(List<String> length) {",
            "      this.length = length;",
            "      return this;",
            "    }",
            "",
            "    public Cube done() {",
            "      this._currently_in_use = false;",
            "      Cube _cube = new Cube( width, length );",
            "      this.width = null;",
            "      this.length = null;",
            "      return _cube;",
            "    }",
            "  }",
            "",
            "  private static final class CubeBuilderImpl implements",
            "        CubeBuilder.Width , CubeBuilder.Length {",
            "    private boolean _currently_in_use;",
            "    private String width;",
            "    CubeBuilderImpl() {}",
            "",
            "    @Override public CubeBuilder.Length width(String width) {",
            "      this.width = width;",
            "      return this; ",
            "    }",
            "",
            "    @Override public Cube length(List<String> length) {",
            "      this._currently_in_use = false;",
            "      Cube _cube = new Cube( width, length );",
            "      this.width = null;",
            "      return _cube;",
            "    }",
            "  }",
            "",
            "  public static final class CubeBuilder {",
            "    private CubeBuilder() {",
            "      throw new UnsupportedOperationException(\"no instances\");",
            "    }",
            "    public interface Width { Length width(String width); }",
            "    public interface Length {",
            "      Cube length(List<String> length);",
            "    }",
            "  }",
            "}");
    assertAbout(javaSources()).that(ImmutableList.of(cube))
        .processedWith(new ZeroProcessor())
        .compilesWithoutError()
        .and().generatesSources(expected);
  }

}
