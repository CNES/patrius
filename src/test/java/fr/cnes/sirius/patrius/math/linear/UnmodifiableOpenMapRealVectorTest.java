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
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.linear;

/**
 * This is an implementation of {@link UnmodifiableRealVectorAbstractTest} for
 * unmodifiable views of {@link OpenMapRealVector}.
 *
 * @version $Id: UnmodifiableOpenMapRealVectorTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
/* public class UnmodifiableOpenMapRealVectorTest
 extends UnmodifiableRealVectorAbstractTest {
 /** To ensure sufficient sparsity. * /
 public static final double PROBABILITY_OF_ZERO = 0.5;

 / **
 * Returns a random vector of type {@link ArrayRealVector}.
 *
 * @return a new random {@link ArrayRealVector}.
 * /
 @Override
 public RealVector createVector() {
 OpenMapRealVector v = new OpenMapRealVector(DIM, EPS);
 for (int i = 0; i < DIM; i++) {
 if (RANDOM.nextDouble() > PROBABILITY_OF_ZERO) {
 v.setEntry(i, RANDOM.nextDouble());
 }
 }
 return v;
 }
 }
 */
