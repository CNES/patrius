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
 * HISTORY
 * VERSION:4.10:DM:DM-3244:03/11/2022:[PATRIUS] Ajout propagation du signal dans ExtremaElevationDetector
 * VERSION:4.10:DM:DM-3228:03/11/2022:[PATRIUS] Integration des evolutions de la branche patrius-for-lotus 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.radiation;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.models.DirectRadiativeModel;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.GradientModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.JacobiansParameterizable;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.ParameterUtils;
import fr.cnes.sirius.patrius.math.parameter.StandardFieldDescriptors;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.propagation.events.EclipseDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Solar radiation pressure force model considering spherical/circular or non-spherical (ellipsoid) occulting bodies.
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
 * This class allows to consider any occulting body (Earth, Moon, etc.).<br>
 * In case of multiple occulting bodies, the assumption is made that only one body occults the spacecraft at a time.
 * </p>
 *
 * <p>
 * Light speed is currently never taken into account.
 * </p>
 *
 * @concurrency not thread-safe
 * @concurrency.comment fields use Frames
 *
 * @author Rami Houdroge, Thibaut Bonit
 *
 * @version $Id: PatriusSolarRadiationPressure.java 18114 2017-10-04 09:32:13Z bignon $
 *
 * @since 1.2
 */
public class SolarRadiationPressure extends JacobiansParameterizable implements
    ForceModel, GradientModel {

    /** Normalized reference flux. */
    public static final String REFERENCE_FLUX = "normalized reference flux";

    /** Serializable UID. */
    private static final long serialVersionUID = -5542959128461000171L;

    /** Sun radius (m). */
    private static final double SUN_RADIUS = 6.95e8;

    /** Convergence threshold. */
    private static final double THRESHOLD = 1E-3;

    /** Maximal checking interval (s). */
    private static final double MAX_CHECK = 60;

    /** Eclipses computation flag, true by default (eclipses computed). */
    private boolean eclipsesComputationFlag = true;

    /** Sun model. */
    private final PVCoordinatesProvider sun;

    /** Occulting body models. */
    private final List<BodyShape> occultingBodies;

    /** Spacecraft. */
    private final RadiationSensitive spacecraft;

    /** Reference flux normalized for a 1m distance (N) */
    private Parameter fluxParam = null;

    /** Cached Satellite-Sun vector. */
    private Vector3D cachedSatSunVector;

    /** Cached satellite position. */
    private Vector3D cachedPosition;

    /** Cached date. */
    private AbsoluteDate cachedDate;

    /** True if acceleration partial derivatives with respect to position have to be computed. */
    private final boolean computePartialDerivativesWrtPosition;

    /** Multiplicative factor. */
    private double multiplicativeFactor = 1.;

    /**
     * Simple constructor with default reference values.
     * <p>
     * When this constructor is used, the reference values are:
     * </p>
     * <ul>
     * <li>d<sub>ref</sub> = 149597870000 m</li>
     * <li>p<sub>ref</sub> = 4.560500000000001 10<sup>-6</sup> N/m<sup>2</sup></li>
     * </ul>
     * <p>
     * Note: this constructor defines a spherical/circular occulting body (see
     * {@link #buildOccultingBodies(double, Frame)}.<br>
     * The {@link #getLightningRatio(SpacecraftState, Frame, AbsoluteDate) lightning ratio} and the
     * {@link #getEventsDetectors() events detectors} computation will be optimized.
     * </p>
     * 
     * @param sun
     *        Sun model
     * @param occultingBodyRadius
     *        Occulting body radius
     * @param spacecraft
     *        The physical and geometrical spacecraft representation
     */
    public SolarRadiationPressure(final PVCoordinatesProvider sun, final double occultingBodyRadius,
                                  final RadiationSensitive spacecraft) {
        this(sun, occultingBodyRadius, spacecraft, true);
    }

    /**
     * Simple constructor with default reference values.
     * <p>
     * When this constructor is used, the reference values are:
     * </p>
     * <ul>
     * <li>d<sub>ref</sub> = 149597870000 m</li>
     * <li>p<sub>ref</sub> = 4.560500000000001 10<sup>-6</sup> N/m<sup>2</sup></li>
     * </ul>
     * <p>
     * Note: this constructor defines a spherical/circular occulting body (see
     * {@link #buildOccultingBodies(double, Frame)}.<br>
     * The {@link #getLightningRatio(SpacecraftState, Frame, AbsoluteDate) lightning ratio} and the
     * {@link #getEventsDetectors() events detectors} computation will be optimized.
     * </p>
     * 
     * @param sun
     *        Sun model
     * @param occultingBodyRadius
     *        Occulting body radius
     * @param spacecraft
     *        The physical and geometrical spacecraft representation
     * @param computePD
     *        {@code true} if the partial derivatives wrt position have to be computed, {@code false} otherwise
     */
    public SolarRadiationPressure(final PVCoordinatesProvider sun, final double occultingBodyRadius,
                                  final RadiationSensitive spacecraft, final boolean computePD) {
        this(sun, occultingBodyRadius, FramesFactory.getGCRF(), spacecraft, computePD);
    }

    /**
     * Simple constructor with default reference values.
     * <p>
     * When this constructor is used, the reference values are:
     * </p>
     * <ul>
     * <li>d<sub>ref</sub> = 149597870000 m</li>
     * <li>p<sub>ref</sub> = 4.560500000000001 10<sup>-6</sup> N/m<sup>2</sup></li>
     * </ul>
     * <p>
     * Note: this constructor defines a spherical/circular occulting body (see
     * {@link #buildOccultingBodies(double, Frame)}.<br>
     * The {@link #getLightningRatio(SpacecraftState, Frame, AbsoluteDate) lightning ratio} and the
     * {@link #getEventsDetectors() events detectors} computation will be optimized.
     * </p>
     * 
     * @param sun
     *        Sun model
     * @param occultingBodyRadius
     *        Occulting body radius
     * @param occultingBodyFrame
     *        Occulting body-centered frame
     * @param spacecraft
     *        The physical and geometrical spacecraft representation
     * @param computePD
     *        {@code true} if the partial derivatives wrt position have to be computed, {@code false} otherwise
     */
    public SolarRadiationPressure(final PVCoordinatesProvider sun, final double occultingBodyRadius,
                                  final Frame occultingBodyFrame, final RadiationSensitive spacecraft,
                                  final boolean computePD) {
        this(Constants.SEIDELMANN_UA, Constants.CONST_SOL_N_M2, sun, occultingBodyRadius, occultingBodyFrame,
                spacecraft, computePD);
    }

    /**
     * Complete constructor.
     * <p>
     * Note that reference solar radiation pressure <code>pRef</code> in N/m<sup>2</sup> is linked to solar flux SF in
     * W/m<sup>2</sup> using formula pRef = SF/c where c is the speed of light (299792458 m/s). So at 1UA a 1367
     * W/m<sup>2</sup> solar flux is a 4.560500000000001 10<sup>-6</sup> N/m<sup>2</sup> solar radiation pressure.
     * </p>
     * <p>
     * Note: this constructor defines a spherical/circular occulting body (see
     * {@link #buildOccultingBodies(double, Frame)}.<br>
     * The {@link #getLightningRatio(SpacecraftState, Frame, AbsoluteDate) lightning ratio} and the
     * {@link #getEventsDetectors() events detectors} computation will be optimized.
     * </p>
     * 
     * @param dRef
     *        Reference distance for the solar radiation pressure (m)
     * @param pRef
     *        Reference solar radiation pressure at dRef (N/m<sup>2</sup>)
     * @param sun
     *        Sun model
     * @param occultingBodyRadius
     *        Occulting body radius
     * @param spacecraft
     *        The physical and geometrical spacecraft representation
     */
    public SolarRadiationPressure(final double dRef, final double pRef, final PVCoordinatesProvider sun,
                                  final double occultingBodyRadius, final RadiationSensitive spacecraft) {
        this(dRef, pRef, sun, occultingBodyRadius, spacecraft, true);
    }

    /**
     * Complete constructor.
     * <p>
     * Note that reference solar radiation pressure <code>pRef</code> in N/m<sup>2</sup> is linked to solar flux SF in
     * W/m<sup>2</sup> using formula pRef = SF/c where c is the speed of light (299792458 m/s). So at 1UA a 1367
     * W/m<sup>2</sup> solar flux is a 4.560500000000001 10<sup>-6</sup> N/m<sup>2</sup> solar radiation pressure.
     * </p>
     * <p>
     * Note: this constructor defines a spherical/circular occulting body (see
     * {@link #buildOccultingBodies(double, Frame)}.<br>
     * The {@link #getLightningRatio(SpacecraftState, Frame, AbsoluteDate) lightning ratio} and the
     * {@link #getEventsDetectors() events detectors} computation will be optimized.
     * </p>
     * 
     * @param dRef
     *        Reference distance for the solar radiation pressure (m)
     * @param pRef
     *        Reference solar radiation pressure at dRef (N/m<sup>2</sup>)
     * @param sun
     *        Sun model
     * @param occultingBodyRadius
     *        Occulting body radius
     * @param spacecraft
     *        The physical and geometrical spacecraft representation
     * @param computePD
     *        {@code true} if the partial derivatives wrt position have to be computed, {@code false} otherwise
     */
    public SolarRadiationPressure(final double dRef, final double pRef, final PVCoordinatesProvider sun,
                                  final double occultingBodyRadius, final RadiationSensitive spacecraft,
                                  final boolean computePD) {
        this(dRef, pRef, sun, occultingBodyRadius, FramesFactory.getGCRF(), spacecraft, computePD);
    }

    /**
     * Complete constructor.
     * <p>
     * Note that reference solar radiation pressure <code>pRef</code> in N/m<sup>2</sup> is linked to solar flux SF in
     * W/m<sup>2</sup> using formula pRef = SF/c where c is the speed of light (299792458 m/s). So at 1UA a 1367
     * W/m<sup>2</sup> solar flux is a 4.560500000000001 10<sup>-6</sup> N/m<sup>2</sup> solar radiation pressure.
     * </p>
     * <p>
     * Note: this constructor defines a spherical/circular occulting body (see
     * {@link #buildOccultingBodies(double, Frame)}.<br>
     * The {@link #getLightningRatio(SpacecraftState, Frame, AbsoluteDate) lightning ratio} and the
     * {@link #getEventsDetectors() events detectors} computation will be optimized.
     * </p>
     * 
     * @param dRef
     *        Reference distance for the solar radiation pressure (m)
     * @param pRef
     *        Reference solar radiation pressure at dRef (N/m<sup>2</sup>)
     * @param sun
     *        Sun model
     * @param occultingBodyRadius
     *        Occulting body radius
     * @param occultingBodyFrame
     *        Occulting body-centered frame
     * @param spacecraft
     *        The physical and geometrical spacecraft representation
     * @param computePD
     *        {@code true} if the partial derivatives wrt position have to be computed, {@code false} otherwise
     */
    public SolarRadiationPressure(final double dRef, final double pRef, final PVCoordinatesProvider sun,
                                  final double occultingBodyRadius, final Frame occultingBodyFrame,
                                  final RadiationSensitive spacecraft, final boolean computePD) {
        this(new Parameter(REFERENCE_FLUX, pRef * dRef * dRef), sun, occultingBodyRadius, occultingBodyFrame,
                spacecraft, computePD);
    }

    /**
     * Complete constructor.
     * <p>
     * Note: this constructor defines a spherical/circular occulting body (see
     * {@link #buildOccultingBodies(double, Frame)}.<br>
     * The {@link #getLightningRatio(SpacecraftState, Frame, AbsoluteDate) lightning ratio} and the
     * {@link #getEventsDetectors() events detectors} computation will be optimized.
     * </p>
     * 
     * @param refFluxParam
     *        Normalized reference flux parameter for a 1m distance (N)
     * @param sun
     *        Sun model
     * @param occultingBodyRadius
     *        Occulting body radius
     * @param spacecraft
     *        The physical and geometrical spacecraft representation
     */
    public SolarRadiationPressure(final Parameter refFluxParam, final PVCoordinatesProvider sun,
                                  final double occultingBodyRadius, final RadiationSensitive spacecraft) {
        this(refFluxParam, sun, occultingBodyRadius, spacecraft, true);
    }

    /**
     * Complete constructor.
     * <p>
     * Note: this constructor defines a spherical/circular occulting body (see
     * {@link #buildOccultingBodies(double, Frame)}.<br>
     * The {@link #getLightningRatio(SpacecraftState, Frame, AbsoluteDate) lightning ratio} and the
     * {@link #getEventsDetectors() events detectors} computation will be optimized.
     * </p>
     * 
     * @param refFluxParam
     *        Normalized reference flux parameter for a 1m distance (N)
     * @param sun
     *        Sun model
     * @param occultingBodyRadius
     *        Occulting body radius
     * @param spacecraft
     *        The physical and geometrical spacecraft representation
     * @param computePD
     *        {@code true} if the partial derivatives wrt position have to be computed, {@code false} otherwise
     */
    public SolarRadiationPressure(final Parameter refFluxParam, final PVCoordinatesProvider sun,
                                  final double occultingBodyRadius, final RadiationSensitive spacecraft,
                                  final boolean computePD) {
        this(refFluxParam, sun, occultingBodyRadius, FramesFactory.getGCRF(), spacecraft, computePD);
    }

    /**
     * Complete constructor.
     * <p>
     * Note: this constructor defines a spherical/circular occulting body (see
     * {@link #buildOccultingBodies(double, Frame)}.<br>
     * The {@link #getLightningRatio(SpacecraftState, Frame, AbsoluteDate) lightning ratio} and the
     * {@link #getEventsDetectors() events detectors} computation will be optimized.
     * </p>
     * 
     * @param refFluxParam
     *        Normalized reference flux parameter for a 1m distance (N)
     * @param sun
     *        Sun model
     * @param occultingBodyRadius
     *        Occulting body radius
     * @param occultingBodyFrame
     *        Occulting body-centered frame
     * @param spacecraft
     *        The physical and geometrical spacecraft representation
     * @param computePD
     *        {@code true} if the partial derivatives wrt position have to be computed, {@code false} otherwise
     */
    public SolarRadiationPressure(final Parameter refFluxParam, final PVCoordinatesProvider sun,
                                  final double occultingBodyRadius, final Frame occultingBodyFrame,
                                  final RadiationSensitive spacecraft, final boolean computePD) {
        this(refFluxParam, sun, buildOccultingBodies(occultingBodyRadius, occultingBodyFrame), spacecraft, computePD);
    }

    /**
     * Simple constructor with default reference values.
     * <p>
     * When this constructor is used, the reference values are:
     * </p>
     * <ul>
     * <li>d<sub>ref</sub> = 149597870000.0 m</li>
     * <li>p<sub>ref</sub> = 4.560500000000001 10<sup>-6</sup> N/m<sup>2</sup></li>
     * </ul>
     *
     * @param sunBody
     *        Sun model
     * @param occultingBody
     *        Occulting body model (for umbra/penumbra computation)
     * @param spacecraftModel
     *        The physical and geometrical spacecraft representation
     */
    public SolarRadiationPressure(final PVCoordinatesProvider sunBody, final BodyShape occultingBody,
                                  final RadiationSensitive spacecraftModel) {
        this(sunBody, occultingBody, spacecraftModel, true);
    }

    /**
     * Simple constructor with default reference values.
     * <p>
     * When this constructor is used, the reference values are:
     * </p>
     * <ul>
     * <li>d<sub>ref</sub> = 149597870000.0 m</li>
     * <li>p<sub>ref</sub> = 4.560500000000001 10<sup>-6</sup> N/m<sup>2</sup></li>
     * </ul>
     *
     * @param sunBody
     *        Sun model
     * @param occultingBody
     *        Occulting body model (for umbra/penumbra computation)
     * @param spacecraftModel
     *        The physical and geometrical spacecraft representation
     * @param computePD
     *        {@code true} if the partial derivatives wrt position have to be computed, {@code false} otherwise
     */
    public SolarRadiationPressure(final PVCoordinatesProvider sunBody, final BodyShape occultingBody,
                                  final RadiationSensitive spacecraftModel, final boolean computePD) {
        this(Constants.SEIDELMANN_UA, Constants.CONST_SOL_N_M2, sunBody, occultingBody, spacecraftModel,
                computePD);
    }

    /**
     * Complete constructor.
     * <p>
     * Note that reference solar radiation pressure <code>pRef</code> in N/m<sup>2</sup> is linked to solar flux SF in
     * W/m<sup>2</sup> using formula pRef = SF/c where c is the speed of light (299792458 m/s). So at 1UA a 1367
     * W/m<sup>2</sup> solar flux is a 4.560500000000001 10<sup>-6</sup> N/m<sup>2</sup> solar radiation pressure.
     * </p>
     *
     * @param dRef
     *        Reference distance for the solar radiation pressure (m)
     * @param pRef
     *        Reference solar radiation pressure at dRef (N/m<sup>2</sup>)
     * @param sunBody
     *        Sun model
     * @param occultingBody
     *        Occulting body model (for umbra/penumbra computation)
     * @param spacecraftModel
     *        The physical and geometrical spacecraft representation
     */
    public SolarRadiationPressure(final double dRef, final double pRef, final PVCoordinatesProvider sunBody,
                                  final BodyShape occultingBody, final RadiationSensitive spacecraftModel) {
        this(dRef, pRef, sunBody, occultingBody, spacecraftModel, true);
    }

    /**
     * Complete constructor.
     * <p>
     * Note that reference solar radiation pressure <code>pRef</code> in N/m<sup>2</sup> is linked to solar flux SF in
     * W/m<sup>2</sup> using formula pRef = SF/c where c is the speed of light (299792458 m/s). So at 1UA a 1367
     * W/m<sup>2</sup> solar flux is a 4.560500000000001 10<sup>-6</sup> N/m<sup>2</sup> solar radiation pressure.
     * </p>
     *
     * @param dRef
     *        Reference distance for the solar radiation pressure (m)
     * @param pRef
     *        Reference solar radiation pressure at dRef (N/m<sup>2</sup>)
     * @param sunBody
     *        Sun model
     * @param occultingBody
     *        Occulting body model (for umbra/penumbra computation)
     * @param spacecraftModel
     *        The physical and geometrical spacecraft representation
     * @param computePD
     *        {@code true} if the partial derivatives wrt position have to be computed, {@code false} otherwise
     */
    public SolarRadiationPressure(final double dRef, final double pRef, final PVCoordinatesProvider sunBody,
                                  final BodyShape occultingBody, final RadiationSensitive spacecraftModel,
                                  final boolean computePD) {
        this(new Parameter(REFERENCE_FLUX, pRef * dRef * dRef), sunBody, occultingBody, spacecraftModel,
                computePD);
    }

    /**
     * Complete constructor using {@link Parameter}.
     *
     * @param referenceFlux
     *        The parameter representing the reference flux normalized for a 1m distance (N)
     * @param sunBody
     *        Sun model
     * @param occultingBody
     *        Occulting body model (for umbra/penumbra computation)
     * @param spacecraftModel
     *        The physical and geometrical spacecraft representation
     */
    public SolarRadiationPressure(final Parameter referenceFlux, final PVCoordinatesProvider sunBody,
                                  final BodyShape occultingBody, final RadiationSensitive spacecraftModel) {
        this(referenceFlux, sunBody, occultingBody, spacecraftModel, true);
    }

    /**
     * Complete constructor using {@link Parameter}.
     *
     * @param referenceFlux
     *        The parameter representing the reference flux normalized for a 1m distance (N)
     * @param sunBody
     *        Sun model
     * @param occultingBody
     *        Occulting body model (for umbra/penumbra computation)
     * @param spacecraftModel
     *        The physical and geometrical spacecraft representation
     * @param computePD
     *        {@code true} if the partial derivatives wrt position have to be computed, {@code false} otherwise
     */
    public SolarRadiationPressure(final Parameter referenceFlux, final PVCoordinatesProvider sunBody,
                                  final BodyShape occultingBody, final RadiationSensitive spacecraftModel,
                                  final boolean computePD) {
        super();

        // Reference flux normalized for a 1m distance (N)
        this.addParameter(referenceFlux);
        // Add all spacecraft parameters
        if (spacecraftModel != null) {
            this.addJacobiansParameter(spacecraftModel.getJacobianParameters());
        }
        // Enrich the parameters with the force model descriptor
        ParameterUtils.addFieldToParameters(getParameters(), StandardFieldDescriptors.FORCE_MODEL, this.getClass());

        // Store the inputs
        this.fluxParam = referenceFlux;
        this.sun = sunBody;
        this.spacecraft = spacecraftModel;
        this.computePartialDerivativesWrtPosition = computePD;

        // Initialize the cache values as empty
        this.cachedSatSunVector = Vector3D.NaN;
        this.cachedPosition = Vector3D.NaN;
        this.cachedDate = AbsoluteDate.PAST_INFINITY;
        this.cachedSatSunVector = Vector3D.NaN;

        // Add the main occulting body to the list
        this.occultingBodies = new ArrayList<>();
        this.occultingBodies.add(occultingBody);
    }

    /**
     * Creates a new instance.
     *
     * @param dRef
     *        Reference distance for the solar radiation pressure (m)
     * @param pRef
     *        Reference solar radiation pressure at dRef (N/m<sup>2</sup>)
     * @param sunBody
     *        Sun model
     * @param occultingBody
     *        Occulting body model (for umbra/penumbra computation)
     * @param assembly
     *        Assembly with aerodynamic properties
     * @param multiplicativeFactorIn
     *        Multiplicative factor
     */
    public SolarRadiationPressure(final double dRef, final double pRef, final PVCoordinatesProvider sunBody,
                                  final BodyShape occultingBody, final Assembly assembly,
                                  final double multiplicativeFactorIn) {
        this(dRef, pRef, sunBody, occultingBody, spacecraftModelFromAssembly(assembly, multiplicativeFactorIn));
        this.multiplicativeFactor = multiplicativeFactorIn;
    }

    /**
     * Creates a new instance from the data in another one but with a different assembly.
     *
     * @param otherInstance
     *        The other instance
     * @param assembly
     *        Assembly with aerodynamic properties
     */
    public SolarRadiationPressure(final SolarRadiationPressure otherInstance, final Assembly assembly) {
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
     * @param assembly
     *        Assembly with radiative properties
     * @param multiplicativeFactor
     *        Multiplicative factor
     * @return the {@link DirectRadiativeModel} object
     */
    private static DirectRadiativeModel spacecraftModelFromAssembly(final Assembly assembly,
                                                                    final double multiplicativeFactor) {
        return new DirectRadiativeModel(assembly, multiplicativeFactor);
    }

    /**
     * Compute radiation coefficient.
     *
     * @param s
     *        Spacecraft state
     * @param satSunVector
     *        Sat-Sun vector in spacecraft state frame
     * @return coefficient for acceleration computation
     * @exception PatriusException thrown if Sun position cannot be retrieved
     */
    private double computeRawP(final SpacecraftState s, final Vector3D satSunVector) throws PatriusException {
        final AbsoluteDate date = s.getDate();
        final Frame frame = s.getFrame();
        final double r2 = satSunVector.getNormSq();
        return this.fluxParam.getValue() * getGlobalLightningRatio(satSunVector, s.getOrbit(), frame, date) / r2;
    }

    /**
     * Compute solar flux.
     *
     * @param s
     *        Spacecraft state
     * @return coefficient for acceleration computation
     * @exception PatriusException thrown if Sun position cannot be retrieved
     */
    public Vector3D getSolarFlux(final SpacecraftState s) throws PatriusException {
        final Vector3D satSunVector = this.getSatSunVector(s);
        final double rawP = this.computeRawP(s, satSunVector);
        return new Vector3D(MathLib.divide(-rawP, satSunVector.getNorm()), satSunVector);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D computeAcceleration(final SpacecraftState s) throws PatriusException {
        final Vector3D flux = this.getSolarFlux(s);
        final Vector3D acc;
        if (flux.getNorm() > Precision.EPSILON) {
            acc = this.spacecraft.radiationPressureAcceleration(s, flux);
        } else {
            acc = Vector3D.ZERO;
        }

        return acc;
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
     * In case of multiple occulting bodies, the assumption is made that only one body occults the spacecraft at a time.
     * </p>
     *
     * @param satSunVector
     *        Sat-Sun vector in spacecraft state frame
     * @param pv
     *        Satellite's position-velocity
     * @param frame
     *        Frame in which is defined the position
     * @param date
     *        Date
     * @return lightning ratio
     * @exception PatriusException
     *            if an error occurs
     */
    private double getGlobalLightningRatio(final Vector3D satSunVector, final PVCoordinatesProvider pv,
                                           final Frame frame, final AbsoluteDate date) throws PatriusException {

        // Full computation

        double lightningRatio = 1.;

        for (int i = 0; i < this.occultingBodies.size(); i++) {
            final double result = getLightningRatio(satSunVector, this.occultingBodies.get(i), pv, frame, date);

            // Update total lightning ratio
            // Assumption: occulting bodies are not superposed from satellite point of view
            lightningRatio = MathLib.min(result, lightningRatio);
        }

        return lightningRatio;
    }

    /**
     * Get the lightning ratio ([0-1]) for provided occulting body.
     *
     * @param satSunVector
     *        Sat-Sun vector in spacecraft state frame
     * @param occultingBody
     *        Occulting body
     * @param pv
     *        Satellite's position-velocity
     * @param frame
     *        Frame in which is defined the position
     * @param date
     *        Date
     * @return lightning ratio
     * @exception PatriusException
     *            if an error occurs
     */
    public double getLightningRatio(final Vector3D satSunVector, final BodyShape occultingBody,
                                    final PVCoordinatesProvider pv, final Frame frame, final AbsoluteDate date)
        throws PatriusException {

        if (!this.eclipsesComputationFlag) {
            // No eclipse computation: lightning ratio is 1
            return 1.;
        }

        // Full computation

        // Get position in occulting body frame
        final Transform t = frame.getTransformTo(occultingBody.getBodyFrame(), date);
        final Vector3D posBodyFrame = t.transformPosition(pv.getPVCoordinates(date, frame).getPosition());
        final Vector3D satSunVectorBodyFrame = t.transformVector(satSunVector);

        // Occulting body apparent radius
        final double r = posBodyFrame.getNorm();
        final double occultingRadius = occultingBody.getApparentRadius(pv, date,
            this.sun, PropagationDelayType.INSTANTANEOUS);
        final double value = MathLib.divide(occultingRadius, r);
        final double alphaOccultingBody = MathLib.asin(MathLib.min(1.0, MathLib.max(-1.0, value)));

        // Definition of the Sun's apparent radius
        final double value2 = MathLib.divide(SUN_RADIUS, satSunVectorBodyFrame.getNorm());
        final double alphaSun = MathLib.asin(MathLib.min(1.0, value2));

        // Sat-occulting body vector
        final Vector3D satBodyVector = posBodyFrame.negate();

        // Retrieve the Sat-Sun / Sat-Central body angle
        final double sunOccultingBodyAngle = Vector3D.angle(satSunVectorBodyFrame, satBodyVector);

        double lightningRatio = 1.;

        // Is the satellite in complete umbra ?
        if (sunOccultingBodyAngle - alphaOccultingBody + alphaSun <= 0.0) {
            lightningRatio = 0.;
        } else if (sunOccultingBodyAngle == 0) {
            // Satellite behind occulting body and exactly in line with occulted and occulting bodies
            lightningRatio = 0.;
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

            lightningRatio = 1. - MathLib.divide(p1 + p2, FastMath.PI * aS2);
        }

        return lightningRatio;
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
        if (!this.eclipsesComputationFlag) {
            // No occultation: no eclipse
            return new EventDetector[0];
        }

        // List of event detectors
        final List<EventDetector> detectors = new ArrayList<>();
        // Add detectors for each occulting bodies
        for (int i = 0; i < this.occultingBodies.size(); i++) {
            EventDetector umbra = null;
            EventDetector penumbra = null;
            final BodyShape occultingBody = this.occultingBodies.get(i);

            umbra = new EclipseDetector(this.sun, Constants.SUN_RADIUS, occultingBody, 0., MAX_CHECK, THRESHOLD){
                /** Serializable UID. */
                private static final long serialVersionUID = 1190059096406524800L;

                /** {@inheritDoc} */
                @Override
                public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                    return Action.RESET_DERIVATIVES;
                }
            };
            penumbra = new EclipseDetector(this.sun, Constants.SUN_RADIUS, occultingBody, 1., MAX_CHECK, THRESHOLD){
                /** Serializable UID. */
                private static final long serialVersionUID = 7357417667871064338L;

                /** {@inheritDoc} */
                @Override
                public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                    return Action.RESET_DERIVATIVES;
                }
            };
            detectors.add(umbra);
            detectors.add(penumbra);
        }

        return detectors.toArray(new EventDetector[2 * this.occultingBodies.size()]);
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
            final double cAlpha = getGlobalLightningRatio(satSunVector, s.getOrbit(), s.getFrame(), s.getDate());

            final double a = MathLib.divide(this.fluxParam.getValue() * cAlpha, satSunVector.getNormSq());

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
        final double cAlpha = getGlobalLightningRatio(satSunVector, s.getOrbit(), s.getFrame(), s.getDate());

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
    public List<BodyShape> getOccultingBodies() {
        return this.occultingBodies;
    }

    /**
     * Getter for the multiplicative factor.
     * 
     * @return the multiplicative factor
     */
    public double getMultiplicativeFactor() {
        return this.multiplicativeFactor;
    }

    /**
     * Getter for the parameter representing the reference flux normalized for a 1m distance (N).
     *
     * @return the normlized reference flux parameter
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
     * 
     * @param eclipsesComputationFlagIn
     *        True if eclipses should be taken into account, false otherwise
     */
    public void setEclipsesComputation(final boolean eclipsesComputationFlagIn) {
        this.eclipsesComputationFlag = eclipsesComputationFlagIn;
    }

    /**
     * Returns flag indicating if eclipses should be taken into account.
     * 
     * @return flag indicating if eclipses should be taken into account
     */
    public boolean isEclipseComputation() {
        return this.eclipsesComputationFlag;
    }

    /**
     * Add an occulting body.
     * 
     * @param body
     *        Occulting body to add
     */
    public final void addOccultingBody(final BodyShape body) {
        this.occultingBodies.add(body);
    }

    /**
     * Build a {@link BodyShape} from a radius and a frame.<br>
     * The body is defined as circular/spherical (will enable performance optimization).
     * 
     * @param occultingBodyRadius
     *        Occulting body radius
     * @param occultingBodyFrame
     *        Occulting body-centered frame
     * @return the occulting body
     */
    private static BodyShape buildOccultingBodies(final double occultingBodyRadius, final Frame occultingBodyFrame) {
        return new OneAxisEllipsoid(occultingBodyRadius, 0., occultingBodyFrame, "CircularBody");
    }
}
