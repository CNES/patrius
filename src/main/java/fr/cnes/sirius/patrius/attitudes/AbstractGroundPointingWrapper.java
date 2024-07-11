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
 * VERSION:4.7:DM:DM-2799:18/05/2021:Suppression des pas de temps fixes codes en dur 
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION:4.3:DM:DM-2104:15/05/2019:[Patrius] Rendre generiques les classes GroundPointing et NadirPointing
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:87:05/08/2013:updated after the GroundPointing class modifications
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * VERSION::DM:489:15/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:583:11/03/2016:simplification of attitude laws architecture
 * VERSION::DM:603:29/08/2016:deleted deprecated methods and classes in package attitudes
 * VERSION::DM:1489:18/05/2018:Rapatriation of custom class from Genopus
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.TimeStampedPVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.TimeStampedAngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class leverages common parts for compensation modes around ground pointing attitudes.
 * 
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Luc Maisonobe
 */
public abstract class AbstractGroundPointingWrapper extends AbstractGroundPointing implements
    AttitudeLawModifier {

    /** Serializable UID. */
    private static final long serialVersionUID = 262999520075931766L;

    /** Underlying ground pointing attitude provider. */
    private final AbstractGroundPointing groundPointingLaw;

    /**
     * Creates a new instance.
     * 
     * @param pGroundPointingLaw ground pointing attitude provider without compensation
     */
    public AbstractGroundPointingWrapper(final AbstractGroundPointing pGroundPointingLaw) {
        super(pGroundPointingLaw.getBodyShape());
        this.groundPointingLaw = pGroundPointingLaw;
    }

    /**
     * Creates a new instance with specified los axis in satellite frame.
     * 
     * @param pGroundPointingLaw ground pointing attitude provider without compensation
     * @param losInSatFrameVec LOS in satellite frame axis
     * @param losNormalInSatFrameVec LOS normal axis in satellite frame
     */
    public AbstractGroundPointingWrapper(final AbstractGroundPointing pGroundPointingLaw,
        final Vector3D losInSatFrameVec, final Vector3D losNormalInSatFrameVec) {
        // Call constructor of superclass
        super(pGroundPointingLaw.getBodyShape(), losInSatFrameVec, losNormalInSatFrameVec);
        this.groundPointingLaw = pGroundPointingLaw;
    }

    /**
     * Creates a new instance with specified los axis in satellite frame.
     * 
     * @param pGroundPointingLaw ground pointing attitude provider without compensation
     * @param losInSatFrameVec LOS in satellite frame axis
     * @param losNormalInSatFrameVec LOS normal axis in satellite frame
     * @param targetVelocityDeltaT the delta-T used to compute target velocity by finite differences
     */
    public AbstractGroundPointingWrapper(final AbstractGroundPointing pGroundPointingLaw,
        final Vector3D losInSatFrameVec, final Vector3D losNormalInSatFrameVec,
        final double targetVelocityDeltaT) {
        // Call constructor of superclass
        super(pGroundPointingLaw.getBodyShape(), losInSatFrameVec, losNormalInSatFrameVec, targetVelocityDeltaT);
        this.groundPointingLaw = pGroundPointingLaw;
    }

    /**
     * Get the underlying (ground pointing) attitude provider.
     * 
     * @return underlying attitude provider
     */
    @Override
    public final AttitudeLaw getUnderlyingAttitudeLaw() {
        return this.groundPointingLaw;
    }

    /** {@inheritDoc} */
    @Override
    protected final Vector3D getTargetPoint(final PVCoordinatesProvider pvProv,
                                            final AbsoluteDate date, final Frame frame) throws PatriusException {
        return this.groundPointingLaw.getTargetPoint(pvProv, date, frame);
    }

    /** {@inheritDoc} */
    @Override
    protected final TimeStampedPVCoordinates
            getTargetPV(final PVCoordinatesProvider pvProv,
                        final AbsoluteDate date, final Frame frame) throws PatriusException {
        return this.groundPointingLaw.getTargetPV(pvProv, date, frame);
    }

    /**
     * Compute the base system state at given date, without compensation.
     * 
     * @param pvProv provider for PV coordinates
     * @param date date at which state is requested
     * @param frame reference frame from which attitude is computed
     * @return satellite base attitude state, i.e without compensation.
     * @throws PatriusException if some specific error occurs
     */
    public final Attitude getBaseState(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                       final Frame frame) throws PatriusException {
        return this.groundPointingLaw.getAttitude(pvProv, date, frame);
    }

    /**
     * Compute the TimeStampedAngularCoordinates at a given time.
     * 
     * @param pvProv provider for PV coordinates
     * @param date date at which rotation is requested
     * @param frame reference frame from which attitude is computed
     * @param base base satellite attitude in given frame.
     * @return compensation rotation at date, i.e rotation between non compensated attitude state
     *         and compensated state.
     * @throws PatriusException if some specific error occurs
     */
    public abstract TimeStampedAngularCoordinates getCompensation(
                                                                  final PVCoordinatesProvider pvProv,
                                                                  final AbsoluteDate date, final Frame frame,
                                                                  final Attitude base) throws PatriusException;

    /**
     * {@inheritDoc}
     * <p>
     * Spin derivatives computation applies to provided law {@link #groundPointingLaw}.
     * </p>
     */
    @Override
    public void setSpinDerivativesComputation(final boolean computeSpinDerivatives) {
        super.setSpinDerivativesComputation(computeSpinDerivatives);
        this.groundPointingLaw.setSpinDerivativesComputation(computeSpinDerivatives);
    }

    /**
     * Getter for the ground pointing attitude provider without yaw compensation.
     * 
     * @return the ground pointing attitude provider without yaw compensation
     */
    public AttitudeLaw getGroundPointingLaw() {
        return this.groundPointingLaw;
    }
}
