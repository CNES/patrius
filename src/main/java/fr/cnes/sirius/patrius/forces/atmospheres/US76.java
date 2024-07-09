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
 * @history Created 15/10/2014
 *
 * HISTORY
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
* VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:267:24/10/2014: US76 atmosphere model added
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::FA:406:20/02/2015:Checkstyle corrections (nb cyclomatic)
 * VERSION::FA:378:14/04/2015:Problems with the implementation of US76 atmosphere model
 * VERSION::DM:268:30/04/2015:drag and lift implementation
 * VERSION::DM:515:02/02/2016:Add getters for temperature and pressure
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres;

import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This class implements the US76 atmospheric model.
 * </p>
 * 
 * @concurrency thread-hostile
 * @concurrency.comment The direct use of thread hostile objects makes this class thread hostile itself.
 * 
 * @author Francois Toussaint
 * @version $Id: US76.java 15077 2016-01-20 10:47:09Z chabaud $
 * @since 2.3
 * 
 */
@SuppressWarnings({"PMD.ShortClassName", "PMD.NullAssignment"})
public class US76 implements Atmosphere {

    /** Gravity at sea level. */
    private static final double GRAVITY_45 = 9.80665;

    /** Layer altitude */
    private static final int DELTA_LAYER = 500;

    /** Layer number */
    private static final int LAYER_NBR = 11;

    /** Adiabatic constant */
    private static final double GAMMA = 1.4;

    // Layer altitudes
    /** Layer altitude for 80 km. */
    private static final double LAYER_80K = 80000;
    /** Layer altitude for 86 km. */
    private static final double LAYER_86K = 86000;
    /** Layer altitude for 91 km. */
    private static final double LAYER_91K = 91000;
    /** Layer altitude for 110 km. */
    private static final double LAYER_110K = 110000;
    /** Layer altitude for 120 km. */
    private static final double LAYER_120K = 120000;

    /** Layer delta for temperatures. */
    private static final double DT_110K = 0.012;

    // Layer temperatures
    /** Layer temperature for 86 km. */
    private static final double TEMP_86K = 186.946;
    /** Layer temperature for 91 km. */
    private static final double TEMP_91K = 186.8673;
    /** Layer temperature for 110 km. */
    private static final double TEMP_110K = 240;
    /** Layer temperature for 120 km. */
    private static final double TEMP_120K = 360;
    /** Layer temperature for 1000 km. */
    private static final double TEMP_1000K = 1000;

    /** Pressure at 86 Km. */
    private static final double P_86K = 3.7338e-01;

    /** serial version UID. */
    private static final long serialVersionUID = -4112665369108873996L;

    /** Layers height. */
    private static final double[] LAYERH = { 0, 11000, 20000, 32000, 47000, 51000, 71000, 84852 };

    /** Delta temperatures from layers. */
    private static final double[] LAYERDT = { -6.5e-3, 0, 1.e-3, 2.8e-3, 0., -2.8e-3, -2.0e-3, 0., 0., 12.e-3, 0., 0.,
        0. };

    /** Layers temperature. */
    private static final double[] LAYERT = { 288.15, 216.65, 216.65, 228.65, 270.65, 270.65,
        214.65, TEMP_86K };

    /** Molar masses of N2,O,O2,Ar,HE et H. */
    private static final double[] LAYERM = { 0, 28.0134, 15.9994, 31.9988, 39.948, 4.0026, 1.0080 };

    /** Weightings for the calculation of temperature between 80 and 86 km. */
    private static final double[] LAYERCT = { 1, 0.999996, 0.999989, 0.999971, 0.999941, 0.999909,
        0.999870, 0.999829, 0.999786, 0.999741, 0.999694, 0.999641, 0.999579 };

    /** Layers pressure. */
    private static final double[] LAYERP = { 1.01325e+05, 2.2632e+04, 5.4748e+03, 8.6801e+02,
        1.1090e+02, 6.6938e+01, 3.9564, P_86K };

