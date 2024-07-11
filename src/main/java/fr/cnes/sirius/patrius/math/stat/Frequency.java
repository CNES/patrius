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
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.stat;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Maintains a frequency distribution.
 * <p>
 * Accepts int, long, char or Comparable values. New values added must be comparable to those that have been added,
 * otherwise the add method will throw an IllegalArgumentException.
 * </p>
 * <p>
 * Integer values (int, long, Integer, Long) are not distinguished by type -- i.e.
 * <code>addValue(Long.valueOf(2)), addValue(2), addValue(2l)</code> all have the same effect (similarly for arguments
 * to <code>getCount,</code> etc.).
 * </p>
 * <p>
 * char values are converted by <code>addValue</code> to Character instances. As such, these values are not comparable
 * to integral values, so attempts to combine integral types with chars in a frequency distribution will fail.
 * </p>
 * <p>
 * The values are ordered using the default (natural order), unless a <code>Comparator</code> is supplied in the
 * constructor.
 * </p>
 * 
 * @version $Id: Frequency.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class Frequency implements Serializable {

    /** Serializable version identifier */
    private static final long serialVersionUID = -3845586908418844111L;

    /** underlying collection */
    @SuppressWarnings("PMD.LooseCoupling")
    private final TreeMap<Comparable<?>, Long> freqTable;

    /**
     * Default constructor.
     */
    public Frequency() {
        this.freqTable = new TreeMap<Comparable<?>, Long>();
    }

    /**
     * Constructor allowing values Comparator to be specified.
     * 
     * @param comparator
     *        Comparator used to order values
     */
    @SuppressWarnings("unchecked")
    public Frequency(final Comparator<?> comparator) {
        this.freqTable = new TreeMap<Comparable<?>, Long>((Comparator<? super Comparable<?>>) comparator);
    }

    /**
     * Return a string representation of this frequency
     * distribution.
     * 
     * @return a string representation.
     */
    @Override
    public String toString() {
        // Number formatter
        final NumberFormat nf = NumberFormat.getPercentInstance();
        final StringBuilder outBuffer = new StringBuilder();
        outBuffer.append("Value \t Freq. \t Pct. \t Cum Pct. \n");
        final Iterator<Comparable<?>> iter = this.freqTable.keySet().iterator();
        // Loop on data
        while (iter.hasNext()) {
            final Comparable<?> value = iter.next();
            outBuffer.append(value);
            outBuffer.append('\t');
            outBuffer.append(this.getCount(value));
            outBuffer.append('\t');
            outBuffer.append(nf.format(this.getPct(value)));
            outBuffer.append('\t');
            outBuffer.append(nf.format(this.getCumPct(value)));
            outBuffer.append('\n');
        }
        // Return result
        //
        return outBuffer.toString();
    }

    /**
     * Adds 1 to the frequency count for v.
     * <p>
     * If other objects have already been added to this Frequency, v must be comparable to those that have already been
     * added.
     * </p>
     * 
     * @param v
     *        the value to add.
     * @throws MathIllegalArgumentException
     *         if <code>v</code> is not comparable with previous entries
     */
    public void addValue(final Comparable<?> v) {
        this.incrementValue(v, 1);
    }

    /**
     * Increments the frequency count for v.
     * <p>
     * If other objects have already been added to this Frequency, v must be comparable to those that have already been
     * added.
     * </p>
     * 
     * @param v
     *        the value to add.
     * @param increment
     *        the amount by which the value should be incremented
     * @throws IllegalArgumentException
     *         if <code>v</code> is not comparable with previous entries
     * @since 3.1
     */
    @SuppressWarnings("PMD.PreserveStackTrace")
    public void incrementValue(final Comparable<?> v, final long increment) {
        Comparable<?> obj = v;
        if (v instanceof Integer) {
            // convert to Long
            obj = Long.valueOf(((Integer) v).longValue());
        }
        try {
            final Long count = this.freqTable.get(obj);
            if (count == null) {
                // create entry in frequency table for current object
                this.freqTable.put(obj, Long.valueOf(increment));
            } else {
                // increment entry in frequency table for current object
                this.freqTable.put(obj, Long.valueOf(count.longValue() + increment));
            }
        } catch (final ClassCastException ex) {
            // TreeMap will throw ClassCastException if v is not comparable
            throw new MathIllegalArgumentException(
                PatriusMessages.INSTANCES_NOT_COMPARABLE_TO_EXISTING_VALUES,
                v.getClass().getName());
        }
    }

    /**
     * Adds 1 to the frequency count for v.
     * 
     * @param v
     *        the value to add.
     * @throws MathIllegalArgumentException
     *         if the table contains entries not
     *         comparable to Integer
     */
    public void addValue(final int v) {
        this.addValue(Long.valueOf(v));
    }

    /**
     * Adds 1 to the frequency count for v.
     * 
     * @param v
     *        the value to add.
     * @throws MathIllegalArgumentException
     *         if the table contains entries not
     *         comparable to Long
     */
    public void addValue(final long v) {
        this.addValue(Long.valueOf(v));
    }

    /**
     * Adds 1 to the frequency count for v.
     * 
     * @param v
     *        the value to add.
     * @throws MathIllegalArgumentException
     *         if the table contains entries not
     *         comparable to Char
     */
    public void addValue(final char v) {
        this.addValue(Character.valueOf(v));
    }

    /** Clears the frequency table */
    public void clear() {
        this.freqTable.clear();
    }

    /**
     * Returns an Iterator over the set of values that have been added.
     * <p>
     * If added values are integral (i.e., integers, longs, Integers, or Longs), they are converted to Longs when they
     * are added, so the objects returned by the Iterator will in this case be Longs.
     * </p>
     * 
     * @return values Iterator
     */
    public Iterator<Comparable<?>> valuesIterator() {
        return this.freqTable.keySet().iterator();
    }

    /**
     * Return an Iterator over the set of keys and values that have been added.
     * Using the entry set to iterate is more efficient in the case where you
     * need to access respective counts as well as values, since it doesn't
     * require a "get" for every key...the value is provided in the Map.Entry.
     * <p>
     * If added values are integral (i.e., integers, longs, Integers, or Longs), they are converted to Longs when they
     * are added, so the values of the map entries returned by the Iterator will in this case be Longs.
     * </p>
     * 
     * @return entry set Iterator
     * @since 3.1
     */
    public Iterator<Map.Entry<Comparable<?>, Long>> entrySetIterator() {
        return this.freqTable.entrySet().iterator();
    }

    // -------------------------------------------------------------------------

    /**
     * Returns the sum of all frequencies.
     * 
     * @return the total frequency count.
     */
    public long getSumFreq() {
        long result = 0;
        final Iterator<Long> iterator = this.freqTable.values().iterator();
        while (iterator.hasNext()) {
            result += iterator.next().longValue();
        }
        return result;
    }

    /**
     * Returns the number of values = v.
     * Returns 0 if the value is not comparable.
     * 
     * @param v
     *        the value to lookup.
     * @return the frequency of v.
     */
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public long getCount(final Comparable<?> v) {
        if (v instanceof Integer) {
            return this.getCount(((Integer) v).longValue());
        }
        long result = 0;
        try {
            final Long count = this.freqTable.get(v);
            if (count != null) {
                result = count.longValue();
            }
        } catch (final ClassCastException ex) {
            // ignore and return 0 -- ClassCastException will be thrown if value is not comparable
        }
        return result;
    }

    /**
     * Returns the number of values = v.
     * 
     * @param v
     *        the value to lookup.
     * @return the frequency of v.
     */
    public long getCount(final int v) {
        return this.getCount(Long.valueOf(v));
    }

    /**
     * Returns the number of values = v.
     * 
     * @param v
     *        the value to lookup.
     * @return the frequency of v.
     */
    public long getCount(final long v) {
        return this.getCount(Long.valueOf(v));
    }

    /**
     * Returns the number of values = v.
     * 
     * @param v
     *        the value to lookup.
     * @return the frequency of v.
     */
    public long getCount(final char v) {
        return this.getCount(Character.valueOf(v));
    }

    /**
     * Returns the number of values in the frequency table.
     * 
     * @return the number of unique values that have been added to the frequency table.
     * @see #valuesIterator()
     */
    public int getUniqueCount() {
        return this.freqTable.keySet().size();
    }

    /**
     * Returns the percentage of values that are equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns <code>Double.NaN</code> if no values have been added.
     * </p>
     * 
     * @param v
     *        the value to lookup
     * @return the proportion of values equal to v
     */
    public double getPct(final Comparable<?> v) {
        final long sumFreq = this.getSumFreq();
        if (sumFreq == 0) {
            return Double.NaN;
        }
        return (double) this.getCount(v) / (double) sumFreq;
    }

    /**
     * Returns the percentage of values that are equal to v
     * (as a proportion between 0 and 1).
     * 
     * @param v
     *        the value to lookup
     * @return the proportion of values equal to v
     */
    public double getPct(final int v) {
        return this.getPct(Long.valueOf(v));
    }

    /**
     * Returns the percentage of values that are equal to v
     * (as a proportion between 0 and 1).
     * 
     * @param v
     *        the value to lookup
     * @return the proportion of values equal to v
     */
    public double getPct(final long v) {
        return this.getPct(Long.valueOf(v));
    }

    /**
     * Returns the percentage of values that are equal to v
     * (as a proportion between 0 and 1).
     * 
     * @param v
     *        the value to lookup
     * @return the proportion of values equal to v
     */
    public double getPct(final char v) {
        return this.getPct(Character.valueOf(v));
    }

    // -----------------------------------------------------------------------------------------

    /**
     * Returns the cumulative frequency of values less than or equal to v.
     * <p>
     * Returns 0 if v is not comparable to the values set.
     * </p>
     * 
     * @param v
     *        the value to lookup.
     * @return the proportion of values equal to v
     */
    // CHECKSTYLE: stop ReturnCount check
    // Reason: Commons-Math code kept as such
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public long getCumFreq(final Comparable<?> v) {
        // CHECKSTYLE: resume ReturnCount check
        if (this.getSumFreq() == 0) {
            // no frequency
            return 0;
        }
        if (v instanceof Integer) {
            return this.getCumFreq(((Integer) v).longValue());
        }
        // get the comparator for the frequency table
        Comparator<Comparable<?>> c = (Comparator<Comparable<?>>) this.freqTable.comparator();
        if (c == null) {
            c = new NaturalComparator();
        }
        long result = 0;

        try {
            // get value for v from frequency table
            final Long value = this.freqTable.get(v);
            if (value != null) {
                result = value.longValue();
            }
        } catch (final ClassCastException ex) {
            // v is not comparable
            return result;
        }

        if (c.compare(v, this.freqTable.firstKey()) < 0) {
            // v is comparable, but less than first value
            return 0;
        }

        if (c.compare(v, this.freqTable.lastKey()) >= 0) {
            // v is comparable, but greater than the last value
            return this.getSumFreq();
        }

        final Iterator<Comparable<?>> values = this.valuesIterator();
        while (values.hasNext()) {
            final Comparable<?> nextValue = values.next();
            if (c.compare(v, nextValue) > 0) {
                result += this.getCount(nextValue);
            } else {
                return result;
            }
        }
        return result;
    }

    /**
     * Returns the cumulative frequency of values less than or equal to v.
     * <p>
     * Returns 0 if v is not comparable to the values set.
     * </p>
     * 
     * @param v
     *        the value to lookup
     * @return the proportion of values equal to v
     */
    public long getCumFreq(final int v) {
        return this.getCumFreq(Long.valueOf(v));
    }

    /**
     * Returns the cumulative frequency of values less than or equal to v.
     * <p>
     * Returns 0 if v is not comparable to the values set.
     * </p>
     * 
     * @param v
     *        the value to lookup
     * @return the proportion of values equal to v
     */
    public long getCumFreq(final long v) {
        return this.getCumFreq(Long.valueOf(v));
    }

    /**
     * Returns the cumulative frequency of values less than or equal to v.
     * <p>
     * Returns 0 if v is not comparable to the values set.
     * </p>
     * 
     * @param v
     *        the value to lookup
     * @return the proportion of values equal to v
     */
    public long getCumFreq(final char v) {
        return this.getCumFreq(Character.valueOf(v));
    }

    // ----------------------------------------------------------------------------------------------

    /**
     * Returns the cumulative percentage of values less than or equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns <code>Double.NaN</code> if no values have been added. Returns 0 if at least one value has been added, but
     * v is not comparable to the values set.
     * </p>
     * 
     * @param v
     *        the value to lookup
     * @return the proportion of values less than or equal to v
     */
    public double getCumPct(final Comparable<?> v) {
        final long sumFreq = this.getSumFreq();
        if (sumFreq == 0) {
            return Double.NaN;
        }
        return (double) this.getCumFreq(v) / (double) sumFreq;
    }

    /**
     * Returns the cumulative percentage of values less than or equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns 0 if v is not comparable to the values set.
     * </p>
     * 
     * @param v
     *        the value to lookup
     * @return the proportion of values less than or equal to v
     */
    public double getCumPct(final int v) {
        return this.getCumPct(Long.valueOf(v));
    }

    /**
     * Returns the cumulative percentage of values less than or equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns 0 if v is not comparable to the values set.
     * </p>
     * 
     * @param v
     *        the value to lookup
     * @return the proportion of values less than or equal to v
     */
    public double getCumPct(final long v) {
        return this.getCumPct(Long.valueOf(v));
    }

    /**
     * Returns the cumulative percentage of values less than or equal to v
     * (as a proportion between 0 and 1).
     * <p>
     * Returns 0 if v is not comparable to the values set.
     * </p>
     * 
     * @param v
     *        the value to lookup
     * @return the proportion of values less than or equal to v
     */
    public double getCumPct(final char v) {
        return this.getCumPct(Character.valueOf(v));
    }

    // ----------------------------------------------------------------------------------------------

    /**
     * Merge another Frequency object's counts into this instance.
     * This Frequency's counts will be incremented (or set when not already set)
     * by the counts represented by other.
     * 
     * @param other
     *        the other {@link Frequency} object to be merged
     * @since 3.1
     */
    public void merge(final Frequency other) {
        for (final Iterator<Map.Entry<Comparable<?>, Long>> iter = other.entrySetIterator(); iter.hasNext();) {
            final Map.Entry<Comparable<?>, Long> entry = iter.next();
            this.incrementValue(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Merge a {@link Collection} of {@link Frequency} objects into this instance.
     * This Frequency's counts will be incremented (or set when not already set)
     * by the counts represented by each of the others.
     * 
     * @param others
     *        the other {@link Frequency} objects to be merged
     * @since 3.1
     */
    public void merge(final Collection<Frequency> others) {
        for (final Frequency frequency : others) {
            this.merge(frequency);
        }
    }

    // ----------------------------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result +
            ((this.freqTable == null) ? 0 : this.freqTable.hashCode());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            // first fast check
            return true;
        }
        if (!(obj instanceof Frequency)) {
            // different types
            return false;
        }
        // check frequency table equality
        final Frequency other = (Frequency) obj;
        if (this.freqTable == null) {
            if (other.freqTable != null) {
                return false;
            }
        } else if (!this.freqTable.equals(other.freqTable)) {
            return false;
        }
        return true;
    }

    /**
     * A Comparator that compares comparable objects using the
     * natural order. Copied from Commons Collections ComparableComparator.
     * 
     * @param <T> object
     */
    private static class NaturalComparator<T extends Comparable<T>> implements Comparator<Comparable<T>>, Serializable {

        /** Serializable version identifier */
        private static final long serialVersionUID = -3852193713161395148L;

        /**
         * Compare the two {@link Comparable Comparable} arguments.
         * This method is equivalent to:
         * 
         * <pre>
         * (({@link Comparable Comparable})o1).{@link Comparable#compareTo compareTo}(o2)
         * </pre>
         * 
         * @param o1
         *        the first object
         * @param o2
         *        the second object
         * @return result of comparison
         * @throws NullPointerException
         *         when <i>o1</i> is <code>null</code>,
         *         or when <code>((Comparable)o1).compareTo(o2)</code> does
         * @throws ClassCastException
         *         when <i>o1</i> is not a {@link Comparable Comparable},
         *         or when <code>((Comparable)o1).compareTo(o2)</code> does
         */
        @Override
        @SuppressWarnings("unchecked")
        // cast to (T) may throw ClassCastException, see Javadoc
                public
                int compare(final Comparable<T> o1, final Comparable<T> o2) {
            return o1.compareTo((T) o2);
        }
    }
}
