# A flexible builder scheme

This project has two different use cases:

* Create and update immutable value objects with minimal effort. 
  See [values](values.md).
* Create mutable JavaBeans and update them with "immutable semantics", i.e. by making shallow copies.
  See [beans](beans.md).

### How to use

This is a standard Java &ge; 7 annotation processor.
The generated code has no runtime dependencies.
After compilation, the annotated source does not depend on zerobuilder; see
[RetentionPolicy.SOURCE](https://docs.oracle.com/javase/7/docs/api/java/lang/annotation/RetentionPolicy.html#SOURCE).

Maven compiler plugin version `3.5.1` or greater is recommended.

Your IDE may need some initial help, to recognize that `target/generated-sources/annotations`
now contains generated sources.

<em>Tip for intellij users:</em> If you do a `mvn install` before opening one of the example projects,
intellij will recognize `target/generated-sources/annotations` automatically.

### Why zero?

Because using the generated builders has zero impact on garbage collection, if the `recycle` option is used.
In this case, the intermediate builder objects are stored in `ThreadLocal` instances and reused.

### Maven

````xml
<dependency>
    <groupId>com.github.h908714124</groupId>
    <artifactId>zerobuilder</artifactId>
    <version>1.471</version>
    <scope>provided</scope>
</dependency>
````
