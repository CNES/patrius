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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:105:21/11/2013: class creation.
 * VERSION::FA:93:01/04/2014:Changed partial derivatives API
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:226:12/09/2014: problem with event detections.
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.smallstepdetection;

import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.GradientModel;
import fr.cnes.sirius.patrius.forces.radiation.RadiationSensitive;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.JacobiansParameterizable;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EclipseDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * Solar radiation pressure force model.
 * 
 * @author Fabien Maussion
 * @author &Eacute;douard Delente
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Pascal Parraud
 */
public class CustomSolarRadiationPressure extends JacobiansParameterizable implements ForceModel, GradientModel {

    /** Parameter name for absorption coefficient. */
    public static final String ABSORPTION_COEFFICIENT = "absorption coefficient";

    /** Parameter name for reflection coefficient. */
    public static final String SPECULAR_COEFFICIENT = "specular reflection coefficient";

    /** Parameter name for diffusion coefficient. */
    public static final String DIFFUSION_COEFFICIENT = "diffusion reflection coefficient";

    /** Normalized reference flux. */
    public static final String REFERENCE_FLUX = "normalized reference flux";

    /** Serializable UID. */
    private static final long serialVersionUID = -4510170320082379419L;

    /** Sun radius (m). */
    private static final double SUN_RADIUS = 6.95e8;

    public static final double TOLERANCE_ECLIPSE = 1e-3;

    /** Reference flux normalized for a 1m distance (N). */
    private final double kRef;

    /** Sun model. */
    private final PVCoordinatesProvider sun;

    /** Earth model. */
    private final double equatorialRadius;

    /** Spacecraft. */
    private final RadiationSensitive spacecraft;

    /**
     * Simple constructor with default reference values.
     * <p>
     * When this constructor is used, the reference values are:
     * </p>
     * <ul>
     * <li>d<sub>ref</sub> = 149597870000 m</li>
     * <li>p<sub>ref</sub> = 4.56 10<sup>-6</sup> N/m<sup>2</sup></li>
     * </ul>
     * 
     * @param sun
     *        Sun model
     * @param equatorialRadius
     *        spherical shape model (for umbra/penumbra computation)
     * @param spacecraft
     *        the object physical and geometrical information
     * @throws PatriusException
     */
    public CustomSolarRadiationPressure(final PVCoordinatesProvider sun,
        final double equatorialRadius, final RadiationSensitive spacecraft) {
        this(Constants.SEIDELMANN_UA, 4.56e-6, sun, equatorialRadius,
            spacecraft);
    }

    /**
     * Complete constructor.
     * <p>
     * Note that reference solar radiation pressure <code>pRef</code> in N/m<sup>2</sup> is linked to solar flux SF in
     * W/m<sup>2</sup> using formula pRef = SF/c where c is the speed of light (299792458 m/s). So at 1UA a 1367
     * W/m<sup>2</sup> solar flux is a 4.56 10<sup>-6</sup> N/m<sup>2</sup> solar radiation pressure.
     * </p>
     * 
     * @param dRef
     *        reference distance for the solar radiation pressure (m)
     * @param pRef
     *        reference solar radiation pressure at dRef (N/m<sup>2</sup>)
     * @param sun
     *        Sun model
     * @param equatorialRadius
     *        spherical shape model (for umbra/penumbra computation)
     * @param spacecraft
     *        the object physical and geometrical information
     * @throws PatriusException
     */
    public CustomSolarRadiationPressure(final double dRef, final double pRef,
        final PVCoordinatesProvider sun, final double equatorialRadius,
        final RadiationSensitive spacecraft) {
        super();
        this.addJacobiansParameter(new Parameter(REFERENCE_FLUX, pRef * dRef * dRef));
        this.addJacobiansParameter(spacecraft.getJacobianParameters());
        this.enrichParameterDescriptors();
        this.kRef = pRef * dRef * dRef;
        this.sun = sun;
        this.equatorialRadius = equatorialRadius;
        this.spacecraft = spacecraft;
    }

