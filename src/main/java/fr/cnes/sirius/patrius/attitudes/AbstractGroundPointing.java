/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
/*
 * HISTORY
* VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2799:18/05/2021:Suppression des pas de temps fixes codes en dur 
 * VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
 * VERSION:4.6:DM:DM-2603:27/01/2021:[PATRIUS] Ajout de getters pour les 2 LOS de la classe AbstractGroundPointing 
 * VERSION:4.6:FA:FA-2741:27/01/2021:[PATRIUS] Chaine de transformation de repere non optimale dans MSIS2000
 * VERSION:4.6:FA:FA-2692:27/01/2021:[PATRIUS] Robustification de AbstractGroundPointing dans le cas de vitesses 
 * non significatives 
 * VERSION:4.5:FA:FA-2464:27/05/2020:Anomalie dans le calcul du vecteur rotation des LOF
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION:4.3:DM:DM-2104:15/05/2019:[Patrius] Rendre generiques les classes GroundPointing et NadirPointing
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:87:05/08/2013:added the GeometricBodyShape attribute
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:583:11/03/2016:simplification of attitude laws architecture
 * VERSION::FA:648:27/07/2016:Corrected minor points staying from V3.2
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.TimeStampedPVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.TimeStampedAngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Base class for ground pointing attitude providers.
 * 
 * <p>
 * This class is a basic model for different kind of ground pointing attitude providers, such as : body center pointing,
 * nadir pointing, target pointing, etc...
 * </p>
 * <p>
 * The object <code>GroundPointing</code> is guaranteed to be immutable.
 * </p>
 * 
 * @see AttitudeProvider
 * @author V&eacute;ronique Pommier-Maurussane
 */
@SuppressWarnings("PMD.NullAssignment")
public abstract class AbstractGroundPointing extends AbstractAttitudeLaw {

    /** Serializable UID. */
    private static final long serialVersionUID = -1459257023765594793L;

    /** Default threshold. */
    private static final double DEFAULT_THRESHOLD = 1.0e-9;

    /** Default value for delta-T used to compute target velocity by finite differences. */
    private static final double DEFAULT_TARGET_VELOCITY_DELTAT = 0.1;

    /** LOS in satellite frame axis. */
    private final PVCoordinates losInSatFrame;

    /** LOS normal axis in satellite frame. */
    private final PVCoordinates losNormalInSatFrame;

    /** Body frame. */
    private final Frame bodyFrame;

    /** Body shape. */
    private final BodyShape shape;

    /** The delta-T used to compute target velocity by finite differences. */
    private final double targetVelocityDeltaT;

    /**
     * Default constructor. Build a new instance with arbitrary default elements.
     * <p>
     * By default, the satellite frame is set as :
     * <ul>
     * <li>LOS axis : {@link Vector3D#PLUS_K K}</li>
     * <li>LOS normal axis : {@link Vector3D#PLUS_J J}</li>
     * </ul>
     * </p>
     * 
     * @param shapeIn the body shape
     */
    protected AbstractGroundPointing(final BodyShape shapeIn) {
        this(shapeIn, Vector3D.PLUS_K, Vector3D.PLUS_J);
    }

    /**
     * Simple constructor. Build a new instance with specified LOS axis in satellite frame.
     * 
     * @param shapeIn the body shape
     * @param losInSatFrameVec LOS in satellite frame axis
     * @param losNormalInSatFrameVec LOS normal axis in satellite frame
     */
    protected AbstractGroundPointing(final BodyShape shapeIn, final Vector3D losInSatFrameVec,
                                     final Vector3D losNormalInSatFrameVec) {
        this(shapeIn, losInSatFrameVec, losNormalInSatFrameVec, DEFAULT_TARGET_VELOCITY_DELTAT);
    }

