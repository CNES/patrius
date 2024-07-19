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
 * @history created 29/08/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3157:10/05/2022:[PATRIUS] Construction d'un AttitudeFrame a partir d'un AttitudeProvider 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2590:18/05/2021:Configuration des TransformProvider 
 * VERSION:4.6:FA:FA-2589:27/01/2021:[PATRIUS] Bug dans AttitudeTransformProvider 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:133:02/10/2013:Javadoc completed
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:12/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:524:10/03/2016:serialization java doc
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.frames.transformations.TransformProvider;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class is a {@link TransformProvider} for {@link AttitudeFrame}; it provides, for a given date,
 * the transformation corresponding to the spacecraft reference frame orientation with respect to the parent frame.
 *
 * <p>Spin derivative is computed when required and correspond to spin derivative of underlying attitude provider.</p>
 * <p>Frames configuration is unused.</p>
 * 
 * @serial given the attributs {@link AttitudeProvider} and {@link PVCoordinatesProvider} an instance of
 *         AttitudeTransformProvider is not guaranteed to be serializable
 * 
 * @concurrency conditionally thread-safe
 * 
 * @concurrency.comment is thread-safe if all attributes are.
 * 
 * @author tournebizej
 * 
 * @see AttitudeFrame
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
@SuppressWarnings("PMD.NullAssignment")
public class AttitudeTransformProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = -9164889216629455626L;

    /** The attitude provider from which the transformation with respect to a parent frame is computed. */
    private final AttitudeProvider attProv;

    /** PV coordinates provider for the spacecraft. */
    private final PVCoordinatesProvider pvProv;

    /** The parent frame. */
    private final Frame refFrame;

    /**
     * Protected constructor.
     * 
     * @param attProvider
     *        the attitude provider from which the transformation with respect to a parent frame is computed.
     * @param pv
     *        the PV coordinates provider.
     * @param frame
     *        the parent frame.
     */
    protected AttitudeTransformProvider(final AttitudeProvider attProvider, final PVCoordinatesProvider pv,
                                        final Frame frame) {

        this.attProv = attProvider;
        this.pvProv = pv;
        this.refFrame = frame;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Spin derivative is computed when required and correspond to spin derivative of underlying attitude provider.
     * </p>
     */
    @Override
    public Transform getTransform(final AbsoluteDate date,
                                  final boolean computeSpinDerivatives) throws PatriusException {
        return this.getTransform(date, FramesFactory.getConfiguration(), computeSpinDerivatives);
    }

    /** {@inheritDoc} */
    @Override
    public Transform getTransform(final AbsoluteDate date) throws PatriusException {
        return this.getTransform(date, FramesFactory.getConfiguration(), false);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Frames configuration is unused.
     * </p>
     */
    @Override
    public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config) throws PatriusException {
        return this.getTransform(date, config, false);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Spin derivative is computed when required and correspond to spin derivative of underlying attitude provider.
     * </p>
     * <p>
     * Frames configuration is unused.
     * </p>
     */
    @Override
    public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                                  final boolean computeSpinDerivatives) throws PatriusException {
        /*
         * the attitude given by the provider at a specified date defines a base (reference frame, rotation and angular
         * velocity wrt this reference frame) from which one we will define the new transformation. This new
         * transformation defines the satellite reference frame wrt the parent parent given at the construction
         */
        this.attProv.setSpinDerivativesComputation(computeSpinDerivatives);
        final Attitude attitude = this.attProv.getAttitude(this.pvProv, date, this.refFrame);

        /*
         * the attitude defines the rotation and the rotation rate of the transformation between the satellite frame and
         * the reference frame which has been specified at the construction
         */
        final Rotation rotation = attitude.getRotation();
        final Vector3D rotationRate = attitude.getSpin();
        final Vector3D acc = computeSpinDerivatives ? attitude.getRotationAcceleration() : null;

        final Transform rotationT = new Transform(date, rotation, rotationRate, acc);

        /*
         * the position / velocity of the satellite defines the translation and the velocity of the origin of the frame
         * which is constructed
         */
        final Vector3D position = this.pvProv.getPVCoordinates(date, this.refFrame).getPosition();
        final Vector3D velocity = this.pvProv.getPVCoordinates(date, this.refFrame).getVelocity();

        final Transform translationT = new Transform(date, position, velocity);

        return new Transform(date, translationT, rotationT, computeSpinDerivatives);
    }

}
