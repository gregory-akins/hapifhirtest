# HAPI FHIR QI-Core Validation Test Project

A Java-based project for validating FHIR R4 resources against QI-Core profiles using HAPI FHIR library with custom validation support.

## Overview

This project demonstrates how to:
- Parse FHIR JSON resources from files
- Validate FHIR resources against QI-Core implementation guides
- Load and use terminology resources (ValueSets) from packaged ZIP files
- Implement custom validation logic with display name validation
- Build a validation support chain with in-memory terminology services

## Project Structure

```
hapifhirtest/
├── pom.xml                                 # Maven project configuration
├── src/
│   ├── main/
│   │   ├── java/org/insomnia/
│   │   │   ├── App.java                   # Main application entry point
│   │   │   └── CustomQiCoreInMemoryValidationSupport.java  # Custom validation logic
│   │   └── resources/
│   │       ├── bundle.json                # Sample FHIR bundle for testing
│   │       └── packages/                  # FHIR implementation guide packages
│   │           ├── hl7.fhir.r5.core.tgz
│   │           ├── hl7.fhir.us.core-3.1.0.tgz
│   │           ├── hl7.fhir.us.core-6.1.0.tgz
│   │           ├── hl7.fhir.us.qicore-4.1.1.tgz
│   │           ├── hl7.fhir.us.qicore-6.0.0.tgz
│   │           ├── hl7.fhir.uv.extensions.r4-5.2.0.tgz
│   │           ├── hl7.fhir.xver-extensions-0.0.13.tgz
│   │           ├── hl7.fhir.xver-extensions-0.1.0.tgz
│   │           └── tx-qicore-6.0.0.zip    # QI-Core terminology resources
│   └── test/
│       └── java/org/insomnia/
│           └── AppTest.java               # Unit tests
└── target/                                # Build output directory
```

## Requirements

- **Java**: 17 or higher
- **Maven**: 3.6 or higher
- **HAPI FHIR**: Version 8.4.0

## Dependencies

### Core FHIR Libraries
- `hapi-fhir-structures-r4` (8.4.0) - FHIR R4 data models
- `hapi-fhir-validation` (8.4.0) - FHIR validation framework
- `hapi-fhir-validation-resources-r4` (8.4.0) - Base FHIR R4 validation resources
- `hapi-fhir-caching-caffeine` (8.4.0) - In-memory caching support

### Utility Libraries
- `commons-collections4` (4.5.0) - Enhanced collections utilities
- `lombok` (1.18.30) - Code generation for boilerplate reduction
- `slf4j-simple` (2.0.9) - Simple logging implementation

### Testing
- `junit-jupiter` (5.11.0) - JUnit 5 testing framework

## Installation

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd hapifhirtest
   ```

2. **Build the project**:
   ```bash
   mvn clean install
   ```

3. **Run the application**:
   ```bash
   mvn exec:java -Dexec.mainClass="org.insomnia.App"
   ```

## Key Components

### 1. App.java

The main application class that orchestrates FHIR resource validation.

**Key Features**:
- Initializes FHIR R4 context
- Loads terminology resources from ZIP files
- Creates validation support chain with custom validation logic
- Parses FHIR JSON resources from classpath
- Executes validation and reports results

**Main Workflow**:
```java
1. Create FhirContext for R4
2. Load terminology resources (ValueSets) from tx-qicore-6.0.0.zip
3. Configure validation with custom display validation
4. Build validation support chain
5. Parse bundle.json resource
6. Validate resource and output results
```

### 2. CustomQiCoreInMemoryValidationSupport.java

A custom validation support class extending `InMemoryTerminologyServerValidationSupport` with specialized logic for QI-Core validation.

**Features**:
- **Display Validation**: Configurable validation of display names against terminology
- **ValueSet Expansion**: Validates codes against expanded ValueSets
- **Code System Matching**: Ensures codes belong to the correct code system
- **Detailed Error Messages**: Provides specific error messages for validation failures

**Validation Logic**:
- Checks if codes exist in ValueSet expansions
- Validates code system URLs and versions
- Optionally validates display names match expected values
- Returns appropriate error codes and severities

**Key Methods**:
```java
- validateCodeInValueSet()        // Validates code against ValueSet expansion
- validateCode()                  // Validates code without ValueSet context
- validateMatchedCodeAndCodeSystem()  // Validates matched code and checks display
- createMissingCodeValidationResult()  // Error for missing codes
- createNotInVsValidationResult()     // Error for codes not in ValueSet
```

### 3. ValidationConfig.java

A simple configuration class for controlling validation behavior.

**Configuration Options**:
- `validateDisplay`: Boolean flag to enable/disable display name validation

### 4. buildPrePopulatedValidationSupportFromZip()

A utility method that loads FHIR terminology resources from ZIP files.

**Process**:
1. Opens ZIP file from classpath
2. Iterates through entries (excluding system files like .DS_Store)
3. Parses XML content to FHIR resources
4. Filters ValueSet resources
5. Adds ValueSets to pre-populated validation support

**Supported Format**: XML-based FHIR resources within ZIP archives

## Usage Examples

### Basic Resource Validation

```java
// Initialize FHIR context
FhirContext fhirContext = FhirContext.forR4();

