/**
 * 
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
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
 * VERSION::FA:178:06/01/2013:log id format corrected
 * VERSION::DM:130:08/10/2013:MSIS2000 model update
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres.MSIS2000;

import java.io.Serializable;

/**
 * 
 * Class Flags
 * Switches: to turn on and off particular variations use these switches.
 * 0 is off, 1 is on, and 2 is main effects off but cross terms on.
 * 
 * Standard values are 0 for switch 0 and 1 for switches 1 to 23. The
 * array "switches" needs to be set accordingly by the calling program.
 * The arrays sw and swc are set internally.
 * 
 * switches[i]:
 * i - explanation
 * -----------------
 * 0 - output in centimeters instead of meters
 * 1 - F10.7 effect on mean
 * 2 - time independent
 * 3 - symmetrical annual
 * 4 - symmetrical semiannual
 * 5 - asymmetrical annual
 * 6 - asymmetrical semiannual
 * 7 - diurnal
 * 8 - semidiurnal
 * 9 - daily ap [when this is set to -1 (!) the pointer
 * ap_a in struct nrlmsise_input must
 * point to a struct ap_array]
 * 10 - all UT/long effects
 * 11 - longitudinal
 * 12 - UT and mixed UT/long
 * 13 - mixed AP/UT/LONG
 * 14 - terdiurnal
 * 15 - departures from diffusive equilibrium
 * 16 - all TINF var
 * 17 - all TLB var
 * 18 - all TN1 var
 * 19 - all S var
 * 20 - all TN2 var
 * 21 - all NLB var
 * 22 - all TN3 var
 * 23 - turbo scale height var
 * 
 * @concurrency not thread-safe
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: Flags.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.2
 * 
 */
public class Flags implements Serializable {

    /** Serial UID. */
    private static final long serialVersionUID = -4710968807924335177L;

    /** Flag size. */
    private static final int FLAGSIZE = 24;
    /** Daily Ap index. */
    private static final int DAILY_AP_INDEX = 9;

    /** Switches. */
    private int[] switches = new int[FLAGSIZE];
    /** sw. */
    private final double[] sw = new double[FLAGSIZE];
    /** swc. */
    private final double[] swc = new double[FLAGSIZE];

    /**
     * Return boolean from double.
     * 
     * @param fakeBool
     *        double
     * @return boolean from double
     * @since 1.0
     */
    public boolean bool(final double fakeBool) {
        return (fakeBool == 0 ? false : true);
    }

    /**
     * Getter for a particular element in the sw array.
     * 
     * @param position
     *        position in the array
     * @return the element at the given position
     */
    public double getSw(final int position) {
        return this.sw[position];
    }

    /**
     * Getter for a particular element in the swc array.
     * 
     * @param position
     *        position in the array
     * @return the element at the given position
     */
    public double getSwc(final int position) {
        return this.swc[position];
    }

    // GETTERS & SETTERS
    /**
     * Getter for switches.
     * 
     * @return the switches
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public int[] getSwitches() {
        return this.switches;
    }

    /**
     * Setter for a specific element of the switches array.
     * 
     * @param position
     *        position in the array
     * @param value
     *        new value.
     */
    public void setSwitches(final int position, final int value) {
        this.switches[position] = value;
    }

    /**
     * Setter for switches.
     * 
     * @param switches
     *        the switches to set
     */
    public void setSwitches(final int[] switches) {
        this.switches = switches;
    }

    /**
     * Prepare sw and swc.
     */
    public void tselec() {
        // loop on the flags size
        for (int i = FLAGSIZE - 1; i >= 0; i--) {
            // if the current index is equal to the Ap index
            if (i == DAILY_AP_INDEX) {
                this.sw[i] = this.switches[i];
                this.swc[i] = this.switches[i];
            } else {
                // test if the switches value for current index is equal to 1
                if (this.switches[i] == 1) {
                    this.sw[i] = 1;
                } else {
                    this.sw[i] = 0;
                }
                // test if the switches value for current index is positve
                if (this.switches[i] > 0) {
                    this.swc[i] = 1;
                } else {
                    this.swc[i] = 0;
                }
            }
        }
    }
}
