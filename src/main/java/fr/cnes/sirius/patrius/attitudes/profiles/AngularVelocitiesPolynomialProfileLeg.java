/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:FA:FA-86:30/06/2023:[PATRIUS] Retours JE Alice
 * VERSION:4.11:DM:DM-3197:22/05/2023:[PATRIUS] Deplacement dans PATRIUS de classes façade ALGO DV SIRUS 
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.4:DM:DM-2231:04/10/2019:[PATRIUS] Creation d'un cache dans les profils de vitesse angulaire
 * VERSION:4.4:DM:DM-2097:04/10/2019:[PATRIUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.profiles;

import java.io.Serializable;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.analysis.integration.SimpsonIntegrator;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3DFunction;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * <p>
 * An attitude angular velocities profile leg, whose x-y-z components are represented with polynomial functions.
 * <p>
 * @author Hubert Marechal, Pierre Brechard
 *
 * @since 4.4
 */
public class AngularVelocitiesPolynomialProfileLeg extends AbstractAngularVelocitiesAttitudeProfile {

    /** Serialization UID. */
    private static final long serialVersionUID = 6380082535101946768L;

    /** Default nature. */
    private static final String DEFAULT_NATURE = "ANGULAR_VELOCITIES_POLYNOMIAL_PROFILE_LEG";

    /** Maximum number of evaluations. */
    private static final int MAXEVAL = Integer.MAX_VALUE;

    /**
     * Build an angular velocity polynomial guidance profile on a leg. The polynomial representing the vector 3D
     * components are generic
     * polynomial functions.
     *
     * @param x
     *        the polynomial function representing the x component of the angular velocity vector3D
     * @param y
     *        the polynomial function representing the y component of the angular velocity vector3D
     * @param z
     *        the polynomial function representing the z component of the angular velocity vector3D
     * @param interval
     *        the time interval of the leg
     * @param frame
     *        Polynomial functions expression frame
     * @param rotationRef
     *        rotation at reference date
     * @param dateRef
     *        Reference date of the polynomial functions
     * @param integType
     *        the Integration type
     * @param integStep
     *        the integration step
     * @throws PatriusException
     *         If the reference date is after the lower bound of the interval to be covered by the profile
     */
    public AngularVelocitiesPolynomialProfileLeg(final PolynomialFunction x,
            final PolynomialFunction y,
            final PolynomialFunction z,
            final Frame frame,
            final AbsoluteDateInterval interval,
            final Rotation rotationRef,
            final AbsoluteDate dateRef,
            final AngularVelocityIntegrationType integType,
            final double integStep) throws PatriusException {
        this(x, y, z, frame, interval, rotationRef, dateRef, integType, integStep, DEFAULT_NATURE);
    }

    /**
     * Build an angular velocity polynomial guidance profile on a leg. The polynomial representing the vector 3D
     * components are generic
     * polynomial functions.
     *
     * @param x
     *        the polynomial function representing the x component of the angular velocity vector3D
     * @param y
     *        the polynomial function representing the y component of the angular velocity vector3D
     * @param z
     *        the polynomial function representing the z component of the angular velocity vector3D
     * @param interval
     *        the time interval of the leg
     * @param frame
     *        Polynomial functions expression frame
     * @param rotationRef
     *        rotation at reference date
     * @param dateRef
     *        Reference date of the polynomial functions
     * @param integType
     *        the Integration type
     * @param integStep
     *        the integration step
     * @param nature
     *        Nature
     * @throws PatriusException
     *         If the reference date is after the lower bound of the interval to be covered by the profile
     */
    public AngularVelocitiesPolynomialProfileLeg(final PolynomialFunction x,
            final PolynomialFunction y,
            final PolynomialFunction z,
            final Frame frame,
            final AbsoluteDateInterval interval,
            final Rotation rotationRef,
            final AbsoluteDate dateRef,
            final AngularVelocityIntegrationType integType,
            final double integStep,
            final String nature) throws PatriusException {
        super(new Polynomial3DFunction(x, y, z, dateRef, interval), frame, interval, rotationRef, dateRef, integType,
                integStep, nature);
    }

    /**
     * Build an angular velocity polynomial guidance profile on a leg. The polynomial representing the vector 3D
     * components are generic
     * polynomial functions.
     *
     * @param x
     *        the polynomial function representing the x component of the angular velocity vector3D
     * @param y
     *        the polynomial function representing the y component of the angular velocity vector3D
     * @param z
     *        the polynomial function representing the z component of the angular velocity vector3D
     * @param interval
     *        the time interval of the leg
     * @param frame
     *        Polynomial functions expression frame
     * @param rotationRef
     *        rotation at reference date
     * @param dateRef
     *        Reference date of the polynomial functions
     * @param integType
     *        the Integration type
     * @param integStep
     *        the integration step
     * @param cacheFreq
     *        Number of integration steps performed between two values stored in the underlying cache
     * @throws PatriusException
     *         If the reference date is after the lower bound of the interval to be covered by the profile
     */
    public AngularVelocitiesPolynomialProfileLeg(final PolynomialFunction x,
            final PolynomialFunction y,
            final PolynomialFunction z,
            final Frame frame,
            final AbsoluteDateInterval interval,
            final Rotation rotationRef,
            final AbsoluteDate dateRef,
            final AngularVelocityIntegrationType integType,
            final double integStep,
            final int cacheFreq) throws PatriusException {
        this(x, y, z, frame, interval, rotationRef, dateRef, integType, integStep, cacheFreq, DEFAULT_NATURE);
    }

    /**
     * Build an angular velocity polynomial guidance profile on a leg. The polynomial representing the vector 3D
     * components are generic
     * polynomial functions.
     *
     * @param x
     *        the polynomial function representing the x component of the angular velocity vector3D
     * @param y
     *        the polynomial function representing the y component of the angular velocity vector3D
     * @param z
     *        the polynomial function representing the z component of the angular velocity vector3D
     * @param interval
     *        the time interval of the leg
     * @param frame
     *        Polynomial functions expression frame
     * @param rotationRef
     *        rotation at reference date
     * @param dateRef
     *        Reference date of the polynomial functions
     * @param integType
     *        the Integration type
     * @param integStep
     *        the integration step
     * @param cacheFreq
     *        Number of integration steps performed between two values stored in the underlying cache
     * @param nature
     *        Nature
     * @throws PatriusException
     *         If the reference date is after the lower bound of the interval to be covered by the profile
     */
    public AngularVelocitiesPolynomialProfileLeg(final PolynomialFunction x,
            final PolynomialFunction y,
            final PolynomialFunction z,
            final Frame frame,
            final AbsoluteDateInterval interval,
            final Rotation rotationRef,
            final AbsoluteDate dateRef,
            final AngularVelocityIntegrationType integType,
            final double integStep,
            final int cacheFreq,
            final String nature) throws PatriusException {
        super(new Polynomial3DFunction(x, y, z, dateRef, interval), frame, interval, rotationRef, dateRef, integType,
                integStep, cacheFreq, nature);
    }

    /**
     * Get the date zero of the polynomial functions.
     * @return the date zero of the polynomial functions.
     */
    public AbsoluteDate getDateZero() {
        return this.dateRef;
    }

    /**
     * Gets the coefficients of the polynomial function representing x angular rate.
     * @return the coefficients of the polynomial function representing x
     */
    public double[] getXCoefficients() {
        return ((Polynomial3DFunction) this.spinVectorFunction).getXCoefficients();
    }

    /**
     * Gets the coefficients of the polynomial function representing y angular rate.
     * @return the coefficients of the polynomial function representing y
     */
    public double[] getYCoefficients() {
        return ((Polynomial3DFunction) this.spinVectorFunction).getYCoefficients();
    }

    /**
     * Gets the coefficients of the polynomial function representing z angular rate.
     * @return the coefficients of the polynomial function representing z
     */
    public double[] getZCoefficients() {
        return ((Polynomial3DFunction) this.spinVectorFunction).getZCoefficients();
    }

    /**
     * Gets the size of the Fourierseries3DFunction, ie 3.
     *
     * @return 3.
     */
    public int getSize() {
        return ((Polynomial3DFunction) this.spinVectorFunction).getSize();
    }

    /** {@inheritDoc} */
    @Override
    public AngularVelocitiesPolynomialProfileLeg copy(final AbsoluteDateInterval newInterval)  {
        // Check new interval is included in old interval
        if (!getTimeInterval().includes(newInterval)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.INTERVAL_MUST_BE_INCLUDED);
        }

        try {
            // Initialization
            final AbsoluteDate dateI = newInterval.getLowerData();
            final Rotation rotationIn = getOrientation(dateI);
            final double[] xCoeffsOld = getXCoefficients();
            final double[] yCoeffsOld = getYCoefficients();
            final double[] zCoeffsOld = getZCoefficients();
            final double newDur = newInterval.getDuration();
            final double delta = dateI.durationFrom(this.dateRef) / getTimeInterval().getDuration();
            final double ratio = newDur / getTimeInterval().getDuration();

            // Truncation
            final double[] xCoeffs = redefinePolynomialCoeffs(xCoeffsOld, delta, ratio);
            final double[] yCoeffs = redefinePolynomialCoeffs(yCoeffsOld, delta, ratio);
            final double[] zCoeffs = redefinePolynomialCoeffs(zCoeffsOld, delta, ratio);

            // Build new instance
            final AngularVelocitiesPolynomialProfileLeg res = new AngularVelocitiesPolynomialProfileLeg(
                    new PolynomialFunction(xCoeffs), new PolynomialFunction(yCoeffs), new PolynomialFunction(zCoeffs),
                    refFrame, newInterval, rotationIn, dateI, type, integStep, cacheFreq, getNature());
            res.setSpinDerivativesComputation(spinDerivativesComputation);
            return res;
        } catch (final PatriusException e) {
            // Should not happen since law is valid on interval
            throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, e);
        }
    }

    /**
     * Redefine polynomial coefficients.
     * @param coeffsOld
     *        the polynomial coefficients on the original leg
     * @param delta
     *        difference between newLowerDate and oldLowerDate
     * @param ratio
     *        ratio
     * @return the new polynomial coefficients
     */
    private static double[] redefinePolynomialCoeffs(final double[] coeffsOld,
            final double delta,
            final double ratio) {

        // Initialization
        final int degree = coeffsOld.length;
        final double[] diff = new double[degree];

        if (Double.compare(delta, 0) == 0) {
            // No delta
            System.arraycopy(coeffsOld, 0, diff, 0, coeffsOld.length);
        } else {
            // Initialization
            final double[][] mtx = new double[degree][degree];

            // Loop on degrees
            for (int n = 0; n < degree; n++) {
                for (int k = 0; k <= n; k++) {
                    final double coeff = ((coeffsOld[n] * factorial(n)) / (factorial(k) * factorial(n - k)))
                            * MathLib.pow(delta, k);
                    mtx[n][k] = coeff;
                    // Sum for fixed (n - k)
                    diff[n - k] = diff[n - k] + mtx[n][k];
                }
            }
        }

        for (int n = 0; n < degree; n++) {
            diff[n] = diff[n] * MathLib.pow(ratio, n);
        }

        // Return result
        return diff;
    }

    /**
     * Factorial.
     * @param n
     *        integer
     * @return compute factorial of n
     */
    private static int factorial(final int n) {
        int result = 1;
        for (int i = 1; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    /**
     * Polynomial normalized {@link Vector3DFunction}
     *
     * @author Hubert Marechal, Pierre Brechard
     *
     * @since 4.4
     */
    private static final class Polynomial3DFunction implements Vector3DFunction, Serializable {

        /** Serialization UID. */
        private static final long serialVersionUID = 7031381783484754784L;

        /** Polynomial function representing the x component of the angular velocity. */
        private final PolynomialFunction xFunction;

        /** Polynomial function representing the y component of the angular velocity. */
        private final PolynomialFunction yFunction;

        /** Polynomial function representing the z component of the angular velocity. */
        private final PolynomialFunction zFunction;

        /** The reference date t<sub>0</sub> of the polynomial leg. */
        private final AbsoluteDate date0;

        /** Interval of validity. */
        private final AbsoluteDateInterval interval;

        /** Interval of validity duration. */
        private final double duration;

        /**
         * Constructor.
         * @param xFunction
         *        Polynomial function representing the x component of the angular velocity
         * @param yFunction
         *        Polynomial function representing the y component of the angular velocity
         * @param zFunction
         *        Polynomial function representing the z component of the angular velocity
         * @param date0
         *        The reference date t<sub>0</sub>
         * @param interval
         *        Interval of validity used for the time-normalization (reduction)
         */
        public Polynomial3DFunction(final PolynomialFunction xFunction,
                final PolynomialFunction yFunction,
                final PolynomialFunction zFunction,
                final AbsoluteDate date0,
                final AbsoluteDateInterval interval) {
            this.xFunction = xFunction;
            this.yFunction = yFunction;
            this.zFunction = zFunction;
            this.date0 = date0;
            this.interval = interval;
            this.duration = interval.getDuration();
        }

        /**
         * Compute the value for the function. The input variable is defined in reduced time :<br>
         * <code>x = (t -t<sub>0</sub>) / T</code><br>
         * where <code>t<sub>0</sub></code> is the initial date, <code>T</code> is the validity interval duration and
         * <code>t<code> is the
         * computation date
         *
         * @param x
         *        the reduced time-point for which the function value should be computed
         * @return the value
         */
        @Override
        public double[] value(final double x) {
            return new double[] { this.xFunction.value(x), this.yFunction.value(x), this.zFunction.value(x) };
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D integral(final double x0,
                final double xf) {
            // Integrate the three components:
            final SimpsonIntegrator integrator = new SimpsonIntegrator();
            final double int1 = integrator.integrate(MAXEVAL, this.xFunction, x0 / this.duration, xf / this.duration)
                    * this.duration;
            final double int2 = integrator.integrate(MAXEVAL, this.yFunction, x0 / this.duration, xf / this.duration)
                    * this.duration;
            final double int3 = integrator.integrate(MAXEVAL, this.zFunction, x0 / this.duration, xf / this.duration)
                    * this.duration;

            // Return result
            return new Vector3D(int1, int2, int3);
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D getVector3D(final AbsoluteDate date) throws PatriusException {
            final double t = date.durationFrom(this.date0) / this.interval.getDuration();
            return new Vector3D(value(t));
        }

        /**
         * Compute the Vector3DFunction representing the n-th derivative of the current vector function. It is
         * implemented considering the
         * time reduction normalization based on the validity interval.
         * @param order
         *        the order n
         * @return the n-th derivative of the current vector function.
         */
        @Override
        public Vector3DFunction nthDerivative(final int order) {
            PolynomialFunction xNthDer = this.xFunction;
            PolynomialFunction yNthDer = this.yFunction;
            PolynomialFunction zNthDer = this.zFunction;
            for (int i = 1; i <= order; ++i) {
                xNthDer = normalizeFunction(xNthDer.derivative(), this.duration);
                yNthDer = normalizeFunction(yNthDer.derivative(), this.duration);
                zNthDer = normalizeFunction(zNthDer.derivative(), this.duration);
            }

            return new Polynomial3DFunction(xNthDer, yNthDer, zNthDer, this.date0, this.interval);
        }

        /**
         * @return the coefficients of the polynomial function representing x angular rate.
         */
        public double[] getXCoefficients() {
            return this.xFunction.getCoefficients();
        }

        /**
         * @return the coefficients of the polynomial function representing y.
         */
        public double[] getYCoefficients() {
            return this.yFunction.getCoefficients();
        }

        /**
         * @return the coefficients of the polynomial function representing z.
         */
        public double[] getZCoefficients() {
            return this.zFunction.getCoefficients();
        }

        /**
         * Creates a {@link PolynomialFunction} normalizing the coefficients of the input function based on the validity
         * duration.
         *
         * @param function
         *        Original function
         * @param duration
         *        Normalization interval duration
         * @return normalized/reduced function
         */
        private static PolynomialFunction normalizeFunction(final PolynomialFunction function,
                final double duration) {
            final double[] coefs = function.getCoefficients();
            final double[] reducedCoefs = new double[coefs.length];
            for (int i = 0; i < coefs.length; ++i) {
                reducedCoefs[i] = coefs[i] / duration;
            }
            return new PolynomialFunction(reducedCoefs);
        }

        /** {@inheritDoc} */
        @Override
        public int getSize() {
            return 3;
        }
    }
}
