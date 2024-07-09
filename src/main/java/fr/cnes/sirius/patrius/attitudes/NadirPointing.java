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
 * VERSION:4.7:DM:DM-2799:18/05/2021:Suppression des pas de temps fixes codes en dur 
 * VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 * VERSION:4.3:DM:DM-2090:15/05/2019:[PATRIUS] ajout de fonctionnalites aux bibliotheques mathematiques
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION:4.3:DM:DM-2104:15/05/2019:[Patrius] Rendre generiques les classes GroundPointing et NadirPointing
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:87:05/08/2013:updated after the GroundPointing class modifications
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.TimeStampedPVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.CartesianDerivativesFilter;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class handles nadir pointing attitude provider.
 * 
 * <p>
 * This class represents the attitude provider where (by default) the satellite z axis is pointing to the vertical of
 * the ground point under satellite.
 * </p>
 * <p>
 * The object <code>NadirPointing</code> is guaranteed to be immutable.
 * </p>
 * 
 * @see GroundPointing
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class NadirPointing extends AbstractGroundPointing {

    /** Serializable UID. */
    private static final long serialVersionUID = 9077899256315179822L;
    /** -2. */
    private static final double C_N2 = -2.;

    /** Default value for delta-T used to compute target PVCoordinates by finite differences. */
    private static final double DEFAULT_DELTAT = 0.01;

    /** The delta-T used to compute target PVCoordinates by finite differences. */
    private final double deltaT;

    /**
     * Creates new instance.
     * 
     * @param shape
     *        Body shape
     */
    public NadirPointing(final BodyShape shape) {
        // Call constructor of superclass
        super(shape);
        this.deltaT = DEFAULT_DELTAT;
    }

    /**
     * Constructor. Create a NadirPointing attitude provider with specified los axis in satellite
     * frame.
     * 
     * @param shape Body shape
     * @param losInSatFrameVec LOS in satellite frame axis
     * @param losNormalInSatFrameVec LOS normal axis in satellite frame
     */
    public NadirPointing(final BodyShape shape, final Vector3D losInSatFrameVec,
        final Vector3D losNormalInSatFrameVec) {
        this(shape, losInSatFrameVec, losNormalInSatFrameVec, DEFAULT_DELTAT);
    }

    /**
     * Constructor. Create a NadirPointing attitude provider with specified los axis in satellite
     * frame.
     * 
     * @param shape Body shape
     * @param losInSatFrameVec LOS in satellite frame axis
     * @param losNormalInSatFrameVec LOS normal axis in satellite frame
     * @param deltaT the delta-T used to compute target PVCoordinates by finite differences
     */
    public NadirPointing(final BodyShape shape, final Vector3D losInSatFrameVec,
        final Vector3D losNormalInSatFrameVec, final double deltaT) {
        // Call constructor of superclass
        super(shape, losInSatFrameVec, losNormalInSatFrameVec);
        this.deltaT = deltaT;
    }

    /** {@inheritDoc} */
    @Override
    public TimeStampedPVCoordinates getTargetPV(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                                final Frame frame) throws PatriusException {
        // transform from specified reference frame to body frame
        final Transform refToBody = frame.getTransformTo(this.getBodyShape().getBodyFrame(), date,
            this.getSpinDerivativesComputation());

        // sample intersection points in current date neighborhood
        final double h = deltaT;
        final List<TimeStampedPVCoordinates> sample = new ArrayList<TimeStampedPVCoordinates>();
        final PVCoordinates pvProvM2h = pvProv.getPVCoordinates(date.shiftedBy(C_N2 * h), frame);
        final PVCoordinates pvProvM1h = pvProv.getPVCoordinates(date.shiftedBy(-h), frame);
        final PVCoordinates pvProvh = pvProv.getPVCoordinates(date, frame);
        final PVCoordinates pvProvP1h = pvProv.getPVCoordinates(date.shiftedBy(+h), frame);
        final PVCoordinates pvProvP2h = pvProv.getPVCoordinates(date.shiftedBy(+2 * h), frame);
        sample.add(this.nadirRef(new TimeStampedPVCoordinates(date.shiftedBy(C_N2 * h), pvProvM2h),
            refToBody.shiftedBy(C_N2 * h)));
        sample.add(this.nadirRef(new TimeStampedPVCoordinates(date.shiftedBy(-h), pvProvM1h), refToBody.shiftedBy(-h)));
        sample.add(this.nadirRef(new TimeStampedPVCoordinates(date, pvProvh), refToBody));
        sample.add(this.nadirRef(new TimeStampedPVCoordinates(date.shiftedBy(+h), pvProvP1h), refToBody.shiftedBy(+h)));
        sample.add(this.nadirRef(new TimeStampedPVCoordinates(date.shiftedBy(+2 * h), pvProvP2h),
            refToBody.shiftedBy(+2 * h)));

        // use interpolation to compute properly the time-derivatives
        return TimeStampedPVCoordinates.interpolate(date, CartesianDerivativesFilter.USE_P, sample);
    }

    /** {@inheritDoc} */
    @Override
    protected Vector3D getTargetPoint(final PVCoordinatesProvider pvProv,
                                      final AbsoluteDate date, final Frame frame) throws PatriusException {

        final Vector3D satInBodyFrame = pvProv.getPVCoordinates(date, this.getBodyFrame()).getPosition();

        // satellite position in geodetic coordinates
        final GeodeticPoint gpSat = this.getBodyShape().transform(satInBodyFrame, this.getBodyFrame(), date);

        // nadir position in geodetic coordinates
        final GeodeticPoint gpNadir = new GeodeticPoint(gpSat.getLatitude(), gpSat.getLongitude(), 0.0);

        // nadir point position in specified frame
        return this.getBodyFrame().getTransformTo(frame, date, this.getSpinDerivativesComputation())
            .transformPosition(this.getBodyShape().transform(gpNadir));

    }

    /**
     * Compute ground point in nadir direction, in reference frame.
     * 
     * @param scRef
     *        spacecraft coordinates in reference frame
     * @param refToBody
     *        transform from reference frame to body frame
     * @return intersection point in body frame (only the position is set!)
     * @exception PatriusException
     *            if line of sight does not intersect body
     */
    private TimeStampedPVCoordinates nadirRef(final TimeStampedPVCoordinates scRef,
                                              final Transform refToBody) throws PatriusException {

        final Vector3D satInBodyFrame = refToBody.transformPosition(scRef.getPosition());

        // satellite position in geodetic coordinates
        final GeodeticPoint gpSat = this.getBodyShape().transform(satInBodyFrame, this.getBodyFrame(), scRef.getDate());

        // nadir position in geodetic coordinates
        final GeodeticPoint gpNadir = new GeodeticPoint(gpSat.getLatitude(), gpSat.getLongitude(), 0.0);

        // nadir point position in body frame
        final Vector3D pNadirBody = this.getBodyShape().transform(gpNadir);

        // nadir point position in reference frame
        final Vector3D pNadirRef = refToBody.getInverse().transformPosition(pNadirBody);

        return new TimeStampedPVCoordinates(scRef.getDate(), pNadirRef, Vector3D.ZERO, Vector3D.ZERO);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