    /**
     * Compute radiation coefficient.
     * 
     * @param s
     *        spacecraft state
     * @return coefficient for acceleration computation
     * @exception PatriusException
     *            if position cannot be computed
     */
    private double computeRawP(final SpacecraftState s) throws PatriusException {
        final AbsoluteDate date = s.getDate();
        final Frame frame = s.getFrame();
        final Vector3D position = s.getPVCoordinates().getPosition();

        final Vector3D satSunVector = this.getSatSunVector(s);
        final double r2 = satSunVector.getNormSq();
        return this.kRef * this.getLightningRatio(position, frame, date) / r2;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D computeAcceleration(final SpacecraftState s)
                                                                throws PatriusException {

        final Vector3D satSunVector = this.getSatSunVector(s);
        final double rawP = this.computeRawP(s);
        final Vector3D flux = new Vector3D(-rawP / satSunVector.getNorm(),
            satSunVector);
        // raw radiation pressure
        final Vector3D gamma = this.spacecraft
            .radiationPressureAcceleration(s, flux);
        return gamma;
    }

    /** {@inheritDoc} */
    @Override
    public void addContribution(final SpacecraftState s,
                                final TimeDerivativesEquations adder) throws PatriusException {

        // provide the perturbing acceleration to the derivatives adder
        adder.addAcceleration(this.computeAcceleration(s), s.getFrame());
    }

    /**
     * Get the lightning ratio ([0-1]).
     * 
     * @param position
     *        the satellite's position in the selected frame.
     * @param frame
     *        in which is defined the position
     * @param date
     *        the date
     * @return lightning ratio
     * @exception PatriusException
     *            if an error occurs
     */
    public double getLightningRatio(final Vector3D position, final Frame frame,
                                    final AbsoluteDate date) throws PatriusException {

        final Vector3D satSunVector = this.sun.getPVCoordinates(date, frame)
            .getPosition().subtract(position);

        // Earth apparent radius
        final double r = position.getNorm();

        final double alphaEarth = MathLib.asin(this.equatorialRadius / r);

        // Definition of the Sun's apparent radius
        final double alphaSun = MathLib.asin(SUN_RADIUS
            / satSunVector.getNorm());

        // Retrieve the Sat-Sun / Sat-Central body angle
        final double sunEarthAngle = Vector3D.angle(satSunVector,
            position.negate());

        double result = 1.0;

        // Is the satellite in complete umbra ?
        if (sunEarthAngle - alphaEarth + alphaSun <= 0.0) {
            result = 0.0;
        } else if (sunEarthAngle - alphaEarth - alphaSun < 0.0) {
            // Compute a lightning ratio in penumbra

            // result = (alphaSun + sunEarthAngle - alphaEarth) / (2*alphaSun);

            final double sEA2 = sunEarthAngle * sunEarthAngle;
            final double oo2sEA = 1.0 / (2. * sunEarthAngle);
            final double aS2 = alphaSun * alphaSun;
            final double aE2 = alphaEarth * alphaEarth;
            final double aE2maS2 = aE2 - aS2;

            final double alpha1 = (sEA2 - aE2maS2) * oo2sEA;
            final double alpha2 = (sEA2 + aE2maS2) * oo2sEA;

            // Protection against numerical inaccuracy at boundaries
            final double a1oaS = MathLib.min(1.0,
                MathLib.max(-1.0, alpha1 / alphaSun));
            final double aS2ma12 = MathLib.max(0.0, aS2 - alpha1 * alpha1);
            final double a2oaE = MathLib.min(1.0,
                MathLib.max(-1.0, alpha2 / alphaEarth));
            final double aE2ma22 = MathLib.max(0.0, aE2 - alpha2 * alpha2);

            final double P1 = aS2 * MathLib.acos(a1oaS) - alpha1
                * MathLib.sqrt(aS2ma12);
            final double P2 = aE2 * MathLib.acos(a2oaE) - alpha2
                * MathLib.sqrt(aE2ma22);

            result = 1. - (P1 + P2) / (FastMath.PI * aS2);
        }

        return result;
    }

    /**
     * Compute sat-Sun vector in spacecraft state frame.
     * 
     * @param state
     *        current spacecraft state
     * @return sat-Sun vector in spacecraft state frame
     * @exception PatriusException
     *            if sun position cannot be computed
     */
    private Vector3D getSatSunVector(final SpacecraftState state)
                                                                 throws PatriusException {
        final PVCoordinates sunPV = this.sun.getPVCoordinates(state.getDate(),
            state.getFrame());
        final PVCoordinates satPV = state.getPVCoordinates();
        return sunPV.getPosition().subtract(satPV.getPosition());
    }

    /**
     * Get the discrete events related to the model.
     * 
     * @return array of events detectors or null if the model is not related to
     *         any discrete events
     */
    @Override
    public EventDetector[] getEventsDetectors() {
        CelestialBody earth;
        EventDetector umbra = null;
        EventDetector penumbra = null;

        try {
            earth = CelestialBodyFactory.getEarth();

            umbra = new EclipseDetector(this.sun, Constants.SUN_RADIUS, earth,
                this.equatorialRadius, 0, 60., TOLERANCE_ECLIPSE){
                private static final long serialVersionUID = 1L;

                @Override
                public Action eventOccurred(final SpacecraftState s,
                                            final boolean increasing, final boolean forward) {
                    if (increasing) {
                        System.err.println("UMBRA IN (SRP detector)\t" + s.getDate());
                    }
                    if (!increasing) {
                        System.err.println("UMBRA OUT (SRP detector)\t" + s.getDate());
                    }
                    return Action.RESET_DERIVATIVES;
                }
            };
            penumbra = new EclipseDetector(this.sun, Constants.SUN_RADIUS, earth,
                this.equatorialRadius, 1, 60., TOLERANCE_ECLIPSE){
                private static final long serialVersionUID = 1L;

                @Override
                public Action eventOccurred(final SpacecraftState s,
                                            final boolean increasing, final boolean forward) {
                    if (increasing) {
                        System.err.println("PENUMBRA IN (SRP detector)\t" + s.getDate());
                    }
                    if (!increasing) {
                        System.err.println("PENUMBRA OUT (SRP detector)\t" + s.getDate());
                    }
                    return Action.RESET_DERIVATIVES;
                }
            };
        } catch (final PatriusException e) {
            throw new PatriusRuntimeException(
                PatriusMessages.NO_DATA_LOADED_FOR_CELESTIAL_BODY, e);
        }

        return new EventDetector[] { umbra, penumbra };

        // return new EventDetector[0];
    }

    /** {@inheritDoc} */
    @Override
    public void addDAccDState(final SpacecraftState s,
                              final double[][] dAccdPos, final double[][] dAccdVel) throws PatriusException {

        final double[][] dAccdPosModel = new double[3][3];
        final double[][] dAccdVelModel = new double[3][3];

        final Vector3D satSunVector = this.getSatSunVector(s);

        // the jacobian with respect to velocity is not computed
        this.spacecraft.addDSRPAccDState(s, dAccdPosModel, dAccdVelModel, satSunVector);

        final double cAlpha = this.getLightningRatio(s.getPVCoordinates()
            .getPosition(), s.getFrame(), s.getDate());
        final double a = this.kRef * cAlpha / satSunVector.getNormSq();

        // jacobian with respect to position
        dAccdPos[0][0] += a * dAccdPosModel[0][0];
        dAccdPos[0][1] += a * dAccdPosModel[0][1];
        dAccdPos[0][2] += a * dAccdPosModel[0][2];
        dAccdPos[1][0] += a * dAccdPosModel[1][0];
        dAccdPos[1][1] += a * dAccdPosModel[1][1];
        dAccdPos[1][2] += a * dAccdPosModel[1][2];
        dAccdPos[2][0] += a * dAccdPosModel[2][0];
        dAccdPos[2][1] += a * dAccdPosModel[2][1];
        dAccdPos[2][2] += a * dAccdPosModel[2][2];
    }

    /** {@inheritDoc} */
    @Override
    public void addDAccDParam(final SpacecraftState s, final Parameter param,
                              final double[] dAccdParam) throws PatriusException {
        if (!this.supportsJacobianParameter(param)) {
            throw new PatriusException(PatriusMessages.UNKNOWN_PARAMETER, param);
        }

        final double[] dAccdParamModel = new double[3];

        final Vector3D satSunVector = this.getSatSunVector(s);

        this.spacecraft.addDSRPAccDParam(s, param, dAccdParamModel, satSunVector);
        final double cAlpha = this.getLightningRatio(s.getPVCoordinates()
            .getPosition(), s.getFrame(), s.getDate());
        final double a = this.kRef * cAlpha / satSunVector.getNormSq();
        dAccdParam[0] += a * dAccdParamModel[0];
        dAccdParam[1] += a * dAccdParamModel[1];
        dAccdParam[2] += a * dAccdParamModel[2];
    }

    /** {@inheritDoc} */
    @Override
    public boolean computeGradientPosition() {
        // Test class, no need to add full parameterization
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean computeGradientVelocity() {
        return false;
    }
    
    /** {@inheritDoc} */
    @Override
    public void checkData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
        // Nothing to do
    }
}
