/**
 * Copyright 2023-2023 CNES
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
 * VERSION:4.13.1:FA:FA-176:17/01/2024:[PATRIUS] Reliquat OPENFD
 * VERSION:4.13:DM:DM-101:08/12/2023:[PATRIUS] Harmonisation des eclipses pour les evenements et pour la PRS
 * VERSION:4.13:DM:DM-37:08/12/2023:[PATRIUS] Date d'evenement et propagation du signal
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.radiation;

import java.io.Serializable;

import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.events.detectors.EclipseDetector;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.signalpropagation.VacuumSignalPropagationModel;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Class computing the lighting ratio in the interval [0; 1], used for {@link EclipseDetector} and
 * {@link SolarRadiationPressure}:
 * <ul>
 * <li>0: occulted body is entirely hidden by occulting body</li>
 * <li>1: occulted body is fully visible from object.</li>
 * <li>Between 0 and 1: occulted body is partly visible from object.</li>
 * </ul>
 * <p>
 * The lighting ratio is the percentage of occulted body visible from spacecraft.
 * </p>
 * <p>
 * Signal propagation can be taken into account.
 * </p>
 * <p>
 * Computation hypothesis:
 * <ul>
 * <li>Occulted body is spherical</li>
 * <li>Occulting body can have any shape, but computation is made using a sphere of radius the
 * "apparent occulting radius" which is the angular radius of the occulting body in the direction of occulted body.
 * </li>
 * </ul>
 * </p>
 *
 * @author Emmanuel Bignon
 *
 * @since 4.13
 */
