/**
 * Copyright 2021-2021 CNES
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
 * VERSION:4.11:DM:DM-38:22/05/2023:[PATRIUS] Suppression de setters pour le MultiNumericalPropagator
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3101:10/05/2022:[PATRIUS] Correction anomalies suite a DM 2767 (bis) 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.8:FA:FA-2941:15/11/2021:[PATRIUS] Correction anomalies suite a DM 2767 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.numerical;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.gravity.AbstractBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.DrozinerGravityModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.parameter.IJacobiansParameterizable;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.multi.MultiPartialDerivativesEquations;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Abstract class for {@link PartialDerivativesEquations} and {@link MultiPartialDerivativesEquations}.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.8
 */
public abstract class AbstractPartialDerivativesEquations implements AdditionalEquations {

    /** Serializable UID. */
    private static final long serialVersionUID = -556926704905099805L;

    /** State vector dimension without additional parameters. */
    private static final int ORBIT_DIMENSION = 6;

    /** Jacobian of acceleration with respect to spacecraft position. */
    private transient double[][] dAccdPos;

    /** Jacobian of acceleration with respect to spacecraft velocity. */
    private transient double[][] dAccdVel;

    /** Jacobian of acceleration with respect to one force model parameter (array reused for all parameters). */
    private transient double[] dAccdParam;

    /** Jacobians providers. */
    private final List<IJacobiansParameterizable> jacobiansProviders;

    /** List of parameters selected for Jacobians computation. */
    private final List<ParameterConfiguration> selectedParameters;

    /** Name. */
    private final String name;

    /** True if the initial Jacobians have not been initialized yet. */
    private boolean isInitialJacobian;

    /** Parameters vector dimension. */
    private int paramDim;

    /** Step used for finite difference computation with respect to spacecraft position. */
    private double hPos;

    /** Boolean for force models / selected parameters consistency. */
    private boolean dirty = false;

    /**
     * Empty constructor for {@link Externalizable} use.
     */
    public AbstractPartialDerivativesEquations() {
        this(null);
    }

    /**
     * Simple constructor.
     * <p>
     * Upon construction, this set of equations is <em>automatically</em> added to the propagator by calling its
     * {@link NumericalPropagator#addAdditionalEquations(AdditionalEquations)} method. So there is no need to call this
     * method explicitly for these equations.
     * </p>
     * 
     * @param nameIn
     *        name of the partial derivatives equations
     */
    public AbstractPartialDerivativesEquations(final String nameIn) {
        this.name = nameIn;
        this.jacobiansProviders = new ArrayList<>();
        this.dirty = true;
        this.selectedParameters = new ArrayList<>();
        this.isInitialJacobian = false;
        this.paramDim = -1;
        this.hPos = Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return this.name;
    }
    
    /**
     * Get a copy of the selected parameters.
     * 
     * @return copy of the list of selected parameters
     */
    public List<ParameterConfiguration> getSelectedParameters(){
        return new ArrayList<>(this.selectedParameters);
    }
    
    /**
     * Clear the selected parameters list.
     */
    public void clearSelectedParameters() {
        this.selectedParameters.clear();
    }
    
    /**
     * Select the parameters to consider for Jacobian processing.
     * <p>
     * Parameters names have to be consistent with some {@link ForceModel} added elsewhere.
     * </p>
     * 
     * @param parameters
     *        parameters to consider for Jacobian processing. Parameters will not be added if already added elsewhere
     * @see NumericalPropagator#addForceModel(ForceModel)
     * @see #setInitialJacobians(SpacecraftState, double[][], double[][])
     * @see ForceModel
     * @see fr.cnes.sirius.patrius.math.parameter.Parameterizable
     */
    public void selectParameters(final Parameter... parameters) {
        for (final Parameter param : parameters) {
            this.selectParamAndStep(param, Double.NaN);
        }
        this.dirty = true;
    }

