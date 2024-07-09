/**
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
 */
/* Copyright 2011-2012 Space Applications Services
 *
 * HISTORY
* VERSION:4.8:DM:DM-2929:15/11/2021:[PATRIUS] Harmonisation des modeles de troposphere 
* VERSION:4.8:FA:FA-2998:15/11/2021:[PATRIUS] Discontinuite methode computeBearing de la classe ProjectionEllipsoidUtils
* VERSION:4.8:FA:FA-2982:15/11/2021:[PATRIUS] Orienter correctement les facettes de la methode toObjFile de GeodeticMeshLoader
* VERSION:4.8:DM:DM-2975:15/11/2021:[PATRIUS] creation du repere synodique via un LOF 
 * VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:88:18/11/2013: the class does not implement TroposphericDelayModel anymore
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation.troposphere;

import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.math.analysis.BivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.interpolation.BicubicSplineInterpolator;
import fr.cnes.sirius.patrius.math.analysis.interpolation.LinearInterpolator;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.models.earth.InterpolationTableLoader;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

//CHECKSTYLE: stop MagicNumber check
//Reason: model - Orekit code

/**
 * The modified Saastamoinen model. Estimates the path delay imposed to
 * electro-magnetic signals by the troposphere according to the formula:
 * 
 * <pre>
 * &delta; = 2.277e-3 / cos z * (P + (1255 / T + 0.05) * e - B * tan<sup>2</sup>
 * z) + &delta;R
 * </pre>
 * 
 * with the following input data provided to the model:
 * <ul>
 * <li>z: zenith angle</li>
 * <li>P: atmospheric pressure</li>
 * <li>T: temperature</li>
 * <li>e: partial pressure of water vapour</li>
 * <li>B, &delta;R: correction terms</li>
 * </ul>
 * <p>
 * The model supports custom &delta;R correction terms to be read from a configuration file
 * (saastamoinen-correction.txt) via the {@link DataProvidersManager}.
 * </p>
 * 
 * <p>Constants used in model are defined as such in the Saastamoinen model article.</p>
 * 
 * @author Thomas Neidhart
 * @see "Guochang Xu, GPS - Theory, Algorithms and Applications, Springer, 2007"
 */
public class SaastamoinenModel implements TroposphericCorrection {

    /** Serial UID. */
    private static final long serialVersionUID = -6104924096062653696L;

    /** m to km. */
    private static final double M_TO_KM = 1000.;

    /** Pa to mBar. */
    private static final double PA_TO_MBAR = 1E-2;

    /** Max model altitude. */
    private static final double MAX_HEIGHT = 5000.;

    /** The temperature at the station [K]. */
    private final double tStation;

    /** The atmospheric pressure at the station [mb]. */
    // Implementation note: kept in milibar to use the constants of the article
    private final double pStation;

    /** The altitude at the station [m]. */
    private final double altitudeStation;

    /** b correction term [mb] */
    // Implementation note: kept in milibar to use the constants of the article
    private final double b;

    /** e the partial pressure of water vapor [mb] */
    // Implementation note: kept in milibar to use the constants of the article
    private final double e;

    /**
     * Create a new Saastamoinen model for the troposphere using the given
     * environmental conditions [T, P, RH] and station altitude.
     * 
     * @param tStation
     *        the temperature at the station [K]
     * @param pStation
     *        the atmospheric pressure at the station [Pa]
     * @param rHStation
     *        the relative humidity at the station [percent] (50% -> 0.5)
     * @param altitude
     *        the altitude above sea level of the station [m]
     */
    public SaastamoinenModel(final double tStation, final double pStation, final double rHStation,
            final double altitude) {
        this.tStation = tStation;
        this.pStation = pStation * PA_TO_MBAR;
        this.altitudeStation = altitude;

        // Interpolate the b correction term which is height-dependent
        b = Functions.INSTANCE.b.value(altitude / M_TO_KM);
        // Calculate e according to Wang et al. (1988) - Equation (5.99)
        e = rHStation * MathLib.exp(Functions.INSTANCE.e.value(this.tStation));
    }

