# Graft (PoC) — Compile‑time Java Mapper

> **Status:** Proof‑of‑Concept. APIs and behavior are unstable and subject to change.

Graft is a tiny **annotation‑processor–based mapping library**. You write a small DSL in a “spec” class; the processor
generates a type‑safe mapper — no reflection, no runtime container.

---

## Example: Bean → DTO

**Spec** (`CarToCarDtoMapperSpec.java`)

```java
@MappingSpec(com.detornium.graft.mappers.CarToCarDtoMapper.class)
class CarToCarDtoMapperSpec extends MappingDsl<Car, CarDto> {
    {
        map(Car::getModel).to(CarDto::setCarModel);
        map(Car::getVersion).converting(String::valueOf).to(CarDto::setVersion);
        exclude(CarDto::setOwner);
        map(Car::getPrevOwners).to(CarDto::setPreviousOwners);
        self().converting(CarToCarDtoMapperSpec::carToDescription).to(CarDto::setDescription);
        value("N/A").to(CarDto::setNotes);
    }
}
```

**Generated mapper (excerpt)** (`CarToCarDtoMapper.java`)

```java
private final Function<Integer, String> versionConverter = String::valueOf;
private final Function<Car, String> descriptionConverter = CarToCarDtoMapperSpec::carToDescription;

public final CarDto map(Car src) {
    if (src == null) {
        return null;
    }
    CarDto dst = new CarDto();
    dst.setCarModel(src.getModel());
    dst.setVersion(versionConverter.apply(src.getVersion()));
    dst.setPreviousOwners(src.getPrevOwners());
    dst.setDescription(descriptionConverter.apply(src));
    dst.setNotes("N/A");
    dst.setColor(src.getColor());
    return dst;
}
```

---

## Example: Bean → DTO Record

**Spec** (`CarToCarDtoRecordMapperSpec.java`)

```java
@MappingSpec(com.detornium.graft.mappers.CarToCarDtoRecordMapper.class)
class CarToCarDtoRecordMapperSpec extends MappingDsl<Car, CarDtoRecord> {
    {
        map(Car::getModel).to(CarDtoRecord::carModel);
        map(Car::getVersion).converting(String::valueOf).to(CarDtoRecord::version);
        exclude(CarDtoRecord::owner);
        self().converting(CarToCarDtoRecordMapperSpec::carToDescription).to(CarDtoRecord::description);
        value("N/A").to(CarDtoRecord::notes);
    }
}
```

**Generated mapper (excerpt)** (`CarToCarDtoRecordMapper.java`)

```java
private final Function<Integer, String> versionConverter = String::valueOf;
private final Function<Car, String> descriptionConverter = CarToCarDtoRecordMapperSpec::carToDescription;

public final CarDtoRecord map(Car src) {
    if (src == null) {
        return null;
    }
    return new CarDtoRecord(src.getColor(),
            src.getModel(),
            versionConverter.apply(src.getVersion()),
            null,
            descriptionConverter.apply(src),
            "N/A");
}
```

---

## Example: Copy (Cloneable + List)

**Spec** (`CopySpec.java`)

```java
@MappingSpec(com.detornium.graft.mappers.CopyTestMapper.class)
class CopySpec extends MappingDsl<CopyTestBean, CopyTestDto> { 
    {
        // Cloneable field
        map(CopyTestBean::getObject).copy().to(CopyTestDto::setObject);
        
        // Collection (List) shallow copy
        map(CopyTestBean::getList).copy().to(CopyTestDto::setList);
    }
}
```

**Generated mapper (excerpt)**(`CopyTestMapper.java`)

```java
   dst.setObject((src.getObject() != null) ? (CloneableObject) (src.getObject()).clone() : null);
   dst.setList((src.getList() != null) ? new ArrayList<>(src.getList()) : null);
```
Semantics

.copy() on Cloneable: generates a null-safe clone() call (shallow clone). clone() must be accessible on the runtime type.

.copy() on List: generates a new ArrayList<>(src) (shallow copy). Elements are not deep-cloned.

---

## Example: Nested Mapping

**Spec** (`NestedPropertiesBeanMapperSpec.java`)

```java
@MappingSpec(com.detornium.graft.mappers.NestedPropertiesBeanMapper.class)
class NestedPropertiesBeanMapperSpec extends MappingDsl<NestedPropertiesBean, NestedPropertiesBean> {
    {
        // 1) Nested source → flat destination setter
        map(NestedPropertiesBean::getSubBean1)
                .nested(SubBean1::getSubBean2)
                .nested(SubBean2::getProp2)
                .to(NestedPropertiesBean::setBeanProp);

        // 2) Nested source → nested destination chain
        map(NestedPropertiesBean::getSubBean1)
                .nested(SubBean1::getSubBean2)
                .nested(SubBean2::getProp2)
                .to(bean(NestedPropertiesBean::setSubBean1)
                        .nested(SubBean1::setSubBean2)
                        .nested(SubBean2::setProp2));
    }
}
```

**Generated mapper (excerpt)** (`NestedPropertiesBeanMapper.java`)

