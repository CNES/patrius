/**
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
 * @history created 11/03/2013
 * HISTORY
* VERSION:4.7:FA:FA-2762:18/05/2021:Probleme lors des controles qualite via la PIC 
 * VERSION:4.6:FA:FA-2655:27/01/2021:[PATRIUS] Anomalie dans la classe estimateRateFunction de la classe 
 * AbtractOrientationFunction
 * VERSION:4.5:FA:FA-2440:27/05/2020:difference finie en debut de segment QuaternionPolynomialProfile 
 * VERSION:4.4:DM:DM-2231:04/10/2019:[PATRIUS] Creation d'un cache dans les profils de vitesse angulaire
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.kinematics;

import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.analysis.differentiation.FiniteDifferencesDifferentiator;
import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateDifferentiableVectorFunction;
import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateVectorFunctionDifferentiator;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.AbstractVector3DFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3DFunction;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

/**
 * This abstract class is a time-dependent function representing an orientation.
 *
 * @author Tiziana Sabatini
 *
 * @concurrency conditionally thread-safe
 *
 * @concurrency.comment thread-safe if the attribute differentiator is thread-safe and if
 *                      the implementation of this abstract class is thread-safe too.
 *
 * @version $Id: AbstractOrientationFunction.java 18065 2017-10-02 16:42:02Z bignon $
 *
 * @since 1.3
 *
 */
public abstract class AbstractOrientationFunction implements OrientationFunction {

    /** Delta-time used for finite differences. */
    private static final double DT = 0.001;

    /** The date at x = 0. */
    private final AbsoluteDate zero;

    /** The differentiator. */
    private final UnivariateVectorFunctionDifferentiator differentiator;

    /**
     * Constructor setting a default finite differences differentiator.
     *
     * @param zeroDate
     *        the date at x = 0.
     */
    public AbstractOrientationFunction(final AbsoluteDate zeroDate) {
        this.zero = zeroDate;
        // default differentiator:
        this.differentiator = new FiniteDifferencesDifferentiator(4, DT);
    }

    /**
     * @param zeroDate
     *        the date at x = 0.
     * @param inDifferentiator
     *        the differentiation method used to compute the first derivative of the current orientation function
     *        components.
     */
    public AbstractOrientationFunction(final AbsoluteDate zeroDate,
            final UnivariateVectorFunctionDifferentiator inDifferentiator) {
        this.zero = zeroDate;
        this.differentiator = inDifferentiator;
    }

    /**
     * Get the orientation at a given date. This method must be implemented in the inherited classes.
     *
     * @param date
     *        the date
     * @return the orientation at a given date; the orientation is an instance of the {@link Rotation} class.
     * @throws PatriusException
     *         if rotation cannot be computed
     */
    @Override
    public abstract Rotation getOrientation(final AbsoluteDate date) throws PatriusException;

    /**
     * Get the date at x = 0.
     *
     * @return the date at x = 0.
     */
    public final AbsoluteDate getZeroDate() {
        return this.zero;
    }

    /**
     * Get the differentiator.
     *
     * @return the differentiator.
     */
    public final UnivariateVectorFunctionDifferentiator getDifferentiator() {
        return this.differentiator;
    }

    /**
     * Compute the quaternion components of the orientation at the (zero + x) date.
     *
     * @param x
     *        the time from the date zero for which the function value should be computed
     * @return the quaternion components representing the orientation at the given date.
     * @throws PatriusExceptionWrapper
     *         if problems to compute value (frame transformation, or other)
     */
    @Override
    public final double[] value(final double x) {
        final AbsoluteDate date = this.zero.shiftedBy(x);
        // Compute the orientation at the zero + x date:
        final Rotation rotation;
        try {
            rotation = this.getOrientation(date);
            return rotation.getQi();
        } catch (final PatriusException e) {
            throw new PatriusExceptionWrapper(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public final Vector3DFunction estimateRateFunction(final double dt, final AbsoluteDateInterval interval) {

        // Return a new Vector3DFunction
        return new AbstractVector3DFunction(this.zero) {

            /** {@inheritDoc} */
            @Override
            public Vector3D getVector3D(final AbsoluteDate date) throws PatriusException {
                
                // Handle specific case of interval size being smaller than dt
                // Finite differences are then performed over interval size
                double deltat = dt;
                if (interval.getDuration() < dt) {
                    deltat = interval.getDuration();
                }

                // Orientation at t - h:
                final Rotation rminus;
                double deltatMinus = deltat / 2.0;
                final AbsoluteDate dateMinus = date.shiftedBy(-deltatMinus);
                if (interval.contains(dateMinus)) {
                    rminus = AbstractOrientationFunction.this.getOrientation(dateMinus);
                } else {
                    // Forward first order finite difference
                    deltatMinus = 0;
                    rminus = AbstractOrientationFunction.this.getOrientation(date);
                }
                // Orientation at t + h:
                final Rotation rplus;
                double deltatPlus = deltat / 2.0;
                final AbsoluteDate datePlus = date.shiftedBy(deltatPlus);
                if (interval.contains(datePlus)) {
                    rplus = AbstractOrientationFunction.this.getOrientation(datePlus);
                } else {
                    // Backward first order finite difference
                    deltatPlus = 0;
                    rplus = AbstractOrientationFunction.this.getOrientation(date);
                }
                // deltat cannot be 0, since it would means the orientation function
                return AngularCoordinates.estimateRate(rminus, rplus, deltatMinus + deltatPlus);
            }
        };
    }

    /**
     * Estimate the spin at a given date from the current {@link OrientationFunction} using the
     * {@link AngularCoordinates#estimateRate(Rotation, Rotation, double)} method.
     *
     * @param date
     *        the current date
     * @param dt
     *        time elapsed between the dates of the two orientations
     * @param interval interval of validity of the function
     * @return the spin at the current date.
     * @throws PatriusException
     *         if the rate cannot be estimated
     */
    public final Vector3D estimateRate(final AbsoluteDate date,
            final double dt, final AbsoluteDateInterval interval) throws PatriusException {
        // Build a new Vector3DFunction:
        final Vector3DFunction vector3DFunction = this.estimateRateFunction(dt, interval);
        return vector3DFunction.getVector3D(date);
    }

    /**
     * Compute the {@link OrientationFunction} representing the first derivative of the current orientation function
     * components.<br>
     * The differentiation is performed using a numerical differentiation method. This method can be overridden if an
     * analytical differentiation should be performed instead.
     *
     * @return a new {@link OrientationFunction} containing the first derivative of the orientation function components.
     */
    @Override
    public OrientationFunction derivative() {

        final OrientationFunction parent = this;

        return new AbstractOrientationFunction(this.zero, this.differentiator) {

            /** {@inheritDoc} */
            @Override
            public Rotation getOrientation(final AbsoluteDate date) {
                final UnivariateDifferentiableVectorFunction diff = this.getDifferentiator().differentiate(parent);
                final double dt = date.durationFrom(this.getZeroDate());
                // Compute the first derivative using the DerivativeStructure:
                final DerivativeStructure s = new DerivativeStructure(1, 2, 0, dt);
                final DerivativeStructure[] sValue = diff.value(s);
                // Create the new Rotation from the derivatives value:
                return new Rotation(false, sValue[0].getPartialDerivative(1), sValue[1].getPartialDerivative(1),
                        sValue[2].getPartialDerivative(1), sValue[3].getPartialDerivative(1));
            }
        };
    }
}
