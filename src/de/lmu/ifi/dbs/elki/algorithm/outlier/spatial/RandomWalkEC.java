package de.lmu.ifi.dbs.elki.algorithm.outlier.spatial;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Spatial outlier detection based on random walks.
 * 
 * Note: this method can only handle one-dimensional data, but could probably be
 * easily extended to higher dimensional data by using an distance function
 * instead of the absolute difference.
 * 
 * <p>
 * X. Liu and C.-T. Lu and F. Chen, <br>
 * Random Walk on Exhaustive Combination <br>
 * Spatial outlier detection: random walk based approaches, <br>
 * in Proceedings of the 18th SIGSPATIAL International Conference on Advances in
 * Geographic Information Systems,2010
 * </p>
 * 
 * @author Ahmed Hettab
 * 
 * @param <N> Spatial Vector type
 * @param <D> Distance to use
 */
@Title("Random Walk on Exhaustive Combination")
@Description("Spatial Outlier Detection using Random Walk on Exhaustive Combination")
@Reference(authors = "X. Liu and C.-T. Lu and F. Chen", title = "Spatial outlier detection: random walk based approaches", booktitle = "Proc. 18th SIGSPATIAL International Conference on Advances in Geographic Information Systems, 2010")
public class RandomWalkEC<N, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<N, D, OutlierResult> implements OutlierAlgorithm {
  /**
   * Logger
   */
  private static final Logging logger = Logging.getLogger(RandomWalkEC.class);

  /**
   * Parameter alpha: Attribute difference exponent
   */
  private double alpha;

  /**
   * Parameter c: damping factor
   */
  private double c;

  /**
   * Parameter k
   */
  private int k;

  /**
   * Constructor
   * 
   * @param distanceFunction Distance function
   * @param alpha Alpha parameter
   * @param c C parameter
   * @param k Number of neighbors
   */
  public RandomWalkEC(DistanceFunction<N, D> distanceFunction, double alpha, double c, int k) {
    super(distanceFunction);
    this.alpha = alpha;
    this.c = c;
    this.k = k;
  }

  /**
   * Run the algorithm
   * 
   * @param spatial Spatial neighborhood relation
   * @param relation Attribute value relation
   * @return Outlier result
   */
  public OutlierResult run(Relation<N> spatial, Relation<? extends NumberVector<?, ?>> relation) {
    DistanceQuery<N, D> distFunc = getDistanceFunction().instantiate(spatial);
    WritableDataStore<Vector> similarityVectors = DataStoreUtil.makeStorage(spatial.getDBIDs(), DataStoreFactory.HINT_TEMP, Vector.class);
    WritableDataStore<DBIDs> neighbors = DataStoreUtil.makeStorage(spatial.getDBIDs(), DataStoreFactory.HINT_TEMP, DBIDs.class);

    // Make a static IDs array for matrix column indexing
    ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());

    // construct the relation Matrix of the ec-graph
    Matrix E = new Matrix(ids.size(), ids.size());
    KNNHeap<D> heap = new KNNHeap<D>(k);
    for(int i = 0; i < ids.size(); i++) {
      final DBID id = ids.get(i);
      final double val = relation.get(id).doubleValue(1);
      assert (heap.size() == 0);
      for(int j = 0; j < ids.size(); j++) {
        if(i == j) {
          continue;
        }
        final DBID n = ids.get(j);
        final double e;
        final D distance = distFunc.distance(id, n);
        heap.add(distance, n);
        double dist = distance.doubleValue();
        if(dist == 0) {
          logger.warning("Zero distances are not supported - skipping: " + id + " " + n);
          e = 0;
        }
        else {
          double diff = Math.abs(val - relation.get(n).doubleValue(1));
          double exp = Math.exp(Math.pow(diff, alpha));
          // Implementation note: not inverting exp worked a lot better.
          // Therefore we diverge from the article here.
          e = exp / dist;
        }
        E.set(j, i, e);
      }
      // Convert kNN Heap into DBID array
      ModifiableDBIDs nids = DBIDUtil.newArray(heap.size());
      while(!heap.isEmpty()) {
        nids.add(heap.poll().getDBID());
      }
      neighbors.put(id, nids);
    }
    // normalize the adjacent Matrix
    // Sum based normalization - don't use E.normalizeColumns()
    // Which normalized to Euclidean length 1.0!
    // Also do the -c multiplication in this process.
    for(int i = 0; i < E.getColumnDimensionality(); i++) {
      double sum = 0.0;
      for(int j = 0; j < E.getRowDimensionality(); j++) {
        sum += E.get(j, i);
      }
      if(sum == 0) {
        sum = 1.0;
      }
      for(int j = 0; j < E.getRowDimensionality(); j++) {
        E.set(j, i, -c * E.get(j, i) / sum);
      }
    }
    // Add identity matrix. The diagonal should still be 0s, so this is trivial.
    assert (E.getRowDimensionality() == E.getColumnDimensionality());
    for(int col = 0; col < E.getColumnDimensionality(); col++) {
      assert (E.get(col, col) == 0.0);
      E.set(col, col, 1.0);
    }
    E = E.inverse().timesEquals(1 - c);

