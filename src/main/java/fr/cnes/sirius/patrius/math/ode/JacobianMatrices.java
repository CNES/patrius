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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class defines a set of {@link SecondaryEquations secondary equations} to
 * compute the Jacobian matrices with respect to the initial state vector and, if
 * any, to some parameters of the primary ODE set.
 * <p>
 * It is intended to be packed into an {@link ExpandableStatefulODE} in conjunction with a primary set of ODE, which may
 * be:
 * <ul>
 * <li>a {@link FirstOrderDifferentialEquations}</li>
 * <li>a {@link MainStateJacobianProvider}</li>
 * </ul>
 * In order to compute Jacobian matrices with respect to some parameters of the primary ODE set, the following parameter
 * Jacobian providers may be set:
 * <ul>
 * <li>a {@link ParameterJacobianProvider}</li>
 * <li>a {@link ParameterizedODE}</li>
 * </ul>
 * </p>
 * 
 * @see ExpandableStatefulODE
 * @see FirstOrderDifferentialEquations
 * @see MainStateJacobianProvider
 * @see ParameterJacobianProvider
 * @see ParameterizedODE
 * 
 * @version $Id: JacobianMatrices.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
@SuppressWarnings("PMD.NullAssignment")
public class JacobianMatrices {

    /** Expandable first order differential equation. */
    private ExpandableStatefulODE efode;

    /** Index of the instance in the expandable set. */
    private int index;

    /** FODE with exact primary Jacobian computation skill. */
    private final MainStateJacobianProvider jode;

    /** FODE without exact parameter Jacobian computation skill. */
    private ParameterizedODE pode;

    /** Main state vector dimension. */
    private final int stateDim;

    /** Selected parameters for parameter Jacobian computation. */
    private ParameterConfiguration[] selectedParameters;

    /** FODE with exact parameter Jacobian computation skill. */
    private final List<ParameterJacobianProvider> jacobianProviders;

    /** Parameters dimension. */
    private int paramDim;

    /** Boolean for selected parameters consistency. */
    private boolean dirtyParameter;

    /** State and parameters Jacobian matrices in a row. */
    private final double[] matricesData;

    /**
     * Simple constructor for a secondary equations set computing Jacobian matrices.
     * <p>
     * Parameters must belong to the supported ones given by {@link Parameterizable#getParametersNames()}, so the
     * primary set of differential equations must be {@link Parameterizable}.
     * </p>
     * <p>
     * Note that each selection clears the previous selected parameters.
     * </p>
     * 
     * @param fode
     *        the primary first order differential equations set to extend
     * @param hY
     *        step used for finite difference computation with respect to state vector
     * @param parameters
     *        parameters to consider for Jacobian matrices processing
     *        (may be null if parameters Jacobians is not desired)
     * @exception DimensionMismatchException
     *            if there is a dimension mismatch between
     *            the steps array {@code hY} and the equation dimension
     */
    public JacobianMatrices(final FirstOrderDifferentialEquations fode, final double[] hY,
        final String... parameters) {
        this(new MainStateJacobianWrapper(fode, hY), parameters);
    }

    /**
     * Simple constructor for a secondary equations set computing Jacobian matrices.
     * <p>
     * Parameters must belong to the supported ones given by {@link Parameterizable#getParametersNames()}, so the
     * primary set of differential equations must be {@link Parameterizable}.
     * </p>
     * <p>
     * Note that each selection clears the previous selected parameters.
     * </p>
     * 
     * @param jodeIn
     *        the primary first order differential equations set to extend
     * @param parameters
     *        parameters to consider for Jacobian matrices processing
     *        (may be null if parameters Jacobians is not desired)
     */
    public JacobianMatrices(final MainStateJacobianProvider jodeIn,
        final String... parameters) {

        this.efode = null;
        this.index = -1;

        this.jode = jodeIn;
        this.pode = null;

        this.stateDim = jodeIn.getDimension();

        if (parameters == null) {
            this.selectedParameters = null;
            this.paramDim = 0;
        } else {
            this.selectedParameters = new ParameterConfiguration[parameters.length];
            for (int i = 0; i < parameters.length; ++i) {
                this.selectedParameters[i] = new ParameterConfiguration(parameters[i], Double.NaN);
            }
            this.paramDim = parameters.length;
        }
        this.dirtyParameter = false;

        this.jacobianProviders = new ArrayList<ParameterJacobianProvider>();

        // set the default initial state Jacobian to the identity
        // and the default initial parameters Jacobian to the null matrix
        this.matricesData = new double[(this.stateDim + this.paramDim) * this.stateDim];
        for (int i = 0; i < this.stateDim; ++i) {
            this.matricesData[i * (this.stateDim + 1)] = 1.0;
        }

    }

