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

import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealVector;

/**
 * A linear constraint for a linear optimization problem.
 * <p>
 * A linear constraint has one of the forms:
 * <ul>
 * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> = v</li>
 * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> &lt;= v</li>
 * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> >= v</li>
 * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> = r<sub>1</sub>x<sub>1</sub> + ...
 * r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
 * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> &lt;= r<sub>1</sub>x<sub>1</sub> +
 * ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
 * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> >= r<sub>1</sub>x<sub>1</sub> + ...
 * r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
 * </ul>
 * The c<sub>i</sub>, l<sub>i</sub> or r<sub>i</sub> are the coefficients of the constraints, the x<sub>i</sub> are the
 * coordinates of the current point and v is the value of the constraint.
 * </p>
 * 
 * @version $Id: LinearConstraint.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public class LinearConstraint implements Serializable {
    /** Serializable version identifier. */
    private static final long serialVersionUID = -764632794033034092L;
    /** Coefficients of the constraint (left hand side). */
    private transient RealVector coefficients;
    /** Relationship between left and right hand sides (=, &lt;=, >=). */
    private final Relationship relationship;
    /** Value of the constraint (right hand side). */
    private final double value;

    /**
     * Build a constraint involving a single linear equation.
     * <p>
     * A linear constraint with a single linear equation has one of the forms:
     * <ul>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> = v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> &lt;= v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> >= v</li>
     * </ul>
     * </p>
     * 
     * @param coefficientsIn
     *        The coefficients of the constraint (left hand side)
     * @param relationshipIn
     *        The type of (in)equality used in the constraint
     * @param valueIn
     *        The value of the constraint (right hand side)
     */
    public LinearConstraint(final double[] coefficientsIn,
        final Relationship relationshipIn,
        final double valueIn) {
        this(new ArrayRealVector(coefficientsIn), relationshipIn, valueIn);
    }

    /**
     * Build a constraint involving a single linear equation.
     * <p>
     * A linear constraint with a single linear equation has one of the forms:
     * <ul>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> = v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> &lt;= v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> >= v</li>
     * </ul>
     * </p>
     * 
     * @param coefficientsIn
     *        The coefficients of the constraint (left hand side)
     * @param relationshipIn
     *        The type of (in)equality used in the constraint
     * @param valueIn
     *        The value of the constraint (right hand side)
     */
    public LinearConstraint(final RealVector coefficientsIn,
        final Relationship relationshipIn,
        final double valueIn) {
        this.coefficients = coefficientsIn;
        this.relationship = relationshipIn;
        this.value = valueIn;
    }

    /**
     * Build a constraint involving two linear equations.
     * <p>
     * A linear constraint with two linear equation has one of the forms:
     * <ul>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> = r<sub>1</sub>x<sub>1</sub> +
     * ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> &lt;=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> >= r<sub>1</sub>x<sub>1</sub> +
     * ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * </ul>
     * </p>
     * 
     * @param lhsCoefficients
     *        The coefficients of the linear expression on the left hand side of the constraint
     * @param lhsConstant
     *        The constant term of the linear expression on the left hand side of the constraint
     * @param relationshipIn
     *        The type of (in)equality used in the constraint
     * @param rhsCoefficients
     *        The coefficients of the linear expression on the right hand side of the constraint
     * @param rhsConstant
     *        The constant term of the linear expression on the right hand side of the constraint
     */
    public LinearConstraint(final double[] lhsCoefficients, final double lhsConstant,
        final Relationship relationshipIn,
        final double[] rhsCoefficients, final double rhsConstant) {
        final double[] sub = new double[lhsCoefficients.length];
        for (int i = 0; i < sub.length; ++i) {
            sub[i] = lhsCoefficients[i] - rhsCoefficients[i];
        }
        this.coefficients = new ArrayRealVector(sub, false);
        this.relationship = relationshipIn;
        this.value = rhsConstant - lhsConstant;
    }

    /**
     * Build a constraint involving two linear equations.
     * <p>
     * A linear constraint with two linear equation has one of the forms:
     * <ul>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> = r<sub>1</sub>x<sub>1</sub> +
     * ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> &lt;=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> >= r<sub>1</sub>x<sub>1</sub> +
     * ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * </ul>
     * </p>
     * 
     * @param lhsCoefficients
     *        The coefficients of the linear expression on the left hand side of the constraint
     * @param lhsConstant
     *        The constant term of the linear expression on the left hand side of the constraint
     * @param relationshipIn
     *        The type of (in)equality used in the constraint
     * @param rhsCoefficients
     *        The coefficients of the linear expression on the right hand side of the constraint
     * @param rhsConstant
     *        The constant term of the linear expression on the right hand side of the constraint
     */
    public LinearConstraint(final RealVector lhsCoefficients, final double lhsConstant,
        final Relationship relationshipIn,
        final RealVector rhsCoefficients, final double rhsConstant) {
        this.coefficients = lhsCoefficients.subtract(rhsCoefficients);
        this.relationship = relationshipIn;
        this.value = rhsConstant - lhsConstant;
    }

    /**
     * Gets the coefficients of the constraint (left hand side).
     * 
     * @return the coefficients of the constraint (left hand side).
     */
    public RealVector getCoefficients() {
        return this.coefficients;
    }

    /**
     * Gets the relationship between left and right hand sides.
     * 
     * @return the relationship between left and right hand sides.
     */
    public Relationship getRelationship() {
        return this.relationship;
    }

    /**
     * Gets the value of the constraint (right hand side).
     * 
     * @return the value of the constraint (right hand side).
     */
    public double getValue() {
        return this.value;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof LinearConstraint) {
            final LinearConstraint rhs = (LinearConstraint) other;
            return this.relationship == rhs.relationship &&
                this.value == rhs.value &&
                this.coefficients.equals(rhs.coefficients);
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return this.relationship.hashCode() ^
            Double.valueOf(this.value).hashCode() ^
            this.coefficients.hashCode();
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