    /** Earth equatorial radius from US standard atmosphere */
    private static final double AE = 6356766;

    /** Precision to test temperature's gradient in pressure computation */
    private static final double EPS = 1e-6;

    /** Molecular mass of atmosphere at ground level */
    private static final double XMOL0 = 28.9644;

    /** Perfect gas constant (N.m/(kmol.K)) */
    private static final double RSTAR = 8314.32;

    /** Constant value used for temperature computation */
    private static final double RLAMBDA = 1.875e-5;

    /** Boltzmann's constant (N.m/K) */
    private static final double BOLTZ = 1.380622e-23;

    /** Ratio value */
    private static final double RATIO = GRAVITY_45 * XMOL0 / RSTAR;
    /**
     * Earth Shape
     */
    private final BodyShape earth;

    /** Total molar mass */
    private double xmol;

    /** Cache mecanism - Output pressure. */
    private double cachedPres;

    /** Cache mecanism - Output density. */
    private double cachedDensity;

    /** Cache mecanism - Output temperature. */
    private double cachedTemp;

    /** Cache mecanism - Input date. */
    private AbsoluteDate cachedDate;

    /** Cache mecanism - Input frame. */
    private Frame cachedFrame;

    /** Cache mecanism - Input position. */
    private Vector3D cachedPosition;

    /**
     * Simple constructor for class US76
     * 
     * @param earthBody
     *        earth body
     */
    public US76(final BodyShape earthBody) {

        this.earth = earthBody;
        this.cachedDate = AbsoluteDate.PAST_INFINITY;
        this.cachedFrame = null;
        this.cachedPosition = Vector3D.ZERO;
        this.cachedTemp = Double.NaN;
        this.cachedPres = Double.NaN;
        this.cachedDensity = Double.NaN;

    }

    /**
     * Compute the index of the atmosphere model layer involved
     * 
     * @param alt
     *        altitude
     * @return the index of the atmosphere model layer involved
     */
    private static int getAtmoIndex(final double alt) {
        int ret = 0;
        if (alt >= US76Data.LAYERE[0][0] && alt <= US76Data.LAYERE[US76Data.LAYERE.length - 1][0]) {

            int k = 0;
            while (alt > US76Data.LAYERE[k][0] && k < US76Data.LAYERE.length) {
                ret = k;
                k++;
            }
        }
        return ret;

    }

    /**
     * <p>
     * Get the local temperature for altitude in interval [0, 1E6] m
     * </p>
     * <p>
     * Note: if altitude < 0 m or altitude > 1E6 m the temperature corresponding to the closest bound is returned.
     * </p>
     * 
     * @param date
     *        date to compute temperature, pression, and density
     * @param position
     *        position vector
     * @param frame
     *        frame where the position vector is expressed
     * @return Cached Temperature
     * @throws PatriusException
     *         OrekitException if point cannot be converted to body frame
     */
    public double getTemp(final AbsoluteDate date,
                          final Vector3D position, final Frame frame) throws PatriusException {
        this.computeTempPressDensity(date, position, frame);
        return this.cachedTemp;
    }

    /**
     * <p>
     * Get the local pressure for altitude in interval [0, 1E6] m
     * </p>
     * <p>
     * Note: if altitude < 0 m or altitude > 1E6 m the pressure corresponding to the closest bound is returned.
     * </p>
     * 
     * @param date
     *        date to compute temperature, pression, and density
     * @param position
     *        position vector
     * @param frame
     *        frame where the position vector is expressed
     * @return Cached Temperature
     * @throws PatriusException
     *         OrekitException if point cannot be converted to body frame
     */
    public double getPress(final AbsoluteDate date,
                           final Vector3D position, final Frame frame) throws PatriusException {
        this.computeTempPressDensity(date, position, frame);
        return this.cachedPres;
    }

