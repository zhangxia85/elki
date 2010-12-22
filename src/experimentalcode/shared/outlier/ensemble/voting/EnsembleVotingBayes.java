package experimentalcode.shared.outlier.ensemble.voting;

import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Combination rule based on Bayes theorems.
 * 
 * @author Erich Schubert
 */
public class EnsembleVotingBayes implements EnsembleVoting {
  /**
   * Option ID for the minimum and maximum vote
   */
  public final static OptionID MIN_ID = OptionID.getOrCreateOptionID("ensemble.bayes.min", "Minimum (and maximum) vote share.");

  /**
   * Minimum vote to cast.
   */
  private final DoubleParameter MIN_PARAM = new DoubleParameter(MIN_ID, new IntervalConstraint(0.0, IntervalConstraint.IntervalBoundary.CLOSE, 0.5, IntervalConstraint.IntervalBoundary.OPEN), 0.05);

  /**
   * Minimum vote to cast
   */
  private double minvote = 0.05;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public EnsembleVotingBayes(Parameterization config) {
    config = config.descend(this);
    if(config.grab(MIN_PARAM)) {
      minvote = MIN_PARAM.getValue();
    }
  }

  @Override
  public double combine(List<Double> scores) {
    double pos = 1.0;
    double neg = 1.0;
    for(Double score : scores) {
      if(score < minvote) {
        score = minvote;
      }
      else if(score > 1.0 - minvote) {
        score = 1.0 - minvote;
      }
      pos *= score;
      neg *= (1.0 - score);
    }
    return pos / (pos + neg);
  }
}