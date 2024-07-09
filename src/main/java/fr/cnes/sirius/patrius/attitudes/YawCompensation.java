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
 * VERSION:4.5:FA:FA-2353:27/05/2020:Correction de bug dans YawCompensation 
 * VERSION:4.5:FA:FA-2464:27/05/2020:Anomalie dans le calcul du vecteur rotation des LOF
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
* VERSION:4.3:DM:DM-2104:15/05/2019:[Patrius] Rendre generiques les classes GroundPointing et NadirPointing
 * VERSION::FA:185:10/04/2014:the getCompensation method has been modified
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:15/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:583:11/03/2016:simplification of attitude laws architecture
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.TimeStampedAngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class handles yaw compensation attitude provider.
 * 
 * <p>
 * Yaw compensation is mainly used for Earth observation satellites. As a satellites moves along its track, the image of
 * ground points move on the focal point of the optical sensor. This motion is a combination of the satellite motion,
 * but also on the Earth rotation and on the current attitude (in particular if the pointing includes Roll or Pitch
 * offset). In order to reduce geometrical distortion, the yaw angle is changed a little from the simple ground pointing
 * attitude such that the apparent motion of ground points is along a prescribed axis (orthogonal to the optical sensors
 * rows), taking into account all effects.
 * </p>
 * <p>
 * This attitude is implemented as a wrapper on top of an underlying ground pointing law that defines the roll and pitch
 * angles.
 * </p>
 * <p>
 * Instances of this class are guaranteed to be immutable.
 * </p>
 * 
 * @see GroundPointing
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class YawCompensation extends AbstractGroundPointingWrapper {

    /** Serializable UID. */
    private static final long serialVersionUID = 1145977506851433023L;

    /** J axis. */
    private static final PVCoordinates PLUS_J =
        new PVCoordinates(Vector3D.PLUS_J, Vector3D.ZERO, Vector3D.ZERO);

    /** K axis. */
    private static final PVCoordinates PLUS_K =
        new PVCoordinates(Vector3D.PLUS_K, Vector3D.ZERO, Vector3D.ZERO);

    /**
     * Creates a new instance.
     * 
     * @param groundPointingLaw
     *        ground pointing attitude provider without yaw compensation
     */
    public YawCompensation(final AbstractGroundPointing groundPointingLaw) {
        super(groundPointingLaw);
    }

    /**
     * Creates a new instance with specified los axis in satellite frame.
     * 
     * @param groundPointingLaw ground pointing attitude provider without yaw compensation
     * @param losInSatFrameVec LOS in satellite frame axis
     * @param losNormalInSatFrameVec LOS normal axis in satellite frame
     */
    public YawCompensation(final AbstractGroundPointing groundPointingLaw,
        final Vector3D losInSatFrameVec,
        final Vector3D losNormalInSatFrameVec) {
        // Call constructor of superclass
        super(groundPointingLaw, losInSatFrameVec, losNormalInSatFrameVec);
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                final AbsoluteDate date, final Frame frame) throws PatriusException {

        // Compute attitude in a Earth-centered frame
        final Frame gcrf = FramesFactory.getGCRF();
        
        // Get target point
        final PVCoordinates targetPoint = this.getTargetPV(pvProv, date, gcrf);

        final Transform bodyFrameToFrame =
            this.getBodyFrame().getTransformTo(gcrf, date, this.getSpinDerivativesComputation());
        final PVCoordinates pv = pvProv.getPVCoordinates(date, gcrf);

        // Compute relative velocity
        final Vector3D bodySpin = bodyFrameToFrame.getRotationRate().negate();
        final Vector3D surfacePointVelocity = Vector3D.crossProduct(bodySpin, targetPoint.getPosition());
        final double r = targetPoint.getPosition().getNorm() / pv.getPosition().getNorm();
        final Vector3D satVelocity = pv.getVelocity().scalarMultiply(r);
        final PVCoordinates slidingBody = bodyFrameToFrame.getInverse().transformPVCoordinates(targetPoint);
        final Vector3D v = satVelocity.subtract(surfacePointVelocity);

        // Final computation
        final Vector3D a = new Vector3D(+1, bodyFrameToFrame.getRotation()
            .applyInverseTo(slidingBody.getAcceleration() == null ? Vector3D.ZERO : slidingBody.getAcceleration()), 1,
            Vector3D.crossProduct(bodyFrameToFrame.getRotationRate(), v));
        final PVCoordinates relativeVelocity = new PVCoordinates(v, a, Vector3D.ZERO);
        final PVCoordinates relativePosition = new PVCoordinates(pv, targetPoint);
        final PVCoordinates relativeNormal = PVCoordinates.crossProduct(relativePosition, relativeVelocity).normalize();
        final TimeStampedAngularCoordinates tsac = new TimeStampedAngularCoordinates(date,
            relativePosition.normalize(), relativeNormal.normalize(), PLUS_K, PLUS_J, 1.0e-9,
            getSpinDerivativesComputation());
        
        // Return attitude in user frame
        return new Attitude(gcrf, tsac).withReferenceFrame(frame, getSpinDerivativesComputation());
    }

    /** {@inheritDoc} */
    @Override
    public TimeStampedAngularCoordinates getCompensation(final PVCoordinatesProvider pvProv,
                                                         final AbsoluteDate date, final Frame orbitFrame,
                                                         final Attitude base) throws PatriusException {

        // compute relative velocity of FIXED ground point with respect to satellite
        // beware the point considered is NOT the sliding point on central body surface
        // as returned by getUnderlyingAttitudeProvider().getTargetPV(), but the fixed
        // point that at current time is the target, but before and after is only a body
        // surface point with its own motion and not aligned with satellite Z axis.
        // So the following computation needs to recompute velocity by itself, using
        // the velocity provided by getTargetPV would be wrong!

        final Frame bodyFrame = this.getBodyFrame();
        final Vector3D surfacePointLocation = ((AbstractGroundPointing) this.getUnderlyingAttitudeLaw())
            .getTargetPoint(pvProv,
                date, orbitFrame);
        final Vector3D bodySpin = bodyFrame.getTransformTo(orbitFrame, date, this.getSpinDerivativesComputation())
            .getRotationRate().negate();
        final Vector3D surfacePointVelocity = Vector3D.crossProduct(bodySpin, surfacePointLocation);

        // compute satellite velocity wrt ground point:
        final double r = surfacePointLocation.getNorm()
            / pvProv.getPVCoordinates(date, orbitFrame).getPosition().getNorm();
        final Vector3D satVelocity = pvProv.getPVCoordinates(date, orbitFrame).getVelocity().scalarMultiply(r);
        final Vector3D relativeVelocity = surfacePointVelocity.subtract(satVelocity);

        // Compensation rotation definition :
        // . Z satellite axis is unchanged
        // . target relative velocity is in (Z,X) plane, in the -X half plane part
        final Rotation compensation =
            new Rotation(Vector3D.PLUS_K, Vector3D.MINUS_I, Vector3D.PLUS_K, base.getRotation().applyInverseTo(
                relativeVelocity));
        return new TimeStampedAngularCoordinates(date, compensation, Vector3D.ZERO, Vector3D.ZERO);
    }

    /**
     * Compute the yaw compensation angle at date.
     * 
     * @param pvProv
     *        provider for PV coordinates
     * @param date
     *        date at which compensation is requested
     * @param frame
     *        reference frame from which attitude is computed
     * @return yaw compensation angle for orbit.
     * @throws PatriusException
     *         if some specific error occurs
     */
    public double getYawAngle(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                              final Frame frame) throws PatriusException {
        return this.getCompensation(pvProv, date, frame, this.getBaseState(pvProv, date, frame)).getRotation()
            .getAngle();
    }
}
