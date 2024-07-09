/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2017 CNES
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
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
/*
 * 
 */
package fr.cnes.sirius.patrius.data;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Base class for nutation series terms.
 * 
 * @author Luc Maisonobe
 * @see PoissonSeries
 */
// CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class SeriesTerm implements Serializable {
    // CHECKSTYLE: resume AbstractClassName check

    /** Serializable UID. */
    private static final long serialVersionUID = 7536446008538764006L;

    /** Coefficient for the sine of the argument. */
    private final double sinCoeff;

    /** Coefficient for the cosine of the argument. */
    private final double cosCoeff;

    /**
     * Simple constructor for the base class.
     * 
     * @param sinCoeffIn
     *        coefficient for the sine of the argument
     * @param cosCoeffIn
     *        coefficient for the cosine of the argument
     */
    protected SeriesTerm(final double sinCoeffIn, final double cosCoeffIn) {
        this.sinCoeff = sinCoeffIn;
        this.cosCoeff = cosCoeffIn;
    }

    /**
     * Compute the value of the term for the current date.
     * 
     * @param elements
     *        luni-solar and planetary elements for the current date
     * @return current value of the term
     */
    public double value(final BodiesElements elements) {
        final double a = this.argument(elements);
        final double[] sincos = MathLib.sinAndCos(a);
        final double sin = sincos[0];
        final double cos = sincos[1];
        return this.sinCoeff * sin + this.cosCoeff * cos;
    }

    /**
     * Compute the value of the term for the current date and its first time derivative.
     * 
     * @param elements
     *        luni-solar and planetary elements for the current date
     * @param elementsP
     *        luni-solar and planetary time derivative elements for the current date
     * @return current value of the term
     */
    public double[] value(final BodiesElements elements, final BodiesElements elementsP) {
        final double a = this.argument(elements);
        final double ap = this.argument(elementsP);
        final double[] sincos = MathLib.sinAndCos(a);
        final double sin = sincos[0];
        final double cos = sincos[1];
        return new double[] { this.sinCoeff * sin + this.cosCoeff * cos,
            ap * (this.sinCoeff * cos - this.cosCoeff * sin) };
    }

    /**
     * Compute the argument for the current date.
     * 
     * @param elements
     *        luni-solar and planetary elements for the current date
     * @return current value of the argument
     */
    protected abstract double argument(final BodiesElements elements);

    /**
     * Factory method for building the appropriate object.
     * <p>
     * The method checks the null coefficients and build an instance of an appropriate type to avoid too many
     * unnecessary multiplications by zero coefficients.
     * </p>
     * 
     * @param sinCoeff
     *        coefficient for the sine of the argument
     * @param cosCoeff
     *        coefficient for the cosine of the argument
     * @param cL
     *        coefficient for mean anomaly of the Moon
     * @param cLPrime
     *        coefficient for mean anomaly of the Sun
     * @param cF
     *        coefficient for L - &Omega; where L is the mean longitude of the Moon
     * @param cD
     *        coefficient for mean elongation of the Moon from the Sun
     * @param cOmega
     *        coefficient for mean longitude of the ascending node of the Moon
     * @param cMe
     *        coefficient for mean Mercury longitude
     * @param cVe
     *        coefficient for mean Venus longitude
     * @param cE
     *        coefficient for mean Earth longitude
     * @param cMa
     *        coefficient for mean Mars longitude
     * @param cJu
     *        coefficient for mean Jupiter longitude
     * @param cSa
     *        coefficient for mean Saturn longitude
     * @param cUr
     *        coefficient for mean Uranus longitude
     * @param cNe
     *        coefficient for mean Neptune longitude
     * @param cPa
     *        coefficient for general accumulated precession in longitude
     * @return a nutation serie term instance well suited for the set of coefficients
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    public static SeriesTerm buildTerm(final double sinCoeff, final double cosCoeff,
                                       final int cL, final int cLPrime, final int cF, final int cD, final int cOmega,
                                       final int cMe, final int cVe, final int cE, final int cMa, final int cJu,
                                       final int cSa,
                                       final int cUr, final int cNe, final int cPa) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        // Conditions
        boolean c1 = cL == 0 && cLPrime == 0 && cF == 0;
        c1 &= cD == 0 && cOmega == 0;
        boolean c2 = cMe == 0 && cVe == 0 && cE == 0;
        c2 &= cMa == 0 && cJu == 0 && cSa == 0;
        c2 &= cUr == 0 && cNe == 0 && cPa == 0;

        // Initialisation
        final SeriesTerm res;

        // Computation
        if (c1) {
            res = new PlanetaryTerm(sinCoeff, cosCoeff, cMe, cVe, cE, cMa, cJu, cSa, cUr, cNe, cPa);
        } else if (c2) {
            res = new LuniSolarTerm(sinCoeff, cosCoeff, cL, cLPrime, cF, cD, cOmega);
        } else if (cLPrime == 0 && cUr == 0 && cNe == 0 && cPa == 0) {
            res = new NoFarPlanetsTerm(sinCoeff, cosCoeff, cL, cF, cD, cOmega,
                cMe, cVe, cE, cMa, cJu, cSa);
        } else {
            res = new GeneralTerm(sinCoeff, cosCoeff, cL, cLPrime, cF, cD, cOmega,
                cMe, cVe, cE, cMa, cJu, cSa, cUr, cNe, cPa);
        }

        // Return result
        return res;
    }

}