    /**
     * <p>
     * Get the local density for altitude in interval [0, 1E6] m
     * </p>
     * <p>
     * Note: if altitude < 0 m or altitude > 1E6 m the density corresponding to the closest bound is returned.
     * </p>
     * 
     * @param date
     *        date
     * @param position
     *        position vector
     * @param frame
     *        frame where the position vector is expressed
     * 
     * @return density value (kg / m<sup>3</sup>)
     * @throws PatriusException
     *         OrekitException if point cannot be converted to body frame
     */
    @Override
    public double getDensity(final AbsoluteDate date, final Vector3D position,
                             final Frame frame) throws PatriusException {
        this.computeTempPressDensity(date, position, frame);
        return this.cachedDensity;
    }

    /**
     * If input parameters are different from cached parameters, re compute cached
     * pression, temperature, and density.
     * 
     * @param date
     *        date to compute temperature, pression, and density
     * @param position
     *        position vector
     * @param frame
     *        frame where the position vector is expressed
     * @throws PatriusException
     *         OrekitException if point cannot be converted to body frame
     */
    private void computeTempPressDensity(final AbsoluteDate date,
                                         final Vector3D position, final Frame frame) throws PatriusException {
        if (this.cachedDate.compareTo(date) != 0 || position.distance(this.cachedPosition) != 0 ||
            !frame.equals(this.cachedFrame)) {

            // Altitude max
            final double altMax = 1.0E6;

            // Geodetic altitude
            final GeodeticPoint point = this.earth.transform(position, frame, date);
            final double z = point.getAltitude();

            // Altitude lower than 86km
            if (z <= LAYER_86K) {

                // If negative altitude : return density at Z = 0 km
                this.computeLower86(MathLib.max(z, 0));

            } else {

                // If altitude > 1000.0 km : return density at Z = 1000 km
                this.computeUpper86(MathLib.min(z, altMax));
            }

            // Density
            final double ro = (this.cachedPres * this.xmol) / (RSTAR * this.cachedTemp);
            this.cachedDensity = ro;

            // store input params used to compute these results in cache
            this.cachedDate = date;
            this.cachedPosition = position;
            this.cachedFrame = frame;
        }
    }

    /**
     * Compute lower part of atmosphere (< 86km).
     * 
     * @param z
     *        altitude (m)
     */
    private void computeLower86(final double z) {
        // Geopotential height
        final double hpot = AE * z / (AE + z);

        // Initialize auxiliary variable to break the for loop
        boolean breakloop = false;
        // Initialize Layer index
        int index = 6;
        // Initialize loop variable (intentionally starting from 1)
        int i = 1;

        // Retrieve the layers index
        while (i <= 6 & !breakloop) {
            if (hpot < LAYERH[i]) {
                index = i - 1;
                // Nothing else to do (quit for loop)
                breakloop = true;
            }
            // Increment loop variable
            i++;
        }

        // Molar temperature
        final double tmol = LAYERT[index] + LAYERDT[index] * (hpot - LAYERH[index]);

        // Kinetic temperature and molar mass computation
        if (z < LAYER_80K) {
            // Altitude < 80km

            this.cachedTemp = tmol;
            this.xmol = XMOL0;

        } else {
            // Altitude >= 80km

            // Re-initialize auxiliary variable
            breakloop = false;
            // Initialize layer index
            int jj = LAYER_NBR;
            // Initialize loop variable (intentionally starting from 1)
            int j = 1;

            double zj = 0;

            while (j < LAYERCT.length - 1 & !breakloop) {
                zj = LAYER_80K + j * DELTA_LAYER;

                if (z <= zj) {
                    jj = j - 1;
                    // nothing else to do
                    breakloop = true;
                }

                // Increment loop variable
                j++;
            }

            final double coeff = LAYERCT[jj] + (LAYERCT[jj + 1] - LAYERCT[jj]) /
                DELTA_LAYER * (z - zj + DELTA_LAYER);
            this.cachedTemp = tmol * coeff;
            this.xmol = XMOL0 * coeff;
        }
        // Pressure
        if (MathLib.abs(LAYERDT[index]) < EPS) {
            final double coef = MathLib.divide(-RATIO * (hpot - LAYERH[index]), LAYERT[index]);
            this.cachedPres = LAYERP[index] * MathLib.exp(coef);
        } else {
            this.cachedPres = LAYERP[index] * MathLib.pow(MathLib.divide(LAYERT[index], tmol),
                MathLib.divide(RATIO, LAYERDT[index]));
        }
    }

