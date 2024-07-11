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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2465:27/05/2020:Refactoring calculs de ralliements
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.slew;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.TabulatedSlew;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 *
 **/
/**
 * <p>
 * Class for two spin bias slew computation. Computation of slew returns a {@link TabulatedSlew}.
 * </p>
 * <p>
 * The two spin bias slew computes the attitude of the satellite from initial and final attitude laws, the parameters of
 * the two angular velocity fields, plus the time step as well as the stabilization margin.<br>
 * The angular velocity depends on the value of the slew angle.<br>
 * Like all the other attitude legs, its interval of validity has closed endpoints.
 * </p>
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.5
 */
public class TwoSpinBiasSlewComputer implements Serializable {

    /** Serial UID. */
    private static final long serialVersionUID = 5892440847034557194L;

    /** Nature. */
    private static final String DEFAULT_NATURE = "TWO_SPIN_BIAS_SLEW";

    /** Reference frame. */
    private final Frame refFrame;

    /** SCAO time step [s]. */
    private final double dtSCAO;

    /** The maximum angle of validity of the model [rad]. */
    private final double thetaMax;

    /** Time constant of the filter [s]. */
    private final double tau;

    /** Initial orientation error threshold [rad]. */
    private final double seuilQcEntree;

    /** The angular velocity for big angular distances (used when seuil_qc_sortie < theta < sbv_haut) [rad/s]. */
    private final double bvHaut;

    /** The switching angular distance [rad]. */
    private final double sbvHaut;

    /** Final orientation error threshold [rad]. */
    private final double seuilQcSortie;

    /** The angular velocity for small angular distances (used when sbv_haut < theta < thetaMax) [rad/s]. */
    private final double bvBas;

    /** Stabilisation margin [s]. */
    private final double tStabSol;

    /** Nature. */
    private final String nature;

    /**
     * Constructor.
     * 
     * @param dtSCAOIn
     *        (<i>dtScao</i>)
     *        the time step [s].
     * @param thetaMaxIn
     *        (<i>angleMax</i>)
     *        the limit of validity of the maneuver amplitude [rad].
     * @param tauIn
     *        (<i>&tau;</i>)
     *        the time constant of the filter [s].
     * @param epsInRall
     *        (<i>seuilEntree</i>)
     *        initial orientation error threshold [rad].
     * @param omegaHigh
     *        (<i>biaisVitesse</i>)
     *        the high angular velocity value [rad/s].
     * @param thetaSwitch
     *        (<i>seuil</i>)
     *        the threshold for the low/high angular velocity switch [rad].
     * @param epsOutRall
     *        (<i>seuilSortie</i>)
     *        final orientation error threshold [rad].
     * @param omegaLow
     *        (<i>biaisVitesseBas</i>)
     *        the low angular velocity value [rad/s].
     * @param tStab
     *        (<i>margeStabilisation</i>)
     *        the stabilisation margin [s].
     * @throws PatriusException
     *         when the sampling step in not valid
     */
    public TwoSpinBiasSlewComputer(final double dtSCAOIn, final double thetaMaxIn,
        final double tauIn, final double epsInRall, final double omegaHigh,
        final double thetaSwitch, final double epsOutRall, final double omegaLow,
        final double tStab) throws PatriusException {
        this(dtSCAOIn, thetaMaxIn, tauIn, epsInRall, omegaHigh,
            thetaSwitch, epsOutRall, omegaLow, tStab, DEFAULT_NATURE);
    }

    /**
     * Constructor.
     * <p>
     * The two spin bias slew computes the attitude of the satellite from initial and final attitude laws, the
     * parameters of the two angular velocity fields, plus the time step as well as the stabilization margin.<br>
     * The angular velocity depends on the value of the slew angle.<br>
     * </p>
     * 
     * @param dtSCAOIn (<i>dtScao</i>) the time step [s].
     * @param thetaMaxIn (<i>angleMax</i>) the limit of validity of the maneuver amplitude [rad].
     * @param tauIn (<i>&tau;</i>) the time constant of the filter [s].
     * @param epsInRall (<i>seuilEntree</i>) initial orientation error threshold [rad].
     * @param omegaHigh (<i>biaisVitesse</i>) the high angular velocity value [rad/s].
     * @param thetaSwitch (<i>seuil</i>) the threshold for the low/high angular velocity switch
     *        [rad].
     * @param epsOutRall (<i>seuilSortie</i>) final orientation error threshold [rad].
     * @param omegaLow (<i>biaisVitesseBas</i>) the low angular velocity value [rad/s].
     * @param tStab (<i>margeStabilisation</i>) the stabilisation margin [s].
     * @param natureIn leg nature
     * @throws PatriusException when the sampling step in not valid
     */
    public TwoSpinBiasSlewComputer(final double dtSCAOIn, final double thetaMaxIn,
        final double tauIn, final double epsInRall, final double omegaHigh,
        final double thetaSwitch, final double epsOutRall, final double omegaLow,
        final double tStab, final String natureIn) throws PatriusException {

        this.nature = natureIn;
        this.refFrame = FramesFactory.getEME2000();
        this.dtSCAO = dtSCAOIn;
        this.thetaMax = thetaMaxIn;
        this.tau = tauIn;
        this.seuilQcEntree = epsInRall;
        this.bvHaut = omegaHigh;
        this.sbvHaut = thetaSwitch;
        this.seuilQcSortie = epsOutRall;
        this.bvBas = omegaLow;
        this.tStabSol = tStab;
    }

