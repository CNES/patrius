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
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:105:21/11/2013: class creation.
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:200:28/08/2014: dealing with a negative mass in the propagator
 * VERSION::DM:226:12/09/2014: problem with event detections.
 * VERSION::DM:454:24/11/2015:Unimplemented method shouldBeRemoved()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.maneuverandapsidedetection;

import java.util.Map;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Impulse maneuver model.
 * <p>
 * This class implements an impulse maneuver as a discrete event that can be provided to any
 * {@link fr.cnes.sirius.patrius.propagation.Propagator
 * Propagator}.
 * </p>
 * <p>
 * The maneuver is triggered when an underlying event generates a
 * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP STOP} event, in which case this class will
 * generate a {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#RESET_STATE RESET_STATE} event (the
 * stop event from the underlying object is therefore filtered out). In the simple cases, the underlying event detector
 * may be a basic {@link fr.cnes.sirius.patrius.propagation.events.DateDetector date event}, but it can also be a more
 * elaborate {@link fr.cnes.sirius.patrius.propagation.events.ApsideDetector apside event} for apogee maneuvers for
 * example.
 * </p>
 * <p>
 * The maneuver is defined by a single velocity increment in satellite frame. The current attitude of the spacecraft,
 * defined by the current spacecraft state, will be used to compute the velocity direction in inertial frame. A typical
 * case for tangential maneuvers is to use a {@link fr.cnes.sirius.patrius.attitudes.LofOffset LOF aligned} attitude
 * provider for state propagation and a velocity increment along the +X satellite axis.
 * </p>
 * <p>
 * Beware that the triggering event detector must behave properly both before and after maneuver. If for example a node
 * detector is used to trigger an inclination maneuver and the maneuver change the orbit to an equatorial one, the node
 * detector will fail just after the maneuver, being unable to find a node on an equatorial orbit! This is a real case
 * that has been encountered during validation ...
 * </p>
 * 
 * @see fr.cnes.sirius.patrius.propagation.Propagator#addEventDetector(EventDetector)
 * @author Luc Maisonobe
 */
public class ImpulseManeuver implements EventDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = -7150871329986590368L;

    /** Triggering event. */
    private final EventDetector trigger;

    /** Velocity increment in satellite frame. */
    private final Vector3D deltaVSat;

    /** Engine exhaust velocity. */
    private final double vExhaust;

    /** Part name. */
    private final String partName;

    /**
     * Build a new instance.
     * 
     * @param trigger2
     *        triggering event
     * @param deltaVSat2
     *        velocity increment in satellite frame
     * @param isp
     *        engine specific impulse (s)
     * @param part
     *        part of the mass model that provides the propellants
     */
    public ImpulseManeuver(final EventDetector trigger2, final Vector3D deltaVSat2, final double isp, final String part) {
        this.trigger = trigger2;
        this.deltaVSat = deltaVSat2;
        this.vExhaust = Constants.G0_STANDARD_GRAVITY * isp;
        this.partName = part;
    }

    /** {@inheritDoc} */
    @Override
    public double getMaxCheckInterval() {
        return this.trigger.getMaxCheckInterval();
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxIterationCount() {
        return this.trigger.getMaxIterationCount();
    }

    /** {@inheritDoc} */
    @Override
    public double getThreshold() {
        return this.trigger.getThreshold();
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldBeRemoved() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public
            Action
            eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                   throws PatriusException {
        // filter underlying event
        return (this.trigger.eventOccurred(s, increasing, forward) == Action.STOP) ? Action.RESET_STATE
            : Action.CONTINUE;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
    }

    /** {@inheritDoc} */
    @Override
    public double g(final SpacecraftState s) throws PatriusException {
        return this.trigger.g(s);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("deprecation")
    public SpacecraftState resetState(final SpacecraftState oldState)
                                                                     throws PatriusException {

        final AbsoluteDate date = oldState.getDate();
        final Attitude attitudeForces = oldState.getAttitudeForces();
        final Attitude attitudeEvents = oldState.getAttitudeEvents();
        final Map<String, double[]> addStates = oldState.getAdditionalStates();

        // convert velocity increment in inertial frame
        final Vector3D deltaV = attitudeEvents.getRotation().applyInverseTo(this.deltaVSat);

        // apply increment to position/velocity
        final PVCoordinates oldPV = oldState.getPVCoordinates();
        final PVCoordinates newPV = new PVCoordinates(oldPV.getPosition(),
            oldPV.getVelocity().add(deltaV));
        final CartesianOrbit newOrbit =
            new CartesianOrbit(newPV, oldState.getFrame(), date, oldState.getMu());

        // compute new mass
        final double[] newMass =
            new double[] { oldState.getMass(this.partName) * MathLib.exp(-deltaV.getNorm() / this.vExhaust) };
        addStates.put(SpacecraftState.MASS + this.partName, newMass);

        // pack everything in a new state
        return new SpacecraftState(oldState.getOrbit().getType().convertType(newOrbit),
            attitudeForces, attitudeEvents, addStates);

    }

    /**
     * Get the triggering event.
     * 
     * @return triggering event
     */
    public EventDetector getTrigger() {
        return this.trigger;
    }

    /**
     * Get the velocity increment in satellite frame.
     * 
     * @return velocity increment in satellite frame
     */
    public Vector3D getDeltaVSat() {
        return this.deltaVSat;
    }

    /**
     * Get the specific impulse.
     * 
     * @return specific impulse
     */
    public double getIsp() {
        return this.vExhaust / Constants.G0_STANDARD_GRAVITY;
    }

    /**
     * 
     * @return 2
     */
    @Override
    public int getSlopeSelection() {
        return 2;
    }

    @Override
    public EventDetector copy() {
        return null;
    }
}