    /**
     * Register the variational equations for the Jacobians matrices to the expandable set.
     * 
     * @param expandable
     *        expandable set into which variational equations should be registered
     * @throws DimensionMismatchException
     *         if the dimension of the partial state does not
     *         match the selected equations set dimension
     * @exception MismatchedEquations
     *            if the primary set of the expandable set does
     *            not match the one used to build the instance
     * @see ExpandableStatefulODE#addSecondaryEquations(SecondaryEquations)
     */
    public void registerVariationalEquations(final ExpandableStatefulODE expandable) {

        // safety checks
        final FirstOrderDifferentialEquations ode = (this.jode instanceof MainStateJacobianWrapper) ?
            ((MainStateJacobianWrapper) this.jode).ode :
            this.jode;
        if (expandable.getPrimary() != ode) {
            throw new MismatchedEquations();
        }

        this.efode = expandable;
        this.index = this.efode.addSecondaryEquations(new JacobiansSecondaryEquations());
        this.efode.setSecondaryState(this.index, this.matricesData);

    }

    /**
     * Add a parameter Jacobian provider.
     * 
     * @param provider
     *        the parameter Jacobian provider to compute exactly the parameter Jacobian matrix
     */
    public void addParameterJacobianProvider(final ParameterJacobianProvider provider) {
        this.jacobianProviders.add(provider);
    }

    /**
     * Set a parameter Jacobian provider.
     * 
     * @param parameterizedOde
     *        the parameterized ODE to compute the parameter Jacobian matrix using finite differences
     */
    public void setParameterizedODE(final ParameterizedODE parameterizedOde) {
        this.pode = parameterizedOde;
        this.dirtyParameter = true;
    }

    /**
     * Set the step associated to a parameter in order to compute by finite
     * difference the Jacobian matrix.
     * <p>
     * Needed if and only if the primary ODE set is a {@link ParameterizedODE}.
     * </p>
     * <p>
     * Given a non zero parameter value pval for the parameter, a reasonable value for such a step is
     * {@code pval * FastMath.sqrt(Precision.EPSILON)}.
     * </p>
     * <p>
     * A zero value for such a step doesn't enable to compute the parameter Jacobian matrix.
     * </p>
     * 
     * @param parameter
     *        parameter to consider for Jacobian processing
     * @param hP
     *        step for Jacobian finite difference computation w.r.t. the specified parameter
     * @see ParameterizedODE
     * @exception UnknownParameterException
     *            if the parameter is not supported
     */
    public void setParameterStep(final String parameter, final double hP) {

        for (final ParameterConfiguration param : this.selectedParameters) {
            if (parameter.equals(param.getParameterName())) {
                param.setHP(hP);
                this.dirtyParameter = true;
                return;
            }
        }

        throw new UnknownParameterException(parameter);

    }

    /**
     * Set the initial value of the Jacobian matrix with respect to state.
     * <p>
     * If this method is not called, the initial value of the Jacobian matrix with respect to state is set to identity.
     * </p>
     * 
     * @param dYdY0
     *        initial Jacobian matrix w.r.t. state
     * @exception DimensionMismatchException
     *            if matrix dimensions are incorrect
     */
    public void setInitialMainStateJacobian(final double[][] dYdY0) {

        // Check dimensions
        this.checkDimension(this.stateDim, dYdY0);
        this.checkDimension(this.stateDim, dYdY0[0]);

        // store the matrix in row major order as a single dimension array
        int i = 0;
        for (final double[] row : dYdY0) {
            System.arraycopy(row, 0, this.matricesData, i, this.stateDim);
            i += this.stateDim;
        }

        if (this.efode != null) {
            this.efode.setSecondaryState(this.index, this.matricesData);
        }

    }

    /**
     * Set the initial value of a column of the Jacobian matrix with respect to one parameter.
     * <p>
     * If this method is not called for some parameter, the initial value of the column of the Jacobian matrix with
     * respect to this parameter is set to zero.
     * </p>
     * 
     * @param pName
     *        parameter name
     * @param dYdP
     *        initial Jacobian column vector with respect to the parameter
     * @exception UnknownParameterException
     *            if a parameter is not supported
     * @throws DimensionMismatchException
     *         if the column vector does not match state dimension
     */
    public void setInitialParameterJacobian(final String pName, final double[] dYdP) {

        // Check dimensions
        this.checkDimension(this.stateDim, dYdP);

        // store the column in a global single dimension array
        int i = this.stateDim * this.stateDim;
        for (final ParameterConfiguration param : this.selectedParameters) {
            if (pName.equals(param.getParameterName())) {
                System.arraycopy(dYdP, 0, this.matricesData, i, this.stateDim);
                if (this.efode != null) {
                    this.efode.setSecondaryState(this.index, this.matricesData);
                }
                return;
            }
            i += this.stateDim;
        }

        throw new UnknownParameterException(pName);

    }