    // Split the matrix into columns
    for(int i = 0; i < ids.size(); i++) {
      DBID id = ids.get(i);
      // Note: matrix times ith unit vector = ith column
      Vector sim = E.getColumnVector(i);
      similarityVectors.put(id, sim);
    }
    E = null;
    // compute the relevance scores between specified Object and its neighbors
    DoubleMinMax minmax = new DoubleMinMax();
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(spatial.getDBIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    for(int i = 0; i < ids.size(); i++) {
      DBID id = ids.get(i);
      double gmean = 1.0;
      int cnt = 0;
      for(DBID n : neighbors.get(id)) {
        if(id.equals(n)) {
          continue;
        }
        double sim = MathUtil.cosineSimilarity(similarityVectors.get(id), similarityVectors.get(n));
        gmean *= sim;
        cnt++;
      }
      final double score = Math.pow(gmean, 1.0 / cnt);
      minmax.put(score);
      scores.put(id, score);
    }

    Relation<Double> scoreResult = new MaterializedRelation<Double>("randomwalkec", "RandomWalkEC", TypeUtil.DOUBLE, scores, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0.0);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction(), VectorFieldTypeInformation.get(NumberVector.class, 1));
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   * 
   * @author Ahmed Hettab
   * 
   * @apiviz.exclude
   * 
   * @param <N> Vector type
   * @param <D> Distance type
   */
  public static class Parameterizer<N, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<N, D> {
    /**
     * Parameter to specify the number of neighbors
     */
    public static final OptionID K_ID = OptionID.getOrCreateOptionID("randomwalkec.k", "Number of nearest neighbors to use.");

    /**
     * Parameter to specify alpha
     */
    public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("randomwalkec.alpha", "Scaling exponent for value differences.");

    /**
     * Parameter to specify the c
     */
    public static final OptionID C_ID = OptionID.getOrCreateOptionID("randomwalkec.c", "The damping parameter c.");

    /**
     * Parameter alpha: scaling
     */
    double alpha = 0.5;

    /**
     * Parameter c: damping coefficient
     */
    double c = 0.9;

    /**
     * Parameter for kNN
     */
    int k;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configK(config);
      configAlpha(config);
      configC(config);
    }

    /**
     * Get the kNN parameter
     * 
     * @param config Parameterization
     */
    protected void configK(Parameterization config) {
      final IntParameter param = new IntParameter(K_ID, new GreaterEqualConstraint(1));
      if(config.grab(param)) {
        k = param.getValue();
      }
    }

    /**
     * Get the alpha parameter
     * 
     * @param config Parameterization
     */
    protected void configAlpha(Parameterization config) {
      final DoubleParameter param = new DoubleParameter(ALPHA_ID, 0.5);
      if(config.grab(param)) {
        alpha = param.getValue();
      }
    }

    /**
     * get the c parameter
     * 
     * @param config
     */
    protected void configC(Parameterization config) {
      final DoubleParameter param = new DoubleParameter(C_ID);
      if(config.grab(param)) {
        c = param.getValue();
      }
    }

    @Override
    protected RandomWalkEC<N, D> makeInstance() {
      return new RandomWalkEC<N, D>(distanceFunction, alpha, c, k);
    }
  }
}