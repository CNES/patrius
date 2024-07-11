/**
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
 * 
 * @history Created 13/07/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:275:10/10/2014:public visibility to OceanTidesCoefficientsSet class
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.tides.coefficients;

/**
 * Represents a line from the ocean tides data file. <br>
 * The proper way to use this it to call the {@link OceanTidesCoefficientsFactory#getCoefficientsProvider()
 * getCoefficientProvider} method. Indeed, the {@link OceanTidesCoefficientsFactory} will determine the best reader to
 * use, depending on file available in the file system.
 * 
 * @concurrency immutable
 * 
 * @author Rami Houdroge
 * 
 * @since 1.2
 * 
 * @version $Id: OceanTidesCoefficientsSet.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 */
public final class OceanTidesCoefficientsSet {

    /** Constant. */
    private static final int C_100000 = 100000;
    /** Constant. */
    private static final int C_1000 = 1000;
    /**
     * Doodson number.
     */
    private final double d;
    /**
     * degree.
     */
    private final int l;
    /**
     * order.
     */
    private final int m;
    /**
     * C<sub>lm</sub><sup>+</sup>.
     */
    private final double csp;
    /**
     * C<sub>lm</sub><sup>-</sup>.
     */
    private final double ccp;
    /**
     * S<sub>lm</sub><sup>+</sup>.
     */
    private final double csm;
    /**
     * S<sub>lm</sub><sup>-</sup>.
     */
    private final double ccm;

    /**
     * C<sub>lm</sub><sup>+</sup>.
     */
    private final double cp;
    /**
     * C<sub>lm</sub><sup>-</sup>.
     */
    private final double cm;
    /**
     * ε<sub>lm</sub><sup>+</sup>.
     */
    private final double ep;
    /**
     * ε<sub>lm</sub><sup>-</sup>.
     */
    private final double em;
    /**
     * Hash code.
     */
    private final double hashcodeNumber;

    /**
     * Constructor. Create new {@link OceanTidesCoefficientsSet} from line of file
     * 
     * @param nDoodson
     *        doodson number
     * @param degree
     *        degree
     * @param order
     *        order
     * @param cslmp
     *        Csin<sub>lm</sub><sup>+</sup>
     * @param cclmp
     *        Ccos<sub>lm</sub><sup>+</sup>
     * @param cslmm
     *        Csin<sub>lm</sub><sup>-</sup>
     * @param cclmm
     *        Ccos<sub>lm</sub><sup>-</sup>
     * @param clmp
     *        C<sub>lm</sub><sup>+</sup>
     * @param elmp
     *        ε<sub>lm</sub><sup>+</sup>
     * @param clmm
     *        C<sub>lm</sub><sup>-</sup>
     * @param elmm
     *        ε<sub>lm</sub><sup>-</sup>
     */
    public OceanTidesCoefficientsSet(final double nDoodson, final int degree, final int order, final double cslmp,
        final double cclmp, final double cslmm, final double cclmm, final double clmp,
        final double elmp, final double clmm, final double elmm) {

        this.d = nDoodson;
        this.l = degree;
        this.m = order;
        this.csp = cslmp;
        this.ccp = cclmp;
        this.csm = cslmm;
        this.ccm = cclmm;
        this.cp = clmp;
        this.ep = elmp;
        this.cm = clmm;
        this.em = elmm;
        this.hashcodeNumber = computeCode(nDoodson, degree, order);
    }

    /**
     * @return the doodson number
     */
    public double getDoodson() {
        return this.d;
    }

    /**
     * @return the degree
     */
    public int getDegree() {
        return this.l;
    }

    /**
     * @return the order
     */
    public int getOrder() {
        return this.m;
    }

    /**
     * @return C<sub>lm</sub><sup>+</sup>
     */
    public double getCcp() {
        return this.ccp;
    }

    /**
     * @return C<sub>lm</sub><sup>-</sup>
     */
    public double getCcm() {
        return this.ccm;
    }

    /**
     * @return S<sub>lm</sub><sup>+</sup>
     */
    public double getCsp() {
        return this.csp;
    }

    /**
     * @return S<sub>lm</sub><sup>-</sup>
     */
    public double getCsm() {
        return this.csm;
    }

    /**
     * @return C<sub>lm</sub><sup>+</sup>
     */
    public double getCp() {
        return this.cp;
    }

    /**
     * @return C<sub>lm</sub><sup>-</sup>
     */
    public double getCm() {
        return this.cm;
    }

    /**
     * @return ε<sub>lm</sub><sup>+</sup>
     */
    public double getEp() {
        return this.ep;
    }

    /**
     * @return ε<sub>lm</sub><sup>-</sup>
     */
    public double getEm() {
        return this.em;
    }

    /**
     * Get a hashcode for this set.
     * 
     * @return hashcode
     */
    public double code() {
        return this.hashcodeNumber;
    }

    /**
     * Computes code of data set. Assigns a unique {@link Integer} to a {@link OceanTidesCoefficientsSet} depending
     * on Doodson number, degree and order.
     * 
     * @param doodson
     *        doodson number
     * @param l
     *        degree
     * @param m
     *        order
     * @return code
     */
    public static double computeCode(final double doodson, final int l, final int m) {
        return (doodson * C_1000) * C_100000 + m * C_1000 + l;
    }

}
