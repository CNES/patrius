/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:200:28/08/2014: (creation) dealing with a negative mass in the propagator
 * VERSION::FA:373:12/01/2015: proper handling of mass event detection
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * VERSION::DM:454:24/11/2015:Unimplemented method shouldBeRemoved()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.propagation.events;

import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class creates an event detector that detects when the mass of the element becomes null. This
 * detector is automatically added (through the continuous thrust) to every propagator and throws a
 * reset_derivatives (because the thrust became null) once it detects a null mass and the mass is
 * set to exactly zero. Since the initial mass of the part is positive or null, the first time where
 * g = 0 will indicate the first time when the mass becomes null.
 * 
 * @author Sophie LAURENS
 * 
 * @version $Id: NullMassPartDetector.java 18094 2017-10-02 17:18:57Z bignon $
 * 
 * @since 2.3
 * 
 */
public class NullMassPartDetector extends AbstractDetector implements EventDetector {

    /** Default convergence threshold (s). */
    private static final double DEFAULT_THRESHOLD = 10.e-10;

    /** Default convergence threshold (s). */
    private static final double DEFAULT_MAXCHECK = 10.e9;

    // attributes
    /** Auto generated. */
    private static final long serialVersionUID = -8442909251407235384L;
    /** Mass equals to zero. */
    private static final double MASS_NULL = 0.0;
    /** Mass provider. */
    private final MassProvider mass;
    /** Name of the part. */
    private final String partName;

    // constructors
    /**
     * Inherited constructor.
     * 
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param massModel mass model
     * @param part name of the part
     * 
     */
    public NullMassPartDetector(final double maxCheck, final double threshold,
        final MassProvider massModel, final String part) {
        super(maxCheck, threshold);
        this.mass = massModel;
        this.partName = part;
    }

    /**
     * Inherited constructor.
     * 
     * @param slopeSelection g-function slope selection (0, 1, or 2)
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param massModel mass model
     * @param part name of the part
     */
    public NullMassPartDetector(final int slopeSelection, final double maxCheck,
        final double threshold, final MassProvider massModel, final String part) {
        super(slopeSelection, maxCheck, threshold);
        this.mass = massModel;
        this.partName = part;
    }

    /**
     * Build a new instance MaxCheck is set to 10.e9, so almost no other date can be added.
     * Tolerance is set to 10.e-10.
     * 
     * @param massModel mass model
     * @param part name of the part
     */
    public NullMassPartDetector(final MassProvider massModel, final String part) {
        super(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
        this.mass = massModel;
        this.partName = part;
    }

    /**
     * Reset the state (including additional states) prior to continue propagation. This method is
     * called after if the mass becomes negative or null : in that case, the mass of the part is set
     * to zero, and this has to be reset in the spacecraftstate
     * 
     * @param oldState the current SpacecraftState whose MassProvider has to be updated
     * @return SpacecraftState
     * @throws PatriusException if mass is negative : bcs function updateMass
     * 
     */
    @Override
    public SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {

        // Update mass provider data: mass and mass derivative
        this.mass.updateMass(this.partName, 0.);
        this.mass.setMassDerivativeZero(this.partName);

        // Update mass equation and pack everything in a new state
        return oldState.updateMass(this.partName, MASS_NULL);
    }

    // inherited methods

    /**
     * If the mass of the element becomes negative, a reset_state is performed.
     * 
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event (note that increase is measured with respect to physical time, not with
     *        respect to propagation which may go backward in time)
     * @param forward if true, the integration variable (time) increases during integration.
     * @return EventDetector.Action.RESET_DERIVATIVES
     * 
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) {
        return Action.RESET_STATE;
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldBeRemoved() {
        // This detector should never be removed
        return false;
    }

    /**
     * Compute the value of the switching function.
     * 
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     * @exception PatriusException if some specific error occurs
     */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public double g(final SpacecraftState s) throws PatriusException {
        return s.getMass(this.partName);
    }

    /**
     * Get the name of the part (attribute).
     * 
     * @return partName String : name of the part.
     */
    public String getPartName() {
        return this.partName;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>mass: {@link MassProvider}</li>
     * </ul>
     * </p>
     */
    @Override
    public EventDetector copy() {
        return new NullMassPartDetector(this.getSlopeSelection(), this.getMaxCheckInterval(), this.getThreshold(),
            this.mass, this.partName);
    }
}
