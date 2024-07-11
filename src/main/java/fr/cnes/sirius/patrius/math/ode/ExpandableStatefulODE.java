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

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;

/**
 * This class represents a combined set of first order differential equations,
 * with at least a primary set of equations expandable by some sets of secondary
 * equations.
 * <p>
 * One typical use case is the computation of the Jacobian matrix for some ODE. In this case, the primary set of
 * equations corresponds to the raw ODE, and we add to this set another bunch of secondary equations which represent the
 * Jacobian matrix of the primary set.
 * </p>
 * <p>
 * We want the integrator to use <em>only</em> the primary set to estimate the errors and hence the step sizes. It
 * should <em>not</em> use the secondary equations in this computation. The {@link AbstractIntegrator integrator} will
 * be able to know where the primary set ends and so where the secondary sets begin.
 * </p>
 * 
 * @see FirstOrderDifferentialEquations
 * @see JacobianMatrices
 * 
 * @version $Id: ExpandableStatefulODE.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */

public class ExpandableStatefulODE {

    /** Primary differential equation. */
    private final FirstOrderDifferentialEquations primary;

    /** Mapper for primary equation. */
    private final EquationsMapper primaryMapper;

    /** Time. */
    private double time;

    /** State. */
    private final double[] primaryState;

    /** State derivative. */
    private final double[] primaryStateDot;

    /** Components of the expandable ODE. */
    private List<SecondaryComponent> components;

    /**
     * Build an expandable set from its primary ODE set.
     * 
     * @param primaryIn
     *        the primary set of differential equations to be integrated.
     */
    public ExpandableStatefulODE(final FirstOrderDifferentialEquations primaryIn) {
        final int n = primaryIn.getDimension();
        this.primary = primaryIn;
        this.primaryMapper = new EquationsMapper(0, n);
        this.time = Double.NaN;
        this.primaryState = new double[n];
        this.primaryStateDot = new double[n];
        this.components = new ArrayList<ExpandableStatefulODE.SecondaryComponent>();
    }

    /**
     * Get the primary set of differential equations.
     * 
     * @return primary set of differential equations
     */
    public FirstOrderDifferentialEquations getPrimary() {
        return this.primary;
    }

    /**
     * Return the dimension of the complete set of equations.
     * <p>
     * The complete set of equations correspond to the primary set plus all secondary sets.
     * </p>
     * 
     * @return dimension of the complete set of equations
     */
    public int getTotalDimension() {
        if (this.components.isEmpty()) {
            // there are no secondary equations, the complete set is limited to the primary set
            return this.primaryMapper.getDimension();
        } else {
            // there are secondary equations, the complete set ends after the last set
            final EquationsMapper lastMapper = this.components.get(this.components.size() - 1).mapper;
            return lastMapper.getFirstIndex() + lastMapper.getDimension();
        }
    }

    /**
     * Get the current time derivative of the complete state vector.
     * 
     * @param t
     *        current value of the independent <I>time</I> variable
     * @param y
     *        array containing the current value of the complete state vector
     * @param yDot
     *        placeholder array where to put the time derivative of the complete state vector
     * @exception MaxCountExceededException
     *            if the number of functions evaluations is exceeded
     * @exception DimensionMismatchException
     *            if arrays dimensions do not match equations settings
     */
    public void computeDerivatives(final double t, final double[] y, final double[] yDot) {

        // compute derivatives of the primary equations
        this.primaryMapper.extractEquationData(y, this.primaryState);
        this.primary.computeDerivatives(t, this.primaryState, this.primaryStateDot);
        this.primaryMapper.insertEquationData(this.primaryStateDot, yDot);

        // Add contribution for secondary equations
        for (final SecondaryComponent component : this.components) {
            component.mapper.extractEquationData(y, component.state);
            component.equation.computeDerivatives(t, this.primaryState, this.primaryStateDot,
                component.state, component.stateDot);
            component.mapper.insertEquationData(component.stateDot, yDot);
        }

    }

    /**
     * Add a set of secondary equations to be integrated along with the primary set.
     * 
     * @param secondary
     *        secondary equations set
     * @return index of the secondary equation in the expanded state
     */
    public int addSecondaryEquations(final SecondaryEquations secondary) {

        final int firstIndex;
        if (this.components.isEmpty()) {
            // lazy creation of the components list
            this.components = new ArrayList<ExpandableStatefulODE.SecondaryComponent>();
            firstIndex = this.primary.getDimension();
        } else {
            final SecondaryComponent last = this.components.get(this.components.size() - 1);
            firstIndex = last.mapper.getFirstIndex() + last.mapper.getDimension();
        }

        this.components.add(new SecondaryComponent(secondary, firstIndex));

        return this.components.size() - 1;

    }

    /**
     * Get an equations mapper for the primary equations set.
     * 
     * @return mapper for the primary set
     * @see #getSecondaryMappers()
     */
    public EquationsMapper getPrimaryMapper() {
        return this.primaryMapper;
    }

