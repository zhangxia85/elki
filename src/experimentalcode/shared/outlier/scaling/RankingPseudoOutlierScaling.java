package experimentalcode.shared.outlier.scaling;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.result.outlier.InvertedOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.scaling.outlier.OutlierScalingFunction;

/**
 * This is a pseudo outlier scoring obtained by only considering the ranks of
 * the objects.
 * 
 * @author Erich Schubert
 */
public class RankingPseudoOutlierScaling implements OutlierScalingFunction {
  /**
   * The actual scores
   */
  private double[] scores;

  private boolean inverted = false;

  @Override
  public void prepare(Database<?> db, OutlierResult or) {
    // collect all outlier scores
    scores = new double[db.size()];
    int pos = 0;
    if (or.getOutlierMeta() instanceof InvertedOutlierScoreMeta) {
      inverted = true;
    }
    for(DBID id : db) {
      scores[pos] = or.getScores().getValueFor(id);
      pos++;
    }
    if(pos != db.size()) {
      throw new AbortException("Database size is incorrect!");
    }
    // sort them
    // TODO: Inverted scores!
    Arrays.sort(scores);
  }

  @Override
  public double getMax() {
    return 1.0;
  }

  @Override
  public double getMin() {
    return 0.0;
  }

  @Override
  public double getScaled(double value) {
    int pos = Arrays.binarySearch(scores, value);
    if(inverted) {
      return 1.0 - ((double) pos) / scores.length;
    }
    else {
      return ((double) pos) / scores.length;
    }
  }
}