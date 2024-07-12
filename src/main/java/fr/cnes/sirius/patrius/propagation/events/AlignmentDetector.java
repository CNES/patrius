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
* VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:454:24/11/2015:Add constructors, overload method shouldBeRemoved() and adapt eventOccured()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Finder for satellite/body alignment events.
 * <p>
 * This class finds alignment events.
 * </p>
 * <p>
 * Alignment means the conjunction, with some threshold angle, between the satellite position and the projection in the
 * orbital plane of some body position.
 * </p>
 * <p>
 * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when alignment is
 * reached. This can be changed by using provided constructors.
 * </p>
 * 
 * @see fr.cnes.sirius.patrius.propagation.Propagator#addEventDetector(EventDetector)
 * @author Pascal Parraud
 */
public class AlignmentDetector extends AbstractDetector {

    /** Default convergence threshold (in % of Keplerian period). */
    private static final double DEFAULT_THRESHOLD = 1.0e-13;

    /** Serializable UID. */
    private static final long serialVersionUID = -5512125598111644915L;

    /** Body to align. */
    private final PVCoordinatesProvider body;

    /** Alignment angle (rad). */
    private final double alignAngle;

    /** Cosinus of alignment angle. */
    private final double cosAlignAngle;

    /** Sinus of alignment angle. */
    private final double sinAlignAngle;

    /**
     * Build a new alignment detector.
     * <p>
     * The orbit is used only to set an upper bound for the max check interval to period/3 and to set the convergence
     * threshold according to orbit size.
     * </p>
     * <p>
     * The default behavior is to {@link EventDetector.Action#STOP stop} propagation when the expected alignment is
     * reached.
     * </p>
     * 
     * @param orbit initial orbit
     * @param bodyIn the body to align
     * @param alignAngleIn the alignment angle (rad)
     */
    public AlignmentDetector(final Orbit orbit, final PVCoordinatesProvider bodyIn,
        final double alignAngleIn) {
        this(orbit, bodyIn, alignAngleIn, DEFAULT_THRESHOLD * orbit.getKeplerianPeriod(),
            Action.STOP, Action.CONTINUE);
    }

    /**
     * Build a new alignment detector.
     * <p>
     * The orbit is used only to set an upper bound for the max check interval to period/3.
     * </p>
     * <p>
     * The default behavior is to {@link EventDetector.Action#STOP stop} propagation when the expected alignment is
     * reached.
     * </p>
     * 
     * @param orbit initial orbit
     * @param bodyIn the body to align
     * @param alignAngleIn the alignment angle (rad)
     * @param thresholdIn convergence threshold (s)
     */
    public AlignmentDetector(final Orbit orbit, final PVCoordinatesProvider bodyIn,
        final double alignAngleIn, final double thresholdIn) {
        this(orbit, bodyIn, alignAngleIn, thresholdIn, Action.STOP, Action.CONTINUE);
    }

    /**
     * Build a new alignment detector.
     * <p>
     * The orbit is used only to set an upper bound for the max check interval to period/3.
     * </p>
     * 
     * @param orbit initial orbit
     * @param bodyIn the body to align
     * @param alignAngleIn the alignment angle (rad)
     * @param threshold convergence threshold (s)
     * @param actionStart action performed when the alignment start
     * @param actionEnd action performed when the alignment end
     */
    public AlignmentDetector(final Orbit orbit, final PVCoordinatesProvider bodyIn,
        final double alignAngleIn, final double threshold, final Action actionStart,
        final Action actionEnd) {
        this(orbit, bodyIn, alignAngleIn, threshold, actionStart, actionEnd, false, false);
    }