    /**
     * Get the current value of the Jacobian matrix with respect to state.
     * 
     * @param dYdY0
     *        current Jacobian matrix with respect to state.
     */
    public void getCurrentMainSetJacobian(final double[][] dYdY0) {

        // get current state for this set of equations from the expandable fode
        final double[] p = this.efode.getSecondaryState(this.index);

        int j = 0;
        for (int i = 0; i < this.stateDim; i++) {
            System.arraycopy(p, j, dYdY0[i], 0, this.stateDim);
            j += this.stateDim;
        }

    }

    /**
     * Get the current value of the Jacobian matrix with respect to one parameter.
     * 
     * @param pName
     *        name of the parameter for the computed Jacobian matrix
     * @param dYdP
     *        current Jacobian matrix with respect to the named parameter
     */
    public void getCurrentParameterJacobian(final String pName, final double[] dYdP) {

        // get current state for this set of equations from the expandable fode
        final double[] p = this.efode.getSecondaryState(this.index);

        int i = this.stateDim * this.stateDim;
        for (final ParameterConfiguration param : this.selectedParameters) {
            if (param.getParameterName().equals(pName)) {
                System.arraycopy(p, i, dYdP, 0, this.stateDim);
                return;
            }
            i += this.stateDim;
        }

    }

    /**
     * Check array dimensions.
     * 
     * @param expected
     *        expected dimension
     * @param array
     *        (may be null if expected is 0)
     * @throws DimensionMismatchException
     *         if the array dimension does not match the expected one
     */
    private void checkDimension(final int expected, final Object array) {
        final int arrayDimension = (array == null) ? 0 : Array.getLength(array);
        if (arrayDimension != expected) {
            throw new DimensionMismatchException(arrayDimension, expected);
        }
    }

    /**
     * Local implementation of secondary equations.
     * <p>
     * This class is an inner class to ensure proper scheduling of calls by forcing the use of
     * {@link JacobianMatrices#registerVariationalEquations(ExpandableStatefulODE)}.
     * </p>
     */
    private class JacobiansSecondaryEquations implements SecondaryEquations {

        /** {@inheritDoc} */
        @Override
        public int getDimension() {
            return JacobianMatrices.this.stateDim * (JacobianMatrices.this.stateDim + JacobianMatrices.this.paramDim);
        }

        /** {@inheritDoc} */
        // CHECKSTYLE: stop CyclomaticComplexity check
        // Reason: Commons-Math code kept as such
        @Override
        public void computeDerivatives(final double t, final double[] y, final double[] yDot,
                                       final double[] z, final double[] zDot) {
            // CHECKSTYLE: resume CyclomaticComplexity check
            // Lazy initialization
            if (JacobianMatrices.this.dirtyParameter && (JacobianMatrices.this.paramDim != 0)) {
                JacobianMatrices.this.jacobianProviders.add(new ParameterJacobianWrapper(JacobianMatrices.this.jode,
                    JacobianMatrices.this.pode, JacobianMatrices.this.selectedParameters));
                JacobianMatrices.this.dirtyParameter = false;
            }

            // variational equations:
            // from d[dy/dt]/dy0 and d[dy/dt]/dp to d[dy/dy0]/dt and d[dy/dp]/dt

            // compute Jacobian matrix with respect to primary state
            final double[][] dFdY = new double[JacobianMatrices.this.stateDim][JacobianMatrices.this.stateDim];
            JacobianMatrices.this.jode.computeMainStateJacobian(t, y, yDot, dFdY);

            // Dispatch Jacobian matrix in the compound secondary state vector
            for (int i = 0; i < JacobianMatrices.this.stateDim; ++i) {
                final double[] dFdYi = dFdY[i];
                for (int j = 0; j < JacobianMatrices.this.stateDim; ++j) {
                    double s = 0;
                    final int startIndex = j;
                    int zIndex = startIndex;
                    for (int l = 0; l < JacobianMatrices.this.stateDim; ++l) {
                        s += dFdYi[l] * z[zIndex];
                        zIndex += JacobianMatrices.this.stateDim;
                    }
                    zDot[startIndex + i * JacobianMatrices.this.stateDim] = s;
                }
            }

            if (JacobianMatrices.this.paramDim != 0) {
                // compute Jacobian matrices with respect to parameters
                final double[] dFdP = new double[JacobianMatrices.this.stateDim];
                int startIndex = JacobianMatrices.this.stateDim * JacobianMatrices.this.stateDim;
                // loop on all parameters
                for (final ParameterConfiguration param : JacobianMatrices.this.selectedParameters) {
                    boolean found = false;
                    // loop on Jacobian providers to find one supporting the current parameter
                    for (int k = 0; (!found) && (k < JacobianMatrices.this.jacobianProviders.size()); ++k) {
                        final ParameterJacobianProvider provider = JacobianMatrices.this.jacobianProviders.get(k);
                        if (provider.isSupported(param.getParameterName())) {
                            // compute Jacobian matrix with respect to the current parameter
                            provider.computeParameterJacobian(t, y, yDot,
                                param.getParameterName(), dFdP);
                            for (int i = 0; i < JacobianMatrices.this.stateDim; ++i) {
                                final double[] dFdYi = dFdY[i];
                                int zIndex = startIndex;
                                double s = dFdP[i];
                                for (int l = 0; l < JacobianMatrices.this.stateDim; ++l) {
                                    s += dFdYi[l] * z[zIndex];
                                    zIndex++;
                                }
                                zDot[startIndex + i] = s;
                            }
                            // the Jacobian provider has been found 
                            found = true;
                        }
                    }
                    if (!found) {
                        // if no Jacobian provider has been found, fill the array zDot with zeros
                        Arrays.fill(zDot, startIndex, startIndex + JacobianMatrices.this.stateDim, 0.0);
                    }
                    startIndex += JacobianMatrices.this.stateDim;
                }
            }

        }
    }

