package de.lmu.ifi.dbs.elki.index.preprocessed.subspaceproj;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.LimitEigenPairFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredResult;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredRunner;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.LessEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterFlagGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;

/**
 * Preprocessor for 4C local dimensionality and locally weighted matrix
 * assignment to objects of a certain database.
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.composedOf PCAFilteredRunner
 * 
 * @param <V> Vector type
 * @param <D> Distance type
 */
@Title("4C Preprocessor")
@Description("Computes the local dimensionality and locally weighted matrix of objects of a certain database according to the 4C algorithm.\n" + "The PCA is based on epsilon range queries.")
public class FourCSubspaceIndex<V extends NumberVector<? extends V, ?>, D extends Distance<D>> extends AbstractSubspaceProjectionIndex<V, D, PCAFilteredResult> {
  /**
   * Our logger
   */
  private final static Logging logger = Logging.getLogger(FourCSubspaceIndex.class);

  /**
   * The Filtered PCA Runner
   */
  private PCAFilteredRunner<V, ?> pca;

  /**
   * Full constructor.
   * 
   * @param database
   * @param epsilon
   * @param rangeQueryDistanceFunction
   * @param minpts
   * @param pca
   */
  public FourCSubspaceIndex(Database<V> database, D epsilon, DistanceFunction<V, D> rangeQueryDistanceFunction, int minpts, PCAFilteredRunner<V, ?> pca) {
    super(database, epsilon, rangeQueryDistanceFunction, minpts);
    this.pca = pca;
  }

  @Override
  protected PCAFilteredResult computeProjection(DBID id, List<DistanceResultPair<D>> neighbors, Database<V> database) {
    ModifiableDBIDs ids = DBIDUtil.newArray(neighbors.size());
    for(DistanceResultPair<D> neighbor : neighbors) {
      ids.add(neighbor.getID());
    }
    PCAFilteredResult pcares = pca.processIds(ids, database);

    if(logger.isDebugging()) {
      StringBuffer msg = new StringBuffer();
      msg.append(id).append(" ").append(database.getObjectLabel(id));
      msg.append("\ncorrDim ").append(pcares.getCorrelationDimension());
      logger.debugFine(msg.toString());
    }
    return pcares;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  @Override
  public String getLongName() {
    return "4C local Subspaces";
  }

  @Override
  public String getShortName() {
    return "4C-subspaces";
  }

  /**
   * Factory class for 4C preprocessors.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses FourCSubspaceIndex oneway - - «creates»
   * 
   * @param <V> Vector type
   * @param <D> Distance type
   */
  public static class Factory<V extends NumberVector<? extends V, ?>, D extends Distance<D>> extends AbstractSubspaceProjectionIndex.Factory<V, D, FourCSubspaceIndex<V, D>> {
    /**
     * Flag for marking parameter delta as an absolute value.
     */
    private final Flag ABSOLUTE_FLAG = new Flag(LimitEigenPairFilter.EIGENPAIR_FILTER_ABSOLUTE);

    /**
     * Option string for parameter delta.
     */
    private final DoubleParameter DELTA_PARAM = new DoubleParameter(LimitEigenPairFilter.EIGENPAIR_FILTER_DELTA, new GreaterEqualConstraint(0), DEFAULT_DELTA);

    /**
     * The default value for delta.
     */
    public static final double DEFAULT_DELTA = LimitEigenPairFilter.DEFAULT_DELTA;

    /**
     * Threshold for strong eigenpairs, can be absolute or relative.
     */
    private double delta;

    /**
     * Indicates whether delta is an absolute or a relative value.
     */
    private boolean absolute;

    /**
     * The Filtered PCA Runner
     */
    private PCAFilteredRunner<V, ?> pca;

    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     * 
     * @param config Parameterization
     */
    public Factory(Parameterization config) {
      super(config);
      config = config.descend(this);

      // flag absolute
      if(config.grab(ABSOLUTE_FLAG)) {
        absolute = ABSOLUTE_FLAG.getValue();
      }

      // Parameter delta
      // parameter constraint are only valid if delta is a relative value!
      // Thus they are dependent on the absolute flag, that is they are global
      // constraints!
      if(config.grab(DELTA_PARAM)) {
        delta = DELTA_PARAM.getValue();
      }
      // Absolute flag doesn't have a sensible default value for delta.
      if(absolute && DELTA_PARAM.tookDefaultValue()) {
        config.reportError(new WrongParameterValueException("Illegal parameter setting: " + "Flag " + ABSOLUTE_FLAG.getName() + " is set, " + "but no value for " + DELTA_PARAM.getName() + " is specified."));
      }

      // if (optionHandler.isSet(DELTA_P)) {
      // delta = (Double) optionHandler.getOptionValue(DELTA_P);
      // try {
      // if (!absolute && delta < 0 || delta > 1)
      // throw new WrongParameterValueException(DELTA_P, "delta", DELTA_D);
      // } catch (NumberFormatException e) {
      // throw new WrongParameterValueException(DELTA_P, "delta", DELTA_D, e);
      // }
      // } else if (!absolute) {
      // delta = LimitEigenPairFilter.DEFAULT_DELTA;
      // } else {
      // throw new WrongParameterValueException("Illegal parameter setting: " +
      // "Flag " + ABSOLUTE_F + " is set, " + "but no value for " + DELTA_P +
      // " is specified.");
      // }

      // Parameterize PCA
      ListParameterization pcaParameters = new ListParameterization();
      // eigen pair filter
      pcaParameters.addParameter(PCAFilteredRunner.PCA_EIGENPAIR_FILTER, LimitEigenPairFilter.class.getName());
      // abs
      if(absolute) {
        pcaParameters.addFlag(LimitEigenPairFilter.EIGENPAIR_FILTER_ABSOLUTE);
      }
      // delta
      pcaParameters.addParameter(LimitEigenPairFilter.EIGENPAIR_FILTER_DELTA, Double.toString(delta));
      // big value
      pcaParameters.addParameter(PCAFilteredRunner.BIG_ID, 50);
      // small value
      pcaParameters.addParameter(PCAFilteredRunner.SMALL_ID, 1);
      pca = new PCAFilteredRunner<V, DoubleDistance>(pcaParameters);
      for(ParameterException e : pcaParameters.getErrors()) {
        LoggingUtil.warning("Error in internal parameterization: " + e.getMessage());
      }

      final ArrayList<ParameterConstraint<Number>> deltaCons = new ArrayList<ParameterConstraint<Number>>();
      // TODO: this constraint is already set in the parameter itself, since it
      // also applies to the relative case, right? -- erich
      // deltaCons.add(new GreaterEqualConstraint(0));
      deltaCons.add(new LessEqualConstraint(1));

      GlobalParameterConstraint gpc = new ParameterFlagGlobalConstraint<Number, Double>(DELTA_PARAM, deltaCons, ABSOLUTE_FLAG, false);
      config.checkConstraint(gpc);
    }

    @Override
    public FourCSubspaceIndex<V, D> instantiate(Database<V> database) {
      return new FourCSubspaceIndex<V, D>(database, epsilon, rangeQueryDistanceFunction, minpts, pca);
    }
  }
}