/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
/*
 *
 * HISTORY
 * VERSION:4.8.1:DM:DM-2900:07/12/2021:[PATRIUS] Possibilite de desactiver les eclipses pour la SRP 
 * VERSION:4.8:DM:DM-2900:15/11/2021:[PATRIUS] Possibilite de desactiver les eclipses pour la SRP 
 * VERSION:4.8:DM:DM-2899:15/11/2021:[PATRIUS] Autres corps occultants que la Terre pour la SRP 
 * VERSION:4.8:DM:DM-2898:15/11/2021:[PATRIUS] Hypothese geocentrique a supprimer pour la SRP 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.7:FA:FA-2897:18/05/2021:Alignement Soleil-Sat-Terre non supporté pour la SRP 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:93:08/08/2013:completed Javadoc concerning the partial derivatives parameters
 * VERSION::FA:93:31/03/2014:changed api for partial derivatives
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:439:12/06/2015:Corrected partial derivatives computation for PRS
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::FA:659:27/07/2016:change eclipse detection threshold to 1E-3 and change deprecated calls
 * VERSION::FA:648:27/07/2016:Corrected minor points staying from V3.2
 * VERSION::FA:704:07/12/2016: write FA 659 in "HISTORY"
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::DM:1489:07/06/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.radiation;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.GradientModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
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

/**
 * Solar radiation pressure force model considering spherical occulting bodies.
 * 
 * <p>
 * The implementation of this class enables the computation of partial derivatives with respect to
 * <b>absorption</b>, <b>specular reflection</b> or <b>diffusion reflection coefficients</b>.
 * </p>
 *
 * <p>
 * Eclipses computation can be deactivated by using {@link #setEclipsesComputation(boolean)}. By default, eclipses are
 * taken into account.
 * </p>
 * 
 * <p>
 * This class allows to consider any occulting body (Earth, Moon, etc.).
 * In case of multiple occulting bodies, the assumption is made that only one body occults the spacecraft at a time.
 * </p>
 * 
 * @author Fabien Maussion
 * @author &Eacute;douard Delente
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Pascal Parraud
 */
