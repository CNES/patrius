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
* VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
* VERSION:4.3:FA:FA-1978:15/05/2019:Anomalie calcul orientation corps celeste (UAI)
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
* VERSION:4.3:DM:DM-2104:15/05/2019:[Patrius] Rendre generiques les classes GroundPointing et NadirPointing
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:87:05/08/2013:updated after the GroundPointing class modifications
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class provides a default attitude provider.
 * <p>
 * The attitude pointing law is defined by an attitude provider and the satellite axis vector chosen for pointing.
 * <p>
 * 
 * @author V&eacute;ronique Pommier-Maurussane
 */
@SuppressWarnings("PMD.NullAssignment")
public class LofOffsetPointing extends AbstractGroundPointing {

    /** Serializable UID. */
    private static final long serialVersionUID = -713570668596014285L;

    /** Rotation from local orbital frame. */
    private final AttitudeProvider attitudeLaw;

    /** Chosen satellite axis for pointing, given in satellite frame. */
    private final Vector3D satPointingVector;

    /**
     * Creates new instance.
     * 
     * @param shape
     *        Body shape
     * @param attLaw
     *        Attitude law
     * @param satPointingVectorIn
     *        satellite vector defining the pointing direction
     */
    public LofOffsetPointing(final BodyShape shape, final AttitudeProvider attLaw,
        final Vector3D satPointingVectorIn) {
        super(shape);
        this.attitudeLaw = attLaw;
        this.satPointingVector = satPointingVectorIn;
    }

    /**
     * Constructor. Create a BodyCenterGroundPointing attitude provider with specified los axis in
     * satellite frame.
     * 
     * @param shape Body shape
     * @param attLaw Attitude law
     * @param satPointingVectorIn satellite vector defining the pointing direction
     * @param losInSatFrameVec LOS in satellite frame axis
     * @param losNormalInSatFrameVec LOS normal axis in satellite frame
     */
    public LofOffsetPointing(final BodyShape shape, final AttitudeProvider attLaw,
        final Vector3D satPointingVectorIn, final Vector3D losInSatFrameVec,
        final Vector3D losNormalInSatFrameVec) {
        super(shape, losInSatFrameVec, losNormalInSatFrameVec);
        this.attitudeLaw = attLaw;
        this.satPointingVector = satPointingVectorIn;
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                final AbsoluteDate date, final Frame frame) throws PatriusException {
        return this.attitudeLaw.getAttitude(pvProv, date, frame);
    }

    /** {@inheritDoc} */
    @Override
    protected Vector3D getTargetPoint(final PVCoordinatesProvider pvProv,
                                      final AbsoluteDate date, final Frame frame) throws PatriusException {

        final PVCoordinates pv = pvProv.getPVCoordinates(date, frame);

        // Compute satellite state at given date in orbit frame
        final Rotation satRot = this.attitudeLaw.getAttitude(pvProv, date, frame).getRotation();

        // Compute satellite pointing axis and position/velocity in body frame
        final Transform t = frame.getTransformTo(this.getBodyFrame(), date, this.getSpinDerivativesComputation());
        final Vector3D pointingBodyFrame = t.transformVector(satRot.applyTo(this.satPointingVector));
        final Vector3D pBodyFrame = t.transformPosition(pv.getPosition());

        // Line from satellite following pointing direction
        // we use arbitrarily the Earth radius as a scaling factor, it could be anything else
        final Line pointingLine = new Line(pBodyFrame,
            pBodyFrame.add(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, pointingBodyFrame));

        // Intersection with body shape
        final GeodeticPoint gpIntersection =
            this.getBodyShape().getIntersectionPoint(pointingLine, pBodyFrame, this.getBodyFrame(), date);
        final Vector3D vIntersection =
            (gpIntersection == null) ? null : this.getBodyShape().transform(gpIntersection);

        // Check there is an intersection and it is not in the reverse pointing direction
        if ((vIntersection == null) ||
            (Vector3D.dotProduct(vIntersection.subtract(pBodyFrame), pointingBodyFrame) < 0)) {
            throw new PatriusException(PatriusMessages.ATTITUDE_POINTING_LAW_DOES_NOT_POINT_TO_GROUND);
        }

        return this.getBodyFrame().getTransformTo(frame, date, this.getSpinDerivativesComputation())
            .transformPosition(vIntersection);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Spin derivatives computation applies to provided law {@link #attitudeLaw}.
     * </p>
     */
    @Override
    public void setSpinDerivativesComputation(final boolean computeSpinDerivatives) {
        super.setSpinDerivativesComputation(computeSpinDerivatives);
        this.attitudeLaw.setSpinDerivativesComputation(computeSpinDerivatives);
    }
}
