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
package fr.cnes.sirius.patrius.frames.configuration.eop;

import java.io.Serializable;

/**
 * Simple container class for nutation correction (IAU 1980) parameters.
 * <p>
 * This class is a simple container, it does not provide any processing method.
 * </p>
 * 
 * @author Pascal Parraud
 */
public class NutationCorrection implements Serializable {

    /** Null correction (ddeps = 0, ddpsi = 0). */
    public static final NutationCorrection NULL_CORRECTION =
        new NutationCorrection(0, 0);

    /** Serializable UID. */
    private static final long serialVersionUID = -2075750534145826411L;

    /** &delta;&Delta;&epsilon;<sub>1980</sub> parameter (radians) or &delta;X<sub>2000</sub> parameter (radians). */
    private final double dy;

    /** &delta;&Delta;&psi;<sub>1980</sub> parameter (radians) or &delta;Y<sub>2000</sub> parameter (radians). */
    private final double dx;

    /**
     * Simple constructor.
     * 
     * @param dxIn
     *        &delta;&Delta;&psi;<sub>1980</sub> parameter (radians) or &delta;X<sub>2000</sub> parameter (radians)
     * @param dyIn
     *        &delta;&Delta;&epsilon;<sub>1980</sub> parameter (radians) or &delta;Y<sub>2000</sub> parameter
     *        (radians)
     */
    public NutationCorrection(final double dxIn, final double dyIn) {
        this.dy = dyIn;
        this.dx = dxIn;
    }

    /**
     * Get the &delta;&Delta;&epsilon;<sub>1980</sub> parameter.
     * 
     * @return &delta;&Delta;&epsilon;<sub>1980</sub> parameter
     */
    public double getDdeps() {
        return this.dy;
    }

    /**
     * Get the &delta;&Delta;&psi;<sub>1980</sub> parameter.
     * 
     * @return &delta;&Delta;&psi;<sub>1980</sub> parameter
     */
    public double getDdpsi() {
        return this.dx;
    }

    /**
     * Get the &delta;Y<sub>2000</sub> parameter (radians).
     * 
     * @return &delta;Y<sub>2000</sub> parameter (radians)
     */
    public double getDY() {
        return this.dy;
    }

    /**
     * Get the &delta;X<sub>2000</sub> parameter (radians).
     * 
     * @return &delta;X<sub>2000</sub> parameter (radians)
     */
    public double getDX() {
        return this.dx;
    }
}