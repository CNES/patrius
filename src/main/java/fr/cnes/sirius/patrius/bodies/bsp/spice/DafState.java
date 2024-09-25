/**
 * Copyright 2023-2023 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.bsp.spice;

import java.util.Arrays;
import java.util.Objects;

/**
 * Define a DAF binary file state indicating where in the file we are and containing the last summary record read.
 * 
 * @author T0281925
 * 
 * @since 4.11
 */
public class DafState {

    /** File handle. */
    private int handle;

    /** Record number of previous array summary. */
    private int recnoPrevSummary;

    /** Record number of current array summary. */
    private int recnoCurrSummary;

    /** Record number of next array summary. */
    private int recnoNextSummary;

    /** Number of summaries in current summary record. */
    private int nSummariesCurrSummaryRecord;

    /** Index of current summary within summary record. */
    private int indexCurrSummary;

    /** Last name record read. */
    private String lastNameRecord;

    /** Flag indicating whether name record containing name of current array is buffered. */
    private boolean buffered;

    /** Last summary record read. */
    private double[] lastSummaryRecord;

    /**
     * Constructor.
     */
    public DafState() {
        final int nWords = 128;
        this.handle = 0;
        this.recnoPrevSummary = 0;
        this.recnoCurrSummary = 0;
        this.recnoNextSummary = 0;
        this.nSummariesCurrSummaryRecord = 0;
        this.indexCurrSummary = 0;
        this.lastNameRecord = "";
        this.buffered = false;
        this.lastSummaryRecord = new double[nWords];
    }

    /**
     * Getter for the state handle.
     * 
     * @return the handle of the DAF
     */
    public int getHandle() {
        return this.handle;
    }

    /**
     * Setter for the state handle.
     * 
     * @param handle
     *        handle of the DAF
     */
    public void setHandle(final int handle) {
        this.handle = handle;
    }

    /**
     * Getter for the record containing the previous summary record.
     * 
     * @return previous summary record
     */
    public int getRecnoPrevSummary() {
        return this.recnoPrevSummary;
    }

    /**
     * Getter for the record containing the current summary record.
     * 
     * @return current summary record
     */
    public int getRecnoCurrSummary() {
        return this.recnoCurrSummary;
    }

    /**
     * Set the current summary record of the DAF.
     * 
     * @param recnoCurrSummary
     *        current summary record
     */
    public void setRecnoCurrSummary(final int recnoCurrSummary) {
        this.recnoCurrSummary = recnoCurrSummary;
    }

    /**
     * Getter for the record containing the next summary record.
     * 
     * @return next summary record
     */
    public int getRecnoNextSummary() {
        return this.recnoNextSummary;
    }

    /**
     * Getter for the number of summaries in the current summary record.
     * 
     * @return number of summaries in the current summary record
     */
    public int getnSummariesCurrSummaryRecord() {
        return this.nSummariesCurrSummaryRecord;
    }

    /**
     * Getter for the index of the current summary within the summary record.
     * 
     * @return index of the current summary within the summary record
     */
    public int getIndexCurrSummary() {
        return this.indexCurrSummary;
    }

    /**
     * Setter for the index of the current summary within the summary record.
     * 
     * @param indexCurrSummary
     *        index of the current summary in the summary record
     */
    public void setIndexCurrSummary(final int indexCurrSummary) {
        this.indexCurrSummary = indexCurrSummary;
    }

    /**
     * Getter for the name of the last name record read.
     * 
     * @return name of the last name record read
     */
    public String getLastNameRecord() {
        return this.lastNameRecord;
    }

    /**
     * Setter for the name of the last name record read.
     * 
     * @param lastNameRecord
     *        last name record read
     */
    public void setLastNameRecord(final String lastNameRecord) {
        this.lastNameRecord = lastNameRecord;
    }

    /**
     * Getter for whether name record containing name of current array is buffered.
     * 
     * @return boolean indicating if the name record is buffered
     */
    public boolean isBuffered() {
        return this.buffered;
    }

    /**
     * Set whether name record containing name of current array is buffered.
     * 
     * @param isBuf
     *        if name record containing name of current array is buffered
     */
    public void setBuffered(final boolean isBuf) {
        this.buffered = isBuf;
    }

    /**
     * Getter for the contents of the last summary record.
     * 
     * @return a double array containing the content of the last summary record
     */
    public double[] getLastSummaryRecord() {
        return Arrays.copyOf(this.lastSummaryRecord, this.lastSummaryRecord.length);
    }

    /**
     * Store the content of the last summary record into the state.
     * 
     * @param lastSummaryRecord
     *        double array containing the last summary record content
     */
    public void setLastSummaryRecord(final double[] lastSummaryRecord) {
        this.lastSummaryRecord = Arrays.copyOf(lastSummaryRecord, lastSummaryRecord.length);
        // As we set the last summaryRecord, some others are updated automatically
        this.recnoNextSummary = (int) lastSummaryRecord[0];
        this.recnoPrevSummary = (int) lastSummaryRecord[1];
        this.nSummariesCurrSummaryRecord = (int) lastSummaryRecord[2];
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(this.handle, this.nSummariesCurrSummaryRecord, this.recnoCurrSummary,
            this.recnoNextSummary, this.recnoPrevSummary);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        // Check the object could be a Counter array
        boolean isEqual = false;

        if (obj == this) {
            // Identity
            isEqual = true;
        } else if ((obj != null) && (obj.getClass() == this.getClass())) {
            final DafState other = (DafState) obj;
            isEqual = Objects.equals(this.handle, other.handle)
                    && Objects.equals(this.nSummariesCurrSummaryRecord, other.nSummariesCurrSummaryRecord)
                    && Objects.equals(this.recnoCurrSummary, other.recnoCurrSummary)
                    && Objects.equals(this.recnoNextSummary, other.recnoNextSummary)
                    && Objects.equals(this.recnoPrevSummary, other.recnoPrevSummary);
        }

        return isEqual;
    }
}
