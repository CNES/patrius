/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
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
package fr.cnes.sirius.patrius.math.geometry.euclidean.oned;

import fr.cnes.sirius.patrius.math.geometry.Vector;
import fr.cnes.sirius.patrius.math.geometry.partitioning.Hyperplane;

//CHECKSTYLE: stop IllegalType check
//Reason: Commons-Math code kept as such

/**
 * This class represents a 1D oriented hyperplane.
 * <p>
 * An hyperplane in 1D is a simple point, its orientation being a boolean.
 * </p>
 * <p>
 * Instances of this class are guaranteed to be immutable.
 * </p>
 * 
 * @version $Id: OrientedPoint.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.0
 */
public class OrientedPoint implements Hyperplane<Euclidean1D> {

    /** Vector location. */
    private final Vector1D location;

    /** Orientation. */
    private boolean direct;

    /**
     * Simple constructor.
     * 
     * @param locationIn
     *        location of the hyperplane
     * @param directIn
     *        if true, the plus side of the hyperplane is towards
     *        abscissas greater than {@code location}
     */
    public OrientedPoint(final Vector1D locationIn, final boolean directIn) {
        this.location = locationIn;
        this.direct = directIn;
    }

    /**
     * Copy the instance.
     * <p>
     * Since instances are immutable, this method directly returns the instance.
     * </p>
     * 
     * @return the instance itself
     */
    @Override
    public OrientedPoint copySelf() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public double getOffset(final Vector<Euclidean1D> point) {
        final double delta = ((Vector1D) point).getX() - this.location.getX();
        return this.direct ? delta : -delta;
    }

    /**
     * Build a region covering the whole hyperplane.
     * <p>
     * Since this class represent zero dimension spaces which does not have lower dimension sub-spaces, this method
     * returns a dummy implementation of a {@link fr.cnes.sirius.patrius.math.geometry.partitioning.SubHyperplane
     * SubHyperplane}. This implementation is only used to allow the
     * {@link fr.cnes.sirius.patrius.math.geometry.partitioning.SubHyperplane
     * SubHyperplane} class implementation to work properly, it should <em>not</em> be used otherwise.
     * </p>
     * 
     * @return a dummy sub hyperplane
     */
    @Override
    public SubOrientedPoint wholeHyperplane() {
        return new SubOrientedPoint(this, null);
    }

    /**
     * Build a region covering the whole space.
     * 
     * @return a region containing the instance (really an {@link IntervalsSet IntervalsSet} instance)
     */
    @Override
    public IntervalsSet wholeSpace() {
        return new IntervalsSet();
    }

    /** {@inheritDoc} */
    @Override
    public boolean sameOrientationAs(final Hyperplane<Euclidean1D> other) {
        return !(this.direct ^ ((OrientedPoint) other).direct);
    }

    /**
     * Get the hyperplane location on the real line.
     * 
     * @return the hyperplane location
     */
    public Vector1D getLocation() {
        return this.location;
    }

    /**
     * Check if the hyperplane orientation is direct.
     * 
     * @return true if the plus side of the hyperplane is towards
     *         abscissae greater than hyperplane location
     */
    public boolean isDirect() {
        return this.direct;
    }

    /**
     * Revert the instance.
     */
    public void revertSelf() {
        this.direct = !this.direct;
    }

    // CHECKSTYLE: resume IllegalType check
}
