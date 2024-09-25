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
* VERSION:4.13:DM:DM-70:08/12/2023:[PATRIUS] Calcul de jacobienne dans OneAxisEllipsoid
 * VERSION:4.12.1:FA:FA-123:05/09/2023:[PATRIUS] Utilisation de getLLHCoordinates() au 
 *          lieu de getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC) 
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
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

import fr.cnes.sirius.patrius.bodies.BodyPoint.BodyPointName;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.LLHCoordinatesSystem;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.utils.TimeStampedPVCoordinates;
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
 * <p>
 * This class is restricted to be used with {@link EllipsoidBodyShape}.
 * </p>
 *
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
    public NadirPointing(final EllipsoidBodyShape shape) {
        // Call constructor of superclass
        super(shape);
        this.deltaT = DEFAULT_DELTAT;
    }

    /**
     * Constructor. Create a NadirPointing attitude provider with specified los axis in satellite frame.
     *
     * @param shape
     *        Body shape
     * @param losInSatFrameVec
     *        LOS in satellite frame axis
     * @param losNormalInSatFrameVec
     *        LOS normal axis in satellite frame
     */
    public NadirPointing(final EllipsoidBodyShape shape, final Vector3D losInSatFrameVec,
                         final Vector3D losNormalInSatFrameVec) {
        this(shape, losInSatFrameVec, losNormalInSatFrameVec, DEFAULT_DELTAT);
    }

    /**
     * Constructor. Create a NadirPointing attitude provider with specified los axis in satellite frame.
     *
     * @param shape
     *        Body shape
     * @param losInSatFrameVec
     *        LOS in satellite frame axis
     * @param losNormalInSatFrameVec
     *        LOS normal axis in satellite frame
     * @param deltaT
     *        the delta-T used to compute target PVCoordinates by finite differences
     */
    public NadirPointing(final EllipsoidBodyShape shape, final Vector3D losInSatFrameVec,
                         final Vector3D losNormalInSatFrameVec, final double deltaT) {
        // Call constructor of superclass
        super(shape, losInSatFrameVec, losNormalInSatFrameVec);
        this.deltaT = deltaT;
    }

    /** {@inheritDoc} */
    @Override
    public TimeStampedPVCoordinates getTargetPV(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                                final Frame frame)
        throws PatriusException {
        // transform from specified reference frame to body frame
        final Transform refToBody = frame.getTransformTo(getBodyShape().getBodyFrame(), date,
            getSpinDerivativesComputation());

        // sample intersection points in current date neighborhood
        final double h = this.deltaT;
        final List<TimeStampedPVCoordinates> sample = new ArrayList<>();
        final PVCoordinates pvProvM2h = pvProv.getPVCoordinates(date.shiftedBy(C_N2 * h), frame);
        final PVCoordinates pvProvM1h = pvProv.getPVCoordinates(date.shiftedBy(-h), frame);
        final PVCoordinates pvProvh = pvProv.getPVCoordinates(date, frame);
        final PVCoordinates pvProvP1h = pvProv.getPVCoordinates(date.shiftedBy(+h), frame);
        final PVCoordinates pvProvP2h = pvProv.getPVCoordinates(date.shiftedBy(+2 * h), frame);
        sample.add(nadirRef(new TimeStampedPVCoordinates(date.shiftedBy(C_N2 * h), pvProvM2h),
            refToBody.shiftedBy(C_N2 * h)));
        sample.add(nadirRef(new TimeStampedPVCoordinates(date.shiftedBy(-h), pvProvM1h), refToBody.shiftedBy(-h)));
        sample.add(nadirRef(new TimeStampedPVCoordinates(date, pvProvh), refToBody));
        sample.add(nadirRef(new TimeStampedPVCoordinates(date.shiftedBy(+h), pvProvP1h), refToBody.shiftedBy(+h)));
        sample.add(nadirRef(new TimeStampedPVCoordinates(date.shiftedBy(+2 * h), pvProvP2h),
            refToBody.shiftedBy(+2 * h)));

        // use interpolation to compute properly the time-derivatives
        return TimeStampedPVCoordinates.interpolate(date, CartesianDerivativesFilter.USE_P, sample);
    }

    /** {@inheritDoc} */
    @Override
    protected Vector3D getTargetPosition(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                         final Frame frame) throws PatriusException {

        final Vector3D satInBodyFrame = pvProv.getPVCoordinates(date, getBodyFrame()).getPosition();

        // nadir position in geodetic coordinates
        final EllipsoidPoint nadirPoint = getNadirPoint(satInBodyFrame, date);

        // nadir point position in specified frame
        return getBodyFrame().getTransformTo(frame, date, getSpinDerivativesComputation()).transformPosition(
            nadirPoint.getPosition());
    }

    /**
     * Compute ground point in nadir direction, in reference frame.
     *
     * @param scRef
     *        spacecraft coordinates in reference frame
     * @param refToBody
     *        transform from reference frame to body frame
     * @return intersection point in body frame (only the position is set!)
     * @throws PatriusException
     *         if failed to build point
     */
    private TimeStampedPVCoordinates nadirRef(final TimeStampedPVCoordinates scRef, final Transform refToBody)
        throws PatriusException {

        final Vector3D satInBodyFrame = refToBody.transformPosition(scRef.getPosition());

        // nadir position in geodetic coordinates
        final EllipsoidPoint nadirPoint = getNadirPoint(satInBodyFrame, scRef.getDate());

        // nadir point position in body frame
        final Vector3D pNadirBody = nadirPoint.getPosition();

        // nadir point position in reference frame
        final Vector3D pNadirRef = refToBody.getInverse().transformPosition(pNadirBody);

        return new TimeStampedPVCoordinates(scRef.getDate(), pNadirRef, Vector3D.ZERO, Vector3D.ZERO);
    }

    /**
     * Build the nadir point (code mutualization).
     * 
     * @param satInBodyFrame
     *        Satellite position in body frame
     * @param date
     *        Date
     * @return the nadir point
     * @throws PatriusException
     *         if failed to build point
     */
    private EllipsoidPoint getNadirPoint(final Vector3D satInBodyFrame, final AbsoluteDate date)
        throws PatriusException {

        // satellite position in geodetic coordinates
        final EllipsoidPoint satPoint = (EllipsoidPoint) getBodyShape().buildPoint(satInBodyFrame, getBodyFrame(),
            date, BodyPointName.DEFAULT);

        // nadir position in geodetic coordinates
        return new EllipsoidPoint(satPoint.getBodyShape(), satPoint.getBodyShape().getLLHCoordinatesSystem(), 
            satPoint.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude(), 
            satPoint.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLongitude(), 0., BodyPointName.DEFAULT);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
