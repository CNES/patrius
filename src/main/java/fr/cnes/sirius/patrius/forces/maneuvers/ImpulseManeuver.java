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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.4:DM:DM-2126:04/10/2019:[PATRIUS] Calcul du DeltaV realise
 * VERSION:4.4:DM:DM-2112:04/10/2019:[PATRIUS] Manoeuvres impulsionnelles sur increments orbitaux
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1173:26/06/2017:add propulsive and tank properties
 * VERSION::FA:1449:15/03/2018:remove PropulsiveProperty name attribute
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.maneuvers;

import fr.cnes.sirius.patrius.assembly.properties.PropulsiveProperty;
import fr.cnes.sirius.patrius.assembly.properties.TankProperty;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Impulse maneuver model.
 * <p>
 * This class implements an impulse maneuver as a discrete event that can be provided to any
 * {@link fr.cnes.sirius.patrius.propagation.Propagator Propagator}.
 * </p>
 * <p>
 * The impulse maneuver is associated to a triggering {@link fr.cnes.sirius.patrius.propagation.events.EventDetector
 * EventDetector}: the maneuver is triggered <b>only if</b> the underlying event generates a
 * {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#STOP STOP} event, in which case this class will
 * generate a {@link fr.cnes.sirius.patrius.propagation.events.EventDetector.Action#RESET_STATE RESET_STATE} event (the
 * stop event from the underlying object is therefore filtered out). In the simple cases, the underlying event detector
 * may be a basic {@link fr.cnes.sirius.patrius.propagation.events.DateDetector date event}, but it can also be a more
 * elaborate {@link fr.cnes.sirius.patrius.propagation.events.ApsideDetector apside event} for apogee maneuvers for
 * example.
 * </p>
 * <p>
 * The maneuver is defined by a single velocity increment satellite frame or in a frame defined by user or in a LOF with
 * type defined by user. The current attitude of the spacecraft, defined by the current spacecraft state, will be used
 * to compute the velocity direction in inertial frame when direction is defined in satellite frame. A typical case for
 * tangential maneuvers is to use a {@link fr.cnes.sirius.patrius.attitudes.LofOffset LOF aligned} attitude provider for
 * state propagation and a velocity increment along the +X satellite axis.
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
@SuppressWarnings("PMD.NullAssignment")
public class ImpulseManeuver extends AbstractDetector implements Maneuver {

    /** Serializable UID. */
    private static final long serialVersionUID = -7150871329986590368L;

    /** Velocity increment in the frame defined by the user. */
    protected Vector3D deltaVSat;

    /** Triggering event. */
    private final EventDetector trigger;

    /**
     * Frame of the velocity increment. If null, the velocity increment is expressed in the
     * satellite frame
     */
    private final Frame frame;

    /** Local orbital frame type. */
    private final LOFType lofType;

    /** Propulsive property. */
    private final PropulsiveProperty engineProp;

    /** Tank property. */
    private final TankProperty tankProp;

    /** Mass provider. */
    private final MassProvider mass;

    /** If true, the integration variable (time) increases during integration. */
    private boolean forwardLocal;
    
    /** False when the maneuver hasn't occurred, true otherwise. **/
    private boolean hasFiredFlag;

    /**
     * Build a new instance.
     * 
     * @param inTrigger triggering event (it must generate a <b>STOP</b> event action to trigger the
     *        maneuver)
     * @param inDeltaVSat velocity increment in satellite frame
     * @param isp engine specific impulse (s)
     * @param massModel mass model
     * @param part part of the mass model that provides the propellants
     * @throws PatriusException thrown if mass from mass provider is negative
     */
    public ImpulseManeuver(final EventDetector inTrigger, final Vector3D inDeltaVSat,
        final double isp, final MassProvider massModel, final String part)
        throws PatriusException {
        super(inTrigger.getSlopeSelection(), inTrigger.getMaxCheckInterval(), inTrigger
            .getThreshold());
        this.trigger = inTrigger;
        this.deltaVSat = inDeltaVSat;
        // Build a constant Propulsive property: thrust unused
        this.engineProp = new PropulsiveProperty(Double.NaN, isp);
        this.tankProp = new TankProperty(massModel.getMass(part));
        this.tankProp.setPartName(part);
        this.frame = null;
        this.lofType = null;
        this.mass = massModel;
        this.hasFiredFlag = false;
    }

    /**
     * Build a new instance.
     * 
     * Note : The frame could be set to null to express the velocity increment in spacecraft frame.
     * WARNING : It is not recommended to use this constructor with a LocalOrbitalFrame built with a
     * PVCoordinatesProvider equal to the current propagator.
     * 
     * @param inTrigger triggering event (it must generate a <b>STOP</b> event action to trigger the
     *        maneuver)
     * @param inDeltaVSat velocity increment in the frame defined by the user
     * @param inFrame the frame of the velocity increment. Null frame means spacecraft frame.
     * @param isp engine specific impulse (s)
     * @param massModel mass model
     * @param part part of the mass model that provides the propellants
     * @throws PatriusException thrown if mass from mass provider is negative
     */
    public ImpulseManeuver(final EventDetector inTrigger, final Vector3D inDeltaVSat,
        final Frame inFrame, final double isp, final MassProvider massModel, final String part)
        throws PatriusException {
        super(inTrigger.getSlopeSelection(), inTrigger.getMaxCheckInterval(), inTrigger
            .getThreshold());

        this.trigger = inTrigger;
        this.deltaVSat = inDeltaVSat;
        // Build a constant Propulsive property: thrust unused
        this.engineProp = new PropulsiveProperty(Double.NaN, isp);
        this.tankProp = new TankProperty(massModel.getMass(part));
        this.tankProp.setPartName(part);
        this.frame = inFrame;
        this.lofType = null;
        this.mass = massModel;
        this.hasFiredFlag = false;
    }

    /**
     * Build a new instance with a LocalOrbitalFrame.
     * 
     * @param inTrigger triggering event (it must generate a <b>STOP</b> event action to trigger the
     *        maneuver)
     * @param inDeltaVSat velocity increment in the frame defined by the user
     * @param isp engine specific impulse (s)
     * @param massModel mass model
     * @param part part of the mass model that provides the propellants
     * @param inLofType the LOF type of the velocity increment
     * @throws PatriusException thrown if mass from mass provider is negative
     */
    public ImpulseManeuver(final EventDetector inTrigger, final Vector3D inDeltaVSat,
        final double isp, final MassProvider massModel, final String part,
        final LOFType inLofType) throws PatriusException {
        super(inTrigger.getSlopeSelection(), inTrigger.getMaxCheckInterval(), inTrigger
            .getThreshold());
        this.trigger = inTrigger;
        this.deltaVSat = inDeltaVSat;
        // Build a constant Propulsive property: thrust unused
        this.engineProp = new PropulsiveProperty(Double.NaN, isp);
        this.tankProp = new TankProperty(massModel.getMass(part));
        this.tankProp.setPartName(part);
        this.frame = null;
        this.lofType = inLofType;
        this.mass = massModel;
        this.hasFiredFlag = false;
    }

    /**
     * Build a new instance using propulsive and engine property.
     * 
     * @param inTrigger triggering event (it must generate a <b>STOP</b> event action to trigger the
     *        maneuver)
     * @param inDeltaVSat velocity increment in satellite frame
     * @param engine engine property (specific impulse)
     * @param massModel mass model
     * @param tank tank property gathering mass and part name information
     */
    public ImpulseManeuver(final EventDetector inTrigger, final Vector3D inDeltaVSat,
        final PropulsiveProperty engine, final MassProvider massModel, final TankProperty tank) {
        super(inTrigger.getSlopeSelection(), inTrigger.getMaxCheckInterval(), inTrigger
            .getThreshold());
        this.trigger = inTrigger;
        this.deltaVSat = inDeltaVSat;
        this.engineProp = engine;
        this.tankProp = tank;
        this.frame = null;
        this.lofType = null;
        this.mass = massModel;
        this.hasFiredFlag = false;
    }

    /**
     * Build a new instance using propulsive and engine property.
     * 
     * Note : The frame could be set to null to express the velocity increment in spacecraft frame.
     * WARNING : It is not recommended to use this constructor with a LocalOrbitalFrame built with a
     * PVCoordinatesProvider equal to the current propagator.
     * 
     * @param inTrigger triggering event (it must generate a <b>STOP</b> event action to trigger the
     *        maneuver)
     * @param inDeltaVSat velocity increment in the frame defined by the user
     * @param inFrame the frame of the velocity increment. Null frame means spacecraft frame
     * @param engine engine property (specific impulse)
     * @param massModel mass model
     * @param tank tank property gathering mass and part name information
     */
    public ImpulseManeuver(final EventDetector inTrigger, final Vector3D inDeltaVSat,
        final Frame inFrame, final PropulsiveProperty engine, final MassProvider massModel,
        final TankProperty tank) {
        super(inTrigger.getSlopeSelection(), inTrigger.getMaxCheckInterval(), inTrigger
            .getThreshold());
        this.trigger = inTrigger;
        this.deltaVSat = inDeltaVSat;
        this.engineProp = engine;
        this.tankProp = tank;
        this.frame = inFrame;
        this.lofType = null;
        this.mass = massModel;
        this.hasFiredFlag = false;
    }

    /**
     * Build a new instance with a LocalOrbitalFrame and using propulsive and engine property.
     * 
     * @param inTrigger triggering event (it must generate a <b>STOP</b> event action to trigger the
     *        maneuver)
     * @param inDeltaVSat velocity increment in the frame defined by the user
     * @param engine engine property (specific impulse)
     * @param massModel mass model
     * @param tank tank property gathering mass and part name information
     * @param inLofType the LOF type of the velocity increment
     */
    public ImpulseManeuver(final EventDetector inTrigger, final Vector3D inDeltaVSat,
        final PropulsiveProperty engine, final MassProvider massModel, final TankProperty tank,
        final LOFType inLofType) {
        super(inTrigger.getSlopeSelection(), inTrigger.getMaxCheckInterval(), inTrigger
            .getThreshold());
        this.trigger = inTrigger;
        this.deltaVSat = inDeltaVSat;
        this.engineProp = engine;
        this.tankProp = tank;
        this.frame = null;
        this.lofType = inLofType;
        this.mass = massModel;
        this.hasFiredFlag = false;
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

    /** {@inheritDoc}. */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) throws PatriusException {
        this.forwardLocal = forward;
        
        // filter underlying event
        return (this.trigger.eventOccurred(s, increasing, forward) == Action.STOP) ? Action.RESET_STATE
            : Action.CONTINUE;
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldBeRemoved() {
        return this.trigger.shouldBeRemoved();
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // Nothing to do
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState s) throws PatriusException {
        return this.trigger.g(s);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws PatriusException thrown if the mass becomes negative
     *         (PatriusMessages.SPACECRAFT_MASS_BECOMES_NEGATIVE)
     * @throws PatriusException thrown if no attitude informations is defined
     * @throws PatriusException thrown if error occurs during transformation
     **/
    @Override
    public SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {

        final AbsoluteDate date = oldState.getDate();
        
        this.hasFiredFlag = true;

        // convert velocity increment in inertial frame
        final Vector3D deltaV;
        if (this.frame == null) {
            if (this.lofType == null) {
                final Attitude attitudeEvents = oldState.getAttitudeEvents();
                // Check if the attitude exists
                if (attitudeEvents == null) {
                    throw new PatriusException(PatriusMessages.NO_ATTITUDE_EVENTS_DEFINED);
                } else {
                    // velocity increment in satellite frame
                    deltaV = attitudeEvents.getRotation().applyTo(this.deltaVSat);
                }
            } else {
                // velocity increment in local orbital frame
                final Transform tranform = this.lofType.transformFromInertial(date,
                    oldState.getPVCoordinates());
                deltaV = tranform.getInverse().transformVector(this.deltaVSat);
            }
        } else {
            // velocity increment in a frame defined by the user
            final Transform tranform = this.frame.getTransformTo(oldState.getFrame(), date);
            deltaV = tranform.transformVector(this.deltaVSat);
        }

        // compute new mass and update mass model
        // deltaV applied onto satellite!
        final double vExhaust = Constants.G0_STANDARD_GRAVITY * this.engineProp.getIsp(oldState);
        final double ratio = MathLib.exp(-deltaV.getNorm() / vExhaust);
        final double oldPartMass = this.mass.getMass(this.tankProp.getPartName());
        final double oldTotalMass = this.mass.getTotalMass();

        final double newPartMass;
        if (this.forwardLocal) {
            newPartMass = oldPartMass - oldTotalMass * (1 - ratio);
        } else {
            newPartMass = oldPartMass + oldTotalMass * (1 - ratio) / ratio;
        }
        if (newPartMass < 0.0) {
            throw new PropagationException(PatriusMessages.NOT_POSITIVE_MASS, newPartMass);
        }
        // NB : additional states map is updated from MassProvider in SpacecraftState constructor
        // update MassProvider
        this.mass.updateMass(this.tankProp.getPartName(), newPartMass);

        // compute thrust direction
        final double direction = this.forwardLocal ? 1.0 : -1.0;

        // apply increment to position/velocity
        final PVCoordinates oldPV = oldState.getPVCoordinates();
        final PVCoordinates newPV = new PVCoordinates(oldPV.getPosition(), oldPV.getVelocity().add(
            new Vector3D(direction, deltaV)));
        final CartesianOrbit newOrbitCartesian = new CartesianOrbit(newPV, oldState.getFrame(),
            date, oldState.getMu());

        // pack everything in a new state
        final Orbit newOrbit = oldState.getOrbit().getType().convertType(newOrbitCartesian);
        return oldState.updateOrbit(newOrbit).addMassProvider(this.mass);
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
     * Get the maneuver instantaneous consumption deltaV.
     * @return the maneuver instantaneous consumption deltaV in maneuver frame (inertial, LOF or satellite)
     */
    public Vector3D getUsedDV() {
        final Vector3D deltaVSatTemp;
        if (hasFiredFlag) {
            deltaVSatTemp = this.deltaVSat;
        } else {
            deltaVSatTemp = Vector3D.ZERO;
        }
        
        return deltaVSatTemp;
    }

    /**
     * Get the specific impulse.
     * <p>
     * Warning: if a variable ISP has been used, NaN will be returned.
     * </p>
     * 
     * @return specific impulse
     */
    public double getIsp() {
        return this.engineProp.getIspParam().getValue();
    }

    /**
     * Get the frame of the velocity increment. Null if the velocity increment is expressed by
     * default in the satellite frame.
     * 
     * @return the frame
     */
    public Frame getFrame() {
        return this.frame;
    }

    /** {@inheritDoc}. */
    @Override
    public int getSlopeSelection() {
        return 2;
    }

    /**
     * Get the tank property.
     * 
     * @return tank property
     */
    public TankProperty getTankProperty() {
        return this.tankProp;
    }

    /**
     * Get the propulsive property.
     * 
     * @return propulsive property
     */
    public PropulsiveProperty getPropulsiveProperty() {
        return this.engineProp;
    }

    /**
     * @return the lofType
     */
    public LOFType getLofType() {
        return this.lofType;
    }
    
    /**
     * Returns the mass provider.
     * @return the mass provider
     */
    public MassProvider getMassProvider() {
        return mass;
    }
    
    /**
     * Return the hasFired variable. False when the maneuver hasn't occurred, true otherwise.
     * @return true is the maneuver has been performed, false otherwise
     */
    public boolean hasFired() {
        return hasFiredFlag;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>mass: {@link MassProvider}</li>
     * <li>frame: {@link Frame}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        final ImpulseManeuver result;
        if (this.frame != null) {
            result = new ImpulseManeuver(this.trigger, new Vector3D(1., this.deltaVSat), this.frame,
                new PropulsiveProperty(this.engineProp), this.mass, new TankProperty(this.tankProp));
        } else if (this.lofType != null) {
            result = new ImpulseManeuver(this.trigger, new Vector3D(1., this.deltaVSat),
                new PropulsiveProperty(this.engineProp), this.mass, new TankProperty(this.tankProp), this.lofType);
        } else {
            result = new ImpulseManeuver(this.trigger, new Vector3D(1., this.deltaVSat),
                new PropulsiveProperty(this.engineProp), this.mass, new TankProperty(this.tankProp));
        }
        result.hasFiredFlag = hasFired();
        return result;
    }
}
