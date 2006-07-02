package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.outlier.LOF;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper class for LOF algorithm. Performs an attribute wise normalization
 * on the database objects.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class LOFWrapper extends FileBasedDatabaseConnectionWrapper {
  /**
   * Holds the class specific debug status.
   */
  @SuppressWarnings({"unused", "UNUSED_SYMBOL"})
  private static final boolean DEBUG = LoggingConfiguration.DEBUG;
//  private static final boolean DEBUG = true;

  /**
   * The logger of this class.
   */
  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * The value of the minpts parameter.
   */
  private String minpts;

  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    LOFWrapper wrapper = new LOFWrapper();
    try {
      wrapper.setParameters(args);
      wrapper.run();
    }
    catch (ParameterException e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      wrapper.logger.log(Level.SEVERE, wrapper.optionHandler.usage(e.getMessage()), cause);
    }
    catch (AbortException e) {
      wrapper.logger.info(e.getMessage());
    }
    catch (Exception e) {
      e.printStackTrace();
      wrapper.logger.log(Level.SEVERE, wrapper.optionHandler.usage(e.getMessage()), e);
    }
  }

  /**
   * Sets the parameter minpts in the parameter map additionally
   * to the parameters provided by super-classes.
   */
  public LOFWrapper() {
    super();
    parameterToDescription.put(LOF.MINPTS_P + OptionHandler.EXPECTS_VALUE, LOF.MINPTS_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see de.lmu.ifi.dbs.wrapper.KDDTaskWrapper#getKDDTaskParameters()
   */
  public List<String> getKDDTaskParameters() {
    List<String> parameters = super.getKDDTaskParameters();

    // algorithm LOF
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(LOF.class.getName());

    // minpts
    parameters.add(OptionHandler.OPTION_PREFIX + LOF.MINPTS_P);
    parameters.add(minpts);

    // distance function
    parameters.add(OptionHandler.OPTION_PREFIX + LOF.DISTANCE_FUNCTION_P);
    parameters.add(EuklideanDistanceFunction.class.getName());

    // normalization
//    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
//    parameters.add(AttributeWiseRealVectorNormalization.class.getName());
//    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

    // page size
//    parameters.add(OptionHandler.OPTION_PREFIX + LOF.PAGE_SIZE_P);
//    parameters.add("8000");

    // cache size
//    parameters.add(OptionHandler.OPTION_PREFIX + LOF.CACHE_SIZE_P);
//    parameters.add("" + 8000 * 10);

    return parameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    // minpts
    minpts = optionHandler.getOptionValue(LOF.MINPTS_P);
    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = super.getAttributeSettings();
    AttributeSettings mySettings = settings.get(0);
    mySettings.addSetting(LOF.MINPTS_P, minpts);
    return settings;
  }

}