public class LightingRatio implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -5542959128461000171L;

    /** Occulting body. */
    private final BodyShape occultingBody;

    /** Occulted body. */
    private final PVCoordinatesProvider occultedBody;

    /** Occulted body radius. */
    private final double occultedRadius;

    /** Propagation delay type (initialized to {@link PropagationDelayType#INSTANTANEOUS} by default). */
    private PropagationDelayType propagationDelayType = PropagationDelayType.INSTANTANEOUS;

    /** Inertial frame for signal propagation computation. */
    private Frame inertialFrame;

    /**
     * Epsilon for signal propagation computation (initialized to {@link VacuumSignalPropagationModel#DEFAULT_THRESHOLD}
     * by default).
     */
    private double epsSignalPropagation = VacuumSignalPropagationModel.DEFAULT_THRESHOLD;

    /**
     * Maximum number of iterations for signal propagation computation (initialized to
     * {@link VacuumSignalPropagationModel#DEFAULT_MAX_ITER} by default).
     */
    private int maxIterSignalPropagation = VacuumSignalPropagationModel.DEFAULT_MAX_ITER;

    /**
     * Constructor.
     * 
     * @param occultingBody
     *        occulting body
     * @param occultedBody
     *        occulted body PV
     * @param occultedRadius
     *        occulted body radius
     */
    public LightingRatio(final BodyShape occultingBody, final PVCoordinatesProvider occultedBody,
                         final double occultedRadius) {
        this.occultingBody = occultingBody;
        this.occultedBody = occultedBody;
        this.occultedRadius = occultedRadius;
    }

    /**
     * Compute lighting ratio.
     * <ul>
     * <li>0: occulted body is entirely hidden by occulting body</li>
     * <li>1: occulted body is fully visible from object.</li>
     * <li>Between 0 and 1: occulted body is partly visible from object.</li>
     * </ul>
     * 
     * @param pv
     *        pv of spacecraft
     * @param date
     *        spacecraft date
     * @return the lighting ratio in [0; 1]
     * @throws PatriusException
     *         if computation failed
     */
    public double compute(final PVCoordinatesProvider pv, final AbsoluteDate date) throws PatriusException {
        return MathLib.max(0., MathLib.min(1., computeExtended(pv, date)));
    }

    /**
     * Compute extended lighting ratio.
     * <ul>
     * <li>Smaller than 0: occulted body is entirely hidden by occulting body. Lighting ratio set to angle to start
     * partial eclipse from total eclipse (negative value).</li>
     * <li>larger than 1: occulted body is fully visible from object. Lighting ratio set 1 + angle to start partial
     * eclipse from no eclipse.</li>
     * <li>Between 0 and 1: occulted body is partly visible from object.</li>
     * </ul>
     * 
     * @param pv
     *        pv of spacecraft
     * @param date
     *        spacecraft date
     * @return the extended lighting ratio
     * @throws PatriusException
     *         if computation failed
     */
    public double computeExtended(final PVCoordinatesProvider pv, final AbsoluteDate date) throws PatriusException {

        // Computation frame
        final Frame referenceFrame;
        if (getPropagationDelayType().equals(PropagationDelayType.LIGHT_SPEED)) {
            // Case of light speed propagation
            referenceFrame = this.inertialFrame;
        } else {
            // Instantaneous case
            referenceFrame = pv.getNativeFrame(date);
        }

        // Dates taking into account signal propagation
        final AbsoluteDate occultedDate = VacuumSignalPropagationModel.getSignalEmissionDate(this.occultedBody, pv,
            date, this.epsSignalPropagation, this.propagationDelayType, referenceFrame, this.maxIterSignalPropagation);
        final AbsoluteDate occultingDate = VacuumSignalPropagationModel.getSignalEmissionDate(this.occultingBody, pv,
            date, this.epsSignalPropagation, this.propagationDelayType, referenceFrame, this.maxIterSignalPropagation);

        // Compute positions in reference frame
        final Vector3D satPos = pv.getPVCoordinates(date, referenceFrame).getPosition();
        final Vector3D occultedPos = this.occultedBody.getPVCoordinates(occultedDate, referenceFrame).getPosition();
        final Vector3D occultingPos = this.occultingBody.getPVCoordinates(occultingDate, referenceFrame).getPosition();

        // Difference vectors
        final Vector3D satOccultedVector = occultedPos.subtract(satPos);
        final Vector3D satOccultingVector = occultingPos.subtract(satPos);
        final double satOccultedNorm = satOccultedVector.getNorm();

        // Specific case: when the occulted body - satellite distance is smaller than the occulting body - satellite
        // distance, eclipse should never happen: lightingRatio is 1
        // Note: arbitrary add +PI to extend the lighting ratio
        if (satOccultedNorm < satOccultingVector.getNorm()) {
            return 1. + MathLib.PI;
        }

        // Occulted body apparent radius
        final double value2 = MathLib.divide(this.occultedRadius, satOccultedNorm);
        final double alphaOccultedBody = MathLib.asin(MathLib.min(1.0, value2));

        // Occulting body apparent radius
        final double satOccultingNorm = satOccultingVector.getNorm();
        final double occultingRadius =
            this.occultingBody.getApparentRadius(pv, date, this.occultedBody, getPropagationDelayType());
        final double value = MathLib.divide(occultingRadius, satOccultingNorm);
        final double alphaOccultingBody = MathLib.asin(MathLib.min(1.0, MathLib.max(-1.0, value)));

        // Sat-Occulted / Sat-Occulting angle
        final double occultedOccultingAngle = Vector3D.angle(satOccultedVector, satOccultingVector);

        // Compute lighting ratio in [0, 1]
        // occultedOccultingAngle >= alphaOccultingBody + alphaOccultedBody means occulted body entirely visible
        final double lightingRatio;

        // 4 cases:
        // - Total eclipse
        // - Annular eclipse
        // - Partial eclipse
        // - No eclipse
        if (occultedOccultingAngle - alphaOccultingBody + alphaOccultedBody <= 0.0) {
            // Total eclipse - Occulted body smaller than occulting body
            lightingRatio = 0.;
        } else if (occultedOccultingAngle + alphaOccultingBody - alphaOccultedBody <= 0) {
            // Annular eclipse - Occulted body larger than occulting body
            // Occulting body projection is "inside" the occulted body
            // Computation under an hypothesis of spherical apparent radius
            // alphaOccultedBody cannot be 0 at this point
            final double alphaOccultingBody2 = alphaOccultingBody * alphaOccultingBody;
            final double alphaOccultedBody2 = alphaOccultedBody * alphaOccultedBody;
            lightingRatio = (alphaOccultedBody2 - alphaOccultingBody2) / alphaOccultedBody2;
        } else if (occultedOccultingAngle - alphaOccultingBody - alphaOccultedBody < 0.0) {
            // Partial eclipse
            // alphaOccultedBody cannot be 0 at this point, so is aS2
            // alphaOccultingBody cannot be 0 at this point
            // occultedOccultingAngle cannot be 0 at this point (perfect alignment)
            final double sEA2 = occultedOccultingAngle * occultedOccultingAngle;
            final double oo2sEA = 1.0 / (2. * occultedOccultingAngle);
            final double aS2 = alphaOccultedBody * alphaOccultedBody;
            final double aE2 = alphaOccultingBody * alphaOccultingBody;
            final double aE2maS2 = aE2 - aS2;

            final double alpha1 = (sEA2 - aE2maS2) * oo2sEA;
            final double alpha2 = (sEA2 + aE2maS2) * oo2sEA;

            // Protection against numerical inaccuracy at boundaries
            final double a1oaS = MathLib.min(1.0, MathLib.max(-1.0, alpha1 / alphaOccultedBody));
            final double aS2ma12 = MathLib.max(0.0, aS2 - alpha1 * alpha1);
            final double a2oaE = MathLib.min(1.0, MathLib.max(-1.0, alpha2 / alphaOccultingBody));
            final double aE2ma22 = MathLib.max(0.0, aE2 - alpha2 * alpha2);

            final double p1 = aS2 * MathLib.acos(a1oaS) - alpha1 * MathLib.sqrt(aS2ma12);
            final double p2 = aE2 * MathLib.acos(a2oaE) - alpha2 * MathLib.sqrt(aE2ma22);

            // Some numerical inaccuracies may lead to a lighting ratio not in [0; 1] which is impossible
            lightingRatio = MathLib.max(0., MathLib.min(1., 1. - MathLib.divide(p1 + p2, MathLib.PI * aS2)));
        } else {
            // No eclipse
            lightingRatio = 1.;
        }

        // Extend lighting ratio
        double extendedLightingRatio = lightingRatio;
        if (lightingRatio == 0) {
            // Lighting ratio set to angle to start partial eclipse from total eclipse (negative value)
            final double umbraAngle = occultedOccultingAngle - alphaOccultingBody + alphaOccultedBody;
            extendedLightingRatio = 0 + umbraAngle;
        }
        if (lightingRatio == 1) {
            // Lighting ratio set 1 + angle to start partial eclipse from no eclipse
            final double penumbraAngle = occultedOccultingAngle - alphaOccultingBody - alphaOccultedBody;
            extendedLightingRatio = 1 + penumbraAngle;
        }

        return extendedLightingRatio;
    }

    /**
     * Getter for the propagation delay type.
     * 
     * @return the propagation delay type
     */
    public PropagationDelayType getPropagationDelayType() {
        return this.propagationDelayType;
    }

    /**
     * Setter for the propagation delay computation type. Warning: check Javadoc of detector to see if detector takes
     * into account propagation time delay. if not, signals are always considered instantaneous. The provided frame is
     * used to compute the signal propagation when delay is taken into account.
     * 
     * @param propagationDelayTypeIn
     *        Propagation delay type used in events computation
     * @param frameIn
     *        Frame to use for signal propagation with delay (may be null if propagation delay type is
     *        considered instantaneous). Warning: the usage of a pseudo inertial frame is tolerated, however it will
     *        lead to some inaccuracies due to the non-invariance of the frame with respect to time. For this reason,
     *        it is suggested to use the ICRF frame or a frame which is frozen with respect to the ICRF.
     * @throws IllegalArgumentException
     *         if the provided frame is not pseudo inertial.
     */
    public void setPropagationDelayType(final PropagationDelayType propagationDelayTypeIn, final Frame frameIn) {

        // check whether the provided frame is pseudo inertial or not
        if (frameIn != null && !frameIn.isPseudoInertial()) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_NOT_INERTIAL_FRAME, frameIn);
        }
        this.propagationDelayType = propagationDelayTypeIn;
        this.inertialFrame = frameIn;
    }

    /**
     * Getter for the inertial frame used for signal propagation computation.
     * 
     * @return the inertial frame
     */
    public Frame getInertialFrame() {
        return this.inertialFrame;
    }

    /**
     * Getter for the epsilon for signal propagation when signal propagation is taken into account.
     * 
     * @return the epsilon for signal propagation when signal propagation is taken into account
     */
    public double getEpsilonSignalPropagation() {
        return this.epsSignalPropagation;
    }

    /**
     * Setter for the epsilon for signal propagation when signal propagation is taken into account.<br>
     * This epsilon (in s) directly reflect the accuracy of signal propagation (1s of accuracy = 3E8m of accuracy on
     * distance between emitter and receiver)
     * 
     * @param epsilon
     *        Epsilon for the signal propagation
     */
    public void setEpsilonSignalPropagation(final double epsilon) {
        this.epsSignalPropagation = epsilon;
    }

    /**
     * Getter for the maximum number of iterations for signal propagation when signal propagation is taken into account.
     * 
     * @return the maximum number of iterations for signal propagation
     */
    public int getMaxIterSignalPropagation() {
        return this.maxIterSignalPropagation;
    }

    /**
     * Setter for the maximum number of iterations for signal propagation when signal propagation is taken into account.
     * 
     * @param maxIterSignalPropagationIn
     *        Maximum number of iterations for signal propagation
     */
    public void setMaxIterSignalPropagation(final int maxIterSignalPropagationIn) {
        this.maxIterSignalPropagation = maxIterSignalPropagationIn;
    }
}