```java
@Override
public final NestedPropertiesBean map(NestedPropertiesBean src) {
    if (src == null) {
        return null;
    }

    // The generator introduces temps for shared subpaths (null-safe)
    SubBean2 srcSubBean2 = (src.getSubBean1() != null) ? src.getSubBean1().getSubBean2() : null;
    String   srcProp2    = (srcSubBean2 != null) ? srcSubBean2.getProp2() : null;

    // Nested destination objects are constructed once and reused
    SubBean2 subBean2 = new SubBean2();
    subBean2.setProp2(srcProp2);

    SubBean1 subBean1 = new SubBean1();
    subBean1.setSubBean2(subBean2);

    NestedPropertiesBean dst = new NestedPropertiesBean();
    dst.setBeanProp(srcProp2);   // flat setter from the same nested source path
    dst.setSubBean1(subBean1);   // nested setter chain

    return dst;
}
```

Semantics

Null-safety along the path: each nested(getter) link is guarded; if an intermediate is null, the whole right-hand side becomes null without throwing NPEs.

Common sub-expression lifting: repeated source prefixes (e.g., src.getSubBean1().getSubBean2()) are cached in a temp variable and reused across mappings.

Destination construction: for POJOs, nested destination beans are constructed once (in dependency order) and wired via the nested setter chain expressed with bean(...).nested(...).nested(...).

Records/immutables: when the destination (or any nested part) is a record/immutable, the generator collects constructor args bottom-up and builds the object in a single constructor call.

Intermixing with converters/constants/copy: you can still use .converting(...), value(...), or .copy() at the leaf; null-safety and temp reuse apply the same way.

---

## Custom Interfaces (implement your own SAM)

Graft can generate a mapper that **implements your own interface** — as long as it’s a _functional interface_ (exactly one non-default instance method). Declare it on the spec with `targetSuperType`:

```java
@MappingSpec(
    value = com.detornium.graft.mappers.CustomMappingInterfaceMapper.class,
    targetSuperType = com.detornium.graft.interfaces.CustomMapperInterface.class
)
class CustomMappingInterfaceSpec extends MappingDsl<SimpleModel, SimpleDto> {}
```

The processor binds the interface’s single abstract method (SAM) to your `S` (source) and `D` (target), then generates an implementation that wires your mappings.
### What qualifies as a custom interface?

- **Interface** with **one** non-default, non-static instance method.
- That method must **accept the source type** (or its type variable) and **return the target type** (or a supertype / its type variable).
- Generics are supported — the processor binds `S`/`T` to your spec’s `<S, D>`.

#### 1) Generic interface `<S,T>` → `T convert(S src)`

**Interface**

```java
public interface CustomMapperInterface<S, T> {
    T convert(S source);
}
```

**Spec (uses the interface as a supertype)**

```java
@MappingSpec(
  value = com.detornium.graft.mappers.CustomMappingInterfaceMapper.class,
  targetSuperType = com.detornium.graft.interfaces.CustomMapperInterface.class)
class CustomMappingInterfaceSpec extends MappingDsl<SimpleModel, SimpleDto> {}
```

**Generated**

```java
public final class CustomMappingInterfaceMapper
        implements CustomMapperInterface<SimpleModel, SimpleDto> {

    @Override
    public final SimpleDto convert(SimpleModel src) {
        if (src == null) return null;
        SimpleDto dst = new SimpleDto();
        dst.setStringField(src.getStringField());
        return dst;
    }
}
```

> The generated class implements your SAM `convert(S)->T` with `S=SimpleModel`, `T=SimpleDto`.

#### 2) Generic interface with **swapped type parameters** `<T,S>` → `T convert(S src)`

**Interface**

```java
public interface CustomMappingInterfaceSwappedParams<T, S> {
    T convert(S source);
}
```

**Spec**

```java
@MappingSpec(
  value = com.detornium.graft.mappers.CustomMappingInterfaceSwappedParamsMapper.class,
  targetSuperType = com.detornium.graft.interfaces.CustomMappingInterfaceSwappedParams.class)
class CustomMappingInterfaceSwappedParamsSpec extends MappingDsl<SimpleModel, SimpleDto> {}
```

**Generated**

```java
public final class CustomMappingInterfaceSwappedParamsMapper
        implements CustomMappingInterfaceSwappedParams<SimpleDto, SimpleModel> {

    @Override
    public final SimpleDto convert(SimpleModel src) {
        if (src == null) return null;
        SimpleDto dst = new SimpleDto();
        dst.setStringField(src.getStringField());
        return dst;
    }
}
```

>Type variables are bound correctly even if their order differs (`<T,S>`).

#### 3) **Non-generic** interface (fixed types)

**Interface**

```java
public interface SpecificTypesMapperInterface {
    SimpleDto convert(SimpleModel source);
}
```

**Spec**

```java
@MappingSpec(
  value = com.detornium.graft.mappers.SpecificTypesMapperInterfaceMapper.class,
  targetSuperType = com.detornium.graft.interfaces.SpecificTypesMapperInterface.class)
class SpecificTypesMapperInterfaceSpec extends MappingDsl<SimpleModel, SimpleDto> {}
```

**Generated**

