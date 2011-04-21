package de.lmu.ifi.dbs.elki.datasource.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.StringLengthConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter;

/**
 * Abstract superclass for all parsers providing the option handler for handling
 * options.
 * 
 * @author Arthur Zimek
 */
public abstract class AbstractParser {
  /**
   * A pattern defining whitespace.
   */
  public static final String WHITESPACE_PATTERN = "\\s+";

  /**
   * A quote pattern
   */
  public static final String QUOTE_CHAR = "\"";

  /**
   * A pattern catching most numbers that can be parsed using Double.valueOf:
   * 
   * Some examples: <code>1</code> <code>1.</code> <code>1.2</code>
   * <code>.2</code> <code>-.2e-03</code>
   */
  public static final String NUMBER_PATTERN = "[+-]?(?:\\d+\\.?|\\d*\\.\\d+)?(?:[eE][-]?\\d+)?";

  /**
   * OptionID for the column separator parameter (defaults to whitespace as in
   * {@link #WHITESPACE_PATTERN}.
   */
  public static final OptionID COLUMN_SEPARATOR_ID = OptionID.getOrCreateOptionID("parser.colsep", "Column separator pattern. The default assumes whitespace separated data.");

  /**
   * OptionID for the quote character parameter (defaults to a double quotation
   * mark as in {@link #QUOTE_CHAR}.
   */
  public static final OptionID QUOTE_ID = OptionID.getOrCreateOptionID("parser.quote", "Quotation character. The default is to use a double quote.");

  /**
   * Stores the column separator pattern
   */
  private Pattern colSep = null;

  /**
   * Stores the quotation character
   */
  protected char quoteChar = QUOTE_CHAR.charAt(0);

  /**
   * The comment character.
   */
  public static final String COMMENT = "#";

  /**
   * A sign to separate attributes.
   */
  public static final String ATTRIBUTE_CONCATENATION = " ";

  /**
   * Constructor.
   * 
   * @param colSep Column separator
   * @param quoteChar Quote character
   */
  public AbstractParser(Pattern colSep, char quoteChar) {
    super();
    this.colSep = colSep;
    this.quoteChar = quoteChar;
  }

  /**
   * Tokenize a string. Works much like colSep.split() except it honors
   * quotation characters.
   * 
   * @param input Input string
   * @return Tokenized string
   */
  protected List<String> tokenize(String input) {
    ArrayList<String> matchList = new ArrayList<String>();
    Matcher m = colSep.matcher(input);

    int index = 0;
    boolean inquote = (input.length() > 0) && (input.charAt(0) == quoteChar);
    while(m.find()) {
      // Quoted code path vs. regular code path
      if(inquote && m.start() > 0) {
        // Closing quote found?
        if(m.start() > index + 1 && input.charAt(m.start() - 1) == quoteChar) {
          // Strip quote characters
          matchList.add(input.subSequence(index + 1, m.start() - 1).toString());
          // Seek past
          index = m.end();
          // new quote?
          inquote = (index < input.length()) && (input.charAt(index) == quoteChar);
        }
      }
      else {
        // Add match before separator
        matchList.add(input.subSequence(index, m.start()).toString());
        // Seek past separator
        index = m.end();
        // new quote?
        inquote = (index < input.length()) && (input.charAt(index) == quoteChar);
      }
    }
    // Nothing found - return original string.
    if(index == 0) {
      matchList.add(input);
      return matchList;
    }
    // Add tail after last separator.
    if(inquote) {
      if(input.charAt(input.length() - 1) == quoteChar) {
        matchList.add(input.subSequence(index + 1, input.length() - 1).toString());
      }
      else {
        getLogger().warning("Invalid quoted line in input.");
        matchList.add(input.subSequence(index, input.length()).toString());
      }
    }
    else {
      matchList.add(input.subSequence(index, input.length()).toString());
    }
    // Return
    return matchList;
  }

  /**
   * Get the logger for this class.
   * 
   * @return Logger.
   */
  protected abstract Logging getLogger();

  /**
   * Returns a string representation of the object.
   * 
   * @return a string representation of the object.
   */
  @Override
  public String toString() {
    return getClass().getName();
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static abstract class Parameterizer extends AbstractParameterizer {
    /**
     * Stores the column separator pattern
     */
    protected Pattern colSep = null;

    /**
     * Stores the quotation character
     */
    protected char quoteChar = QUOTE_CHAR.charAt(0);

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      PatternParameter colParam = new PatternParameter(COLUMN_SEPARATOR_ID, WHITESPACE_PATTERN);
      if(config.grab(colParam)) {
        colSep = colParam.getValue();
      }
      StringParameter quoteParam = new StringParameter(QUOTE_ID, new StringLengthConstraint(1, 1), QUOTE_CHAR);
      if(config.grab(quoteParam)) {
        quoteChar = quoteParam.getValue().charAt(0);
      }
    }

    @Override
    protected abstract AbstractParser makeInstance();
  }
}