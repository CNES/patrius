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
 * @history created 23/05/12
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2929:15/11/2021:[PATRIUS] Harmonisation des modeles de troposphere 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:88:18/11/2013: refactoring and renaming of the class
 * VERSION::FA:183:17/03/2014: javadoc correction
 * VERSION::FA:186:03/04/2014:bad test on the temperature
 * VERSION::FA:255:13/10/2014:header correction
 * VERSION::FA:261:13/10/2014:JE V2.2 corrections (merge terms theoretical=measured=real=true, and geometric  )
 * VERSION::FA:273:20/10/2014:Minor code problems
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation.troposphere;

import java.util.ArrayList;

import fr.cnes.sirius.patrius.math.exception.ConvergenceException;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.signalpropagation.AngularCorrection;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class is a tropospheric correction model that implements the TroposphericCorrection
 * and AngularCorrection interfaces to correct a signal with the Azoulay model.
 * 
 * @concurrency immutable
 * 
 * @see TroposphericCorrection
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public final class AzoulayModel implements TroposphericCorrection, AngularCorrection {

    /** Serializable UID. */
    private static final long serialVersionUID = 4794751982189820026L;

    /** epsilon */
    private static final double EPS = 0.0017;
    /** to kelvin degrees transformation */
    private static final double TK = 273.16;
    /** coeff 1 for Goff and Gratch formula */
    private static final double C1 = 10.79574;
    /** coeff 2 for Goff and Gratch formula */
    private static final double C2 = 5.028;
    /** coeff 3 for Goff and Gratch formula */
    private static final double C3 = 1.50475e-4;
    /** coeff 4 for Goff and Gratch formula */
    private static final double C4 = -8.2969;
    /** coeff 5 for Goff and Gratch formula */
    private static final double C5 = 0.42873e-3;
    /** coeff 6 for Goff and Gratch formula */
    private static final double C6 = 4.76955;
    /** coeff 7 for Goff and Gratch formula */
    private static final double C7 = 0.78614;
    /** 100.0 */
    private static final double HUNDRED = 100.0;
    /** 1.0e-3 */
    private static final double EMINUS3 = 1.0e-3;
    /** 1.0e-6 */
    private static final double EMINUS6 = 1.0e-6;
    /** coeff 77.6 */
    private static final double C8 = 77.6;
    /** coeff 4810.0 */
    private static final double C9 = 4810.0;
    /** coeff 370.0 */
    private static final double C10 = 370.0;
    /** coeff 1.437e+3 */
    private static final double C11 = 1.437e+3;

    /** convergence epsilon distance */
    private static final double EPSDIST = 1.0;
    /** convergence epsilon elevation */
    private static final double EPSELEV = 1.0e-4;
    /** max iterations */
    private static final int MAXITER = 50;

    /** the ground atmospheric pressure */
    private final double inPressure;
    /** the ground atmospheric temperature */
    private final double tK;
    /** the ground atmospheric moisture */
    private final double inMoisture;
    /** the ground point geodetic altitude */
    private final double inAltitude;
    /**
     * True if the correction due to the troposphere is computed from the geometric value of the elevation,
     * to get the apparent elevation value; false if the correction is computed from the apparent value
     * of the elevation (the measure), to get the geometric value.
     */
    private final boolean inIsGeometricElevation;

    /**
     * @param pressure
     *        the ground atmospheric pressure [Pa]
     * @param temperature
     *        the ground atmospheric temperature [K]
     * @param moisture
     *        the ground atmospheric moisture [percent]
     * @param geodeticAltitude
     *        the ground point geodetic altitude [m]
     */
    public AzoulayModel(final double pressure, final double temperature,
        final double moisture, final double geodeticAltitude) {
        this(pressure, temperature, moisture, geodeticAltitude, true);
    }

    /**
     * @param pressure
     *        the ground atmospheric pressure [Pa]
     * @param temperature
     *        the ground atmospheric temperature [K]
     * @param moisture
     *        the ground atmospheric moisture [percent]
     * @param geodeticAltitude
     *        the ground point geodetic altitude [m]
     * @param isGeometricElevation
     *        true if the computed correction is used to get the apparent elevation from
     *        the geometric elevation value, false if it is used to get the geometric elevation from
     *        the apparent elevation value
     */
    public AzoulayModel(final double pressure, final double temperature,
        final double moisture, final double geodeticAltitude, final boolean isGeometricElevation) {
        if (temperature < Precision.DOUBLE_COMPARISON_EPSILON) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_NULL_TEMPERATURE);
        }
        this.inPressure = pressure / HUNDRED;
        this.tK = temperature;
        this.inMoisture = moisture;
        this.inAltitude = geodeticAltitude;
        this.inIsGeometricElevation = isGeometricElevation;
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsParameter(final Parameter param) {
        return false; // No supported parameter yet
    }

    /** {@inheritDoc} */
    @Override
    public ArrayList<Parameter> getParameters() {
        return new ArrayList<>(); // No supported parameter yet
    }

    /** {@inheritDoc} */
    @Override
    public double derivativeValue(final Parameter p, final double elevation) {
        return 0.; // No supported parameter yet
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDifferentiableBy(final Parameter p) {
        return supportsParameter(p); // No supported parameter yet
    }

    /**
     * Computes the corrections due to the troposphere from the geometric value
     * of the elevation.
     * 
     * @param geometricElevation
     *        the geometric elevation [rad]
     * @return an array that contains both corrections :
     *         {(geometric elevation - apparent elevation), (geometric distance - apparent distance)}
     */
    public double[] getCorrectionsFromGeometricElevation(final double geometricElevation) {

        // initialisations
        double distMes = 0.0;
        double elevMes = geometricElevation;
        boolean condition = true;
        int failCheck = 0;
        double[] corrections = { 0.0, 0.0 };

        // iterations
        while (condition) {

            // Correction
            corrections = this.getCorrectionsFromApparentElevation(elevMes);
            final double newElev = geometricElevation + corrections[0];

            condition = ((MathLib.abs(newElev - elevMes) > EPSELEV)
                || (MathLib.abs(corrections[1] - distMes) > EPSDIST));

            if (condition) {
                elevMes = newElev;
                distMes = corrections[1];
            }

            // check of the max iterations number
            if (failCheck > MAXITER) {
                // Too many iterations
                throw new ConvergenceException();
            }
            failCheck++;
        }

        // Return correction
        return corrections;
    }

    /**
     * Computes the corrections due to the troposphere from the apparent value
     * of the elevation.
     * 
     * @param apparentElevation
     *        the apparent elevation (rad)
     * @return an array that contains both corrections :
     *         {(geometric elevation - apparent elevation), (geometric distance - apparent distance)}
     */
    public double[] getCorrectionsFromApparentElevation(final double apparentElevation) {

        // kelvin temperature
        final double t = this.tK;

        // Goff and Gratch formula
        final double xlew = C1 * (1.0 - MathLib.divide(TK, t)) - C2 * MathLib.log10(t / TK)
            + C3 * (1.0 - MathLib.pow(10.0, C4 * (t / TK - 1.0)))
            + C5 * (MathLib.pow(10.0, C6 * (1.0 - MathLib.divide(TK, t))) - 1.0)
            + C7;
        final double ew = MathLib.exp(MathLib.log(10.0) * xlew);

        // water steam partial pressure
        final double pve = ew * this.inMoisture / HUNDRED;

        // co-index at measure point altitude
        final double h0 = this.inAltitude * EMINUS3;
        final double xnh0 = MathLib.divide(C8 * (this.inPressure + MathLib.divide(C9 * pve, t)), t);

        // vertical distance error
        double delta0 = 0.0;
        if (xnh0 >= C10) {
            delta0 = 1.0;
        }
        final double fnh0 = 3.0 * xnh0 + C11 + delta0 * (xnh0 - C10) * (xnh0 - C10) / 10.0;
        final double dlv = fnh0 - h0 * xnh0 / 2.0;

        // distance and elevation correction
        // "denom" can't be zero
        final double[] sincos = MathLib.sinAndCos(apparentElevation);
        final double sinEl = sincos[0];
        final double cosEl = sincos[1];
        final double denom = MathLib.sqrt(sinEl * sinEl + EPS * cosEl * cosEl);

        return new double[] { MathLib.divide(xnh0 * EMINUS6 * (1.0 - EPS / 2.) * cosEl, denom),
            MathLib.divide(dlv * EMINUS6, (denom * EMINUS3)) };
    }

    /** {@inheritDoc} */
    @Override
    public double computeElevationCorrection(final double elevation) {
        if (this.inIsGeometricElevation) {
            return this.getCorrectionsFromGeometricElevation(elevation)[0];
        }
        return this.getCorrectionsFromApparentElevation(elevation)[0];
    }

    /** {@inheritDoc} */
    @Override
    public double computeSignalDelay(final double elevation) {
        if (this.inIsGeometricElevation) {
            return this.getCorrectionsFromGeometricElevation(elevation)[1]
                    / Constants.SPEED_OF_LIGHT;
        }
        return this.getCorrectionsFromApparentElevation(elevation)[1] / Constants.SPEED_OF_LIGHT;
    }
}
