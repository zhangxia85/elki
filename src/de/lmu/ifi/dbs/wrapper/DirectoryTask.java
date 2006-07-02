package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper to run another wrapper for all files in the directory given as input.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class DirectoryTask extends StandAloneInputWrapper {
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
   * Label for parameter wrapper.
   */
  public static final String WRAPPER_P = "wrapper";

  /**
   * Description for parameter wrapper.
   */
  public static final String WRAPPER_D = "<class>wrapper to run over all files in a specified directory " +
                                         Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Wrapper.class) +
                                         ".";

  /**
   * Wrapper to run over all files.
   */
  private Wrapper wrapper;

  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    DirectoryTask wrapper = new DirectoryTask();
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
      wrapper.logger.log(Level.SEVERE, wrapper.optionHandler.usage(e.getMessage()), e);
    }
  }

  public DirectoryTask() {
    parameterToDescription.put(WRAPPER_P + OptionHandler.EXPECTS_VALUE, WRAPPER_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * Runs the wrapper.
   */
  public void run() throws UnableToComplyException {
    File inputDir = new File(getInput());
    if (!inputDir.isDirectory()) {
      throw new IllegalArgumentException(getInput() + " is not a directory");
    }
    File[] inputFiles = inputDir.listFiles();
    for (File inputFile : inputFiles) {
      try {
        List<String> wrapperParameters = getRemainingParameters();
        wrapperParameters.add(OptionHandler.OPTION_PREFIX + INPUT_P);
        wrapperParameters.add(inputFile.getAbsolutePath());
        wrapperParameters.add(OptionHandler.OPTION_PREFIX + OUTPUT_P);
        wrapperParameters.add(getOutput() + File.separator + inputFile.getName());
        wrapper.setParameters(wrapperParameters.toArray(new String[wrapperParameters.size()]));
        wrapper.run();
      }
      catch (ParameterException e) {
        throw new UnableToComplyException(e.getMessage(), e);
      }
    }
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    // wrapper
    try {
      wrapper = Util.instantiate(Wrapper.class, optionHandler.getOptionValue(WRAPPER_P));
    }
    catch (UnableToComplyException e) {
      throw new WrongParameterValueException(WRAPPER_P, optionHandler.getOptionValue(WRAPPER_P), WRAPPER_D);
    }

    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = super.getAttributeSettings();
    AttributeSettings mySettings = settings.get(0);
    mySettings.addSetting(WRAPPER_P, wrapper.getClass().getName());
    return settings;
  }

}
