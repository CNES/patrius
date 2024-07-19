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
 */
/*
 *
 * HISTORY
* VERSION:4.11:DM:DM-38:22/05/2023:[PATRIUS] Suppression de setters pour le MultiNumericalPropagator
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:FA:FA-2942:15/11/2021:[PATRIUS] Reliquat FA 2902 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.7:FA:FA-2902:18/05/2021:Anomalie dans la gestion du JacobiansMapper
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * VERSION::DM:441:12/05/2015:add methods to set and retrieve partial derivatives
 * VERSION::DM:483:20/10/2015: Modification of signature of some methods JaccobianMapper
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.numerical;

import java.io.Serializable;
import java.util.List;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.DecompositionSolver;
import fr.cnes.sirius.patrius.math.linear.QRDecomposition;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Mapper between two-dimensional Jacobian matrices and one-dimensional
 * additional state arrays.
 * <p>
 * This class does not hold the states by itself. Instances of this class are guaranteed to be
 * immutable.
 * </p>
 *
 * @author Luc Maisonobe
 * @see fr.cnes.sirius.patrius.propagation.numerical.PartialDerivativesEquations
 * @see fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator
 * @see fr.cnes.sirius.patrius.propagation.AbstractPropagator
 */
public class JacobiansMapper implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -2125324068169838971L;

    /** State vector dimension without additional parameters. */
    private static final int ORBIT_DIMENSION = 6;

    /** Name. */
    private final String name;

    /** Number of Parameters. */
    private final int parameters;

    /** Parameters list. */
    private final List<Parameter> parametersList;

    /** Orbit type. */
    private final OrbitType orbitType;

    /** Position angle type. */
    private final PositionAngle angleType;

    /** Propagation frame. */
    private final Frame propagationFrame;

    /**
     * Simple constructor.
     *
     * @param nameIn
     *        name of the Jacobians
     *        being included or not)
     * @param list
     *        parameters list
     * @param orbitTypeIn
     *        orbit type
     * @param angleTypeIn
     *        position angle type
     * @param frame
     *        propagation frame
     */
    public JacobiansMapper(final String nameIn, final List<Parameter> list,
            final OrbitType orbitTypeIn, final PositionAngle angleTypeIn, final Frame frame) {
        this.name = nameIn;
        this.parametersList = list;
        this.parameters = list.size();
        this.orbitType = orbitTypeIn;
        this.angleType = angleTypeIn;
        this.propagationFrame = frame;
    }

    /**
     * Get the name of the partial Jacobians.
     *
     * @return name of the Jacobians
     */
    public String getName() {
        return this.name;
    }

    /**
     * Compute the length of the one-dimensional additional state array needed.
     *
     * @return length of the one-dimensional additional state array
     */
    public int getAdditionalStateDimension() {
        return ORBIT_DIMENSION * (ORBIT_DIMENSION + this.parameters);
    }

    /**
     * Get the state vector dimension.
     *
     * @return state vector dimension
     */
    public int getStateDimension() {
        return ORBIT_DIMENSION;
    }

    /**
     * Get the number of parameters.
     *
     * @return number of parameters
     */
    public int getParameters() {
        return this.parameters;
    }

    /**
     * Get parameters list.
     *
     * @return parameters list
     */
    public List<Parameter> getParametersList() {
        return this.parametersList;
    }

    /**
     * Getter for the orbit type.
     *
     * @return the orbit type
     */
    public OrbitType getOrbitType() {
        return this.orbitType;
    }

    /**
     * Getter for the position angle type.
     *
     * @return the position angle type
     */
    public PositionAngle getAngleType() {
        return this.angleType;
    }
    
    /**
     * Getter for the propagation frame.
     *
     * @return the propagation frame
     */
    public Frame getPropagationFrame() {
        return this.propagationFrame;
    }

    /**
     * Get the conversion Jacobian from initial state parameters (cartesian, propagationFrame) to
     * current state parameters expressed in the orbit type, positionAngle and frame of the spacecraftState.
     *
     * @param state
     *        spacecraft state
     * @return conversion Jacobian
     * @throws PatriusException thrown if jacobian computation failed
     */
    private double[][] getdYdC(final SpacecraftState state) throws PatriusException {
        return getdYdC(state, this.orbitType, this.angleType, this.propagationFrame);
    }

    /**
     * Get the conversion Jacobian from initial state parameters (cartesian, propagationFrame) to
     * current state parameters expressed in the required orbit type, positionAngle and frame.
     *
     * @param state
     *        spacecraft state
     * @param orbitTypeOut
     *        orbit type
     * @param angleTypeOut
     *        position angle type
     * @param frameOut
     *        propagation frame
     * @return conversion Jacobian
     * @throws PatriusException thrown if jacobian computation failed
     */
    private double[][] getdYdC(final SpacecraftState state, final OrbitType orbitTypeOut,
            final PositionAngle angleTypeOut, final Frame frameOut) throws PatriusException {
        return state
                .getOrbit()
                .getJacobian(this.propagationFrame, frameOut, OrbitType.CARTESIAN, orbitTypeOut,
                        PositionAngle.TRUE, angleTypeOut).getData(false);
    }

    /**
     * Set the Jacobian with respect to state into a one-dimensional additional state array.
     * <p>
     * This method converts the Jacobians to cartesian parameters and put the converted data in the
     * one-dimensional {@code p} array.
     * </p>
     *
     * @param state
     *        spacecraft state
     * @param dY1dY0
     *        Jacobian of current state at time t<sub>1</sub>
     *        with respect to state at some previous time t<sub>0</sub>
     * @param dY1dP
     *        Jacobian of current state at time t<sub>1</sub>
     *        with respect to parameters (may be null if there are no parameters)
     * @param p
     *        placeholder where to put the one-dimensional additional state
     * @throws PatriusException thrown if jacobian computation failed
     * @see #getStateJacobian(SpacecraftState, double[][])
     */
    public void setInitialJacobians(final SpacecraftState state, final double[][] dY1dY0,
            final double[][] dY1dP, final double[] p) throws PatriusException {

        // set up a converter between state parameters and cartesian parameters
        final RealMatrix dY1dC1 = new Array2DRowRealMatrix(this.getdYdC(state), false);
        final DecompositionSolver solver = new QRDecomposition(dY1dC1).getSolver();

        // convert the provided state Jacobian to cartesian parameters
        final RealMatrix dC1dY0 = solver.solve(new Array2DRowRealMatrix(dY1dY0, false));

        // map the converted state Jacobian to one-dimensional array
        int index = 0;
        for (int i = 0; i < ORBIT_DIMENSION; ++i) {
            for (int j = 0; j < ORBIT_DIMENSION; ++j) {
                p[index++] = dC1dY0.getEntry(i, j);
            }
        }

        if (this.parameters > 0) {
            // convert the provided state Jacobian to cartesian parameters
            final RealMatrix dC1dP = solver.solve(new Array2DRowRealMatrix(dY1dP, false));

            // map the converted parameters Jacobian to one-dimensional array
            for (int i = 0; i < ORBIT_DIMENSION; ++i) {
                for (int j = 0; j < this.parameters; ++j) {
                    p[index++] = dC1dP.getEntry(i, j);
                }
            }
        }
    }

    /**
     * Get the Jacobian with respect to state.
     * <p>
     * This method extract the data from the state and put it in the {@code dYdY0} array.
     * </p>
     *
     * @param state
     *        spacecraft state
     * @param dYdY0
     *        placeholder where to put the Jacobian with respect to state
     * @throws PatriusException
     *         thrown if partial derivatives are not included in state
     * @see #getParametersJacobian(SpacecraftState, double[][])
     * @see #getParametersJacobian(Parameter, SpacecraftState, double[])
     */
    public void getStateJacobian(final SpacecraftState state, final double[][] dYdY0)
            throws PatriusException {
        getStateJacobian(state, dYdY0, this.orbitType, this.angleType, this.propagationFrame);
    }

    /**
     * Get the Jacobian with respect to state.
     * <p>
     * This method extract the data from the state and put it in the {@code dYdY0} array.
     * </p>
     * <p>
     * The jacobian is converted into the specified orbit type, angle type and frame. To be more
     * specific, if M is the conversion, the new jacobian is Jn = M * J (i.e. only the output part
     * is converted)
     * </p>
     *
     * @param state
     *        spacecraft state
     * @param dYdY0
     *        placeholder where to put the Jacobian with respect to state
     * @param orbitTypeIn
     *        orbit type
     * @param angleTypeIn
     *        position angle type
     * @param frameIn
     *        propagation frame
     * @throws PatriusException
     *         thrown if partial derivatives are not included in state
     * @see #getParametersJacobian(SpacecraftState, double[][])
     * @see #getParametersJacobian(Parameter, SpacecraftState, double[])
     */
    public void getStateJacobian(final SpacecraftState state, final double[][] dYdY0,
            final OrbitType orbitTypeIn, final PositionAngle angleTypeIn, final Frame frameIn)
            throws PatriusException {
        // Get the Jacobian
        final double[][] res = this.getStateJacobian(state, orbitTypeIn, angleTypeIn, frameIn);
        // Copy result into array
        for (int i = 0; i < ORBIT_DIMENSION; i++) {
            for (int j = 0; j < ORBIT_DIMENSION; ++j) {
                dYdY0[i][j] = res[i][j];
            }
        }
    }

    /**
     * Get the Jacobian with respect to state.
     * <p>
     * This method extract the data from the state.
     * </p>
     *
     * @param state
     *        spacecraft state
     * @return Jacobian with respect to state
     * @throws PatriusException
     *         thrown if partial derivatives are not included in state
     * @see #getParametersJacobian(SpacecraftState)
     * @see #getParametersJacobian(Parameter, SpacecraftState)
     */
    public double[][] getStateJacobian(final SpacecraftState state) throws PatriusException {
        return getStateJacobian(state, this.orbitType, this.angleType, this.propagationFrame);
    }

    /**
     * Get the Jacobian with respect to state.
     * <p>
     * This method extract the data from the state.
     * </p>
     * <p>
     * The jacobian is converted into the specified orbit type, angle type and frame. To be more
     * specific, if M is the conversion, the new jacobian is Jn = M * J (i.e. only the output part
     * is converted)
     * </p>
     *
     * @param state
     *        spacecraft state
     * @param orbitTypeOut
     *        orbit type
     * @param angleTypeOut
     *        position angle type
     * @param frameOut
     *        frame
     * @return Jacobian with respect to state
     * @throws PatriusException
     *         thrown if partial derivatives are not included in state
     * @see #getParametersJacobian(SpacecraftState)
     * @see #getParametersJacobian(Parameter, SpacecraftState)
     */
    public double[][] getStateJacobian(final SpacecraftState state, final OrbitType orbitTypeOut,
            final PositionAngle angleTypeOut, final Frame frameOut) throws PatriusException {

        // get the conversion Jacobian between state parameters and cartesian parameters
        final double[][] dYdY0 = new double[ORBIT_DIMENSION][ORBIT_DIMENSION];

        // get the conversion Jacobian between state parameters and cartesian parameters
        final double[][] dYdC = getdYdC(state, orbitTypeOut, angleTypeOut, frameOut);

        // Get partial derivatives vector
        final double[] p = state.getAdditionalState(this.name);

        // compute dYdY0 = dYdC * dCdY0, without allocating new arrays
        for (int i = 0; i < ORBIT_DIMENSION; i++) {
            final double[] rowC = dYdC[i];
            final double[] rowD = dYdY0[i];
            for (int j = 0; j < ORBIT_DIMENSION; ++j) {
                double sum = 0;
                int pIndex = j;
                for (int k = 0; k < ORBIT_DIMENSION; ++k) {
                    sum += rowC[k] * p[pIndex];
                    pIndex += ORBIT_DIMENSION;
                }
                rowD[j] = sum;
            }
        }
        return dYdY0;
    }

    /**
     * Get the Jacobian with respect to parameters.
     * <p>
     * This method extract the data from the state and put it in the {@code dYdP} array.
     * </p>
     * <p>
     * If no parameters have been set in the constructor, the method returns immediately and does
     * not reference {@code dYdP} which can safely be null in this case.
     * </p>
     *
     * @param state
     *        spacecraft state
     * @param dYdP
     *        placeholder where to put the Jacobian with respect to parameters
     * @throws PatriusException
     *         thrown if partial derivatives are not included in state
     * @see #getStateJacobian(SpacecraftState, double[][])
     * @see #getParametersJacobian(Parameter, SpacecraftState, double[])
     */
    public void getParametersJacobian(final SpacecraftState state, final double[][] dYdP)
            throws PatriusException {
        getParametersJacobian(state, dYdP, this.orbitType, this.angleType, this.propagationFrame);
    }

    /**
     * Get the Jacobian with respect to parameters.
     * <p>
     * This method extract the data from the state and put it in the {@code dYdP} array.
     * </p>
     * <p>
     * If no parameters have been set in the constructor, the method returns immediately and does
     * not reference {@code dYdP} which can safely be null in this case.
     * </p>
     * <p>
     * The jacobian is converted into the specified orbit type, angle type and frame. To be more
     * specific, if M is the conversion, the new jacobian is Jn = M * J (i.e. only the output part
     * is converted)
     * </p>
     *
     * @param state
     *        spacecraft state
     * @param dYdP
     *        placeholder where to put the Jacobian with respect to parameters
     * @param orbitTypeIn
     *        orbit type
     * @param angleTypeIn
     *        position angle type
     * @param frameIn
     *        propagation frame
     * @throws PatriusException
     *         thrown if partial derivatives are not included in state
     * @see #getStateJacobian(SpacecraftState, double[][])
     * @see #getParametersJacobian(Parameter, SpacecraftState, double[])
     */
    public void getParametersJacobian(final SpacecraftState state, final double[][] dYdP,
            final OrbitType orbitTypeIn, final PositionAngle angleTypeIn, final Frame frameIn)
            throws PatriusException {
        // Get the Jacobian
        final double[][] res = this.getParametersJacobian(state, orbitTypeIn, angleTypeIn, frameIn);
        // Copy result into array
        for (int i = 0; i < ORBIT_DIMENSION; i++) {
            for (int j = 0; j < this.parameters; ++j) {
                dYdP[i][j] = res[i][j];
            }
        }
    }

    /**
     * Get the Jacobian with respect to parameters.
     * <p>
     * This method extract the data from the state.
     * </p>
     *
     * @param state
     *        spacecraft state
     * @return Jacobian with respect to parameters
     * @throws PatriusException
     *         thrown if partial derivatives are not included in state
     * @see #getStateJacobian(SpacecraftState)
     * @see #getParametersJacobian(Parameter, SpacecraftState)
     */
    public final double[][] getParametersJacobian(final SpacecraftState state)
            throws PatriusException {
        return getParametersJacobian(state, this.orbitType, this.angleType, this.propagationFrame);
    }

    /**
     * Get the Jacobian with respect to parameters.
     * <p>
     * This method extract the data from the state.
     * </p>
     * <p>
     * The jacobian is converted into the specified orbit type, angle type and frame. To be more
     * specific, if M is the conversion, the new jacobian is Jn = M * J (i.e. only the output part
     * is converted)
     * </p>
     *
     * @param state
     *        spacecraft state
     * @param orbitTypeOut
     *        orbit type
     * @param angleTypeOut
     *        position angle type
     * @param frameOut
     *        frame
     * @return Jacobian with respect to parameters
     * @throws PatriusException
     *         thrown if partial derivatives are not included in state
     * @see #getStateJacobian(SpacecraftState)
     * @see #getParametersJacobian(Parameter, SpacecraftState)
     */
    public final double[][] getParametersJacobian(final SpacecraftState state,
            final OrbitType orbitTypeOut, final PositionAngle angleTypeOut, final Frame frameOut)
            throws PatriusException {

        // Initialization
        final double[][] dYdP = new double[ORBIT_DIMENSION][this.parameters];

        if (this.parameters > 0) {

            // get the conversion Jacobian between state parameters and cartesian parameters
            final double[][] dYdC = getdYdC(state, orbitTypeOut, angleTypeOut, frameOut);

            // Get partial derivatives vector
            final double[] p = state.getAdditionalState(this.name);

            // compute dYdP = dYdC * dCdP, without allocating new arrays
            // Simple matrices multiplication
            for (int i = 0; i < ORBIT_DIMENSION; i++) {
                final double[] rowC = dYdC[i];
                final double[] rowD = dYdP[i];
                for (int j = 0; j < this.parameters; ++j) {
                    double sum = 0;
                    int pIndex = j + (ORBIT_DIMENSION * ORBIT_DIMENSION);
                    for (int k = 0; k < ORBIT_DIMENSION; ++k) {
                        sum += rowC[k] * p[pIndex];
                        pIndex += this.parameters;
                    }
                    rowD[j] = sum;
                }
            }
        }
        // Return result
        return dYdP;
    }

    /**
     * Get the Jacobian with respect to provided parameter {@code parameter}.
     * <p>
     * This method extract the data from the state.
     * </p>
     *
     * @param parameter
     *        parameter
     * @param state
     *        spacecraft state
     * @param dYdP
     *        placeholder where to put the Jacobian with respect to provided parameter
     * @throws PatriusException
     *         thrown if partial derivatives are not included in state
     * @see #getParametersJacobian(SpacecraftState, double[][])
     */
    public void getParametersJacobian(final Parameter parameter, final SpacecraftState state,
            final double[] dYdP) throws PatriusException {
        getParametersJacobian(parameter, state, dYdP, this.orbitType, this.angleType,
                this.propagationFrame);
    }

    /**
     * Get the Jacobian with respect to provided parameter {@code parameter}.
     * <p>
     * This method extract the data from the state.
     * </p>
     * <p>
     * The jacobian is converted into the specified orbit type, angle type and frame. To be more
     * specific, if M is the conversion, the new jacobian is Jn = M * J (i.e. only the output part
     * is converted)
     * </p>
     *
     * @param parameter
     *        parameter
     * @param state
     *        spacecraft state
     * @param dYdP
     *        placeholder where to put the Jacobian with respect to provided parameter
     * @param orbitTypeIn
     *        orbit type
     * @param angleTypeIn
     *        position angle type
     * @param frameIn
     *        propagation frame
     * @throws PatriusException
     *         thrown if partial derivatives are not included in state
     * @see #getParametersJacobian(SpacecraftState, double[][])
     */
    public void getParametersJacobian(final Parameter parameter, final SpacecraftState state,
            final double[] dYdP, final OrbitType orbitTypeIn, final PositionAngle angleTypeIn,
            final Frame frameIn) throws PatriusException {
        // Get the Jacobian
        final double[] res = this.getParametersJacobian(parameter, state, orbitTypeIn, angleTypeIn,
                frameIn);
        // Copy result into array
        if (res != null) {
            System.arraycopy(res, 0, dYdP, 0, ORBIT_DIMENSION);
        }
    }

    /**
     * Get the Jacobian with respect to provided parameter {@code parameter}.
     * <p>
     * This method extract the data from the state and put it in the {@code dYdP} array.
     * </p>
     * <p>
     * If the parameter is not present in the partial derivatives, the method returns immediately
     * and does not reference {@code dYdP} which can safely be null in this case.
     * </p>
     *
     * @param parameter
     *        parameter
     * @param state
     *        spacecraft state
     * @return Jacobian with respect to provided parameter, null if parameter is not included in
     *         Jacobian matrix
     * @throws PatriusException
     *         thrown if partial derivatives are not included in state
     * @see #getParametersJacobian(SpacecraftState)
     */
    public double[] getParametersJacobian(final Parameter parameter, final SpacecraftState state)
            throws PatriusException {
        return getParametersJacobian(parameter, state, this.orbitType, this.angleType,
                this.propagationFrame);
    }

    /**
     * Get the Jacobian with respect to provided parameter {@code parameter}.
     * <p>
     * This method extract the data from the state and put it in the {@code dYdP} array.
     * </p>
     * <p>
     * If the parameter is not present in the partial derivatives, the method returns immediately
     * and does not reference {@code dYdP} which can safely be null in this case.
     * </p>
     * <p>
     * The jacobian is converted into the specified orbit type, angle type and frame. To be more
     * specific, if M is the conversion, the new jacobian is Jn = M * J (i.e. only the output part
     * is converted)
     * </p>
     *
     * @param parameter
     *        parameter
     * @param state
     *        spacecraft state
     * @param orbitTypeIn
     *        orbit type
     * @param angleTypeIn
     *        position angle type
     * @param frameIn
     *        propagation frame
     * @return Jacobian with respect to provided parameter, null if parameter is not included in
     *         Jacobian matrix
     * @throws PatriusException
     *         thrown if partial derivatives are not included in state
     * @see #getParametersJacobian(SpacecraftState)
     */
    public double[] getParametersJacobian(final Parameter parameter, final SpacecraftState state,
            final OrbitType orbitTypeIn, final PositionAngle angleTypeIn, final Frame frameIn)
            throws PatriusException {

        // Find column number
        int index = -1;
        for (int i = 0; i < this.parametersList.size(); i++) {
            if (parameter.equals(this.parametersList.get(i))) {
                index = i;
                break;
            }
        }

        // Extract column if parameter exist
        if (index == -1) {
            // Parameter does not exist
            return null;
        }

        // Retrieve matrix
        final double[][] matdYdP = this.getParametersJacobian(state, orbitTypeIn, angleTypeIn,
                frameIn);

        final double[] dYdP = new double[ORBIT_DIMENSION];
        for (int i = 0; i < ORBIT_DIMENSION; i++) {
            dYdP[i] = matdYdP[i][index];
        }
        // Return result
        return dYdP;
    }
}
