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
 * @history 03/07/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:130:08/10/2013:MSIS2000 model update
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres.MSIS2000;

/**
 * /**
 * Class Ap_coef
 * Array containing the following magnetic values:
 * 0 : daily AP
 * 1 : 3 hr AP index for current time
 * 2 : 3 hr AP index for 3 hrs before current time
 * 3 : 3 hr AP index for 6 hrs before current time
 * 4 : 3 hr AP index for 9 hrs before current time
 * 5 : Average of eight 3 hr AP indicies from 12 to 33 hrs
 * prior to current time
 * 6 : Average of eight 3 hr AP indicies from 36 to 57 hrs
 * prior to current time
 * 
 * @concurrency immutable
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: ApCoef.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.2
 * 
 */
public class ApCoef {

    /** AP coefs size. */
    private static final int APCOEFSIZE = 7;

    /** AP coefs. */
    private double[] ap;

    /** Constructor. */
    public ApCoef() {
        this.ap = new double[APCOEFSIZE];
    }

    /**
     * Constructor.
     * 
     * @param inAp
     *        magnetic values
     */
    public ApCoef(final double[] inAp) {
        if (inAp.length == APCOEFSIZE) {
            this.ap = inAp.clone();
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Getter for AP.
     * 
     * @return the ap
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[] getAp() {
        return this.ap;
    }

    /**
     * Setter for AP.
     * 
     * @param ap
     *        the ap to set
     */
    public void setAp(final double[] ap) {
        this.ap = ap;
    }

    /**
     * Setter for a specific element of the AP array.
     * 
     * @param position
     *        position in the array.
     * @param value
     *        new value.
     */
    public void setAp(final int position, final int value) {
        this.ap[position] = value;
    }

}