    /**
     * Compute upper part of atmosphere (> 86km).
     * 
     * @param z
     *        altitude (m)
     */
    private void computeUpper86(final double z) {
        // Compute temperature for each altitude
        final double temp76;
        if (z < LAYER_91K) {
            // Altitude < 91km
            temp76 = TEMP_91K;
        } else if (z < LAYER_110K) {
            // 91km <= Altitude < 110km

            // Initialize some constants
            final double pta = -19.9429e+03;
            final double tc = 263.1905;
            final double gda = -76.3232;
            final double rcar = MathLib.sqrt(MathLib.max(0.0, 1 - MathLib.pow(MathLib.divide((z - LAYER_91K),
                pta), 2)));

            temp76 = tc + gda * rcar;

        } else if (z < LAYER_120K) {
            // 110km <= Altitude < 120km
            temp76 = TEMP_110K + DT_110K * (z - LAYER_110K);
        } else {
            // Altitude >= 120km
            final double xi = (z - LAYER_120K) * (AE + LAYER_120K) / (AE + z);
            temp76 = TEMP_1000K - (TEMP_1000K - TEMP_120K) * MathLib.exp(-RLAMBDA * xi);

        }

        // Include the atmosphere composition
        final int indexAtmo = getAtmoIndex(z);

        // Computation of total molar mass (xmol)
        double pen = 0;
        this.xmol = 0.0;
        double rn = 0.0;
        for (int l = 1; l < US76Data.LAYERE[0].length; l++) {
            pen = (US76Data.LAYERE[MathLib.min(US76Data.LAYERE.length - 1, indexAtmo + 1)][l] -
                US76Data.LAYERE[indexAtmo][l])
                / (US76Data.LAYERE[MathLib.min(US76Data.LAYERE.length - 1, indexAtmo + 1)][0] -
                US76Data.LAYERE[indexAtmo][0]);
            final double rnn = US76Data.LAYERE[indexAtmo][l] + pen * (z - US76Data.LAYERE[indexAtmo][0]);
            this.xmol = this.xmol + rnn * LAYERM[l];
            rn += rnn;
        }
        this.xmol = MathLib.divide(this.xmol, rn);
        this.cachedTemp = temp76;
        // Pressure
        this.cachedPres = rn * BOLTZ * this.cachedTemp;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getVelocity(final AbsoluteDate date, final Vector3D position,
                                final Frame frame) throws PatriusException {
        final Transform bodyToFrame = this.earth.getBodyFrame().getTransformTo(frame, date);
        final Vector3D posInBody = bodyToFrame.getInverse().transformPosition(position);
        final PVCoordinates pvBody = new PVCoordinates(posInBody, new Vector3D(0, 0, 0));
        final PVCoordinates pvFrame = bodyToFrame.transformPVCoordinates(pvBody);
        return pvFrame.getVelocity();
    }

    /** {@inheritDoc} */
    @Override
    public double getSpeedOfSound(final AbsoluteDate date, final Vector3D position,
                                  final Frame frame) throws PatriusException {
        final double ro = this.getDensity(date, position, frame);
        return MathLib.sqrt(MathLib.divide(GAMMA * this.cachedPres, ro));
    }

    /**
     * Getter for the earth body.
     * 
     * @return the earth body
     */
    public BodyShape getEarthBody() {
        return this.earth;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>earth: {@link BodyShape}</li>
     * </ul>
     * </p>
     */
    @Override
    public Atmosphere copy() {
        return new US76(this.earth);
    }
    
    /** {@inheritDoc} */
    @Override
    public void checkSolarActivityData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
        // Nothing to do
    }
}
