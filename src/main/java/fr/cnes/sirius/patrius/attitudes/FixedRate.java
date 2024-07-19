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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:583:11/03/2016:simplification of attitude laws architecture
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.TimeStampedAngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class handles a simple attitude provider at constant rate around a fixed axis.
 * <p>
 * This attitude provider is a simple linear extrapolation from an initial orientation, a rotation axis and a rotation
 * rate. All this elements can be specified as a simple {@link Attitude reference attitude}.
 * </p>
 * <p>
 * Instances of this class are guaranteed to be immutable.
 * </p>
 * 
 * @author Luc Maisonobe
 */
public class FixedRate extends AbstractAttitudeLaw {

    /** Serializable UID. */
    private static final long serialVersionUID = 6874119218379303688L;

    /** Reference attitude. */
    private final Attitude referenceAttitude;

    /**
     * Creates a new instance.
     * 
     * @param pReferenceAttitude
     *        attitude at reference date
     */
    public FixedRate(final Attitude pReferenceAttitude) {
        super();
        this.referenceAttitude = pReferenceAttitude;
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                final Frame frame) throws PatriusException {

        // Initialization
        final Rotation rotation = this.referenceAttitude.getRotation();
        final Vector3D refSpin = this.referenceAttitude.getSpin();
        final AngularCoordinates orientation = this.referenceAttitude.getOrientation();

        // Get orientation
        final double rate = refSpin.getNorm();
        final Transform t = frame.getTransformTo(this.referenceAttitude.getReferenceFrame(), date,
            this.getSpinDerivativesComputation());
        Rotation q = t.getRotation().applyTo(rotation);
        if (rate != 0.0) {
            // Non-constant rotations:
            final double timeShift = date.durationFrom(this.referenceAttitude.getDate());
            final Rotation evolution = new Rotation(refSpin, rate * timeShift);
            q = q.applyTo(evolution);
        }

        // Get spin
        final Vector3D spin = orientation.getRotationRate().add(
            orientation.getRotation().applyInverseTo(t.getRotationRate()));

        // Get spin derivative
        Vector3D spinDerivative = null;
        if (this.getSpinDerivativesComputation() && orientation.getRotationAcceleration() != null) {
            spinDerivative = orientation.getRotationAcceleration().add(
                orientation.getRotation().applyInverseTo(t.getRotationAcceleration()));
        }

        // Return attitude
        return new Attitude(frame, new TimeStampedAngularCoordinates(date, q, spin, spinDerivative));
    }

    /**
     * Get the reference attitude.
     * 
     * @return reference attitude
     */
    public final Attitude getReferenceAttitude() {
        return this.referenceAttitude;
    }
}
