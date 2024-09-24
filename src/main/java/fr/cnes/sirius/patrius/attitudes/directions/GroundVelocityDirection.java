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
 * 
 * @history creation 21/06/2012
 *
 * HISTORY
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3174:10/05/2022:[PATRIUS] Corriger les differences de convention...
 * VERSION:4.9:DM:DM-3135:10/05/2022:[PATRIUS] Calcul d'intersection sur GeometricBodyShape  
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::FA:185:11/04/2014:the getLine method has been modified
 * VERSION::FA:261:13/10/2014:JE V2.2 corrections (move IDirection in package attitudes.directions)
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:12/01/2016:Correction of the velocity computation sign.
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.directions;

import fr.cnes.sirius.patrius.bodies.BodyPoint;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Ground velocity direction. This direction depends on the location of the target point on the ground surface and
 * therefore it depends on the pointing direction and the body shape. The intersection between the pointing direction
 * and the body shape defines the target point.
 * 
 * @concurrency conditionally thread-safe
 * 
 * @concurrency.comment The use of a celestial body linked to the tree of frames
 *                      makes this class thread-safe only if the underlying CelestialBody is too.
 * 
 * @author Julie Anton
 * 
 * @version $Id: GroundVelocityDirection.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.2
 * 
 */
public class GroundVelocityDirection implements IDirection {

    /** Serializable UID. */
    private static final long serialVersionUID = 793861790322991707L;

    /** Body shape. */
    private final BodyShape shape;

    /** Satellite line of sight. */
    private final IDirection direction;

    /**
     * Constructor.
     * 
     * @param bodyShape
     *        Shape of the body for which one we want to compute the ground velocity direction of a satellite.
     * @param trackingDirection
     *        Line of sight of the satellite (pointing direction)
     */
    public GroundVelocityDirection(final BodyShape bodyShape, final IDirection trackingDirection) {
        this.shape = bodyShape;
        this.direction = trackingDirection;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getVector(final PVCoordinatesProvider pvCoord, final AbsoluteDate date,
                              final Frame frame) throws PatriusException {
        return this.getLine(pvCoord, date, frame).getDirection();
    }

    /** {@inheritDoc} */
    @Override
    public Line getLine(final PVCoordinatesProvider pvCoord, final AbsoluteDate date,
                        final Frame frame) throws PatriusException {

        // frame attached to the body
        final Frame bodyFrame = this.shape.getBodyFrame();
        // inertial frame
        final Frame eme2000 = FramesFactory.getEME2000();

        // surface point location
        final Vector3D surfacePointLocation1 = this.getTargetPoint(pvCoord, date, frame);
        final Vector3D surfacePointLocation = frame.getTransformTo(eme2000, date)
            .transformPosition(surfacePointLocation1);
        // surface point velocity
        final Vector3D bodySpin = bodyFrame.getTransformTo(eme2000, date).
            getRotationRate().negate();
        final Vector3D surfacePointVelocity = Vector3D.crossProduct(bodySpin, surfacePointLocation);

        // relative velocity
        final PVCoordinates pv = (pvCoord == null) ? PVCoordinates.ZERO : pvCoord.getPVCoordinates(date, frame);
        final double r = MathLib.divide(surfacePointLocation.getNorm(), pv.getPosition().getNorm());
        Vector3D satVelocity = frame.getTransformTo(eme2000, date).transformPVCoordinates(pv).getVelocity();
        satVelocity = satVelocity.scalarMultiply(r);
        final Vector3D relativeVelocity = satVelocity.subtract(surfacePointVelocity);

        // projection in the given frame from eme2000
        final Transform toProjectionFrame = eme2000.getTransformTo(frame, date);

        // transformed position
        final Vector3D transfSurfPointLoc = toProjectionFrame.transformPosition(surfacePointLocation);

        return new Line(transfSurfPointLoc,
            transfSurfPointLoc.add(toProjectionFrame.transformVector(relativeVelocity)),
            transfSurfPointLoc);
    }

    /**
     * Compute the line of sight's target point position.
     * 
     * @param pvProv
     *        the pv coordinates provider (satellite orbit)
     * @param date
     *        date at which target point is requested
     * @param frame
     *        frame in which observed ground point should be provided
     * @return nadir point position as a {@link Vector3D}
     * @throws PatriusException
     *         if the direction does not cross the body shape
     */
    private Vector3D getTargetPoint(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                    final Frame frame) throws PatriusException {
        // satellite position in specified frame
        final Vector3D satPosition = (pvProv == null) ? Vector3D.ZERO : pvProv.getPVCoordinates(date, frame)
            .getPosition();

        final Transform toBodyFrame = frame.getTransformTo(this.shape.getBodyFrame(), date);
        // position expressed in body frame
        final Vector3D satInBodyFrame = toBodyFrame.transformPosition(satPosition);

        // ground target point
        final Line line = this.direction.getLine(pvProv, date, frame);
        final BodyPoint groundPoint = this.shape.getIntersectionPoint(line, satInBodyFrame, frame, date);

        if (groundPoint == null) {
            // the pointing direction does not cross the body shape
            throw new PatriusException(PatriusMessages.UNABLE_TO_COMPUTE_GROUND_VELOCITY_DIRECTION);
        }

        // cartesian coordinates of the nadir point in the given frame
        return this.shape.getBodyFrame().getTransformTo(frame, date).transformPosition(groundPoint.getPosition());
    }
}
