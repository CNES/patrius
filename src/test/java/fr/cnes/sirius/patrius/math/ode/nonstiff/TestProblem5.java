/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 *
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
 */
package fr.cnes.sirius.patrius.math.ode.nonstiff;

/**
 * This class is used in the junit tests for the ODE integrators.
 * <p>
 * This is the same as problem 1 except integration is done backward in time
 * </p>
 */
public class TestProblem5
    extends TestProblem1 {

    /**
     * Simple constructor.
     */
    public TestProblem5() {
        super();
        this.setFinalConditions(2 * this.t0 - this.t1);
    }

    /** {@inheritDoc} */
    @Override
    public TestProblem5 copy() {
        return new TestProblem5();
    }
}
