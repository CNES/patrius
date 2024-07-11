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
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::FA:423:17/11/2015: improve computation times
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:583:11/03/2016:simplification of attitude laws architecture
 * VERSION::DM:603:29/08/2016:deleted deprecated methods and classes in package attitudes
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
 * This class handles a spin stabilized attitude provider.
 * <p>
 * Spin stabilized laws are handled as wrappers for an underlying non-rotating law. This underlying law is typically an
 * instance of {@link CelestialBodyPointed} with the pointing axis equal to the rotation axis, but can in fact be
 * anything.
 * </p>
 * <p>
 * Instances of this class are guaranteed to be immutable.
 * </p>
 * 
 * @author Luc Maisonobe
 */
public class SpinStabilized extends AbstractAttitudeLaw implements AttitudeLawModifier {

    /** Serializable UID. */
    private static final long serialVersionUID = -7025790361794748354L;

    /** Underlying non-rotating attitude law. */
    private final AttitudeLaw nonRotatingLaw;

    /** Start date of the rotation. */
    private final AbsoluteDate start;

    /** Rotation axis in satellite frame. */
    private final Vector3D axis;

    /** Spin rate in radians per seconds. */
    private final double rate;

    /** Spin vector. */
    private final Vector3D spin;

    /**
     * Creates a new instance.
     * 
     * @param pNonRotatingLaw underlying non-rotating attitude law
     * @param pStart start date of the rotation
     * @param pAxis rotation axis in satellite frame
     * @param pRate spin rate in radians per seconds
     */
    public SpinStabilized(final AttitudeLaw pNonRotatingLaw, final AbsoluteDate pStart,
        final Vector3D pAxis, final double pRate) {
        super();
        this.nonRotatingLaw = pNonRotatingLaw;
        this.start = pStart;
        this.axis = pAxis;
        this.rate = pRate;
        this.spin = new Vector3D(this.rate / this.axis.getNorm(), this.axis);
    }

    /** {@inheritDoc} */
    @Override
    public final Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                      final Frame frame) throws PatriusException {

        // get attitude from underlying non-rotating law
        final Attitude base = this.nonRotatingLaw.getAttitude(pvProv, date, frame);
        final Transform baseTransform = new Transform(date, base.getOrientation());

        // compute spin transform due to spin from reference to current date
        final Transform spinInfluence = new Transform(date, new Rotation(this.axis, this.rate
            * date.durationFrom(this.start)), this.spin);

        // combine the two transforms
        final Transform transform = new Transform(date, baseTransform, spinInfluence,
            this.getSpinDerivativesComputation());

        // Get components
        final Rotation q = transform.getRotation();
        final Vector3D s = transform.getRotationRate();
        final Vector3D spinDerivative = transform.getRotationAcceleration();

        // Return attitude
        return new Attitude(frame, new TimeStampedAngularCoordinates(date, q, s, spinDerivative));
    }

    /** {@inheritDoc} */
    @Override
    public final AttitudeLaw getUnderlyingAttitudeLaw() {
        return this.nonRotatingLaw;
    }

    /**
     * Getter for the underlying non-rotating attitude law.
     * 
     * @return the underlying non-rotating attitude law
     */
    public AttitudeLaw getNonRotatingLaw() {
        return this.nonRotatingLaw;
    }

    /**
     * Getter for the start date of the rotation.
     * 
     * @return the start date of the rotation
     */
    public AbsoluteDate getStartDate() {
        return this.start;
    }

    /**
     * Getter for the rotation axis in satellite frame.
     * 
     * @return the rotation axis in satellite frame
     */
    public Vector3D getAxis() {
        return this.axis;
    }

    /**
     * Getter for the spin rate in radians per seconds.
     * 
     * @return the spin rate in radians per seconds
     */
    public double getRate() {
        return this.rate;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format(
            "Attitude law type: SpinStabilized, nonRotatingLaw=%s, start=%s, axis=%s, rate=%s",
            this.nonRotatingLaw.toString(), this.start.toString(), this.axis.toString(), this.rate);
    }
}