    /**
     * Wrapper class to compute jacobian matrices by finite differences for ODE
     * which do not compute them by themselves.
     */
    private static class MainStateJacobianWrapper implements MainStateJacobianProvider {

        /** Raw ODE without jacobians computation skill to be wrapped into a MainStateJacobianProvider. */
        private final FirstOrderDifferentialEquations ode;

        /** Steps for finite difference computation of the jacobian df/dy w.r.t. state. */
        private final double[] hY;

        /**
         * Wrap a {@link FirstOrderDifferentialEquations} into a {@link MainStateJacobianProvider}.
         * 
         * @param odeIn
         *        original ODE problem, without jacobians computation skill
         * @param hYIn
         *        step sizes to compute the jacobian df/dy
         * @see JacobianMatrices#setMainStateSteps(double[])
         * @exception DimensionMismatchException
         *            if there is a dimension mismatch between
         *            the steps array {@code hY} and the equation dimension
         */
        public MainStateJacobianWrapper(final FirstOrderDifferentialEquations odeIn,
            final double[] hYIn) {
            this.ode = odeIn;
            this.hY = hYIn.clone();
            if (hYIn.length != odeIn.getDimension()) {
                throw new DimensionMismatchException(odeIn.getDimension(), hYIn.length);
            }
        }

        /** {@inheritDoc} */
        @Override
        public int getDimension() {
            return this.ode.getDimension();
        }

        /** {@inheritDoc} */
        @Override
        public void computeDerivatives(final double t, final double[] y, final double[] yDot) {
            this.ode.computeDerivatives(t, y, yDot);
        }

        /** {@inheritDoc} */
        @Override
        public void computeMainStateJacobian(final double t, final double[] y,
                                             final double[] yDot, final double[][] dFdY) {

            // get ODE dimension
            final int n = this.ode.getDimension();
            // Initialize variable for time derivative of the state vector
            final double[] tmpDot = new double[n];

            for (int j = 0; j < n; ++j) {
                final double savedYj = y[j];
                // temporarily update main state vector
                y[j] += this.hY[j];
                // compute current derivative of the updated state vector
                this.ode.computeDerivatives(t, y, tmpDot);
                // compute column j of Jacobian matrix
                for (int i = 0; i < n; ++i) {
                    dFdY[i][j] = (tmpDot[i] - yDot[i]) / this.hY[j];
                }
                // restore main state vector
                y[j] = savedYj;
            }
        }

    }

    /**
     * Special exception for equations mismatch.
     * 
     * @since 3.1
     */
    public static class MismatchedEquations extends MathIllegalArgumentException {

        /** Serializable UID. */
        private static final long serialVersionUID = 20120902L;

        /** Simple constructor. */
        public MismatchedEquations() {
            super(PatriusMessages.UNMATCHED_ODE_IN_EXPANDED_SET);
        }

    }

}