    /**
     * Compute the slew.
     * @param pvProv satellite PV coordinates through time
     * @param initialLaw initial attitude law (before the slew)
     * @param initialDate slew start date (may be null if slew defined with its end date)
     * @param finalLaw final attitude law (after the slew)
     * @param finalDate slew end date (currently unused)
     * @return built slew
     * @throws PatriusException thrown if computation failed
     */
    public TabulatedSlew compute(final PVCoordinatesProvider pvProv, final AttitudeProvider initialLaw,
            final AbsoluteDate initialDate,
            final AttitudeProvider finalLaw,
            final AbsoluteDate finalDate) throws PatriusException {

        // Ephemeris set initialization:
        final List<Attitude> slewEphem = new ArrayList<Attitude>();

        // Target attitude at t=0:
        final Attitude initTargetAtt = finalLaw.getAttitude(pvProv, initialDate, this.refFrame);
        final Rotation initTargetRot = initTargetAtt.getRotation();

        // Initial attitude:
        final Attitude initAtt = initialLaw.getAttitude(pvProv, initialDate, this.refFrame);
        // This first attitude is added to the ephemeris set:
        slewEphem.add(initAtt);
        final Rotation initRot = initAtt.getRotation();

        // Computation of the rotation between the initial attitude and the target attitude at t=0:
        final Rotation drotInit = initRot.applyInverseTo(initTargetRot);
        final double initTheta = drotInit.getAngle();

        // Maximum amplitude control:
        if (initTheta >= this.thetaMax) {
            throw new PatriusException(PatriusMessages.MANEUVER_AMPLITUDE_EXCEEDS_FIXED_MAXIMUM_VALUE);
        }

        final Vector3D w0 = initAtt.getSpin();
        final Vector3D initThetaAxis = drotInit.getAxis();

        // Temporary variables initialization:
        // Duration from the beginning of the slew:
        double t = 0.0;
        // true if the spin bias mode is on, false otherwise:
        boolean flagBV = false;
        // Spin bias value:
        double bv = 0.0;
        // Spin bias reset date (from the beginning of the slew):
        double tbv = 0.0;
        // Angular velocity at the date of beginning of the spin bias:
        Vector3D w0BV = w0;
        // Next attitude = initial attitude at t=0:
        Attitude nextAtt = initAtt;
        // Rotation axis associated to delta_q:
        Vector3D u = initThetaAxis;
        Vector3D uPrec = u;

        // Slew duration preliminary computation:
        final double duration = this.computeDuration(pvProv, initialLaw, initialDate, finalLaw, null);

        // While loop:
        while (t < duration - Precision.DOUBLE_COMPARISON_EPSILON) {

            // Current attitude/spin computation at t:
            final Attitude curAtt = nextAtt;
            final Rotation qt = curAtt.getRotation();
            final Vector3D omegat = curAtt.getSpin();

            // Target attitude/spin computation at t:
            final Attitude attCons = finalLaw.getAttitude(pvProv, initialDate.shiftedBy(t), this.refFrame);
            final Rotation qCons = attCons.getRotation();
            final Vector3D wCons = attCons.getSpin();

            // Computation of the rotation between the current attitude and the target attitude at t:
            final Rotation deltaQ = qt.applyInverseTo(qCons);
            final double theta = deltaQ.getAngle();

            if (MathLib.abs(theta) < Precision.DOUBLE_COMPARISON_EPSILON) {
                u = Vector3D.PLUS_I;
            } else {
                u = deltaQ.getAxis();
            }

            // Verification of the continuity of the rotation direction:
            if (uPrec.dotProduct(u) < 0) {
                u = u.negate();
            }
            uPrec = u;

            // Attitude computation at t+dtScao:
            // The rotation error is evaluated in order to verify if "Spin biais" conditions apply:
            if (((theta >= this.seuilQcEntree - Precision.DOUBLE_COMPARISON_EPSILON) && (!flagBV))
                || ((theta >= this.seuilQcSortie - Precision.DOUBLE_COMPARISON_EPSILON) && (flagBV))) {
                // "Spin biais" conditions apply:
                flagBV = true;

                // The spin is chosen:
                if (theta > this.sbvHaut + Precision.DOUBLE_COMPARISON_EPSILON) {
                    // Big angles spin:
                    bv = this.bvHaut;
                } else if (MathLib.abs(bv - this.bvHaut) < Precision.DOUBLE_COMPARISON_EPSILON) {
                    // Switch to small angles spin:
                    bv = this.bvBas;
                    tbv = t;
                    w0BV = omegat;
                } else {
                    // Small angles spin is mantained:
                    bv = this.bvBas;
                }

                nextAtt = this.computeNextStepAttitude(bv, u, wCons, w0BV, t, tbv, qt, initialDate);

            } else {
                // "Spin biais" conditions do not apply: the target attitude is attended.
                nextAtt = finalLaw.getAttitude(pvProv, initialDate.shiftedBy(t + this.dtSCAO), this.refFrame);
            }

            // The current date is increased:
            t += this.dtSCAO;

            slewEphem.add(nextAtt);
        }
        // End of while loop

        // Slew computation finalisation:
        return new TabulatedSlew(slewEphem, 2, nature);
    }

