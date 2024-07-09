/**
 * 
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
 * 
 * @history creation 23/07/2012
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:85:04/09/2013:Generic wrench model
 * VERSION::DM:289:27/08/2014:Add exception to SpacececraftState.getAttitude()
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.wrenches;

import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class represents a generic wrench model.
 * 
 * @concurrency not thread-safe
 * @concurrency.comment uses frames
 * 
 * @author Rami Houdroge
 * @since 2.1
 * @version $$
 */
public class GenericWrenchModel implements WrenchModel {

    /** Forcemodel */
    private final ForceModel forceModel;
    /** Origin of force */
    private final Vector3D origine;

    /**
     * Create a generic wrench model. It is assumed that the application point is expressed in the frame of
     * the main part of the satellite. It is also assumed that the mass center of the spacecraft is the origin of the
     * frame of the main part.
     * 
     * @param force
     *        force
     * @param origin
     *        application point
     */
    public GenericWrenchModel(final ForceModel force, final Vector3D origin) {
        this.forceModel = force;
        this.origine = origin;
    }

    /**
     * Get the main frame of the spacecraft, where the force is expressed
     * 
     * @param s
     *        spacecraft state
     * @return the main frame
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

        // force in the main frame of the spacecraft
        final Vector3D f = this.forceModel.computeAcceleration(s);
        final Frame main = this.getMainFrame(s);
        final Vector3D fInSpcFrame = s.getFrame().getTransformTo(main, s.getDate()).transformVector(f);

        // wrench at given origin
        return new Wrench(Vector3D.ZERO, fInSpcFrame, this.origine.crossProduct(fInSpcFrame));
    }

    /** {@inheritDoc} */
    @Override
    public Wrench computeWrench(final SpacecraftState s, final Vector3D origin,
                                final Frame frame) throws PatriusException {

        // Calculate new origin in main part frame
        final Vector3D f = this.forceModel.computeAcceleration(s);
        final Frame main = this.getMainFrame(s);

        final Transform spcToMain = s.getFrame().getTransformTo(main, s.getDate());
        final Transform fraToMain = frame.getTransformTo(main, s.getDate());

        final Vector3D fInSpcFrame = spcToMain.transformVector(f);
        final Vector3D newOrigin = fraToMain.transformPosition(origin);

        // wrench at given origin
        return new Wrench(newOrigin, fInSpcFrame, this.origine.subtract(newOrigin).crossProduct(fInSpcFrame));
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
