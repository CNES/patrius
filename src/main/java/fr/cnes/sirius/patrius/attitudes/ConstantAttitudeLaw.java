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
 * @history creation 12/03/2015
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::FA:423:17/11/2015: improve computation times
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:583:11/03/2016:simplification of attitude laws architecture
 * VERSION::FA:675:01/09/2016:corrected anomalies reducing the performances
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.TimeStampedAngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class handles a constant attitude law.
 * <p>
 * Instances of this class are guaranteed to be immutable.
 * </p>
 * 
 * @author pontisso
 * 
 * @version $Id: ConstantAttitudeLaw.java 18065 2017-10-02 16:42:02Z bignon $
 * 
 * @since 3.0
 * 
 */
public class ConstantAttitudeLaw extends AbstractAttitudeLaw {

    /** Serializable UID. */
    private static final long serialVersionUID = -2143350226737178213L;

    /** Reference frame. */
    private final Frame referenceFrame;

    /** Rotation from reference frame to desired satellite frame. */
    private final Rotation rotation;

    /**
     * Creates new instance.
     * 
     * @param referenceFrameIn the reference frame
     * @param rotationIn rotation from reference frame to satellite frame.
     */
    public ConstantAttitudeLaw(final Frame referenceFrameIn, final Rotation rotationIn) {
        super();
        this.referenceFrame = referenceFrameIn;
        this.rotation = rotationIn;
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                final Frame frame) throws PatriusException {

        // Compute transform from referenceFrame to frame : composition of two transforms
        final Transform transform1 = frame.getTransformTo(this.referenceFrame, date);
        final Transform transform2 = new Transform(date, this.rotation);
        final Transform transform = new Transform(date, transform1, transform2,
            this.getSpinDerivativesComputation());

        // Get components
        final Rotation q = transform.getRotation();
        final Vector3D spin = transform.getRotationRate();
        final Vector3D spinDerivative = transform.getRotationAcceleration();

        // Return attitude
        return new Attitude(frame, new TimeStampedAngularCoordinates(date, q, spin, spinDerivative));
    }

    /**
     * Getter for the reference frame.
     * 
     * @return the reference frame
     */
    public Frame getReferenceFrame() {
        return this.referenceFrame;
    }

    /**
     * Getter for the rotation from reference frame to satellite frame.
     * 
     * @return the rotation from reference frame to satellite frame
     */
    public Rotation getRotation() {
        return this.rotation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%s: referenceFrame=%s, rotation=%s", this.getClass().getSimpleName(),
            this.referenceFrame.toString(), this.rotation.toString());
    }
}
