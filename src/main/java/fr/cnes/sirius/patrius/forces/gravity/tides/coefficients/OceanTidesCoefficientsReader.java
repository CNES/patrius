/**
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
 * 
 * @history Created 13/07/2012
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:241:01/10/2014:improved tides conception
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.tides.coefficients;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import fr.cnes.sirius.patrius.data.DataLoader;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This abstract class represents a Ocean Tides Coefficients file reader.
 * 
 * <p>
 * For any format specific reader of ocean tides coefficients file, this interface represents all the methods that
 * should be implemented by a reader. <br>
 * The proper way to use this it to call the {@link OceanTidesCoefficientsFactory#getCoefficientsProvider()
 * getCoefficientProvider} method. Indeed, the {@link OceanTidesCoefficientsFactory} will determine the best reader to
 * use, depending on file available in the file system.
 * </p>
 * 
 * @concurrency not thread-safe
 * @concurrency.comment instance is mutable
 * 
 * @see OceanTidesCoefficientsFactory
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: OceanTidesCoefficientsReader.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.2
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class OceanTidesCoefficientsReader implements DataLoader, OceanTidesCoefficientsProvider {
    // CHECKSTYLE: resume AbstractClassName check

    /**
     * 1E2
     */
    private static final int ONEPOWTHREE = 1000;
    /**
     * 1E4
     */
    private static final int ONEPOWFIVE = 100000;
    /**
     * 1E7
     */
    private static final int ONEPOWEIGHT = 100000000;

    /**
     * Indicator for completed read.
     */
    protected boolean readCompleted;
    /**
     * Map for ocean tides coefficients
     */
    @SuppressWarnings("PMD.LooseCoupling")
    private final TreeMap<Double, OceanTidesCoefficientsSet> map = new TreeMap<Double, OceanTidesCoefficientsSet>();
    /**
     * Supported names
     */
    private final String names;

    /**
     * Simple constructor.
     * <p>
     * Build an uninitialized reader.
     * </p>
     * 
     * @param supportedNames
     *        regular expression for supported files names
     */
    protected OceanTidesCoefficientsReader(final String supportedNames) {
        this.names = supportedNames;
    }

    /** {@inheritDoc} */
    @Override
    public double[] getDoodsonNumbers() {

        // initialize results container
        final Set<Double> list = new TreeSet<Double>();

        // local map, that we will go through until emptied
        SortedMap<Double, OceanTidesCoefficientsSet> localMap = this.map;

        // store first key and jump to hashCode of current key + 10000 (see the computeHash method)
        Double currentKey;
        while (!localMap.isEmpty()) {
            currentKey = localMap.firstKey();
            list.add(localMap.get(currentKey).getDoodson());
            localMap = localMap.tailMap(currentKey + ONEPOWFIVE);
        }

        // store all Doodson numbers in an array
        final double[] dList = new double[list.size()];
        int temp = 0;
        for (final double doodson : list) {
            dList[temp++] = doodson;
        }

        return dList;
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxDegree(final double doodson, final int order) {

        // keep sub map of given Doodson number
        final double d = doodson * ONEPOWEIGHT;

        // store orders and degrees in a new array
        final SortedMap<Double, OceanTidesCoefficientsSet> reducedMap =
            this.map.subMap(d + order * ONEPOWTHREE, d + order
                * ONEPOWTHREE + ONEPOWTHREE);
        return reducedMap.get(reducedMap.lastKey()).getDegree();
    }

    /** {@inheritDoc} */
    @Override
    public int getMinDegree(final double doodson, final int order) {

        // keep sub map of given Doodson number
        final double d = doodson * ONEPOWEIGHT;

        // store orders and degrees in a new array
        final SortedMap<Double, OceanTidesCoefficientsSet> reducedMap =
            this.map.subMap(d + order * ONEPOWTHREE, d + order
                * ONEPOWTHREE + ONEPOWTHREE);
        return reducedMap.get(reducedMap.firstKey()).getDegree();
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxOrder(final double doodson) {

        // keep sub map of given Doodson number
        final double d = doodson * ONEPOWEIGHT;

        // store orders and degrees in a new array
        final SortedMap<Double, OceanTidesCoefficientsSet> reducedMap = this.map.subMap(d, d + ONEPOWFIVE);
        return reducedMap.get(reducedMap.lastKey()).getOrder();
    }

    /** {@inheritDoc} */
    @Override
    public double[] getCpmSpm(final double nDoodson, final int l, final int m) {

        // retrieve the data set
        final OceanTidesCoefficientsSet current = this.getSet(nDoodson, l, m);

        // return data
        return new double[] { current.getCcp(), current.getCcm(), current.getCsp(), current.getCsm() };
    }

    /** {@inheritDoc} */
    @Override
    public double[] getCpmEpm(final double nDoodson, final int l, final int m) {

        // retrieve dataset
        final OceanTidesCoefficientsSet current = this.getSet(nDoodson, l, m);

        // return data
        return new double[] { current.getCp(), current.getCm(), current.getEp(), current.getEm() };
    }

    /** {@inheritDoc} */
    @Override
    public boolean stillAcceptsData() {
        return !this.readCompleted;
    }

    /** {@inheritDoc} */
    @Override
    public abstract void loadData(InputStream input, String name) throws IOException, ParseException, PatriusException;

    /**
     * Returns a working {@link OceanTidesCoefficientSet}
     * 
     * @param nDoodson
     *        doodson number
     * @param l
     *        degree
     * @param m
     *        order
     * @return the working set
     */
    private OceanTidesCoefficientsSet getSet(final double nDoodson, final int l, final int m) {
        synchronized (this.map) {
            return this.map.get(OceanTidesCoefficientsSet.computeCode(nDoodson, l, m));
        }
    }

    /**
     * Get the regular expression for supported files names.
     * 
     * @return regular expression for supported files names
     */
    public String getSupportedNames() {
        return this.names;
    }

    /**
     * Chech if data map is empty
     * 
     * @return true if it is
     */
    protected boolean isEmpty() {
        synchronized (this.map) {
            return this.map.isEmpty();
        }
    }

    /**
     * Add a {@link OceanTidesCoefficientsSet} to the data map
     * 
     * @param set
     *        set to add to map
     */
    protected void add(final OceanTidesCoefficientsSet set) {
        synchronized (this.map) {
            this.map.put(set.code(), set);
        }
    }

}
