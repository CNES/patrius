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
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat.descriptive;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.util.DefaultTransformer;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.NumberTransformer;

/**
 * @version $Id: ListUnivariateImpl.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class ListUnivariateImpl extends DescriptiveStatistics implements Serializable {

    /** Serializable version identifier */
    private static final long serialVersionUID = -8837442489133392138L;

    /**
     * Holds a reference to a list - GENERICs are going to make
     * our lives easier here as we could only accept List<Number>
     */
    protected List<Object> list;

    /** Number Transformer maps Objects to Number for us. */
    protected NumberTransformer transformer;

    /**
     * No argument Constructor
     */
    public ListUnivariateImpl() {
        this(new ArrayList<Object>());
    }

    /**
     * Construct a ListUnivariate with a specific List.
     * 
     * @param list
     *        The list that will back this DescriptiveStatistics
     */
    public ListUnivariateImpl(final List<Object> list) {
        this(list, new DefaultTransformer());
    }

    /**
     * Construct a ListUnivariate with a specific List.
     * 
     * @param list
     *        The list that will back this DescriptiveStatistics
     * @param transformer
     *        the number transformer used to convert the list items.
     */
    public ListUnivariateImpl(final List<Object> list, final NumberTransformer transformer) {
        super();
        this.list = list;
        this.transformer = transformer;
    }

    /** {@inheritDoc} */
    @Override
    public double[] getValues() {

        int length = this.list.size();

        // If the window size is not INFINITE_WINDOW AND
        // the current list is larger that the window size, we need to
        // take into account only the last n elements of the list
        // as definied by windowSize

        final int wSize = this.getWindowSize();
        if (wSize != DescriptiveStatistics.INFINITE_WINDOW && wSize < this.list.size()) {
            length = this.list.size() - MathLib.max(0, this.list.size() - wSize);
        }

        // Create an array to hold all values
        final double[] copiedArray = new double[length];

        for (int i = 0; i < copiedArray.length; i++) {
            copiedArray[i] = this.getElement(i);
        }
        return copiedArray;
    }

    /** {@inheritDoc} */
    @Override
    public double getElement(final int index) {

        double value = Double.NaN;

        int calcIndex = index;

        final int wSize = this.getWindowSize();
        if (wSize != DescriptiveStatistics.INFINITE_WINDOW && wSize < this.list.size()) {
            calcIndex = (this.list.size() - wSize) + index;
        }

        try {
            value = this.transformer.transform(this.list.get(calcIndex));
        } catch (final MathIllegalArgumentException e) {
            e.printStackTrace();
        }

        return value;
    }

    /** {@inheritDoc} */
    @Override
    public long getN() {
        int n = 0;

        final int wSize = this.getWindowSize();
        if (wSize != DescriptiveStatistics.INFINITE_WINDOW) {
            if (this.list.size() > wSize) {
                n = wSize;
            } else {
                n = this.list.size();
            }
        } else {
            n = this.list.size();
        }
        return n;
    }

    /** {@inheritDoc} */
    @Override
    public void addValue(final double v) {
        this.list.add(Double.valueOf(v));
    }

    /**
     * Adds an object to this list.
     * 
     * @param o
     *        Object to add to the list
     */
    public void addObject(final Object o) {
        this.list.add(o);
    }

    /**
     * Clears all statistics.
     * <p>
     * <strong>N.B.: </strong> This method has the side effect of clearing the underlying list.
     */
    @Override
    public void clear() {
        this.list.clear();
    }

    /**
     * Apply the given statistic to this univariate collection.
     * 
     * @param stat
     *        the statistic to apply
     * @return the computed value of the statistic.
     */
    @Override
    public double apply(final UnivariateStatistic stat) {
        final double[] v = this.getValues();

        if (v != null) {
            return stat.evaluate(v, 0, v.length);
        }
        return Double.NaN;
    }

    /**
     * Access the number transformer.
     * 
     * @return the number transformer.
     */
    public NumberTransformer getTransformer() {
        return this.transformer;
    }

    /**
     * Modify the number transformer.
     * 
     * @param transformer
     *        the new number transformer.
     */
    public void setTransformer(final NumberTransformer transformer) {
        this.transformer = transformer;
    }

    /** {@inheritDoc} */
    @Override
    public void setWindowSize(final int windowSize) {
        super.setWindowSize(windowSize);
        // Discard elements from the front of the list if the windowSize is less than
        // the size of the list.
        final int extra = this.list.size() - windowSize;
        for (int i = 0; i < extra; i++) {
            this.list.remove(0);
        }
    }

}
