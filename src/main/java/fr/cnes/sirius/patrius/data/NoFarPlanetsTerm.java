/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2022 CNES
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
 * END-HISTORY
 */
/*
 * 
 */
package fr.cnes.sirius.patrius.data;

/**
 * Class for terms that do not depend on far planets and some other elements.
 * 
 * @author Luc Maisonobe
 */
class NoFarPlanetsTerm extends SeriesTerm {

    /** Serializable UID. */
    private static final long serialVersionUID = -6466886528892169861L;

    /** Coefficient for mean anomaly of the Moon. */
    private final int cL;

    /** Coefficient for L - &Omega; where L is the mean longitude of the Moon. */
    private final int cF;

    /** Coefficient for mean elongation of the Moon from the Sun. */
    private final int cD;

    /** Coefficient for mean longitude of the ascending node of the Moon. */
    private final int cOmega;

    /** Coefficient for mean Mercury longitude. */
    private final int cMe;

    /** Coefficient for mean Venus longitude. */
    private final int cVe;

    /** Coefficient for mean Earth longitude. */
    private final int cE;

    /** Coefficient for mean Mars longitude. */
    private final int cMa;

    /** Coefficient for mean Jupiter longitude. */
    private final int cJu;

    /** Coefficient for mean Saturn longitude. */
    private final int cSa;

    /**
     * Build a planetary term for nutation series.
     * 
     * @param sinCoeff
     *        coefficient for the sine of the argument
     * @param cosCoeff
     *        coefficient for the cosine of the argument
     * @param cLIn
     *        coefficient for mean anomaly of the Moon
     * @param cFIn
     *        coefficient for L - &Omega; where L is the mean longitude of the Moon
     * @param cDIn
     *        coefficient for mean elongation of the Moon from the Sun
     * @param cOmegaIn
     *        coefficient for mean longitude of the ascending node of the Moon
     * @param cMeIn
     *        coefficient for mean Mercury longitude
     * @param cVeIn
     *        coefficient for mean Venus longitude
     * @param cEIn
     *        coefficient for mean Earth longitude
     * @param cMaIn
     *        coefficient for mean Mars longitude
     * @param cJuIn
     *        coefficient for mean Jupiter longitude
     * @param cSaIn
     *        coefficient for mean Saturn longitude
     */
    public NoFarPlanetsTerm(final double sinCoeff, final double cosCoeff,
        final int cLIn, final int cFIn, final int cDIn, final int cOmegaIn,
        final int cMeIn, final int cVeIn, final int cEIn, final int cMaIn,
        final int cJuIn, final int cSaIn) {
        super(sinCoeff, cosCoeff);
        this.cL = cLIn;
        this.cF = cFIn;
        this.cD = cDIn;
        this.cOmega = cOmegaIn;
        this.cMe = cMeIn;
        this.cVe = cVeIn;
        this.cE = cEIn;
        this.cMa = cMaIn;
        this.cJu = cJuIn;
        this.cSa = cSaIn;
    }

    /** {@inheritDoc} */
    @Override
    protected double argument(final BodiesElements elements) {
        return this.cL * elements.getL() + this.cF * elements.getF() +
            this.cD * elements.getD() + this.cOmega * elements.getOmega() +
            this.cMe * elements.getLMe() + this.cVe * elements.getLVe() + this.cE * elements.getLE() +
            this.cMa * elements.getLMa() + this.cJu * elements.getLJu() + this.cSa * elements.getLSa();

    }

}
