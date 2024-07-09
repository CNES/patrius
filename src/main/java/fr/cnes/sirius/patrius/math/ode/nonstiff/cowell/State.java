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
 * HISTORY
* VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.ode.nonstiff.cowell;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Container for (t, y, yDot) state.
 *
 * @author Emmanuel Bignon
 *
 * @4.6
 */
class State implements Externalizable {

    /** Time. */
    protected double time;

    /** State vector. */
    protected double[] y;

    /** State vector derivative. */
    protected double[] yDot;

    /**
     * Empty constructor for {@link Externalizable} methods use.
     */
    @SuppressWarnings("PMD.NullAssignment")
    public State() {
        this.time = -1;
        this.y = null;
        this.yDot = null;
    }

    /**
     * Constructor.
     * @param time time
     * @param y state vector
     * @param yDot state vector derivative
     */
    public State(final double time,
            final double[] y,
            final double[] yDot) {
        this.time = time;
        this.y = y;
        this.yDot = yDot;
    }

    /**
     * Copy constructor.
     * @param state state to copy
     */
    public State(final State state) {
        this.time = state.time;
        this.y = state.y.clone();
        this.yDot = state.yDot.clone();
    }
    
    /** {@inheritDoc} */
    @Override
    public void writeExternal(final ObjectOutput oo) throws IOException {
        // Store all data in ObjectOutput
        // For arrays, store size first
        // Data is stored in the same order they are read in readExternal method
        oo.writeDouble(time);
        oo.writeInt(y.length);
        for (int i = 0; i < y.length; ++i) {
            oo.writeDouble(y[i]);
        }
        oo.writeInt(yDot.length);
        for (int i = 0; i < yDot.length; ++i) {
            oo.writeDouble(yDot[i]);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void readExternal(final ObjectInput oi) throws IOException, ClassNotFoundException {
        // Read all data in ObjectInput
        // For arrays, read size first
        // Data is read in the same order they are stored in writeExternal method
        time = oi.readDouble();
        y = new double[oi.readInt()];
        for (int i = 0; i < y.length; i++) {
            y[i] = oi.readDouble();
        }
        yDot = new double[oi.readInt()];
        for (int i = 0; i < yDot.length; i++) {
            yDot[i] = oi.readDouble();
        }
    }
}