    /**
     * Select the parameters to consider for Jacobian processing.
     * <p>
     * Parameters names have to be consistent with some {@link ForceModel} added elsewhere.
     * </p>
     * 
     * @param parameters
     *        list of parameters to consider for Jacobian processing
     * @see NumericalPropagator#addForceModel(ForceModel)
     * @see #setInitialJacobians(SpacecraftState, double[][], double[][])
     * @see ForceModel
     * @see fr.cnes.sirius.patrius.math.parameter.Parameterizable
     */
    public void selectParameters(final List<Parameter> parameters) {
        this.concatenate(parameters);
        this.dirty = true;
    }

    /**
     * Select the parameters to consider for Jacobian processing.
     * <p>
     * Parameters names have to be consistent with some {@link ForceModel} added elsewhere.
     * </p>
     * 
     * @param parameter
     *        parameter to consider for Jacobian processing. Parameter will not be added if already added elsewhere
     * @param hP
     *        step to use for computing Jacobian column with respect to the specified parameter
     * @see NumericalPropagator#addForceModel(ForceModel)
     * @see #setInitialJacobians(SpacecraftState, double[][], double[][])
     * @see ForceModel
     * @see fr.cnes.sirius.patrius.math.parameter.Parameterizable
     */
    public void selectParamAndStep(final Parameter parameter, final double hP) {
        if (!this.contains(parameter)){
            this.selectedParameters.add(new ParameterConfiguration(parameter, hP));
        }
        this.dirty = true;
    }

    /**
     * Get the names of the available parameters in the propagator.
     * <p>
     * The names returned depend on the force models set up in the propagator, including the Newtonian attraction from
     * the central body.
     * </p>
     * 
     * @return available parameters
     */
    public List<Parameter> getAvailableParameters() {
        final List<Parameter> available = new ArrayList<>();
        for (final ForceModel model : this.getForceModels()) {
            available.addAll(model.getParameters());
        }
        return available;
    }

    /**
     * Get a mapper between two-dimensional Jacobians and one-dimensional additional state.
     * 
     * @return a mapper between two-dimensional Jacobians and one-dimensional additional state,
     *         with the same name as the instance
     * @exception PatriusException
     *            if the initial Jacobians have not been initialized yet
     * @see #setInitialJacobians(SpacecraftState)
     * @see #setInitialJacobians(SpacecraftState, double[][], double[][])
     */
    public JacobiansMapper getMapper() throws PatriusException {
        // Check if the initial Jacobians have been initialized
        if (!this.isInitialJacobians()) {
            throw new PatriusException(PatriusMessages.STATE_JACOBIAN_NOT_INITIALIZED);
        }

        // List parameters
        final List<Parameter> list = new ArrayList<>();
        for (int i = 0; i < this.selectedParameters.size(); i++) {
            list.add(this.selectedParameters.get(i).getParameter());
        }

        // Build jacobian mapper
        return new JacobiansMapper(this.getName(), list, this.getOrbitType(), this.getPositionAngle(),
            this.getFrame());
    }

    /**
     * Concatenate a list of parameters with the already existing selectedParameters list
     * 
     * Check that the parameter to add is not already on the list (to avoid problems with the step)
     * 
     * @param parameters
     *        list of parameters to concatenate
     */
    public void concatenate(final List<Parameter> parameters) {
        for (final Parameter param : parameters) {
            if (!this.contains(param)){
                this.selectedParameters.add(new ParameterConfiguration(param, Double.NaN));
            }
        }
    }
    
