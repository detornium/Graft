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
- [ ] Nested mapping support

---

## License

Licensed under the **Apache License, Version 2.0**.

Copyright © 2025 Taras Semaniv

