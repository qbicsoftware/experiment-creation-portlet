package life.qbic.portal.parsing;

import java.util.HashSet;
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

  public boolean transformAndValidateExperimentMetadata(List<Map<String, Object>> metadataList,
      Map<String, Set<String>> pretransformedProperties) {

    for (Map<String, Object> experimentProperties : metadataList) {
      Set<String> props = new HashSet<>();
      props.addAll(experimentProperties.keySet());
      for (String propertyName : props) {
        if (pretransformedProperties.containsKey(propertyName)) {
          boolean success = transformProperty(propertyName, experimentProperties,
              pretransformedProperties.get(propertyName));
          if (!success) {
            return false;
          }
          experimentProperties.remove(propertyName);
        }
      }
    }

    for (Map<String, Object> experimentProperties : metadataList) {
      Set<String> props = new HashSet<>();
      props.addAll(experimentProperties.keySet());
      for (String propertyName : props) {
        if (vocabularies.containsKey(propertyName)) {
          Set<String> set = vocabularies.get(propertyName);
          Object valueObject = experimentProperties.get(propertyName);
          if (valueObject instanceof List) {
            List<String> values = (List<String>) valueObject;
            for (String val : values) {
              if (!set.contains(val.toUpperCase()) && !set.contains(val)) {
                logger.debug(val.toUpperCase());
                setErrorMessage(val, propertyName, set);
                return false;
              }
            }
            String newVal = String.join(", ", values);
            experimentProperties.put(propertyName, newVal);
          } else {
            String value = valueObject.toString();
            if (!set.contains(value.toUpperCase()) && !set.contains(value)) {
              logger.debug(value.toUpperCase());
              setErrorMessage(value, propertyName, set);
              return false;
            }
          }
        }
      }
    }
    return true;
  }

  private boolean transformProperty(String propertyName, Map<String, Object> experimentProperties,
      Set<String> possibleProps) {
    Object valueObject = experimentProperties.get(propertyName);
    if (valueObject instanceof String) {
      String value = (String) valueObject;
      for (String truePropertyName : possibleProps) {
        // String vocabName = transformationMapping.get(truePropertyName);
        // System.out.println(truePropertyName);
        // System.out.println(transformationMapping);
        // System.out.println("---");
        // System.out.println(vocabularies.keySet());
        // System.out.println(vocabName);
        String vocabName = truePropertyName;
        Set<String> set = vocabularies.get(vocabName);
        if (set.contains(value.toUpperCase()) || set.contains(value)) {
          experimentProperties.put(truePropertyName, value);
          return true;
        }
      }
      error = "Property " + value + " is not a valid value for either of these categories: "
          + possibleProps;
      return false;
    } else {
      logger.error("value for " + propertyName + " not a string. was: " + valueObject);
      return false;
    }
  }

  private void setErrorMessage(String property, String propertyName, Set<String> vocabulary) {
    error = "Property " + property + " is not a valid value for " + propertyName;
    error += "\nValid values: " + vocabulary;
  }



}
