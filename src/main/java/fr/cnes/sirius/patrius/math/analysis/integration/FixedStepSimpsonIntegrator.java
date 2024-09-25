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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1271:04/11/2017:change Coppola method
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.integration;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements <a href="http://mathworld.wolfram.com/SimpsonsRule.html">
 * Simpson's Rule</a> for integration of real univariate functions.
 * This integrator uses a fixed step to perform integration unlike {@link SimpsonIntegrator}.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.0
 *
 * @version $Id: SimpsonIntegrator.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class FixedStepSimpsonIntegrator extends BaseAbstractUnivariateIntegrator {

     /** Serializable UID. */
    private static final long serialVersionUID = 2257851888329989798L;

    /** Number of evaluations points. */
    private final int evaluationPoints;

    /** Cumulated integration data (specific to COLOSUS). */
    private List<Double> cumulatedIntegration;

    /**
     * Constructor.
     * 
     * @param evaluationPointsIn
     *        number of evaluations points
     */
    public FixedStepSimpsonIntegrator(final int evaluationPointsIn) {
        super(1, Integer.MAX_VALUE);
        this.evaluationPoints = evaluationPointsIn;
    }

    /** {@inheritDoc} */
    @Override
    protected double doIntegrate() {

        this.cumulatedIntegration = new ArrayList<>();

        // Step
        final double step = (this.getMax() - this.getMin()) / (this.evaluationPoints - 1);
        final double scale = step / 3.;

        // Integration

        // Lower bounds
        double res = this.computeObjectiveValue(this.getMin()) * scale;
        this.cumulatedIntegration.add(res);

        // Points within bounds
        for (int i = 1; i < this.evaluationPoints - 1; i++) {
            final double t = this.getMin() + i * step;
            double tmp = 0;
            if (i % 2 == 0) {
                tmp = 2. * this.computeObjectiveValue(t) * scale;
            } else {
                tmp = 4. * this.computeObjectiveValue(t) * scale;
            }
            res += tmp;
            this.cumulatedIntegration.add(res);
        }

        // Higher bound
        final double high = this.computeObjectiveValue(this.getMax()) * scale;
        res += high;
        this.cumulatedIntegration.add(res);

        // Return result
        return res;
    }

    /**
     * Returns cumulated integration data (specific to COLOSUS).
     * 
     * @return cumulated integration data. Last value correspond to integration from min to max
     */
    public List<Double> getCumulatedIntegration() {
        return this.cumulatedIntegration;
    }

}
