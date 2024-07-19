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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation;

import java.io.Serializable;

import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.signalpropagation.VacuumSignalPropagationModel.FixedDate;
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
public class VacuumSignalPropagation implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -2660829788878498506L;

    /** Reference frame. */
    private final Frame frame;

    /** Fixed date : emission or reception. */
    private final FixedDate inFixedDate;

    /** Emitter PV in the reference frame. */
    private final PVCoordinates emitterPV;

    /** Receiver PV in the reference frame. */
    private final PVCoordinates receiverPV;

    /**
     * Propagation vector (stores {@code receiverPV.getPosition().subtract(emitterPV.getPosition())} in cache).
     */
    private final Vector3D propagation;

    /** Emission date. */
    private final AbsoluteDate emissionDate;

    /** Reception date. */
    private final AbsoluteDate receptionDate;

    /** Date to consider for computation (saved in cache for optimization). */
    private final AbsoluteDate date;

    /**
     * Signal propagation constructor.
     *
     * @param emitterPV
     *        the emitter PV in the reference frame
     * @param receiverPV
     *        the receiver PV in the reference frame
     * @param inEmissionDate
     *        the emission date
     * @param inReceptionDate
     *        the reception date
     * @param refFrame
     *        the reference frame for the vector
     * @param fixedDate
     *        the fixed date for computations (reception or emission)
     * @throws IllegalArgumentException if
     *         if the emitter and receiver PV are described at the same position
     */
    public VacuumSignalPropagation(final PVCoordinates emitterPV, final PVCoordinates receiverPV,
                                   final AbsoluteDate inEmissionDate, final AbsoluteDate inReceptionDate,
                                   final Frame refFrame, final FixedDate fixedDate) {

        // Initializations
        this.propagation = receiverPV.getPosition().subtract(emitterPV.getPosition());
        // Check the propagation vector
        if (this.propagation.getNorm() < Precision.DOUBLE_COMPARISON_EPSILON) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.NULL_VECTOR);
        }

        this.emitterPV = emitterPV;
        this.receiverPV = receiverPV;
        this.emissionDate = inEmissionDate;
        this.receptionDate = inReceptionDate;
        this.frame = refFrame;
        this.inFixedDate = fixedDate;

        if (this.inFixedDate == FixedDate.EMISSION) {
            this.date = this.getEmissionDate();
        } else if (this.inFixedDate == FixedDate.RECEPTION) {
            this.date = this.getReceptionDate();
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
    public final AbsoluteDate getEmissionDate() {
        return this.emissionDate;
    }

    /**
     * Getter for the reception date.
     * 
     * @return the reception date
     */
    public final AbsoluteDate getReceptionDate() {
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
     * Getter for the signal propagation duration (delay in seconds between the {@link #emissionDate emission date} and
     * the {@link #receptionDate reception date}).
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
            dPropdT = this.receiverPV.getVelocity().scalarMultiply(1 + getdTpropdT())
                .subtract(this.emitterPV.getVelocity());
        } else {
            dPropdT = this.receiverPV.getVelocity().subtract(
                this.emitterPV.getVelocity().scalarMultiply(1 - getdTpropdT()));
        }
        return dPropdT;
    }

    /**
     * Getter for the propagation position/velocity vectors in the reference frame.
     * <p>
     * This method is a combination of the methods {@link VacuumSignalPropagation#getVector() getVector()} and
     * {@link VacuumSignalPropagation#getdPropdT() getdPropdT()}.
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
        // Compute the dTpropdPem vector in the expression frame
        final Transform trans = this.frame.getTransformTo(expressionFrame, this.date);
        final Vector3D dTpropdPem = trans.transformVector(getdTpropdPem());

        // Compute the appropriate velocity in the expression frame
        final Vector3D vel;
        if (this.inFixedDate == FixedDate.EMISSION) {
            vel = trans.transformPVCoordinates(this.receiverPV).getVelocity();
        } else {
            vel = trans.transformPVCoordinates(this.emitterPV).getVelocity();
        }

        // outInRefFrame = -Id + vel * dTpropdPem.transpose()
        final double[][] outInRefFrame = new double[][] {
            { (vel.getX() * dTpropdPem.getX()) - 1., vel.getX() * dTpropdPem.getY(),
                vel.getX() * dTpropdPem.getZ() },
            { vel.getY() * dTpropdPem.getX(), (vel.getY() * dTpropdPem.getY()) - 1.,
                vel.getY() * dTpropdPem.getZ() },
            { vel.getZ() * dTpropdPem.getX(), vel.getZ() * dTpropdPem.getY(),
                (vel.getZ() * dTpropdPem.getZ()) - 1. } };
        // Return the dPropdPem matrix
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
        // Compute the dTpropdPrec vector in the expression frame
        final Transform trans = this.frame.getTransformTo(expressionFrame, this.date);
        final Vector3D dTpropdPrec = trans.transformVector(getdTpropdPrec());

        // Compute the appropriate velocity in the expression frame
        final Vector3D vel;
        if (this.inFixedDate == FixedDate.EMISSION) {
            vel = trans.transformPVCoordinates(this.receiverPV).getVelocity();
        } else {
            vel = trans.transformPVCoordinates(this.emitterPV).getVelocity();
        }
        // outInRefFrame = +Id + vel * dTpropdPrec.transpose()
        final double[][] outInRefFrame = new double[][] {
            { (vel.getX() * dTpropdPrec.getX()) + 1., vel.getX() * dTpropdPrec.getY(),
                vel.getX() * dTpropdPrec.getZ() },
            { vel.getY() * dTpropdPrec.getX(), (vel.getY() * dTpropdPrec.getY()) + 1.,
                vel.getY() * dTpropdPrec.getZ() },
            { vel.getZ() * dTpropdPrec.getX(), vel.getZ() * dTpropdPrec.getY(),
                (vel.getZ() * dTpropdPrec.getZ()) + 1. } };
        // Return the dPropdPrec matrix
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
        return this.propagation.dotProduct(this.receiverPV.getVelocity().subtract(
            this.emitterPV.getVelocity()))
                * getK();
    }

    /**
     * Computes the Shapiro time dilation due to the gravitational attraction of the provided body.
     * 
     * @param body
     *        The body responsible for the time dilation
     * @return
     *         The Shapiro time dilation
     * @throws PatriusException
     *         if position cannot be computed in given frame
     */
    public double getShapiroTimeCorrection(final CelestialBody body) throws PatriusException {
        // Extract the information from the body
        final Vector3D bodyPosition = body.getPVCoordinates(this.date, this.frame).getPosition();
        final Vector3D emitterPosition = this.emitterPV.getPosition();
        final Vector3D receiverPosition = this.receiverPV.getPosition();
        // Compute the Shapiro time dilation
        return getShapiroTimeCorrection(body.getGM(), bodyPosition.subtract(emitterPosition)
            .getNorm(), bodyPosition.subtract(receiverPosition).getNorm(), emitterPosition
            .subtract(receiverPosition).getNorm());
    }

    /**
     * Computes the Shapiro time dilation due to the gravitational attraction of the body present at
     * the center of the {@link VacuumSignalPropagation#getFrame()}.<br>
     * 
     * Optimized version of the {@link VacuumSignalPropagation#getShapiroTimeCorrection(CelestialBody)} method for the
     * frame
     * attractive body.
     * 
     * @param mu
     *        The gravitational constant of the body.
     * @return
     *         The Shapiro time dilation
     */
    public double getShapiroTimeCorrection(final double mu) {
        final Vector3D emitterPosition = this.emitterPV.getPosition();
        final Vector3D receiverPosition = this.receiverPV.getPosition();
        // Compute the Shapiro time dilation
        return getShapiroTimeCorrection(mu, emitterPosition.getNorm(), receiverPosition.getNorm(),
            emitterPosition.subtract(receiverPosition).getNorm());
    }

    /**
     * Internal method to compute the Shapiro time correction
     * 
     * @param mu
     *        The mass of the body responsible for the time dilation
     * @param emitterToBodyDist
     *        The distance between the emitter and the body
     * @param receiverToBodyDist
     *        The distance between the receiver and the body
     * @param emitterToReceiverDist
     *        The distance between the emitter and receiver
     * @return
     *         The Shapiro time correction
     * @throws PatriusException
     */
    private static double getShapiroTimeCorrection(final double mu, final double emitterToBodyDist,
                                                   final double receiverToBodyDist,
                                                   final double emitterToReceiverDist) {
        final double distRatio = (emitterToBodyDist + receiverToBodyDist + emitterToReceiverDist)
                / (emitterToBodyDist + receiverToBodyDist - emitterToReceiverDist);
        return 2 * mu * MathLib.log(distRatio) / MathLib.pow(Constants.SPEED_OF_LIGHT, 3);
    }

    /**
     * Compute the factor "k" according to the fixed date selection.
     *
     * @return the "k" factor
     */
    private double getK() {
        final double k;
        if (this.inFixedDate == FixedDate.EMISSION) {
            k = 1 / ((Constants.SPEED_OF_LIGHT * this.propagation.getNorm()) - this.receiverPV
                .getVelocity().dotProduct(this.propagation));
        } else {
            k = 1 / ((Constants.SPEED_OF_LIGHT * this.propagation.getNorm()) - this.emitterPV
                .getVelocity().dotProduct(this.propagation));
        }

        return k;
    }
}