// Create validation configuration
ValidationConfig config = new ValidationConfig();
config.setValidateDisplay(true);

// Build validation support chain
ValidationSupportChain validationSupportChain = new ValidationSupportChain(
    new CustomQiCoreInMemoryValidationSupport(fhirContext, config)
);

// Create validator
FhirInstanceValidator validatorModule = new FhirInstanceValidator(validationSupportChain);
FhirValidator validator = fhirContext.newValidator().registerValidatorModule(validatorModule);

// Parse and validate resource
try (InputStream jsonInputStream = ClasspathUtil.loadResourceAsStream("bundle.json")) {
    IBaseResource resource = fhirContext.newJsonParser().parseResource(jsonInputStream);
    ValidationResult result = validator.validateWithResult(resource);
    
    // Check validation results
    if (result.isSuccessful()) {
        System.out.println("Validation successful!");
    } else {
        result.getMessages().forEach(msg -> 
            System.out.println(msg.getSeverity() + ": " + msg.getMessage())
        );
    }
}
```

### Loading Custom Terminology

```java
PrePopulatedValidationSupport prePopulatedSupport = 
    buildPrePopulatedValidationSupportFromZip(
        fhirContext, 
        "classpath:packages/tx-qicore-6.0.0.zip"
    );

// Add to validation support chain
ValidationSupportChain chain = new ValidationSupportChain(
    prePopulatedSupport,
    new DefaultProfileValidationSupport(fhirContext),
    new CustomQiCoreInMemoryValidationSupport(fhirContext, config)
);
```

### Reading JSON from File to InputStream

```java
// From classpath
try (InputStream is = ClasspathUtil.loadResourceAsStream("bundle.json")) {
    IBaseResource resource = fhirContext.newJsonParser().parseResource(is);
}

// From file system
try (InputStream is = new FileInputStream("/path/to/resource.json")) {
    IBaseResource resource = fhirContext.newJsonParser().parseResource(is);
}
```

## FHIR Packages

The project includes several FHIR implementation guide packages:

- **hl7.fhir.r5.core.tgz**: FHIR R5 core specifications
- **hl7.fhir.us.core-3.1.0/6.1.0.tgz**: US Core Implementation Guide
- **hl7.fhir.us.qicore-4.1.1/6.0.0.tgz**: Quality Improvement Core (QI-Core) profiles
- **hl7.fhir.uv.extensions.r4-5.2.0.tgz**: Universal extensions for R4
- **hl7.fhir.xver-extensions**: Cross-version extensions
- **tx-qicore-6.0.0.zip**: QI-Core terminology resources (ValueSets)

## Configuration

### Validation Options

```java
ValidationConfig config = new ValidationConfig();
config.setValidateDisplay(true);  // Enable display name validation
```

### Logging

The project uses SLF4J with a simple logger. Logging levels can be configured via system properties:

```bash
-Dorg.slf4j.simpleLogger.defaultLogLevel=debug
```

## Testing

Run tests using Maven:

```bash
mvn test
```

## Common Issues and Solutions

### Issue: FileNotFoundException when loading resources

**Solution**: Ensure resources are in the `src/main/resources` directory and use `ClasspathUtil.loadResourceAsStream()` with the correct relative path.

### Issue: Validation fails with "Unknown code system"

**Solution**: Ensure the terminology ZIP file is loaded and contains the required ValueSets. Check that the code system URL matches exactly.

### Issue: Display name validation errors

**Solution**: Either fix the display names to match terminology, or disable display validation:
```java
config.setValidateDisplay(false);
```

## Build and Package

### Compile

```bash
mvn compile
```

### Package as JAR

```bash
mvn package
```

The JAR file will be created in the `target/` directory.

### Clean Build

```bash
mvn clean install
```

## Architecture

### Validation Flow

```
1. Load FHIR Resource (JSON/XML)
           ↓
2. Parse with HAPI FHIR Parser
           ↓
3. ValidationSupportChain
    ├── CustomQiCoreInMemoryValidationSupport
    │   ├── Check code in ValueSet expansion
    │   ├── Validate code system
    │   └── Validate display (optional)
    └── DefaultProfileValidationSupport
           ↓
4. FhirInstanceValidator
           ↓
5. ValidationResult
    ├── isSuccessful()
    └── getMessages()
```

## Contributing

When contributing to this project:

1. Follow Java coding conventions
2. Add appropriate Lombok annotations to reduce boilerplate
3. Include Javadoc for public methods
4. Write unit tests for new functionality
5. Ensure Maven build succeeds before submitting

## License

[Specify your license here]

## Contact

[Specify contact information here]

## References

- [HAPI FHIR Documentation](https://hapifhir.io/)
- [FHIR R4 Specification](https://hl7.org/fhir/R4/)
- [QI-Core Implementation Guide](http://hl7.org/fhir/us/qicore/)
- [US Core Implementation Guide](http://hl7.org/fhir/us/core/)

## Version History

- **1.0-SNAPSHOT**: Initial version with QI-Core 6.0.0 validation support

