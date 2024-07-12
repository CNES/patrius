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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math;

import fr.cnes.sirius.patrius.math.exception.MathArithmeticException;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;

/**
 * Interface representing <a href="http://mathworld.wolfram.com/Field.html">field</a> elements.
 * 
 * @param <T>
 *        the type of the field elements
 * @see Field
 * @version $Id: FieldElement.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 2.0
 */
public interface FieldElement<T> {

    /**
     * Compute this + a.
     * 
     * @param a
     *        element to add
     * @return a new element representing this + a
     * @throws NullArgumentException
     *         if {@code addend} is {@code null}.
     */
    T add(T a);

    /**
     * Compute this - a.
     * 
     * @param a
     *        element to subtract
     * @return a new element representing this - a
     * @throws NullArgumentException
     *         if {@code a} is {@code null}.
     */
    T subtract(T a);

    /**
     * Returns the additive inverse of {@code this} element.
     * 
     * @return the opposite of {@code this}.
     */
    T negate();

    /**
     * Compute n &times; this. Multiplication by an integer number is defined
     * as the following sum
     * <center>
     * n &times; this = &sum;<sub>i=1</sub><sup>n</sup> this.
     * </center>
     * 
     * @param n
     *        Number of times {@code this} must be added to itself.
     * @return A new element representing n &times; this.
     */
    T multiply(int n);

    /**
     * Compute this &times; a.
     * 
     * @param a
     *        element to multiply
     * @return a new element representing this &times; a
     * @throws NullArgumentException
     *         if {@code a} is {@code null}.
     */
    T multiply(T a);

    /**
     * Compute this &divide; a.
     * 
     * @param a
     *        element to add
     * @return a new element representing this &divide; a
     * @throws NullArgumentException
     *         if {@code a} is {@code null}.
     * @throws MathArithmeticException
     *         if {@code a} is zero
     */
    T divide(T a);

    /**
     * Returns the multiplicative inverse of {@code this} element.
     * 
     * @return the inverse of {@code this}.
     * @throws MathArithmeticException
     *         if {@code this} is zero
     */
    T reciprocal();

    /**
     * Get the {@link Field} to which the instance belongs.
     * 
     * @return {@link Field} to which the instance belongs
     */
    Field<T> getField();
}
