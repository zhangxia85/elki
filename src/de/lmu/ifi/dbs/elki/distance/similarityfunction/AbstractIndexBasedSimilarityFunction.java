package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.query.similarity.AbstractDBIDSimilarityQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract super class for distance functions needing a preprocessor.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.uses Preprocessor
 * 
 * @param <O> object type
 * @param <I> index type
 * @param <D> distance type
 */
public abstract class AbstractIndexBasedSimilarityFunction<O extends DatabaseObject, I extends Index<O>, R, D extends Distance<D>> implements IndexBasedSimilarityFunction<O, D> {
  /**
   * OptionID for {@link #INDEX_PARAM}
   */
  public static final OptionID INDEX_ID = OptionID.getOrCreateOptionID("similarityfunction.preprocessor", "Preprocessor to use.");

  /**
   * Parameter to specify the preprocessor to be used.
   * <p>
   * Key: {@code -similarityfunction.preprocessor}
   * </p>
   */
  private final ObjectParameter<IndexFactory<O, I>> INDEX_PARAM;

  /**
   * Parameter to specify the preprocessor to be used.
   * <p>
   * Key: {@code -similarityfunction.preprocessor}
   * </p>
   */
  protected IndexFactory<O, I> index;

  /**
   * Constructor, supporting
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public AbstractIndexBasedSimilarityFunction(Parameterization config) {
    super();
    config = config.descend(this);
    INDEX_PARAM = new ObjectParameter<IndexFactory<O, I>>(INDEX_ID, getIndexFactoryRestriction(), getIndexFactoryDefaultClass());
    if(config.grab(INDEX_PARAM)) {
      index = INDEX_PARAM.instantiateClass(config);
    }
  }

  /**
   * Get the index factory restriction
   * 
   * @return Factory class restriction
   */
  abstract protected Class<?> getIndexFactoryRestriction();

  /**
   * Get the default index factory class.
   * 
   * @return Index factory
   */
  abstract protected Class<?> getIndexFactoryDefaultClass();

  @Override
  abstract public <T extends O> Instance<T, ?, R, D> instantiate(Database<T> database);

  /**
   * The actual instance bound to a particular database.
   * 
   * @author Erich Schubert
   * 
   * @param <O> Object type
   * @param <P> Preprocessor type
   * @param <D> Distance result type
   */
  abstract public static class Instance<O extends DatabaseObject, I extends Index<O>, R, D extends Distance<D>> extends AbstractDBIDSimilarityQuery<O, D> implements IndexBasedSimilarityFunction.Instance<O, I, D> {
    /**
     * Parent index
     */
    protected final I index;

    /**
     * Constructor.
     * 
     * @param database Database
     * @param index Index to use
     */
    public Instance(Database<O> database, I index) {
      super(database);
      this.index = index;
    }

    @Override
    public I getIndex() {
      return index;
    }
  }

  @Override
  public boolean isSymmetric() {
    return true;
  }
}