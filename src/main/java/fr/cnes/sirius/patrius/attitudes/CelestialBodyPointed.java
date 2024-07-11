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
 * VERSION::FA:423:17/11/2015: improve computation times
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
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.TimeStampedAngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class handles a celestial body pointed attitude provider.
 * <p>
 * The celestial body pointed law is defined by two main elements:
 * <ul>
 * <li>a celestial body towards which some satellite axis is exactly aimed</li>
 * <li>a phasing reference defining the rotation around the pointing axis</li>
 * </ul>
 * </p>
 * <p>
 * The celestial body implicitly defines two of the three degrees of freedom and the phasing reference defines the
 * remaining degree of freedom. This definition can be represented as first aligning exactly the satellite pointing axis
 * to the current direction of the celestial body, and then to find the rotation around this axis such that the
 * satellite phasing axis is in the half-plane defined by a cut line on the pointing axis and containing the celestial
 * phasing reference.
 * </p>
 * <p>
 * In order for this definition to work, the user must ensure that the phasing reference is <strong>never</strong>
 * aligned with the pointing reference. Since the pointed body moves as the date changes, this should be ensured
 * regardless of the date. A simple way to do this for Sun, Moon or any planet pointing is to choose a phasing reference
 * far from the ecliptic plane. Using <code>Vector3D.PLUS_K</code>, the equatorial pole, is perfect in these cases.
 * </p>
 * <p>
 * Instances of this class are guaranteed to be immutable.
 * </p>
 * 
 * @author Luc Maisonobe
 */
@SuppressWarnings("PMD.NullAssignment")
public class CelestialBodyPointed extends AbstractAttitudeLaw {

    /** Serializable UID. */
    private static final long serialVersionUID = 6222161082155807729L;

    /** Frame in which {@link #phasingCel} is defined. */
    private final Frame celestialFrame;

    /** Celestial body to point at. */
    private final PVCoordinatesProvider pointedBody;

    /** Phasing reference, in celestial frame. */
    private final Vector3D phasingCel;

    /** Satellite axis aiming at the pointed body, in satellite frame. */
    private final Vector3D pointingSat;

    /** Phasing reference, in satellite frame. */
    private final Vector3D phasingSat;

    /**
     * Creates new instance.
     * 
     * @param pCelestialFrame frame in which <code>phasingCel</code> is defined
     * @param pPointedBody celestial body to point at
     * @param pPhasingCel phasing reference, in celestial frame
     * @param pPointingSat satellite vector defining the pointing direction
     * @param pPhasingSat phasing reference, in satellite frame
     */
    public CelestialBodyPointed(final Frame pCelestialFrame,
        final PVCoordinatesProvider pPointedBody, final Vector3D pPhasingCel,
        final Vector3D pPointingSat, final Vector3D pPhasingSat) {
        super();
        this.celestialFrame = pCelestialFrame;
        this.pointedBody = pPointedBody;
        this.phasingCel = pPhasingCel;
        this.pointingSat = pPointingSat;
        this.phasingSat = pPhasingSat;
    }

    /** {@inheritDoc} */
    @Override
    public final Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                      final Frame frame) throws PatriusException {
        final PVCoordinates satPV = pvProv.getPVCoordinates(date, this.celestialFrame);

        // compute celestial references at the specified date
        final PVCoordinates bodyPV = this.pointedBody.getPVCoordinates(date, this.celestialFrame);
        final PVCoordinates pointing = new PVCoordinates(satPV, bodyPV);
        final Vector3D pointingP = pointing.getPosition();
        final double r2 = Vector3D.dotProduct(pointingP, pointingP);

        // evaluate instant rotation axis due to sat and body motion only (no phasing yet)
        final Vector3D rotAxisCel =
            new Vector3D(1 / r2, Vector3D.crossProduct(pointingP, pointing.getVelocity()));

        // fix instant rotation to take phasing constraint into account
        // (adding a rotation around pointing axis ensuring the motion of the phasing axis
        // is constrained in the pointing-phasing plane)
        final Vector3D v1 = Vector3D.crossProduct(rotAxisCel, this.phasingCel);
        final Vector3D v2 = Vector3D.crossProduct(pointingP, this.phasingCel);
        final double compensation = -Vector3D.dotProduct(v1, v2) / v2.getNormSq();
        final Vector3D phasedRotAxisCel = new Vector3D(1.0, rotAxisCel, compensation, pointingP);

        // compute rotation from celestial frame to satellite frame
        final Rotation celToSatRotation =
            new Rotation(this.pointingSat, this.phasingSat, pointingP, this.phasingCel);

        // build transform combining rotation and instant rotation axis
        Transform transform =
            new Transform(date, celToSatRotation,
                celToSatRotation.applyInverseTo(phasedRotAxisCel),
                this.getSpinDerivativesComputation() ? Vector3D.ZERO : null);
        if (frame != this.celestialFrame) {
            // prepend transform from specified frame to celestial frame
            transform =
                new Transform(date, frame.getTransformTo(this.celestialFrame, date,
                    this.getSpinDerivativesComputation()), transform,
                    this.getSpinDerivativesComputation());
        }

        // Get components
        final Rotation q = transform.getRotation();
        final Vector3D spin = transform.getRotationRate();
        final Vector3D spinDerivative = transform.getRotationAcceleration();

        // Return attitude
        return new Attitude(frame, new TimeStampedAngularCoordinates(date, q, spin, spinDerivative));
    }
}
