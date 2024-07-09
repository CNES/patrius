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
 * @history creation 23/04/12
 */
/*
 * HISTORY
* VERSION:4.8:DM:DM-2929:15/11/2021:[PATRIUS] Harmonisation des modeles de troposphere 
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:88:18/11/2013: the getSignalTropoCorrection method has been modified
 * VERSION::FA:212:17/03/2014: correction of the computeSignalPropagation method
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.math.exception.ConvergenceException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.signalpropagation.troposphere.TroposphericCorrection;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Model for the computation of a signal propagation vector and toolbox
 * for the different corrections to be applied to it.
 *
 * @concurrency not thread-safe
 *
 * @concurrency.comment use of the frames tree
 *
 * @author Thomas Trapier
 *
 * @version $Id$
 *
 * @since 1.2
 *
 */
public class SignalPropagationModel {

    /** fail criteria */
    private static final int FAILCRITERIA = 100;
    /** the threshold for the iterative computations */
    private final double inThreshold;
    /** the work frame */
    private final Frame inFrame;

    /** the fixed date of computation */
    public enum FixedDate {
        /** Emission */
        EMISSION,
        /** Reception */
        RECEPTION;
    }

    /**
     * Constructor of the signal propagation model.
     *
     * @param frame
     *        the work frame : must be inertial
     * @param threshold
     *        the iterative computation algorithm convergence threshold: propagation time precision
     *        required
     * @throws IllegalArgumentException if the provided frame is not pseudo-inertial
     */
    public SignalPropagationModel(final Frame frame, final double threshold) {

        // The frame must be inertial
        if (!frame.isPseudoInertial()) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_NOT_INERTIAL_FRAME);
        }

        this.inThreshold = threshold;
        this.inFrame = frame;
    }

    /**
     * Computes the signal propagation object in the void at a particular date
     *
     * @param emitter
     *        the PVCoordinatesProvider of the emitter object
     * @param receiver
     *        the PVCoordinatesProvider of the receiver object
     * @param date
     *        the emission or reception date
     * @param fixedDateType
     *        type of the previous given date : emission or reception
     * @return the signal propagation object
     * @throws PatriusException
     *         if a problem occurs during PVCoodinates providers manipulations
     */
    public final SignalPropagation computeSignalPropagation(final PVCoordinatesProvider emitter,
                                                            final PVCoordinatesProvider receiver,
                                                            final AbsoluteDate date,
            final FixedDate fixedDateType) throws PatriusException {

        // Dates initializations
        AbsoluteDate emissionDate;
        AbsoluteDate receptionDate;

        // General initializations
        Vector3D propagation = Vector3D.ZERO;
        double newTprop = 0.0;
        double criteria = 1.0;
        int failCheck = 0;

        PVCoordinates emitterPV;
        PVCoordinates receiverPV;
        final Vector3D velEmitter;
        final Vector3D velReceiver;

        final SignalPropagation signalPropagation;

        // if the EMISSION DATE IS KNWON
        if (fixedDateType == FixedDate.EMISSION) {
            emissionDate = date;

            // initial values
            emitterPV = emitter.getPVCoordinates(emissionDate, this.inFrame);
            receptionDate = emissionDate;
            receiverPV = PVCoordinates.ZERO;

            while (criteria > this.inThreshold) {

                // after 100 iterations, the computation is assumed to have fail
                failCheck++;
                if (failCheck > FAILCRITERIA) {
                    throw new ConvergenceException();
                }

                final double tProp = newTprop;
                // computation of the new reception date
                receptionDate = emissionDate.shiftedBy(tProp);

                // receiver position at the approximative reception date
                receiverPV = receiver.getPVCoordinates(receptionDate, this.inFrame);

                // computation of the propagation vector
                propagation = receiverPV.getPosition().subtract(emitterPV.getPosition());

                // computation of the new propagation time
                newTprop = propagation.getNorm() / Constants.SPEED_OF_LIGHT;

                // update of the end criteria
                criteria = MathLib.abs(tProp - newTprop);
            }

            velEmitter = emitterPV.getVelocity();
            velReceiver = receiverPV.getVelocity();

            signalPropagation = new SignalPropagation(propagation, emissionDate, receptionDate,
                    this.inFrame, FixedDate.EMISSION, velEmitter, velReceiver);
        } else {
            // if the RECEPTION DATE IS KNWON
            receptionDate = date;

            // initial values
            emitterPV = PVCoordinates.ZERO;
            emissionDate = receptionDate;
            receiverPV = receiver.getPVCoordinates(receptionDate, this.inFrame);

            while (criteria > this.inThreshold) {

                // after 100 iterations, the computation is assumed to have fail
                failCheck++;
                if (failCheck > FAILCRITERIA) {
                    throw new ConvergenceException();
                }

                final double tProp = newTprop;
                // computation of the new emission date
                emissionDate = receptionDate.shiftedBy(-tProp);

                // emitter position at the approximative emission date
                emitterPV = emitter.getPVCoordinates(emissionDate, this.inFrame);

                // computation of the propagation vector
                propagation = receiverPV.getPosition().subtract(emitterPV.getPosition());

                // computation of the new propagation time
                newTprop = propagation.getNorm() / Constants.SPEED_OF_LIGHT;

                // update of the end criteria
                criteria = MathLib.abs(tProp - newTprop);
            }

            velEmitter = emitterPV.getVelocity();
            velReceiver = receiverPV.getVelocity();

            signalPropagation = new SignalPropagation(propagation, emissionDate, receptionDate,
                    this.inFrame, FixedDate.RECEPTION, velEmitter, velReceiver);
        }

        return signalPropagation;
    }

    /**
     * Computes the tropospheric effects corrections to be applied to a given
     * {@link SignalPropagation} object.
     *
     * @param correction
     *        the tropospheric correction model
     * @param signal
     *        the signal to correct
     * @param topo
     *        the ground antenna topocentric frame
     * @return the signal delay correction [s]
     * @throws PatriusException
     *         if frame transformation problem occurs
     */
    public double getSignalTropoCorrection(final TroposphericCorrection correction,
            final SignalPropagation signal,
            final TopocentricFrame topo) throws PatriusException {

        final Vector3D signalVect = signal.getVector(topo);

        // true (geometric) elevation of the signal
        final double value = signalVect.normalize().getZ();
        final double trueElevation = MathLib.asin(MathLib.min(1.0, MathLib.max(-1.0, value)));

        // correction computation
        return correction.computeSignalDelay(trueElevation) * Constants.SPEED_OF_LIGHT;
    }
}