```java
public final class SpecificTypesMapperInterfaceMapper
        implements SpecificTypesMapperInterface {

    @Override
    public final SimpleDto convert(SimpleModel src) {
        if (src == null) return null;
        SimpleDto dst = new SimpleDto();
        dst.setStringField(src.getStringField());
        return dst;
    }
}
```

> Fixed-type contracts work the same: Graft implements your `convert` method and fills it with the mapping logic.

#### 4) **Common generic type parameter** (same source & target)

Sometimes your interface uses a **single generic type parameter** for both the parameter and the return type.

**Interface**

```java
public interface CommonGenericTypeInterface<T> {
    T copy(T item);
}
```

**Spec**

```java
@MappingSpec(
  value = com.detornium.graft.mappers.CommonGenericTypeInterfaceMapper.class,
  targetSuperType = com.detornium.graft.interfaces.CommonGenericTypeInterface.class
)
class CommonGenericTypeInterfaceSpec extends MappingDsl<SimpleModel, SimpleModel> {}
```

**Generated**

```java
public final class CommonGenericTypeInterfaceMapper
        implements CommonGenericTypeInterface<SimpleModel> {

    @Override
    public final SimpleModel copy(SimpleModel src) {
        if (src == null) return null;
        SimpleModel dst = new SimpleModel();
        dst.setStringField(src.getStringField());
        return dst;
    }
}
```

> **Why this works:** the processor binds the single type variable `T` to `SimpleModel` in both positions, so the SAM becomes `SimpleModel copy(SimpleModel)`.

---

## How it works

1. You declare mappings in a spec class (extends `MappingDsl<S, D>`):
    - `map(getter).to(setter)`
    - `map(getter).converting(fn).to(setter)`
    - `map(getter).copy().to(setter)`
    - `exclude(setter)`
    - `self().converting(fn).to(setter)`
    - `value(constant).to(setter)`
2. The **annotation processor** parses the call chains and generates a concrete mapper.

> Records are supported via component getters; for immutable targets, values are set via constructor/builder as
> applicable.

---

## Annotations

### `@MappingSpec`

Marks a mapping **spec** class and declares the fully-qualified name of the **generated mapper**.

```java
@MappingSpec(com.detornium.graft.mappers.CarToCarDtoMapper.class)
public class CarToCarDtoMapperSpec extends MappingDsl<Car, CarDto> { /* ... */ }
```

### `@IgnoreUnmapped`

Suppresses errors/warnings about target properties that are **not explicitly mapped**.

```java
@IgnoreUnmapped
@MappingSpec(com.detornium.graft.mappers.IgnoreUnmappedMapper.class)
public class IgnoreUnmappedSpec extends MappingDsl<Car, CarDto> {
    {
        map(Car::getModel).to(CarDto::setCarModel);
        // Other CarDto properties will be ignored instead of reported.
    }
}
```

### `@DisableAutoMapping`

Disables **automatic by-name** mapping for properties. When present, only properties you map explicitly in the DSL (e.g., `map(...).to(...)`) will be considered; same-name properties won’t be auto-mapped. Typically combined with `@IgnoreUnmapped` if you want a strict, **whitelist-only** spec.

```java
@DisableAutoMapping
@IgnoreUnmapped
@MappingSpec(com.detornium.graft.mappers.DisableAutoMappingMapper.class)
public class DisableAutoMappingSpec extends MappingDsl<Car, Car> {
    {
        map(Car::getVersion).to(Car::setVersion);
    }
}

```

---

## Lombok Binding (SPI)

If you use **Lombok** (e.g., `@Getter`, `@Setter`, `@Builder`), add the optional **Graft Lombok Binding**.  
It integrates via a small **SPI** so Graft can detect when Lombok has finished AST changes and correctly discover generated getters/setters/builders during code generation.

### Why you might need it

- Without the binding, Graft may analyze a type **before** Lombok has injected members, leading to “getter/setter not found” diagnostics.

- With the binding, Graft defers until the type is **complete**, then generates code.


### Maven (simple setup)

Add the following dependencies (compile your API; put processors/bindings on the compile classpath):

```xml
<dependency>
  <groupId>com.detornium.graft</groupId>
  <artifactId>core</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
<dependency>
  <groupId>com.detornium.graft</groupId>
  <artifactId>processor</artifactId>
  <version>1.0-SNAPSHOT</version>
  <scope>provided</scope>
</dependency>
<dependency>
  <groupId>com.detornium.graft</groupId>
  <artifactId>graft-lombok-binding</artifactId>
  <version>1.0-SNAPSHOT</version>
  <scope>provided</scope>
</dependency>
<dependency>
  <groupId>org.projectlombok</groupId>
  <artifactId>lombok</artifactId>
  <version>1.18.30</version>
  <scope>provided</scope>
</dependency>
```

---

## Roadmap (PoC)

- [x] Lombok binding SPI
- [ ] Better diagnostics & source ranges
- [ ] Lambda lifting for `converting(...)`
- [ ] Collection/array mapping options
- [x] Clone support
- [x] Nested mapping support

---

## License

Licensed under the **Apache License, Version 2.0**.

Copyright © 2025 Taras Semaniv