    /**
     * Get the equations mappers for the secondary equations sets.
     * 
     * @return equations mappers for the secondary equations sets
     * @see #getPrimaryMapper()
     */
    public EquationsMapper[] getSecondaryMappers() {
        final EquationsMapper[] mappers = new EquationsMapper[this.components.size()];
        for (int i = 0; i < mappers.length; ++i) {
            mappers[i] = this.components.get(i).mapper;
        }
        return mappers;
    }

    /**
     * Set current time.
     * 
     * @param timeIn
     *        current time
     */
    public void setTime(final double timeIn) {
        this.time = timeIn;
    }

    /**
     * Get current time.
     * 
     * @return current time
     */
    public double getTime() {
        return this.time;
    }

    /**
     * Set primary part of the current state.
     * 
     * @param primaryStateIn
     *        primary part of the current state
     * @throws DimensionMismatchException
     *         if the dimension of the array does not
     *         match the primary set
     */
    public void setPrimaryState(final double[] primaryStateIn) {

        // safety checks
        if (primaryStateIn.length != this.primaryState.length) {
            throw new DimensionMismatchException(primaryStateIn.length, this.primaryState.length);
        }

        // set the data
        System.arraycopy(primaryStateIn, 0, this.primaryState, 0, primaryStateIn.length);

    }

    /**
     * Get primary part of the current state.
     * 
     * @return primary part of the current state
     */
    public double[] getPrimaryState() {
        return this.primaryState.clone();
    }

    /**
     * Get primary part of the current state derivative.
     * 
     * @return primary part of the current state derivative
     */
    public double[] getPrimaryStateDot() {
        return this.primaryStateDot.clone();
    }

    /**
     * Set secondary part of the current state.
     * 
     * @param index
     *        index of the part to set as returned by {@link #addSecondaryEquations(SecondaryEquations)}
     * @param secondaryState
     *        secondary part of the current state
     * @throws DimensionMismatchException
     *         if the dimension of the partial state does not
     *         match the selected equations set dimension
     */
    public void setSecondaryState(final int index, final double[] secondaryState) {

        // get either the secondary state
        final double[] localArray = this.components.get(index).state;

        // safety checks
        if (secondaryState.length != localArray.length) {
            throw new DimensionMismatchException(secondaryState.length, localArray.length);
        }

        // set the data
        System.arraycopy(secondaryState, 0, localArray, 0, secondaryState.length);

    }

    /**
     * Get secondary part of the current state.
     * 
     * @param index
     *        index of the part to set as returned by {@link #addSecondaryEquations(SecondaryEquations)}
     * @return secondary part of the current state
     */
    public double[] getSecondaryState(final int index) {
        return this.components.get(index).state.clone();
    }

    /**
     * Get secondary part of the current state derivative.
     * 
     * @param index
     *        index of the part to set as returned by {@link #addSecondaryEquations(SecondaryEquations)}
     * @return secondary part of the current state derivative
     */
    public double[] getSecondaryStateDot(final int index) {
        return this.components.get(index).stateDot.clone();
    }

    /**
     * Set the complete current state.
     * 
     * @param completeState
     *        complete current state to copy data from
     * @throws DimensionMismatchException
     *         if the dimension of the complete state does not
     *         match the complete equations sets dimension
     */
    public void setCompleteState(final double[] completeState) {

        // safety checks
        if (completeState.length != this.getTotalDimension()) {
            throw new DimensionMismatchException(completeState.length, this.getTotalDimension());
        }

        // set the data
        this.primaryMapper.extractEquationData(completeState, this.primaryState);
        for (final SecondaryComponent component : this.components) {
            component.mapper.extractEquationData(completeState, component.state);
        }

    }

    /**
     * Get the complete current state.
     * 
     * @return complete current state
     * @throws DimensionMismatchException
     *         if the dimension of the complete state does not
     *         match the complete equations sets dimension
     */
    public double[] getCompleteState() {

        // allocate complete array
        final double[] completeState = new double[this.getTotalDimension()];

        // set the data
        this.primaryMapper.insertEquationData(this.primaryState, completeState);
        for (final SecondaryComponent component : this.components) {
            component.mapper.insertEquationData(component.state, completeState);
        }

        return completeState;

    }

    /** Components of the compound stateful ODE. */
    private static class SecondaryComponent {

        /** Secondary differential equation. */
        private final SecondaryEquations equation;

        /** Mapper between local and complete arrays. */
        private final EquationsMapper mapper;

        /** State. */
        private final double[] state;

        /** State derivative. */
        private final double[] stateDot;

        /**
         * Simple constructor.
         * 
         * @param equationIn
         *        secondary differential equation
         * @param firstIndex
         *        index to use for the first element in the complete arrays
         */
        public SecondaryComponent(final SecondaryEquations equationIn, final int firstIndex) {
            final int n = equationIn.getDimension();
            this.equation = equationIn;
            this.mapper = new EquationsMapper(firstIndex, n);
            this.state = new double[n];
            this.stateDot = new double[n];
        }

    }

}
