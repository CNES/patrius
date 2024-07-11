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
 * @history creation 17/09/2012
 *
 * HISTORY
 * VERSION:4.9.1:FA:FA-3193:01/06/2022:[PATRIUS] Revenir a la signature initiale de la methode getLocalRadius
 * VERSION:4.9.1:DM:DM-3168:01/06/2022:[PATRIUS] Ajout de la classe ConstantPVCoordinatesProvider
 * VERSION:4.9:DM:DM-3127:10/05/2022:[PATRIUS] Ajout de deux methodes resize a l'interface GeometricBodyShape ...
 * VERSION:4.9:FA:FA-3170:10/05/2022:[PATRIUS] Incoherence de datation entre la methode getLocalRadius de GeometricBodyShape...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8.1:DM:DM-2900:07/12/2021:[PATRIUS] Possibilite de desactiver les eclipses pour la SRP 
 * VERSION:4.8:DM:DM-2898:15/11/2021:[PATRIUS] Hypothese geocentrique a supprimer pour la SRP 
 * VERSION:4.8:DM:DM-2899:15/11/2021:[PATRIUS] Autres corps occultants que la Terre pour la SRP 
 * VERSION:4.8:DM:DM-2900:15/11/2021:[PATRIUS] Possibilite de desactiver les eclipses pour la SRP 
 * VERSION:4.8:FA:FA-3009:15/11/2021:[PATRIUS] IllegalArgumentException SolarActivityToolbox
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.7:FA:FA-2897:18/05/2021:Alignement Soleil-Sat-Terre non supporté pour la SRP 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:FA:FA-2096:15/05/2019:[PATRIUS] Attribut coefficient multiplicatif non mis a jour dans la classe
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:93:08/08/2013:completed Javadoc concerning the partial derivatives parameters
 * VERSION::DM:85:29/08/2013:Made the getLighting ratio method static
 * VERSION::FA:93:31/03/2014:changed api for partial derivatives
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:439:12/06/2015:Corrected partial derivatives computation for PRS
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::FA:659:27/07/2016:change eclipse detection threshold to 1E-3 and change deprecated calls
 * VERSION::FA:648:27/07/2016:Corrected minor points staying from V3.2
 * VERSION::DM:611:04/08/2016:New implementation using radii provider for visibility of main/inhibition targets
 * VERSION::FA:675:01/09/2016:corrected anomalies reducing the performances
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::FA:704:07/12/2016: write FA 659 in "HISTORY"
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::FA:1279:15/11/2017:add getSolarFlux() method
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.radiation;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.models.DirectRadiativeModel;
import fr.cnes.sirius.patrius.bodies.GeometricBodyShape;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.GradientModel;
import fr.cnes.sirius.patrius.frames.Frame;
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
 * Solar radiation pressure force model with {@link GeometricBodyShape spheroids} (taking ellipsoid shape of occulting
 * body into account). Based on the
 * other implementation {@link SolarRadiationPressureCircular} for a spherical occulting body.
 *
 * <p>
 * The implementation of this class enables the computation of partial derivatives with respect to <b>absorption</b>,
 * <b>specular reflection</b> or <b>diffusion reflection coefficients</b>.
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
 * @concurrency not thread-safe
 * @concurrency.comment fields use Frames
 *
 * @author Rami Houdroge
 *
 * @version $Id: PatriusSolarRadiationPressure.java 18114 2017-10-04 09:32:13Z bignon $
 *
 * @since 1.2
 */