    /**
     * Simple constructor. Build a new instance with specified LOS axis in satellite frame.
     * 
     * @param shapeIn the body shape
     * @param losInSatFrameVec LOS in satellite frame axis
     * @param losNormalInSatFrameVec LOS normal axis in satellite frame
     * @param targetVelocityDeltaT the delta-T used to compute target velocity by finite differences
     */
    protected AbstractGroundPointing(final BodyShape shapeIn, final Vector3D losInSatFrameVec,
                                     final Vector3D losNormalInSatFrameVec,
                                     final double targetVelocityDeltaT) {
        super();
        this.losInSatFrame = new PVCoordinates(losInSatFrameVec, Vector3D.ZERO, Vector3D.ZERO);
        this.losNormalInSatFrame = new PVCoordinates(losNormalInSatFrameVec, Vector3D.ZERO,
            Vector3D.ZERO);
        this.bodyFrame = shapeIn.getBodyFrame();
        this.shape = shapeIn;
        this.targetVelocityDeltaT = targetVelocityDeltaT;
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                final AbsoluteDate date, final Frame frame) throws PatriusException {

        // inertial frame
        final Frame eme2000 = FramesFactory.getEME2000();

        // satellite-target relative vector
        PVCoordinates pv0 = pvProv.getPVCoordinates(date, eme2000);
        if (pv0.getAcceleration() == null) {
            // Set acceleration to zero in case it has not been provided
            pv0 = new PVCoordinates(pv0.getPosition(), pv0.getVelocity(), Vector3D.ZERO);
        }
        final TimeStampedPVCoordinates deltaP0 =
            new TimeStampedPVCoordinates(date, pv0, this.getTargetPV(pvProv, date, eme2000));

        // New orekit exception if null position.
        if (deltaP0.getPosition().getNorm() < Precision.EPSILON) {
            throw new PatriusException(PatriusMessages.SATELLITE_COLLIDED_WITH_TARGET);
        }

        // attitude definition:
        // line of sight -> +z satellite axis,
        // orbital velocity -> (z, +x) half plane
        final Vector3D p = pv0.getPosition();
        final Vector3D v = pv0.getVelocity();
        final Vector3D a = pv0.getAcceleration();
        final double r2 = p.getNormSq();
        final double r = MathLib.sqrt(r2);
        final Vector3D keplerianJerk = new Vector3D(-3 * Vector3D.dotProduct(p, v) / r2, a, -a.getNorm() / r, v);
        final PVCoordinates velocity = new PVCoordinates(v, a, keplerianJerk);

        final PVCoordinates los = deltaP0.normalize();
        final PVCoordinates normal = PVCoordinates.crossProduct(deltaP0, velocity).normalize();

        TimeStampedAngularCoordinates ac;
        try {
            ac = new TimeStampedAngularCoordinates(date, los, normal, this.losInSatFrame, this.losNormalInSatFrame,
                DEFAULT_THRESHOLD, getSpinDerivativesComputation());
        } catch (final PatriusException e) {
            // Problem in computing spin
            // Do not take velocities into account
            final Rotation rot = new Rotation(this.losInSatFrame.getPosition(), this.losNormalInSatFrame.getPosition(),
                los.getPosition(), normal.getPosition());
            ac = new TimeStampedAngularCoordinates(date, new AngularCoordinates(rot, Vector3D.ZERO));
        }

        // Transform in new frame
        return new Attitude(eme2000, ac).withReferenceFrame(frame, this.getSpinDerivativesComputation());
    }

    /**
     * Compute the target ground point position in specified frame.
     * 
     * @param pvProv
     *        provider for PV coordinates
     * @param date
     *        date at which target point is requested
     * @param frame
     *        frame in which observed ground point should be provided
     * @return observed ground point position in specified frame
     * @throws PatriusException
     *         if some specific error occurs,
     *         such as no target reached
     */
    protected abstract Vector3D getTargetPosition(final PVCoordinatesProvider pvProv,
                                                  final AbsoluteDate date, final Frame frame) throws PatriusException;

    /**
     * Compute the target point position/velocity in specified frame.
     * <p>
     * The default implementation use a simple centered two points finite differences scheme, it may be replaced by more
     * accurate models in specialized implementations.
     * </p>
     * In the case where the point is computed at a date corresponding to the lower/upper bound of
     * the ephemeris, the finite differences scheme used is either 1st order forward or backward.
     * <p>
     * The calculated intersection velocity corresponds to the "virtual" intersection point velocity between the
     * satellite los and the Earth, not the velocity of the physical point.
     * </p>
     * 
     * @param pvProv
     *        provider for PV coordinates
     * @param date
     *        date at which target point is requested
     * @param frame
     *        frame in which observed ground point should be provided
     * @return observed ground point position/velocity in specified frame
     * @throws PatriusException
     *         if some specific error occurs,
     *         such as no target reached
     */
    protected TimeStampedPVCoordinates getTargetPV(final PVCoordinatesProvider pvProv,
                                                   final AbsoluteDate date, final Frame frame) throws PatriusException {

        // target point position in same frame as initial pv
        final Vector3D intersectionP = this.getTargetPosition(pvProv, date, frame);

        // velocity of target point due to satellite and target motions
        final double h = this.targetVelocityDeltaT;
        // calculate scale with velocity h
        double scale = 1.0 / (2 * h);
        Vector3D intersectionM1h;
        Vector3D intersectionP1h;

        // Manage the eventual bound of the ephemeris
        try {
            // try to compute the target point M1h
            intersectionM1h = this.getTargetPosition(pvProv, date.shiftedBy(-h), frame);
        } catch (final PropagationException exc) {
            intersectionM1h = this.getTargetPosition(pvProv, date, frame);
            scale = 1.0 / h;
        }

        try {
            // try to compute the target point M1h
            intersectionP1h = this.getTargetPosition(pvProv, date.shiftedBy(h), frame);
        } catch (final PropagationException exc) {
            intersectionP1h = this.getTargetPosition(pvProv, date, frame);
            scale = 1.0 / h;
        }

        final Vector3D intersectionV = new Vector3D(scale, intersectionP1h, -scale, intersectionM1h);
        return new TimeStampedPVCoordinates(date, new PVCoordinates(intersectionP, intersectionV,
            getSpinDerivativesComputation() ? Vector3D.ZERO : null));
    }

    /**
     * Getter for the body frame.
     * 
     * @return body frame
     */
    public final Frame getBodyFrame() {
        return this.bodyFrame;
    }

    /**
     * Getter for the body shape.
     * 
     * @return body shape
     */
    public final BodyShape getBodyShape() {
        return this.shape;
    }

    /**
     * Getter for the LOS in satellite frame axis.
     * 
     * @return the LOS in satellite frame axis
     */
    public PVCoordinates getLosInSatFrame() {
        return this.losInSatFrame;
    }

    /**
     * Getter for the LOS normal axis in satellite frame.
     * 
     * @return the LOS normal axis in satellite frame
     */
    public PVCoordinates getLosNormalInSatFrame() {
        return this.losNormalInSatFrame;
    }
}