    /**
     * Create a new Saastamoinen model using a standard atmosphere model. 
     * The standard atmosphere model uses the following reference values at mean sea level:
     * <ul>
     * <li>reference temperature: 18 degree Celsius</li>
     * <li>reference pressure: 101325 Pa</li>
     * <li>reference humidity: 50%</li>
     * </ul>
     * 
     * @param altitude
     *        the altitude above the mean sea level of the station [m]
     * @return a Saastamoinen model with standard environmental values
     */
    public static SaastamoinenModel getStandardModel(final double altitude) {
        final double[] standardValues = TroposphericCorrection.computeStandardValues(altitude);
        return new SaastamoinenModel(standardValues[0], standardValues[1], standardValues[2], altitude);
    }

    /**
     * Calculates the tropospheric path delay for the signal path from a ground
     * station to a satellite.
     * 
     * @param elevation
     *        the elevation of the satellite in radians
     * @return the path delay due to the troposphere in m
     */
    public double calculatePathDelay(final double elevation) {

        // Calculate the zenith angle from the elevation
        final double z = MathLib.abs(MathLib.PI / 2. - elevation);

        // Get correction factor dR (bicubic spline interpolation given altitude and zenith angle)
        final double deltaR = this.getDeltaR(altitudeStation, z);

        // Calculate the path delay in m - Equation (5.98)
        final double tan = MathLib.tan(z);
        return 2.277e-3 / MathLib.cos(z) *
                (this.pStation + (1255d / this.tStation + 5e-2) * e - b * tan * tan) + deltaR;
    }

    /**
     * Calculates the tropospheric signal delay for the signal path from a
     * ground station to a satellite. This method exists only for convenience
     * reasons and returns the same as
     * 
     * <pre>
     *   {@link SaastamoinenModel#calculatePathDelay(double)}/
     *   {@link fr.cnes.sirius.patrius.utils.Constants#SPEED_OF_LIGHT}
     * </pre>
     * 
     * @param elevation
     *        the elevation of the satellite in radians
     * @return the signal delay due to the troposphere in s
     */
    @Override
    public double computeSignalDelay(final double elevation) {
        return this.calculatePathDelay(elevation) / Constants.SPEED_OF_LIGHT;
    }

    /**
     * Calculates the delta R correction term using bicubic spline interpolation.
     * 
     * @param height
     *        the height of the station in m
     * @param zenith
     *        the zenith angle of the satellite in radians
     * @return the delta R correction term in m
     */
    private double getDeltaR(final double height, final double zenith) {
        // limit the height to a range of [0, 5000] m
        final double h = MathLib.min(MathLib.max(0, height), MAX_HEIGHT);
        // limit the zenith angle to 90 degree
        // Note: the function is symmetric for negative zenith angles
        final double z = MathLib.min(MathLib.abs(zenith), MathLib.PI / 2.);
        // Get correction factor dR (bicubic spline interpolation given altitude and zenith angle)
        return Functions.INSTANCE.deltaR.value(h, z);
    }

    /**
     * Contains several functions used by the Saastamoinen model to calculate
     * the path delay. The functions are static and thus accessed via a static
     * instance of this class. The &delta;R correction terms can be optionally
     * loaded from a configuration file, otherwise default values are used.
     */
    private static final class Functions {

        /** The singleton instance containing the functions. */
        private static final Functions INSTANCE = new Functions();

        /** Linear interpolation function for the B correction term. */
        private final UnivariateFunction b;

        /** Polynomial function for the e term. */
        private final PolynomialFunction e;

        /** Bicubic spline interpolation function for the delta R correction term. */
        private final BivariateFunction deltaR;

