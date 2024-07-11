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
 * @history creation 23/04/12
 */
/*
 * HISTORY
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3148:10/05/2022:[PATRIUS] Erreur dans la methode getSignalReceptionDate de la classe 
 * AbstractDetector 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
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
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.AzimuthElevationCalculator;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector.PropagationDelayType;
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
public class VacuumSignalPropagationModel {

    /** fail criteria */
    private static final int FAILCRITERIA = 100;

    /** the threshold for the iterative computations */
    private final double inThreshold;

    /** the work frame */
    private final Frame inFrame;

    /** the convergence algorithm */
    private final ConvergenceAlgorithm convAlgo;

    /**
     * Constructor of the signal propagation model.
     * <p>
     * By default, this model will use the {@link ConvergenceAlgorithm#NEWTON NEWTON} convergence
     * algorithm.
     * </p>
     *
     * @param frame
     *        the work frame : must be inertial
     * @param threshold
     *        the iterative computation algorithm convergence threshold: propagation time precision
     *        required
     * @throws IllegalArgumentException if the provided frame is not pseudo-inertial
     */
    public VacuumSignalPropagationModel(final Frame frame, final double threshold) {
        this(frame, threshold, ConvergenceAlgorithm.NEWTON);
    }

    /**
     * Constructor of the signal propagation model.
     *
     * @param frame
     *        the work frame : must be inertial and invariant over time
     * @param threshold
     *        the iterative computation algorithm convergence threshold: propagation time precision
     *        required
     * @param convAlgo
     *        the convergence algorithm
     * @throws IllegalArgumentException if the provided frame is not pseudo-inertial
     */
    public VacuumSignalPropagationModel(final Frame frame, final double threshold,
            final ConvergenceAlgorithm convAlgo) {

        // The frame must be inertial
        if (!frame.isPseudoInertial()) {
            throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.PDB_NOT_INERTIAL_FRAME);
        }

        this.inThreshold = threshold;
        this.inFrame = frame;
        this.convAlgo = convAlgo;
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
    public final VacuumSignalPropagation computeSignalPropagation(
            final PVCoordinatesProvider emitter, final PVCoordinatesProvider receiver,
            final AbsoluteDate date, final FixedDate fixedDateType) throws PatriusException {

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

        final VacuumSignalPropagation signalPropagation;

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
                newTprop = this.convAlgo.computeTprop(newTprop, receiverPV.getVelocity(),
                        propagation);

                // update of the end criteria
                criteria = MathLib.abs(tProp - newTprop);
            }

            signalPropagation = new VacuumSignalPropagation(emitterPV, receiverPV, emissionDate,
                    receptionDate, this.inFrame, FixedDate.EMISSION);
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
                newTprop = this.convAlgo.computeTprop(newTprop, emitterPV.getVelocity(),
                        propagation);

                // update of the end criteria
                criteria = MathLib.abs(tProp - newTprop);
            }

            signalPropagation = new VacuumSignalPropagation(emitterPV, receiverPV, emissionDate,
                    receptionDate, this.inFrame, FixedDate.RECEPTION);
        }
        return signalPropagation;
    }

    /**
     * Computes the tropospheric effects corrections to be applied to a given
     * {@link VacuumSignalPropagation} object.
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
     * @deprecated prefer using {@link TroposphericCorrection} with
     *             {@link AzimuthElevationCalculator#getElevation(Vector3D)} instead.
     */
    @Deprecated
    public double getSignalTropoCorrection(final TroposphericCorrection correction,
            final VacuumSignalPropagation signal, final TopocentricFrame topo)
            throws PatriusException {

        final Vector3D signalVect = signal.getVector(topo);

        // true (geometric) elevation of the signal
        final double value = signalVect.normalize().getZ();
        final double trueElevation = MathLib.asin(MathLib.min(1.0, MathLib.max(-1.0, value)));

        // correction computation
        return correction.computeSignalDelay(trueElevation) * Constants.SPEED_OF_LIGHT;
    }

    /**
     * Compute signal emission date which is the date at which the signal received by the spacecraft
     * (receiver) has been
     * emitted by the emitter depending on {@link PropagationDelayType}.
     * 
     * @param emitter emitter
     * @param orbit orbit of the spacecraft (receiver)
     * @param date date at which the spacecraft orbit is defined
     * @param epsilon absolute duration threshold used for convergence of signal propagation
     *        computation. The epsilon is
     *        a time absolute error (ex: 1E-14s, in this case, the signal distance accuracy is
     *        1E-14s x 3E8m/s = 3E-6m).
     * @param propagationDelayType propagation delay type
     * @param inertialFrame inertial frame
     * @return signal emission date
     * @throws PatriusException thrown if computation failed
     */
    public static AbsoluteDate getSignalEmissionDate(final PVCoordinatesProvider emitter,
            final PVCoordinatesProvider orbit, final AbsoluteDate date, final double epsilon,
            final PropagationDelayType propagationDelayType, final Frame inertialFrame)
            throws PatriusException {
        final AbsoluteDate res;
        if (propagationDelayType.equals(PropagationDelayType.INSTANTANEOUS)) {
            // Instantaneous signal
            res = date;
        } else {
            // Take light speed into account
            final VacuumSignalPropagationModel model = new VacuumSignalPropagationModel(
                    inertialFrame, epsilon);
            final VacuumSignalPropagation signal = model.computeSignalPropagation(emitter, orbit,
                    date, FixedDate.RECEPTION);
            res = signal.getEmissionDate();
        }
        return res;
    }

    /**
     * Compute signal reception date which is the date at which the signal emitted by the spacecraft
     * (emitter) has been
     * received by the receiver depending on {@link PropagationDelayType}.
     * 
     * @param receiver receiver
     * @param orbit orbit of the spacecraft (emitter)
     * @param date date at which the spacecraft orbit is defined
     * @param epsilon absolute duration threshold used for convergence of signal propagation
     *        computation. The epsilon is
     *        a time absolute error (ex: 1E-14s, in this case, the signal distance accuracy is
     *        1E-14s x 3E8m/s = 3E-6m).
     * @param propagationDelayType propagation delay type
     * @param inertialFrame inertial frame
     * @return signal reception date
     * @throws PatriusException thrown if computation failed
     */
    public static AbsoluteDate getSignalReceptionDate(final PVCoordinatesProvider receiver,
            final PVCoordinatesProvider orbit, final AbsoluteDate date, final double epsilon,
            final PropagationDelayType propagationDelayType, final Frame inertialFrame)
            throws PatriusException {
        final AbsoluteDate res;
        if (propagationDelayType.equals(PropagationDelayType.INSTANTANEOUS)) {
            // Instantaneous signal
            res = date;
        } else {
            // Take light speed into account
            final VacuumSignalPropagationModel model = new VacuumSignalPropagationModel(
                    inertialFrame, epsilon);
            final VacuumSignalPropagation signal = model.computeSignalPropagation(orbit, receiver,
                    date, FixedDate.EMISSION);
            res = signal.getReceptionDate();
        }
        return res;
    }

    /** the fixed date of computation */
    public enum FixedDate {
        /** Emission */
        EMISSION,
        /** Reception */
        RECEPTION;
    }

    /**
     * Convergence algorithm to compute the geometric signal propagation duration.
     * The equation to solve is f(x)=x where x is the propagation time.
     */
    public enum ConvergenceAlgorithm {

        /** Fixe point convergence algorithm. The algorithm is x_n+1 = f(x_n). */
        FIXE_POINT {
            /** {@inheritDoc} */
            @Override
            public double computeTprop(final double oldTprop, final Vector3D velEmitterOrReceptor,
                    final Vector3D propagation) {
                return propagation.getNorm() / Constants.SPEED_OF_LIGHT;
            }
        },

        /**
         * Newton convergence algorithm. The algorithm is x_n+1 = x_n - h(x_n)/h'(x_n) where
         * h(x)=f(x)-x.
         */
        NEWTON {
            /** {@inheritDoc} */
            @Override
            public double computeTprop(final double oldTprop, final Vector3D velEmitterOrReceptor,
                    final Vector3D propagation) {
                final double propNorm = propagation.getNorm();
                final double h = (propNorm / Constants.SPEED_OF_LIGHT) - oldTprop;
                final double hprime =
                    (velEmitterOrReceptor.dotProduct(propagation) / (propNorm * Constants.SPEED_OF_LIGHT)) - 1;
                return oldTprop - (h / hprime);
            }
        };

        /**
         * Recompute the signal propagation duration with the convergence algorithm.
         * 
         * @param oldTprop
         *        the old signal propagation duration (n-1)
         * @param velEmitterOrReceptor
         *        the emitter (resp. receptor) velocity at the emission (resp. reception) date
         *        according to the FixedDate value
         * @param propagation
         *        the propagation vector in the reference frame
         * @return the updated signal propagation duration
         */
        public abstract double computeTprop(double oldTprop, Vector3D velEmitterOrReceptor,
                final Vector3D propagation);
    }
}
