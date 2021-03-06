package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static net.zerobuilder.compiler.test_util.GeneratedLines.GENERATED_ANNOTATION;

public class RecyclingStaticTest {

  @Test
  public void simpleCube() {
    JavaFileObject cube = forSourceLines("cube.Cube",
        "package cube;",
        "import net.zerobuilder.*;",
        "import java.util.List;",
        "",
        "abstract class Cube {",
        "  abstract double height();",
        "  abstract List<String> length();",
        "  abstract String width();",
        "  @Builder",
        "  @Updater",
        "  @GoalName(\"cuboid\")",
        "  @Recycle",
        "  @AccessLevel(Level.PACKAGE)",
        "  static Cube create(double height, List<String> length, String width) {",
        "    return null;",
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
            "  private static final ThreadLocal<CuboidBuilderImpl> cuboidBuilderImpl = new ThreadLocal<CuboidBuilderImpl>() {",
            "    @Override",
            "    protected CuboidBuilderImpl initialValue() {",
            "      return new CuboidBuilderImpl();",
            "    }",
            "  }",
            "",
            "  private static final ThreadLocal<CuboidUpdater> cuboidUpdater = new ThreadLocal<CuboidUpdater>() {",
            "    @Override",
            "    protected CuboidUpdater initialValue() {",
            "      return new CuboidUpdater();",
            "    }",
            "  }",
            "",
            "  private CubeBuilders() {",
            "    throw new UnsupportedOperationException(\"no instances\");",
            "  }",
            "",
            "  static CuboidBuilder.Height cuboidBuilder() {",
            "    CuboidBuilderImpl _builder = cuboidBuilderImpl.get();",
            "    if (_builder._currently_in_use) {",
            "      cuboidBuilderImpl.remove();",
            "      _builder = cuboidBuilderImpl.get();",
            "    }",
            "    _builder._currently_in_use = true;",
            "    return _builder;",
            "  }",
            "",
            "  static CuboidUpdater cuboidUpdater(Cube cube) {",
            "    CuboidUpdater _updater = cuboidUpdater.get();",
            "    if (_updater._currently_in_use) {",
            "      cuboidUpdater.remove();",
            "      _updater = cuboidUpdater.get();",
            "    }",
            "    _updater._currently_in_use = true;",
            "    _updater.height = cube.height();",
            "    _updater.length = cube.length(),",
            "    _updater.width = cube.width();",
            "    return _updater;",
            "  }",
            "",
            "  private static final class CuboidBuilderImpl implements",
            "        CuboidBuilder.Height, CuboidBuilder.Length, CuboidBuilder.Width {",
            "    private boolean _currently_in_use;",
            "    private double height;",
            "    private List<String> length;",
            "    CuboidBuilderImpl() {}",
            "    @Override public CuboidBuilder.Length height(double height) { this.height = height; return this; }",
            "    @Override public CuboidBuilder.Width length(List<String> length) { this.length = length; return this; }",
            "    @Override public Cube width(String width) {",
            "      this._currently_in_use = false;",
            "      Cube _cube = Cube.create( height, length, width );",
            "      this.length = null;",
            "      return _cube;",
            "    }",
            "  }",
            "",
            "  public static final class CuboidBuilder {",
            "    private CuboidBuilder() {",
            "      throw new UnsupportedOperationException(\"no instances\");",
            "    }",
            "    public interface Height { Length height(double height); }",
            "    public interface Length {",
            "      Width length(List<String> length);",
            "    }",
            "    public interface Width { Cube width(String width); }",
            "  }",
            "",
            "  public static final class CuboidUpdater {",
            "    private boolean _currently_in_use;",
            "    private double height;",
            "    private List<String> length;",
            "    private String width;",
            "    private CuboidUpdater() {}",
            "    public CuboidUpdater height(double height) { this.height = height; return this; }",
            "    public CuboidUpdater length(List<String> length) { this.length = length; return this; }",
            "    public CuboidUpdater width(String width) {",
            "      this.width = width;",
            "      return this;",
            "    }",
            "    public Cube done() {",
            "      this._currently_in_use = false;",
            "      Cube _cube = Cube.create( height, length, width );",
            "      this.length = null;",
            "      this.width = null;",
            "      return _cube;",
            "    }",
            "  }",
            "}");
    assertAbout(javaSources()).that(ImmutableList.of(cube))
        .processedWith(new ZeroProcessor())
        .compilesWithoutError()
        .and().generatesSources(expected);
  }

}
