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
 * VERSION::DM:200:28/08/2014: (creation) dealing with a negative mass in the propagator
 * VERSION::FA:373:12/01/2015: proper handling of mass event detection
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * VERSION::DM:454:24/11/2015:Unimplemented method shouldBeRemoved()
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.propagation.events;

import java.util.List;

import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class creates an event detector that detects when the global mass of the satellite becomes
 * null. This detector is automatically added (in first position) to every propagator and stops the
 * propagation once it detects a null global mass. Since the initial mass is positive or null, the
 * first time where g = 0 will indicate the first time when the mass becomes null.
 * 
 * @author Sophie LAURENS
 * 
 * @version $Id: NullMassDetector.java 18094 2017-10-02 17:18:57Z bignon $
 * 
 * @since 2.3
 * 
 */
public class NullMassDetector extends AbstractDetector {

    /** Default convergence threshold (s). */
    private static final double DEFAULT_THRESHOLD = 10.e-10;

    /** Default convergence threshold (s). */
    private static final double DEFAULT_MAXCHECK = 10.e9;

    // attributes
    /** Default serial version. */
    private static final long serialVersionUID = 1L;
    /** Zero constant. */
    private static final double ZERO = 0.0;
    /** Mass provider. */
    private final MassProvider mass;
    /** True is detector has been triggered. */
    private boolean triggered;

    // constructors
    /**
     * Inherited constructor.
     * 
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param massModel mass model
     */
    public NullMassDetector(final double maxCheck, final double threshold,
        final MassProvider massModel) {
        super(maxCheck, threshold);
        this.mass = massModel;
        this.triggered = false;
    }

    /**
     * Inherited constructor.
     * 
     * @param slopeSelection g-function slope selection (0, 1, or 2)
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param massModel mass model
     */
    public NullMassDetector(final int slopeSelection, final double maxCheck,
        final double threshold, final MassProvider massModel) {
        super(slopeSelection, maxCheck, threshold);
        this.mass = massModel;
        this.triggered = false;
    }

    /**
     * Build a new instance (based on DateDetector).
     * <p>
     * This constructor is dedicated to single date detection. MaxCheck is set to 10.e9, so almost no other date can be
     * added. Tolerance is set to 10.e-10.
     * </p>
     * 
     * @param massModel mass model
     */
    public NullMassDetector(final MassProvider massModel) {
        super(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
        this.mass = massModel;
        this.triggered = false;
    }

    // inherited methods
    /**
     * If the global mass of the satellite becomes negative, the propagation is stopped.
     * 
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases when times increases
     *        around event (note that increase is measured with respect to physical time, not with
     *        respect to propagation which may go backward in time)
     * @param forward if true, the integration variable (time) increases during integration.
     * @return EventDetector.Action.STOP
     * 
     */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                final boolean forward) {
        this.triggered = true;
        return Action.STOP;
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

        // get the total mass of the satellite
        final List<String> partNamesList = this.mass.getAllPartsNames();
        double totalMass = ZERO;
        for (int partNumber = 0; partNumber < partNamesList.size(); partNumber++) {
            totalMass = totalMass + s.getMass(partNamesList.get(partNumber));
        }
        return totalMass;
    }

    /**
     * Returns true if detector has been triggered.
     * 
     * @return true if detector has been triggered
     */
    public final boolean isTriggered() {
        return this.triggered;
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldBeRemoved() {
        // This detector should never be removed
        return false;
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
        return new NullMassDetector(this.getSlopeSelection(), this.getMaxCheckInterval(), this.getThreshold(),
            this.mass);
    }
}