public class SolarRadiationPressureCircular extends JacobiansParameterizable implements ForceModel,
        GradientModel {

    /** Normalized reference flux. */
    public static final String REFERENCE_FLUX = "normalized reference flux";

    /** Serializable UID. */
    private static final long serialVersionUID = -4510170320082379419L;

    /** Sun radius (m). */
    private static final double SUN_RADIUS = 6.95e8;

    /** Default reference solar radiation pressure at reference distance (N/m<sup>2</sup>). */
    private static final double PREF = 4.56e-6;

    /** Convergence threshold. */
    private static final double THRESHOLD = 1E-3;

    /** maximal checking interval (s). */
    private static final double MAX_CHECK = 60.;

    /** Reference flux normalized for a 1m distance (N). */
    private final Parameter kRef;

    /** Sun model. */
    private final PVCoordinatesProvider sun;

    /** Occulting bodies radius. */
    private final List<Double> occultingbodiesRadius;

    /** Occulting bodies frame. */
    private final List<Frame> occultingBodiesFrame;
    
    /** Spacecraft. */
    private final RadiationSensitive spacecraft;

    /** True if acceleration partial derivatives with respect to position have to be computed. */
    private final boolean computePartialDerivativesWrtPosition;

    /** Eclipses computation flag. */
    private boolean eclipsesComputationFlag = true;

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
     * @param sunIn Sun model
     * @param equatorialRadiusIn spherical shape model (for umbra/penumbra computation)
     * @param spacecraftIn the object physical and geometrical information
     */
    public SolarRadiationPressureCircular(final PVCoordinatesProvider sunIn,
            final double equatorialRadiusIn, final RadiationSensitive spacecraftIn) {
        this(Constants.SEIDELMANN_UA, PREF, sunIn, equatorialRadiusIn, spacecraftIn, true);
    }

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
     * @param sunIn Sun model
     * @param equatorialRadiusIn spherical shape model (for umbra/penumbra computation)
     * @param spacecraftIn the object physical and geometrical information
     * @param computePD true if partial derivatives wrt position have to be computed
     */
    public SolarRadiationPressureCircular(final PVCoordinatesProvider sunIn,
            final double equatorialRadiusIn,
            final RadiationSensitive spacecraftIn,
            final boolean computePD) {
        this(Constants.SEIDELMANN_UA, PREF, sunIn, equatorialRadiusIn, FramesFactory.getGCRF(), spacecraftIn, 
                computePD);
    }

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
     * @param sunIn Sun model
     * @param equatorialRadiusIn spherical shape model (for umbra/penumbra computation)
     * @param occultingBodyFrame occulting body-centered frame
     * @param spacecraftIn the object physical and geometrical information
     * @param computePD true if partial derivatives wrt position have to be computed
     */
    public SolarRadiationPressureCircular(final PVCoordinatesProvider sunIn,
            final double equatorialRadiusIn,
            final Frame occultingBodyFrame, final RadiationSensitive spacecraftIn,
            final boolean computePD) {
        this(Constants.SEIDELMANN_UA, PREF, sunIn, equatorialRadiusIn, occultingBodyFrame, spacecraftIn, computePD);
    }

    /**
     * Complete constructor.
     * <p>
     * Note that reference solar radiation pressure <code>pRef</code> in N/m<sup>2</sup> is linked
     * to solar flux SF in W/m<sup>2</sup> using formula pRef = SF/c where c is the speed of light
     * (299792458 m/s). So at 1UA a 1367 W/m<sup>2</sup> solar flux is a 4.56 10<sup>-6</sup>
     * N/m<sup>2</sup> solar radiation pressure.
     * </p>
     * 
     * @param dRef reference distance for the solar radiation pressure (m)
     * @param pRef reference solar radiation pressure at dRef (N/m<sup>2</sup>)
     * @param sunIn Sun model
     * @param equatorialRadiusIn spherical shape model (for umbra/penumbra computation)
     * @param spacecraftIn the object physical and geometrical information
     */
    public SolarRadiationPressureCircular(final double dRef, final double pRef,
            final PVCoordinatesProvider sunIn, final double equatorialRadiusIn,
            final RadiationSensitive spacecraftIn) {
        this(new Parameter(REFERENCE_FLUX, pRef * dRef * dRef), sunIn, equatorialRadiusIn,
                spacecraftIn, true);
    }

    /**
     * Complete constructor.
     * <p>
     * Note that reference solar radiation pressure <code>pRef</code> in N/m<sup>2</sup> is linked
     * to solar flux SF in W/m<sup>2</sup> using formula pRef = SF/c where c is the speed of light
     * (299792458 m/s). So at 1UA a 1367 W/m<sup>2</sup> solar flux is a 4.56 10<sup>-6</sup>
     * N/m<sup>2</sup> solar radiation pressure.
     * </p>
     * 
     * @param dRef reference distance for the solar radiation pressure (m)
     * @param pRef reference solar radiation pressure at dRef (N/m<sup>2</sup>)
     * @param sunIn Sun model
     * @param equatorialRadiusIn spherical shape model (for umbra/penumbra computation)
     * @param spacecraftIn the object physical and geometrical information
     * @param computePD true if partial derivatives wrt position have to be computed
     */
    public SolarRadiationPressureCircular(final double dRef, final double pRef,
            final PVCoordinatesProvider sunIn, final double equatorialRadiusIn,
            final RadiationSensitive spacecraftIn,
            final boolean computePD) {
        this(new Parameter(REFERENCE_FLUX, pRef * dRef * dRef), sunIn, equatorialRadiusIn,
                FramesFactory.getGCRF(), spacecraftIn, computePD);
    }

    /**
     * Complete constructor.
     * <p>
     * Note that reference solar radiation pressure <code>pRef</code> in N/m<sup>2</sup> is linked
     * to solar flux SF in W/m<sup>2</sup> using formula pRef = SF/c where c is the speed of light
     * (299792458 m/s). So at 1UA a 1367 W/m<sup>2</sup> solar flux is a 4.56 10<sup>-6</sup>
     * N/m<sup>2</sup> solar radiation pressure.
     * </p>
     * 
     * @param dRef reference distance for the solar radiation pressure (m)
     * @param pRef reference solar radiation pressure at dRef (N/m<sup>2</sup>)
     * @param sunIn Sun model
     * @param equatorialRadiusIn spherical shape model (for umbra/penumbra computation)
     * @param occultingBodyFrame occulting body-centered frame
     * @param spacecraftIn the object physical and geometrical information
     * @param computePD true if partial derivatives wrt position have to be computed
     */
    public SolarRadiationPressureCircular(final double dRef, final double pRef,
            final PVCoordinatesProvider sunIn, final double equatorialRadiusIn,
            final Frame occultingBodyFrame,
            final RadiationSensitive spacecraftIn,
            final boolean computePD) {
        this(new Parameter(REFERENCE_FLUX, pRef * dRef * dRef), sunIn, equatorialRadiusIn,
                occultingBodyFrame, spacecraftIn, computePD);
    }

    /**
     * Complete constructor.
     * 
     * @param refFlux parameter representing the reference flux normalized for a 1m distance (N)
     * @param sunIn Sun model
     * @param equatorialRadiusIn spherical shape model (for umbra/penumbra computation)
     * @param spacecraftIn the object physical and geometrical information
     */
    public SolarRadiationPressureCircular(final Parameter refFlux,
            final PVCoordinatesProvider sunIn, final double equatorialRadiusIn,
            final RadiationSensitive spacecraftIn) {
        this(refFlux, sunIn, equatorialRadiusIn, spacecraftIn, true);
    }

    /**
     * Complete constructor.
     * 
     * @param refFlux parameter representing the reference flux normalized for a 1m distance (N)
     * @param sunIn Sun model
     * @param equatorialRadiusIn spherical shape model (for umbra/penumbra computation)
     * @param spacecraftIn the object physical and geometrical information
     * @param computePD true if partial derivatives wrt position have to be computed
     */
    public SolarRadiationPressureCircular(final Parameter refFlux,
            final PVCoordinatesProvider sunIn, final double equatorialRadiusIn,
            final RadiationSensitive spacecraftIn,
            final boolean computePD) {
        this(refFlux, sunIn, equatorialRadiusIn, FramesFactory.getGCRF(), spacecraftIn, computePD);
    }

    /**
     * Complete constructor.
     * 
     * @param refFlux parameter representing the reference flux normalized for a 1m distance (N)
     * @param sunIn Sun model
     * @param equatorialRadiusIn spherical shape model (for umbra/penumbra computation)
     * @param occultingBodyFrame occulting body-centered frame
     * @param spacecraftIn the object physical and geometrical information
     * @param computePD true if partial derivatives wrt position have to be computed
     */
    public SolarRadiationPressureCircular(final Parameter refFlux,
            final PVCoordinatesProvider sunIn, final double equatorialRadiusIn,
            final Frame occultingBodyFrame,
            final RadiationSensitive spacecraftIn,
            final boolean computePD) {
        super();

        // Reference flux normalized for a 1m distance (N)
        this.addParameter(refFlux);
        // add all spacecraft parameters
        this.addJacobiansParameter(spacecraftIn.getJacobianParameters());
        this.enrichParameterDescriptors();

        this.kRef = refFlux;
        this.sun = sunIn;
        this.occultingbodiesRadius = new ArrayList<Double>();
        this.occultingbodiesRadius.add(equatorialRadiusIn);
        this.occultingBodiesFrame = new ArrayList<Frame>();
        this.occultingBodiesFrame.add(occultingBodyFrame);
        this.spacecraft = spacecraftIn;
        this.computePartialDerivativesWrtPosition = computePD;
    }

    /**
     * Compute radiation coefficient.
     * 
     * @param s spacecraft state
     * @return coefficient for acceleration computation
     * @exception PatriusException if position cannot be computed
     */
    private double computeRawP(final SpacecraftState s) throws PatriusException {
        final AbsoluteDate date = s.getDate();
        final Frame frame = s.getFrame();
        final Vector3D position = s.getPVCoordinates().getPosition();

        final Vector3D satSunVector = this.getSatSunVector(s);
        final double r2 = satSunVector.getNormSq();
        return this.kRef.getValue() * this.getLightningRatio(position, frame, date) / r2;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D computeAcceleration(final SpacecraftState s) throws PatriusException {

        final Vector3D satSunVector = this.getSatSunVector(s);
        final double rawP = this.computeRawP(s);
        final Vector3D flux = new Vector3D(-rawP / satSunVector.getNorm(), satSunVector);
        // raw radiation pressure
        return this.spacecraft.radiationPressureAcceleration(s, flux);
    }

    /** {@inheritDoc} */
    @Override
    public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder)
            throws PatriusException {

        // provide the perturbing acceleration to the derivatives adder
        adder.addAcceleration(this.computeAcceleration(s), s.getFrame());
    }

    /**
     * Get the lightning ratio ([0-1]).
     * <p>
     * In case of multiple occulting bodies, the assumption is made that only one body occults the spacecraft at a 
     * time.
     * </p>
     * 
     * @param position the satellite's position in the selected frame.
     * @param frame in which is defined the position
     * @param date the date
     * @return lightning ratio
     * @exception PatriusException if an error occurs
     */
    public double getLightningRatio(final Vector3D position, final Frame frame,
            final AbsoluteDate date) throws PatriusException {

        if (!this.eclipsesComputationFlag) {
            // No eclipse computation: lightning ratio is 1
            return 1.;
        }
        
        // Full computation

        double lightningRatio = 1;
        
        for (int i = 0; i < this.occultingBodiesFrame.size(); i++) {
            // Get position in occulting body frame
            final Transform t = frame.getTransformTo(this.occultingBodiesFrame.get(i), date);
            final Vector3D pos = t.transformPosition(position);

            // Sat-Sun vector in occulting body frame
            final Vector3D satSunVector = this.sun
                    .getPVCoordinates(date, this.occultingBodiesFrame.get(i)).getPosition()
                    .subtract(pos);

            // Occulting body apparent radius
            final double r = pos.getNorm();
            final double alphaOccultingBody = MathLib.asin(MathLib.min(1.0, this.occultingbodiesRadius.get(i) / r));

            // Definition of the Sun's apparent radius
            final double value = SUN_RADIUS / satSunVector.getNorm();
            final double alphaSun = MathLib.asin(MathLib.min(1.0, value));

            // Sat-occulting body vector
            final Vector3D satBodyVector = pos.negate();

            // Retrieve the Sat-Sun / Sat-Central body angle
            final double sunOccultingBodyAngle = Vector3D.angle(satSunVector, satBodyVector);

            double result = 1.0;

            // Is the satellite in complete umbra ?
            if (sunOccultingBodyAngle - alphaOccultingBody + alphaSun <= 0.0) {
                result = 0.0;
            } else if (sunOccultingBodyAngle == 0) {
                // Satellite behind occulting body and exactly in line with occulted and occulting bodies
                result = 0.0;
            } else if (sunOccultingBodyAngle - alphaOccultingBody - alphaSun < 0.0) {
                // Compute a lightning ratio in penumbra

                // result = (alphaSun + sunOccultingBodyAngle - alphaOccultingBody) / (2*alphaSun);

                final double sEA2 = sunOccultingBodyAngle * sunOccultingBodyAngle;
                final double oo2sEA = 1.0 / (2. * sunOccultingBodyAngle);
                final double aS2 = alphaSun * alphaSun;
                final double aE2 = alphaOccultingBody * alphaOccultingBody;
                final double aE2maS2 = aE2 - aS2;

                final double alpha1 = (sEA2 - aE2maS2) * oo2sEA;
                final double alpha2 = (sEA2 + aE2maS2) * oo2sEA;

                // Protection against numerical inaccuracy at boundaries
                final double a1oaS = MathLib.min(1.0, MathLib.max(-1.0, alpha1 / alphaSun));
                final double aS2ma12 = MathLib.max(0.0, aS2 - alpha1 * alpha1);
                final double a2oaE = MathLib.min(1.0, MathLib.max(-1.0, alpha2 / alphaOccultingBody));
                final double aE2ma22 = MathLib.max(0.0, aE2 - alpha2 * alpha2);

                final double p1 = aS2 * MathLib.acos(a1oaS) - alpha1 * MathLib.sqrt(aS2ma12);
                final double p2 = aE2 * MathLib.acos(a2oaE) - alpha2 * MathLib.sqrt(aE2ma22);

                result = 1. - (p1 + p2) / (FastMath.PI * aS2);
            }

            // Update total lightning ratio
            // Assumption: occulting bodies are not superposed from satellite point of view
            lightningRatio = MathLib.min(result, lightningRatio);
        }

        return lightningRatio;
    }

    /**
     * Compute sat-Sun vector in spacecraft state frame.
     * 
     * @param state current spacecraft state
     * @return sat-Sun vector in spacecraft state frame
     * @exception PatriusException if sun position cannot be computed
     */
    private Vector3D getSatSunVector(final SpacecraftState state) throws PatriusException {
        final PVCoordinates sunPV = this.sun.getPVCoordinates(state.getDate(), state.getFrame());
        final PVCoordinates satPV = state.getPVCoordinates();
        return sunPV.getPosition().subtract(satPV.getPosition());
    }

    /**
     * Get the discrete events related to the model.
     * 
     * @return array of events detectors or null if the model is not related to any discrete events
     */
    @Override
    public EventDetector[] getEventsDetectors() {
        if (eclipsesComputationFlag) {
            final List<EventDetector> detectors = new ArrayList<EventDetector>();
            for (int i = 0; i < this.occultingBodiesFrame.size(); i++) {
                final int index = i;
                final PVCoordinatesProvider occultingBody = new PVCoordinatesProvider() {
                    /** {@inheritDoc} */
                    @Override
                    public PVCoordinates getPVCoordinates(final AbsoluteDate date,
                            final Frame frame) throws PatriusException {
                        return SolarRadiationPressureCircular.this.occultingBodiesFrame.get(index)
                                .getTransformTo(frame, date).getCartesian();
                    };
                };
                EventDetector umbra = null;
                EventDetector penumbra = null;

                umbra = new EclipseDetector(this.sun, Constants.SUN_RADIUS, occultingBody,
                        this.occultingbodiesRadius.get(i), 0., MAX_CHECK, THRESHOLD) {
                    /** UID. */
                    private static final long serialVersionUID = 1L;

                    /** {@inheritDoc} */
                    @Override
                    public Action eventOccurred(final SpacecraftState s,
                            final boolean increasing,
                            final boolean forward) {
                        return Action.RESET_DERIVATIVES;
                    }
                };
                penumbra = new EclipseDetector(this.sun, Constants.SUN_RADIUS, occultingBody,
                        this.occultingbodiesRadius.get(i), 1., MAX_CHECK, THRESHOLD) {
                    /** UID. */
                    private static final long serialVersionUID = 1L;

                    /** {@inheritDoc} */
                    @Override
                    public Action eventOccurred(final SpacecraftState s,
                            final boolean increasing,
                            final boolean forward) {
                        return Action.RESET_DERIVATIVES;
                    }
                };
                detectors.add(umbra);
                detectors.add(penumbra);
            }

            return detectors.toArray(new EventDetector[2 * this.occultingBodiesFrame.size()]);
        } else {
            // No occultation: no eclipse
            return new EventDetector[0];
        }
    }

    /** {@inheritDoc} */
    @Override
    public void addDAccDState(final SpacecraftState s, final double[][] dAccdPos,
            final double[][] dAccdVel) throws PatriusException {

        if (this.computeGradientPosition()) {
            // Only derivative wrt position
            final double[][] dAccdPosModel = new double[3][3];
            final double[][] dAccdVelModel = new double[3][3];

            // Sat - Sun vector
            final Vector3D satSunVector = this.getSatSunVector(s);

            // the jacobian with respect to velocity is not computed
            this.spacecraft.addDSRPAccDState(s, dAccdPosModel, dAccdVelModel, satSunVector);

            final double cAlpha = this.getLightningRatio(s.getPVCoordinates().getPosition(),
                    s.getFrame(), s.getDate());
            final double a = this.kRef.getValue() * cAlpha / satSunVector.getNormSq();

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
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void addDAccDParam(final SpacecraftState s, final Parameter param,
            final double[] dAccdParam) throws PatriusException {

        if (!this.supportsJacobianParameter(param)) {
            throw new PatriusException(PatriusMessages.UNKNOWN_PARAMETER, param);
        }

        final double[] dAccdParamModel = new double[3];

        final Vector3D satSunVector = this.getSatSunVector(s);

        this.spacecraft.addDSRPAccDParam(s, param, dAccdParamModel, satSunVector);
        final double cAlpha = this.getLightningRatio(s.getPVCoordinates().getPosition(),
                s.getFrame(), s.getDate());
        final double a = this.kRef.getValue() * cAlpha / satSunVector.getNormSq();
        dAccdParam[0] += a * dAccdParamModel[0];
        dAccdParam[1] += a * dAccdParamModel[1];
        dAccdParam[2] += a * dAccdParamModel[2];
    }

    /** {@inheritDoc} */
    @Override
    public boolean computeGradientPosition() {
        return this.computePartialDerivativesWrtPosition;
    }

    /** {@inheritDoc} */
    @Override
    public boolean computeGradientVelocity() {
        return false;
    }

    /**
     * Get the parameter value representing the reference flux normalized for a 1m distance (N).
     * 
     * @return the kRef value
     */
    public double getkRefValue() {
        return this.kRef.getValue();
    }

    /**
     * Get sun model
     * 
     * @return the sun
     */
    public PVCoordinatesProvider getSun() {
        return this.sun;
    }

    /**
     * Get the equatorial radius of occulting bodies.
     * 
     * @return the equatorial radius of occulting bodies
     */
    public List<Double> getEquatorialRadius() {
        return this.occultingbodiesRadius;
    }
    
    /** {@inheritDoc} */
    @Override
    public void checkData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
        // Nothing to do
    }
    
    /**
     * Setter for enabling/disabling eclipses computation.
     * @param eclipsesComputationFlagIn true if eclipses should be taken into account, false otherwise
     */
    public void setEclipsesComputation(final boolean eclipsesComputationFlagIn) {
        this.eclipsesComputationFlag = eclipsesComputationFlagIn;
    }

    /**
     * Returns flag indicating if eclipses should be taken into account.
     * @return flag indicating if eclipses should be taken into account
     */
    public boolean isEclipseComputation() {
        return this.eclipsesComputationFlag;
    }
    
    /**
     * Add an occulting body.
     * @param bodyRadius occulting body radius to add
     * @param bodyFrame occulting body-centered frame to add
     */
    public final void addOccultingBody(final double bodyRadius, final Frame bodyFrame) {
        this.occultingbodiesRadius.add(bodyRadius);
        this.occultingBodiesFrame.add(bodyFrame);
    }

    /**
     * Returns the list of occulting bodies radius.
     * @return the list of occulting bodies radius
     */
    public List<Double> getOccultingbodiesRadius() {
        return this.occultingbodiesRadius;
    }

    /**
     * Returns the list of occulting bodies frame.
     * @return the list of occulting bodies frame
     */
    public List<Frame> getOccultingBodiesFrame() {
        return this.occultingBodiesFrame;
    }
}