public class SolarRadiationPressureEllipsoid extends JacobiansParameterizable implements
    ForceModel, GradientModel {

    /** Normalized reference flux. */
    public static final String REFERENCE_FLUX = "normalized reference flux";

    /** Serializable UID. */
    private static final long serialVersionUID = -4510170320082379419L;

    /** Sun radius (m). */
    private static final double SUN_RADIUS = 6.95e8;

    /** Convergence threshold. */
    private static final double THRESHOLD = 1E-3;
    
    /** maximal checking interval (s). */
    private static final double MAX_CHECK = 60;

    /** Eclipses computation flag, true by default (eclipses computed). */
    private boolean eclipsesComputationFlag = true;

    /** Sun model. */
    private final PVCoordinatesProvider sun;

    /** Occulting body models. */
    private final List<GeometricBodyShape> occultingBodies;

    /** Spacecraft. */
    private final RadiationSensitive spacecraft;

    /** Normalized reference flux parameter */
    private Parameter fluxParam = null;

    /** Cached Satellite-Sun vector. */
    private Vector3D cachedSatSunVector;

    /** Cached satellite position. */
    private Vector3D cachedPosition;

    /** Cached date. */
    private AbsoluteDate cachedDate;

    /**
     * True if acceleration partial derivatives with respect to position have to be computed.
     */
    private final boolean computePartialDerivativesWrtPosition;

    /** Multiplicative factor. */
    private double multiplicativeFactor = 1;
    
    /**
     * Simple constructor with default reference values.
     * <p>
     * When this constructor is used, the reference values are:
     * </p>
     * <ul>
     * <li>d<sub>ref</sub> = 149597870000.0 m</li>
     * <li>p<sub>ref</sub> = 4.56 10<sup>-6</sup> N/m<sup>2</sup></li>
     * </ul>
     *
     * @param sunBody Sun model
     * @param occultingBody occulting body model (for umbra/penumbra computation)
     * @param spacecraftModel the object physical and geometrical information
     */
    public SolarRadiationPressureEllipsoid(final PVCoordinatesProvider sunBody,
        final GeometricBodyShape occultingBody, final RadiationSensitive spacecraftModel) {
        this(Constants.SEIDELMANN_UA, Constants.CONST_SOL_N_M2, sunBody, occultingBody, spacecraftModel);
    }

    /**
     * Simple constructor with default reference values.
     * <p>
     * When this constructor is used, the reference values are:
     * </p>
     * <ul>
     * <li>d<sub>ref</sub> = 149597870000.0 m</li>
     * <li>p<sub>ref</sub> = 4.56 10<sup>-6</sup> N/m<sup>2</sup></li>
     * </ul>
     *
     * @param sunBody Sun model
     * @param occultingBody occulting body model (for umbra/penumbra computation)
     * @param spacecraftModel the object physical and geometrical information
     * @param computePD true if partial derivatives wrt position have to be computed
     */
    public SolarRadiationPressureEllipsoid(final PVCoordinatesProvider sunBody,
        final GeometricBodyShape occultingBody, final RadiationSensitive spacecraftModel,
        final boolean computePD) {
        this(Constants.SEIDELMANN_UA, Constants.CONST_SOL_N_M2, sunBody, occultingBody, spacecraftModel,
            computePD);
    }

    /**
     * Complete constructor.
     * <p>
     * Note that reference solar radiation pressure <code>pRef</code> in N/m<sup>2</sup> is linked to solar flux SF in
     * W/m<sup>2</sup> using formula pRef = SF/c where c is the speed of light (299792458 m/s). So at 1UA a 1367
     * W/m<sup>2</sup> solar flux is a 4.56 10<sup>-6</sup> N/m<sup>2</sup> solar radiation pressure.
     * </p>
     *
     * @param dRef reference distance for the solar radiation pressure (m)
     * @param pRef reference solar radiation pressure at dRef (N/m<sup>2</sup>)
     * @param sunBody Sun model
     * @param occultingBody occulting body model (for umbra/penumbra computation)
     * @param spacecraftModel the object physical and geometrical information
     */
    public SolarRadiationPressureEllipsoid(final double dRef, final double pRef,
        final PVCoordinatesProvider sunBody, final GeometricBodyShape occultingBody,
        final RadiationSensitive spacecraftModel) {
        this(new Parameter(REFERENCE_FLUX, pRef * dRef * dRef), sunBody, occultingBody, spacecraftModel);
    }

    /**
     * Complete constructor.
     * <p>
     * Note that reference solar radiation pressure <code>pRef</code> in N/m<sup>2</sup> is linked to solar flux SF in
     * W/m<sup>2</sup> using formula pRef = SF/c where c is the speed of light (299792458 m/s). So at 1UA a 1367
     * W/m<sup>2</sup> solar flux is a 4.56 10<sup>-6</sup> N/m<sup>2</sup> solar radiation pressure.
     * </p>
     *
     * @param dRef reference distance for the solar radiation pressure (m)
     * @param pRef reference solar radiation pressure at dRef (N/m<sup>2</sup>)
     * @param sunBody Sun model
     * @param occultingBody occulting body model (for umbra/penumbra computation)
     * @param spacecraftModel the object physical and geometrical information
     * @param computePD true if partial derivatives wrt position have to be computed
     */
    public SolarRadiationPressureEllipsoid(final double dRef, final double pRef,
        final PVCoordinatesProvider sunBody, final GeometricBodyShape occultingBody,
        final RadiationSensitive spacecraftModel, final boolean computePD) {
        this(new Parameter(REFERENCE_FLUX, pRef * dRef * dRef), sunBody, occultingBody, spacecraftModel,
            computePD);
    }

    /**
     * Complete constructor using {@link Parameter}.
     *
     * @param referenceFlux the parameter representing the normalized reference flux
     * @param sunBody Sun model
     * @param occultingBody occulting body model (for umbra/penumbra computation)
     * @param spacecraftModel the object physical and geometrical information
     */
    public SolarRadiationPressureEllipsoid(final Parameter referenceFlux,
        final PVCoordinatesProvider sunBody, final GeometricBodyShape occultingBody,
        final RadiationSensitive spacecraftModel) {
        this(referenceFlux, sunBody, occultingBody, spacecraftModel, true);
    }

    /**
     * Complete constructor using {@link Parameter}.
     *
     * @param referenceFlux the parameter representing the normalized reference flux
     * @param sunBody Sun model
     * @param occultingBody occulting body model (for umbra/penumbra computation)
     * @param spacecraftModel the object physical and geometrical information
     * @param computePD true if partial derivatives wrt position have to be computed
     */
    public SolarRadiationPressureEllipsoid(final Parameter referenceFlux,
        final PVCoordinatesProvider sunBody, final GeometricBodyShape occultingBody,
        final RadiationSensitive spacecraftModel, final boolean computePD) {
        super();
        // Reference flux normalized for a 1m distance (N)
        this.addParameter(referenceFlux);
        // add all spacecraft parameters
        if (spacecraftModel != null) {
            this.addJacobiansParameter(spacecraftModel.getJacobianParameters());
        }
        this.enrichParameterDescriptors();

        this.fluxParam = referenceFlux;
        this.sun = sunBody;
        this.occultingBodies = new ArrayList<GeometricBodyShape>();
        this.occultingBodies.add(occultingBody);
        this.spacecraft = spacecraftModel;
        this.computePartialDerivativesWrtPosition = computePD;
        this.cachedSatSunVector = Vector3D.NaN;
        this.cachedPosition = Vector3D.NaN;
        this.cachedDate = AbsoluteDate.PAST_INFINITY;
        this.cachedSatSunVector = Vector3D.NaN;
    }

    /**
     * Creates a new instance.
     *
     * @param dRef reference distance for the solar radiation pressure (m)
     * @param pRef reference solar radiation pressure at dRef (N/m<sup>2</sup>)
     * @param sunBody Sun model
     * @param occultingBody occulting body model (for umbra/penumbra computation)
     * @param assembly assembly with aerodynamic properties
     * @param multiplicativeFactorIn multiplicative factor.
     */
    public SolarRadiationPressureEllipsoid(final double dRef, final double pRef,
        final PVCoordinatesProvider sunBody, final GeometricBodyShape occultingBody, final Assembly assembly,
        final double multiplicativeFactorIn) {
        this(dRef, pRef, sunBody, occultingBody,
            spacecraftModelFromAssembly(assembly, multiplicativeFactorIn));
        this.multiplicativeFactor = multiplicativeFactorIn;
    }

    /**
     * Creates a new instance from the data in another one but with a different assembly.
     *
     * @param otherInstance the other instance
     * @param assembly the new assembly
     */
    public SolarRadiationPressureEllipsoid(final SolarRadiationPressureEllipsoid otherInstance,
        final Assembly assembly) {
        this(otherInstance.getReferenceFlux(), otherInstance.getSunBody(),
            otherInstance.getOccultingBodies().get(0), spacecraftModelFromAssembly(assembly,
                otherInstance.getMultiplicativeFactor()));
        for (int i = 1; i < otherInstance.getOccultingBodies().size(); i++) {
            addOccultingBody(otherInstance.getOccultingBodies().get(i));
        }
        this.multiplicativeFactor = otherInstance.getMultiplicativeFactor();
    }

    /**
     * Creates an instance of {@link DirectRadiativeModel} from the radiative properties found in
     * the assembly.
     *
     * @param assembly with radiative properties.
     * @param multiplicativeFactor multiplicative factor.
     * @return the {@link DirectRadiativeModel} object.
     */
    private static DirectRadiativeModel spacecraftModelFromAssembly(final Assembly assembly,
                                                                    final double multiplicativeFactor) {
        return new DirectRadiativeModel(assembly, multiplicativeFactor);
    }

    /**
     * Compute radiation coefficient.
     *
     * @param s spacecraft state
     * @return coefficient for acceleration computation
     * @exception PatriusException thrown if Sun position cannot be retrieved
     */
    private double computeRawP(final SpacecraftState s) throws PatriusException {
        final AbsoluteDate date = s.getDate();
        final Frame frame = s.getFrame();
        final Vector3D position = s.getPVCoordinates().getPosition();
        final Vector3D satSunVector = this.getSatSunVector(s);
        final double r2 = satSunVector.getNormSq();
        return this.fluxParam.getValue()
            * getGlobalLightningRatio(satSunVector, position, frame, date) / r2;
    }

    /**
     * Compute solar flux.
     *
     * @param s spacecraft state
     * @return coefficient for acceleration computation
     * @exception PatriusException thrown if Sun position cannot be retrieved
     */
    public Vector3D getSolarFlux(final SpacecraftState s) throws PatriusException {
        final Vector3D satSunVector = this.getSatSunVector(s);
        final double rawP = this.computeRawP(s);
        return new Vector3D(MathLib.divide(-rawP, satSunVector.getNorm()), satSunVector);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D computeAcceleration(final SpacecraftState s) throws PatriusException {
        final Vector3D flux = this.getSolarFlux(s);
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
     * Get the global lightning ratio ([0-1]).
     * <p>
     * In case of multiple occulting bodies, the assumption is made that only one body occults the spacecraft at a 
     * time.
     * </p>
     *
     * @param satSunVector Satellite-Sun vector
     * @param position the satellite's position in the selected frame.
     * @param frame in which is defined the position
     * @param date the date
     * @return lightning ratio
     * @exception PatriusException if an error occurs
     */
    private double getGlobalLightningRatio(final Vector3D satSunVector,
            final Vector3D position,
            final Frame frame,
            final AbsoluteDate date) throws PatriusException {
        
        // Full computation

        double lightningRatio = 1;
        
        for (int i = 0; i < this.occultingBodies.size(); i++) {
            final double result = getLightningRatio(satSunVector, this.occultingBodies.get(i), position, frame, date);
            
            // Update total lightning ratio
            // Assumption: occulting bodies are not superposed from satellite point of view
            lightningRatio = MathLib.min(result, lightningRatio);
        }

        return lightningRatio;
    }

    /**
     * Get the lightning ratio ([0-1]) for provided occulting body.
     *
     * @param satSunVector Satellite-Sun vector
     * @param occultingBody occulting body
     * @param position the satellite's position in the selected frame.
     * @param frame in which is defined the position
     * @param date the date
     * @return lightning ratio
     * @exception PatriusException if an error occurs
     */
    public double getLightningRatio(final Vector3D satSunVector,
            final GeometricBodyShape occultingBody,
            final Vector3D position,
            final Frame frame,
            final AbsoluteDate date) throws PatriusException {

        if (!this.eclipsesComputationFlag) {
            // No eclipse computation: lightning ratio is 1
            return 1.;
        }
        
        // Full computation

        // Get position in occulting body frame
        final Transform t = frame.getTransformTo(occultingBody.getBodyFrame(), date);
        final Vector3D posBodyFrame = t.transformPosition(position);
        final Vector3D satSunVectorBodyFrame = t.transformVector(satSunVector);

        // Occulting body apparent radius
        final double r = posBodyFrame.getNorm();
        final double occultingRadius = occultingBody.getLocalRadius(posBodyFrame, occultingBody.getBodyFrame(), date,
            this.sun);
        final double value = MathLib.divide(occultingRadius, r);
        final double alphaOccultingBody = MathLib.asin(MathLib.min(1.0, MathLib.max(-1.0, value)));

        // Definition of the Sun's apparent radius
        final double value2 = MathLib.divide(SUN_RADIUS, satSunVectorBodyFrame.getNorm());
        final double alphaSun = MathLib.asin(MathLib.min(1.0, value2));

        // Sat-occulting body vector
        final Vector3D satBodyVector = posBodyFrame.negate();

        // Retrieve the Sat-Sun / Sat-Central body angle
        final double sunOccultingBodyAngle = Vector3D.angle(satSunVectorBodyFrame, satBodyVector);

        double result = 1.0;

        // Is the satellite in complete umbra ?
        if (sunOccultingBodyAngle - alphaOccultingBody + alphaSun <= 0.0) {
            result = 0.0;
        } else if (sunOccultingBodyAngle == 0) {
            // Satellite behind occulting body and exactly in line with occulted and occulting bodies
            result = 0.0;
        } else if (sunOccultingBodyAngle - alphaOccultingBody - alphaSun < 0.0) {
            // Compute a lightning ratio in penumbra

            final double sEA2 = sunOccultingBodyAngle * sunOccultingBodyAngle;
            final double oo2sEA = MathLib.divide(1.0, 2. * sunOccultingBodyAngle);
            final double aS2 = alphaSun * alphaSun;
            final double aE2 = alphaOccultingBody * alphaOccultingBody;
            final double aE2maS2 = aE2 - aS2;

            final double alpha1 = (sEA2 - aE2maS2) * oo2sEA;
            final double alpha2 = (sEA2 + aE2maS2) * oo2sEA;

            // Protection against numerical inaccuracy at boundaries
            final double a1oaS = MathLib.min(1.0, MathLib.max(-1.0, MathLib.divide(alpha1, alphaSun)));
            final double aS2ma12 = MathLib.max(0.0, aS2 - alpha1 * alpha1);
            final double a2oaE = MathLib.min(1.0, MathLib.max(-1.0, MathLib.divide(alpha2, alphaOccultingBody)));
            final double aE2ma22 = MathLib.max(0.0, aE2 - alpha2 * alpha2);

            final double p1 = aS2 * MathLib.acos(a1oaS) - alpha1 * MathLib.sqrt(aS2ma12);
            final double p2 = aE2 * MathLib.acos(a2oaE) - alpha2 * MathLib.sqrt(aE2ma22);

            result = 1. - MathLib.divide(p1 + p2, FastMath.PI * aS2);
        }

        return result;
    }

    /**
     * Compute sat-Sun vector in spacecraft state frame.
     *
     * @param state spacecraft state
     * @return sat-Sun vector in spacecraft state frame
     * @exception PatriusException thrown if sun position cannot be computed
     */
    private Vector3D getSatSunVector(final SpacecraftState state) throws PatriusException {
        // Invalidate the cache if input date or position are different
        if (this.cachedDate.compareTo(state.getDate()) != 0.
            || state.getPVCoordinates().getPosition().distance(this.cachedPosition) != 0.) {

            final PVCoordinates sunPV = this.sun.getPVCoordinates(state.getDate(), state.getFrame());
            final PVCoordinates satPV = state.getPVCoordinates();

            // Compute cached quantities
            this.cachedSatSunVector = sunPV.getPosition().subtract(satPV.getPosition());
            this.cachedDate = state.getDate();
            this.cachedPosition = state.getPVCoordinates().getPosition();
        }

        return this.cachedSatSunVector;
    }

    /**
     * Get the discrete events related to the model.
     *
     * @return array of events detectors or null if the model is not related to any discrete events
     */
    @Override
    public EventDetector[] getEventsDetectors() {
        // Add event detectors only if eclipses are computed (optimization)
        if (eclipsesComputationFlag) {
            // List of event detectors
            final List<EventDetector> detectors = new ArrayList<EventDetector>();
            // Add detectors for each occulting bodies
            for (int i = 0; i < this.occultingBodies.size(); i++) {
                EventDetector umbra = null;
                EventDetector penumbra = null;

                umbra = new EclipseDetector(this.sun, Constants.SUN_RADIUS, this.occultingBodies.get(i), 0., MAX_CHECK,
                        THRESHOLD) {
                    private static final long serialVersionUID = 1L;

                    /** {@inheritDoc} */
                    @Override
                    public Action eventOccurred(final SpacecraftState s,
                            final boolean increasing,
                            final boolean forward) {
                        return Action.RESET_DERIVATIVES;
                    }
                };
                penumbra = new EclipseDetector(this.sun, Constants.SUN_RADIUS, this.occultingBodies.get(i), 1.,
                        MAX_CHECK, THRESHOLD) {
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

            return detectors.toArray(new EventDetector[2 * this.occultingBodies.size()]);
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
            // containers
            final double[][] dAccdPosModel = new double[3][3];
            final double[][] dAccdVelModel = new double[3][3];

            // vector from satellite to sun
            final Vector3D satSunVector = this.getSatSunVector(s);

            // the jacobian with respect to velocity is not computed
            this.spacecraft.addDSRPAccDState(s, dAccdPosModel, dAccdVelModel, satSunVector);

            // multiplication factor
            final double cAlpha = getGlobalLightningRatio(satSunVector, s
                .getPVCoordinates().getPosition(), s.getFrame(), s.getDate());

            final double a = MathLib.divide(this.fluxParam.getValue() * cAlpha,
                satSunVector.getNormSq());

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

    /** {@inheritDoc} */
    @Override
    public void addDAccDParam(final SpacecraftState s, final Parameter param,
                              final double[] dAccdParam) throws PatriusException {

        // parameter
        if (!this.supportsJacobianParameter(param)) {
            throw new PatriusException(PatriusMessages.UNKNOWN_PARAMETER, param);
        }

        // container
        final double[] dAccdParamModel = new double[3];

        // derivatives
        final Vector3D satSunVector = this.getSatSunVector(s);
        this.spacecraft.addDSRPAccDParam(s, param, dAccdParamModel, satSunVector);

        // coefficient
        final double cAlpha = getGlobalLightningRatio(satSunVector, s.getPVCoordinates()
            .getPosition(), s.getFrame(), s.getDate());

        final double a = MathLib.divide(this.fluxParam.getValue() * cAlpha, satSunVector.getNormSq());

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
     * Getter for the Sun model used at construction.
     *
     * @return the Sun model.
     */
    public PVCoordinatesProvider getSunBody() {
        return this.sun;
    }

    /**
     * Getter for the occulting bodies.
     *
     * @return the occulting bodies.
     */
    public List<GeometricBodyShape> getOccultingBodies() {
        return this.occultingBodies;
    }

    /**
     * @return the multiplicativeFactor
     */
    public double getMultiplicativeFactor() {
        return this.multiplicativeFactor;
    }

    /**
     * Get the normalized reference flux parameter
     *
     * @return flux parameter
     */
    public Parameter getReferenceFlux() {
        return this.fluxParam;
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
     * @param body occulting body to add
     */
    public final void addOccultingBody(final GeometricBodyShape body) {
        this.occultingBodies.add(body);
    }
}