    /**
     * Build a new alignment detector.
     * <p>
     * The orbit is used only to set an upper bound for the max check interval to period/3.
     * </p>
     * 
     * @param orbit initial orbit
     * @param bodyIn the body to align
     * @param alignAngleIn the alignment angle (rad)
     * @param threshold convergence threshold (s)
     * @param actionStart action performed when the alignment start
     * @param actionEnd action performed when the alignment end
     * @param removeStart true if detector should be removed when the alignment start
     * @param removeEnd true if detector should be removed when the alignment end
     * @since 3.1
     */
    public AlignmentDetector(final Orbit orbit, final PVCoordinatesProvider bodyIn,
        final double alignAngleIn, final double threshold, final Action actionStart,
        final Action actionEnd, final boolean removeStart, final boolean removeEnd) {
        super(orbit.getKeplerianPeriod() / 3, threshold, actionStart, actionEnd, removeStart, removeEnd);
        this.body = bodyIn;
        this.alignAngle = alignAngleIn;
        final double[] sincos = MathLib.sinAndCos(alignAngleIn);
        this.sinAlignAngle = sincos[0];
        this.cosAlignAngle = sincos[1];
    }

    /**
     * Build a new alignment detector.
     * <p>
     * The orbit is used only to set an upper bound for the max check interval to period/3.
     * </p>
     * 
     * @param maxcheck max check interval
     * @param bodyIn the body to align
     * @param alignAngleIn the alignment angle (rad)
     * @param threshold convergence threshold (s)
     * @param actionStart action performed when the alignment start
     * @param actionEnd action performed when the alignment end
     * @param removeStart true if detector should be removed when the alignment start
     * @param removeEnd true if detector should be removed when the alignment end
     * @since 3.1
     */
    public AlignmentDetector(final PVCoordinatesProvider bodyIn, final double alignAngleIn,
        final double maxcheck, final double threshold, final Action actionStart,
        final Action actionEnd, final boolean removeStart, final boolean removeEnd) {
        super(maxcheck, threshold, actionStart, actionEnd, removeStart, removeEnd);
        this.body = bodyIn;
        this.alignAngle = alignAngleIn;
        final double[] sincos = MathLib.sinAndCos(alignAngleIn);
        this.sinAlignAngle = sincos[0];
        this.cosAlignAngle = sincos[1];
    }

    /**
     * Get the body to align.
     * 
     * @return the body to align
     */
    public PVCoordinatesProvider getPVCoordinatesProvider() {
        return this.body;
    }

    /**
     * Get the alignment angle (rad).
     * 
     * @return the alignment angle
     */
    public double getAlignAngle() {
        return this.alignAngle;
    }

    /**
     * Handle an alignment event and choose what to do next.
     * <p>
     * The default implementation behavior is to {@link EventDetector.Action#STOP stop} propagation when alignment is
     * reached.
     * </p>
     * 
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event
     * @param forward if true, the integration variable (time) increases during integration.
     * @return the action performed when alignment is reached.
     * @exception PatriusException if some specific error occurs
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        return super.eventOccurred(s, increasing, forward);
    }

    /**
     * Compute the value of the switching function. This function measures the difference between
     * the alignment angle and the angle between the satellite position and the body position
     * projection in the orbital plane.
     * 
     * @param state state
     * @return value of the switching function
     * @exception PatriusException if some specific error occurs
     */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState state) throws PatriusException {

        // Intermediate computations
        final PVCoordinates pv = state.getPVCoordinates();
        final Vector3D a = pv.getPosition().normalize();
        final Vector3D z = pv.getMomentum().negate().normalize();
        final Vector3D b = Vector3D.crossProduct(a, z).normalize();
        final Vector3D x = new Vector3D(this.cosAlignAngle, a, this.sinAlignAngle, b);
        final Vector3D y = new Vector3D(this.sinAlignAngle, a, -this.cosAlignAngle, b);
        final Vector3D pb = this.body.getPVCoordinates(state.getDate(), state.getFrame()).getPosition();
        // Computation
        final double beta = MathLib.atan2(Vector3D.dotProduct(pb, y), Vector3D.dotProduct(pb, x));
        final double betm = -FastMath.PI - beta;
        final double betp = FastMath.PI - beta;

        // Compute result
        final double res;
        if (beta < betm) {
            res = betm;
        } else if (beta < betp) {
            res = beta;
        } else {
            res = betp;
        }
        // Return result
        return res;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>body: {@link PVCoordinatesProvider}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        return new AlignmentDetector(this.body, this.alignAngle, this.getMaxCheckInterval(), this.getThreshold(),
            this.getActionAtEntry(), this.getActionAtExit(), this.isRemoveAtEntry(), this.isRemoveAtExit());
    }
}