    /**
     * Check if the parameter is already in the selectedParameters list
     * 
     * @param parameter to check
     * @return true if the parameter is in the list
     *         false otherwise 
     */
    public boolean contains(final Parameter parameter) {
        for (int i = 0; i < this.selectedParameters.size(); i++) {
            final Parameter param = this.selectedParameters.get(i).getParameter();
            if (param.equals(parameter)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set the step for finite differences with respect to spacecraft position.
     * 
     * @param hPosition
     *        step used for finite difference computation with respect to spacecraft position (m)
     */
    public void setSteps(final double hPosition) {
        this.hPos = hPosition;
    }

    /**
     * Set the initial value of the Jacobian with respect to state and parameter.
     * <p>
     * This method is equivalent to call {@link #setInitialJacobians(SpacecraftState, double[][], double[][])} with
     * dYdY0 set to the identity matrix and dYdP set to a zero matrix.
     * </p>
     * 
     * @param s0
     *        initial state
     * @return the updated SpacecraftState with the added additional state
     * @exception PatriusException
     *            if the partial equation has not been registered in
     *            the propagator or if matrices dimensions are incorrect
     * @see #selectedParameters
     * @see #selectParamAndStep(Parameter, double)
     */
    public SpacecraftState setInitialJacobians(final SpacecraftState s0) throws PatriusException {
        final double[][] dYdY0 = new double[ORBIT_DIMENSION][ORBIT_DIMENSION];
        final double[][] dYdP = new double[ORBIT_DIMENSION][this.selectedParameters.size()];
        for (int i = 0; i < ORBIT_DIMENSION; ++i) {
            dYdY0[i][i] = 1.0;
        }
        return this.setInitialJacobians(s0, dYdY0, dYdP);
    }

    /**
     * Set the initial value of the Jacobian with respect to state and parameter.
     * 
     * @param s1
     *        current state
     * @param dY1dY0
     *        Jacobian of current state at time t<sub>1</sub> with respect
     *        to state at some previous time t<sub>0</sub> (6x6)
     * @param dY1dP
     *        Jacobian of current state at time t<sub>1</sub> with respect
     *        to parameters (may be null if no parameters are selected)
     * @return the updated SpacecraftStatetate
     * @exception PatriusException
     *            if the partial equation has not been registered in
     *            the propagator or if matrices dimensions are incorrect
     * @see #selectedParameters
     * @see #selectParamAndStep(Parameter, double)
     */
    public SpacecraftState setInitialJacobians(final SpacecraftState s1,
            final double[][] dY1dY0, final double[][] dY1dP) throws PatriusException {

        // Check dimensions
        this.isInitialJacobian = true;
        final int stateDim = dY1dY0.length;
        if ((stateDim != ORBIT_DIMENSION) || (stateDim != dY1dY0[0].length)) {
            throw new PatriusException(PatriusMessages.STATE_JACOBIAN_SHOULD_BE_6X6,
                stateDim, dY1dY0[0].length);
        }
        if ((dY1dP != null) && (stateDim != dY1dP.length)) {
            throw new PatriusException(PatriusMessages.STATE_AND_PARAMETERS_JACOBIANS_ROWS_MISMATCH,
                stateDim, dY1dP.length);
        }

        this.paramDim = (dY1dP == null) ? 0 : dY1dP[0].length;

        // store the matrices as a single dimension array
        final JacobiansMapper mapper = this.getMapper();
        final double[] p = new double[mapper.getAdditionalStateDimension()];
        mapper.setInitialJacobians(s1, dY1dY0, dY1dP, p);

        // add additional state to SpacecraftState
        return s1.addAdditionalState(this.name, p);
    }

    /**
     * Set the initial value of the Jacobian with respect to state.
     * <p>
     * If state does not have partial derivatives yet, an exception is thrown. Call
     * {@link #setInitialJacobians(SpacecraftState)} to initialize Jacobians with default values or
     * {@link #setInitialJacobians(SpacecraftState, double[][], double[][])} to initialize Jacobians with own values.
     * </p>
     * 
     * @param s
     *        current state
     * @param dY1dY0
     *        Jacobian of current state at time t<sub>1</sub> with respect
     *        to state at some previous time t<sub>0</sub> (6x6)
     * @return the updated SpacecraftState
     * @exception PatriusException
     *            if the partial equation has not been registered in
     *            the propagator or if matrices dimensions are incorrect
     * @see #selectedParameters
     * @see #selectParamAndStep(Parameter, double)
     */
    public SpacecraftState setInitialJacobians(final SpacecraftState s,
                                               final double[][] dY1dY0) throws PatriusException {

        // Extract current partial derivatives
        final double[] partialDerivatives = s.getAdditionalState(this.name);

        if (partialDerivatives == null) {
            // At least derivatives with respect to state should have been initialized first
            throw new PatriusException(PatriusMessages.STATE_JACOBIAN_NOT_INITIALIZED);
        }

        // Extract current parameters part
        final JacobiansMapper mapper = this.getMapper();
        double[][] dY1dPCurrent = null;

        if (!this.selectedParameters.isEmpty()) {
            dY1dPCurrent = new double[ORBIT_DIMENSION][this.selectedParameters.size()];
            mapper.getParametersJacobian(s, dY1dPCurrent);
        }

        // Initialize Jacobian
        return this.setInitialJacobians(s, dY1dY0, dY1dPCurrent);
    }

    /**
     * Set the initial value of the Jacobian with respect to state.
     * <p>
     * If state does not have partial derivatives yet, an exception is thrown. Call
     * {@link #setInitialJacobians(SpacecraftState)} to initialize Jacobians with default values or
     * {@link #setInitialJacobians(SpacecraftState, double[][], double[][])} to initialize Jacobians with own values.
     * </p>
     * 
     * @param s
     *        current state
     * @param parameter
     *        parameter
     * @param dY1dP
     *        Jacobian of current state at time t<sub>1</sub> with respect to provided parameter
     * @return the updated SpacecraftState
     * @exception PatriusException
     *            if the partial equation has not been registered in
     *            the propagator or if matrices dimensions are incorrect or if state partial derivatives have not
     *            been initialized beforehand
     * @see #selectedParameters
     * @see #selectParamAndStep(Parameter, double)
     */
    @SuppressWarnings("PMD.AvoidArrayLoops")
    public SpacecraftState setInitialJacobians(final SpacecraftState s, final Parameter parameter,
                                               final double[] dY1dP) throws PatriusException {

        // Extract current partial derivatives
        final double[] partialDerivatives = s.getAdditionalState(this.name);

        if (partialDerivatives == null) {
            // At least derivatives with respect to state should have been initialized first
            throw new PatriusException(PatriusMessages.STATE_JACOBIAN_NOT_INITIALIZED);
        }

        // Retrieve current partial derivatives
        final JacobiansMapper mapper = this.getMapper();
        final double[][] dY1dY0Current = new double[ORBIT_DIMENSION][ORBIT_DIMENSION];
        final double[][] dY1dPCurrent = new double[ORBIT_DIMENSION][this.selectedParameters.size()];
        mapper.getStateJacobian(s, dY1dY0Current);
        mapper.getParametersJacobian(s, dY1dPCurrent);

        // Find column number
        int index = -1;
        for (int i = 0; i < this.selectedParameters.size(); i++) {
            if (parameter.equals(this.selectedParameters.get(i).getParameter())) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            // Replace parameter column with provided values
            for (int i = 0; i < ORBIT_DIMENSION; i++) {
                dY1dPCurrent[i][index] = dY1dP[i];
            }
        }

        // Initialize Jacobian
        return this.setInitialJacobians(s, dY1dY0Current, dY1dPCurrent);
    }

    /** {@inheritDoc} */
    @Override
    public int getFirstOrderDimension() {
        return ORBIT_DIMENSION * (ORBIT_DIMENSION + this.paramDim);
    }

    /** {@inheritDoc} */
    @Override
    public int getSecondOrderDimension() {
        return 3 * (ORBIT_DIMENSION + this.paramDim);
    }

    /** {@inheritDoc} */
    @Override
    public void writeExternal(final ObjectOutput oo) throws IOException {
        oo.writeInt(this.paramDim);
        // Other attributes are not necessary for interpolation
    }

    /** {@inheritDoc} */
    @Override
    public void readExternal(final ObjectInput oi) throws IOException, ClassNotFoundException {
        this.paramDim = oi.readInt();
        // Other attributes are not necessary for interpolation
    }

    /**
     * Get the list of jacobians providers.
     * @return the list of jacobians providers
     */
    public List<IJacobiansParameterizable> getJacobiansProviders() {
        return this.jacobiansProviders;
    }

    /**
     * Returns true if the initial Jacobians have not been initialized yet.
     * @return the if the initial Jacobians have not been initialized yet
     */
    public boolean isInitialJacobians() {
        return this.isInitialJacobian;
    }

    /**
     * Returns the parameters dimension.
     * @return the parameters dimension
     */
    public int getParamDim() {
        return this.paramDim;
    }

    /**
     * Returns the step used for finite difference computation with respect to spacecraft position.
     * @return the step used for finite difference computation with respect to spacecraft position
     */
    public double gethPos() {
        return this.hPos;
    }

    /**
     * Setter for the step used for finite difference computation with respect to spacecraft position
     * @param value the new step used for finite difference computation with respect to spacecraft position
     */
    public void setHPos(final double value) {
        this.hPos = value;
    }
    
    /**
     * Returns a boolean for force models / selected parameters consistency.
     * @return the boolean for force models / selected parameters consistency
     */
    public boolean isDirty() {
        return this.dirty;
    }

    /**
     * Setter for the boolean for force models / selected parameters consistency.
     * @param value the new boolean for force models / selected parameters consistency
     */
    public void setDirty(final boolean value) {
        this.dirty = value;
    }

    /** {@inheritDoc} */
    @Override
    public void computeDerivatives(final SpacecraftState s,
                                   final TimeDerivativesEquations adder) throws PatriusException {
        // Compute derivatives
        this.computeDAccDState(s);

        // Build derivatives vector

        final int dim = 3;

        // the variational equations of the complete state Jacobian matrix have the
        // following form
        // (Only the A, B, D and E matrices are used along with their derivatives):

        // [      ] [      ] [               ] [                ] [   ] [   ]
        // [ Adot ] [ Bdot ] [ dVel/dPos = 0 ] [ dVel/dVel = Id ] [ A ] [ B ]
        // [      ] [      ] [               ] [                ] [   ] [   ]
        // --------+--------+ ------------------+------------------ -----+-----
        // [      ] [      ]   [           ] [           ]   [   ] [   ]
        // [ Ddot ] [ Edot ] = [ dAcc/dPos ] [ dAcc/dVel ] * [ D ] [ E ]
        // [      ] [      ]   [           ] [           ]   [   ] [   ]
        // --------+--------+ ------------------+------------------ -----+-----

        // The A, B, D and E sub-matrices and their derivatives (Adot ...) are 3x3 matrices,

        // The expanded multiplication above can be rewritten to take into account
        // the fixed values found in the sub-matrices in the left factor. This leads to:

        // [ Adot ] = [ D ]
        // [ Bdot ] = [ E ]
        // [ Ddot ] = [ dAcc/dPos ] * [ A ] + [ dAcc/dVel ] * [ D ]
        // [ Edot ] = [ dAcc/dPos ] * [ B ] + [ dAcc/dVel ] * [ E ]

        // The following loops compute these expressions taking care of the mapping of the
        // (A, B, D, E) matrices into the single dimension array p and of the mapping of the
        // (Adot, Bdot, ... Edot) matrices into the single dimension array pDot.


        // initialize the derivatives of the additional parameters:
        final double[] p = s.getAdditionalState(this.getName());
        final double[] pDot = new double[p.length];

        // copy D, E into Adot, Bdot
        System.arraycopy(p, dim * ORBIT_DIMENSION, pDot, 0, dim * ORBIT_DIMENSION);

        // compute Ddot, Edot
        for (int i = 0; i < dim; ++i) {
            final double[] dAdPi = this.dAccdPos[i];
            final double[] dAdVi = this.dAccdVel[i];
            for (int j = 0; j < ORBIT_DIMENSION; ++j) {
                pDot[(dim + i) * ORBIT_DIMENSION + j] =
                    dAdPi[0] * p[j] + dAdPi[1] * p[j + ORBIT_DIMENSION] + dAdPi[2] * p[j + 2 * ORBIT_DIMENSION] +
                        dAdVi[0] * p[j + 3 * ORBIT_DIMENSION] + dAdVi[1] * p[j + 4 * ORBIT_DIMENSION]
                        + dAdVi[2] * p[j + 5 * ORBIT_DIMENSION];
            }
        }

        for (int k = 0; k < this.getParamDim(); ++k) {

            // compute the acceleration gradient with respect to current parameter
            final ParameterConfiguration param = this.selectedParameters.get(k);
            final IJacobiansParameterizable provider = param.getProvider();
            Arrays.fill(this.dAccdParam, 0.0);
            provider.addDAccDParam(s, param.getParameter(), this.dAccdParam);

            // the variational equations of the parameters Jacobian matrix are computed
            // one column at a time, they have the following form:
            // [ ] [ ] [ ] [ ] [ ]
            // [ Jdot ] [ dVel/dPos = 0 ] [ dVel/dVel = Id ] [ J ] [ dVel/dParam = 0 ]
            // [ ] [ ] [ ] [ ] [ ]
            // -------- ------------------+------------------+ ----- --------------------
            // [ ] [ ] [ ] [ ] [ ]
            // [ Kdot ] = [ dAcc/dPos ] [ dAcc/dVel ] * [ K ] + [ dAcc/dParam ]
            // [ ] [ ] [ ] [ ] [ ]
            // -------- ------------------+------------------+ ----- --------------------

            // The J and K sub-columns and their derivatives (Jdot ...) are 3 elements columns

            // The expanded multiplication and addition above can be rewritten to take into
            // account the fixed values found in the sub-matrices in the left factor. This leads to:

            // [ Jdot ] = [ K ]
            // [ Kdot ] = [ dAcc/dPos ] * [ J ] + [ dAcc/dVel ] * [ K ] + [ dAcc/dParam ]

            // The following loops compute these expressions taking care of the mapping of the
            // (J, K, L) columns into the single dimension array p and of the mapping of the
            // (Jdot, Kdot, Ldot) columns into the single dimension array pDot.

            // copy K into Jdot
            final int columnTop = ORBIT_DIMENSION * ORBIT_DIMENSION + k;
            pDot[columnTop] = p[columnTop + 3 * this.getParamDim()];
            pDot[columnTop + this.getParamDim()] = p[columnTop + 4 * this.getParamDim()];
            pDot[columnTop + 2 * this.getParamDim()] = p[columnTop + 5 * this.getParamDim()];

            // compute Kdot
            for (int i = 0; i < dim; ++i) {
                final double[] dAdPi = this.dAccdPos[i];
                final double[] dAdVi = this.dAccdVel[i];
                pDot[columnTop + (dim + i) * this.getParamDim()] = this.dAccdParam[i] + dAdPi[0] * p[columnTop]
                        + dAdPi[1] * p[columnTop + this.getParamDim()] + dAdPi[2]
                        * p[columnTop + 2 * this.getParamDim()] + dAdVi[0] * p[columnTop + 3 * this.getParamDim()]
                        + dAdVi[1] * p[columnTop + 4 * this.getParamDim()] + dAdVi[2]
                        * p[columnTop + 5 * this.getParamDim()];
            }
        }
        adder.addAdditionalStateDerivative(this.getName(), pDot);
    }

    /** {@inheritDoc} */
    @Override
    public double[] computeSecondDerivatives(final SpacecraftState s) throws PatriusException {
        // Compute derivatives
        this.computeDAccDState(s);

        final int dim = 3;

        // initialize the derivatives of the additional parameters:
        final double[] pDotDot = new double[3 * (ORBIT_DIMENSION + this.getParamDim())];

        // Build second derivative of state transition matrix
        // First 3x3 matrix: da/dr * dr/dr0 (da/dv * dv/dr0 = 0)
        // Second 3x3 matrix: da/dr * dr/dv0 + da/dv * dv/dv0
        final double[][] dXdX0 = this.getMapper().getStateJacobian(s);
        final double[][] pDotDotMat = new double[3][6];
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                for (int k = 0; k < dim; k++) {
                    pDotDotMat[i][j] += this.dAccdPos[i][k] * dXdX0[k][j];
                    pDotDotMat[i][j + 3] += this.dAccdPos[i][k] * dXdX0[k][j + 3]
                            + this.dAccdVel[i][k] * dXdX0[k + 3][j + 3];
                }
            }
        }
        // Convert into 1D-vector
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                pDotDot[i * ORBIT_DIMENSION + j] = pDotDotMat[i][j];
                pDotDot[i * ORBIT_DIMENSION + j + dim] = pDotDotMat[i][j + dim];
            }
        }

