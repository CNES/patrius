/**
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
 * @history created 05/08/2016
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:606:05/08/2016:extended atmosphere data
 * VERSION::DM:1175:29/06/2017:add validation test aero vs global aero
 * VERSION::FA:1275:30/08/2017:correct partial density computation
 * VERSION::FA:1486:18/05/2018:modify the hydrogen mass unity to USI and add precision
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.forces.atmospheres;

import java.io.Serializable;

/**
 * Container for extended atmosphere data.
 * <p>
 * Some atmosphere model do not provide all information. The list of available information is detailed for each
 * atmosphere model.
 * </p>
 * 
 * @author Emmanuel Bignon
 * @since 3.3
 * @version $Id: AtmosphereData.java 18079 2017-10-02 16:52:15Z bignon $
 */
public class AtmosphereData implements Serializable {

    /** Hydrogen atomic mass. */
    public static final double HYDROGEN_MASS = 1.660538921E-27;

    /** Serial UID. */
    private static final long serialVersionUID = -880609450888259017L;

    /** Relative atomic mass He, O, N2, O2, Ar, H, N. */
    private static final double[] ATOMIC_MASS = { 4.0, 16.0, 28.0, 32.0, 40.0, 1.0, 14.0 };

    /** Total density. */
    private final double density;

    /** Local temperature. */
    private final double localTemperature;

    /** Exospheric temperature. */
    private final double exosphericTemperature;

    /** Mean atomic mass (in unit of hydrogen mass). */
    private final double meanAtomicMass;

    /** Helium density. */
    private final double densityHe;

    /** Oxygen density. */
    private final double densityO;

    /** Dinitrogen density. */
    private final double densityN2;

    /** Dioxygen density. */
    private final double densityO2;

    /** Argon density. */
    private final double densityAr;

    /** Hydrogen density. */
    private final double densityH;

    /** Nitrogen density. */
    private final double densityN;

    /** Anomalous oxygen density. */
    private final double densityAnomalousOxygen;

    /**
     * Constructor.
     * 
     * @param densityIn total density
     * @param localTemperatureIn local temperature
     * @param exosphericTemperatureIn exospheric temperature
     * @param nHe Helium number of particules per m3
     * @param nO Oxygen number of particules per m3
     * @param nN2 dinitrogen number of particules per m3
     * @param nO2 Dioxygen number of particules per m3
     * @param nAr Argon number of particules per m3
     * @param nH hydrogen number of particules per m3
     * @param nN nitrogen number of particules per m3
     * @param nAnomalousOxygen anomalous oxygen density
     */
    public AtmosphereData(final double densityIn, final double localTemperatureIn,
        final double exosphericTemperatureIn, final double nHe, final double nO,
        final double nN2, final double nO2, final double nAr, final double nH, final double nN,
        final double nAnomalousOxygen) {

        this.density = densityIn;
        this.localTemperature = localTemperatureIn;
        this.exosphericTemperature = exosphericTemperatureIn;

        // Partial densities
        final double sumDensities = nHe + nO + nN2 + nO2 + nAr + nH + nN + nAnomalousOxygen;
        this.densityHe = densityIn * nHe / sumDensities;
        this.densityO = densityIn * nO / sumDensities;
        this.densityN2 = densityIn * nN2 / sumDensities;
        this.densityO2 = densityIn * nO2 / sumDensities;
        this.densityAr = densityIn * nAr / sumDensities;
        this.densityH = densityIn * nH / sumDensities;
        this.densityN = densityIn * nN / sumDensities;
        this.densityAnomalousOxygen = densityIn * nAnomalousOxygen / sumDensities;

        this.meanAtomicMass = (nHe * ATOMIC_MASS[0] + nO * ATOMIC_MASS[1] + nN2 * ATOMIC_MASS[2]
            + nO2 * ATOMIC_MASS[3] + nAr * ATOMIC_MASS[4] + nH * ATOMIC_MASS[5] + nN
            * ATOMIC_MASS[6] + nAnomalousOxygen * ATOMIC_MASS[1])
            / sumDensities;
    }

    /**
     * Returns the total density.
     * 
     * @return the total density
     */
    public double getDensity() {
        return this.density;
    }

    /**
     * Returns the local temperature.
     * 
     * @return the local temperature
     */
    public double getLocalTemperature() {
        return this.localTemperature;
    }

    /**
     * Returns the exospheric temperature.
     * 
     * @return the exospheric temperature
     */
    public double getExosphericTemperature() {
        return this.exosphericTemperature;
    }

    /**
     * Returns the mean atomic mass or the molar mass.
     * 
     * @return the mean atomic mass (in unit of hydrogen mass) or the molar mass (in kg). To get the
     *         mean atomic mass in kg, multiply it with {@link #HYDROGEN_MASS}
     */
    public double getMeanAtomicMass() {
        return this.meanAtomicMass;
    }

    /**
     * Return the Helium density.
     * 
     * @return the Helium density
     */
    public double getDensityHe() {
        return this.densityHe;
    }

    /**
     * Returns the Oxygen density.
     * 
     * @return the Oxygen density
     */
    public double getDensityO() {
        return this.densityO;
    }

    /**
     * Returns the dinitrogen density.
     * 
     * @return the dinitrogen density
     */
    public double getDensityN2() {
        return this.densityN2;
    }

    /**
     * Returns the dioxygen density.
     * 
     * @return the dioxygen density
     */
    public double getDensityO2() {
        return this.densityO2;
    }

    /**
     * Returns the Argon density.
     * 
     * @return the Argon density
     */
    public double getDensityAr() {
        return this.densityAr;
    }

    /**
     * Returns the hydrogen density.
     * 
     * @return the hydrogen density
     */
    public double getDensityH() {
        return this.densityH;
    }

    /**
     * Returns the nitrogen density.
     * 
     * @return the nitrogen density
     */
    public double getDensityN() {
        return this.densityN;
    }

    /**
     * Returns the anomalous oxygen density.
     * 
     * @return the anomalous oxygen density
     */
    public double getDensityAnomalousOxygen() {
        return this.densityAnomalousOxygen;
    }
}
