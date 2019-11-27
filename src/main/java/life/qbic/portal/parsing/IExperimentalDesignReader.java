package life.qbic.portal.parsing;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.portal.model.PreliminaryOpenbisExperiment;
import life.qbic.xml.study.TechnologyType;

public interface IExperimentalDesignReader {

  List<ISampleBean> readSamples(File file, boolean parseGraph) throws IOException, JAXBException;

  String getError();

  Map<ExperimentType, List<PreliminaryOpenbisExperiment>> getExperimentInfos();

//  Set<String> getSpeciesSet();
//
//  Set<String> getTissueSet();

  Set<String> getAnalyteSet();

  List<String> getTSVByRows();

//  StructuredExperiment getGraphStructure();

  int countEntities(File file) throws IOException;

  List<TechnologyType> getTechnologyTypes();

  Map<String, List<String>> getParsedCategoriesToValues(List<String> header);

}
