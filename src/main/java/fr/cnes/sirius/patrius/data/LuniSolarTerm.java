/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2017 CNES
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
/*
 * 
 */
package fr.cnes.sirius.patrius.data;

/**
 * Class for luni-solar only terms.
 * 
 * @author Luc Maisonobe
 */
class LuniSolarTerm extends SeriesTerm {

    /** Serializable UID. */
    private static final long serialVersionUID = -461066685792014379L;

    /** Coefficient for mean anomaly of the Moon. */
    private final int cL;

    /** Coefficient for mean anomaly of the Sun. */
    private final int cLPrime;

    /** Coefficient for L - &Omega; where L is the mean longitude of the Moon. */
    private final int cF;

    /** Coefficient for mean elongation of the Moon from the Sun. */
    private final int cD;

    /** Coefficient for mean longitude of the ascending node of the Moon. */
    private final int cOmega;

    /**
     * Build a luni-solar term for nutation series.
     * 
     * @param sinCoeff
     *        coefficient for the sine of the argument
     * @param cosCoeff
     *        coefficient for the cosine of the argument
     * @param cLIn
     *        coefficient for mean anomaly of the Moon
     * @param cLPrimeIn
     *        coefficient for mean anomaly of the Sun
     * @param cFIn
     *        coefficient for L - &Omega; where L is the mean longitude of the Moon
     * @param cDIn
     *        coefficient for mean elongation of the Moon from the Sun
     * @param cOmegaIn
     *        coefficient for mean longitude of the ascending node of the Moon
     */
    public LuniSolarTerm(final double sinCoeff, final double cosCoeff,
        final int cLIn, final int cLPrimeIn, final int cFIn, final int cDIn, final int cOmegaIn) {
        super(sinCoeff, cosCoeff);
        this.cL = cLIn;
        this.cLPrime = cLPrimeIn;
        this.cF = cFIn;
        this.cD = cDIn;
        this.cOmega = cOmegaIn;
    }

    /** {@inheritDoc} */
    @Override
    protected double argument(final BodiesElements elements) {
        return this.cL * elements.getL() + this.cLPrime * elements.getLPrime() + this.cF * elements.getF() +
            this.cD * elements.getD() + this.cOmega * elements.getOmega();
    }

}
