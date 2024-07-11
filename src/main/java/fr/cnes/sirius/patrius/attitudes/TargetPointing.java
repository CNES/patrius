/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:FA:FA-2464:27/05/2020:Anomalie dans le calcul du vecteur rotation des LOF
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:87:05/08/2013:broken the relationship between this class and GroundPointing
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:06/010/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:583:11/03/2016:simplification of attitude laws architecture
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.TimeStampedPVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.TimeStampedAngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class handles target pointing attitude provider.
 * <p>
 * This class represents the attitude provider where the satellite z axis is pointing to a ground point target.
 * </p>
 * <p>
 * The target position is defined in a body frame specified by the user. It is important to make sure this frame is
 * consistent.
 * </p>
 * <p>
 * The object <code>TargetPointing</code> is guaranteed to be immutable.
 * </p>
 * 
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class TargetPointing extends AbstractAttitudeLaw {

    /** Serializable UID. */
    private static final long serialVersionUID = -8002434923471977301L;

    /** J axis. */
    private static final PVCoordinates PLUS_J =
        new PVCoordinates(Vector3D.PLUS_J, Vector3D.ZERO, Vector3D.ZERO);

    /** K axis. */
    private static final PVCoordinates PLUS_K =
        new PVCoordinates(Vector3D.PLUS_K, Vector3D.ZERO, Vector3D.ZERO);

    /** Body frame. */
    private final Frame bodyFrame;

    /** Target in body frame. */
    private final PVCoordinates target;

    /**
     * Creates a new instance from body frame and target expressed in cartesian coordinates.
     * 
     * @param bodyFrameIn
     *        body frame.
     * @param targetIn
     *        target position in body frame
     */
    public TargetPointing(final Frame bodyFrameIn, final Vector3D targetIn) {
        super();
        this.bodyFrame = bodyFrameIn;
        this.target = new PVCoordinates(targetIn, Vector3D.ZERO);
    }

    /**
     * Creates a new instance from body shape and target expressed in geodetic coordinates.
     * 
     * @param targetGeo
     *        target defined as a geodetic point in body shape frame
     * @param shape
     *        body shape
     */
    public TargetPointing(final GeodeticPoint targetGeo, final BodyShape shape) {
        super();
        this.bodyFrame = shape.getBodyFrame();
        // Transform target from geodetic coordinates to position-velocity coordinates
        this.target = new PVCoordinates(shape.transform(targetGeo), Vector3D.ZERO);
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
        if (deltaP0.getPosition().isZero()) {
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

        final TimeStampedAngularCoordinates ac =
            new TimeStampedAngularCoordinates(date, los, normal, PLUS_K, PLUS_J, 1.0e-9,
                this.getSpinDerivativesComputation());

        // Transform in frame :
        return new Attitude(eme2000, ac).withReferenceFrame(frame, this.getSpinDerivativesComputation());
    }

    /**
     * Private method to get target PV.
     * 
     * @param pvProv
     *        local position-velocity provider around current date
     * @param date
     *        the date to compute rotation
     * @param frame
     *        reference frame from which rotation is computed
     * @return the rotation of reference frame at the given date for the given position-velocity state
     * @throws PatriusException
     *         if target PV cannot be computed
     */
    private TimeStampedPVCoordinates getTargetPV(final PVCoordinatesProvider pvProv,
                                                 final AbsoluteDate date, final Frame frame) throws PatriusException {
        final Transform t = this.bodyFrame.getTransformTo(frame, date);
        final TimeStampedPVCoordinates pv =
            new TimeStampedPVCoordinates(date, this.target.getPosition(), Vector3D.ZERO, Vector3D.ZERO);
        return t.transformPVCoordinates(pv);
    }
}
