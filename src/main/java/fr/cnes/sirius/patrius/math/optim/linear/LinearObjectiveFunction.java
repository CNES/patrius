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
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.optim.linear;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import fr.cnes.sirius.patrius.math.analysis.MultivariateFunction;
import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.math.optim.OptimizationData;

/**
 * An objective function for a linear optimization problem.
 * <p>
 * A linear objective function has one the form:
 * 
 * <pre>
 * c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> + d
 * </pre>
 * 
 * The c<sub>i</sub> and d are the coefficients of the equation, the x<sub>i</sub> are the coordinates of the current
 * point.
 * </p>
 * 
 * @version $Id: LinearObjectiveFunction.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public class LinearObjectiveFunction
    implements MultivariateFunction,
    OptimizationData,
    Serializable {
    /** Serializable version identifier. */
    private static final long serialVersionUID = -4531815507568396090L;
    /** Coefficients of the linear equation (c<sub>i</sub>). */
    private transient RealVector coefficients;
    /** Constant term of the linear equation. */
    private final double constantTerm;

    /**
     * @param coefficientsIn
     *        Coefficients for the linear equation being optimized.
     * @param constantTermIn
     *        Constant term of the linear equation.
     */
    public LinearObjectiveFunction(final double[] coefficientsIn, final double constantTermIn) {
        this(new ArrayRealVector(coefficientsIn), constantTermIn);
    }

    /**
     * @param coefficientsIn
     *        Coefficients for the linear equation being optimized.
     * @param constantTermIn
     *        Constant term of the linear equation.
     */
    public LinearObjectiveFunction(final RealVector coefficientsIn, final double constantTermIn) {
        this.coefficients = coefficientsIn;
        this.constantTerm = constantTermIn;
    }

    /**
     * Gets the coefficients of the linear equation being optimized.
     * 
     * @return coefficients of the linear equation being optimized.
     */
    public RealVector getCoefficients() {
        return this.coefficients;
    }

    /**
     * Gets the constant of the linear equation being optimized.
     * 
     * @return constant of the linear equation being optimized.
     */
    public double getConstantTerm() {
        return this.constantTerm;
    }

    /**
     * Computes the value of the linear equation at the current point.
     * 
     * @param point
     *        Point at which linear equation must be evaluated.
     * @return the value of the linear equation at the current point.
     */
    @Override
    public double value(final double[] point) {
        return this.value(new ArrayRealVector(point, false));
    }

    /**
     * Computes the value of the linear equation at the current point.
     * 
     * @param point
     *        Point at which linear equation must be evaluated.
     * @return the value of the linear equation at the current point.
     */
    public double value(final RealVector point) {
        return this.coefficients.dotProduct(point) + this.constantTerm;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof LinearObjectiveFunction) {
            final LinearObjectiveFunction rhs = (LinearObjectiveFunction) other;
            return (this.constantTerm == rhs.constantTerm) && this.coefficients.equals(rhs.coefficients);
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Double.valueOf(this.constantTerm).hashCode() ^ this.coefficients.hashCode();
    }

    /**
     * Serialize the instance.
     * 
     * @param oos
     *        stream where object should be written
     * @throws IOException
     *         if object cannot be written to stream
     */
    private void writeObject(final ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        MatrixUtils.serializeRealVector(this.coefficients, oos);
    }

    /**
     * Deserialize the instance.
     * 
     * @param ois
     *        stream from which the object should be read
     * @throws ClassNotFoundException
     *         if a class in the stream cannot be found
     * @throws IOException
     *         if object cannot be read from the stream
     */
    private void readObject(final ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        coefficients = MatrixUtils.deserializeRealVector(ois);
    }
}