    /**
     * This method computes the attitude at the next date (t+dtScao):<br>
     * spin = bv.u + wCons + (w0bv - bv.u - wCons).exp(-(t-tbv)/tau);
     * the quaternion is computed via the spin integration at the same date.
     * 
     * @param bv
     *        the current spin bias value [rad/s].
     * @param u
     *        the rotation axis associated to delta_q.
     * @param wCons
     *        the spin of the target law at t [rad/s].
     * @param w0bv
     *        the spin at t=tbv [rad/s].
     * @param t
     *        the current lapsed time from the beginning of the slew [s].
     * @param tbv
     *        the time of the switch between spin bias [s].
     * @param qt
     *        the quaternion at t.
     * @param initDate
     *        initial date
     * @return the attitude at the date t+dtScao
     */
    private Attitude computeNextStepAttitude(final double bv, final Vector3D u,
                                             final Vector3D wCons, final Vector3D w0bv, final double t,
                                             final double tbv, final Rotation qt, final AbsoluteDate initDate) {

        final Vector3D term1 = u.scalarMultiply(bv).add(wCons);

        final double exp = MathLib.exp(MathLib.divide(-(t - tbv), this.tau));

        final Vector3D term2 = w0bv.subtract(bv, u).subtract(wCons).scalarMultiply(exp);

        final Vector3D wNext = term1.add(term2);

        // Quaternion computation at t + dtSCAO via the spin integration at the same date:
        Rotation dq = Rotation.IDENTITY;
        if (wNext.getNorm() != 0.) {
            dq = new Rotation(wNext, this.dtSCAO * wNext.getNorm());
        }
        final Rotation qNext = qt.applyTo(dq);

        // Attitude at t + dtSCAO:
        return new Attitude(initDate.shiftedBy(t + this.dtSCAO), this.refFrame,
            qNext, wNext);
    }

    /**
     * Computes the actual slew duration.
     * 
     * @param pvProv
     *        the PV coordinates provider
     * @param initialLaw initial attitude law (before the slew)
     * @param initialDate slew start date (may be null if slew defined with its end date)
     * @param finalLaw final attitude law (after the slew)
     * @param finalDate slew end date (currently unused)
     * @return the actual slew duration
     * @throws PatriusException
     *         when an error occurs during attitudes computation
     */
    public double computeDuration(final PVCoordinatesProvider pvProv,
            final AttitudeProvider initialLaw,
            final AbsoluteDate initialDate,
            final AttitudeProvider finalLaw,
            final AbsoluteDate finalDate) throws PatriusException {

        // Target attitude at t=0:
        final Attitude initTargetAtt = finalLaw.getAttitude(pvProv, initialDate, this.refFrame);

        // Actual attitude at t=0
        final Attitude initAtt = initialLaw.getAttitude(pvProv, initialDate, this.refFrame);

        // The rotation between the initial and target attitudes is computed:
        final double theta = Rotation.distance(
            initTargetAtt.getRotation(),
            initAtt.getRotation());

        // Maximum amplitude control:
        if (theta >= this.thetaMax) {
            throw new PatriusException(PatriusMessages.MANEUVER_AMPLITUDE_EXCEEDS_FIXED_MAXIMUM_VALUE);
        }

        // The limit angle between the two angular velocity profiles is computed:
        final double thetaLimGAPA = MathLib.divide(this.bvHaut * this.sbvHaut, this.bvBas);

        // Slew actual duration computation:
        final double duration;
        if (theta > thetaLimGAPA) {
            duration = 6.0 * this.tau + MathLib.divide(theta, this.bvHaut) + this.tStabSol;
        } else {
            duration = 6.0 * this.tau + MathLib.divide(this.sbvHaut, this.bvBas) + this.tStabSol;
        }
        return (MathLib.floor(duration / this.dtSCAO) + 1) * this.dtSCAO;
    }

    /**
     * Estimate the maximum duration of the slew, before computing it.
     * 
     * @return the estimated maximum duration of the slew.
     */
    public double computeMaxDuration() {

        // The limit angle between the two angular velocity profiles is computed:
        final double thetaLimGAPA = MathLib.divide(this.bvHaut * this.sbvHaut, this.bvBas);

        // Maximum duration computation:
        double duration = 0.0;
        if (FastMath.PI > thetaLimGAPA) {
            // "big angles" profile:
            duration = 6.0 * this.tau + MathLib.divide(FastMath.PI, this.bvHaut) + this.tStabSol;
        } else {
            // "small angles" profile:
            duration = 6.0 * this.tau + MathLib.divide(this.sbvHaut, this.bvBas) + this.tStabSol;
        }
        return (MathLib.floor(duration / this.dtSCAO) + 1) * this.dtSCAO;
    }
}
