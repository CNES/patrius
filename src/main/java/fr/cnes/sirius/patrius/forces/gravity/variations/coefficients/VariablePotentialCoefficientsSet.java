/**
 * 
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
 * 
 * @history Created 07/11/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.variations.coefficients;

import java.io.Serializable;

/**
 * Represents a variable potential coefficients set for a given degree and order
 * 
 * @concurrency immutable
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: VariablePotentialCoefficientsSet.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.3
 */
public class VariablePotentialCoefficientsSet implements Serializable {

     /** Serializable UID. */
    private static final long serialVersionUID = 3662552678425158131L;

    /** Degree */
    private final int degree;

    /** Order */
    private final int order;

    /** C */
    private final double coefC;

    /** S */
    private final double coefS;

    /** C corrections */
    private final double[] corC;

    /** S corrections */
    private final double[] corS;

    /**
     * Create a set for a given order and degree
     * 
     * @param d
     *        degree of set
     * @param o
     *        order of set
     * @param c
     *        normalized c coefficient
     * @param s
     *        normalized s coefficient
     * @param cc
     *        c coefficient corrections {DOT, S1A, C1A, S2A, C2A}
     * @param sc
     *        s coefficient corrections {DOT, S1A, C1A, S2A, C2A}
     * 
     */
    public VariablePotentialCoefficientsSet(final int d, final int o, final double c, final double s,
        final double[] cc, final double[] sc) {

        checkSanity(d, o, cc, sc);

        this.degree = d;
        this.order = o;
        this.coefC = c;
        this.coefS = s;
        this.corC = cc.clone();
        this.corS = sc.clone();
    }

    /**
     * Check sanity of input arguments
     * 
     * @param d
     *        degree
     * @param o
     *        order
     * @param cc
     *        c corrections
     * @param sc
     *        s corrections
     */
    private static void checkSanity(final int d, final int o, final double[] cc, final double[] sc) {
        if (d < 0 || o < 0 || cc.length != 5 || sc.length != 5) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * @return the degree of the set
     */
    public int getDegree() {
        return this.degree;
    }

    /**
     * @return the order of the set
     */
    public int getOrder() {
        return this.order;
    }

    /**
     * @return normalized c coefficient of the set
     */
    public double getC() {
        return this.coefC;
    }

    /**
     * @return normalized s coefficient of the set
     */
    public double getS() {
        return this.coefS;
    }

    /**
     * @return the c coefficient corrections of the set {DOT, S1A, C1A, S2A, C2A}
     */
    public double[] getCc() {
        return this.corC.clone();
    }

    /**
     * @return the s coefficient corrections of the set {DOT, S1A, C1A, S2A, C2A}
     */
    public double[] getSc() {
        return this.corS.clone();
    }

}
