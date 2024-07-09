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
 * @history creation 23/05/2018
 *
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1489:23/05/2018:add GENOPUS Custom classes
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models.aerocoeffs;

import java.util.ArrayList;

import fr.cnes.sirius.patrius.math.analysis.interpolation.UniLinearIntervalsFunction;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.utils.BinarySearchIndexClosedOpen;
import fr.cnes.sirius.patrius.math.utils.RecordSegmentSearchIndex;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Aerodynamic coefficient function of one variable. Aerodynamic coefficient is then retrieved by
 * linear interpolation of an <x variable, aerodynamic coefficient> array.
 *
 * @author Marie Capot
 *
 * @version $Id$
 *
 * @since 4.1
 *
 */
public abstract class AbstractAeroCoeff1D implements AerodynamicCoefficient {

    /** Serial UID. */
    private static final long serialVersionUID = 9043513987409475841L;

    /** Function. */
    private final UniLinearIntervalsFunction function;

    /** X variable array. */
    private final double[] xTab;

    /** Y variable array. */
    private final double[] yTab;

    /**
     * Constructor.
     * 
     * @param xVariables array of x variables
     * @param yVariables array of y variables
     */
    protected AbstractAeroCoeff1D(final double[] xVariables, final double[] yVariables) {

        // Extend arrays with constant data in order to avoid interpolation problems
        final double[] x = extendedVector(xVariables, xVariables[0] - 1.,
            xVariables[xVariables.length - 1] + 1.);
        final double[] y = extendedVector(yVariables, yVariables[0],
            yVariables[yVariables.length - 1]);

        // Search algorithm and function
        final RecordSegmentSearchIndex searchAlgorithm = new RecordSegmentSearchIndex(
            new BinarySearchIndexClosedOpen(x));
        this.function = new UniLinearIntervalsFunction(searchAlgorithm, y);

        this.xTab = xVariables;
        this.yTab = yVariables;
    }

    /** {@inheritDoc} */
    @Override
    public double value(final SpacecraftState state) {

        double value = 0;
        try {
            final double xVariable = this.computeXVariable(state);
            value = this.function.value(xVariable);
        } catch (final PatriusException e) {
            // If the x variable cannot be computed, as this function cannot
            // throw an exception, it returns infinite.
            value = Double.POSITIVE_INFINITY;
        }
        return value;

    }

    /**
     * Computes the x variable from the spacecraft state.
     * 
     * @param state the spacecraft state
     * @return the x variable
     * @throws PatriusException if there is a problem computing x from state.
     */
    protected abstract double computeXVariable(final SpacecraftState state) throws PatriusException;

    /** {@inheritDoc} */
    @Override
    public ArrayList<Parameter> getParameters() {
        // No supported parameters
        return new ArrayList<Parameter>();
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsParameter(final Parameter param) {
        // No supported parameters
        return false;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    public double derivativeValue(final Parameter p, final SpacecraftState s) {
        // Non supported derivatives
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDifferentiableBy(final Parameter p) {
        // Non supported differences
        return false;
    }

    /**
     * Getter for the x variable array of values used for the function definition.
     * 
     * @return the x variable array of values
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[] getXArray() {
        return this.xTab;
    }

    /**
     * Getter for the y variable array of values used for the function definition.
     * 
     * @return the y variable array of values
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[] getYArray() {
        return this.yTab;
    }

    /**
     * Getter for the function.
     * 
     * @return the function
     */
    public UniLinearIntervalsFunction getFunction() {
        return this.function;
    }

    /**
     * Takes an array of numbers and extends it by adding the first value at the left side of the
     * array and the last value at the right side.
     * 
     * @param values array of numbers to extend
     * @param first first value
     * @param last last value
     * @return the extended array
     */
    private static double[] extendedVector(final double[] values, final double first,
                                           final double last) {
        final double[] outputValues = new double[values.length + 2];
        outputValues[0] = first;
        outputValues[outputValues.length - 1] = last;
        System.arraycopy(values, 0, outputValues, 1, values.length);
        return outputValues;

    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        // Initialization
        //
        final String lit1 = ":[";
        final String lit2 = ", ";
        final StringBuffer buff = new StringBuffer(this.getClass().getSimpleName());
        // Add x array
        buff.append(":[xArray=");
        buff.append(this.getXArray().getClass().getSimpleName());
        buff.append(lit1);
        int last = this.getXArray().length - 1;
        for (int i = 0; i < last; i++) {
            buff.append(this.getXArray()[i]);
            buff.append(lit2);
        }
        buff.append(this.getXArray()[last]);
        // Add y array
        buff.append("], yArray=");
        buff.append(this.getYArray().getClass().getSimpleName());
        buff.append(lit1);
        last = this.getYArray().length - 1;
        for (int i = 0; i < last; i++) {
            buff.append(this.getYArray()[i]);
            buff.append(lit2);
        }
        buff.append(this.getYArray()[last]);
        buff.append("]]");
        // Return result
        //
        return buff.toString();
    }
}
