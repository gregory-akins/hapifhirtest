package org.insomnia;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ValueSet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.util.ClasspathUtil;

/**
 * Hello world!
 */
public class App {
	
	static FhirContext fhirContext = FhirContext.forR4();
    
	public static void main(String[] args) throws IOException {
        System.out.println("Starting!");
        ValidationConfig validationConfig = new ValidationConfig();
        validationConfig.setValidateDisplay(true);
        PrePopulatedValidationSupport prePopulatedValidationSupport =
                buildPrePopulatedValidationSupportFromZip(
                    fhirContext, "classpath:packages/tx-qicore-6.0.0.zip");
        DefaultProfileValidationSupport defaultSupport = new DefaultProfileValidationSupport(fhirContext);
        ValidationSupportChain validationSupportChain = new ValidationSupportChain(
                prePopulatedValidationSupport,
                defaultSupport,
                new CustomQiCoreInMemoryValidationSupport(fhirContext, validationConfig));
        FhirInstanceValidator validatorModule = new FhirInstanceValidator(validationSupportChain);
        FhirValidator validator = fhirContext.newValidator().registerValidatorModule(validatorModule);
        try (InputStream jsonInputStream = ClasspathUtil.loadResourceAsStream("bundle.json")) {
            IBaseResource resource = fhirContext.newJsonParser().parseResource(jsonInputStream);
            ValidationResult result = validator.validateWithResult(resource);
            if (resource != null) {
                System.out.println("Resource parsed successfully");
            }

            // Display validation results
            System.out.println("\n=== Validation Results ===");
            if (result.isSuccessful()) {
                System.out.println("✓ Validation SUCCESSFUL - No errors found");
            } else {
                System.out.println("✗ Validation FAILED");
            }

            // Show all validation messages (errors, warnings, information)
            if (!result.getMessages().isEmpty()) {
                System.out.println("\nValidation Messages (" + result.getMessages().size() + " total):");
                System.out.println("─────────────────────────────────────────────────────────────");
                result.getMessages().forEach(msg -> {
                    String severity = msg.getSeverity() != null ? msg.getSeverity().name() : "UNKNOWN";
                    String location = msg.getLocationString() != null ? msg.getLocationString() : "N/A";
                    String message = msg.getMessage() != null ? msg.getMessage() : "";

                    System.out.println("\n[" + severity + "]");
                    System.out.println("Location: " + location);
                    System.out.println("Message: " + message);
                });
                System.out.println("\n─────────────────────────────────────────────────────────────");
            } else {
                System.out.println("\nNo validation messages to display.");
            }
        }
        System.out.println("Done!");
    }
	static public PrePopulatedValidationSupport buildPrePopulatedValidationSupportFromZip(
		      FhirContext qicore6FhirContext, String zipFileName) throws IOException {
		    PrePopulatedValidationSupport prePopulatedValidationSupport =
		        new PrePopulatedValidationSupport(qicore6FhirContext);
		    IParser xmlParser = qicore6FhirContext.newXmlParser();

		    try (InputStream is = ClasspathUtil.loadResourceAsStream(zipFileName);
		        ZipInputStream zipInputStream = new ZipInputStream(is)) {

		      if (is == null) {
		        throw new IllegalArgumentException(
		            "ZIP file not found in resources/packages: " + zipFileName);
		      }

		      ZipEntry entry;
		      while ((entry = zipInputStream.getNextEntry()) != null) {
		        if (!entry.isDirectory()
		            && !entry.getName().startsWith("__MACOSX")
		            && !entry.getName().endsWith(".DS_Store")) {

		          StringBuilder fileContent = new StringBuilder();
		          byte[] buffer = new byte[1024];
		          int read;
		          while ((read = zipInputStream.read(buffer)) != -1) {
		            fileContent.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
		          }

		          String xmlContent = fileContent.toString();
		          if (xmlContent.startsWith("\uFEFF")) {
		            xmlContent = xmlContent.substring(1);
		          }
		          IBaseResource baseResource = xmlParser.parseResource(xmlContent);
		          if (baseResource instanceof ValueSet) {
		            prePopulatedValidationSupport.addValueSet(baseResource);
		          }
		        }
		        zipInputStream.closeEntry();
		      }
		    }

		    return prePopulatedValidationSupport;
		  }
}

class ValidationConfig {	  
	  private boolean validateDisplay;

		public void setValidateDisplay(boolean validateDisplay) {
			this.validateDisplay = validateDisplay;
		}

		public boolean getValidateDisplay() {
			return validateDisplay;
		}
}


