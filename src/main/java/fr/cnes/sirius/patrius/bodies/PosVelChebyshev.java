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
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:FA:FA-3257:22/05/2023:[PATRIUS] Suite 3182
 * VERSION:4.11:FA:FA-3314:22/05/2023:[PATRIUS] Anomalie evaluation ForceModel SpacecraftState en ITRF
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
/*
 *
 */
package fr.cnes.sirius.patrius.bodies;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeStamped;

/**
 * Position-Velocity-Acceleration model based on Chebyshev polynomials.
 * <p>
 * This class represent the most basic element of the piecewise ephemerides for solar system bodies like JPL DE 405
 * ephemerides.
 * </p>
 *
 * @see JPLCelestialBodyLoader
 * @author Luc Maisonobe
 */
public class PosVelChebyshev implements TimeStamped, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -2220448511466595393L;

    /** Is-in-range threshold. */
    private static final double THRESHOLD = 0.001;

    /** Start of the validity range of the instance. */
    private final AbsoluteDate start;

    /** Duration of validity range of the instance. */
    private final double duration;

    /** Chebyshev polynomials coefficients for the X component. */
    private final double[] xCoeffs;

    /** Chebyshev polynomials coefficients for the Y component. */
    private final double[] yCoeffs;

    /** Chebyshev polynomials coefficients for the Z component. */
    private final double[] zCoeffs;

    /**
     * Simple constructor.
     *
     * @param startIn
     *        start of the validity range of the instance
     * @param durationIn
     *        duration of the validity range of the instance
     * @param xCoeffsIn
     *        Chebyshev polynomials coefficients for the X component
     *        (a reference to the array will be stored in the instance)
     * @param yCoeffsIn
     *        Chebyshev polynomials coefficients for the Y component
     *        (a reference to the array will be stored in the instance)
     * @param zCoeffsIn
     *        Chebyshev polynomials coefficients for the Z component
     *        (a reference to the array will be stored in the instance)
     */
    public PosVelChebyshev(final AbsoluteDate startIn, final double durationIn,
        final double[] xCoeffsIn, final double[] yCoeffsIn,
        final double[] zCoeffsIn) {
        this.start = startIn;
        this.duration = durationIn;
        this.xCoeffs = xCoeffsIn;
        this.yCoeffs = yCoeffsIn;
        this.zCoeffs = zCoeffsIn;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return this.start;
    }

    /**
     * Get model validity duration.
     *
     * @return model validity duration in seconds
     */
    public double getValidityDuration() {
        return this.duration;
    }

    /**
     * Check if the instance is the exact successor of another model.
     * <p>
     * The instance is the successor of another model if its start date is within a 1ms tolerance interval of the end
     * date of the other model.
     * </p>
     *
     * @param predecessor
     *        model to check instance against
     * @return true if the instance is the successor of the predecessor model
     */
    public boolean isSuccessorOf(final PosVelChebyshev predecessor) {
        final double gap = this.start.durationFrom(predecessor.start) - predecessor.duration;
        return MathLib.abs(gap) < THRESHOLD;
    }

    /**
     * Check if a date is in validity range.
     *
     * @param date
     *        date to check
     * @return true if date is in validity range
     */
    public boolean inRange(final AbsoluteDate date) {
        final double dt = date.durationFrom(this.start);
        return (dt >= -THRESHOLD) && (dt <= this.duration + THRESHOLD);
    }

    /**
     * Get the position-velocity-acceleration at a specified date.
     *
     * @param date
     *        date at which position-velocity-acceleration is requested
     * @return position-velocity-acceleration at specified date
     */
    public PVCoordinates getPositionVelocity(final AbsoluteDate date) {

        // normalize date
        final double t = (2 * date.durationFrom(this.start) - this.duration) / this.duration;
        final double twoT = 2 * t;

        // initialize Chebyshev polynomials recursion
        double pKm1 = 1;
        double pK = t;
        double xP = this.xCoeffs[0];
        double yP = this.yCoeffs[0];
        double zP = this.zCoeffs[0];

        // initialize Chebishev polynomials derivatives recursion
        double qKm1 = 0;
        double qK = 1;
        double xV = 0;
        double yV = 0;
        double zV = 0;

        // initialize Chebishev polynomials 2nd derivatives recursion
        double rKm1 = 0;
        double rK = 0;
        double xA = 0;
        double yA = 0;
        double zA = 0;

        // combine polynomials by applying coefficients
        for (int k = 1; k < this.xCoeffs.length; ++k) {

            // consider last computed polynomials on position
            xP += this.xCoeffs[k] * pK;
            yP += this.yCoeffs[k] * pK;
            zP += this.zCoeffs[k] * pK;

            // consider last computed polynomials on velocity
            xV += this.xCoeffs[k] * qK;
            yV += this.yCoeffs[k] * qK;
            zV += this.zCoeffs[k] * qK;

            // consider last computed polynomials on acceleration
            xA += this.xCoeffs[k] * rK;
            yA += this.yCoeffs[k] * rK;
            zA += this.zCoeffs[k] * rK;

            // compute next Chebyshev polynomial value
            final double pKm2 = pKm1;
            pKm1 = pK;
            pK = twoT * pKm1 - pKm2;

            // compute next Chebyshev polynomial derivative
            final double qKm2 = qKm1;
            qKm1 = qK;
            qK = twoT * qKm1 + 2 * pKm1 - qKm2;

            // compute next Chebyshev polynomial second derivative
            final double rKm2 = rKm1;
            rKm1 = rK;
            rK = twoT * rKm1 + 4 * qKm1 - rKm2;
        }

        final double vScale = 2 / this.duration;
        final double aScale = vScale * vScale;
        return new PVCoordinates(new Vector3D(xP, yP, zP),
            new Vector3D(xV * vScale, yV * vScale, zV * vScale), new Vector3D(xA * aScale, yA * aScale, zA * aScale));
    }

}