        // Build second derivative of sensitivity matrix
        for (int l = 0; l < this.getParamDim(); ++l) {
            // Compute the acceleration gradient with respect to current parameter
            final ParameterConfiguration param = this.selectedParameters.get(l);
            final IJacobiansParameterizable provider = param.getProvider();
            Arrays.fill(this.dAccdParam, 0.0);
            provider.addDAccDParam(s, param.getParameter(), this.dAccdParam);

            // Build second derivative of sensitivity matrix for current parameter
            // 3x1 matrix: da/dp + da/dr * Sr + da/dv * SrDot
            final double[] dXdP = this.getMapper().getParametersJacobian(param.getParameter(), s);
            final double[] pDotDotMat2 = new double[3];
            for (int i = 0; i < dim; i++) {
                pDotDotMat2[i] = this.dAccdParam[i];
                for (int j = 0; j < dim; j++) {
                    pDotDotMat2[i] += this.dAccdPos[i][j] * dXdP[j];
                    pDotDotMat2[i] += this.dAccdVel[i][j] * dXdP[j + dim];
                }
            }
            // Convert into 1D-vector
            for (int i = 0; i < dim; i++) {
                pDotDot[l + i * this.getParamDim() + ORBIT_DIMENSION * dim] = pDotDotMat2[i];
            }
        }

