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
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.random;

import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.math.exception.OutOfRangeException;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * This class provides a stable normalized random generator. It samples from a stable distribution with location
 * parameter 0 and scale 1.
 * </p>
 * 
 * <p>
 * The implementation uses the Chambers-Mallows-Stuck method as described in <i>Handbook of computational statistics:
 * concepts and methods</i> by James E. Gentle, Wolfgang H&auml;rdle, Yuichi Mori.
 * </p>
 * 
 * @version $Id: StableRandomGenerator.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class StableRandomGenerator implements NormalizedRandomGenerator {

    /** 0.5. */
    private static final double HALF = 0.5;

    /** Threshold. */
    private static final double THRESHOLD = 1E-8;

    /** Underlying generator. */
    private final RandomGenerator generator;

    /** stability parameter */
    private final double alpha;

    /** skewness parameter */
    private final double beta;

    /** cache of expression value used in generation */
    private final double zeta;

    /**
     * Create a new generator.
     * 
     * @param generatorIn
     *        underlying random generator to use
     * @param alphaIn
     *        Stability parameter. Must be in range (0, 2]
     * @param betaIn
     *        Skewness parameter. Must be in range [-1, 1]
     * @throws NullArgumentException
     *         if generator is null
     * @throws OutOfRangeException
     *         if {@code alpha <= 0} or {@code alpha > 2} or {@code beta < -1} or {@code beta > 1}
     */
    public StableRandomGenerator(final RandomGenerator generatorIn,
        final double alphaIn, final double betaIn) {
        if (generatorIn == null) {
            throw new NullArgumentException();
        }

        if (!(alphaIn > 0d && alphaIn <= 2d)) {
            throw new OutOfRangeException(PatriusMessages.OUT_OF_RANGE_LEFT,
                alphaIn, 0, 2);
        }

        if (!(betaIn >= -1d && betaIn <= 1d)) {
            throw new OutOfRangeException(PatriusMessages.OUT_OF_RANGE_SIMPLE,
                betaIn, -1, 1);
        }

        this.generator = generatorIn;
        this.alpha = alphaIn;
        this.beta = betaIn;
        if (alphaIn < 2d && betaIn != 0d) {
            this.zeta = betaIn * MathLib.tan(FastMath.PI * alphaIn / 2);
        } else {
            this.zeta = 0d;
        }
    }

    /**
     * Generate a random scalar with zero location and unit scale.
     * 
     * @return a random scalar with zero location and unit scale
     */
    @Override
    public double nextNormalizedDouble() {
        // we need 2 uniform random numbers to calculate omega and phi
        final double omega = -MathLib.log(this.generator.nextDouble());
        final double phi = FastMath.PI * (this.generator.nextDouble() - HALF);

        // Normal distribution case (Box-Muller algorithm)
        if (this.alpha == 2d) {
            return MathLib.sqrt(2d * omega) * MathLib.sin(phi);
        }

        double x;
        // when beta = 0, zeta is zero as well
        // Thus we can exclude it from the formula
        if (this.beta == 0d) {
            // Cauchy distribution case
            if (this.alpha == 1d) {
                x = MathLib.tan(phi);
            } else {
                x = MathLib.pow(omega * MathLib.cos((1 - this.alpha) * phi),
                    1d / this.alpha - 1d) *
                    MathLib.sin(this.alpha * phi) /
                    MathLib.pow(MathLib.cos(phi), 1d / this.alpha);
            }
        } else {
            // Generic stable distribution
            final double cosPhi = MathLib.cos(phi);
            // to avoid rounding errors around alpha = 1
            if (MathLib.abs(this.alpha - 1d) > THRESHOLD) {
                final double alphaPhi = this.alpha * phi;
                final double invAlphaPhi = phi - alphaPhi;
                final double[] sincosAlphaPhi = MathLib.sinAndCos(alphaPhi);
                final double sinAlphaPhi = sincosAlphaPhi[0];
                final double cosAlphaPhi = sincosAlphaPhi[1];
                final double[] sincosInvAlphaPhi = MathLib.sinAndCos(invAlphaPhi);
                final double sinInvPhi = sincosInvAlphaPhi[0];
                final double cosInvPhi = sincosInvAlphaPhi[1];
                x = (sinAlphaPhi + this.zeta * cosAlphaPhi) / cosPhi *
                    (cosInvPhi + this.zeta * sinInvPhi) /
                    MathLib.pow(omega * cosPhi, (1 - this.alpha) / this.alpha);
            } else {
                final double betaPhi = FastMath.PI / 2 + this.beta * phi;
                x = 2d / FastMath.PI * (betaPhi * MathLib.tan(phi) - this.beta *
                    MathLib.log(FastMath.PI / 2d * omega * cosPhi / betaPhi));

                if (this.alpha != 1d) {
                    x = x + this.beta * MathLib.tan(FastMath.PI * this.alpha / 2);
                }
            }
        }
        return x;
    }
}
