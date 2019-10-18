package life.qbic.portal.parsing;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Validates vocabulary values of parsed designs
 * 
 * @author Andreas Friedrich
 *
 */
public class VocabularyValidator {

  private final Logger logger = LogManager.getLogger(VocabularyValidator.class);

  private Map<String, Set<String>> vocabularies;
  private String error = "";

  public VocabularyValidator(Map<String, Set<String>> vocabularies) {
    this.vocabularies = vocabularies;
  }

  public String getError() {
    return error;
  }

  public boolean validateExperimentMetadata(List<Map<String, String>> metadataList) {
    for (Map<String, String> experimentProperties : metadataList) {
      for (String propertyName : experimentProperties.keySet()) {
        if (vocabularies.containsKey(propertyName)) {
          Set<String> vocabulary = vocabularies.get(propertyName);
          String property = (String) experimentProperties.get(propertyName);
          if (!vocabulary.contains(property.toUpperCase())) {
            logger.debug(property.toUpperCase());
            error = "Property " + property + " is not a valid value for " + propertyName;
            error+="\nValid values: "+vocabulary;
            return false;
          }
        }
      }
    }
    return true;
  }

}