        // Return result
        return pDotDot;
    }

    /**
     * Compute derivatives dAcc/dState.
     * @param s spacecraft state
     * @throws PatriusException thrown if parameters list in inconsistent or one parameter is unknown
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    private void computeDAccDState(final SpacecraftState s) throws PatriusException {
        // CHECKSTYLE: resume CyclomaticComplexity check

        final int dim = 3;

        // Lazy initialization
        if (this.isDirty()) {

            // if step has not been set by user, set a default value
            if (Double.isNaN(this.gethPos())) {
                this.setHPos(MathLib.sqrt(Precision.EPSILON) * s.getPVCoordinates().getPosition().getNorm());
            }

            // set up Jacobians providers
            this.getJacobiansProviders().clear();
            for (final ForceModel model : this.getForceModels()) {
                // Specific case of Droziner model which cannot be computed analytically
                if (model instanceof AbstractBodyAttraction
                        && ((AbstractBodyAttraction) model).getGravityModel() instanceof DrozinerGravityModel) {
                    // wrap the force model to compute the Jacobians by finite differences
                    this.getJacobiansProviders().add(
                            new Jacobianizer(model, this.selectedParameters, this.gethPos()));
                } else {
                    // the force model already provides the Jacobians by itself
                    this.getJacobiansProviders().add((IJacobiansParameterizable) model);
                }
            }

            // check all parameters are handled by at least one Jacobian provider
            for (final ParameterConfiguration param : this.selectedParameters) {
                final Parameter parameterName = param.getParameter();
                boolean found = false;
                for (final IJacobiansParameterizable provider : this.getJacobiansProviders()) {
                    if (provider.supportsParameter(parameterName)) {
                        param.setProvider(provider);
                        found = true;
                    }
                }
                if (!found) {
                    throw new PatriusException(PatriusMessages.UNKNOWN_PARAMETER, parameterName);
                }
            }

            // check the numbers of parameters and matrix size agree
            if (this.selectedParameters.size() != this.getParamDim()) {
                throw new PatriusException(PatriusMessages.INITIAL_MATRIX_AND_PARAMETERS_NUMBER_MISMATCH,
                    this.getParamDim(), this.selectedParameters.size());
            }

            this.dAccdParam = new double[dim];
            this.dAccdPos = new double[dim][dim];
            this.dAccdVel = new double[dim][dim];

            this.setDirty(false);
        }

        // compute forces gradients dAccDState
        for (final double[] row : this.dAccdPos) {
            Arrays.fill(row, 0.0);
        }
        for (final double[] row : this.dAccdVel) {
            Arrays.fill(row, 0.0);
        }
        for (final IJacobiansParameterizable jacobProv : this.getJacobiansProviders()) {
            jacobProv.addDAccDState(s, this.dAccdPos, this.dAccdVel);
        }
    }

    /** {@inheritDoc} */
    @Override
    public double[] buildAdditionalState(final double[] y,
            final double[] yDot) {
        final double[] res = new double[ORBIT_DIMENSION * (ORBIT_DIMENSION + this.getParamDim())];
        // State transition matrix
        System.arraycopy(y, 0, res, 0, 3 * ORBIT_DIMENSION);
        System.arraycopy(yDot, 0, res, 3 * ORBIT_DIMENSION, 3 * ORBIT_DIMENSION);
        // Sensitivity matrix
        for (int i = 0; i < this.getParamDim(); i++) {
            System.arraycopy(y, 3 * (ORBIT_DIMENSION + i), res, ORBIT_DIMENSION * ORBIT_DIMENSION + i * 3, 3);
            System.arraycopy(yDot, 3 * (ORBIT_DIMENSION + i), res, ORBIT_DIMENSION * ORBIT_DIMENSION
                    + (i + this.getParamDim()) * 3, 3);
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public double[] extractY(final double[] additionalState) {
        final double[] y = new double[3 * (ORBIT_DIMENSION + this.getParamDim())];
        // State transition matrix
        System.arraycopy(additionalState, 0, y, 0, 3 * ORBIT_DIMENSION);
        // Sensitivity matrix
        for (int i = 0; i < this.getParamDim(); i++) {
            System.arraycopy(additionalState, ORBIT_DIMENSION * ORBIT_DIMENSION + i * 3, y, 3 * (ORBIT_DIMENSION + i),
                    3);
        }
        return y;
    }

    /** {@inheritDoc} */
    @Override
    public double[] extractYDot(final double[] additionalState) {
        final double[] yDot = new double[3 * (ORBIT_DIMENSION + this.getParamDim())];
        // State transition matrix
        System.arraycopy(additionalState, 3 * ORBIT_DIMENSION, yDot, 0, 3 * ORBIT_DIMENSION);
        // Sensitivity matrix
        for (int i = 0; i < this.getParamDim(); i++) {
            System.arraycopy(additionalState, ORBIT_DIMENSION * ORBIT_DIMENSION + (i + this.getParamDim()) * 3, yDot,
                    3 * (ORBIT_DIMENSION + i), 3);
        }
        return yDot;
    }

    /**
     * Returns the propagator force models.
     * @return the propagator force models
     */
    protected abstract List<ForceModel> getForceModels();
    
    /**
     * Returns the propagator frame.
     * @return the propagator frame
     */
    protected abstract Frame getFrame();
    
    /**
     * Returns the propagator orbit type.
     * @return the propagator orbit type
     */
    protected abstract OrbitType getOrbitType();
    
    /**
     * Returns the propagator position angle.
     * @return the propagator position angle
     */
    protected abstract PositionAngle getPositionAngle();
}
