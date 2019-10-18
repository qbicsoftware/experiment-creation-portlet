package life.qbic.portal.parsing;

import java.util.Arrays;
import java.util.List;

public enum ExperimentalDesignType {

  TopDown("a4b topdown format",
      "Protein identification or quantitation using intact proteins and mass spectrometry.",
      Arrays.asList("File Name", "MS Device", "Sample Preparation Date", "MS Run Date",
          "Protein Barcode"),
      Arrays.asList("Chromatography Type", "Enrichment/Fractionation Type", "Fraction Name",
          "Labeling Type", "Label", "LCMS Method", "Comment"),
      new TopDownDesignReader()), BottomUp("a4b bottomup format",
          "Protein identification by proteolytic digestion of proteins prior to analysis by mass spectrometry.",
          Arrays.asList("File Name", "Digestion", "MS Device", "Sample Preparation Date",
              "MS Run Date", "Protein Barcode"),
          Arrays.asList("Chromatography Type", "Enrichment/Fractionation Type", "Fraction Name",
              "Labeling Type", "Label", "LCMS Method", "Comment"),
          new BottomUpDesignReader()), Deglycosylation("a4b deglycosylation ms format",
              "Glycopeptide or glycan identification by digestion of proteins or release of glycans prior to analysis by mass spectrometry.",
              Arrays.asList("File Name", "Digestion", "MS Device", "Sample Preparation Date",
                  "MS Run Date", "Protein Barcode"),
              Arrays.asList("Chromatography Type", "Enrichment/Fractionation Type", "Fraction Name",
                  "Labeling Type", "Label", "LCMS Method", "Comment"),
              new DeclygDesignReader());

  private final String name;
  private final String description;
  private final List<String> required;
  private final List<String> optional;
  private final IExperimentalDesignReader parser;

  private ExperimentalDesignType(String name, String description, List<String> required,
      List<String> optional, IExperimentalDesignReader parser) {
    this.name = name;
    this.description = description;
    this.required = required;
    this.optional = optional;
    this.parser = parser;
  }

  public String getName() {
    return name;
  }

  public String getFileName() {
    return name.replace(" ", "_") + ".tsv";
  }

  public String getDescription() {
    return description;
  }

  public List<String> getRequired() {
    return required;
  }

  public IExperimentalDesignReader getParser() {
    return parser;
  }

  public List<String> getOptional() {
    return optional;
  }

}
