/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.nonlinear.scalar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import fr.cnes.sirius.patrius.math.exception.NotStrictlyPositiveException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.optim.BaseMultiStartMultivariateOptimizer;
import fr.cnes.sirius.patrius.math.optim.PointValuePair;
import fr.cnes.sirius.patrius.math.random.RandomVectorGenerator;

/**
 * Multi-start optimizer.
 * 
 * This class wraps an optimizer in order to use it several times in
 * turn with different starting points (trying to avoid being trapped
 * in a local extremum when looking for a global one).
 * 
 * @version $Id: MultiStartMultivariateOptimizer.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class MultiStartMultivariateOptimizer
    extends BaseMultiStartMultivariateOptimizer<PointValuePair> {
    /** Underlying optimizer. */
    private final MultivariateOptimizer optimizer;
    /** Found optima. */
    private final List<PointValuePair> optima = new ArrayList<PointValuePair>();

    /**
     * Create a multi-start optimizer from a single-start optimizer.
     * 
     * @param optimizerIn
     *        Single-start optimizer to wrap.
     * @param starts
     *        Number of starts to perform.
     *        If {@code starts == 1}, the result will be same as if {@code optimizer} is called directly.
     * @param generator
     *        Random vector generator to use for restarts.
     * @throws NullArgumentException
     *         if {@code optimizer} or {@code generator} is {@code null}.
     * @throws NotStrictlyPositiveException
     *         if {@code starts < 1}.
     */
    public MultiStartMultivariateOptimizer(final MultivariateOptimizer optimizerIn,
        final int starts,
        final RandomVectorGenerator generator) {
        super(optimizerIn, starts, generator);
        this.optimizer = optimizerIn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PointValuePair[] getOptima() {
        Collections.sort(this.optima, this.getPairComparator());
        return this.optima.toArray(new PointValuePair[this.optima.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void store(final PointValuePair optimum) {
        this.optima.add(optimum);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void clear() {
        this.optima.clear();
    }

    /**
     * @return a comparator for sorting the optima.
     */
    private Comparator<PointValuePair> getPairComparator() {
        return new Comparator<PointValuePair>(){
            /** {@inheritDoc} */
            @Override
            public int compare(final PointValuePair o1,
                               final PointValuePair o2) {
                // initialize result
                final int res;
                if (o1 == null) {
                    // return 1 if o2 exists (o2 greater than null), 0 otherwise
                    res = (o2 == null) ? 0 : 1;
                } else if (o2 == null) {
                    // o1 is greater than null, return negative value
                    res = -1;
                } else {
                    // compute result of comparaison depending on the goaltype of the optimizer:
                    // - compare the value of o1 to the value of o2 if the goal is to minimize
                    // - compare the value of o2 to the value of o1 if the goal is to maximize
                    final double v1 = o1.getValue();
                    final double v2 = o2.getValue();
                    res = (MultiStartMultivariateOptimizer.this.optimizer.getGoalType() == GoalType.MINIMIZE) ?
                        Double.compare(v1, v2) : Double.compare(v2, v1);
                }
                return res;
            }
        };
    }
}
