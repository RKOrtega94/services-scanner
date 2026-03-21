# Services Scanner library

This library is used to scan controllers `@Controller` or/and `@RestController` java Spring classes.

## Usage

To use this library, you can include it in your project and configure it to scan your Spring controllers.

### Cloning the repository

1. Clone the repository:

```bash
git clone https://github.com/RKOrtega94/services-scanner.git
```

2. Add the dependency to your project:

- `pom.xml`:

```xml
<dependency>
    <groupId>com.rkorte.services-scanner</groupId>
    <artifactId>services-scanner</artifactId>
    <version>1.0.0</version>
</dependency>
```
- `build.gradle`:
```groovy
dependencies {
    implementation project(':services-scanner') // Here you can change as you want
}
```

### Adding the dependency from a Maven repository

1. Add the dependency:

- `pom.xml`:

```xml
<dependency>
    <groupId>com.rkorte.services-scanner</groupId>
    <artifactId>services-scanner</artifactId>
    <version>1.0.0</version>
</dependency>
```
- `build.gradle`:

```groovy
dependencies {
    implementation 'com.rkorte.services-scanner:services-scanner:1.0.0'
}
```

