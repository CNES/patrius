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
 * @history 29/08/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2799:18/05/2021:Suppression des pas de temps fixes codes en dur 
 * VERSION:4.7:DM:DM-2590:18/05/2021:Configuration des TransformProvider 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:133:02/10/2013:Javadoc completed
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
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class is a {@link TransformProvider} for {@link OrientationFrame}; it provides, for a given date,
 * the transformation corresponding to the frame orientation with respect to the parent frame.
 *
 * <p>
 * Spin derivative is never computed and is either 0 or null. No analytical formula is available for spin derivative.
 * Spin is already computed by finite differences.
 * </p>
 * <p>Frames configuration is unused.</p>
 * 
 * @serial serializable given a serializable attribut {@link IOrientationLaw}.
 * 
 * @concurrency conditionally thread-safe
 * 
 * @concurrency.comment thread-safe if all attributes are.
 * 
 * @author tournebizej
 * 
 * @see OrientationFrame
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
@SuppressWarnings("PMD.NullAssignment")
public class OrientationTransformProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = -6420772272580569770L;

    /** Default value for delta-T used to compute spin by finite differences. */
    private static final double DEFAULT_SPIN_DELTAT = 0.1;
    
    /** The orientation law from which the transformation with respect to a parent frame is computed. */
    private final IOrientationLaw orientationLaw;

    /** The parent frame. */
    private final Frame refFrame;

    /** The delta-T used to compute spin by finite differences. */
    private final double spinDeltaT;
    
    /**
     * Protected constructor.
     * 
     * @param law
     *        the orientation law from which the transformation with respect to a parent frame is computed.
     * @param frame
     *        the parent frame.
     */
    protected OrientationTransformProvider(final IOrientationLaw law, final Frame frame) {
        this(law, frame, DEFAULT_SPIN_DELTAT);

    }

    /**
     * Protected constructor.
     * 
     * @param law
     *        the orientation law from which the transformation with respect to a parent frame is computed.
     * @param frame
     *        the parent frame.
     * @param spinDeltaT the delta-T used to compute spin by finite differences
     */
    protected OrientationTransformProvider(final IOrientationLaw law, final Frame frame, final double spinDeltaT) {

        this.orientationLaw = law;
        this.refFrame = frame;
        this.spinDeltaT = spinDeltaT;

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
    public Transform getTransform(final AbsoluteDate date,
                                  final FramesConfiguration config) throws PatriusException {
        return this.getTransform(date, config, false);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Spin derivative is never computed and is either 0 or null. No analytical formula is available for spin
     * derivative. Spin is already computed by finite differences.
     * </p>
     */
    @Override
    public Transform getTransform(final AbsoluteDate date,
                                  final boolean computeSpinDerivatives) throws PatriusException {
        return this.getTransform(date, FramesFactory.getConfiguration(), computeSpinDerivatives);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Spin derivative is never computed and is either 0 or null. No analytical formula is available for spin
     * derivative. Spin is already computed by finite differences.
     * </p>
     * <p>
     * Frames configuration is unused.
     * </p>
     */
    @Override
    public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                                  final boolean computeSpinDerivatives) throws PatriusException {
        final Rotation attitude = this.orientationLaw.getOrientation(date, this.refFrame);

        final double h = spinDeltaT;

        final Rotation rPrevious = this.orientationLaw.getOrientation(date.shiftedBy(-h), this.refFrame);
        final Rotation rAfter = this.orientationLaw.getOrientation(date.shiftedBy(h), this.refFrame);

        final Vector3D rotationRate = AngularCoordinates.estimateRate(rPrevious, rAfter, 2 * h);

        final Vector3D acc = computeSpinDerivatives ? Vector3D.ZERO : null;

        return new Transform(date, attitude, rotationRate, acc);
    }
}
