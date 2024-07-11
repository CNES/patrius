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
 * @history creation 23/07/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:85:04/09/2013:Magnetic dipole model
 * VERSION::DM:289:27/08/2014:Add exception to SpacececraftState.getAttitude()
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.wrenches;

import fr.cnes.sirius.patrius.assembly.models.MagneticMomentProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.models.earth.GeoMagneticField;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class represents a wrench model
 * 
 * @concurrency conditionally thread-safe
 * @concurrency.comment thread-safe if the underlying geo magnetic field is also thread-safe
 * 
 * @author Rami Houdroge
 * @since 2.1
 * @version $$
 * 
 */
public class MagneticWrench implements WrenchModel {

    /**
     * nT to T
     */
    private static final double NANOT_TO_T = 1e-9;
    /**
     * Dipole model
     */
    private final MagneticMomentProvider magneticMoment;
    /**
     * Earths geomagnetic field
     */
    private final GeoMagneticField geoMagField;

    /**
     * Constructor
     * 
     * @param moment
     *        dipole model of spacecraft expressed in spacecraft frame
     * @param field
     *        geomagnetic field
     */
    public MagneticWrench(final MagneticMomentProvider moment, final GeoMagneticField field) {
        this.magneticMoment = moment;
        this.geoMagField = field;
    }

    /**
     * Get the main frame of the spacecraft, it is the frame in which the
     * moment is expressed and the wrench is computed.
     * 
     * @param s
     *        the spacecraft state
     * @return the main frame of the spacecraft, with the attitude accounted for
     * @throws PatriusException
     *         if no attitude is defined
     */
    private Frame getMainFrame(final SpacecraftState s) throws PatriusException {

        // combination of the position and the attitude
        final Transform rotation = new Transform(AbsoluteDate.J2000_EPOCH, s.getAttitude()
            .getOrientation());
        final Transform translation = new Transform(AbsoluteDate.J2000_EPOCH, s.getOrbit().getPVCoordinates());

        final Transform transform = new Transform(AbsoluteDate.J2000_EPOCH, translation, rotation);

        // main part frame
        return new Frame(s.getFrame(), transform, "mainPartFrame");
    }

    /** {@inheritDoc} */
    @Override
    public Wrench computeWrench(final SpacecraftState s) throws PatriusException {

        final Frame frame = this.getMainFrame(s);

        final Vector3D mag = this.geoMagField.calculateField(s.getPVCoordinates(frame).getPosition(), frame,
            s.getDate()).getFieldVector().scalarMultiply(NANOT_TO_T);

        return new Wrench(Vector3D.ZERO, Vector3D.ZERO, Vector3D.crossProduct(
            this.magneticMoment.getMagneticMoment(s.getDate()), mag));
    }

    /** {@inheritDoc} */
    @Override
    public Wrench computeWrench(final SpacecraftState s, final Vector3D origin,
                                final Frame frame) throws PatriusException {

        final Frame oFrame = this.getMainFrame(s);

        // wrench in mass centre of spacecraft
        Wrench wrench = this.computeWrench(s);

        final Transform fr = frame.getTransformTo(oFrame, s.getDate());
        final Vector3D newOrigin = fr.transformPosition(origin);

        wrench = wrench.displace(newOrigin);

        return fr.transformWrench(wrench);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D computeTorque(final SpacecraftState s) throws PatriusException {
        return this.computeWrench(s).getTorque();
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D computeTorque(final SpacecraftState s, final Vector3D origin,
                                  final Frame frame) throws PatriusException {
        return this.computeWrench(s, origin, frame).getTorque();
    }

}
