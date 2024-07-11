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
 *
 * @history created 11/03/2013
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:655:27/07/2016:add finite differences step
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.analysis.differentiation.FiniteDifferencesDifferentiator;
import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateDifferentiableVectorFunction;
import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateVectorFunctionDifferentiator;
import fr.cnes.sirius.patrius.math.analysis.integration.TrapezoidIntegrator;
import fr.cnes.sirius.patrius.math.analysis.integration.UnivariateIntegrator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

/**
 * This abstract class is a time-dependent function representing a vector 3D.
 *
 * @author Tiziana Sabatini
 *
 * @concurrency conditionally thread-safe
 *
 * @concurrency.comment thread-safe if the attributes differentiator and integrator are thread-safe and if
 *                      the implementation of this abstract class is thread-safe too.
 *
 * @version $Id: AbstractVector3DFunction.java 18065 2017-10-02 16:42:02Z bignon $
 *
 * @since 1.3
 *
 */
public abstract class AbstractVector3DFunction implements Vector3DFunction {

    /** Default finite difference step. */
    public static final double DEFAULT_STEP = 0.001;

    /**
     * Maximum number of evaluations.
     */
    private static final int MAXEVAL = Integer.MAX_VALUE;

    /** The date at x = 0. */
    private final AbsoluteDate zero;

    /** The differentiator. */
    private final UnivariateVectorFunctionDifferentiator differentiator;

    /** The integrator. */
    private final UnivariateIntegrator integrator;

    /**
     * Constructor setting a default finite differences differentiator and a default trapezoid integrator.
     *
     * @param zeroDate
     *        the date at x = 0.
     */
    public AbstractVector3DFunction(final AbsoluteDate zeroDate) {
        this.zero = zeroDate;
        // default differentiator:
        this.differentiator = new FiniteDifferencesDifferentiator(4, DEFAULT_STEP);
        // default integrator:
        this.integrator = new TrapezoidIntegrator();
    }

    /**
     * Constructor setting a default trapezoid integrator.
     *
     * @param zeroDate
     *        the date at x = 0.
     * @param inDifferentiator
     *        the differentiation method used to compute the first derivative of the current vector function
     *        components.
     */
    public AbstractVector3DFunction(final AbsoluteDate zeroDate,
                                    final UnivariateVectorFunctionDifferentiator inDifferentiator) {
        this.zero = zeroDate;
        this.differentiator = inDifferentiator;
        // default integrator:
        this.integrator = new TrapezoidIntegrator();
    }

    /**
     * Constructor.
     *
     * @param zeroDate
     *        the date at x = 0.
     * @param inDifferentiator
     *        the differentiation method used to compute the first derivative of the current vector function
     *        components.
     * @param inIntegrator
     *        the integration method used to compute the integral of the current vector function components.
     */
    public AbstractVector3DFunction(final AbsoluteDate zeroDate,
            final UnivariateVectorFunctionDifferentiator inDifferentiator,
            final UnivariateIntegrator inIntegrator) {
        this.zero = zeroDate;
        this.differentiator = inDifferentiator;
        this.integrator = inIntegrator;
    }

    /**
     * Get the vector at a given date. This method must be implemented in the inherited classes.
     *
     * @param date
     *        the date
     * @return the vector at a given date; the vector is an instance of the {@link Vector3D} class.
     * @throws PatriusException
     *         if vector3D cannot be computed
     */
    @Override
    public abstract Vector3D getVector3D(final AbsoluteDate date) throws PatriusException;

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
     * Get the integrator.
     *
     * @return the integrator.
     */
    public final UnivariateIntegrator getIntegrator() {
        return this.integrator;
    }

    /**
     * Compute the components of the vector at the (zero + x) date.
     *
     * @param x
     *        the time from the date zero for which the function value should be computed
     * @return the three components of the vector at the given date.
     * @throws PatriusExceptionWrapper
     *         if problems to compute value (frame transformation, or other)
     */
    @Override
    public final double[] value(final double x) {
        final AbsoluteDate date = this.zero.shiftedBy(x);
        // Compute the vector at the zero + x date:
        final Vector3D vector;
        try {
            vector = this.getVector3D(date);
            return new double[] { vector.getX(), vector.getY(), vector.getZ() };
        } catch (final PatriusException e) {
            throw new PatriusExceptionWrapper(e);
        }
    }

    /**
     * Compute the {@link Vector3DFunction} representing the n-th derivative of the current vector function.<br>
     * The differentiation is performed using a numerical differentiation method. This method can be overridden if an
     * analytical differentiation should be performed instead.
     *
     * @param order
     *        the order n
     * @return the n-th derivative of the current vector function.
     */
    @Override
    public Vector3DFunction nthDerivative(final int order) {
        final Vector3DFunction parent = this;

        return new AbstractVector3DFunction(this.zero, this.differentiator){
            /** {@inheritDoc} */
            @Override
            public Vector3D getVector3D(final AbsoluteDate date) {
                final UnivariateDifferentiableVectorFunction diff = this.getDifferentiator().differentiate(parent);
                final double dt = date.durationFrom(this.getZeroDate());
                // Compute the nth derivative using the DerivativeStructure:
                final DerivativeStructure s = new DerivativeStructure(1, order, 0, dt);
                // Create the new Vector3D from the derivatives value:
                return new Vector3D(diff.value(s)[0].getPartialDerivative(order),
                    diff.value(s)[1].getPartialDerivative(order), diff.value(s)[2].getPartialDerivative(order));
            }
        };
    }

    /**
     * Returns the integral of the vector function in the given interval. The integration is performed using a numerical
     * integration method. This method can be overridden if an analytical integration should be performed instead.
     *
     * @param x0
     *        the lower bound of the interval.
     * @param xf
     *        the upper bound of the interval.
     * @return the value of the integral
     */
    @Override
    public Vector3D integral(final double x0, final double xf) {
        // Integrate the three components:
        final double int1 = this.integrator.integrate(MAXEVAL, this.getV1(), x0, xf);
        final double int2 = this.integrator.integrate(MAXEVAL, this.getV2(), x0, xf);
        final double int3 = this.integrator.integrate(MAXEVAL, this.getV3(), x0, xf);
        return new Vector3D(int1, int2, int3);
    }

    /**
     * Gets the univariate function representing the first component of the vector.
     *
     * @return the function representing V1.
     */
    private UnivariateFunction getV1() {
        final Vector3DFunction parent = this;
        return new UnivariateFunction(){
            /** Serializable UID. */
            private static final long serialVersionUID = -8583744653653197417L;

            /** {@inheritDoc} */
            @Override
            public double value(final double x) {
                return parent.value(x)[0];
            }
        };
    }

    /**
     * Gets the univariate function representing the second component of the vector.
     *
     * @return the function representing V2.
     */
    private UnivariateFunction getV2() {
        final Vector3DFunction parent = this;
        return new UnivariateFunction(){
            /** Serializable UID. */
            private static final long serialVersionUID = 1743961029888280348L;

            /** {@inheritDoc} */
            @Override
            public double value(final double x) {
                return parent.value(x)[1];
            }
        };
    }

    /**
     * Gets the univariate function representing the third component of the vector.
     *
     * @return the function representing V3.
     */
    private UnivariateFunction getV3() {
        final Vector3DFunction parent = this;
        return new UnivariateFunction(){
            /** Serializable UID. */
            private static final long serialVersionUID = -2715098889602998115L;

            /** {@inheritDoc} */
            @Override
            public double value(final double x) {
                return parent.value(x)[2];
            }
        };
    }
}
