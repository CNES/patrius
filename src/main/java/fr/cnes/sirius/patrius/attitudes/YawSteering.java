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
* VERSION:4.3:DM:DM-2104:15/05/2019:[Patrius] Rendre generiques les classes GroundPointing et NadirPointing
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:15/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:583:11/03/2016:simplification of attitude laws architecture
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.TimeStampedAngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class handles yaw steering law.
 * 
 * <p>
 * Yaw steering is mainly used for low Earth orbiting satellites with no missions-related constraints on yaw angle. It
 * sets the yaw angle in such a way the solar arrays have maximal lightning without changing the roll and pitch.
 * </p>
 * <p>
 * The motion in yaw is smooth when the Sun is far from the orbital plane, but gets more and more <i>square like</i> as
 * the Sun gets closer to the orbital plane. The degenerate extreme case with the Sun in the orbital plane leads to a
 * yaw angle switching between two steady states, with instantaneaous &pi; radians rotations at each switch, two times
 * per orbit. This degenerate case is clearly not operationally sound so another pointing mode is chosen when Sun comes
 * closer than some predefined threshold to the orbital plane.
 * </p>
 * <p>
 * This class can handle (for now) only a theoretically perfect yaw steering (i.e. the yaw angle is exactly the optimal
 * angle). Smoothed yaw steering with a few sine waves approaching the optimal angle will be added in the future if
 * needed.
 * </p>
 * <p>
 * This attitude is implemented as a wrapper on top of an underlying ground pointing law that defines the roll and pitch
 * angles.
 * </p>
 * <p>
 * Instances of this class are guaranteed to be immutable.
 * </p>
 * 
 * @author Luc Maisonobe
 */
public class YawSteering extends AbstractGroundPointingWrapper {

    /** Serializable UID. */
    private static final long serialVersionUID = -5804405406938727964L;

    /** Pointing axis. */
    private static final PVCoordinates PLUS_Z = new PVCoordinates(Vector3D.PLUS_K, Vector3D.ZERO,
        Vector3D.ZERO);

    /** 1.0E9 */
    private static final double C_NANO = 1.0e-9;

    /** Sun motion model. */
    private final PVCoordinatesProvider sun;

    /** Satellite axis that must be roughly in Sun direction. */
    private final Vector3D phasingAxis;

    /**
     * Creates a new instance.
     * 
     * @param groundPointingLaw ground pointing attitude provider without yaw compensation
     * @param sunIn sun motion model
     * @param phasingAxisIn satellite axis that must be roughly in Sun direction (if solar arrays
     *        rotation axis is Y, then this axis should be either +X or -X)
     */
    public YawSteering(final AbstractGroundPointing groundPointingLaw,
        final PVCoordinatesProvider sunIn,
        final Vector3D phasingAxisIn) {
        super(groundPointingLaw);
        this.sun = sunIn;
        this.phasingAxis = phasingAxisIn;
    }

    /**
     * Constructor. Create a BodyCenterGroundPointing attitude provider with specified los axis in
     * satellite frame.
     * 
     * @param groundPointingLaw ground pointing attitude provider without yaw compensation
     * @param sunIn sun motion model
     * @param phasingAxisIn satellite axis that must be roughly in Sun direction (if solar arrays
     *        rotation axis is Y, then this axis should be either +X or -X)
     * @param losInSatFrameVec LOS in satellite frame axis
     * @param losNormalInSatFrameVec LOS normal axis in satellite frame
     */
    public YawSteering(final AbstractGroundPointing groundPointingLaw,
        final PVCoordinatesProvider sunIn,
        final Vector3D phasingAxisIn, final Vector3D losInSatFrameVec,
        final Vector3D losNormalInSatFrameVec) {
        // Call constructor of superclass
        super(groundPointingLaw, losInSatFrameVec, losNormalInSatFrameVec);
        this.sun = sunIn;
        this.phasingAxis = phasingAxisIn;
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                final Frame frame) throws PatriusException {

        // Attitude from base attitude provider
        final Attitude base = this.getBaseState(pvProv, date, frame);

        // Compensation
        final TimeStampedAngularCoordinates compensation = this.getCompensation(pvProv, date, frame,
            base);

        // Add compensation
        return new Attitude(frame, base.getOrientation().addOffset(compensation,
            this.getSpinDerivativesComputation()));
    }

    /** {@inheritDoc} */
    @Override
    public TimeStampedAngularCoordinates getCompensation(final PVCoordinatesProvider pvProv,
                                                         final AbsoluteDate date, final Frame orbitFrame,
                                                         final Attitude base)
                                                                             throws PatriusException {

        // Compensation rotation definition :
        // . Z satellite axis is unchanged
        // . phasing axis shall be aligned to sun direction
        final PVCoordinates sunDirection = new PVCoordinates(pvProv.getPVCoordinates(date,
            orbitFrame), this.sun.getPVCoordinates(date, orbitFrame));
        final PVCoordinates sunNormal = PVCoordinates.crossProduct(PLUS_Z, base.getOrientation()
            .applyTo(sunDirection));

        final PVCoordinates phasingNormal = new PVCoordinates(Vector3D.crossProduct(
            Vector3D.PLUS_K, this.phasingAxis).normalize(), Vector3D.ZERO, Vector3D.ZERO);

        // compensation in base attitude frame
        return new TimeStampedAngularCoordinates(date,
            PLUS_Z, sunNormal.normalize(), PLUS_Z, phasingNormal, C_NANO,
            this.getSpinDerivativesComputation());
    }

    /**
     * Getter for the satellite axis that must be roughly in Sun direction.
     * 
     * @return the satellite axis that must be roughly in Sun direction.
     */
    public Vector3D getPhasingAxis() {
        return this.phasingAxis;
    }

    /**
     * Returns the Sun.
     * 
     * @return the Sun
     */
    public PVCoordinatesProvider getSun() {
        return this.sun;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("%s: groundPointingLaw=%s, phasingAxi=%s", this.getClass().getSimpleName(),
            this.getGroundPointingLaw().toString(), this.phasingAxis.toString());
    }
}
