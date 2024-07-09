/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
 *
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:454:24/11/2015:Unimplemented method shouldBeRemoved() (StepProblem inherits from EventHandler)
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.math.ode.nonstiff;

import fr.cnes.sirius.patrius.math.ode.FirstOrderDifferentialEquations;
import fr.cnes.sirius.patrius.math.ode.events.EventHandler;

public class StepProblem
    implements FirstOrderDifferentialEquations, EventHandler {

    public StepProblem(final double rateBefore, final double rateAfter,
        final double switchTime) {
        this.rateAfter = rateAfter;
        this.switchTime = switchTime;
        this.setRate(rateBefore);
    }

    @Override
    public void computeDerivatives(final double t, final double[] y, final double[] yDot) {
        yDot[0] = this.rate;
    }

    @Override
    public int getDimension() {
        return 1;
    }

    public void setRate(final double rate) {
        this.rate = rate;
    }

    @Override
    public void init(final double t0, final double[] y0, final double t) {
    }

    @Override
    public boolean shouldBeRemoved() {
        return false;
    }

    @Override
    public Action eventOccurred(final double t, final double[] y, final boolean increasing, final boolean forward) {
        this.setRate(this.rateAfter);
        return Action.RESET_DERIVATIVES;
    }

    @Override
    public double g(final double t, final double[] y) {
        return t - this.switchTime;
    }

    @Override
    public void resetState(final double t, final double[] y) {
    }

    @Override
    public int getSlopeSelection() {
        return 2;
    }

    private double rate;
    private final double rateAfter;
    private final double switchTime;

}
