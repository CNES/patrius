/**
 * 
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
 * 
 * @history 03/07/2012
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:The format of the log id has been corrected
 * VERSION::DM:130:08/10/2013:MSIS2000 model update
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres.MSIS2000;

import java.io.Serializable;

/**
 * Class Output
 * OUTPUT VARIABLES:
 * d[0] - HE NUMBER DENSITY(CM-3)
 * d[1] - O NUMBER DENSITY(CM-3)
 * d[2] - N2 NUMBER DENSITY(CM-3)
 * d[3] - O2 NUMBER DENSITY(CM-3)
 * d[4] - AR NUMBER DENSITY(CM-3)
 * d[5] - TOTAL MASS DENSITY(GM/CM3) [includes d[8] in td7d]
 * d[6] - H NUMBER DENSITY(CM-3)
 * d[7] - N NUMBER DENSITY(CM-3)
 * d[8] - Anomalous oxygen NUMBER DENSITY(CM-3)
 * t[0] - EXOSPHERIC TEMPERATURE
 * t[1] - TEMPERATURE AT ALT
 * 
 * 
 * O, H, and N are set to zero below 72.5 km
 * 
 * t[0], Exospheric temperature, is set to global average for
 * altitudes below 120 km. The 120 km gradient is left at global
 * average value for altitudes below 72 km.
 * 
 * d[5], TOTAL MASS DENSITY, is NOT the same for subroutines GTD7
 * and GTD7D
 * 
 * SUBROUTINE GTD7 -- d[5] is the sum of the mass densities of the
 * species labeled by indices 0-4 and 6-7 in output variable d.
 * This includes He, O, N2, O2, Ar, H, and N but does NOT include
 * anomalous oxygen (species index 8).
 * 
 * SUBROUTINE GTD7D -- d[5] is the "effective total mass density
 * for drag" and is the sum of the mass densities of all species
 * in this model, INCLUDING anomalous oxygen.
 * 
 * @concurrency not thread-safe
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: Output.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.2
 * 
 */
public final class Output implements Serializable {

    /** Serial UID. */
    private static final long serialVersionUID = -3148310297236715788L;

    /** Density size. */
    private static final int DENSITY_SIZE = 9;
    /** Temperature size. */
    private static final int TEMPERATURE_SIZE = 2;

    /** Densities. */
    private final double[] d = new double[DENSITY_SIZE];
    /** Temperatures. */
    private final Double[] t = new Double[TEMPERATURE_SIZE];

    /** Constructor. */
    public Output() {
        java.util.Arrays.fill(this.d, 0.0);
        java.util.Arrays.fill(this.t, 0.0);
    }

    // ================================= GETTERS =================================

    /**
     * Getter for density (d).
     * 
     * @return the d
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[] getD() {
        return this.d;
    }

    /**
     * Getter for density component.
     * 
     * @param i
     *        index
     * @return the ith density component
     */
    public double getD(final int i) {
        return this.d[i];
    }

    /**
     * Getter for temperature (t).
     * 
     * @return the t
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public Double[] getT() {
        return this.t;
    }

    /**
     * Getter for temperature component.
     * 
     * @param i
     *        index
     * @return the ith temperature component
     */
    public Double getT(final int i) {
        return this.t[i];
    }

    // ================================= SETTERS =================================

    /**
     * Setter for ith density component.
     * 
     * @param i
     *        index
     * @param value
     *        value to set
     */
    public void setD(final int i, final double value) {
        this.d[i] = value;
    }

}