        /** Initialize the functions. */
        @SuppressWarnings("PMD.EmptyCatchBlock")
        private Functions() {
            // Values from Table 5.1
            final double[] xValForB = { 0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0 };
            final double[] yValForB = { 1.156, 1.079, 1.006, 0.938, 0.874, 0.813, 0.757, 0.654, 0.563 };

            this.b = new LinearInterpolator().interpolate(xValForB, yValForB);

            // a function to estimate the partial pressure of water vapour
            this.e = new PolynomialFunction(new double[] { -37.2465, 0.213166, -0.000256908 });

            // read the delta R interpolation function from the config file
            final InterpolationTableLoader loader = new InterpolationTableLoader();
            BivariateFunction func = null;
            try {
                DataProvidersManager.getInstance().feed("^saastamoinen-correction\\.txt$", loader);
                if (!loader.stillAcceptsData()) {
                    func = new BicubicSplineInterpolator().interpolate(loader.getAbscissaGrid(),
                            loader.getOrdinateGrid(),
                            loader.getValuesSamples());
                }
            } catch (final PatriusException ex) {
                // config file could not be loaded, use the default values instead
            }

            if (func == null) {
                // use default values if the file could not be read

                // the correction table in the referenced book only contains values for an angle of 60 - 80
                // degree, thus for 0 degree, the correction term is assumed to be 0, for degrees > 80 it
                // is assumed to be the same value as for 80.

                // Values from Table 5.2
                // the height in m
                final double[] xValForR = { 0, 500, 1000, 1500, 2000, 3000, 4000, 5000 };
                // the zenith angle in radians
                final double[] yValForRDeg = { 0.0, 60.0, 66.0, 70.0, 73.0, 75.0, 76.0, 77.0,
                    78.0, 78.50, 79.0, 79.50, 79.75, 80.0, 90.0 };
                final double[] yValForR = new double[yValForRDeg.length];
                for (int i = 0; i < yValForR.length; i++) {
                    yValForR[i] = MathLib.toRadians(yValForRDeg[i]);
                }

                final double[][] fval = new double[][] {
                        { 0.000, 0.003, 0.006, 0.012, 0.020, 0.031, 0.039, 0.050, 0.065, 0.075, 0.087, 0.102, 0.111,
                            0.121, 0.121 },
                        { 0.000, 0.003, 0.006, 0.011, 0.018, 0.028, 0.035, 0.045, 0.059, 0.068, 0.079, 0.093, 0.101,
                            0.110, 0.110 },
                        { 0.000, 0.002, 0.005, 0.010, 0.017, 0.025, 0.032, 0.041, 0.054, 0.062, 0.072, 0.085, 0.092,
                            0.100, 0.100 },
                        { 0.000, 0.002, 0.005, 0.009, 0.015, 0.023, 0.029, 0.037, 0.049, 0.056, 0.065, 0.077, 0.083,
                            0.091, 0.091 },
                        { 0.000, 0.002, 0.004, 0.008, 0.013, 0.021, 0.026, 0.033, 0.044, 0.051, 0.059, 0.070, 0.076,
                            0.083, 0.083 },
                        { 0.000, 0.002, 0.003, 0.006, 0.011, 0.017, 0.021, 0.027, 0.036, 0.042, 0.049, 0.058, 0.063,
                            0.068, 0.068 },
                        { 0.000, 0.001, 0.003, 0.005, 0.009, 0.014, 0.017, 0.022, 0.030, 0.034, 0.040, 0.047, 0.052,
                            0.056, 0.056 },
                        { 0.000, 0.001, 0.002, 0.004, 0.007, 0.011, 0.014, 0.018, 0.024, 0.028, 0.033, 0.039, 0.043,
                            0.047, 0.047 } };

                // the actual delta R is interpolated using a a bi-cubic spline interpolator
                this.deltaR = new BicubicSplineInterpolator().interpolate(xValForR, yValForR, fval);
            } else {
                this.deltaR = func;
            }
        }
    }

    // CHECKSTYLE: resume MagicNumber check
}
