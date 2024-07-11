/**
 * 
 * Copyright 2011-2022 CNES
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
 * @history Created 07/11/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.variations.coefficients;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import fr.cnes.sirius.patrius.data.DataLoader;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Abstract class representing a variable potential coefficients file reader. No actual "file reading" takes place in
 * this class, as the loadData method is delegated to sub-classes, but this class handles all the data structures and
 * answers the {@link VariablePotentialCoefficientsProvider} interface.
 * 
 * @concurrency not thread-safe
 * @concurrency.comment because of static fields
 * 
 * @see VariableGravityFieldFactory
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: VariablePotentialCoefficientsReader.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.3
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class VariablePotentialCoefficientsReader implements DataLoader, VariablePotentialCoefficientsProvider {
    // CHECKSTYLE: resume AbstractClassName check

    /** equatorial radius */
    private double ae;

    /** gravitational constant */
    private double mu;

    /** Indicator for completed read */
    private boolean readCompleted;

    /** Normalized data map */
    private final Map<Integer, Map<Integer, VariablePotentialCoefficientsSet>> data = new ConcurrentHashMap<>();

    /**
     * Supported names
     */
    private final String names;

    /**
     * Max degree of data (from provider)
     */
    private int maxDegree;

    /**
     * Ref date of file
     */
    private AbsoluteDate date;

    /**
     * Simple constructor.
     * <p>
     * Build an uninitialized reader.
     * </p>
     * 
     * @param supportedNames
     *        regular expression for supported files names
     */
    protected VariablePotentialCoefficientsReader(final String supportedNames) {
        this.names = supportedNames;
    }

    /** {@inheritDoc} */
    @Override
    public boolean stillAcceptsData() {
        return !this.readCompleted;
    }

    /** {@inheritDoc} */

    /**
     * Get the regular expression for supported files names.
     * 
     * @return regular expression for supported files names
     */
    public String getSupportedNames() {
        return this.names;
    }

    /** {@inheritDoc} */
    @Override
    public Map<Integer, Map<Integer, VariablePotentialCoefficientsSet>> getData() {
        return Collections.unmodifiableMap(this.data);
    }

    /** {@inheritDoc} */
    @Override
    public double getMu() {
        return this.mu;
    }

    /** {@inheritDoc} */
    @Override
    public double getAe() {
        return this.ae;
    }

    /**
     * Add an entry
     * 
     * @param set
     *        set to add to the map
     */
    protected void add(final VariablePotentialCoefficientsSet set) {
        this.data.get(set.getDegree()).put(set.getOrder(), set);
    }

    /**
     * Add a new orders list for given degree
     * 
     * @param deg
     *        degree
     */
    protected void put(final Integer deg) {
        this.data.put(deg, new TreeMap<Integer, VariablePotentialCoefficientsSet>());
    }

    /**
     * @param aeIn
     *        the ae to set
     */
    protected void setAe(final double aeIn) {
        this.ae = aeIn;
    }

    /**
     * @param muIn
     *        the mu to set
     */
    protected void setMu(final double muIn) {
        this.mu = muIn;
    }

    /**
     * @param read
     *        the readCompleted to set
     */
    protected void setReadCompleted(final boolean read) {
        this.readCompleted = read;
    }

    /**
     * Set file year
     * 
     * @param fileYear
     *        reference year of the loaded file
     * @throws PatriusException
     *         if UTC timescale fails
     */
    protected void setYear(final int fileYear) throws PatriusException {
        this.date = new AbsoluteDate(fileYear, 1, 1, TimeScalesFactory.getUTC());
    }

    /**
     * @return the reference year of the file
     */
    @Override
    public AbsoluteDate getDate() {
        return this.date;
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxDegree() {
        return this.maxDegree;
    }

    /**
     * Set max degree
     * 
     * @param degree
     *        max degree available
     */
    protected void setMaxDegree(final int degree) {
        this.maxDegree = degree;
    }

}
