/**
 *
 * Copyright 2011-2022 CNES
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
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.signalpropagation.SignalPropagationModel.FixedDate;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class describes the propagation of a signal in space
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
public class SignalPropagation {

    /** Propagation vector. */
    private final Vector3D propagation;

    /** Emission date. */
    private final AbsoluteDate emissionDate;

    /** Reception date. */
    private final AbsoluteDate receptionDate;

    /** Reference frame. */
    private final Frame frame;

    /** Fixed date : emission or reception. */
    private final FixedDate inFixedDate;

    /** Emitter velocity in the reference frame. */
    private final Vector3D velEmitter;

    /** Receiver velocity in the reference frame. */
    private final Vector3D velReceiver;

    /** Date to consider for computation (saved in cache for optimization). */
    private final AbsoluteDate date;

    /**
     * Old constructor with computation of one of the dates
     *
     * @param propagation
     *        the propagation vector
     * @param inEmissionDate
     *        the emission date
     * @param inReceptionDate
     *        the reception date
     * @param refFrame
     *        the reference frame for the vector
     * @param fixedDate
     *        the fixed date for computations (reception or emission)
     * @throws IllegalArgumentException if
     *         {@code propagation.getNorm() < Precision.DOUBLE_COMPARISON_EPSILON}
     */
    public SignalPropagation(final Vector3D propagation, final AbsoluteDate inEmissionDate,
            final AbsoluteDate inReceptionDate, final Frame refFrame, final FixedDate fixedDate) {
        this(propagation, inEmissionDate, inReceptionDate, refFrame, fixedDate, Vector3D.ZERO,
                Vector3D.ZERO);
    }

    /**
     * New extended constructor with computation of one of the dates (supports derivatives
     * computation).
     *
     * @param propagation
     *        the propagation vector
     * @param inEmissionDate
     *        the emission date
     * @param inReceptionDate
     *        the reception date
     * @param refFrame
     *        the reference frame for the vector
     * @param fixedDate
     *        the fixed date for computations (reception or emission)
     * @param velEmitter
     *        Emitter velocity in the reference frame
     * @param velReceiver
     *        Receiver velocity in the reference frame
     * @throws IllegalArgumentException if
     *         {@code propagation.getNorm() < Precision.DOUBLE_COMPARISON_EPSILON}
     */
    public SignalPropagation(final Vector3D propagation, final AbsoluteDate inEmissionDate,
            final AbsoluteDate inReceptionDate, final Frame refFrame, final FixedDate fixedDate,
            final Vector3D velEmitter, final Vector3D velReceiver) {

        // Check the propagation vector
        if (propagation.getNorm() < Precision.DOUBLE_COMPARISON_EPSILON) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.NULL_VECTOR);
        }

        // Initializations
        this.propagation = propagation;
        this.emissionDate = inEmissionDate;
        this.receptionDate = inReceptionDate;
        this.frame = refFrame;
        this.inFixedDate = fixedDate;
        this.velEmitter = velEmitter;
        this.velReceiver = velReceiver;

        if (this.inFixedDate == FixedDate.EMISSION) {
            this.date = this.getStartDate();
        } else if (this.inFixedDate == FixedDate.RECEPTION) {
            this.date = this.getEndDate();
        } else {
            // Should never happen, kept for safety
            throw new EnumConstantNotPresentException(FixedDate.class, this.inFixedDate.toString());
        }
    }

    /**
     * Getter for the emission date.
     * 
     * @return the emission date
     */
    public final AbsoluteDate getStartDate() {
        return this.emissionDate;
    }

    /**
     * Getter for the reception date.
     * 
     * @return the reception date
     */
    public final AbsoluteDate getEndDate() {
        return this.receptionDate;
    }

    /**
     * Getter for the reference frame.
     * 
     * @return the reference frame
     */
    public final Frame getFrame() {
        return this.frame;
    }

    /**
     * Getter for the signal propagation duration (delay in seconds between the
     * {@link #emissionDate emission date} and the {@link #receptionDate reception date}).
     *
     * @return the signal propagation duration
     */
    public final double getSignalPropagationDuration() {
        return this.receptionDate.durationFrom(this.emissionDate);
    }

    /**
     * Getter for the propagation vector in the given frame.
     *
     * @param expressionFrame frame in which vector should be expressed
     * @return the propagation vector in the given frame
     * @throws PatriusException
     *         if frame transformation problem occurs
     */
    public final Vector3D getVector(final Frame expressionFrame) throws PatriusException {
        final Transform trans = this.frame.getTransformTo(expressionFrame, this.date);
        return trans.transformVector(this.propagation);
    }

    /**
     * Getter for the propagation vector in the reference frame.
     *
     * @return the propagation vector
     */
    public final Vector3D getVector() {
        return this.propagation;
    }

    /**
     * Getter for the propagation velocity vector (= propagation vector derivative wrt time) in the
     * reference frame.
     *
     * @return the propagation velocity vector
     */
    public Vector3D getdPropdT() {
        final Vector3D dPropdT;
        if (this.inFixedDate == FixedDate.EMISSION) {
            dPropdT = this.velReceiver.scalarMultiply(1 + getdTpropdT()).subtract(this.velEmitter);
        } else {
            dPropdT = this.velReceiver.subtract(this.velEmitter.scalarMultiply(1 - getdTpropdT()));
        }
        return dPropdT;
    }

    /**
     * Getter for the propagation position/velocity vectors in the reference frame.
     * <p>
     * This method is a combination of the methods {@link SignalPropagation#getVector() getVector()}
     * and {@link SignalPropagation#getdPropdT() getdPropdT()}.
     * </p>
     *
     * @return the propagation position/velocity vectors
     */
    public PVCoordinates getPVPropagation() {
        return new PVCoordinates(this.getVector(), this.getdPropdT());
    }

    /**
     * Getter for the propagation velocity vector (= propagation vector derivative wrt time) in the
     * given frame.
     *
     * @param expressionFrame frame in which vector should be expressed
     * @return the propagation velocity vector
     * @throws PatriusException
     *         if frame transformation problem occurs
     */
    public Vector3D getdPropdT(final Frame expressionFrame) throws PatriusException {

        final Transform trans = this.frame.getTransformTo(expressionFrame, this.date);
        return trans.transformPVCoordinates(getPVPropagation()).getVelocity();
    }

    /**
     * Getter for the propagation position vector derivatives wrt the emitter position express in
     * the given frame.
     *
     * @param expressionFrame frame in which the derivatives should be expressed
     * @return the propagation position vector derivatives wrt the emitter position
     * @throws IllegalArgumentException if the provided frame is not pseudo-inertial
     * @throws PatriusException if some frame specific error occurs
     */
    public RealMatrix getdPropdPem(final Frame expressionFrame) throws PatriusException {
        // The frame must be inertial
        if (!expressionFrame.isPseudoInertial()) {
            throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.PDB_NOT_INERTIAL_FRAME);
            // Implementation note: this class does not contain the necessary info to convert the
            // velocity in non inertial frame
        }

        final Transform trans = this.frame.getTransformTo(expressionFrame, this.date);
        final Vector3D dTpropdPem = trans.transformVector(getdTpropdPem());
        final Vector3D vel;
        if (this.inFixedDate == FixedDate.EMISSION) {
            vel = trans.transformVector(this.velReceiver);
        } else {
            vel = trans.transformVector(this.velEmitter);
        }
        // outInRefFrame = -Id + vel * dTpropdPem.transpose()
        final double[][] outInRefFrame = new double[][] {
                { (vel.getX() * dTpropdPem.getX()) - 1., vel.getX() * dTpropdPem.getY(), vel.getX() * dTpropdPem.getZ()
                },
                { vel.getY() * dTpropdPem.getX(), (vel.getY() * dTpropdPem.getY()) - 1., vel.getY() * dTpropdPem.getZ()
                },
                { vel.getZ() * dTpropdPem.getX(), vel.getZ() * dTpropdPem.getY(), 
                (vel.getZ() * dTpropdPem.getZ()) - 1. } };
        return new Array2DRowRealMatrix(outInRefFrame, false);
    }

    /**
     * Getter for the propagation position vector derivatives wrt the receiver position express in
     * the given frame.
     *
     * @param expressionFrame frame in which the derivatives should be expressed
     * @return the propagation position vector derivatives wrt the receiver position
     * @throws IllegalArgumentException if the provided frame is not pseudo-inertial
     * @throws PatriusException if some frame specific error occurs
     */
    public RealMatrix getdPropdPrec(final Frame expressionFrame) throws PatriusException {
        // The frame must be inertial
        if (!expressionFrame.isPseudoInertial()) {
            throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.PDB_NOT_INERTIAL_FRAME);
            // Implementation note: this class does not contain the necessary info to convert the
            // velocity in non inertial frame
        }

        final Transform trans = this.frame.getTransformTo(expressionFrame, this.date);
        final Vector3D dTpropdPrec = trans.transformVector(getdTpropdPrec());
        final Vector3D vel;
        if (this.inFixedDate == FixedDate.EMISSION) {
            vel = trans.transformVector(this.velReceiver);
        } else {
            vel = trans.transformVector(this.velEmitter);
        }
        // outInRefFrame = +Id + vel * dTpropdPrec.transpose()
        final double[][] outInRefFrame = new double[][] {
                { (vel.getX() * dTpropdPrec.getX()) + 1., vel.getX() * dTpropdPrec.getY(),
                        vel.getX() * dTpropdPrec.getZ() },
                { vel.getY() * dTpropdPrec.getX(), (vel.getY() * dTpropdPrec.getY()) + 1.,
                        vel.getY() * dTpropdPrec.getZ() },
                { vel.getZ() * dTpropdPrec.getX(), vel.getZ() * dTpropdPrec.getY(),
                    (vel.getZ() * dTpropdPrec.getZ()) + 1. } };
        return new Array2DRowRealMatrix(outInRefFrame, false);
    }

    /**
     * @return the inFixedDate
     */
    public FixedDate getFixedDateType() {
        return this.inFixedDate;
    }

    /**
     * Getter for the signal propagation partial derivatives vector wrt the receiver position in the
     * reference frame at the reception date.
     *
     * @return the signal propagation partial derivatives vector
     */
    public Vector3D getdTpropdPrec() {
        return this.propagation.scalarMultiply(getK());
    }

    /**
     * Getter for the signal propagation partial derivatives vector wrt the receiver position in the
     * specified frame at the reception date.
     *
     * @param expressionFrame
     *        the signal propagation partial derivatives vector frame expression
     * @return the signal propagation partial derivatives vector
     * @throws PatriusException if some frame specific error occurs
     */
    public Vector3D getdTpropdPrec(final Frame expressionFrame) throws PatriusException {
        final Transform trans = this.frame.getTransformTo(expressionFrame, this.receptionDate);
        return trans.transformVector(getdTpropdPrec());
    }

    /**
     * Getter for the signal propagation partial derivatives vector wrt the emitter position in the
     * reference frame at the emitting date.
     * <p>
     * <i>Note: dTpropdPem = -dTpropdPrec</i>
     * </p>
     *
     * @return the signal propagation partial derivatives vector
     */
    public Vector3D getdTpropdPem() {
        return this.getdTpropdPrec().negate();
    }

    /**
     * Getter for the signal propagation partial derivatives vector wrt the emitter position in the
     * specified frame at the emitting date.
     * <p>
     * <i>Note: dTpropdPem = -dTpropdPrec</i>
     * </p>
     *
     * @param expressionFrame
     *        the signal propagation partial derivatives vector frame expression
     * @return the signal propagation partial derivatives vector
     * @throws PatriusException if some frame specific error occurs
     */
    public Vector3D getdTpropdPem(final Frame expressionFrame) throws PatriusException {
        return this.getdTpropdPrec(expressionFrame).negate();
    }

    /**
     * Getter for the signal propagation partial derivatives wrt time.
     *
     * @return the signal propagation partial derivatives
     */
    public double getdTpropdT() {
        return this.propagation.dotProduct(this.velReceiver.subtract(this.velEmitter)) * getK();
    }

    /**
     * Compute the factor "k" according to the fixed date selection.
     *
     * @return the "k" factor
     */
    private double getK() {
        final double k;
        if (this.inFixedDate == FixedDate.EMISSION) {
            k = 1 / ((Constants.SPEED_OF_LIGHT * this.propagation.getNorm()) - this.velReceiver
                    .dotProduct(this.propagation));
        } else {
            k = 1 / ((Constants.SPEED_OF_LIGHT * this.propagation.getNorm()) - this.velEmitter
                    .dotProduct(this.propagation));
        }

        return k;
    }
}
